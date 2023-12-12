package fr.ses10doigts.coursesCrawler.repository.course;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.Rapport;

public interface RapportRepository extends JpaRepository<Rapport, Long> {

	Set<Rapport> findAllByCourseID(Long courseID);
}
