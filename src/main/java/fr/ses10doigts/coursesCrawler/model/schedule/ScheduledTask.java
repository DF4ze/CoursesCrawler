package fr.ses10doigts.coursesCrawler.model.schedule;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ScheduledTask {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private Long CourseID;

    @Enumerated(EnumType.STRING)
    private ScheduleStatus status; // SCHEDULED, RUNNING, SUCCESS, ERROR

	private LocalDateTime plannedExecution;
    private LocalDateTime lastExecution;
    private String errorMessage;
	private LocalDateTime creationDate;

	private String courseUrl;
	private Long telegramMessageId;
	private String courseDescription;
}