package fr.ses10doigts.coursesCrawler.model.paris;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BilanParis {
    private Long nbCourses;
    private Long nbWin;
    private Long nbLoose;
    private BigDecimal amountWin;
    private BigDecimal amountLoose;
    private BigDecimal benefits;
    private BigDecimal totalMise;
}
