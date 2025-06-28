package fr.ses10doigts.coursesCrawler.repository.course;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.CourseComplete;

public interface CourseCompleteRepository extends JpaRepository<CourseComplete, Long> {

//    List<CourseComplete> findAllOrderByDateCourseAsc();
	@Override
	List<CourseComplete> findAll(Sort sort);

	@Query("SELECT c FROM CourseComplete c WHERE c.dateCourse = :today AND c.nombrePartant > :nbPartant")
	List<CourseComplete> findTodayWithMoreThanTenPartants(@Param("today") String today,
			@Param("nbPartant") int nbPartant);

}
