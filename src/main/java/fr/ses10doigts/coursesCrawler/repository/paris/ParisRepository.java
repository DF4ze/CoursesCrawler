package fr.ses10doigts.coursesCrawler.repository.paris;

import fr.ses10doigts.coursesCrawler.model.paris.Paris;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	@Query("""
        SELECT
            COUNT(p),
            SUM(CASE WHEN p.isWin = true THEN 1L ELSE 0L END),
            SUM(CASE WHEN p.isWin = false THEN 1L ELSE 0L END),
            SUM(CASE WHEN p.isWin = true THEN p.gain ELSE 0L END),
            SUM(CASE WHEN p.isWin = false THEN p.mise ELSE 0L END)
        FROM Paris p
        WHERE (:annee IS NULL OR p.annee = :annee)
          AND (:mois IS NULL OR p.mois = :mois)
          AND (:semaine IS NULL OR p.semaine = :semaine)
    """)
	Object[] computeBilan(
			@Param("annee") Integer annee,
			@Param("mois") Integer mois,
			@Param("semaine") Integer semaine
	);
}
