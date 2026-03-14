package fr.ses10doigts.coursesCrawler.service.scheduler;

import fr.ses10doigts.coursesCrawler.model.Configuration;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.service.crawl.CrawlService;
import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;
import fr.ses10doigts.coursesCrawler.service.web.TelegramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Service
@Profile({ "prod" })
@Slf4j
public class CrawlScheduledService {
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private CrawlService crawlService;
    @Autowired
    private TelegramService telegramService;

    @Scheduled(cron = "${fr.ses10doigts.crawler.monthlyTask}") // Tous les mois (en fonction de la property)
    public void everyFirstOfMonth(){
        LocalDate now = LocalDate.now();

        LocalDate firstDayPrevMonth = now.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayPrevMonth  = now.withDayOfMonth(1).minusDays(1);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String start = firstDayPrevMonth.format(fmt);
        String end   = lastDayPrevMonth.format(fmt);

        String urls = configurationService.generateUrlFromDates(start, end);

        Configuration conf = new Configuration();
        conf.setAgressivity(Agressivity.MEDIUM);
        conf.setAuthorized("/cotes\narrivee-et-rapports-pmu\npartants-pmu\n");
        conf.setLaunchCrawl(true);
        conf.setLaunchRefacto(true);
        conf.setLaunchExcel(true);
        conf.setLaunchArchive(true);
        conf.setStartGenDate(start);
        conf.setEndGenDate(end);
        conf.setMaxHop(1);
        conf.setMaxRetry(3);
        conf.setWaitOnRetry(false);
        conf.setTxtSeeds(urls);
        configurationService.saveConfiguration(conf);

        log.debug("Starting scheduled crawl");
        telegramService.sendMessage(configurationService.getProps().getTelegramChatId(), "Crawl du mois lancé");

        crawlService.crawlFromConfig(false);

        Thread crawlThread = crawlService.getTreatment();

        CompletableFuture.runAsync(() -> {
            if (crawlThread == null) {
                log.warn("Unable to send end notification: crawl thread is null");
                return;
            }

            try {
                log.debug("Scheduled crawl launched, waiting for thread completion...");
                crawlThread.join();

                log.debug("End scheduled crawl");
                telegramService.sendMessage(configurationService.getProps().getTelegramChatId(), "Crawl du mois terminé");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for scheduled crawl end", e);
            } catch (Exception e) {
                log.error("Error while sending scheduled crawl end notification: {}", e.getMessage());
            }
        });
    }
}
