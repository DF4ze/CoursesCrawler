package fr.ses10doigts.coursesCrawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CoursesCrawlerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoursesCrawlerApplication.class, args);

//		System.getProperties().put("proxySet", "true");
//		System.getProperties().put("socksProxyHost", "127.0.0.1");
//		System.getProperties().put("socksProxyPort", "9050");

		// Tor
//		System.getProperties().put("proxySet", "true");
//		System.getProperties().put("socksProxyHost", "192.168.1.104");
//		System.getProperties().put("socksProxyPort", "9050");
	}

}
