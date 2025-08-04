package fr.ses10doigts.coursesCrawler.repository.course;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.Cote;

public interface CoteRepository extends JpaRepository<Cote, Long>{

    Set<Cote> findByCourseID( Long courseID );

    Optional<Cote> findByCourseIDAndNumCheval( Long courseID, Integer numCheval );
}
