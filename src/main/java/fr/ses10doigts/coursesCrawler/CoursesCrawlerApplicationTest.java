package fr.ses10doigts.coursesCrawler;

import fr.ses10doigts.coursesCrawler.model.Configuration;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.service.crawl.CrawlService;
import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;
import fr.ses10doigts.coursesCrawler.service.web.TelegramService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public class CoursesCrawlerApplicationTest {
	private static final Logger logger = LoggerFactory.getLogger(CoursesCrawlerApplicationTest.class);

	private static Environment environment;
	private static CrawlService crawlService;
	private static ConfigurationService configurationService;

	public static void mainAble(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(CoursesCrawlerApplicationTest.class, args);

		// Check Telegram Webhook
		crawlService = context.getBean(CrawlService.class);
		configurationService = context.getBean(ConfigurationService.class);
		environment = context.getBean(Environment.class);


		manageLaunch();

	}

	private static boolean isProfileActive(String profile) {
		for (String activeProfile : environment.getActiveProfiles()) {
			if (activeProfile.equalsIgnoreCase(profile)) {
				return true;
			}
		}
		return false;
	}

	static void manageLaunch() {
		String startDay = dayDate();
		String endDay = dayDate();

		logger.debug("DAYS : {} {}", startDay, endDay);
		String urls = "https://www.geny.com/partants-pmu?id_course=1582808&info=2025-07-09-Senonnes-Pouanc%c3%a9-pmu-Prix+Radio+Oxyg%c3%a8ne";//configurationService.generateUrlFromDates(startDay, endDay);
		logger.debug("URLS : {}", urls);

		Configuration conf = new Configuration();
		conf.setAgressivity(Agressivity.MEDIUM_HARD);
		conf.setAuthorized("partants-pmu\r\n");
		conf.setLaunchCrawl(true);
		conf.setLaunchRefacto(false);
		conf.setMaxRetry(10);
		conf.setWaitOnRetry(true);
		conf.setMaxHop(1);
		conf.setTxtSeeds(urls);
		configurationService.saveConfiguration(conf);

		crawlService.manageLaunch(true);

	}
	static private String dayDate() {
		LocalDate day = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		return day.format(formatter);
	}

}
