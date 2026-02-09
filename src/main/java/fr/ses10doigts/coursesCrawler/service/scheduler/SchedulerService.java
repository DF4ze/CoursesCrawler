package fr.ses10doigts.coursesCrawler.service.scheduler;

import fr.ses10doigts.coursesCrawler.model.paris.GlobalBilanParis;
import fr.ses10doigts.coursesCrawler.model.paris.Paris;
import fr.ses10doigts.coursesCrawler.model.schedule.ScheduleStatus;
import fr.ses10doigts.coursesCrawler.model.schedule.ScheduledTask;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Course;
import fr.ses10doigts.coursesCrawler.model.telegram.Verbose;
import fr.ses10doigts.coursesCrawler.repository.ScheduledTaskRepository;
import fr.ses10doigts.coursesCrawler.repository.course.CourseRepository;
import fr.ses10doigts.coursesCrawler.service.bet.BetService;
import fr.ses10doigts.coursesCrawler.service.bet.GlobalBilanAsyncService;
import fr.ses10doigts.coursesCrawler.service.crawl.CrawlService;
import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;
import fr.ses10doigts.coursesCrawler.service.web.TelegramService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Profile({ "devWithTelegram", "telegram" })
public class SchedulerService {

	private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);
	private static final Long TELEGRAM_GROUP = -4706435457L;

	@Getter
    private int timeBefore = 20;
	public void setTimeBefore( int tb ){
		timeBefore = tb;
		changeScheduledTaskTiming( tb );
	}



	@Autowired
	private CrawlService crawlService;
	@Autowired
	private TelegramService telegramService;
	@Autowired
	private CrawlJobCheckerService checkerService;
	@Autowired
	private ConfigurationService configurationService;

	@Autowired
	private ScheduledTaskRepository taskRepository;
	@Autowired
	private CourseRepository courseRepository;
	@Autowired
	private BetService betService;
	@Autowired
	private GlobalBilanAsyncService bilanService;


	@Scheduled(cron = "${fr.ses10doigts.crawler.schedulerChecker}") // Toutes les minutes
	public void everyMinutes() {
		// recup en db si job scheduledDate avant maintenant et status SCHEDULED
		List<ScheduledTask> scheduledTasks = getScheduledTasks();
		logger.debug("Founded {} task(s) to execute.", scheduledTasks.size());

		boolean once = true;

		for(ScheduledTask task : scheduledTasks){
			// Une seule fois, on met à jour la dernière course
			if( once ) {
				once = false;
				try {
					Paris paris = betService.getLastBet();
					if (paris != null && !paris.getIsEnded())
						checkerService.checkEnd(paris.getCourse().getCourseID(), task.getTelegramMessageId());
				} catch (Exception e) {
					logger.error("Error during ScheduledTask : {}", e.getMessage());
				}
			}

			try{

				checkerService.checkStart(task,
						configurationService.getProps().getFilterMinPourcent(),
						configurationService.getProps().getFilterTypeCourse(),
						configurationService.getProps().getFilterListNbPartants(),
						configurationService.getProps().getFilterNbReunionMax());

				if( task.getStatus() == ScheduleStatus.RESCHEDULED ){
					logger.debug("RESCHEDULED task for course : {}", task.getIdCourse());
					List<Course> byCourseID = courseRepository.findByCourseID(task.getIdCourse());
					if( !byCourseID.isEmpty() ){
						Course c = byCourseID.get(0);
						LocalDateTime target = getTargetFromCourse(c, timeBefore);

						logger.debug("new target : {}", target);

						task.setStatus(ScheduleStatus.SCHEDULED);
						task.setPlannedExecution(target);

						String courseDescription = "⚠ Départ Décalé à "+c.getHeures()+"h"+c.getMinutes()+"\n"
								+task.getCourseDescription();
						task.setCourseDescription(courseDescription);
						taskRepository.save(task);

					}else{
						logger.warn("No course founded for id : {}", task.getIdCourse());
					}
				}
			}catch (Exception e){
                logger.error("Error during task execution : {}", e.getMessage());
				task.setStatus(ScheduleStatus.ERROR);
				task.setErrorMessage(e.getMessage());
				taskRepository.save(task);
			}
		}

		// Si plus vieux que timeBefore => status = MISSED
		List<ScheduledTask> oldScheduledTasks = getOldScheduledTasks();
		for (ScheduledTask task : oldScheduledTasks){
			task.setStatus(ScheduleStatus.MISSED);
		}
		if( !oldScheduledTasks.isEmpty() )
			logger.debug("Founded {} missed task(s)", oldScheduledTasks.size());

		scheduledTasks.addAll(oldScheduledTasks);
		for (ScheduledTask task : scheduledTasks){
			taskRepository.updateStatus(task.getId(), task.getStatus(), task.getLastExecution(), task.getErrorMessage());
		}


	}

	@Scheduled(cron = "${fr.ses10doigts.crawler.dailyMorningTask}") // Tous les jours à 9h00 du matin (en fonction de la property)
	public void everyMorning() {
		logger.info("Starting Daily Morning Crawl");
		String toDay = dayDate();
		launchMainScheduledCrawl(toDay, toDay, false, TELEGRAM_GROUP);

	}

	@Scheduled(cron = "${fr.ses10doigts.crawler.dailyEveningTask}") // Tous les jours à 22h00 du matin (en fonction de la property)
	public void everyEvening() {
		logger.info("Starting Daily Evening task");

		List<Paris> unendedBet = betService.getUnendedBet();
		for(Paris bet : unendedBet){
			checkerService.checkEnd(bet.getCourse().getCourseID(), TELEGRAM_GROUP);
		}

		CompletableFuture<GlobalBilanParis> bilanFuture = bilanService.computeGlobalBilan();
		bilanFuture.thenAccept(bilan -> telegramService.sendMessage(TELEGRAM_GROUP, bilan.toString()) );
	}



	@PostConstruct
	public void runAtStartup() {
		logger.debug("======================= ScheduledJobs");
		List<ScheduledTask> scheduled = taskRepository.findScheduledTasksFromNow(ScheduleStatus.SCHEDULED, LocalDateTime.now());

		for ( ScheduledTask task : scheduled ){
			logger.debug(" - "+task.getPlannedExecution()+" : "+task.getCourseUrl());
		}

		List<ScheduledTask> oldScheduledTasks = taskRepository.findOldScheduledTasks(ScheduleStatus.SCHEDULED, LocalDateTime.now());
		if( !oldScheduledTasks.isEmpty() ) {
			logger.debug("/!\\ Missed tasks : ");
			for (ScheduledTask task : oldScheduledTasks) {
				logger.debug(" - " + task.getPlannedExecution() + " : " + task.getCourseUrl());
				taskRepository.updateStatus(task.getId(), ScheduleStatus.MISSED, null, "Too late");
			}
		}

		logger.debug("=======================================");
	}

	public void launchMainScheduledCrawl(String startDay, String endDay, boolean startAndStop, Long chatId){

		crawlService.datesCrawl(startDay, endDay);
		manageEndOfCrawl(crawlService.getTreatment(), chatId, startDay, endDay);

		if (configurationService.getConfiguration().getTelegramVerbose().equals(Verbose.HIGH)) {
			telegramService.sendMessage(chatId, "Crawl du " + startDay + (startDay.equals(endDay) ? "" : " au " + endDay)
					+ " lancé. Résultat dans env. 5-10 min.");
		}

		if (startAndStop)
			crawlService.stopCurrentCrawl();
	}

	private void manageEndOfCrawl( Thread t, long messageId, String startDate,
			String endDate) {

		if (t == null) {
			logger.error("Crawl Thread is null...");
			return;
		}

		logger.info("Props : \n- Min partants: {}\n- Type course: {}\n- Reu max: {}\n- Min %: {}\n- Nb Partants: {}\n- List nb partants: {}\n",
				configurationService.getProps().getFilterMinPartants(),
				configurationService.getProps().getFilterTypeCourse(),
				configurationService.getProps().getFilterNbReunionMax(),
				configurationService.getProps().getFilterMinPourcent(),
				configurationService.getProps().getFilterNbPartants(),
				configurationService.getProps().getFilterListNbPartants());

		// Lancement async pour attendre la fin du crawl
		CompletableFuture.runAsync(() -> {
			try {
				t.join();

			} catch (Exception e) {
				logger.error("Error during Crawl waiting... Stopping main Survey...");
				// TODO see for relaunch? Scheduled
				return;
			}

			DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
			DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

			LocalDate start = LocalDate.parse(startDate, inputFormat);
			LocalDate end = LocalDate.parse(endDate, inputFormat);

			// pour toutes les dates envoyées
			while (!start.isAfter(end)) {
				// Récupération du crawl en BDD avec la date
				String day = start.format(outputFormat);
				start = start.plusDays(1);

				List<Course> courses = courseRepository.findCoursesWithCriteria(
						configurationService.getProps().getFilterMinPartants(),
						configurationService.getProps().getFilterTypeCourse(),
						configurationService.getProps().getFilterNbReunionMax(),
						day
				);

				courses.sort(
						Comparator
								.comparingInt((Course c) -> Integer.parseInt(c.getHeures()))
								.thenComparingInt(c -> Integer.parseInt(c.getMinutes()))
				);

				int nbScheduled = 0;
				StringBuilder rep = new StringBuilder();
				List<Course> retainedCourses = new ArrayList<>();
				for (Course course : courses) {

					boolean inStats = configurationService.getProps().getFilterTypeCourse().equalsIgnoreCase(course.getType())
                            && course.getReunion() <= configurationService.getProps().getFilterNbReunionMax()
							&& !(
								course.getHippodrome().contains("(")
								|| course.getHippodrome().contains(")")
								|| course.getHippodrome().contains("[")
								|| course.getHippodrome().contains("]")
								|| course.getHippodrome().toLowerCase().contains("fontainebleau")
								|| course.getHippodrome().toLowerCase().contains("marseille")
								|| course.getHippodrome().toLowerCase().contains("longchamp")
								|| course.getHippodrome().toLowerCase().contains("pau")
								|| course.getHippodrome().toLowerCase().contains("strasbourg")
							);
					if( course.getHippodrome().contains("(")
							|| course.getHippodrome().contains(")")
							|| course.getHippodrome().contains("[")
							|| course.getHippodrome().contains("]") ){
						logger.warn("Current course remove because seems to not be in France : {}", course.getHippodrome());
					}

					if( course.getHippodrome().toLowerCase().contains("fontainebleau")
							|| course.getHippodrome().toLowerCase().contains("marseille")
							|| course.getHippodrome().toLowerCase().contains("longchamp")
							|| course.getHippodrome().toLowerCase().contains("pau")
							|| course.getHippodrome().toLowerCase().contains("strasbourg") ){
						logger.warn("Current course remove because unwanted hippo : {}", course.getHippodrome());
					}

					if( !inStats ){
						continue;
					}

					LocalDateTime targetTime = getTargetFromCourse(course, timeBefore);

					String courseDescr = "\uD83C\uDFC7 " + course.getHippodrome() + ", R:" + course.getReunion()
							+ ", C:" + course.getCourse() + " à " + course.getHeures() + "h" + course.getMinutes() + "\n"
							+ "\uD83D\uDD17 [Lien Partants]("+course.getUrl() + ")\n";

					// Si l'heure de la course est déjà passée aujourd'hui, Passe à la course suivante
					if (targetTime.plusMinutes(timeBefore).isBefore(LocalDateTime.now())) {
                        logger.debug("Course {} déjà passée...", course.getUrl());
						courseDescr += "❌ Déjà passée...\n";
						rep.append( courseDescr ).append( "\n" );
						continue;

					} else {
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
						String result = targetTime.format(formatter);
						rep.append(courseDescr);
						rep.append("⏰ Check lancé à ").append(result).append("\n\n");
					}

					scheduleTask(targetTime, course, messageId, courseDescr);
					nbScheduled++;
					retainedCourses.add(course);
				}

				logger.debug("Après le crawl, {} courses trouvées en BDD avec nbPartant {}, type '{}', Reu max {}.",
						nbScheduled,
						configurationService.getProps().getFilterNbPartants(),
						configurationService.getProps().getFilterTypeCourse(),
						configurationService.getProps().getFilterNbReunionMax()
				);

				StringBuilder finalRep = new StringBuilder();
				finalRep.append("\uD83D\uDCC6").append( day ).append(" : \n");
				if( retainedCourses.isEmpty() ){
					finalRep.append( "❌ Pas de courses répondant aux critères aujourd'hui !" );
				}else {
					finalRep.append("✅ ").append( nbScheduled ).append(" course(s) aujourd'hui !" );
				}
				finalRep.append("\nCritères :\n" )
						.append("✔️ Partants min : ").append( configurationService.getProps().getFilterMinPartants() ).append( "\n" )
						.append("✔️ Type course : ").append( configurationService.getProps().getFilterTypeCourse() ).append( "\n" )
						.append("✔️ Réunion max : ").append( configurationService.getProps().getFilterNbReunionMax() ).append( "\n\n" )
						.append(rep);
				logger.info("{} New tasks scheduled.", nbScheduled);

				try {
                    logger.debug("Send message : {}", finalRep);
					telegramService.sendMessage(messageId, finalRep.toString());

				} catch (Exception e) {
                    logger.error("Error sending Telegram : {}\n-------------\nException message : {}", finalRep, e.getMessage());
				}
			}


			logger.info("✅ Main Crawl is Done! Now waiting for task scheduling execution");
		});
	}

	@NotNull
	private LocalDateTime getTargetFromCourse( Course course, int minusMinute ) {
		// Calculer l'heure de la course
		int hour = Integer.parseInt(course.getHeures());
		int minute = Integer.parseInt(course.getMinutes());

		LocalDate date = LocalDate.parse(course.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		LocalTime time = LocalTime.of(hour, minute, 0);

        return LocalDateTime.of(date, time).minusMinutes(minusMinute);
	}

	private void scheduleTask(LocalDateTime date, Course course, Long telegramMessageId,
			String courseDescription)  {

		// Créer ScheduledTask
		ScheduledTask task = new ScheduledTask();

		Optional<ScheduledTask> byCourseUrl = taskRepository.findByCourseUrl(course.getUrl());
		if (byCourseUrl.isPresent()) {
            logger.debug("Modifying already planned task: {}", course.getUrl());
			task.setId(byCourseUrl.get().getId());
		}else{
			logger.debug("Planning task for {}", course.getUrl());
		}

		task.setName(course.getUrl());
		task.setIdCourse(course.getCourseID());
		task.setCourse(course);
		task.setCourseStart(getTargetFromCourse(course, 0));
		task.setPlannedExecution(date);
		task.setStatus(ScheduleStatus.SCHEDULED);
		if( task.getId() == null )
			task.setCreationDate(LocalDateTime.now());
		task.setCourseUrl(course.getUrl());
		task.setTelegramMessageId(telegramMessageId);
		task.setCourseDescription(courseDescription);

		logger.debug("Saving it to DB");
		logger.debug(task.toString());
		// Sauvegarder en base
		ScheduledTask savedTask = taskRepository.save(task);

	}

	private String dayDate() {
		LocalDate day = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		return day.format(formatter);
	}

	private void changeScheduledTaskTiming(int tb) {
		List<ScheduledTask> scheduledTasksFromNow = taskRepository.findScheduledTasksFromNow(ScheduleStatus.SCHEDULED, LocalDateTime.now());
		for( ScheduledTask task : scheduledTasksFromNow ){
			task.setPlannedExecution( task.getCourseStart().minusMinutes(tb) );
			taskRepository.save(task);
		}
		logger.info("{} Scheduled task updated", scheduledTasksFromNow.size());
	}

	private List<ScheduledTask> getOldScheduledTasks() {
		LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeBefore);
		return taskRepository.findOldScheduledTasks(ScheduleStatus.SCHEDULED, threshold);
	}

	public List<ScheduledTask> getScheduledTasks() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime from = now.minusMinutes(timeBefore);
		return taskRepository.findScheduledTasksWithinWindow(ScheduleStatus.SCHEDULED, from, now);
	}

	public List<ScheduledTask> getFutureScheduledTasks() {
		LocalDateTime now = LocalDateTime.now();
		return taskRepository.findScheduledTasksFromNow(ScheduleStatus.SCHEDULED, now);
	}

}
