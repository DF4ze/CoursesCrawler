package fr.ses10doigts.coursesCrawler;

import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.model.telegram.Verbose;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "fr.ses10doigts.crawler")
public class CustomProperties {

    private static final Logger logger = LoggerFactory.getLogger(CustomProperties.class);

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
	private String startRefactoDate;
	private String endRefactoDate;

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
        logger.info("=== CustomProperties ===");
        logger.info("botToken = {}", "***");
        logger.info("botUsername = {}", botUsername);
        logger.info("telegramVerbose = {}", telegramVerbose);
        logger.info("telegramChatIds = {}", telegramChatIds);
        logger.info("puppeteerActivated = {}", puppeteerActivated);
        logger.info("puppeteerUser = {}", puppeteerUser);
        logger.info("puppeteerPwd = {}", "***");
        logger.info("puppeteerPath = {}", puppeteerPath);
        logger.info("puppeteerInitialBet = {}", puppeteerInitialBet);
        logger.info("puppeteerMartingale = {}", puppeteerMartingale);
        logger.info("filterMinPartants = {}", filterMinPartants);
        logger.info("filterMinPourcent = {}", filterMinPourcent);
        logger.info("filterNbPartants = {}", filterNbPartants);
        logger.info("filterTypeCourse = {}", filterTypeCourse);
        logger.info("filterNbReunionMax = {}", filterNbReunionMax);
        logger.info("filterListNbPartants = {}", filterListNbPartants);
        logger.info("filterListRejectedHippo = {}", filterListRejectedHippo);
        logger.info("========================");
    }
}
