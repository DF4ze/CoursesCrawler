package fr.ses10doigts.coursesCrawler.service.scheduler;

import fr.ses10doigts.coursesCrawler.model.Configuration;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.service.crawl.CrawlService;
import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Profile({ "prod" })
public class CrawlScheduledService {
    private static final Logger logger = LoggerFactory.getLogger(CrawlScheduledService.class);

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private CrawlService crawlService;

    @Scheduled(cron = "${fr.ses10doigts.crawler.monthlyTask}") // Tous les mois (en fonction de la property)
    public void everyFirstOfMonth(){
        LocalDate now = LocalDate.now();

        LocalDate firstDayPrevMonth = now.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayPrevMonth  = now.withDayOfMonth(1).minusDays(1);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");;

        String start = firstDayPrevMonth.format(fmt);
        String end   = lastDayPrevMonth.format(fmt);

        String urls = configurationService.generateUrlFromDates(start, end);

        Configuration conf = new Configuration();
        conf.setAgressivity(Agressivity.MEDIUM);
        conf.setAuthorized("/cotes\narrivee-et-rapports-pmu\npartants-pmu\n");
        conf.setLaunchCrawl(true);
        conf.setLaunchRefacto(true);
        conf.setLaunchExcel(true);
        conf.setStartGenDate(start);
        conf.setEndGenDate(end);
        conf.setMaxHop(1);
        conf.setMaxRetry(3);
        conf.setWaitOnRetry(false);
        conf.setTxtSeeds(urls);
        configurationService.saveConfiguration(conf);
        logger.debug("Starting scheduled crawl");
        crawlService.crawlFromConfig(false);

    }
}
