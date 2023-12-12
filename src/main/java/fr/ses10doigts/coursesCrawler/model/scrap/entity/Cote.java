package fr.ses10doigts.coursesCrawler.model.scrap.entity;

import java.util.Objects;

import fr.ses10doigts.coursesCrawler.model.scrap.AbstractCourseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"courseID", "numCheval"}))
@Data
public class Cote extends AbstractCourseEntity {


    private Integer numCheval;
    private Float   coteDepart;
    private Float   coteAvant;
    private Float   enjeuxDepart;
    private Float   enjeuxAvant;
    private Float   rapportProbableGeny;

    public Cote() {
    }


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ Objects.hash(coteAvant, coteDepart, enjeuxAvant, enjeuxDepart, numCheval, rapportProbableGeny);
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Cote other = (Cote) obj;
		return Objects.equals(coteAvant, other.coteAvant) && Objects.equals(coteDepart, other.coteDepart)
				&& Objects.equals(enjeuxAvant, other.enjeuxAvant) && Objects.equals(enjeuxDepart, other.enjeuxDepart)
				&& Objects.equals(numCheval, other.numCheval)
				&& Objects.equals(rapportProbableGeny, other.rapportProbableGeny);
	}



	@Override
    public String toString() {
	return "Cote [id=" + getId() + ", courseID=" + getCourseID() + ", numCheval=" + numCheval + ", coteDepart="
		+ coteDepart + ", coteAvant=" + coteAvant + ", enjeuxDepart=" + enjeuxDepart + ", enjeuxAvant="
		+ enjeuxAvant + "]";
    }


}
