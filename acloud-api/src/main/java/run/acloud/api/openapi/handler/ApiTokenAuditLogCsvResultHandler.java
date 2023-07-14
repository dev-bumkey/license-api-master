package run.acloud.api.openapi.handler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ResultContext;
import run.acloud.api.openapi.vo.ApiTokenAuditLogVO;
import run.acloud.commons.handler.AbstractCsvDownloadResultHandler;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ApiTokenAuditLogCsvResultHandler<T> extends AbstractCsvDownloadResultHandler<ApiTokenAuditLogVO> {

    public ApiTokenAuditLogCsvResultHandler(HttpServletResponse response, List<String> headers) {
        super(response, headers);
    }


    @Override
    public void handleResult(ResultContext<? extends ApiTokenAuditLogVO> resultContext) {

        List<String> nextLine = new ArrayList<>();
        nextLine.add(resultContext.getResultObject().getLogDatetime());
        nextLine.add(resultContext.getResultObject().getApiName());
        nextLine.add(resultContext.getResultObject().getHttpMethod());
        nextLine.add(resultContext.getResultObject().getUrl());
        nextLine.add(resultContext.getResultObject().getReferer());
        nextLine.add(resultContext.getResultObject().getUserAgent());
        nextLine.add(resultContext.getResultObject().getApiTokenName());
        nextLine.add(resultContext.getResultObject().getResult());
        nextLine.add(resultContext.getResultObject().getClientIp());
        nextLine.add(Double.toString(resultContext.getResultObject().getProcessingTime()));
        nextLine.add(resultContext.getResultObject().getRequest());
        nextLine.add(resultContext.getResultObject().getResponse());

        super.writeNext(nextLine);
    }

}
