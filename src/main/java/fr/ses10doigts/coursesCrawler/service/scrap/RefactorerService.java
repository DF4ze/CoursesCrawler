package fr.ses10doigts.coursesCrawler.service.scrap;

import java.lang.Thread.State;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.ses10doigts.coursesCrawler.model.crawl.Report;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.FinalState;
import fr.ses10doigts.coursesCrawler.model.crawl.enumerate.RunningState;
import fr.ses10doigts.coursesCrawler.service.scrap.tool.Chrono;
import fr.ses10doigts.coursesCrawler.service.scrap.tool.RefactorerReport;

@Component
public class RefactorerService {

	@Autowired
	private Refactorer refactorer;
	private Thread thread;

	public RefactorerService() {

	}

	public void launchRefactorer() {
		thread = new Thread(refactorer);
		resetRefactorer();

		thread.start();
	}

	public void launchRefactorer(Thread t) {
		refactorer.setFriend(t);
		launchRefactorer();
	}

	public void stopRefactorer() {
		refactorer.stop();
	}

	public void resetRefactorer() {
		refactorer.setStart();
		refactorer.resetReport();
	}

	public Report getReportCurrentRefact() {
		RefactorerReport refReport = refactorer.getReport();
		Report report = new Report();

		if (thread != null) {
			State state = thread.getState();
			// if thread is terminated ?
			if (state.equals(State.TERMINATED)) {
				// has it (not) been stop by user ?
				if (refactorer.getRunning()) {
					report.setFinalState(FinalState.SUCCESS);
				} else {
					report.setFinalState(FinalState.WARNING);
				}
				report.setRunningState(RunningState.ENDED);
			} else if (state.equals(State.WAITING)) {
				report.setRunningState(RunningState.WAITING);
			} else {
				report.setRunningState(RunningState.PROCESSING);
			}
		} else {
			report.setRunningState(RunningState.PENDING);
			report.setFinalState(FinalState.SUCCESS);
		}

	// @formatter:off
	String message = "Refactorer status : "+report.getRunningState()+"\n"+
		"- Total course : "+refReport.totalCourse()+"\n"+
		"- Course treated : "+refReport.getTreated()+"\n"+
		"- Course skipped : "+refReport.getSkipped()+"\n"+
		"- Time :   " + Chrono.longMillisToHour( refReport.getTime() )+ "\n";
	//@formatter:on

		report.setMessage(message);
		return report;
	}

	public Thread getThread() {
		return thread;
	}

}
