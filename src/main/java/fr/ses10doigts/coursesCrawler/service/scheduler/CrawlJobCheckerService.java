package fr.ses10doigts.coursesCrawler.service.scheduler;

import fr.ses10doigts.coursesCrawler.model.Configuration;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.model.paris.Paris;
import fr.ses10doigts.coursesCrawler.model.schedule.ScheduleStatus;
import fr.ses10doigts.coursesCrawler.model.schedule.ScheduledTask;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.*;
import fr.ses10doigts.coursesCrawler.model.telegram.Verbose;
import fr.ses10doigts.coursesCrawler.repository.ScheduledTaskRepository;
import fr.ses10doigts.coursesCrawler.repository.course.*;
import fr.ses10doigts.coursesCrawler.service.bet.BetService;
import fr.ses10doigts.coursesCrawler.service.crawl.CrawlService;
import fr.ses10doigts.coursesCrawler.service.scrap.tool.FieldTool;
import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;
import fr.ses10doigts.coursesCrawler.service.web.TelegramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Profile({ "devWithTelegram", "telegram" })
@Slf4j
public class CrawlJobCheckerService {
	@Value("${fr.ses10doigts.crawler.puppeteerInitialBet}")
	private Long INITIAL_BET;

    @Autowired
	private ConfigurationService configurationService;
    @Autowired
	private CrawlService crawlService;
    @Autowired
    private PartantRepository partantRepository;
	@Autowired
	private CoteRepository coteRepository;
	@Autowired
	private CourseRepository courseRepository;
	@Autowired
	private RapportRepository rapportRepository;
	@Autowired
	private ArriveeRepository arriveeRepository;
	@Autowired
	private ScheduledTaskRepository taskRepository;
	@Autowired
	private TelegramService telegramService;
	@Autowired
	private BetService betService;
	@Autowired
	private CourseFilterService courseFilterService;

