package fr.ses10doigts.coursesCrawler.repository.paris;

import fr.ses10doigts.coursesCrawler.model.paris.Paris;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
     COUNT(p) as nbCourses,
     SUM(CASE WHEN p.isWin = true THEN 1 ELSE 0 END) as nbWin,
     SUM(CASE WHEN p.isWin = false THEN 1 ELSE 0 END) as nbLoose,
     SUM(CASE WHEN p.isWin = true THEN (p.gain * p.mise) ELSE 0 END) as amountWin,
     SUM(CASE WHEN p.isWin = false THEN p.mise ELSE 0 END) as amountLoose
   FROM Paris p
   WHERE (:annee IS NULL OR p.annee = :annee)
     AND (:mois IS NULL OR p.mois = :mois)
     AND (:semaine IS NULL OR p.semaine = :semaine)
     AND (:jour IS NULL OR p.jour = :jour)
""")
BilanProjection computeBilan(
		@Param("annee") Integer annee,
		@Param("mois") Integer mois,
		@Param("semaine") Integer semaine,
		@Param("jour") Integer jour
);

	@Query("SELECT p FROM Paris p WHERE p.isEnded = 0")
	List<Paris> findAllNotEnded();
}
