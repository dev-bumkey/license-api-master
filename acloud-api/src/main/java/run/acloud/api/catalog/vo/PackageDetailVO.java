package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

@Getter
@Setter
@Schema(title="PackageDetail", description="패키지 상세정보 모델")
public class PackageDetailVO extends HasUseYnVO {
	Integer componentSeq;

	String appDetail;
}
