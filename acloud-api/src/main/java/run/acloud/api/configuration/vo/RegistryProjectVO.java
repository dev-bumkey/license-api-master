package run.acloud.api.configuration.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 6.
 */
@Getter
@Setter
public class RegistryProjectVO {
    private Integer projectId;

    private String name;

    private boolean isPublic = false;

    private Integer ownerId;

    private String owenrName;
}
