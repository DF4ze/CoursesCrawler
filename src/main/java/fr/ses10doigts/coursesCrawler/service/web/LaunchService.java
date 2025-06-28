package fr.ses10doigts.coursesCrawler.service.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.ses10doigts.coursesCrawler.model.crawl.Report;
import fr.ses10doigts.coursesCrawler.service.crawl.CrawlService;
import fr.ses10doigts.coursesCrawler.service.scrap.RefactorerService;

@Component
public class LaunchService {

	@Autowired
	private ConfigurationService conf;
	@Autowired
	private CrawlService crawl;
	@Autowired
	private RefactorerService refacto;

	private Thread t = null;

	public Report manageLaunch() {
		return crawl.manageLaunch();
	}

	public Thread getCrawlThread() {
		return t;
	}

	public Thread getRefactoThread() {
		return refacto.getThread();
	}

}
