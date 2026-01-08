package fr.ses10doigts.coursesCrawler;

import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.model.telegram.Verbose;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

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
	private boolean waitOnRetry = false;
	private Date startRefactoDate;
	private Date endRefactoDate;

	private String botToken;
	private String botUsername;

    private Verbose telegramVerbose;

    private String puppeteerActivated;
    private String puppeteerUser;
    private String puppeteerPwd;
    private String puppeteerPath;
    private String puppeteerInitialBet;

    @PostConstruct
    public void printProperties() {
        logger.info("=== CustomProperties ===");
        logger.info("botToken = {}", "***");
        logger.info("botUsername = {}", botUsername);
        logger.info("telegramVerbose = {}", telegramVerbose);
        logger.info("puppeteerActivated = {}", puppeteerActivated);
        logger.info("puppeteerUser = {}", puppeteerUser);
        logger.info("puppeteerPwd = {}", "***");
        logger.info("puppeteerPath = {}", puppeteerPath);
        logger.info("puppeteerInitialBet = {}", puppeteerInitialBet);
        logger.info("========================");
    }
}
