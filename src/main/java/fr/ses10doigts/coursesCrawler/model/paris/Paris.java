package fr.ses10doigts.coursesCrawler.model.paris;

import fr.ses10doigts.coursesCrawler.model.scrap.AbstractEntity;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Course;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class Paris extends AbstractEntity {

    @ManyToOne
    private Course course;
    @ManyToOne(fetch = FetchType.LAZY)
    private Paris parisPrecedent;
    private Boolean isWin;
    private Double gain;
    private Integer numChevalMise;
    private BigDecimal mise;
    private Boolean isEnded;
}
