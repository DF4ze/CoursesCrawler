package fr.ses10doigts.coursesCrawler.service.web;

import fr.ses10doigts.coursesCrawler.model.scrap.entity.CourseComplete;
import fr.ses10doigts.coursesCrawler.repository.course.CourseCompleteRepository;
import fr.ses10doigts.coursesCrawler.service.web.tool.ReflectionTool;
import jakarta.persistence.EntityManager;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class ExcelStreamExtractorService {
    @Autowired
    private CourseCompleteRepository repository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ConfigurationService configurationService;

    @Value("${fr.ses10doigts.crawler.export-dir}")
    private String exportDir;
    private static final String SHEET_NAME = "Courses";

    public void waitAndExtract(Thread crawlTreatment, Thread refactoThread) {
        try {
            if( crawlTreatment != null && crawlTreatment.isAlive()){
                crawlTreatment.join();
            }

            if( refactoThread != null && refactoThread.isAlive() ){
                refactoThread.join();
            }

            extractCourseCompletes(
                    configurationService.getConfiguration().getStartGenDate(),
                    configurationService.getConfiguration().getEndGenDate()
            );
        } catch (InterruptedException ignored) {
            ;
        }
    }

    @Transactional(readOnly = true) // important pour le streaming
    public void extractCourseCompletes( String startDate, String endDate)  {
        try {
            String sFile = exportDir + "courses.xlsx";
            log.info("Starting generating Excel from {} to {}, into file: {}", startDate, endDate, sFile);
            try (SXSSFWorkbook workbook = new SXSSFWorkbook(50)) { // buffer 100 lignes

                SXSSFSheet sheet = workbook.createSheet(SHEET_NAME);

                // En-tête
                generateHeaderFromCourseComplete(workbook);

                // Corps
                int[] rowNum = {1};

                CellStyle style = workbook.createCellStyle();
                style.setWrapText(true);

                if( startDate == null && endDate == null ) {
                    try (Stream<CourseComplete> stream = repository.streamAll()) {
                        stream.forEach(entity -> {
                            generateRowFromCourseComplete(entity, sheet, style, rowNum[0]++);

                            entityManager.detach(entity);
                            if (rowNum[0] % 50 == 0) {
                                log.debug("Clear buffer");
                                entityManager.clear();
                            }

                            if (rowNum[0] % 100 == 0) {
                                try {
                                    sheet.flushRows(100);
                                } catch (IOException e) {
                                    log.warn("Error occurred during flushRows");
                                }
                            }
                        });
                    }
                    
                } else if (startDate != null && endDate != null) {
                    // Convertir format date
                    DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                    LocalDate date = LocalDate.parse(startDate, inputFormat);
                    startDate = date.format(outputFormat);

                    date = LocalDate.parse(endDate, inputFormat);
                    endDate = date.format(outputFormat);

                    try (Stream<CourseComplete> stream = repository.streamAllBetweenDates(startDate, endDate)) {
                        stream.forEach(entity -> {
                            generateRowFromCourseComplete(entity, sheet, style, rowNum[0]++);

                            entityManager.detach(entity);
                            if (rowNum[0] % 50 == 0) {
                                log.debug("Clear buffer");
                                entityManager.clear();
                            }

                            if (rowNum[0] % 100 == 0) {
                                try {
                                    sheet.flushRows(100);
                                } catch (IOException e) {
                                    log.warn("Error occurred during flushRows");
                                }
                            }
                        });
                    }
                }else{
                    log.error("startDate and endDate, both must be null or set...");
                    return;
                }

                try (FileOutputStream out = new FileOutputStream( sFile )) {
                    workbook.write(out);
                }

                log.debug("End Clear buffer");
                entityManager.clear();
            }
            log.info("Generation successful");

        }catch (Exception e){
            log.error("Error extracting to Excel file : {}", e.getMessage());
        }
    }

    private void generateRowFromCourseComplete(CourseComplete cc, Sheet sheet, CellStyle style, int line) {

        Row row = sheet.createRow(line);

        List<String> fields = ReflectionTool.getDesirableCourseCompleteFields();
        int i = 0;
        // Special Field : Date complete
        Cell headerCellDate = row.createCell(i++);
        headerCellDate.setCellValue(cc.getDateCourse()+" "+cc.getHeures()+":"+cc.getMinutes());
        headerCellDate.setCellStyle(style);

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
        // Special Field : Date complete
        Cell headerCellDate = header.createCell(i++);
        headerCellDate.setCellValue("Date complete");
        headerCellDate.setCellStyle(headerStyle);

        for (String field : capFieldsName) {
            Cell headerCell = header.createCell(i++);
            headerCell.setCellValue(field);
            headerCell.setCellStyle(headerStyle);
        }
    }

}
