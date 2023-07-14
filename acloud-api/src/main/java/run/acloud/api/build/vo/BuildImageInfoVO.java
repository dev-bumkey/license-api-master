package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@ToString
@Schema(description = "빌드 이미지 Tag 정보 모델")
public class BuildImageInfoVO extends BaseVO {

    private Integer buildRunSeq;
    private String tag;
    private String runDesc;
    private String imageUrl;
    private String imageSize;
    private String imageDigest;
    private String beginTime;
    private String endTime;

}

