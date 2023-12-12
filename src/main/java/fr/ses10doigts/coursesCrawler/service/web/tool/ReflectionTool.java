package fr.ses10doigts.coursesCrawler.service.web.tool;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.CourseComplete;

public class ReflectionTool {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionTool.class);

    /**
     * Return a list of all fields of a given Class
     *
     * @param clazz
     * @return
     */
    public static List<Field> getListAllFields(@SuppressWarnings("rawtypes") Class clazz) {
	if (clazz == null) {
	    return Collections.emptyList();
	}

	List<Field> result = new ArrayList<>(getListAllFields(clazz.getSuperclass()));
	List<Field> filteredFields = Arrays
		.stream(clazz.getDeclaredFields()).filter(f -> Modifier.isPublic(f.getModifiers())
			|| Modifier.isProtected(f.getModifiers()) || Modifier.isPrivate(f.getModifiers()))
		.collect(Collectors.toList());
	result.addAll(filteredFields);
	return result;
    }

    /**
     * Return an field name indexed map with all the fields of a given Class
     *
     * @param clazz
     * @return
     */
    public static Map<String, Field> getMapAllFields(@SuppressWarnings("rawtypes") Class clazz) {
	Map<String, Field> mReturn = new HashMap<>();
	if (clazz == null) {
	    return mReturn;
	}

	List<Field> list = getListAllFields(clazz);
	for (Field field : list) {
	    mReturn.put(field.getName(), field);
	}

	return mReturn;
    }

    /**
     * Return all fields name of {@link CourseComplete} without technical fields
     *
     * @return
     */
    public static List<String> getDesirableCourseCompleteFields() {
		// Retrieve all fields
		List<Field> fields = getListAllFields(CourseComplete.class);
		// Extract only names
		List<String> fieldsName = new ArrayList<>();
		for (Field field : fields) {
			fieldsName.add(field.getName());
		}

		// remove unwanted fields
		List<String> unwantedFields = new ArrayList<>();
		unwantedFields.add("id");
		unwantedFields.add("serialVersionUID");
		unwantedFields.add("url");
		unwantedFields.add("courseID");
		fieldsName.removeAll(unwantedFields);

		return fieldsName;
    }

    public static String getValueOfCourseCompleteField(CourseComplete cc, String fieldName) {
	String sReturn = null;
	try {
	    Map<String, Field> fields = getMapAllFields(cc.getClass());
	    Field field = fields.get(fieldName);
	    if (field == null) {
		throw new Exception("Field " + fieldName + " not found in CC object");
	    }

	    field.setAccessible(true);
	    Object value = field.get(cc);

	    sReturn = (value == null ? "" : value) + "";

	} catch (Exception e) {
	    logger.warn("Error during Reflection reading of CourseComplete field : " + e.getMessage());
	}


	return sReturn;
    }
}
