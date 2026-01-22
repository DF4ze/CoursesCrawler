SELECT c.courseid, r.num_cheval, r.gagnant_geny, r.place_geny
FROM courses.course c
left join rapport r on r.courseid = c.courseid
where c.courseid = 1621283;

SELECT 
    count( distinct c.courseid )
    -- ,CASE WHEN r.courseid IS NULL THEN 1 ELSE 0 END AS rapport_manquant
    -- ,CASE WHEN co.courseid IS NULL THEN 1 ELSE 0 END AS cote_manquante
FROM course c
LEFT JOIN rapport r ON r.courseid = c.courseid
LEFT JOIN cote co ON co.courseid = c.courseid
WHERE r.courseid IS NULL
   OR co.courseid IS NULL;

SELECT DISTINCT
    c.courseid,
    CASE WHEN r.courseid IS NULL THEN 1 ELSE 0 END AS rapport_manquant,
    CASE WHEN co.courseid IS NULL THEN 1 ELSE 0 END AS cote_manquante,
    CASE 
        WHEN r.courseid IS NULL 
        THEN CONCAT('https://www.geny.com/arrivee-et-rapports-pmu?id_course=', c.courseid)
        ELSE NULL
    END AS url_rapport,
    CASE 
        WHEN co.courseid IS NULL 
        THEN CONCAT('https://www.geny.com/cotes?id_course=', c.courseid)
        ELSE NULL
    END AS url_cote
FROM course c
LEFT JOIN rapport r ON r.courseid = c.courseid
LEFT JOIN cote co ON co.courseid = c.courseid
WHERE r.courseid IS NULL
   OR co.courseid IS NULL;


   		SELECT * FROM course c
		JOIN partant p ON c.courseid = p.courseid
		WHERE c.date = "2026-01-20"
		  AND c.type = "plat"
		  AND c.reunion <= 5
		GROUP BY c.id
		HAVING COUNT(CASE WHEN p.probable_geny IS NOT NULL THEN 1 END) >= 10;
        
SELECT 
    c.*,
    COUNT(CASE WHEN p.probable_geny IS NOT NULL THEN 1 END) AS nb_probables
FROM course c
JOIN partant p ON c.courseid = p.courseid
WHERE c.date = '2026-01-20'
  AND c.type = 'plat'
  AND c.reunion <= 5
GROUP BY c.id
HAVING nb_probables >= 10;

SELECT 
    c.*,
    COUNT(CASE WHEN p.probable_geny IS NOT NULL THEN 1 END) AS nb_probables
FROM course c
JOIN partant p ON c.courseid = p.courseid
WHERE c.date = '2026-01-20'
  AND c.type = 'plat'
  AND c.reunion <= 5
GROUP BY c.id;