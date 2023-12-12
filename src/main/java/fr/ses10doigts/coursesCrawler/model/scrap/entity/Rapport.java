package fr.ses10doigts.coursesCrawler.model.scrap.entity;

import java.util.Objects;

import fr.ses10doigts.coursesCrawler.model.scrap.AbstractCourseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

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


    public Rapport() {
    }


    @Override
    public String toString() {
	return "Rapport [id=" + getId() + ", courseID=" + getCourseID() + ", numCheval=" + numCheval + ", arrivee="
		+ arrivee + ", placeGeny=" + placeGeny + ", gagnantGeny=" + gagnantGeny + ", placePmu=" + placePmu
		+ ", gagantPmu=" + gagnantPmu + "]";
    }


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rapport other = (Rapport) obj;
		return Objects.equals(arrivee, other.arrivee) && Objects.equals(numCheval, other.numCheval);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(arrivee, numCheval);
		return result;
	}
}
