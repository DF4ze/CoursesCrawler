package fr.ses10doigts.coursesCrawler.model.course.entity;

import fr.ses10doigts.coursesCrawler.model.course.AbstractCourseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Course extends AbstractCourseEntity {

    @Id
    private Long courseID;
    private String date;
    private String hippodrome;
    private Integer reunion;
    private Integer course;
    private String prix;
    private String type;
    private String prime;
    private String depart;

    public Course() {
    }

    @Override
    public Long getId() {
	return courseID;
    }
    public void setId(Long id) {
	this.courseID = id;
    }

    @Override
    public String toString() {
	return "Course [courseID=" + courseID + ", date=" + date + ", hippodrome=" + hippodrome + ", reunion=" + reunion
		+ ", course=" + course + ", prix=" + prix + ", type=" + type + ", prime=" + prime + ", depart=" + depart
		+ "]";
    }

    @Override
    public Long getCourseID() {
	return courseID;
    }
    @Override
    public void setCourseID(Long courseID) {
	this.courseID = courseID;
    }

    public String getDate() {
	return date;
    }
    public void setDate(String date) {
	this.date = date;
    }

    public String getHippodrome() {
	return hippodrome;
    }
    public void setHippodrome(String hippodrome) {
	this.hippodrome = hippodrome;
    }

    public Integer getReunion() {
	return reunion;
    }
    public void setReunion(Integer reunion) {
	this.reunion = reunion;
    }

    public Integer getCourse() {
	return course;
    }
    public void setCourse(Integer course) {
	this.course = course;
    }

    public String getPrix() {
	return prix;
    }
    public void setPrix(String prix) {
	this.prix = prix;
    }

    public String getType() {
	return type;
    }
    public void setType(String type) {
	this.type = type;
    }

    public String getPrime() {
	return prime;
    }
    public void setPrime(String prime) {
	this.prime = prime;
    }

    public String getDepart() {
	return depart;
    }
    public void setDepart(String depart) {
	this.depart = depart;
    }

}
