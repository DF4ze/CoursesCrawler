package fr.ses10doigts.coursesCrawler.service.scheduler;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.Course;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Cote;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Partant;
import fr.ses10doigts.coursesCrawler.repository.course.CoteRepository;
import fr.ses10doigts.coursesCrawler.repository.course.PartantRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseFilterServiceTest {

    private final PartantRepository partantRepository = mock(PartantRepository.class);
    private final CoteRepository coteRepository = mock(CoteRepository.class);
    private final CourseFilterService service = new CourseFilterService(partantRepository, coteRepository);

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

    @Test
    void evaluateAuthorizedAgesMustOnlyUseStarterListWhenAvailable() {
        Course course = new Course();
        course.setUrl("test://course");
        course.setCourseID(42L);

        Partant starter = partant("5M");
        starter.setCourseID(42L);
        starter.setNumCheval(1);
        starter.setProbableGeny(null);
        starter.setProbablePMU(null);

        Partant nonStarter = partant("10F");
        nonStarter.setCourseID(42L);
        nonStarter.setNumCheval(2);
        nonStarter.setProbableGeny(9f);

        Cote coteStarter = new Cote();
        coteStarter.setCourseID(42L);
        coteStarter.setNumCheval(1);
        coteStarter.setValide(true);

        when(partantRepository.findByCourseID(42L)).thenReturn(Set.of(starter, nonStarter));
        when(coteRepository.findByCourseID(42L)).thenReturn(Set.of(coteStarter));

        CourseFilterService.AgeCheckResult result = service.evaluateAuthorizedAges(course, List.of("5-5"));

        assertTrue(result.matches());
        assertEquals(5, result.ageMin());
        assertEquals(5, result.ageMax());
    }

    private static int numCheval = 0;
    private static long courseId = 0;
    private Partant partant(String ageSexe) {
        Partant partant = new Partant();
        partant.setNumCheval(numCheval++);
        partant.setCourseID(courseId++);
        partant.setAgeSexe(ageSexe);
        partant.setProbableGeny(9f);
        return partant;
    }
}
