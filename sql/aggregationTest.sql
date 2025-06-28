-- Liste des "erreurs"
SELECT distinct 
    course.courseid as courseId,
	rapport.courseid as rapportId,
    arrivee.courseid as arriveeId,
    cote.courseid as coteId,
    partant.courseid as partantId,
    course.date as date
FROM courses.rapport 
right join courses.course on course.courseid = rapport.courseid
left join courses.arrivee on arrivee.courseid = course.courseid
left join courses.cote on cote.courseid = course.courseid
left join courses.partant on partant.courseid = course.courseid
-- where course.date > '2023-08-30'
where course.courseid is null 
    or rapport.courseid is null
    or arrivee.courseid is null
    or cote.courseid is null
    or partant.courseid is null
ORDER BY course.courseid ASC  
;

-- Nb "d'erreurs"
SELECT count(distinct course.courseid) as NbErreur
FROM courses.rapport 
right join courses.course on course.courseid = rapport.courseid
left join courses.arrivee on arrivee.courseid = course.courseid
left join courses.cote on cote.courseid = course.courseid
left join courses.partant on partant.courseid = course.courseid
where course.courseid is null 
    or rapport.courseid is null
    or arrivee.courseid is null
    or cote.courseid is null
    or partant.courseid is null
    ;



select * from courses.course;
select count(distinct courseid) from courses.course_complete;

select distinct rapport.courseid from courses.rapport where courseid = 1379439;