package fr.ses10doigts.coursesCrawler.service.crawl;

import fr.ses10doigts.coursesCrawler.CustomProperties;
import fr.ses10doigts.coursesCrawler.model.Configuration;
import fr.ses10doigts.coursesCrawler.model.crawl.Report;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.Agressivity;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.FinalState;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.RunningState;
import fr.ses10doigts.coursesCrawler.repository.web.WebCrawlingProxy;
import fr.ses10doigts.coursesCrawler.service.crawl.tool.LineReader;
import fr.ses10doigts.coursesCrawler.service.scrap.RefactorerService;
import fr.ses10doigts.coursesCrawler.service.scrap.tool.Chrono;
import fr.ses10doigts.coursesCrawler.service.web.ConfigurationService;
import fr.ses10doigts.coursesCrawler.service.web.ExcelStreamExtractorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.lang.Thread.State;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class CrawlService {
    @Autowired
    private WebCrawlingProxy webService;
    @Autowired
	private LineReader reader;
	@Autowired
	private CustomProperties props;
    @Autowired
	private ProcessorChain processorChain;
    @Autowired
	private ConfigurationService configurationService;
	@Autowired
	private RefactorerService refactoService;
	@Autowired
	private ExcelStreamExtractorService excelService;
	@Autowired
	private TransactionTemplate transactionTemplate;



	private static Thread crawlTreatment = null;

    private static final Logger	logger = LoggerFactory.getLogger(CrawlService.class);


	private Report startCrawl(boolean withException) throws IOException {
		if (crawlTreatment == null || crawlTreatment.getState().equals(State.TERMINATED)) {
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
			processorChain.setSeeds(urls);
			processorChain.setMaxHop(props.getMaxHop());
			processorChain.setAuthorised(urlAuthorised);
			processorChain.setAgressivity(props.getAgressivity());
			processorChain.setWithException(withException);
			processorChain.setWaitOnRetry(props.isWaitOnRetry());
			processorChain.initialize();
			crawlTreatment = new Thread(processorChain);
			crawlTreatment.start();
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

	public Report crawlFromConfig() {
		return crawlFromConfig(false);
	}

	public Report crawlFromConfig(boolean withException) {
		Configuration configuration = configurationService.getConfiguration();

		Report cr = new Report();
		try {
			if (configuration.isLaunchCrawl()) {
				cr = startCrawl(withException);
			}

			if (configuration.isLaunchRefacto()) {
				refactoService.launchRefactorer(crawlTreatment);
			}

			if( configuration.isLaunchExcel() ){
				ExecutorService executor = Executors.newSingleThreadExecutor();
				Future<?> future = executor.submit(() -> {
					transactionTemplate.execute(status -> {
						excelService.waitAndExtract( crawlTreatment, refactoService.getThread() );
						return null;
					});
				});
			}

		} catch (IOException e) {

			cr.setFinalState(FinalState.ERROR);
			cr.setRunningState(RunningState.ENDED);
			cr.setMessage("Error reading seeds file");
		}

		return cr;
	}

	public void datesCrawl(String startDay, String endDay) {
        logger.debug("DAYS : {} {}", startDay, endDay);
		String urls = configurationService.generateUrlFromDates(startDay, endDay);
        logger.debug("URLS : {}", urls);

		Configuration conf = new Configuration();
		conf.setAgressivity(Agressivity.MEDIUM);
		conf.setAuthorized("partants-pmu\r\n");
		conf.setLaunchCrawl(true);
		conf.setLaunchRefacto(false);
		conf.setMaxRetry(10);
		conf.setWaitOnRetry(true);
		conf.setMaxHop(1);
		conf.setTxtSeeds(urls);
		configurationService.saveConfiguration(conf);

		crawlFromConfig();
	}

    public Report getReportCurrentCrawl() {
	fr.ses10doigts.coursesCrawler.service.crawl.tool.CrawlReport crawlReport = processorChain.getReport();
	Report report = new Report();

	if (crawlTreatment != null) {
	    State state = crawlTreatment.getState();
	    // if thread is terminated ?
	    if (state.equals(State.TERMINATED)) {
			// has it (not) been stop by user ?
			if (processorChain.getRunning()) {
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

    public void stopCurrentCrawl() {
		processorChain.askToStop();
		processorChain.resetReport();
	}

    public Thread getTreatment() {
		return crawlTreatment;
    }


}
