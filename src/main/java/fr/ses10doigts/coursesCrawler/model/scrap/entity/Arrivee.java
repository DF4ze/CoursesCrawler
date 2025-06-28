package fr.ses10doigts.coursesCrawler.model.scrap.entity;

import java.util.Objects;

import fr.ses10doigts.coursesCrawler.model.scrap.AbstractCourseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"courseID", "numChv"}))
@Data
public class Arrivee extends AbstractCourseEntity {



    private Integer numArrivee;
    private Integer numChv;
    private String nomChv;

    public Arrivee(){
		super();
    }

    public Arrivee(String url, Long courseId, Integer numArrivee, Integer numChv, String nomChv) {
		super();
		setUrl(url);
		setCourseID(courseId);
		this.numArrivee = numArrivee;
		this.numChv = numChv;
		this.nomChv = nomChv;
    }






	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(nomChv, numArrivee, numChv);
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
		Arrivee other = (Arrivee) obj;
		return Objects.equals(nomChv, other.nomChv) && Objects.equals(numArrivee, other.numArrivee)
				&& Objects.equals(numChv, other.numChv);
	}

	@Override
    public String toString() {
	return "Arrivee [id=" + getId() + ", CourseId=" + getCourseID() + ", numArrivee=" + numArrivee + ", numChv="
		+ numChv + ", nomChv=" + nomChv + "]";
    }





}
