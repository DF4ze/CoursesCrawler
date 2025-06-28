package fr.ses10doigts.coursesCrawler.repository.course;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByCourseID( Long courseID );

    @Query("from Course where ?1 <= courseID ")
    List<Course> findAllFrom( Long courseID );

    @Query("from Course where ?1 <= courseID and ?2 >= courseID")
    List<Course> findAllBetween( Long courseIDStart, Long courseIDStop );

	@Query("""
			    SELECT c FROM Course c
			    JOIN Partant p ON c.courseID = p.courseID
			    WHERE c.date = :date
			    GROUP BY c.id
			    HAVING COUNT(p.id) > :minPartants
			""")
	List<Course> findCoursesWithMoreThanXPartantsOnDate(@Param("minPartants") long minPartants,
			@Param("date") String date);

}
