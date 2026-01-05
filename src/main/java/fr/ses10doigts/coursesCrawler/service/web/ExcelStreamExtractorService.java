package fr.ses10doigts.coursesCrawler.service.web;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.CourseComplete;
import fr.ses10doigts.coursesCrawler.repository.course.CourseCompleteRepository;
import fr.ses10doigts.coursesCrawler.service.web.tool.ReflectionTool;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ExcelStreamExtractorService {
    @Autowired
    private CourseCompleteRepository repository;
    @Value("${fr.ses10doigts.crawler.export-dir}")
    private String exportDir;

    private static final Logger logger  = LoggerFactory.getLogger(ExcelStreamExtractorService.class);
    private static final String SHEET_NAME = "Courses";

    @Transactional(readOnly = true) // important pour le streaming
    public void extractCourseCompletes()  {
        try {
            String sFile = exportDir + "courses.xlsx";
            logger.info("Starting generating Excel file to : {}", sFile);
            try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) { // buffer 100 lignes

                Sheet sheet = workbook.createSheet(SHEET_NAME);

                // En-tête
                generateHeaderFromCourseComplete(workbook);

                // Corps
                int[] rowNum = {1};

                CellStyle style = workbook.createCellStyle();
                style.setWrapText(true);

                try (Stream<CourseComplete> stream = repository.streamAll()) {
                    stream.forEach(entity -> {
                        generateRowFromCourseComplete(entity, sheet, style, rowNum[0]++);
                    });
                }

                try (FileOutputStream out = new FileOutputStream( sFile )) {
                    workbook.write(out);
                }
            }
            logger.info("Generation successful");

        }catch (Exception e){
            logger.error("Error extracting to Excel file : {}", e.getMessage());
        }
    }

    private void generateRowFromCourseComplete(CourseComplete cc, Sheet sheet, CellStyle style, int line) {

        Row row = sheet.createRow(line);

        List<String> fields = ReflectionTool.getDesirableCourseCompleteFields();
        int i = 0;
        for (String fieldName : fields) {
            String value = ReflectionTool.getValueOfCourseCompleteField(cc, fieldName);

            Cell cell = row.createCell(i++);
            cell.setCellValue(value);
            cell.setCellStyle(style);
        }

    }

    private void generateHeaderFromCourseComplete(SXSSFWorkbook workbook) {
        SXSSFSheet sheet = workbook.getSheet(SHEET_NAME);
        Row header = sheet.createRow(0);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.AQUA.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 8);
        font.setBold(true);
        headerStyle.setFont(font);

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
    }


}
