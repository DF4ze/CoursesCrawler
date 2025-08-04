package fr.ses10doigts.coursesCrawler.model;

import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.model.telegram.Verbose;
import lombok.Data;

@Data
public class Configuration {

    private String	txtSeeds;
    private String	authorized;
    private int		maxHop;
    private int		maxRetry;
    private Agressivity	agressivity;
    private boolean	launchCrawl;
    private boolean	launchRefacto;
	private boolean waitOnRetry;

    private String	startGenDate;
    private String	endGenDate;

	private String startRefactoDate;
	private String endRefactoDate;

	private String botToken;
	private String botUsername;

    private Verbose telegramVerbose;
}
