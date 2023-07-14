package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.ProviderCode;
import run.acloud.commons.vo.HasUseYnVO;

@Getter
@Setter
@Schema(description = "Provider 서버 현황 모델")
public class ProviderAccountCountVO extends HasUseYnVO {

	@Schema(title = "providerAccount Seq")
	private int providerAccountSeq;

	@Schema(title = "ProviderCode")
	private ProviderCode providerCode;

	@Schema(title = "providerName")
	private String providerName;

	@Schema(title = "accountGroupId")
	private String accountGroupId;

	@Schema(title = "apiAccountId")
	private String apiAccountId;

	@Schema(title = "cluster 갯수")
	private Integer clusterCount;

	@Schema(title = "서비스 갯수")
	private Integer serviceCount;

	@Schema(title = "appmap 갯수")
	private Integer appmapCount;

	@Schema(title = "server 갯수")
	private Integer serverCount;


}
