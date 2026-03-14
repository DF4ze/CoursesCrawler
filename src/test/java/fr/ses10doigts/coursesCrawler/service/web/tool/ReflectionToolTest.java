package fr.ses10doigts.coursesCrawler.service.web.tool;


import fr.ses10doigts.coursesCrawler.model.scrap.entity.CourseComplete;
import jakarta.annotation.Generated;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

@Generated(value = "org.junit-tools-1.1.0")
public class ReflectionToolTest {


    @Test
    public void testGetAllFields() {
		List<Field> fields = ReflectionTool.getListAllFields(CourseComplete.class);

		for (Field field : fields) {
			System.out.println(field.getName());
		}
    }

    @Test
    public void testGetValueOfCourseCompleteField() {
		CourseComplete cc = new CourseComplete();
		cc.setCourseID(405070L);

		String courseID = ReflectionTool.getValueOfCourseCompleteField(cc, "courseID");

		System.out.println(courseID);
    }
}