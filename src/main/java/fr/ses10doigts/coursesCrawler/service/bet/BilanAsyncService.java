package fr.ses10doigts.coursesCrawler.service.bet;

import fr.ses10doigts.coursesCrawler.model.paris.BilanParis;
import fr.ses10doigts.coursesCrawler.repository.paris.BilanProjection;
import fr.ses10doigts.coursesCrawler.repository.paris.ParisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Service
public class BilanAsyncService {
    private static final Logger logger = LoggerFactory.getLogger(BilanAsyncService.class);

    @Autowired
    private ParisRepository parisRepository;

    @Async
    public CompletableFuture<BilanParis> computeBilanAsync(
            Integer annee, Integer mois, Integer semaine, Integer jour) {

        logger.debug("Start bilan Y: {}, M: {}, W: {}",annee, mois, semaine);

        BilanProjection bilanProjection = parisRepository.computeBilan(annee, mois, semaine, jour);

        BilanParis bilan = new BilanParis();
        bilan.setNbCourses(bilanProjection.getNbCourses() != null ? bilanProjection.getNbCourses() : 0L);
        bilan.setNbWin(bilanProjection.getNbWin() != null ? bilanProjection.getNbWin() : 0L);
        bilan.setNbLoose(bilanProjection.getNbLoose() != null ? bilanProjection.getNbLoose() : 0L);
        bilan.setAmountWin(bilanProjection.getAmountWin() != null ? bilanProjection.getAmountWin() : BigDecimal.ZERO);
        bilan.setAmountLoose(bilanProjection.getAmountLoose() != null ? bilanProjection.getAmountLoose() : BigDecimal.ZERO);
        bilan.setBenefits(bilan.getAmountWin().subtract(bilan.getAmountLoose()));
        bilan.setTotalMise(bilanProjection.getTotalMise() != null ? bilanProjection.getTotalMise() :  BigDecimal.ZERO);

        logger.debug("Query ended for Y: {}, M: {}, W: {}",annee, mois, semaine);

        return CompletableFuture.completedFuture(bilan);
    }
}