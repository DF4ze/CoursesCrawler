package fr.ses10doigts.coursesCrawler.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.ses10doigts.coursesCrawler.model.web.ScheduledTask;
import fr.ses10doigts.coursesCrawler.model.web.Status;
import jakarta.transaction.Transactional;

@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long> {

	@Modifying
	@Transactional
	@Query("UPDATE ScheduledTask t SET t.status = :status, t.lastExecution = :lastExec, t.errorMessage = :errorMsg WHERE t.id = :taskId")
	void updateStatus(@Param("taskId") Long taskId, @Param("status") Status status,
			@Param("lastExec") LocalDateTime lastExecution, @Param("errorMsg") String errorMessage);

}