    public void checkStart(ScheduledTask task, float percent, String courseType, List<Integer>nbPartants, int nbReunionMax)  {

		if( task == null ){
			log.warn("/!\\ Given task is null");
			return;
		}

		log.debug("Scheduled execution : taskId: " + task
				+ ", messageId: " + task.getTelegramMessageId()
				+ ", courseUrl: "+ task.getCourseUrl()
				+ ", courseDescr: " + task.getCourseDescription());

        try {
			Long codeCourse = FieldTool.extractCode(task.getCourseUrl());

			// Lancer un crawl de la page uniquement
			Configuration conf = new Configuration();
			conf.setAgressivity(Agressivity.MEDIUM);
			conf.setAuthorized("/cotes.*" + codeCourse + "\r\n");
			conf.setLaunchCrawl(true);
			conf.setLaunchRefacto(false);
			conf.setLaunchExcel(false);
			conf.setMaxHop(0);
			conf.setMaxRetry(10);
			conf.setWaitOnRetry(true);
			conf.setTxtSeeds("https://www.geny.com/cotes?id_course="+codeCourse);
			configurationService.saveConfiguration(conf);
			log.debug("Starting scheduled crawl");
			crawlService.crawlFromConfig(true);

			// Attendre sa fin
			Thread crawlThread = crawlService.getTreatment();
			crawlThread.join();
			log.debug("Scheduled crawl ended");

			// Récup de la course
			List<Course> courses = courseRepository.findByCourseID(codeCourse);

			// Si l'heure à changer... rescheduler
			Course c = null;
			if( !courses.isEmpty() ){
				c = courses.get(0);
				if( c.getDateChanged() != null && c.getDateChanged() ){
					// Est-on après "now"
					LocalTime nowPlus = LocalTime.now().plusMinutes(4);

					int hour = Integer.parseInt(c.getHeures());
					int minute = Integer.parseInt(c.getMinutes());
					LocalTime courseTime = LocalTime.of(hour, minute);
					if( courseTime.isAfter(nowPlus) ){
						task.setStatus(ScheduleStatus.RESCHEDULED);
						return;
					}
				}
			}

			// Sinon récupération des cotes
			Set<Cote> cotes = coteRepository.findByCourseID(codeCourse);

			// Calcul stats
			log.debug("Retreive stats");
			CourseComplete courseStats = getCourseStats(codeCourse, cotes);

			boolean isInStats = false;
			String stats = "";
			// Parfois pas de résultat de la course...
			if( courseStats!= null && courseStats.getNombrePartant() != null ) {
				// Nb Partants
				boolean isPartantsOK = nbPartants.contains(courseStats.getNombrePartant());

				// Percents
				boolean isPercentOk = true;
				float sumPercent = 0;
				if (courseStats.getPourcentPremierAvant() != null && courseStats.getPourcentDeuxiemeAvant() != null &&
						courseStats.getPourcentTroisiemeAvant() != null) {
					sumPercent = courseStats.getPourcentPremierAvant() + courseStats.getPourcentDeuxiemeAvant()
							+ courseStats.getPourcentTroisiemeAvant();
				}

				if (sumPercent < percent) {
					isPercentOk = false;
				}

				// Type
				boolean isTypeOk = courseType.equalsIgnoreCase(courseStats.getTypeCourse());

				// Nb Reunion
				boolean isNbReuMaxOk = courseStats.getNumeroReunion() <= nbReunionMax;

				// Age
				Course course = courseRepository.findByCourseID(courseStats.getCourseID()).get(0);
				CourseFilterService.AgeCheckResult ageCheck = courseFilterService.evaluateAuthorizedAges(
						course,
						configurationService.getProps().getFilterListAuthorizedAges()
				);
				String ageLine = formatAgeCheckLine(ageCheck, configurationService.getProps().getFilterListAuthorizedAges());

				// Total
				isInStats = isTypeOk && isPercentOk && isPartantsOK && isNbReuMaxOk && ageCheck.matches();

				Paris paris = null;
				if( isInStats ){
					paris = betService.generateBet(BigInteger.valueOf(INITIAL_BET), courseStats, c);
				}

				stats = ( isInStats ? "✅ Course dans les stats.\nMise de "+paris.getMise()+"e sur Cheval N°"+courseStats.getNumeroChlPremierAvant()
									: "❌ Course hors stats...")+"\n\n"
						+(isPartantsOK?"✅":"❌")+" nb Partant: "+courseStats.getNombrePartant()+"\n"
						+(isPercentOk?"✅":"❌")+" Pourcent: "+courseStats.getPourcentPremierAvant()
						+" + "+ courseStats.getPourcentDeuxiemeAvant()
						+" + "+ courseStats.getPourcentTroisiemeAvant()
						+" = "+sumPercent+"\n"
						+(isTypeOk?"✅":"❌")+" Type: "+courseStats.getTypeCourse()+"\n"
						+(isNbReuMaxOk?"✅":"❌")+" N° Réunion: "+courseStats.getNumeroReunion()+"\n"
						+(ageCheck.matches()?"✅":"❌")+ageLine;

				if( isInStats &&  !paris.getIsWebActionOk()) {
					String url = "https://www.genybet.fr/courses/partants-pronostics/"+courseStats.getCourseID();

					stats += "\n\n⚠\uFE0F Vérifier si le paris s'est bien déroulé sur le site ⚠\uFE0F";
					stats += "\uD83D\uDD17 [Lien Genybet]("+ url + ")";
					telegramService.sendPhoto(task.getTelegramMessageId(), "/home/oklm/courses/lastAction.png");
				}

			}else{
				stats = "❌ Sans résultats...";
			}

            log.debug("Course checked : {}\n{}", task.getCourseDescription(), stats);

			if( isInStats || configurationService.getConfiguration().getTelegramVerbose().equals(Verbose.HIGH) ) {

				telegramService.sendMessage(task.getTelegramMessageId(),
						task.getCourseDescription()
								+ "\uD83D\uDD17 [Lien Cotes](https://www.geny.com/cotes?id_course=" + codeCourse + ")\n\n"
								+ stats);

			}

			// Update
			task.setLastExecution(LocalDateTime.now());
			task.setStatus(ScheduleStatus.SUCCESS);

        } catch (Exception e) {
            // tracer ERREUR
			task.setLastExecution(LocalDateTime.now());
			task.setStatus(ScheduleStatus.ERROR);
			task.setErrorMessage(e.getMessage());

            throw new RuntimeException(e); // TODO personal exception
        }
    }

