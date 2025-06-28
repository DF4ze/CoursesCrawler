package fr.ses10doigts.coursesCrawler.repository.course;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.Partant;
import jakarta.transaction.Transactional;

public interface PartantRepository extends JpaRepository<Partant, Long> {

    Set<Partant> findByCourseID( Long courseID );

	@Modifying
	@Transactional
	@Query(value = """
			INSERT INTO partant (courseid, url, age_sexe, gains, i_gains, musique, nom_cheval, num_cheval, probable_geny, probablepmu)
			VALUES ( :#{#p.courseID}, :#{#p.url}, :#{#p.ageSexe}, :#{#p.gains}, :#{#p.iGains}, :#{#p.musique}, :#{#p.nomCheval}, :#{#p.numCheval}, :#{#p.probableGeny}, :#{#p.probablePMU})
			ON DUPLICATE KEY UPDATE
			  url = IFNULL(VALUES(url), url),
			  age_sexe = IFNULL(VALUES(age_sexe), age_sexe),
			  gains = IFNULL(VALUES(gains), gains),
			  i_gains = IFNULL(VALUES(i_gains), i_gains),
			  musique = IFNULL(VALUES(musique), musique),
			  nom_cheval = IFNULL(VALUES(nom_cheval), nom_cheval),
			  probable_geny = IFNULL(VALUES(probable_geny), probable_geny),
			  probablepmu = IFNULL(VALUES(probablepmu), probablepmu)
			""", nativeQuery = true)
	void saveOrUpdate(@Param("p") Partant partant);
}
