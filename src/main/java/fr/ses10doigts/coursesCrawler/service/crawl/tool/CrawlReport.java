package fr.ses10doigts.coursesCrawler.service.crawl.tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.ses10doigts.coursesCrawler.model.crawl.Page;
import fr.ses10doigts.coursesCrawler.model.crawl.PageReport;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.ChainState;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.FinalState;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.RunningState;
import fr.ses10doigts.coursesCrawler.service.scrap.tool.Chrono;

public class CrawlReport {
    private static Map<String, PageReport> urlsReport = new HashMap<>();
    private long			   time	      = 0;
    private boolean			   running    = false;
    private Chrono			   chrono     = new Chrono();

    private CrawlReport() {

    }

    public static CrawlReport getInstance() {
	return new CrawlReport();
    }

	public void setSeeds(List<String> urls) {
	for (String url : urls) {
	    addAndSetPending(url);
	}
    }

	public void addNewPages(List<Page> pages) {
	for (Page page : pages) {
	    addAndSetPending(page.getUrl());
	}
    }

    public int getSuccessCrawled() {
		int count = 0;
		synchronized (urlsReport) {
			for (Entry<String, PageReport> urlReport : urlsReport.entrySet()) {
				if (urlReport.getValue().chainState == ChainState.CRAWLING
						&& urlReport.getValue().runningState == RunningState.ENDED
						&& urlReport.getValue().finalState == FinalState.SUCCESS) {
					count++;
				}
			}
		}
		return count;
    }

    public int size() {
		int size = 0;
		synchronized (urlsReport) {
			size = urlsReport.size();
		}
		return size;
    }

    public int getErrorCrawled() {
		int count = 0;
		synchronized (urlsReport) {
			for (Entry<String, PageReport> urlReport : urlsReport.entrySet()) {
				if (urlReport.getValue().chainState == ChainState.CRAWLING
						&& (urlReport.getValue().finalState == FinalState.ERROR
								|| urlReport.getValue().finalState == FinalState.WARNING)) {
					count++;
				}
			}
		}
		return count;
    }

    public int getRetryCrawled() {
		int count = 0;
		synchronized (urlsReport) {
			for (Entry<String, PageReport> urlReport : urlsReport.entrySet()) {
				if (urlReport.getValue().chainState == ChainState.CRAWLING
						&& urlReport.getValue().runningState == RunningState.PENDING_RETRY) {
					count++;
				}
			}
		}
		return count;
    }

    public int getPendingCrawled() {
		int count = 0;
		synchronized (urlsReport) {
			for (Entry<String, PageReport> urlReport : urlsReport.entrySet()) {
				if (urlReport.getValue().chainState == ChainState.CRAWLING
						&& (urlReport.getValue().runningState == RunningState.PENDING_RETRY
								|| urlReport.getValue().runningState == RunningState.PENDING)) {
					count++;
				}
			}
		}

		return count;
    }

    public void startCrawl(String url) {
		synchronized (urlsReport) {
			PageReport pr = new PageReport();
			pr.chainState = ChainState.CRAWLING;
			pr.runningState = RunningState.PROCESSING;
			urlsReport.put(url, pr);
			running = true;
			chrono.pick();
		}
    }

    public void errorCrawl(String url) {
		synchronized (urlsReport) {
			PageReport pr = new PageReport();
			pr.chainState = ChainState.CRAWLING;
			pr.finalState = FinalState.ERROR;
			pr.runningState = RunningState.PENDING_RETRY;
			urlsReport.put(url, pr);
		}

    }

    public void stopCrawl(String url) {
		synchronized (urlsReport) {
			PageReport pr = new PageReport();
			pr.chainState = ChainState.CRAWLING;
			pr.finalState = FinalState.SUCCESS;
			pr.runningState = RunningState.ENDED;
			urlsReport.put(url, pr);
			running = false;
			time = chrono.compare();
		}
    }

    public void startStore(String url) {
		synchronized (urlsReport) {
			PageReport pr = new PageReport();
			pr.chainState = ChainState.STORING;
			pr.runningState = RunningState.ENDED;
			urlsReport.put(url, pr);
		}

    }

    public void errorStore(String url) {
		synchronized (urlsReport) {
			PageReport pr = new PageReport();
			pr.chainState = ChainState.STORING;
			pr.finalState = FinalState.ERROR;
			pr.runningState = RunningState.PENDING_RETRY;
			urlsReport.put(url, pr);
		}

    }

    public void stopStore(String url) {
		synchronized (urlsReport) {
			PageReport pr = new PageReport();
			pr.chainState = ChainState.STORING;
			pr.finalState = FinalState.SUCCESS;
			pr.runningState = RunningState.ENDED;
			urlsReport.put(url, pr);
		}
    }

    private void addAndSetPending(String url) {
		synchronized (urlsReport) {
			PageReport pr = new PageReport();
			pr.chainState = ChainState.CRAWLING;
			pr.runningState = RunningState.PENDING;
			urlsReport.put(url, pr);
		}
    }

    public long getTime() {
	if( running ){
	    return chrono.compare();
	}
	return time;
    }

    public void setTime(long time) {
	this.time = time;
    }

}
