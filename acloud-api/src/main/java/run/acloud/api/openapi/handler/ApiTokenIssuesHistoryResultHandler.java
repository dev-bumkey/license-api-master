package run.acloud.api.openapi.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ibatis.session.ResultContext;
import run.acloud.api.openapi.vo.ApiTokenIssueHistoryExcelVO;
import run.acloud.commons.handler.AbstractExcelDownloadResultHandler;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.framework.enums.ExceptionType;

import java.util.List;

@Slf4j
public class ApiTokenIssuesHistoryResultHandler<T> extends AbstractExcelDownloadResultHandler<ApiTokenIssueHistoryExcelVO> {

    public ApiTokenIssuesHistoryResultHandler(HttpServletResponse response, String fileName, String sheetName, List<Pair<String, Integer>> headers) {
        super(response, fileName, sheetName, headers);
    }


    @Override
    public void handleResult(ResultContext<? extends ApiTokenIssueHistoryExcelVO> resultContext) {

        addRow();

        addCell(getDateFormatyyyymdhmmssCenterStyle(), resultContext.getResultObject().getHistoryDatetime());
        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getHistoryState());
        if (resultContext.getResultObject().getUpdateUserSeq() != null && !Integer.valueOf(0).equals(resultContext.getResultObject().getUpdateUserSeq())) {
            addCell(getMainGeneralLeftStyle(), String.format("%s (%s)", resultContext.getResultObject().getUpdateUserId(), resultContext.getResultObject().getUpdateUserName()));
        } else {
            addCell(getMainGeneralLeftStyle(), "-");
        }
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getApiTokenName());
        if (StringUtils.isNotBlank(resultContext.getResultObject().getExpirationDatetime())) {
            addCell(getDateFormatyyyymdCenterStyle(), resultContext.getResultObject().getExpirationDatetime());
        } else {
            addCell(getMainGeneralCenterStyle(), "Indefinitely");
        }
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getWhiteIpListJson());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getBlackIpListJson());
        if (resultContext.getResultObject().getWithApi()) {
            if (CollectionUtils.isNotEmpty(resultContext.getResultObject().getPermissionsScopes())) {
                try {
                    addCell(getMainGeneralLeftStyle(), ObjectMapperUtils.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(resultContext.getResultObject().getPermissionsScopes()));
                } catch (JsonProcessingException e) {
                    log.error("Error generate excel.", e, ExceptionType.ExcelDownloadFail);
                }
            } else {
                addCell(getMainGeneralLeftStyle(), "");
            }
        }

        // rowNum 증가.
        endRow();
    }

}
