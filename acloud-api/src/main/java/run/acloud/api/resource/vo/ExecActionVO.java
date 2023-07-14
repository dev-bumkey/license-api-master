package run.acloud.api.resource.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@EqualsAndHashCode
public class ExecActionVO implements Serializable {
    private List<String> command = null;
}
