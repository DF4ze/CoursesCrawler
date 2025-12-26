package fr.ses10doigts.coursesCrawler.service.bet;

import fr.ses10doigts.coursesCrawler.CustomProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;

@Service
public class BetNodeService {
    private static final Logger logger = LoggerFactory.getLogger(BetNodeService.class);

    @Autowired
    private CustomProperties props;

    boolean launchBetProcess(long courseID, BigDecimal bet, int chvlNb){
        String sCourse = String.valueOf(courseID);
        String sBet =String.valueOf(bet);
        String sChvlNb = String.valueOf(chvlNb);
        ProcessBuilder pb = new ProcessBuilder(
                "node", "/home/oklm/auto_lumex/script.js", sCourse, sBet, sChvlNb,
                props.getPuppeteerUser(),
                props.getPuppeteerPwd()
        );

        boolean isOk = true;
        try {
            Process process = pb.start();

            // Récupérer la sortie standard
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }

            // Récupérer la sortie d'erreur
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                logger.error(line);
            }

            int exitCode = process.waitFor();
            logger.info("Node terminé avec le code : {}", exitCode);

            isOk = exitCode == 0;

        } catch (Exception e) {
            isOk = false;
            logger.error("An error occurred during action on website : {}", e.getMessage());
        }

        return isOk;
    }
}
