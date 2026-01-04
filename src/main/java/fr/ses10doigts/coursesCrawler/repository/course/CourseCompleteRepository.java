package fr.ses10doigts.coursesCrawler.repository.course;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.CourseComplete;

public interface CourseCompleteRepository extends JpaRepository<CourseComplete, Long> {

	@Override
	List<CourseComplete> findAll(Sort sort);

	@Query("SELECT e FROM CourseComplete e")
    Stream<CourseComplete> streamAll();
}
