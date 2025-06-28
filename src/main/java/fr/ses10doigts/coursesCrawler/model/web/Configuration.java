package fr.ses10doigts.coursesCrawler.model.web;

import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
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

    private String	startGenDate;
    private String	endGenDate;

	private String startRefactoDate;
	private String endRefactoDate;

	private String botToken;
	private String botUsername;
}
