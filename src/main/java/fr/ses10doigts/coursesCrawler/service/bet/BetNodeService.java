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

    public boolean launchBetProcess(long courseID, BigDecimal bet, int chvlNb){
        if( !props.getPuppeteerActivated().equals("true") )
            return false;

        String sCourse = String.valueOf(courseID);
        String sBet =String.valueOf(bet);
        String sChvlNb = String.valueOf(chvlNb);
        ProcessBuilder pb = new ProcessBuilder(
                "/usr/bin/node", props.getPuppeteerPath(), sCourse, sBet, sChvlNb,
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

            isOk = exitCode == 0;

            if( isOk ) logger.info("Success action on site (code : {})", exitCode);
            else logger.error("Error during action on site (code : {})", exitCode);

        } catch (Exception e) {
            isOk = false;
            logger.error("An error occurred during action on website : {}", e.getMessage());
        }

        return isOk;
    }
}
