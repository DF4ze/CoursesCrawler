package fr.ses10doigts.coursesCrawler.service.crawl;

import fr.ses10doigts.coursesCrawler.CustomProperties;
import fr.ses10doigts.coursesCrawler.model.crawl.Page;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.repository.web.WebCrawlingProxy;
import fr.ses10doigts.coursesCrawler.service.archive.ArchiveService;
import fr.ses10doigts.coursesCrawler.service.crawl.tool.CrawlReport;
import fr.ses10doigts.coursesCrawler.service.crawl.tool.PageTool;
import fr.ses10doigts.coursesCrawler.service.scrap.VisitorParseAndStore;
import fr.ses10doigts.coursesCrawler.service.scrap.tool.Chrono;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProcessorChain implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ProcessorChain.class);

	@Autowired
	protected PageTool pageTool;
	@Autowired
	protected WebCrawlingProxy webRepo;
	@Autowired
	protected VisitorParseAndStore parseAndStore;
	@Autowired
	protected ArchiveService archiveService;
	@Autowired
	private CustomProperties props;

	@Setter
	protected List<String> seeds;
	@Setter
	protected int maxHop;
	@Setter
	protected boolean withException = false;
	@Setter
	protected boolean waitOnRetry = true;
	@Setter
	protected boolean doArchive = false;
	@Setter
	protected Agressivity agressivity;
	@Getter
	protected CrawlReport report = CrawlReport.getInstance();

	protected boolean running = true;
	protected List<String> authorized;
	protected Queue<Page> enQueued;
	protected boolean isFirst = true;



	@Override
	public void run() {
        Chrono chrono = new Chrono();
		chrono.pick();
		if (seeds.isEmpty()) {
			throw new RuntimeException("Seeds is empty!");
		}

		// Initialisation
		enQueued = new LinkedList<>(pageTool.url2Pages(seeds));
		report.setSeeds(seeds);

		// Informations
		StringBuilder auth = new StringBuilder();
		for (String string : authorized) {
			auth.append(string).append(", ");
		}

		logger.info("Crawl Start with parameters :\n - {} seed(s)\n- maxHop : {}\n- authorized : {}\n- agressivity : {}"
				,enQueued.size(),maxHop,auth,agressivity);

		do {
			for (Page page = null; (page = enQueued.poll()) != null;) {

				// Retrieve inner-links
				downloadAndSeek(page);
			}

		} while (!enQueued.isEmpty());

		// Report

		logger.info("=========================");
		logger.info("Crawl ended");
        logger.info("Crawled {}/{} pages in {}", report.getSuccessCrawled(), report.size(), chrono.compareToHour());
		report.setTime(chrono.compare());

	}

	protected void downloadAndSeek(Page page) {
		List<Page> newPages = new ArrayList<>();

		report.startCrawl(page.getUrl());

		// Wait for a user-friendly crawl
		if (!isFirst) {
			sleep(agressivity);
		} else {
			isFirst = false;
		}

		// soft way to stop thread
		if (!running) {
			logger.debug("running is false : stopping crawl thread");
			return;
		}

		try {
			// Download content
			String content = webRepo.getRawPage(page.getUrl());
			// String content = webRepo.getUrlContents(page.getUrl());

            logger.debug("Url downloaded : {}", page.getUrl());
			report.lastCrawledUrl(page.getUrl());

			if( doArchive ){
				archiveService.archive(page.getUrl(), content);
				logger.debug("Url archived : {}", page.getUrl());
			}

			// Parse content
			parseAndStore.indexify(page, content);

			// If maxhop not reached
			if (page.getHop() < maxHop) {
				// retrieve new URL to download
				newPages = pageTool.findUrlsInContent(page, content, authorized);
				report.addNewPages(newPages);

				// Download content and search for new URL
				for (Page newPage : newPages) {
					if( newPage.getUrl().equals( page.getUrl() ) ){
						continue;
					}

					if( getRunning() )
						downloadAndSeek(newPage);

					page.getUrlsContained().add(newPage.getUrl());

					// soft way to stop thread
					if (!running) {
						logger.debug("running is false : stopping crawl thread");
						break;
					}
				}
			} else {
				logger.debug("Max hop reached, no more research of inner URL  ");

			}
		} catch (Exception /*| RestClientException*/ e) {
            logger.error("Exception on page {}\n Message : {}", page.getUrl(), e.getMessage());
			report.errorCrawl(page.getUrl());
			setAsCrawlErrorPage(page);

			// Launching a connectivity test to know if we still have the Internet
			// ... and wait if not
			webRepo.connectivityTest();
		}

	}

	void setAsCrawlErrorPage(Page page) {

        logger.warn("Error crawling : {}", page.getUrl());
		if (page.getNbRetry() < props.getMaxRetry()) {
            logger.warn("Page is set to retry (nb retry: {}/max retry {})", page.getNbRetry(), props.getMaxRetry());
			page.setNbRetry(page.getNbRetry() + 1);
			enQueued.add(page);

			if (waitOnRetry) {
				logger.debug("waitOnRetry is set");
				sleep(agressivity);
			}

		} else {
            logger.warn("Reach max retry : {}/{}", page.getNbRetry(), props.getMaxRetry());
			if (withException) {
				throw new RuntimeException("Max retry reached on page " + page.getUrl());
			}
		}
	}

	void sleep(Agressivity ag) {
		try {
			int range = getRandomNumberInRange(ag.getMin(), ag.getMax());
			range = range * 1000;
            logger.debug("Thread will sleep {}ms", range);
			Thread.sleep(range);

		} catch (InterruptedException e) {
			logger.warn("Thread sleep caused an Exception");
		}
	}

	private static int getRandomNumberInRange(int min, int max) {

		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}

		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
	}

	public void askToStop() {
		running = false;
	}

	public void initialize() {
		running = true;
		isFirst = true;
		report = CrawlReport.getInstance();
	}


    public void setAuthorised(List<String> authorised) {
		this.authorized = authorised;
	}

    public boolean getRunning() {
		return running;
	}

    public void resetReport() {
		report = CrawlReport.getInstance();
	}

}
