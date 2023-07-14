package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@EqualsAndHashCode
public class ContainerLifecycleVO implements Serializable {
    @SerializedName("postStart")
    private ContainerLifecycleHandlerVO postStart;
    @SerializedName("preStop")
    private ContainerLifecycleHandlerVO preStop;
}
