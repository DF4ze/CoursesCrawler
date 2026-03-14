package fr.ses10doigts.coursesCrawler.service.scheduler;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.Course;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Partant;
import fr.ses10doigts.coursesCrawler.repository.course.PartantRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CourseFilterServiceTest {

    private final CourseFilterService service = new CourseFilterService(mock(PartantRepository.class));

    @Test
    void strictRangeMustMatchExactMinAndMax() {
        Course course = new Course();
        course.setUrl("test://course");


        assertFalse(
                service.matchesAuthorizedAges(course,
                        Set.of(partant("5M"), partant("7F")),
                        List.of("5-10"))
        );
        assertTrue(
                service.matchesAuthorizedAges(course,
                        Set.of(partant("5M"), partant("10F")),
                        List.of("5-10"))
        );
    }

    @Test
    void largeRangeMustMatchExactMinAndMaxBelowCeiling() {
        Course course = new Course();
        course.setUrl("test://course");

        assertTrue(service.matchesAuthorizedAges(course,
                Set.of(partant("5M"), partant("7F")),
                List.of("5~10"))
        );
        assertFalse(service.matchesAuthorizedAges(course,
                Set.of(partant("6M"), partant("7F")),
                List.of("5~10"))
        );
        assertFalse(service.matchesAuthorizedAges(course,
                Set.of(partant("5M"), partant("11F")),
                List.of("5~10"))
        );
    }

    @Test
    void wildcardWorksForMinOrMax() {
        Course course = new Course();
        course.setUrl("test://course");

        assertTrue(service.matchesAuthorizedAges(course,
                Set.of(partant("5M"), partant("11F")),
                List.of("5-*"))
        );
        assertTrue(service.matchesAuthorizedAges(course,
                Set.of(partant("5M"), partant("11F")),
                List.of("*-11"))
        );
        assertFalse(service.matchesAuthorizedAges(course,
                Set.of(partant("6M"), partant("11F")),
                List.of("5-*"))
        );
        assertFalse(service.matchesAuthorizedAges(course,
                Set.of(partant("5M"), partant("10F")),
                List.of("*-11"))
        );


        assertTrue(service.matchesAuthorizedAges(course,
                Set.of(partant("5M"), partant("6F")),
                List.of("5~*"))
        );
        assertTrue(service.matchesAuthorizedAges(course,
                Set.of(partant("6M"), partant("10F")),
                List.of("*~11"))
        );
        assertFalse(service.matchesAuthorizedAges(course,
                Set.of(partant("6M"), partant("11F")),
                List.of("5~*"))
        );
        assertFalse(service.matchesAuthorizedAges(course,
                Set.of(partant("6M"), partant("11F")),
                List.of("*~10"))
        );
    }

    private static int numCheval = 0;
    private static long courseId = 0;
    private Partant partant(String ageSexe) {
        Partant partant = new Partant();
        partant.setNumCheval(numCheval++);
        partant.setCourseID(courseId++);
        partant.setAgeSexe(ageSexe);
        return partant;
    }
}
