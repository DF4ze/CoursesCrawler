package fr.ses10doigts.coursesCrawler.service.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


import fr.ses10doigts.coursesCrawler.model.paris.Paris;
import fr.ses10doigts.coursesCrawler.model.telegram.Verbose;
import fr.ses10doigts.coursesCrawler.service.bet.BetService;
import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;
import fr.ses10doigts.coursesCrawler.service.web.TelegramService;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.Course;
import fr.ses10doigts.coursesCrawler.model.schedule.ScheduledTask;
import fr.ses10doigts.coursesCrawler.model.schedule.ScheduleStatus;
import fr.ses10doigts.coursesCrawler.repository.ScheduledTaskRepository;
import fr.ses10doigts.coursesCrawler.repository.course.CourseRepository;
import fr.ses10doigts.coursesCrawler.service.crawl.CrawlService;
import jakarta.annotation.PostConstruct;

@Service
@Profile({ "dev", "telegram" })
public class SchedulerService {
	@Getter
    private int timeBefore = 20;
	public void setTimeBefore( int tb ){
		timeBefore = tb;
		changeScheduledTaskTiming( tb );
	}

	@Getter  @Setter
    private static int nbPartantMin = 14;
	@Getter @Setter
	private static int nbPartantMax = 16;
	@Getter  @Setter
    private static String typeCourse = "plat";
	@Getter  @Setter
	private static float pourcentFavoris = 41;
	@Getter  @Setter
	private static int nbReunionMax = 5;

	private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

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
					if (paris != null)
						checkerService.checkEnd(paris.getCourse().getCourseID(), task.getTelegramMessageId());
				} catch (Exception e) {
					logger.error("Error during ScheduledTask : {}", e.getMessage());
				}
			}

			try{
				checkerService.checkStart(task, pourcentFavoris, typeCourse, nbPartantMin, nbPartantMax, nbReunionMax);

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

	@Scheduled(cron = "${fr.ses10doigts.crawler.dailyTask}") // Tous les jours à 9h00 du matin (en fonction de la property)
	public void everyMorning() {
		logger.info("Starting Daily Crawl");
		String toDay = dayDate();
		try {
			launchMainScheduledCrawl(toDay, toDay, false, -4706435457L);
		} catch (TelegramApiException e) {
            logger.error("Error while launching daily crawl : {}", e.getMessage());
		}
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

	public void launchMainScheduledCrawl(String startDay, String endDay, boolean startAndStop, Long chatId)
			throws TelegramApiException {

		crawlService.launchSurveyCrawl(startDay, endDay);

		manageEndOfCrawl(crawlService.getTreatment(), chatId, startDay, endDay);

		try {
			if (configurationService.getConfiguration().getTelegramVerbose().equals(Verbose.HIGH)) {
				telegramService.sendMessage(chatId, "Crawl du " + startDay + (startDay.equals(endDay) ? "" : " au " + endDay)
						+ " lancé. Résultat dans env. 5-10 min.");
			}
		}catch (Exception e){
			logger.warn("Error sending Telegram message... Daily crawl is done! {}", e.getMessage());
		}

		if (startAndStop)
			crawlService.stopCurrentCrawl();
	}

	private void manageEndOfCrawl( Thread t, long messageId, String startDate,
			String endDate) {

		if (t == null) {
			logger.warn("Crawl Thread is null...");
			return;
		}

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
						nbPartantMin, typeCourse, nbReunionMax, day
				);

				int nbScheduled = 0;
				StringBuilder rep = new StringBuilder();
				for (Course course : courses) {

					boolean inStats = typeCourse.equalsIgnoreCase(course.getType())
                            && course.getReunion() <= nbReunionMax
							&& !(
								course.getHippodrome().contains("(")
								|| course.getHippodrome().contains(")")
								|| course.getHippodrome().contains("[")
								|| course.getHippodrome().contains("]")
							);
					if( course.getHippodrome().contains("(")
							|| course.getHippodrome().contains(")")
							|| course.getHippodrome().contains("[")
							|| course.getHippodrome().contains("]") ){
						logger.warn("Current course remove because seems to not be in France : {}", course.getHippodrome());
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
				}

				logger.debug("Après le crawl, {} courses trouvées en BDD avec nbPartant {}, type '{}', Reu max {}.",
						nbScheduled, nbPartantMin, typeCourse, nbReunionMax
				);

				StringBuilder finalRep = new StringBuilder();
				finalRep.append("\uD83D\uDCC6").append( day ).append(" : \n");
				if( courses.isEmpty() ){
					finalRep.append( "❌ Pas de courses répondant aux critères aujourd'hui !" );
				}else {
					finalRep.append("✅ ").append( nbScheduled ).append(" course(s) aujourd'hui !" );
				}
				finalRep.append("\nCritères :\n" )
						.append("✔️ Partants min : ").append( nbPartantMin ).append( "\n" )
						.append("✔️ Type course : ").append( typeCourse ).append( "\n" )
						.append("✔️ Réunion max : ").append( nbReunionMax ).append( "\n\n" )
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
