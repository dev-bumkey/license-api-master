package run.acloud.commons.handler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import run.acloud.commons.util.ExcelUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
public abstract class AbstractExcelDownloadResultHandler<T> implements ResultHandler<T> {

    private HttpServletResponse response;
    private SXSSFWorkbook workbook;
    private SXSSFSheet sheet;
    private SXSSFRow row;
    private Map<String, CellStyle> styles;
    private List<Pair<String, Integer>> headers;

    private int rowNum = 0;
    private int colIdx = 0;

    public CellStyle getMainGeneralCenterStyle() {
        return getStyles().get(ExcelUtils.CELL_STYLE_mainGeneralCenter);
    }

    public CellStyle getMainGeneralLeftStyle() {
        return getStyles().get(ExcelUtils.CELL_STYLE_mainGeneralLeft);
    }

    public CellStyle getHyperLinkCenterStyle() {
        return getStyles().get(ExcelUtils.CELL_STYLE_hyperLinkCenter);
    }

    public CellStyle getDateFormatyyyymdhmmssCenterStyle() {
        return getStyles().get(ExcelUtils.CELL_STYLE_dateFormatyyyymdhmmssCenter);
    }

    public CellStyle getDateFormatyyyymdCenterStyle() {
        return getStyles().get(ExcelUtils.CELL_STYLE_dateFormatyyyymdCenter);
    }

    /**
     * 생성자
     *
     * @param response
     * @param fileName
     * @param sheetName
     * @param headers List<Pair<columnName, columnWidth(1/256)>> header를 구성할 정보 셋팅
     */
    public AbstractExcelDownloadResultHandler(HttpServletResponse response, String fileName, String sheetName, List<Pair<String, Integer>> headers) {
        this.response = response;
        this.headers = headers;

        // response header 및 filename 셋팅
        ExcelUtils.setHeaderFileName(this.response, fileName);
        // workbook
        workbook = new SXSSFWorkbook(null, 100, true, true);
        // style
        styles = ExcelUtils.createStyles(workbook);
        // Sheet 생성
        sheet = workbook.createSheet(sheetName);

        this.createSheetTiles();
    }

    @Override
    public abstract void handleResult(ResultContext<? extends T> resultContext);

    public void createSheetTiles(){
        if (CollectionUtils.isNotEmpty(headers)) {

            int idx = 0;

            // 눈금선 없애기
            sheet.setDisplayGridlines(false);

            //CellStyle
            CellStyle header2Style = styles.get(ExcelUtils.CELL_STYLE_header2);

            // top 여백
            sheet.createRow(rowNum++);

            //*** hearder ***
            SXSSFRow row = sheet.createRow(rowNum);

            // left 여백
            sheet.setColumnWidth(0, 5 * 256); // 5
            SXSSFCell cell = row.createCell(idx++);
            cell.setCellValue("");

            for (int colIdx = 1, ecolIdx = headers.size(); colIdx <= ecolIdx; colIdx++) {
                // width 셋팅
                sheet.setColumnWidth(colIdx, headers.get(colIdx - 1).getValue() * 256);

                // header 셋팅
                cell = row.createCell(colIdx);
                cell.setCellStyle(header2Style);
                cell.setCellValue(headers.get(colIdx - 1).getKey());
            }

            // data count 증가.
            rowNum++;
        }
    }

    public SXSSFRow addRow() {
        colIdx = 0;

        this.setRow(this.getSheet().createRow(getRowNum()));

        SXSSFCell cell = this.getRow().createCell(colIdx++); // 여백
        cell.setCellValue("");

        return this.getRow();
    }

    public void endRow() {
        // data count 증가.
        this.setRowNum(this.getRowNum() + 1);
    }

    private SXSSFCell createCell(CellStyle cellStyle) {
        SXSSFCell cell = this.getRow().createCell(colIdx++);
        cell.setCellStyle(cellStyle);

        return cell;
    }

    public SXSSFCell addCell(CellStyle cellStyle, String value) {
        SXSSFCell cell = this.createCell(cellStyle);
        cell.setCellValue(StringUtils.substring(value, 0,  32767));

        return cell;
    }

    public SXSSFCell addCell(CellStyle cellStyle, Date value) {
        SXSSFCell cell = this.createCell(cellStyle);
        cell.setCellValue(value);

        return cell;
    }

    public SXSSFCell addCell(CellStyle cellStyle, double value) {
        SXSSFCell cell = this.createCell(cellStyle);
        cell.setCellValue(value);

        return cell;
    }

    public SXSSFCell addCell(CellStyle cellStyle, Calendar value) {
        SXSSFCell cell = this.createCell(cellStyle);
        cell.setCellValue(value);

        return cell;
    }

    public Cell addCell(CellStyle cellStyle, RichTextString value) {
        Cell cell = this.createCell(cellStyle);
        cell.setCellValue(value);

        return cell;
    }

    public SXSSFCell addCell(CellStyle cellStyle, boolean value) {
        SXSSFCell cell = this.createCell(cellStyle);
        cell.setCellValue(value);

        return cell;
    }
}
