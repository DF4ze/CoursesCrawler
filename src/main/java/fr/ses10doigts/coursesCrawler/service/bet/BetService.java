package fr.ses10doigts.coursesCrawler.service.bet;

import fr.ses10doigts.coursesCrawler.CustomProperties;
import fr.ses10doigts.coursesCrawler.model.paris.Paris;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Course;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.CourseComplete;
import fr.ses10doigts.coursesCrawler.repository.paris.ParisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Optional;

@Service
@Profile({ "devWithTelegram", "telegram" })
public class BetService {
    private static final Logger logger = LoggerFactory.getLogger(BetService.class);

    @Autowired
    private ParisRepository parisRepository;
    @Autowired
    private BetNodeService betNodeService;
    @Autowired
    private CustomProperties props;

    @Transactional
    public Paris generateBet(BigInteger bet, CourseComplete course, Course c){

        logger.debug("Bet service Generate");
        LocalDate now = LocalDate.now(ZoneId.of("Europe/Paris"));
        Paris paris = new Paris();
        paris.setDate(now);
        paris.setAnnee(now.getYear());
        paris.setMois(now.getMonthValue());
        paris.setSemaine(now.get(WeekFields.ISO.weekOfWeekBasedYear()));
        paris.setJour(now.getDayOfMonth());

        Paris lastParis = parisRepository.findLastParis().orElse(null);
        long coefBet = 1;

        if (lastParis != null){
            paris.setParisPrecedent(lastParis);

            logger.debug("Last bet on course : {}", lastParis.getCourse().getCourseID());

            List<Integer> martingale = props.getPuppeteerMartingale();//List.of(1.0f, 2.0f, 3.0f, 2.0f);

            if( lastParis.getIsEnded()) {
                Paris betCursor = lastParis;
                int count = 0;
                while (betCursor.getIsEnded() && !betCursor.getIsWin()) {
                    count++;
                    coefBet = martingale.get(count % martingale.size());

                    betCursor = betCursor.getParisPrecedent();
                    if( betCursor == null )
                        break;
                }
                logger.info("{} looses, {} place of Martingale, Coef define on {}", count, count % martingale.size(), coefBet);

            }else{
                logger.warn("!!! Last Paris not ended (cur: {}, last: {}) !!!!", course.getCourseID(), lastParis.getCourse().getCourseID());
            }

        }else{
            logger.warn("Last Paris is null!");
        }

        // Gestion d'un ratio avec la cote du gagnant.
        paris.setMise(BigInteger.valueOf( coefBet ).multiply(bet));

        int numCheval = course.getNumeroChlPremierAvant();
        paris.setIsEnded(false);
        paris.setNumChevalMise(numCheval);
        paris.setCourse(c);

        logger.info("New bet on c{}, {}€ on N°{}", paris.getCourse().getCourseID(), paris.getMise(), numCheval);

        try {
            logger.debug("Launching webAction");
            boolean isActionOK = betNodeService.launchBetProcess(course.getCourseID(), paris.getMise(), numCheval);
            paris.setIsWebActionOk(isActionOK);

            logger.info("Ended");
        }catch (Exception e){
            logger.error("Error occurred during WebAction : {}",e.getMessage());
            paris.setIsWebActionOk(false);
        }

        parisRepository.save(paris);

        return paris;
    }

    public Paris updateBetResult(CourseComplete course){
        Optional<Paris> optParis = parisRepository.findByCourse_CourseID(course.getCourseID());

        Paris paris = null;
        if(optParis.isPresent() ){
            paris = optParis.get();
            if( course.getNumeroChvlPremier() != null ) {
                paris.setIsWin(course.getNumeroChvlPremier().equals(paris.getNumChevalMise()));
            }else{
                logger.warn("No First place?!!!");
            }

            if (true == paris.getIsWin()){
                paris.setGain( course.getRapGagnantGeny() );

                if( paris.getGain() == null ){
                    logger.warn("!!!!! Error in parsing course arrival for {}... unable to retreive Gain !!!", course.getCourseID());
                }
            }

            paris.setIsEnded(true);

            parisRepository.save(paris);

        }else{
            logger.warn("!!! Unexisting bet for course {}!!!", course.getCourseID());
        }

        return paris;
    }

    public Paris getLastBet() {
        return parisRepository.findLastParis().orElse(null);
    }

    public List<Paris> getUnendedBet(){
        return parisRepository.findAllNotEnded();
    }
}
