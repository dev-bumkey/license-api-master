package run.acloud.api.configuration.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by choewonseog on 2017. 1. 25..
 */
@Getter
@Setter
public class ImageRepositoryTagVO {
    private String imageName;
    private String tagName;
    private String architecture;
    private String digest;
    private String dockerVersion;
    private String created;
    private Long size;
}
