package fr.ses10doigts.coursesCrawler.model.crawl;

import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.FinalState;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.RunningState;
import lombok.Data;

@Data
public class Report {

    private RunningState runningState;
    private FinalState   finalState;
    private String	 message;

}
