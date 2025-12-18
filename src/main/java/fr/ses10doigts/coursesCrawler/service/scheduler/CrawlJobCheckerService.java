package fr.ses10doigts.coursesCrawler.service.scheduler;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.ses10doigts.coursesCrawler.model.paris.Paris;
import fr.ses10doigts.coursesCrawler.model.schedule.ScheduledTask;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.*;
import fr.ses10doigts.coursesCrawler.model.telegram.Verbose;
import fr.ses10doigts.coursesCrawler.repository.course.*;
import fr.ses10doigts.coursesCrawler.service.bet.BetService;
import fr.ses10doigts.coursesCrawler.service.web.TelegramService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.model.Configuration;
import fr.ses10doigts.coursesCrawler.model.schedule.ScheduleStatus;
import fr.ses10doigts.coursesCrawler.repository.ScheduledTaskRepository;
import fr.ses10doigts.coursesCrawler.service.crawl.CrawlService;
import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({ "dev", "telegram" })
public class CrawlJobCheckerService {
	private static final Logger logger = LoggerFactory.getLogger(CrawlJobCheckerService.class);

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
	private ScheduledTaskRepository taskRepository;
	@Autowired
	private TelegramService telegramService;
	@Autowired
	private BetService betService;


    public void checkStart(ScheduledTask task, float percent, String courseType, int nbPartantMin, int nbPartantMax, int nbReunionMax)  {

		if( task == null ){
			logger.warn("/!\\ Given task is null");
			return;
		}

		logger.debug("Scheduled execution : taskId: " + task
				+ ", messageId: " + task.getTelegramMessageId()
				+ ", courseUrl: "+ task.getCourseUrl()
				+ ", courseDescr: " + task.getCourseDescription());

        try {
			Long codeCourse = extractCode(task.getCourseUrl());

			// Lancer un crawl de la page uniquement
			Configuration conf = new Configuration();
			conf.setAgressivity(Agressivity.MEDIUM);
			conf.setAuthorized("/cotes.*" + codeCourse + "\r\n");
			conf.setLaunchCrawl(true);
			conf.setLaunchRefacto(false);
			conf.setMaxHop(0);
			conf.setMaxRetry(10);
			conf.setWaitOnRetry(true);
			conf.setTxtSeeds("https://www.geny.com/cotes?id_course="+codeCourse);
			configurationService.saveConfiguration(conf);
			logger.debug("Starting scheduled crawl");
			crawlService.manageLaunch(true);

			// Attendre sa fin
			Thread crawlThread = crawlService.getTreatment();
			crawlThread.join();
			logger.debug("Scheduled crawl ended");

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
			logger.debug("Retreive stats");
			CourseComplete courseStats = getCourseStats(codeCourse, cotes);

			boolean isInStats = false;
			String stats = "";
			// Parfois pas de résultat de la course...
			if( courseStats.getNombrePartant() != null ) {
				boolean isPartantsOK = courseStats.getNombrePartant() >= nbPartantMin
						&& courseStats.getNombrePartant() <= nbPartantMax;

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

				boolean isTypeOk = courseType.equalsIgnoreCase(courseStats.getTypeCourse());

				boolean isNbReuMaxOk = courseStats.getNumeroReunion() <= nbReunionMax;

				isInStats = isTypeOk && isPercentOk && isPartantsOK && isNbReuMaxOk;

				Paris paris = null;
				if( isInStats ){
					paris = betService.generateBet(c, new BigDecimal(100), courseStats.getNumeroChlPremierAvant());
				}


				//@formatter:off
				stats = ( isInStats ? "✅ Course dans les stats.\nMise de "+paris.getMise()+"e sur Cheval N°"+courseStats.getNumeroChlPremierAvant()
									: "❌ Course hors stats...")+"\n\n"
						+(isPartantsOK?"✅":"❌")+" nb Partant: "+courseStats.getNombrePartant()+"\n"
						+(isPercentOk?"✅":"❌")+" Pourcent: "+courseStats.getPourcentPremierAvant()
						+" + "+ courseStats.getPourcentDeuxiemeAvant()
						+" + "+ courseStats.getPourcentTroisiemeAvant()
						+" = "+sumPercent+"\n"
						+(isTypeOk?"✅":"❌")+" Type: "+courseStats.getTypeCourse()+"\n"
						+(isNbReuMaxOk?"✅":"❌")+" N° Réunion: "+courseStats.getNumeroReunion();
				//@formatter:on

			}else{
				stats = "❌ Sans résultats...";
			}

            logger.debug("Course checked : {}\n{}", task.getCourseDescription(), stats);

			if( isInStats || configurationService.getConfiguration().getTelegramVerbose().equals(Verbose.HIGH) ) {
				try {
					telegramService.sendMessage(task.getTelegramMessageId(),
							task.getCourseDescription()
									+ "\uD83D\uDD17 [Lien Cotes](https://www.geny.com/cotes?id_course=" + codeCourse + ")\n\n"
									+ stats);
				}catch (Exception e){
					logger.error("Error sending Telegram message !!! \n{}", stats);
				}
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

		logger.info("Scheduled check end for {}", courseID );

		try {
			// Lancer un crawl de la page uniquement
			Configuration conf = new Configuration();
			conf.setAgressivity(Agressivity.MEDIUM);
			conf.setAuthorized("/arrivee-et-rapports-pmu.*" + courseID + "\r\n");
			conf.setLaunchCrawl(true);
			conf.setLaunchRefacto(false);
			conf.setMaxHop(0);
			conf.setMaxRetry(10);
			conf.setWaitOnRetry(true);
			conf.setTxtSeeds("https://www.geny.com/arrivee-et-rapports-pmu?id_course="+courseID);
			configurationService.saveConfiguration(conf);
			logger.info("Starting crawl for ended course");
			crawlService.manageLaunch(true);

			// Attendre sa fin
			Thread crawlThread = crawlService.getTreatment();
			crawlThread.join();
			logger.info("Crawl for ended course ended");

			// Récup des infos
			Set<Rapport> rapports = rapportRepository.findAllByCourseID(courseID);
			List<Course> courses = courseRepository.findByCourseID(courseID);
			Course course = null;
			if(!courses.isEmpty())
				course = courses.get(0);
			else
				throw new RuntimeException("CourseID "+courseID+" undefined..." );


			// Calcul stats
			logger.info("Retrieve rapports...");
			CourseComplete courseStats = getCourseStats(rapports);
			Paris paris = betService.updateBetResult(courseStats);

			//@formatter:off
			String txt = "\uD83C\uDFC1 Résultat course: "+course.getHippodrome()+", "+course.getReunion()+", "+course.getCourse()+" à "+course.getHeures()+"h"+course.getMinutes()
					+ "\uD83D\uDD17 [Lien Arrivées](https://www.geny.com/arrivee-et-rapports-pmu?id_course=" + courseID + ")\n\n"
					+( paris.getIsWin() ?
						"✅ Gagnée.\n" +
						"Misé "+paris.getMise()+" sur Cheval N°"+paris.getNumChevalMise()+"\n"+
						"- Gain: "+paris.getGain()
					: "❌ Perdue.\n" +
						"Misé "+paris.getMise()+" sur Cheval N°"+paris.getNumChevalMise()+"\n"+
						"Est arrivé 1er le cheval N°"+courseStats.getNumeroChvlPremier()
					);
			//@formatter:on

			logger.info("Course Arrivee checked : {}\n{}", paris, txt);

			if( configurationService.getConfiguration().getTelegramVerbose().equals(Verbose.HIGH) ) {
				telegramService.sendMessage(telegramMessageId, txt);
			}

		} catch (Exception e) {
			logger.error("Error during CheckEnd : {}", e.getMessage());
			throw new RuntimeException(e); // TODO personal exception
		}
	}


	private Long extractCode(String url) {
		Pattern pattern = Pattern.compile("_c(\\d+)$");
		Matcher matcher = pattern.matcher(url);

		Long found = null;
		if (matcher.find()) {
			String code = matcher.group(1);
			found = Long.parseLong(code);

		}else{
			pattern = Pattern.compile("id_course=(\\d+)$");
			matcher = pattern.matcher(url);
			if (matcher.find()) {
				String code = matcher.group(1);
				found = Long.parseLong(code);

			}
		}

		return found;
	}

	private CourseComplete getCourseStats(Set<Rapport> rapports) {
		CourseComplete cc = new CourseComplete();

        boolean found = false;
		for(Rapport rapport : rapports){
			if( rapport.getArrivee() == 1 ){
				cc.setCourseID(rapport.getCourseID());
				cc.setRapGagnantPmu( rapport.getGagnantPmu() );
				cc.setNumeroChvlPremier( rapport.getNumCheval() );
                found = true;
                break;
			}
		}

        if( found ){
            logger.info("Rapport found: c{}, rap {}, N°{}",
                    cc.getCourseID(), cc.getRapGagnantPmu(), cc.getNumeroChvlPremier());
        }else{
            logger.warn("No first place in given Rapports !!!!!!!!!");
        }

		return cc;
	}

	private CourseComplete getCourseStats(Long courseID, Set<Cote> cotes) {
		CourseComplete cc = new CourseComplete();
		Course course = null;
		List<Course> byCourseID = courseRepository.findByCourseID(courseID);
		if (byCourseID.size() == 1) {
			course = byCourseID.get(0);
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

		return cc;
	}

}