package fr.ses10doigts.coursesCrawler.service.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.Course;
import fr.ses10doigts.coursesCrawler.model.web.ScheduledTask;
import fr.ses10doigts.coursesCrawler.model.web.Status;
import fr.ses10doigts.coursesCrawler.repository.ScheduledTaskRepository;
import fr.ses10doigts.coursesCrawler.repository.course.CourseRepository;
import fr.ses10doigts.coursesCrawler.service.crawl.CrawlService;
import fr.ses10doigts.coursesCrawler.service.web.schedul.CrawlCheckJob;

@Service
public class TelegramService {
	private static int timeBefore = 25;
	private static int nbPartant = 15;

	private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);

	@Autowired
	private Environment environment;

	@Autowired
	private CourseRepository courseRepo;
	@Autowired
	private CrawlService crawlService;

	@Autowired
	private Scheduler scheduler;
	@Autowired
	private ScheduledTaskRepository taskRepository;

	private TelegramClient telegramClient = null;

	@Autowired
	public TelegramService(ConfigurationService configurationService) {
		telegramClient = new OkHttpTelegramClient(configurationService.getConfiguration().getBotToken());
		logger.info("Telegram service loaded");
	}

	public void launchMainScheduledCrawl(String startDay, String endDay, boolean startAndStop, Long chatId)
			throws TelegramApiException {

		crawlService.launchSurveyCrawl(startDay, endDay);

		manageEndOfCrawl(telegramClient, crawlService.getTreatment(), chatId, startDay, endDay);

		sendMessage(chatId, "Crawl du " + startDay + (startDay.equals(endDay) ? "" : " au " + endDay)
				+ " lancé. Résultat dans env. 5-10 min.");

		if (startAndStop)
			crawlService.stopCurrentCrawl();
	}

	public void manageEndOfCrawl(TelegramClient telegramClient, Thread t, long messageId, String startDate,
			String endDate) {
		if (t == null) {
			return;
		}

		this.telegramClient = telegramClient;

		// Lancement async pour attendre la fin du crawl
		CompletableFuture.runAsync(() -> {
			try {
				t.join();

				DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
				DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

				LocalDate start = LocalDate.parse(startDate, inputFormat);
				LocalDate end = LocalDate.parse(endDate, inputFormat);

				// pour toutes les dates envoyées
				while (!start.isAfter(end)) {
					// Récupération du crawl avec la date
					String day = start.format(outputFormat);
					start = start.plusDays(1);

					List<Course> courses = courseRepo.findCoursesWithMoreThanXPartantsOnDate(nbPartant, day);

					String rep = day + ": " + courses.size() + " courses > " + nbPartant + " partants :\n\n";
					int courseNb = 0;
					for (Course course : courses) {
						courseNb++;

						// Calculer l'heure de la course
						int hour = Integer.parseInt(course.getHeures());
						int minute = Integer.parseInt(course.getMinutes());

						LocalDate date = LocalDate.parse(course.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
						LocalTime time = LocalTime.of(hour, minute, 0);
						LocalDateTime targetTime = null;
						if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
							// ! \\ In dev mode : Target is every 30s
							targetTime = LocalDateTime.now().plusSeconds(courseNb * 30);
						} else {
							targetTime = LocalDateTime.of(date, time).minusMinutes(timeBefore);
						}

						// String message
						String courseDescr = "⚆ " + course.getHippodrome() + ", R:" + course.getReunion() + ", N°"
								+ course.getCourse() + " à " + course.getHeures() + "h" + course.getMinutes() + "\n"
								+ course.getUrl() + "\n";

						// Si l'heure cible est déjà passée aujourd'hui, Passe a la course suivante
						if (targetTime.isBefore(LocalDateTime.now())) {
							logger.debug("Course " + course.getUrl() + " déjà passée...");
							courseDescr += "❌ Déjà passée...\n";
							rep += courseDescr + "\n";
							continue;

						} else {
							DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm 'le' dd/MM/yyyy");
							String resultat = targetTime.format(formatter);
							courseDescr += "✅ Check lancé à " + resultat + "\n";
						}
						rep += courseDescr + "\n";

						logger.debug(rep);

						scheduleTask(targetTime, course.getUrl(), course.getUrl(), messageId, courseDescr);
						// Planifier le check < 30 min
						/*
						 * CompletableFuture.runAsync(() -> { try { Thread.sleep(delay); // Attendre
						 * jusqu'à l'heure cible
						 * 
						 * // Lancer un crawl de la page uniquement Configuration conf = new
						 * Configuration(); conf.setAgressivity(Agressivity.MEDIUM_HARD);
						 * conf.setAuthorized(// "www.geny.com/cotes\r\n" // + "arrivee-et-rapports\r\n"
						 * // + "partants-pmu\r\n"); conf.setLaunchCrawl(true);
						 * conf.setLaunchRefacto(false); conf.setMaxHop(0);
						 * conf.setTxtSeeds(course.getUrl());
						 * configurationService.saveConfiguration(conf); crawlService.manageLaunch();
						 * 
						 * // Attendre sa fin Thread crawlThread = crawlService.getTreatment();
						 * crawlThread.join();
						 * 
						 * // Récup du crawl Long codeCourse = extractCode(course.getUrl());
						 * Set<Partant> partants = partantRepository.findByCourseID(codeCourse);
						 * 
						 * // Calcul stats CourseComplete courseStats = getCourseStats(partants);
						 * 
						 * String stats = "\nNb Geny < 5 : " +
						 * courseStats.getNombreChevauxInfCinqProbableGeny() + "\nNb PMU < 5 : " +
						 * courseStats.getNombreChevauxInfCinqProbablePMU() + "\nDépart : " +
						 * courseStats.getAutoStart() + "\nType : " + courseStats.getTypeCourse();
						 * 
						 * sendMessage(messageId, "Regarder la course suivante : " + descr + "\n" +
						 * stats); } catch (InterruptedException | TelegramApiException e) {
						 * e.printStackTrace(); } });
						 */

					}

					logger.debug("Send message : " + rep);
					sendMessage(messageId, rep);
				}
			} catch (InterruptedException | TelegramApiException | SchedulerException e) {
				e.printStackTrace();
			}
		});
	}

	private void scheduleTask(LocalDateTime date, String name, String courseUrl, Long telegramMessageId,
			String courseDescription) throws SchedulerException {
		// 1. Convertir en cron
		String cron = convertToCron(date);

		// 2. Créer ScheduledTask
		ScheduledTask task = new ScheduledTask();
		task.setName(name);
		task.setCronExpression(cron);
		task.setStatus(Status.SCHEDULED);
		task.setCreationDate(LocalDateTime.now());
		task.setCourseUrl(courseUrl);
		task.setTelegramMessageId(telegramMessageId);
		task.setCourseDescription(courseDescription);

		// 3. Sauvegarder en base
		ScheduledTask savedTask = taskRepository.save(task);

		// 4. Planifier avec Quartz
		JobDetail job = JobBuilder.newJob(CrawlCheckJob.class).withIdentity(savedTask.getName(), "default")
				.usingJobData("taskId", savedTask.getId())
				.usingJobData("telegramMessageId", telegramMessageId).usingJobData("courseUrl", courseUrl)
				.usingJobData("courseDescription", courseDescription)
				.build();

		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger-" + savedTask.getId(), "default")
				.withSchedule(CronScheduleBuilder.cronSchedule(cron)).build();

		scheduler.scheduleJob(job, trigger);
	}

	private String convertToCron(LocalDateTime dateTime) {
		int second = dateTime.getSecond();
		int minute = dateTime.getMinute();
		int hour = dateTime.getHour();
		int day = dateTime.getDayOfMonth();
		int month = dateTime.getMonthValue();
		int year = dateTime.getYear();

		// ⚠️ Quartz ne supporte pas l'année dans le cron de base
		// Mais on peut ajouter une clause d'arrêt dans MyJob si la date est passée

		// Format Quartz : second minute hour day month dayOfWeek (année facultative)
		return String.format("%d %d %d %d %d ? %d", second, minute, hour, day, month, year);
	}

	public void sendMessage(Long chatId, String text) throws TelegramApiException {
		SendMessage sendMessage = new SendMessage(chatId + "", text);
		telegramClient.execute(sendMessage);
	}



	public static int getTimeBefore() {
		return timeBefore;
	}

	public static void setTimeBefore(int timeBefore) {
		TelegramService.timeBefore = timeBefore;
	}

	public static int getNbPartant() {
		return nbPartant;
	}

	public static void setNbPartant(int nbPartant) {
		TelegramService.nbPartant = nbPartant;
	}

}
