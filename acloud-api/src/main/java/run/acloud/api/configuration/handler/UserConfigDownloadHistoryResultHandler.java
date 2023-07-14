package run.acloud.api.configuration.handler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ibatis.session.ResultContext;
import run.acloud.api.configuration.vo.UserConfigDownloadHistoryVO;
import run.acloud.commons.handler.AbstractExcelDownloadResultHandler;

import java.util.List;

@Slf4j
public class UserConfigDownloadHistoryResultHandler<T> extends AbstractExcelDownloadResultHandler<UserConfigDownloadHistoryVO> {

    public UserConfigDownloadHistoryResultHandler(HttpServletResponse response, String fileName, String sheetName, List<Pair<String, Integer>> headers) {
        super(response, fileName, sheetName, headers);
    }


    @Override
    public void handleResult(ResultContext<? extends UserConfigDownloadHistoryVO> resultContext) {

        addRow();

        addCell(getDateFormatyyyymdhmmssCenterStyle(), resultContext.getResultObject().getDownloadDatetime());
        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getHistoryState());
        if (StringUtils.isNotBlank(resultContext.getResultObject().getExpirationDatetime())) {
            addCell(getDateFormatyyyymdCenterStyle(), resultContext.getResultObject().getExpirationDatetime());
        } else {
            addCell(getMainGeneralCenterStyle(), "Indefinitely");
        }
        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getDownloadState());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getDownloadAccountName());
        addCell(getMainGeneralLeftStyle(), String.format("%s (%s)", resultContext.getResultObject().getUserId(), resultContext.getResultObject().getUserName()));
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getClusterName());
        addCell(getMainGeneralLeftStyle(), String.format("%s (%s)", resultContext.getResultObject().getIssueUserId(), resultContext.getResultObject().getIssueUserName()));

        // rowNum 증가.
        endRow();
    }

}
