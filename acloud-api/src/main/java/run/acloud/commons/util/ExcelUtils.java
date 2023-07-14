package run.acloud.commons.util;

import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public final class ExcelUtils {
	// Private constructor prevents instantiation from other classes
	private ExcelUtils() { }

	public static final String CELL_STYLE_header1 = "header1";
	public static final String CELL_STYLE_header2 = "header2";
	public static final String CELL_STYLE_mainGeneralCenter = "mainGeneralCenter";
	public static final String CELL_STYLE_mainGeneralLeft = "mainGeneralLeft";
	public static final String CELL_STYLE_hyperLinkCenter = "hyperLinkCenter";
	public static final String CELL_STYLE_dateFormatyyyymdhmmssCenter = "dateFormatyyyymdhmmssCenter";
	public static final String CELL_STYLE_dateFormatyyyymdCenter = "dateFormatyyyymdCenter";

	/**
	 * Create a library of cell styles.
	 *
	 * @param wb
	 * @return
	 */
	public static Map<String, CellStyle> createStyles(SXSSFWorkbook wb){
		Map<String, CellStyle> styles = Maps.newHashMap();
		DataFormat fmt = wb.createDataFormat();

		Font headerFont = wb.createFont();
		headerFont.setBold(true);
//		headerFont.setFontName("맑은 고딕");

		Font mainFont = wb.createFont();
//		mainFont.setFontName("맑은 고딕");

		Font linkFont = wb.createFont();
		linkFont.setUnderline(FontUnderline.SINGLE.getByteValue());
		linkFont.setColor(IndexedColors.BLUE.getIndex());
//		linkFont.setFontName("맑은 고딕");

		CellStyle style1 = wb.createCellStyle();
		style1.setFont(mainFont);
		style1.setAlignment(HorizontalAlignment.CENTER);
		style1.setVerticalAlignment(VerticalAlignment.CENTER);
		style1.setBorderTop(BorderStyle.THIN);
		style1.setBorderLeft(BorderStyle.THIN);
		style1.setBorderBottom(BorderStyle.THIN);
		style1.setBorderRight(BorderStyle.THIN);
		styles.put(CELL_STYLE_mainGeneralCenter, style1);

		CellStyle style2 = wb.createCellStyle();
		style2.setFont(mainFont);
		style2.setAlignment(HorizontalAlignment.LEFT);
		style2.setVerticalAlignment(VerticalAlignment.CENTER);
		style2.setBorderTop(BorderStyle.THIN);
		style2.setBorderLeft(BorderStyle.THIN);
		style2.setBorderBottom(BorderStyle.THIN);
		style2.setBorderRight(BorderStyle.THIN);
		styles.put(CELL_STYLE_mainGeneralLeft, style2);

		CellStyle style3 = wb.createCellStyle();
		style3.setFont(linkFont);
		style3.setAlignment(HorizontalAlignment.CENTER);
		style3.setVerticalAlignment(VerticalAlignment.CENTER);
		style3.setBorderTop(BorderStyle.THIN);
		style3.setBorderLeft(BorderStyle.THIN);
		style3.setBorderBottom(BorderStyle.THIN);
		style3.setBorderRight(BorderStyle.THIN);
		styles.put(CELL_STYLE_hyperLinkCenter, style3);

		CellStyle style41 = wb.createCellStyle();
		style41.setAlignment(HorizontalAlignment.CENTER);
		style41.setVerticalAlignment(VerticalAlignment.CENTER);
		style41.setDataFormat(fmt.getFormat("yyyy-m-d h:mm:ss"));
		style41.setBorderTop(BorderStyle.THIN);
		style41.setBorderLeft(BorderStyle.THIN);
		style41.setBorderBottom(BorderStyle.THIN);
		style41.setBorderRight(BorderStyle.THIN);
		styles.put(CELL_STYLE_dateFormatyyyymdhmmssCenter, style41);

		CellStyle style42 = wb.createCellStyle();
		style42.setAlignment(HorizontalAlignment.CENTER);
		style42.setVerticalAlignment(VerticalAlignment.CENTER);
		style42.setDataFormat(fmt.getFormat("yyyy-m-d"));
		style42.setBorderTop(BorderStyle.THIN);
		style42.setBorderLeft(BorderStyle.THIN);
		style42.setBorderBottom(BorderStyle.THIN);
		style42.setBorderRight(BorderStyle.THIN);
		styles.put(CELL_STYLE_dateFormatyyyymdCenter, style42);

		CellStyle style5 = wb.createCellStyle();
		style5.setAlignment(HorizontalAlignment.RIGHT);
		style5.setVerticalAlignment(VerticalAlignment.CENTER);
		style5.setDataFormat(fmt.getFormat("#,##0.000"));
		style5.setBorderTop(BorderStyle.THIN);
		style5.setBorderLeft(BorderStyle.THIN);
		style5.setBorderBottom(BorderStyle.THIN);
		style5.setBorderRight(BorderStyle.THIN);
		styles.put("second", style5);

		// 메인 타이틀
		XSSFCellStyle headerStyle1 = wb.getXSSFWorkbook().createCellStyle();
		XSSFColor header1Color = new XSSFColor(new byte[]{(byte)242, (byte)220, (byte)219});
		headerStyle1.setFillForegroundColor(header1Color);
		headerStyle1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerStyle1.setFont(headerFont);
		headerStyle1.setAlignment(HorizontalAlignment.CENTER);
		headerStyle1.setVerticalAlignment(VerticalAlignment.CENTER);
		styles.put(CELL_STYLE_header1, headerStyle1);

		// 테이블 헤더
		XSSFCellStyle headerStyle2 = wb.getXSSFWorkbook().createCellStyle();
		XSSFColor header2Color = new XSSFColor(new byte[]{(byte)184, (byte)184, (byte)184});
		headerStyle2.setFillForegroundColor(header2Color);
		headerStyle2.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerStyle2.setFont(headerFont);
		headerStyle2.setAlignment(HorizontalAlignment.CENTER);
		headerStyle2.setVerticalAlignment(VerticalAlignment.CENTER);
		headerStyle2.setBorderTop(BorderStyle.THIN);
		headerStyle2.setBorderLeft(BorderStyle.THIN);
		headerStyle2.setBorderBottom(BorderStyle.THIN);
		headerStyle2.setBorderRight(BorderStyle.THIN);
		styles.put(CELL_STYLE_header2, headerStyle2);

		return styles;
	}

	public static void setHeaderFileName(HttpServletResponse response, String fileName) {
		if(fileName == null) {
			fileName = "Cocktail_Excel.xlsx";
		}

		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setHeader("Set-Cookie", "fileDownload=true; path=/");
		response.setHeader("Content-Disposition", String.format("attachment; filename=%s; filename*=UTF-8''%s", fileName, fileName));

	}


	public static void closeAfterWrite(HttpServletResponse response, SXSSFWorkbook workbook) throws Exception {

		try {
			workbook.write(response.getOutputStream());
			workbook.dispose();
			response.flushBuffer();
		} catch (IOException e) {
			log.error("CommonResultHandler error.", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} finally {
			if (workbook != null) {
				try {
					workbook.close();
				} catch (IOException ie) {
					log.error("workbook close error.", ie);
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			}
		}
	}
}
