package run.acloud.commons.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "리스트 카운트 모델")
public class ListCountVO implements Serializable {

    private static final long serialVersionUID = 4196924232430126253L;

    @Schema(title = "전체 데이터 갯수")
    private Integer cnt;

    @Schema(title = "더보기 중복 방지를 위한 데이터셋 위치 지정")
    private Integer maxId;
}