package fr.ses10doigts.coursesCrawler.repository.course;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.CourseComplete;

public interface CourseCompleteRepository extends JpaRepository<CourseComplete, Long> {

//    List<CourseComplete> findAllOrderByDateCourseAsc();
	@Override
	List<CourseComplete> findAll(Sort sort);
}
