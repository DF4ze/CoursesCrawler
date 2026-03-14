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

        Set<Partant> partants = Set.of(partant("5 M"), partant("7 F"));

        assertFalse(service.matchesAuthorizedAges(course, partants, List.of("5-10")));
        assertTrue(service.matchesAuthorizedAges(course, Set.of(partant("5 M"), partant("10 F")), List.of("5-10")));
    }

    @Test
    void largeRangeMustMatchExactMinAndMaxBelowCeiling() {
        Course course = new Course();
        course.setUrl("test://course");

        assertTrue(service.matchesAuthorizedAges(course, Set.of(partant("5 M"), partant("7 F")), List.of("5~10")));
        assertFalse(service.matchesAuthorizedAges(course, Set.of(partant("6 M"), partant("7 F")), List.of("5~10")));
        assertFalse(service.matchesAuthorizedAges(course, Set.of(partant("5 M"), partant("11 F")), List.of("5~10")));
    }

    @Test
    void wildcardWorksForMinOrMax() {
        Course course = new Course();
        course.setUrl("test://course");

        Set<Partant> partants = Set.of(partant("5 M"), partant("11 F"));
        assertTrue(service.matchesAuthorizedAges(course, partants, List.of("5-*")));
        assertTrue(service.matchesAuthorizedAges(course, Set.of(partant("3 M"), partant("5 F")), List.of("*~5")));
        assertFalse(service.matchesAuthorizedAges(course, Set.of(partant("6 M"), partant("11 F")), List.of("5-*")));
    }

    private Partant partant(String ageSexe) {
        Partant partant = new Partant();
        partant.setAgeSexe(ageSexe);
        return partant;
    }
}
