package run.acloud.api.cserver.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpecFileDeployVO {
	@NotNull
	private String namespace;   // namespace

	@NotNull
	private String name;        // name

	@NotNull
	private String type;        // json, yaml

	@NotNull
	private String data;        // data string

	private String kind;        // kind

	private String apiVersion;  // apiVersion

}
