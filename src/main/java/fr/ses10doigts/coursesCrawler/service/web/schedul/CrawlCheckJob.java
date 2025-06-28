package fr.ses10doigts.coursesCrawler.service.web.schedul;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Course;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.CourseComplete;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Partant;
import fr.ses10doigts.coursesCrawler.model.web.Configuration;
import fr.ses10doigts.coursesCrawler.model.web.Status;
import fr.ses10doigts.coursesCrawler.repository.ScheduledTaskRepository;
import fr.ses10doigts.coursesCrawler.repository.course.CourseRepository;
import fr.ses10doigts.coursesCrawler.repository.course.PartantRepository;
import fr.ses10doigts.coursesCrawler.service.crawl.CrawlService;
import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;
import fr.ses10doigts.coursesCrawler.service.web.TelegramService;

public class CrawlCheckJob implements Job {
    @Autowired
    private ScheduledTaskRepository taskRepository;
    @Autowired
	private ConfigurationService configurationService;
    @Autowired
	private CrawlService crawlService;
    @Autowired
    private PartantRepository partantRepository;
	@Autowired
	private CourseRepository courseRepo;
	@Autowired
	private TelegramService telegramService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        Long taskId = context.getJobDetail().getJobDataMap().getLong("taskId");
		Long messageId = context.getJobDetail().getJobDataMap().getLong("telegramMessageId");
		String courseUrl = context.getJobDetail().getJobDataMap().getString("courseUrl");
		String courseDescription = context.getJobDetail().getJobDataMap().getString("courseDescription");
        try {
			// Lancer un crawl de la page uniquement
			Configuration conf = new Configuration();
			conf.setAgressivity(Agressivity.MEDIUM_HARD);
			conf.setAuthorized("partants-pmu\r\n");
			conf.setLaunchCrawl(true);
			conf.setLaunchRefacto(false);
			conf.setMaxHop(0);
			conf.setTxtSeeds(courseUrl);
			configurationService.saveConfiguration(conf);
			crawlService.manageLaunch();

			// Attendre sa fin
			Thread crawlThread = crawlService.getTreatment();
			crawlThread.join();

			// Récup du crawl
			Long codeCourse = extractCode(courseUrl);
			Set<Partant> partants = partantRepository.findByCourseID(codeCourse);

			// Calcul stats
			CourseComplete courseStats = getCourseStats(partants);

			String stats = "\nNb Geny < 5 : " + courseStats.getNombreChevauxInfCinqProbableGeny() + "\nNb PMU < 5 : "
					+ courseStats.getNombreChevauxInfCinqProbablePMU() + "\nDépart : " + courseStats.getAutoStart()
					+ "\nType : " + courseStats.getTypeCourse();

			telegramService.sendMessage(messageId,
					"Regarder la course suivante :\n" + courseDescription + "\n" + stats);


            // tracer OK
            taskRepository.updateStatus(taskId, Status.SUCCESS, LocalDateTime.now(), null);
        } catch (Exception e) {
            // tracer ERREUR
            taskRepository.updateStatus(taskId, Status.ERROR, LocalDateTime.now(), e.getMessage());
            throw new JobExecutionException(e);
        }
    }


	private Long extractCode(String url) {
		Pattern pattern = Pattern.compile("_c(\\d+)$");
		Matcher matcher = pattern.matcher(url);

		Long found = null;
		if (matcher.find()) {
			String code = matcher.group(1);
			found = Long.parseLong(code);
		}

		return found;
	}

	private CourseComplete getCourseStats(Set<Partant> partants) {
		CourseComplete cc = new CourseComplete();
		Course course = null;
		boolean isFirst = true;

		int nbInf5PMU = 0;
		int nbInf5GENY = 0;

		for (Partant partant : partants) {
			if (isFirst) {
				List<Course> byCourseID = courseRepo.findByCourseID(partant.getCourseID());
				if (byCourseID.size() == 1) {
					course = byCourseID.get(0);
				}
			}
			if (partant.getProbableGeny() != null)
				nbInf5GENY += partant.getProbableGeny() < 5 ? 1 : 0;

			if (partant.getProbablePMU() != null)
				nbInf5PMU += partant.getProbablePMU() < 5 ? 1 : 0;
		}

		cc.setNombreChevauxInfCinqProbableGeny(nbInf5GENY);
		cc.setNombreChevauxInfCinqProbablePMU(nbInf5PMU);
		cc.setAutoStart(course.getDepart());
		cc.setTypeCourse(course.getType());
		// TODO plus si besoin...

		return cc;
	}

}