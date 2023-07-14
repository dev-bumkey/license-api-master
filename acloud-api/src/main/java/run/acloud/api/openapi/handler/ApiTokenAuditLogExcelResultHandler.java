package run.acloud.api.openapi.handler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ibatis.session.ResultContext;
import run.acloud.api.openapi.vo.ApiTokenAuditLogVO;
import run.acloud.commons.handler.AbstractExcelDownloadResultHandler;

import java.util.List;

@Slf4j
public class ApiTokenAuditLogExcelResultHandler<T> extends AbstractExcelDownloadResultHandler<ApiTokenAuditLogVO> {

    public ApiTokenAuditLogExcelResultHandler(HttpServletResponse response, String fileName, String sheetName, List<Pair<String, Integer>> headers) {
        super(response, fileName, sheetName, headers);
    }


    @Override
    public void handleResult(ResultContext<? extends ApiTokenAuditLogVO> resultContext) {

        addRow();

        addCell(getDateFormatyyyymdhmmssCenterStyle(), resultContext.getResultObject().getLogDatetime());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getApiName());
        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getHttpMethod());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getUrl());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getReferer());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getUserAgent());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getApiTokenName());
        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getResult());
        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getClientIp());
        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getProcessingTime());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getRequest());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getResponse());

        // rowNum 증가.
        endRow();
    }

}
