package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "빌드 이미지 정보 tags 모델")
public class BuildImageVO {

    private Integer buildSeq;
    private String registryImagePath;
    private Integer registryProjectId;
    private String registryName;
    private String imageName;
    private Integer externalRegistrySeq;

    private List<BuildImageInfoVO> tags;

}

