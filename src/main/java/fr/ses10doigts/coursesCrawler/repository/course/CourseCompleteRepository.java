package fr.ses10doigts.coursesCrawler.repository.course;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.CourseComplete;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.stream.Stream;

public interface CourseCompleteRepository extends JpaRepository<CourseComplete, Long> {

	@NotNull
	@Override
	List<CourseComplete> findAll(@NotNull Sort sort);

	@Query("""
    SELECT e
    FROM CourseComplete e
    ORDER BY
        e.dateCourse ASC,
        e.hour ASC,
        e.minute ASC
""")
    Stream<CourseComplete> streamAll();
}
