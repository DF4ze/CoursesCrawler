package fr.ses10doigts.coursesCrawler.service.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.CourseComplete;
import fr.ses10doigts.coursesCrawler.repository.course.CourseCompleteRepository;
import fr.ses10doigts.coursesCrawler.service.web.tool.ReflectionTool;

@Component
public class ExcelExtractorService {

    private static final String	     COURSES = "Courses";
    //    private static final SimpleDateFormat sdf	  = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");

    private static final Logger	     logger  = LoggerFactory.getLogger(ExcelExtractorService.class);

    @Autowired
	private CourseCompleteRepository ccRepo;

	/**
	 * Read the "coursesComplete" DB and write an Excel file
	 *
	 * @return String with the path of the file
	 */
	public String extractCourseCompletes() {
		Workbook workbook = prepareExcelWorkBook();

		workbook = feedFromDB(workbook);

		String fileName = filenameGenerator();
		if (!writeToFile(workbook, fileName)) {
			fileName = null;
		} else {
			logger.info("Excel file successfully generated");
		}

		return fileName;
	}

	private Workbook prepareExcelWorkBook() {
		Workbook workbook = new XSSFWorkbook();

		Sheet sheet = workbook.createSheet(COURSES);
		// sheet.setColumnWidth(0, 6000);
		// sheet.setColumnWidth(1, 4000);
		// ...

		Row header = sheet.createRow(0);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFillForegroundColor(IndexedColors.AQUA.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		XSSFFont font = ((XSSFWorkbook) workbook).createFont();
		font.setFontName("Arial");
		font.setFontHeightInPoints((short) 8);
		font.setBold(true);
		headerStyle.setFont(font);

		header = generateHeaderFromCourseComplete(header, headerStyle);

		return workbook;
	}

	private Row generateHeaderFromCourseComplete(Row header, CellStyle headerStyle) {
		List<String> fieldsName = ReflectionTool.getDesirableCourseCompleteFields();

		// Capitalize fields
		List<String> capFieldsName = new ArrayList<>();
		for (String fieldName : fieldsName) {
			capFieldsName.add(fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
		}

		int i = 0;
		for (String field : capFieldsName) {
			Cell headerCell = header.createCell(i++);
			headerCell.setCellValue(field);
			headerCell.setCellStyle(headerStyle);
		}

		return header;
    }

    private Row generateRowFromCourseComplete(CourseComplete cc, Sheet sheet, CellStyle style, int line) {

		Row row = sheet.createRow(line);

		List<String> fields = ReflectionTool.getDesirableCourseCompleteFields();
		int i = 0;
		for (String fieldName : fields) {
			String value = ReflectionTool.getValueOfCourseCompleteField(cc, fieldName);

			Cell cell = row.createCell(i++);
			cell.setCellValue(value);
			cell.setCellStyle(style);
		}

		return row;
    }

    private Workbook feedFromDB(Workbook workbook) {
		// List<CourseComplete> findAll = ccRepo.findAllOrderByDateCourseAsc();
		List<CourseComplete> findAll = ccRepo
				.findAll(Sort.by(Sort.Direction.ASC, "dateCourse", "numeroReunion", "numeroCourse"));

		CellStyle style = workbook.createCellStyle();
		style.setWrapText(true);

		int i = 1; // leave a place for the header
		for (CourseComplete cc : findAll) {
			generateRowFromCourseComplete(cc, workbook.getSheet(COURSES), style, i++);
		}

		return workbook;
	}

	private boolean writeToFile(Workbook wb, String filename) {
		File currDir = new File(".");
		String path = currDir.getAbsolutePath();
		// String path2 = currDir.getAbsolutePath() +
		// "\\src\\main\\resources\\static\\";
		String fileLocation = path.substring(0, path.length() - 1) + filename;
		String fileLocation2 = path.substring(0, path.length() - 1) + "src\\main\\resources\\static\\" + filename;

		FileOutputStream outputStream;
		FileOutputStream outputStream2;
		boolean writeStatus = true;
		try {
			outputStream = new FileOutputStream(fileLocation);
			outputStream2 = new FileOutputStream(fileLocation2);

			wb.write(outputStream);
			wb.write(outputStream2);
			wb.close();
		} catch (IOException e) {
			logger.error("Error writing Excel file : " + e.getMessage());

			writeStatus = false;
		}
		return writeStatus;
	}

	private String filenameGenerator() {
		String fileName = "courses.xlsx";
		// return sdf.format(new Date()) + fileName;
		return fileName;
    }
}