	public void checkEnd( long courseID, long telegramMessageId ){

		log.info("Scheduled check end for {}", courseID );

		try {
			// Lancer un crawl de la page uniquement
			Configuration conf = new Configuration();
			conf.setAgressivity(Agressivity.MEDIUM);
			conf.setAuthorized("/arrivee-et-rapports-pmu.*" + courseID + "\r\n");
			conf.setLaunchCrawl(true);
			conf.setLaunchRefacto(false);
			conf.setLaunchExcel(false);
			conf.setMaxHop(0);
			conf.setMaxRetry(10);
			conf.setWaitOnRetry(true);
			conf.setTxtSeeds("https://www.geny.com/arrivee-et-rapports-pmu?id_course="+courseID);
			configurationService.saveConfiguration(conf);
			log.info("Starting crawl for ended course");
			crawlService.crawlFromConfig(true);

			// Attendre sa fin
			Thread crawlThread = crawlService.getTreatment();
			crawlThread.join();
			log.info("Crawl for ended course ended");

			// Récup des infos
			Set<Rapport> rapports = rapportRepository.findAllByCourseID(courseID);
			List<Course> courses = courseRepository.findByCourseID(courseID);
			Course course = null;
			if(!courses.isEmpty())
				course = courses.get(0);
			else
				throw new RuntimeException("CourseID "+courseID+" undefined..." );

			// Calcul stats
			log.info("Retrieve rapports...");
			CourseComplete courseStats = new CourseComplete();
			if( !rapports.isEmpty() ) {
				courseStats = getCourseStats(rapports);
			}else{
				log.warn("Seems that trouble parsing Rapport, will use Arrivee to define 1st place...");
				Set<Arrivee> arrivees = arriveeRepository.findByCourseID(courseID);
				courseStats = getCourseResult(arrivees);
			}
			courseStats.setCourseID(courseID); // parfois problème de parsing des rapport et donc ils sont vides... TODO
			Paris paris = betService.updateBetResult(courseStats);

			String txt = "\uD83C\uDFC1 Résultat course: \n"+course.getHippodrome()+", R"+course.getReunion()+", C"+course.getCourse()+" à "+course.getHeures()+"h"+course.getMinutes()
					+ "\n\uD83D\uDD17 [Lien Arrivées](https://www.geny.com/arrivee-et-rapports-pmu?id_course=" + courseID + ")\n\n"
					+( paris.getIsWin() ?
						"✅ Gagnée.\n" +
						"Misé "+paris.getMise()+" sur N°"+paris.getNumChevalMise()+"\n"+
						"- Gain: "+(paris.getGain() == null? "(Problème récup Gain...)":paris.getGain()+"x"+paris.getMise()+"="
								+(new BigDecimal(paris.getMise()).multiply( BigDecimal.valueOf(paris.getGain()) )))
					: "❌ Perdue.\n" +
						"Misé "+paris.getMise()+" sur N°"+paris.getNumChevalMise()+"\n"+
						"Est arrivé 1er le N°"+courseStats.getNumeroChvlPremier()
					);

			log.info("Course Arrivee checked : {}\n{}", paris, txt);

			if( configurationService.getConfiguration().getTelegramVerbose().equals(Verbose.HIGH) ) {
				telegramService.sendMessage(telegramMessageId, txt);
			}

		} catch (Exception e) {
			log.error("Error during CheckEnd : {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private String formatAgeCheckLine(CourseFilterService.AgeCheckResult ageCheck, List<String> configuredRanges) {
		String configured = (configuredRanges == null || configuredRanges.isEmpty())
				? "aucun filtre"
				: String.join(", ", configuredRanges);

		if (!ageCheck.filterEnabled()) {
			return "Ages: (non filtre) - config: " + configured;
		}

		String status = ageCheck.matches() ? "OK" : "KO";
		if (ageCheck.ageMin() == null || ageCheck.ageMax() == null) {
			return "Ages: " + status + " " + ageCheck.reason() + " - config: " + configured;
		}

		return "Ages: " + status + " " + ageCheck.ageMin() + "-" + ageCheck.ageMax() + " - config: " + configured;
	}

	private CourseComplete getCourseResult(Set<Arrivee> arrivees) {
		CourseComplete cc = new CourseComplete();

		boolean found = false;
		for(Arrivee arrivee: arrivees){
			if( arrivee.getNumArrivee() == 1 ){
				cc.setCourseID(arrivee.getCourseID());
				//cc.setRapGagnantPmu( arrivee.getGagnantPmu() );
				cc.setNumeroChvlPremier( arrivee.getNumChv() );
				found = true;
				break;
			}
		}

		if( found ){
			log.info("Arrivée found: c{}, rap {}, N°{}",
					cc.getCourseID(), cc.getRapGagnantPmu(), cc.getNumeroChvlPremier());
		}else{
			log.warn("No first place in given Arrivée !!!!!!!!!");
		}

		return cc;
	}

	private CourseComplete getCourseStats(Set<Rapport> rapports) {
		CourseComplete cc = new CourseComplete();

        boolean found = false;
		for(Rapport rapport : rapports){
			if( rapport.getArrivee() == 1 ){
				cc.setCourseID(rapport.getCourseID());
				cc.setRapGagnantGeny( rapport.getGagnantGeny() );
				cc.setNumeroChvlPremier( rapport.getNumCheval() );
                found = true;
                break;
			}
		}

        if( found ){
            log.info("Rapport found: c{}, rap {}, N°{}",
                    cc.getCourseID(), cc.getRapGagnantPmu(), cc.getNumeroChvlPremier());
        }else{
            log.warn("No first place in given Rapports !!!!!!!!!");
        }

		return cc;
	}

	private CourseComplete getCourseStats(Long courseID, Set<Cote> cotes) {
		CourseComplete cc = new CourseComplete();
		Course course = null;
		List<Course> byCourseID = courseRepository.findByCourseID(courseID);
		if (byCourseID.size() == 1) {
			course = byCourseID.get(0);
		}else{
			return null;
		}

		// Récupération des 3 plus gros "enjeuxAvant"
		List<Cote> top3 = cotes.stream().filter(c -> c.getEnjeuxAvant() != null)
				.sorted(Comparator.comparing(Cote::getEnjeuxAvant).reversed()).limit(3).toList();

		int i = 0;
		for (Cote cote : top3) {
			if (i == 0) {
				cc.setPourcentPremierAvant(cote.getEnjeuxAvant());
				cc.setNumeroChlPremierAvant(cote.getNumCheval());
			} else if (i == 1) {
				cc.setPourcentDeuxiemeAvant(cote.getEnjeuxAvant());
			} else if (i == 2) {
				cc.setPourcentTroisiemeAvant(cote.getEnjeuxAvant());
			}
			i++;
		}

		cc.setAutoStart( course.getDepart() );
		cc.setTypeCourse( course.getType() );
		cc.setNombrePartant( cotes.size() );
		cc.setNumeroReunion( course.getReunion() );
		cc.setCourseID(courseID);

		return cc;
	}

}



