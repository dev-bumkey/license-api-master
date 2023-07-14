package run.acloud.api.configuration.vo;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Created by choewonseog on 2017. 1. 25..
 */
@Getter
@Setter
public class ImageRepositoryVO {
    private String name;

    private String projectName;

    private Integer projectId;

    private List<Pair<String, String>> tags;
}
