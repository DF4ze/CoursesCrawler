package fr.ses10doigts.coursesCrawler.service.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile({ "prod" })
@Slf4j
public class CrawlScheduledService {
    @Autowired
    private MonthlyCrawlService monthlyCrawlService;

    @Scheduled(cron = "${fr.ses10doigts.crawler.monthlyTask}") // Tous les mois (en fonction de la property)
    public void everyFirstOfMonth(){
        monthlyCrawlService.runMonthlyCrawl(null);
    }
}
