package run.acloud.api.configuration.handler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ibatis.session.ResultContext;
import run.acloud.api.configuration.vo.UserClusterRoleIssueHistoryVO;
import run.acloud.commons.handler.AbstractExcelDownloadResultHandler;

import java.util.List;

@Slf4j
public class UserClusterRoleIssueHistoryResultHandler<T> extends AbstractExcelDownloadResultHandler<UserClusterRoleIssueHistoryVO> {

    public UserClusterRoleIssueHistoryResultHandler(HttpServletResponse response, String fileName, String sheetName, List<Pair<String, Integer>> headers) {
        super(response, fileName, sheetName, headers);
    }


    @Override
    public void handleResult(ResultContext<? extends UserClusterRoleIssueHistoryVO> resultContext) {

        addRow();

        addCell(getDateFormatyyyymdhmmssCenterStyle(), resultContext.getResultObject().getHistoryDatetime());
        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getHistoryState());
        addCell(getMainGeneralLeftStyle(), String.format("%s (%s)", resultContext.getResultObject().getUserName(), resultContext.getResultObject().getUserId()));
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getIssueAccountName());
        if (StringUtils.equalsIgnoreCase(resultContext.getResultObject().getBindingType(), "CLUSTER")) {
            addCell(getMainGeneralLeftStyle(), String.format("%s-%s", resultContext.getResultObject().getIssueAccountName(), resultContext.getResultObject().getClusterId()));
            addCell(getMainGeneralLeftStyle(), "ALL");
        }
        else { // "NAMESPACE"
            addCell(getMainGeneralLeftStyle(), String.format("%s-%s", resultContext.getResultObject().getIssueAccountName(), resultContext.getResultObject().getBindingNamespace()));
            addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getBindingNamespace());
        }
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getClusterId());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getIssueRole());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getBindingType());
        addCell(getMainGeneralLeftStyle(), String.format("%s (%s)", resultContext.getResultObject().getIssueUserName(), resultContext.getResultObject().getIssueUserId()));

//        addCell(getDateFormatyyyymdhmmssCenterStyle(), resultContext.getResultObject().getHistoryDatetime());
//        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getHistoryState());
//        if (IssueType.valueOf(resultContext.getResultObject().getIssueType()) == IssueType.KUBECONFIG) {
//            addCell(getDateFormatyyyymdCenterStyle(), resultContext.getResultObject().getExpirationDatetime());
//        }
//        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getIssueAccountName());
//        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getIssueRole());
//        addCell(getMainGeneralLeftStyle(), String.format("%s (%s)", resultContext.getResultObject().getUserId(), resultContext.getResultObject().getUserName()));
//        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getClusterName());
//        addCell(getMainGeneralLeftStyle(), String.format("%s (%s)", resultContext.getResultObject().getIssueUserId(), resultContext.getResultObject().getIssueUserName()));

        // rowNum 증가.
        endRow();
    }

}
