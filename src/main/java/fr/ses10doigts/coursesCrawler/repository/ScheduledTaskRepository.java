package fr.ses10doigts.coursesCrawler.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.ses10doigts.coursesCrawler.model.schedule.ScheduledTask;
import fr.ses10doigts.coursesCrawler.model.schedule.ScheduleStatus;
import jakarta.transaction.Transactional;

@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long> {

	@Modifying
	@Transactional
	@Query("UPDATE ScheduledTask t " +
			"SET t.status = :status, " +
			"t.lastExecution = :lastExec, " +
			"t.errorMessage = :errorMsg " +
			"WHERE t.id = :taskId")
	void updateStatus(
			@Param("taskId") Long taskId,
			@Param("status") ScheduleStatus status,
			@Param("lastExec") LocalDateTime lastExecution,
			@Param("errorMsg") String errorMessage
	);

	boolean existsByCourseUrl(String courseUrl);

	Optional<ScheduledTask> findByCourseUrl( String courseUrl );

	@Query("SELECT s FROM ScheduledTask s " +
			"WHERE s.status = :status " +
			"AND s.plannedExecution >= :now " +
			"ORDER BY s.plannedExecution ASC")
	List<ScheduledTask> findScheduledTasksFromNow(
			@Param("status") ScheduleStatus status,
			@Param("now") LocalDateTime now
	);

	@Query("SELECT s FROM ScheduledTask s " +
			"WHERE s.status = :status " +
			"AND s.plannedExecution BETWEEN :start AND :end " +
			"ORDER BY s.plannedExecution ASC")
	List<ScheduledTask> findScheduledTasksWithinWindow(
			@Param("status") ScheduleStatus status,
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end
	);

	@Query("SELECT s FROM ScheduledTask s " +
			"WHERE s.status = :status " +
			"AND s.plannedExecution < :before " +
			"ORDER BY s.plannedExecution ASC")
	List<ScheduledTask> findOldScheduledTasks(
			@Param("status") ScheduleStatus status,
			@Param("before") LocalDateTime before
	);
}
