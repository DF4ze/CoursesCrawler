package fr.ses10doigts.coursesCrawler.model.scrap;

import java.util.Objects;

import jakarta.persistence.MappedSuperclass;
import lombok.Data;

@MappedSuperclass
@Data
public abstract class AbstractCourseEntity extends AbstractEntity {

    private String url;
    private Long   courseID;

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractCourseEntity other = (AbstractCourseEntity) obj;
		return Objects.equals(courseID, other.courseID) && Objects.equals(url, other.url);
	}

	@Override
	public int hashCode() {
		return Objects.hash(courseID, url);
	}




}
