package fr.ses10doigts.coursesCrawler.service.crawl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import fr.ses10doigts.coursesCrawler.CustomProperties;
import fr.ses10doigts.coursesCrawler.model.crawl.Page;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.repository.web.WebCrawlingProxy;
import fr.ses10doigts.coursesCrawler.service.crawl.tool.CrawlReport;
import fr.ses10doigts.coursesCrawler.service.crawl.tool.PageTool;
import fr.ses10doigts.coursesCrawler.service.scrap.HtmlVisitor;
import fr.ses10doigts.coursesCrawler.service.scrap.tool.Chrono;

@Service
public class ProcessorChain implements Runnable {

	@Autowired
	private PageTool pageTool;
	@Autowired
	private WebCrawlingProxy webRepo;
	@Autowired
	private HtmlVisitor htmlVisitor;
	@Autowired
	private CustomProperties props;

	private List<String> seeds;
	private int maxHop;
	private List<String> authorized;
	private Agressivity agressivity;
	private boolean running = true;
	private Queue<Page> enQueued;
	private CrawlReport report = CrawlReport.getInstance();
	private Chrono chrono;

	private boolean isFirst = true;
	private boolean withException = false;
	private boolean waitOnRetry = false;

	private static final Logger logger = LoggerFactory.getLogger(ProcessorChain.class);

	public ProcessorChain() {
	}

	public ProcessorChain(List<String> seeds, int maxHop, List<String> authorized, Agressivity agressivity) {
		super();
		this.seeds = seeds;
		this.maxHop = maxHop;
		this.authorized = authorized;
		this.agressivity = agressivity;
	}

	@Override
	public void run() {
		chrono = new Chrono();
		chrono.pick();
		if (seeds.isEmpty()) {
			throw new RuntimeException("Seeds is empty!");
		}

		// Initialisation
		enQueued = new LinkedList<Page>(pageTool.url2Pages(seeds));
		report.setSeeds(seeds);

		// Informations
		String auth = "";
		for (String string : authorized) {
			auth += string + ", ";
		}
		// @formatter:off
		logger.info("Crawl Start with parameters :\n " +
			"- " + enQueued.size() + " seed(s)\n" +
			"- maxHop : " + maxHop + "\n" +
			"- authorized : "+auth+"\n"
			+ "- agressivity : "+agressivity);
		// @formatter:on

		do {
			for (Page page = null; (page = enQueued.poll()) != null;) {

				// Retrieve inner-links
				downloadAndSeek(page);
			}

		} while (!enQueued.isEmpty());

		// Report

		logger.info("=========================");
		logger.info("Crawl ended");
		logger.info(
				"Crawled " + report.getSuccessCrawled() + "/" + report.size() + " pages in " + chrono.compareToHour());
		report.setTime(chrono.compare());

	}

	private void downloadAndSeek(Page page) {
		List<Page> newPages = new ArrayList<>();

		report.startCrawl(page.getUrl());

		// Wait for a user friendly crawl
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
			report.stopCrawl(page.getUrl());

			// Parse content
			htmlVisitor.indexify(page.getUrl(), content);

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
			logger.error("RestClientException on page " + page.getUrl() + "\n Message : " + e.getMessage());
			e.printStackTrace();
			report.errorCrawl(page.getUrl());
			setAsCrawlErrorPage(page);

			// Launching a connectivity test to know if we still have Internet
			// ... and wait if not
			webRepo.connectivityTest();
		}

	}

	private void setAsCrawlErrorPage(Page page) {

		logger.warn("Error crawling : " + page.getUrl());
		if (page.getNbRetry() < props.getMaxRetry()) {
			logger.warn(
					"Page is set to retry (nb retry: " + page.getNbRetry() + "/max retry " + props.getMaxRetry() + ")");
			page.setNbRetry(page.getNbRetry() + 1);
			enQueued.add(page);

			if (waitOnRetry) {
				logger.debug("waitOnRetry is set");
				sleep(agressivity);
			}

		} else {
			logger.warn("Reach max retry : " + page.getNbRetry() + "/" + props.getMaxRetry());
			if (withException) {
				throw new RuntimeException("Max retry reached on page " + page.getUrl());
			}
		}
	}

	private void sleep(Agressivity ag) {
		try {
			int range = getRandomNumberInRange(ag.getMin(), ag.getMax());
			range = range * 1000;
			logger.debug("Thread will sleep " + range + "ms");
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

	public void askToStart() {
		running = true;
		isFirst = true;
		report = CrawlReport.getInstance();
	}

	public List<String> getSeeds() {
		return seeds;
	}

	public void setSeeds(List<String> seeds) {
		this.seeds = seeds;
	}

	public int getMaxHop() {
		return maxHop;
	}

	public void setMaxHop(int maxHop) {
		this.maxHop = maxHop;
	}

	public List<String> getAuthorised() {
		return authorized;
	}

	public void setAuthorised(List<String> authorised) {
		this.authorized = authorised;
	}

	public Agressivity getAgressivity() {
		return agressivity;
	}

	public void setAgressivity(Agressivity agressivity) {
		this.agressivity = agressivity;
	}

	public boolean getRunning() {
		return running;
	}

	public CrawlReport getReport() {
		return report;
	}

	public void resetReport() {
		report = CrawlReport.getInstance();
	}

	public Chrono getChrono() {
		return chrono;
	}

	public void setWithException(boolean withException) {
		this.withException = withException;

	}

	public void setWaitOnRetry(boolean waitOnRetry) {
		this.waitOnRetry = waitOnRetry;

	}
}
