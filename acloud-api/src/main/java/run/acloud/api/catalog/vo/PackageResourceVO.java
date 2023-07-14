package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(title="PackageResource", description="패키지 리소스 정보 모델")
public class PackageResourceVO {
	Integer componentSeq;
	String componentId;
	String kind;
	String name;
	String json;
}
