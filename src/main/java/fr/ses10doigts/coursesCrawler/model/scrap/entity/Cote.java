package fr.ses10doigts.coursesCrawler.model.scrap.entity;

import fr.ses10doigts.coursesCrawler.model.scrap.AbstractCourseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"courseID", "numCheval"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class Cote extends AbstractCourseEntity {


    private Integer numCheval;
    private Float   coteDepart;
    private Float   coteAvant;
    private Float   enjeuxDepart;
    private Float   enjeuxAvant;
    private Float   rapportProbableGeny;
	private Boolean valide;

}
