package fr.ses10doigts.coursesCrawler.service.scrap.tool;

import java.time.LocalTime;
import java.util.Date;

public class Chrono {


    private long time1;
    private long time2;

    public Chrono() {
    }

    public void pick() {
	time1 = new Date().getTime();
    }

    public long compare() {
	long delta = 0;

	time2 = new Date().getTime();
	delta = time2-time1;

	return delta;
    }

    public String compareToHour() {
	return Chrono.longMillisToHour(compare());
    }

    public static String longMillisToHour(long millis) {
		try {
			long seconds = millis / 1000;
			LocalTime timeOfDay = LocalTime.ofSecondOfDay(seconds);
			String time = timeOfDay.toString();

			return time;
		} catch (Exception e) {
			return "00:00";
		}
    }
}
