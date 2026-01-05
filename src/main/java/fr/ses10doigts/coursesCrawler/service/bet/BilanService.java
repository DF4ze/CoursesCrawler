package fr.ses10doigts.coursesCrawler.service.bet;

import fr.ses10doigts.coursesCrawler.model.paris.BilanParis;
import fr.ses10doigts.coursesCrawler.model.paris.GlobalBilanParis;
import fr.ses10doigts.coursesCrawler.repository.paris.ParisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.IsoFields;

@Service
public class BilanService {
    private static final Logger logger = LoggerFactory.getLogger(BilanService.class);

    @Autowired
    private ParisRepository parisRepository;

    public GlobalBilanParis getGlobalBilan() {
        LocalDate today = LocalDate.now();

        int annee = today.getYear();
        int mois = today.getMonthValue();
        int semaine = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR); // semaine ISO

        logger.info("Ask for bilan Y: {}, M: {}, W: {}",annee, mois, semaine);

        return getGlobalBilan(annee, mois, semaine);
    }

    /**
     * Return a global report
     *
     * @param annee number of the year
     * @param mois number of the month
     * @param semaine number of the week in the year
     * @return a {@link GlobalBilanParis}
     */
    public GlobalBilanParis getGlobalBilan(int annee, int mois, int semaine) {
        GlobalBilanParis globalBilan = new GlobalBilanParis();

        globalBilan.setBilanAnnee(computeBilan("YEAR(p.course.date) = :annee", annee, null, null));
        globalBilan.setBilanMois(computeBilan("YEAR(p.course.date) = :annee AND MONTH(p.course.date) = :mois", annee, mois, null));
        globalBilan.setBilanSemaine(computeBilan("YEAR(p.course.date) = :annee AND WEEK(p.course.date, 1) = :semaine", annee, null, semaine));

        return globalBilan;
    }

    private BilanParis computeBilan(String whereClause, Integer annee, Integer mois, Integer semaine) {
        Object[] result = parisRepository.computeBilan(annee, mois, semaine);

        BilanParis bilan = new BilanParis();
        bilan.setNbCourses(((Number) result[0]).intValue());
        bilan.setNbWin(((Number) result[1]).intValue());
        bilan.setNbLoose(((Number) result[2]).intValue());
        bilan.setAmountWin(result[3] != null ? (BigDecimal) result[3] : BigDecimal.ZERO);
        bilan.setAmountLoose(result[4] != null ? (BigDecimal) result[4] : BigDecimal.ZERO);
        bilan.setBenefits(bilan.getAmountWin().subtract(bilan.getAmountLoose()));

        logger.info("Bilan : {}", bilan);
        return bilan;
    }
}

