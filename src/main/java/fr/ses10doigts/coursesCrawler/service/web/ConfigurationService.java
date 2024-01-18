package fr.ses10doigts.coursesCrawler.service.web;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.ses10doigts.coursesCrawler.CustomProperties;
import fr.ses10doigts.coursesCrawler.model.web.Configuration;
import fr.ses10doigts.coursesCrawler.service.crawl.tool.LineReader;
import fr.ses10doigts.coursesCrawler.service.crawl.tool.LineWriter;

@Component
public class ConfigurationService {
    @Autowired
    private LineReader	     reader;
    @Autowired
    private LineWriter	     writer;
    @Autowired
    private CustomProperties props;

    private static final Logger	logger = LoggerFactory.getLogger(ConfigurationService.class);
    private static final DateFormat urlDateFormat    = new SimpleDateFormat("yyyy-MM-dd");
    private static final DateFormat writenDateFormat = new SimpleDateFormat("dd/MM/yyyy");


    public Configuration getConfiguration() {
	Configuration conf = new Configuration();

	conf.setAgressivity(props.getAgressivity());
	conf.setMaxHop(props.getMaxHop());
	conf.setMaxRetry(props.getMaxRetry());
	conf.setLaunchCrawl(props.isDoCrawl());
	conf.setLaunchRefacto(props.isDoRefacto());

	try {
	    // Retrieve seeds
	    reader.setFilePath(props.getSeedsFile());
		List<String> urls = reader.fileToSet();
	    String txtUrl = "";
	    for (String url : urls) {
		txtUrl += url + "\n";
	    }
	    conf.setTxtSeeds(txtUrl);

	    // retrieve authorized words in url
	    reader.setFilePath(props.getAuthorizedFile());
		List<String> urlAuthorised = reader.fileToSet();
	    String txtAuth = "";
	    for (String url : urlAuthorised) {
		txtAuth += url + "\n";
	    }
	    conf.setAuthorized(txtAuth);

	} catch (Exception e) {
	    logger.warn("Seeds or/and Authorised file(s) not found ");
	}
	return conf;
    }

    public void saveConfiguration(Configuration conf) {
	try {


	    props.setAgressivity(conf.getAgressivity());
	    props.setMaxHop(conf.getMaxHop());
	    props.setMaxRetry(conf.getMaxRetry());
	    props.setDoCrawl(conf.isLaunchCrawl());
	    props.setDoRefacto(conf.isLaunchRefacto());

	    writer.setFilePath(props.getSeedsFile());
	    writer.StringToFile(conf.getTxtSeeds());

	    writer.setFilePath(props.getAuthorizedFile());
	    writer.StringToFile(conf.getAuthorized());

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    public String generateUrlFromDates(String sStartDate, String sEndDate) {

		Date startDate = null;
		Date endDate = null;
		try {
			startDate = writenDateFormat.parse(sStartDate);
			endDate = writenDateFormat.parse(sEndDate);
		} catch (ParseException e) {
			return "Date must be dd/mm/yyyy";
		}

		// Secure
		Date comp = new Date();
		if (startDate == null || endDate == null || startDate.after(endDate) || startDate.after(comp)
				|| endDate.after(comp)) {
			return null;
		}
		// convert date to calendar
		Calendar c = Calendar.getInstance();
		c.setTime(startDate);

		String urlList = "";
		do {
			comp = c.getTime();
			urlList += "https://www.geny.com/reunions-courses-pmu?date=" + urlDateFormat.format(comp) + "\n";
			c.add(Calendar.DATE, 1);
		} while (comp.before(endDate));

		return urlList;
    }

}
