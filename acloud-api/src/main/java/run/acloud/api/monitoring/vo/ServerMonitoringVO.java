package run.acloud.api.monitoring.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Data
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class ServerMonitoringVO extends BaseVO {
	
	private MonitoringVO average;
	
	private MonitoringVO top;
}
