package fr.ses10doigts.coursesCrawler.service.bet;

import fr.ses10doigts.coursesCrawler.model.paris.Paris;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Course;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.CourseComplete;
import fr.ses10doigts.coursesCrawler.repository.paris.ParisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class BetService {
    private static final Logger logger = LoggerFactory.getLogger(BetService.class);

    @Autowired
    private ParisRepository parisRepository;

    public Paris generateBet(Course course, BigDecimal bet, int numCheval){

        logger.info("Bet service Generate");
        Optional<Paris> optLastParis = parisRepository.findLastParis();
        Paris paris = new Paris();
        int coefBet = 1;

        if (optLastParis.isPresent()){
            Paris lastParis = optLastParis.get();
            paris.setParisPrecedent(lastParis);

            logger.info("Last bet on course : {}", lastParis.getCourse().getCourseID());

            if( lastParis.getIsEnded()) {
                Paris betCursor = lastParis;
                while (!betCursor.getIsWin()) {
                    if( coefBet == 4 )
                        coefBet = 1;
                    coefBet *= 2;

                    betCursor = betCursor.getParisPrecedent();
                    if( betCursor == null )
                        break;
                }
            }else{
                logger.warn("!!! Last Paris not ended (cur: {}, last: {}) !!!!", course.getCourseID(), lastParis.getCourse().getCourseID());
            }

            logger.info("Coef define on {}", coefBet);
        }
        paris.setMise(bet.multiply(new BigDecimal( coefBet )));

        paris.setIsEnded(false);
        paris.setNumChevalMise(numCheval);
        paris.setCourse(course);

        logger.info("New bet on {}, {}€", paris.getCourse().getCourseID(), paris.getMise());

        parisRepository.save(paris);

        logger.info("Bet saved");

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
                paris.setGain( course.getRapGagnantPmu() );
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
}
