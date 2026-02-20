package fr.ses10doigts.coursesCrawler;

import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;
import fr.ses10doigts.coursesCrawler.service.web.TelegramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;

@EnableScheduling
@SpringBootApplication
@Slf4j
public class CoursesCrawlerApplication {
	private static Environment environment;

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(CoursesCrawlerApplication.class, args);

		// Check Telegram Webhook
		ConfigurationService confService = context.getBean(ConfigurationService.class);
		environment = context.getBean(Environment.class);
		boolean isDevOrTelegram = isProfileActive("dev") || isProfileActive("telegram");

		if( isDevOrTelegram ) {
			// Appel de la méthode
			boolean webhookActif = TelegramService.isWebhookActive(confService.getConfiguration().getBotToken());
			if (webhookActif) {
				log.error("Cannot launch App due to already opened Telegram Bot Webhook ");
				System.exit(0);
			}
		}

	}

	private static boolean isProfileActive(String profile) {
		for (String activeProfile : environment.getActiveProfiles()) {
			if (activeProfile.equalsIgnoreCase(profile)) {
				return true;
			}
		}
		return false;
	}
}
