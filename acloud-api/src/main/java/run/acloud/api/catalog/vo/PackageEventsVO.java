package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(title="PackageEvents", description="패키지 이벤트 리스트 모델")
public class PackageEventsVO {
	Integer componentSeq;
	List<PackageEventVO> events;
}
