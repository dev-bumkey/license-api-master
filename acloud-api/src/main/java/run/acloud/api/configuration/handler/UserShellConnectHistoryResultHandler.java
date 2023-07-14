package run.acloud.api.configuration.handler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ibatis.session.ResultContext;
import run.acloud.api.configuration.vo.UserShellConnectHistoryVO;
import run.acloud.commons.handler.AbstractExcelDownloadResultHandler;

import java.util.List;

@Slf4j
public class UserShellConnectHistoryResultHandler<T> extends AbstractExcelDownloadResultHandler<UserShellConnectHistoryVO> {

    public UserShellConnectHistoryResultHandler(HttpServletResponse response, String fileName, String sheetName, List<Pair<String, Integer>> headers) {
        super(response, fileName, sheetName, headers);
    }


    @Override
    public void handleResult(ResultContext<? extends UserShellConnectHistoryVO> resultContext) {

        addRow();

        addCell(getDateFormatyyyymdhmmssCenterStyle(), resultContext.getResultObject().getConnectDatetime());
        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getHistoryState());
        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getConnectState());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getConnectAccountName());
        addCell(getMainGeneralLeftStyle(), String.format("%s (%s)", resultContext.getResultObject().getUserId(), resultContext.getResultObject().getUserName()));
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getClusterName());
        addCell(getMainGeneralLeftStyle(), String.format("%s (%s)", resultContext.getResultObject().getIssueUserId(), resultContext.getResultObject().getIssueUserName()));

        // rowNum 증가.
        endRow();
    }

}
