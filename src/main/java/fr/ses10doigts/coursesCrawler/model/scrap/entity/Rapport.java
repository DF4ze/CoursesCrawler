package fr.ses10doigts.coursesCrawler.model.scrap.entity;

import fr.ses10doigts.coursesCrawler.model.scrap.AbstractCourseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"courseId", "numCheval"}))
@Data
public class Rapport extends AbstractCourseEntity{


    private Integer numCheval;
    private Integer arrivee;
    private Double  placeGeny;
    private Double  gagnantGeny;
    private Double  placePmu;
    private Double  gagnantPmu;
	private Boolean valide;

}
