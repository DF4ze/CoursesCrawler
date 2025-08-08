package fr.ses10doigts.coursesCrawler.model.schedule;

import java.time.LocalDateTime;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.Course;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ScheduledTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Long idCourse;
    private LocalDateTime courseStart;

    @ManyToOne(optional = false)
    @JoinColumn(name = "courseid", nullable = false)
    private Course course;

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