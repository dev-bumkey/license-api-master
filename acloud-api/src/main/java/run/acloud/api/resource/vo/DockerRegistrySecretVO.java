package run.acloud.api.resource.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 8. 31.
 */
@Getter
@Setter
public class DockerRegistrySecretVO extends SecretGuiVO {
    private String name;

    private String serverUrl;

    private String userName;

    private String password;

    private String email;
}
