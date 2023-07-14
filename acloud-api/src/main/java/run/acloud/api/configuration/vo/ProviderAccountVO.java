package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.ProviderAccountType;
import run.acloud.api.resource.enums.ProviderCode;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.vo.HasUseYnVO;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class ProviderAccountVO extends HasUseYnVO implements Serializable {
	private static final long serialVersionUID = -6221367368600633133L;

	@Schema(title = "프로바이더 계정 시퀀스")
	@JsonProperty("seq")
	private int providerAccountSeq;

	@Schema(title = "프로바이더 코드", example = "OPM")
	private ProviderCode providerCode;

	@Schema(title = "프로바이더 코드 이름", example = "Baremetal")
	private String providerCodeName;

	public String getProviderCodeName() {
		if (providerCode != null) {
			providerCodeName = providerCode.getDescription();
		} else {
			providerCodeName = null;
		}
		return providerCodeName;
	}

	@Schema(title = "프로바이더 이름", example = "Acorn-Test-Privider")
	private String providerName;

	@Schema(title = "프로바이더 설명", example = "A to the CORN")
	private String description;

	private String status;

	@JsonProperty("groupId")
	private String accountGroupId;

	@Schema(title = "프로바이더 유형", example = "USER")
	@JsonProperty("useType")
	private ProviderAccountType accountUseType;

	@Schema(title = "계정 번호")
	private Integer accountSeq;

	@JsonIgnore
	private String apiAccountId;

	@JsonIgnore
	private String apiAccountPassword;

	private String providerAccountConfig;

	private String projectId;

	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private String credential;
	public void setCredential(String credential){
		this.credential = CryptoUtils.encryptAES(credential);
	}
	public String getCredential(){
		return CryptoUtils.decryptAES(this.credential);
	}

	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private String config;
	public void setConfig(String config){
		this.config = CryptoUtils.encryptAES(config);
	}
	public String getConfig(){
		return CryptoUtils.decryptAES(this.config);
	}

	private List<String> clusterNames;

	// 데이터 수정시 생성/수정/삭제 등의 변경 유형 판단을 위해 추가
	// C(Create), U(Update), D(Delete), N(No Change : UI에서 No Change일때 값을 넘기지 않고 있음.. R4.1.0 => null도 No Change로 간주..)
	private String modifyType;

}
