package fr.ses10doigts.coursesCrawler.model.paris;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BilanParis {
    private Integer nbCourses;
    private Integer nbWin;
    private Integer nbLoose;
    private BigDecimal amountWin;
    private BigDecimal amountLoose;
    private BigDecimal benefits;
}
