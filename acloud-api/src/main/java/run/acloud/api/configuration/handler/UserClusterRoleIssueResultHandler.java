package run.acloud.api.configuration.handler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ibatis.session.ResultContext;
import run.acloud.api.configuration.enums.IssueType;
import run.acloud.api.configuration.vo.UserClusterRoleIssueVO;
import run.acloud.commons.handler.AbstractExcelDownloadResultHandler;

import java.util.List;

@Slf4j
public class UserClusterRoleIssueResultHandler<T> extends AbstractExcelDownloadResultHandler<UserClusterRoleIssueVO> {

    public UserClusterRoleIssueResultHandler(HttpServletResponse response, String fileName, String sheetName, List<Pair<String, Integer>> headers) {
        super(response, fileName, sheetName, headers);
    }


    @Override
    public void handleResult(ResultContext<? extends UserClusterRoleIssueVO> resultContext) {

        addRow();

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
        addCell(getMainGeneralLeftStyle(), String.format("%s (%s)", resultContext.getResultObject().getCreatorName(), resultContext.getResultObject().getCreatorId()));
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getIssueDatetime());
        if (IssueType.valueOf(resultContext.getResultObject().getIssueType()) == IssueType.KUBECONFIG) {
            if (StringUtils.isNotBlank(resultContext.getResultObject().getExpirationDatetime())) {
                addCell(getDateFormatyyyymdCenterStyle(), resultContext.getResultObject().getExpirationDatetime());
            } else {
                addCell(getMainGeneralCenterStyle(), "Indefinitely");
            }
        }

//        addCell(getMainGeneralLeftStyle(), String.format("%s (%s)", resultContext.getResultObject().getUserId(), resultContext.getResultObject().getUserName()));
//        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getClusterName());
//        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getIssueRole());
//        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getProviderCodeName());
//        addCell(getMainGeneralCenterStyle(), resultContext.getResultObject().getRegionName());
//        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getIssueAccountName());
//        addCell(getMainGeneralLeftStyle(), String.format("%s (%s)", resultContext.getResultObject().getCreatorId(), resultContext.getResultObject().getCreatorName()));
//        if (IssueType.valueOf(resultContext.getResultObject().getIssueType()) == IssueType.KUBECONFIG) {
//            addCell(getDateFormatyyyymdCenterStyle(), resultContext.getResultObject().getExpirationDatetime());
//        }
//        addCell(getDateFormatyyyymdhmmssCenterStyle(), resultContext.getResultObject().getCreated());

        // rowNum 증가.
        endRow();
    }

}
