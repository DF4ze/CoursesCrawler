package fr.ses10doigts.coursesCrawler.repository.paris;

import fr.ses10doigts.coursesCrawler.model.paris.Paris;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ParisRepository extends JpaRepository<Paris, Long> {

	Optional<Paris> findByCourse_CourseID(Long courseID );

	@Query("""
		SELECT p
		FROM Paris p
		LEFT JOIN Paris p2 ON p = p2.parisPrecedent
		WHERE p2 IS NULL
	""")
	Optional<Paris> findLastParis();

}
