package fr.ses10doigts.coursesCrawler.service.scheduler;

import fr.ses10doigts.coursesCrawler.CustomProperties;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Cote;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Course;
import fr.ses10doigts.coursesCrawler.model.scrap.entity.Partant;
import fr.ses10doigts.coursesCrawler.repository.course.CoteRepository;
import fr.ses10doigts.coursesCrawler.repository.course.PartantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseFilterService {
    private static final Pattern AGE_PATTERN = Pattern.compile("(\\d+)");
    private static final Pattern AGE_RANGE_PATTERN = Pattern.compile("\\s*(\\*|\\d+)\\s*([~-])\\s*(\\*|\\d+)\\s*");

    private final PartantRepository partantRepository;
    private final CoteRepository coteRepository;

    public boolean matches(Course course, CustomProperties props) {
        if (!matchesWithoutAges(course, props)) {
            return false;
        }

        Set<Partant> partants = partantRepository.findByCourseID(course.getCourseID());
        return matchesAuthorizedAges(course, partants, props.getFilterListAuthorizedAges());
    }

    public boolean matchesWithoutAges(Course course, CustomProperties props) {
        if (!matchesCourseType(course, props.getFilterTypeCourse())) {
            return false;
        }

        if (!matchesReunion(course, props.getFilterNbReunionMax())) {
            return false;
        }

        if (!matchesHippodrome(course, props.getFilterListAcceptedHippo())) {
            return false;
        }

        Set<Partant> partants = partantRepository.findByCourseID(course.getCourseID());
        return matchesMinPartants(course, partants, props.getFilterMinPartants());
    }

    private boolean matchesCourseType(Course course, String expectedType) {
        boolean ok = expectedType != null && expectedType.equalsIgnoreCase(course.getType());
        if (!ok) {
            log.warn("Current course({}, {}) removed because type '{}' != expected '{}'",
                    course.getUrl(), course.getHippodrome(), course.getType(), expectedType);
        }
        return ok;
    }

    private boolean matchesReunion(Course course, Integer maxReunion) {
        boolean ok = maxReunion != null && course.getReunion() <= maxReunion;
        if (!ok) {
            log.warn("Current course({}, {}) removed because reunion {} > max {}",
                    course.getUrl(), course.getHippodrome(), course.getReunion(), maxReunion);
        }
        return ok;
    }

    private boolean matchesHippodrome(Course course, List<String> acceptedHippos) {
        String hippo = Optional.ofNullable(course.getHippodrome()).orElse("").toLowerCase();

        boolean ok = Optional.ofNullable(acceptedHippos).orElse(List.of())
                .stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .anyMatch(hippo::contains);

        if (acceptedHippos == null || acceptedHippos.isEmpty()) {
            return true;
        }

        if (!ok) {
            log.warn("Current course({}, {}) removed because hippo is not whitelisted",
                    course.getUrl(), course.getHippodrome());
        }

        return ok;
    }

    private boolean matchesMinPartants(Course course, Set<Partant> partants, Integer minPartants) {
        if (minPartants == null) {
            return true;
        }

        long count = Optional.ofNullable(partants).orElse(Set.of())
                .stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getProbableGeny() != null)
                .count();

        boolean ok = count >= minPartants;
        if (!ok) {
            log.warn("Current course({}, {}) removed because partants {} < min {}",
                    course.getUrl(), course.getHippodrome(), count, minPartants);
        }

        return ok;
    }

    boolean matchesAuthorizedAges(Course course, Set<Partant> partants, List<String> configuredRanges) {
        AgeCheckResult result = evaluateAuthorizedAges(course, partants, configuredRanges);
        return result.matches();
    }

    public AgeCheckResult evaluateAuthorizedAges(Course course, List<String> configuredRanges) {
        Set<Partant> partants = partantRepository.findByCourseID(course.getCourseID());
        return evaluateAuthorizedAges(course, partants, configuredRanges);
    }

    private AgeCheckResult evaluateAuthorizedAges(Course course, Set<Partant> partants, List<String> configuredRanges) {
        List<AgeRangeFilter> authorizedRanges = Optional.ofNullable(configuredRanges).orElse(List.of()).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::parseAgeRange)
                .filter(Objects::nonNull)
                .toList();

        if (authorizedRanges.isEmpty()) {
            return new AgeCheckResult(false, true, null, null, "Aucun filtre d'age");
        }

        if (partants == null || partants.isEmpty()) {
            log.warn("No partants found for course {}, age filter rejects it", course.getUrl());
            return new AgeCheckResult(true, false, null, null, "Ages indisponibles (partants manquants)");
        }

        Set<Integer> starterNumbers = findStarterNumbers(course.getCourseID());
        boolean startersFromCotes = !starterNumbers.isEmpty();

        List<Integer> ages = partants.stream()
                .filter(Objects::nonNull)
                .filter(p -> isStarterForAgeCheck(p, starterNumbers, startersFromCotes))
                .map(Partant::getAgeSexe)
                .map(this::extractAge)
                .filter(Objects::nonNull)
                .toList();

        if (ages.isEmpty()) {
            log.warn("No starter age found for course {}, age filter rejects it", course.getUrl());
            return new AgeCheckResult(true, false, null, null, "Ages indisponibles (partants manquants)");
        }

        int ageMin = ages.stream().min(Comparator.naturalOrder()).orElseThrow();
        int ageMax = ages.stream().max(Comparator.naturalOrder()).orElseThrow();

        boolean ok = authorizedRanges.stream().anyMatch(range -> range.matches(ageMin, ageMax));
        if (!ok) {
            log.warn("Current course({}, {}) removed because age span {}-{} does not match configured ranges {}",
                    course.getUrl(), course.getHippodrome(), ageMin, ageMax, configuredRanges);
        }

        return new AgeCheckResult(true, ok, ageMin, ageMax, ok ? "Ages autorises" : "Ages hors plage");
    }

    private Set<Integer> findStarterNumbers(Long courseId) {
        if (courseId == null) {
            return Set.of();
        }

        return Optional.ofNullable(coteRepository.findByCourseID(courseId)).orElse(Set.of()).stream()
                .filter(Objects::nonNull)
                .filter(cote -> !Boolean.FALSE.equals(cote.getValide()))
                .map(Cote::getNumCheval)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean isStarterForAgeCheck(Partant partant, Set<Integer> starterNumbers, boolean startersFromCotes) {
        if (startersFromCotes) {
            return starterNumbers.contains(partant.getNumCheval());
        }

        return partant.getProbableGeny() != null || partant.getProbablePMU() != null;
    }

    private Integer extractAge(String ageSexe) {
        if (ageSexe == null || ageSexe.isBlank()) {
            return null;
        }

        Matcher matcher = AGE_PATTERN.matcher(ageSexe);
        if (!matcher.find()) {
            return null;
        }

        return Integer.parseInt(matcher.group(1));
    }

    private AgeRangeFilter parseAgeRange(String rawRange) {
        Matcher matcher = AGE_RANGE_PATTERN.matcher(rawRange);
        if (!matcher.matches()) {
            log.warn("Ignoring invalid age range configuration: {}", rawRange);
            return null;
        }

        Integer min = parseBound(matcher.group(1));
        Integer max = parseBound(matcher.group(3));
        AgeRangeMode mode = "-".equals(matcher.group(2)) ? AgeRangeMode.STRICT : AgeRangeMode.LARGE;

        if (min != null && max != null && min > max) {
            log.warn("Ignoring invalid age range configuration (min > max): {}", rawRange);
            return null;
        }

        return new AgeRangeFilter(min, max, mode);
    }

    private Integer parseBound(String rawBound) {
        return "*".equals(rawBound) ? null : Integer.parseInt(rawBound);
    }

    enum AgeRangeMode { STRICT, LARGE }

    record AgeRangeFilter(Integer min, Integer max, AgeRangeMode mode) {
        boolean matches(int courseAgeMin, int courseAgeMax) {
            boolean minMatch = min == null || courseAgeMin == min;
            boolean maxMatch;

            if (max == null) {
                maxMatch = true;
            } else if (mode == AgeRangeMode.STRICT) {
                maxMatch = courseAgeMax == max;
            } else {
                maxMatch = courseAgeMax <= max;
            }

            return minMatch && maxMatch;
        }
    }

    public record AgeCheckResult(boolean filterEnabled, boolean matches, Integer ageMin, Integer ageMax, String reason) {}
}
