package fr.ses10doigts.coursesCrawler.service.bet;

import fr.ses10doigts.coursesCrawler.CustomProperties;

import lombok.extern.slf4j.Slf4j;
import fr.ses10doigts.coursesCrawler.service.web.TelegramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;

@Service
@Slf4j
public class BetNodeService {
    @Autowired
    private CustomProperties props;
    @Autowired
    private TelegramService telegramService;

    private static final boolean isDev = false;
    private static final String user = "oklm";

    public boolean launchBetProcess(long courseID, BigInteger bet, int chvlNb){
        if( !props.getPuppeteerActivated().equals("true") )
            return false;

        String sCourse = String.valueOf(courseID);
        String sBet = bet.toString();
        String sChvlNb = String.valueOf(chvlNb);
        ProcessBuilder pb = new ProcessBuilder(
                "/usr/bin/node",
                props.getPuppeteerPath(),
                sCourse,
                sBet,
                sChvlNb,
                isDev?"true":"false",
                user
        );

        boolean isOk = true;
        try {
            Process process = pb.start();

            // Récupérer la sortie standard
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }

            // Récupérer la sortie d'erreur
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder error = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                log.error(line);
                error.append(line).append("\n");
                isOk = false;
            }

            int exitCode = process.waitFor();

            isOk = isOk && exitCode == 0;

            if( isOk ) logger.info("Success action on site (code : {})", exitCode);
            else{
                log.error("Error during action on site (code : {})", exitCode);
                telegramService.sendMessage(props.getTelegramErrorChatId(), "Erreur lors du paris :\n"+error.toString());
            }

        } catch (Exception e) {
            isOk = false;
            log.error("An error occurred during action on website : {}", e.getMessage());
        }

        return isOk;
    }
}
