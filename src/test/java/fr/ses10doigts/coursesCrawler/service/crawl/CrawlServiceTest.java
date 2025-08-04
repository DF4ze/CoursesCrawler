package fr.ses10doigts.coursesCrawler.service.crawl;

import fr.ses10doigts.coursesCrawler.model.Configuration;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CrawlServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(CrawlServiceTest.class);

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private CrawlService crawlService;


    @Test
    void manageLaunch() {
        String startDay = dayDate();
        String endDay = dayDate();

        logger.debug("DAYS : {} {}", startDay, endDay);
        String urls = "https://www.geny.com/partants-pmu/2025-07-10-chateaubriant-pmu-prix-claude-rouget_c1582902";//configurationService.generateUrlFromDates(startDay, endDay);
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

    private String dayDate() {
        LocalDate day = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return day.format(formatter);
    }
}