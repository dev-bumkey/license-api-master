package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 2021. 06. 18.
 */
@Schema(title = "K8s CustomResourceDefinition 모델")
@Data
public class K8sCRDResultVO {

	@Schema(title = "name")
	private String name;

	@Schema(title = "group")
	private String group;

	@Schema(title = "fullName")
	private String fullName;

	@Schema(title = "scope", allowableValues = {"Cluster","Namespaced"})
	private String scope;

	@Schema(title = "acceptedNames")
	@SerializedName(value = "acceptedNames", alternate = "names")
	private K8sCRDNamesVO acceptedNames;

	@Schema(title = "storedVersion")
	private String storedVersion;

	@Schema(title = "versions")
	private List<K8sCRDVersionVO> versions = new ArrayList<>();

	@Schema(title = "labels")
	private Map<String, String> labels = new HashMap<>();

	@Schema(title = "annotations")
	private Map<String, String> annotations = new HashMap<>();

	@Schema(title = "생성시간")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private OffsetDateTime creationTimestamp;

	@Schema(title = "배포 정보 (yaml)")
	private String yaml;

}
