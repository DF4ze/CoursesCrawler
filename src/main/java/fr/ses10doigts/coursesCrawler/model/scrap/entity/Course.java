package fr.ses10doigts.coursesCrawler.model.scrap.entity;

import java.util.Objects;

import fr.ses10doigts.coursesCrawler.model.scrap.AbstractCourseEntity;
import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@Data
public class Course extends AbstractCourseEntity {

    private String  date;
	private String heures;
	private String minutes;
    private String  hippodrome;
    private Integer reunion;
    private Integer course;
    private String  prix;
    private String  type;
    private String  prime;
    private String depart;

    public Course() {
    }

    @Override
    public String toString() {
	return "Course [courseID=" + getCourseID() + ", date=" + date + ", hippodrome=" + hippodrome + ", reunion="
		+ reunion + ", course=" + course + ", prix=" + prix + ", type=" + type + ", prime=" + prime
		+ ", depart=" + depart + "]";
    }

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Course other = (Course) obj;
		return Objects.equals(course, other.course) && Objects.equals(date, other.date)
				&& Objects.equals(hippodrome, other.hippodrome) && Objects.equals(reunion, other.reunion)
				&& Objects.equals(type, other.type);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(course, date, hippodrome, reunion, type);
		return result;
	}


}
