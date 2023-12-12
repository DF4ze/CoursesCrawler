package fr.ses10doigts.coursesCrawler.model.scrap.entity;

import java.util.Objects;

import fr.ses10doigts.coursesCrawler.model.scrap.AbstractCourseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "courseId", "numCheval" }))
@Data
public class Partant extends AbstractCourseEntity implements Cloneable {

	private Integer numCheval;
	private String nomCheval;
	private String ageSexe;
	private String musique;
	private String gains;
	private Integer iGains;
	private Float probableGeny;
	private Float probablePMU;

	public Partant() {
	}

	public void setGains(String legains) {
		this.gains = legains;

		try {
			if (gains != null) {
				gains = gains.replace(" ", "");

				this.iGains = Integer.parseInt(gains);
			}
		} catch (Exception e) {
			;
		}

	}

	@Override
	public Partant clone() {
		Partant o = new Partant();

		o.ageSexe = ageSexe;
		o.setCourseID(this.getCourseID());
		o.gains = gains;
		o.setId(getId());
		o.iGains = iGains.intValue();
		o.musique = musique;
		o.nomCheval = nomCheval;
		o.numCheval = numCheval.intValue();

		// on renvoie le clone
		return o;
	}

	@Override
	public String toString() {
		return "Partant [id=" + getId() + ", courseID=" + getCourseID() + ", numCheval=" + numCheval + ", nomCheval="
				+ nomCheval + ", ageSexe=" + ageSexe + ", musique=" + musique + ", gains=" + gains + ", iGains="
				+ iGains + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Partant other = (Partant) obj;
		return Objects.equals(numCheval, other.numCheval);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(numCheval);
		return result;
	}

}
