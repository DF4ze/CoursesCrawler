package fr.ses10doigts.coursesCrawler;

import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.model.telegram.Verbose;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Date;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "fr.ses10doigts.crawler")
@Slf4j
public class CustomProperties {
    // @Value("#{'${fr.ses10doigts.webapp.urls}'.split(',')}")
    // private List<String> urls;

    private Integer	maxHop;
    private String	seedsFile;
    private String	authorizedFile;
    private Integer	maxRetry;
    private Agressivity	agressivity;
    private boolean	doCrawl;
    private boolean	doRefacto;
    private boolean	doExcel;
    private boolean	doArchive = false;
	private boolean waitOnRetry = false;
	private Date startRefactoDate;
	private Date endRefactoDate;

	private String botToken;
	private String botUsername;

    private Verbose telegramVerbose;
    @Value("#{'${fr.ses10doigts.crawler.telegramChatIds}'.split(',')}")
    private List<Long> telegramChatIds;
    private Long telegramChatId;
    private Long telegramErrorChatId;

    private String puppeteerActivated;
    private String puppeteerUser;
    private String puppeteerPwd;
    private String puppeteerPath;
    private String puppeteerInitialBet;
    @Value("#{'${fr.ses10doigts.crawler.puppeteerMartingale}'.split(',')}")
    private List<Integer> puppeteerMartingale;

    private Integer filterMinPartants;
    private Float filterMinPourcent;
    private String filterNbPartants;
    private String filterTypeCourse;
    private Integer filterNbReunionMax;
    @Value("#{'${fr.ses10doigts.crawler.filterNbPartants}'.split(',')}")
    private List<Integer> filterListNbPartants;
    @Value("#{'${fr.ses10doigts.crawler.filterListRejectedHippo}'.split(',')}")
    private List<String> filterListRejectedHippo;

    @PostConstruct
    public void printProperties() {
        log.info("=== CustomProperties ===");
        log.info("botToken = {}", "***");
        log.info("botUsername = {}", botUsername);
        log.info("telegramVerbose = {}", telegramVerbose);
        log.info("telegramChatIds = {}", telegramChatIds);
        log.info("puppeteerActivated = {}", puppeteerActivated);
        log.info("puppeteerUser = {}", puppeteerUser);
        log.info("puppeteerPwd = {}", "***");
        log.info("puppeteerPath = {}", puppeteerPath);
        log.info("puppeteerInitialBet = {}", puppeteerInitialBet);
        log.info("puppeteerMartingale = {}", puppeteerMartingale);
        log.info("filterMinPartants = {}", filterMinPartants);
        log.info("filterMinPourcent = {}", filterMinPourcent);
        log.info("filterNbPartants = {}", filterNbPartants);
        log.info("filterTypeCourse = {}", filterTypeCourse);
        log.info("filterNbReunionMax = {}", filterNbReunionMax);
        log.info("filterListNbPartants = {}", filterListNbPartants);
        log.info("filterListRejectedHippo = {}", filterListRejectedHippo);
        log.info("========================");
    }
}
