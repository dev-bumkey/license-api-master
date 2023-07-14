package run.acloud.commons.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "Paging 모델")
public class PagingVO implements Serializable {

    private static final long serialVersionUID = -6125946952469130478L;

    @Schema(title = "orderColumn")
    private String orderColumn;

    @Schema(title = "order", allowableValues = {"ASC","DESC"})
    private String order;

    @Schema(title = "nextPage")
    private Integer nextPage;

    @Schema(title = "itemPerPage")
    private Integer itemPerPage;

    @Schema(title = "maxId")
    private String maxId;

    @Schema(title = "listCount")
    private ListCountVO listCount;

}
