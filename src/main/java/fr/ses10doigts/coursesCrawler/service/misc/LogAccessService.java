package fr.ses10doigts.coursesCrawler.service.misc;

import fr.ses10doigts.coursesCrawler.service.scheduler.CrawlJobCheckerService;
import org.apache.commons.io.input.ReversedLinesFileReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Service
@Slf4j
public class LogAccessService {
    @Value("${logging.file.name}")
    private String logFilePath;

    public List<String> getLastLines( int lineCount )  {
        Deque<String> result = new ArrayDeque<>();

        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(new File(logFilePath), 4096, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null && result.size() < lineCount) {
                result.addFirst(line);
            }
        }catch (Exception e){
            log.error("Error during log retrieving...");
        }

        return new ArrayList<>(result);
    }
}
