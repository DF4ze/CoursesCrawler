package fr.ses10doigts.coursesCrawler.repository.paris;

import java.math.BigDecimal;

public interface BilanProjection {
    Long getNbCourses();
    Long getNbWin();
    Long getNbLoose();
    BigDecimal getAmountWin();
    BigDecimal getAmountLoose();
    BigDecimal getTotalMise();
}
