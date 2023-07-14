package run.acloud.api.monitoring.handler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ibatis.session.ResultContext;
import run.acloud.api.monitoring.vo.AlertRuleClusterMappingVO;
import run.acloud.api.monitoring.vo.AlertRuleVO;
import run.acloud.commons.handler.AbstractExcelDownloadResultHandler;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class AlertRuleResultHandler<T> extends AbstractExcelDownloadResultHandler<AlertRuleVO> {

    public AlertRuleResultHandler(HttpServletResponse response, String fileName, String sheetName, List<Pair<String, Integer>> headers) {
        super(response, fileName, sheetName, headers);
    }


    @Override
    public void handleResult(ResultContext<? extends AlertRuleVO> resultContext) {

        addRow();

        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getAlertName());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getAlertState());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getAlertMessage());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getDuration());
        addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getDescription());
        if (CollectionUtils.isNotEmpty(resultContext.getResultObject().getAlertClusters())) {
            addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getAlertClusters().stream().map(AlertRuleClusterMappingVO::getClusterId).collect(Collectors.joining(", ")));
        } else {
            addCell(getMainGeneralLeftStyle(), "");
        }
        if (CollectionUtils.isNotEmpty(resultContext.getResultObject().getAlertReceivers())) {
            addCell(getMainGeneralLeftStyle(), resultContext.getResultObject().getAlertReceivers().stream().map(u -> (String.format("%s (%s)", u.getUserName(), u.getPhoneNumber()))).collect(Collectors.joining(", ")));
        } else {
            addCell(getMainGeneralLeftStyle(), "");
        }

        // rowNum 증가.
        endRow();
    }

}
