package fr.ses10doigts.coursesCrawler;

import java.util.Date;

import fr.ses10doigts.coursesCrawler.model.telegram.Verbose;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "fr.ses10doigts.crawler")
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
	private boolean waitOnRetry = false;
	private Date startRefactoDate;
	private Date endRefactoDate;

	private String botToken;
	private String botUsername;

    private Verbose telegramVerbose;
}
