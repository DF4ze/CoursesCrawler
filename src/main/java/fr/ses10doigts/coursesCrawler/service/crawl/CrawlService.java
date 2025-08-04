package fr.ses10doigts.coursesCrawler.service.crawl;

import java.io.IOException;
import java.lang.Thread.State;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.ses10doigts.coursesCrawler.CustomProperties;
import fr.ses10doigts.coursesCrawler.model.crawl.Report;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.FinalState;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.RunningState;
import fr.ses10doigts.coursesCrawler.model.Configuration;
import fr.ses10doigts.coursesCrawler.repository.web.WebCrawlingProxy;
import fr.ses10doigts.coursesCrawler.service.crawl.tool.CrawlReport;
import fr.ses10doigts.coursesCrawler.service.crawl.tool.LineReader;
import fr.ses10doigts.coursesCrawler.service.scrap.RefactorerService;
import fr.ses10doigts.coursesCrawler.service.scrap.tool.Chrono;
import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;

@Service
public class CrawlService {
    @Autowired
    private WebCrawlingProxy webService;
    @Autowired
	private LineReader reader;
	@Autowired
	private CustomProperties props;
    @Autowired
	private ProcessorChain pc;
    @Autowired
	private ConfigurationService configurationService;
	@Autowired
	private RefactorerService refactoService;


    private static Thread	treatment = null;

    private static final Logger	logger = LoggerFactory.getLogger(CrawlService.class);

    public String getPage(String url) {
		return webService.getRawPage(url);
    }



	public Report launchCrawl(boolean withException) throws IOException {
		if (treatment == null || treatment.getState().equals(State.TERMINATED)) {
			// Retrieve seeds
			reader.setFilePath(props.getSeedsFile());
			List<String> urls = reader.fileToSet();

			// retrieve authorized words in url
			reader.setFilePath(props.getAuthorizedFile());
			List<String> urlAuthorised = reader.fileToSet();

			logger.info("Following SEEDS will be crawled with a maxHop of " + props.getMaxHop());
			for (String string : urls) {
				logger.info("   - " + string);
			}

			// Creating and launching thread
			// ProcessorChain pc = new ProcessorChain(urls, props.getMaxHop(),
			// urlAuthorised, Agressivity.REALLY_SOFT);
			pc.setSeeds(urls);
			pc.setMaxHop(props.getMaxHop());
			pc.setAuthorised(urlAuthorised);
			pc.setAgressivity(props.getAgressivity());
			pc.setWithException(withException);
			pc.setWaitOnRetry(props.isWaitOnRetry());
			pc.askToStart();
			treatment = new Thread(pc);
			treatment.start();
			logger.info("Thread started");

			// CrawlReport report = new CrawlReport();
			// report.setRunningState(RunningState.PROCESSING);
			// report.setFinalState(FinalState.SUCCESS);
			// report.setMessage("Will run " + urls.size() + " urls");
		} else {
			logger.warn("Unable to launch a crawl, One is already running...");
		}
		return getReportCurrentCrawl();
	}

	public Report manageLaunch() {
		return manageLaunch(false);
	}

	public Report manageLaunch(boolean withException) {
		Configuration configuration = configurationService.getConfiguration();

		Report cr = new Report();
		try {
			if (configuration.isLaunchCrawl()) {
				cr = launchCrawl(withException);
			}

			if (configuration.isLaunchRefacto()) {
				// TODO rapport
				refactoService.launchRefactorer(treatment);

			}
		} catch (IOException e) {

			cr.setFinalState(FinalState.ERROR);
			cr.setRunningState(RunningState.ENDED);
			cr.setMessage("Error reading seeds file");
		}

		return cr;
	}

	public void launchSurveyCrawl(String startDay, String endDay) {
        logger.debug("DAYS : {} {}", startDay, endDay);
		String urls = configurationService.generateUrlFromDates(startDay, endDay);
        logger.debug("URLS : {}", urls);

		Configuration conf = new Configuration();
		conf.setAgressivity(Agressivity.MEDIUM_HARD);
		conf.setAuthorized("partants-pmu\r\n");
		conf.setLaunchCrawl(true);
		conf.setLaunchRefacto(false);
		conf.setMaxRetry(10);
		conf.setWaitOnRetry(true);
		conf.setMaxHop(1);
		conf.setTxtSeeds(urls);
		configurationService.saveConfiguration(conf);

		manageLaunch();
	}

    public Report getReportCurrentCrawl() {
	CrawlReport crawlReport = pc.getReport();
	Report report = new Report();

	if (treatment != null) {
	    State state = treatment.getState();
	    // if thread is terminated ?
	    if (state.equals(State.TERMINATED)) {
		// has it (not) been stop by user ?
		if (pc.getRunning()) {
		    report.setFinalState(FinalState.SUCCESS);
		} else {
		    report.setFinalState(FinalState.WARNING);
		}
		report.setRunningState(RunningState.ENDED);
	    } else {
		report.setRunningState(RunningState.PROCESSING);
	    }
	} else {
	    report.setRunningState(RunningState.PENDING);
	    report.setFinalState(FinalState.SUCCESS);
	}

	// @formatter:off
	String message = "Crawl status : "+report.getRunningState()+"\n"+
		"- Total page : "+crawlReport.size()+"\n"+
		"- Page pending : "+crawlReport.getPendingCrawled()+"\n"+
		"- Page ended :   "+crawlReport.getSuccessCrawled()+"\n"+
		"- Page error :   "+crawlReport.getErrorCrawled()+"\n"+
		"- Time :   " + Chrono.longMillisToHour( crawlReport.getTime() )+ "\n";
	//@formatter:on


	report.setMessage(message);
	return report;
    }

    public Report stopCurrentCrawl() {
		pc.askToStop();

		Report report = getReportCurrentCrawl();
		pc.resetReport();

		return report;
	}

	public Report startCurrentCrawl() {
		pc.askToStart();

		return getReportCurrentCrawl();
    }

    public Thread getTreatment() {
	return treatment;
    }

    public boolean testConnectivity() {
	return webService.connectivityTest();
    }

}
