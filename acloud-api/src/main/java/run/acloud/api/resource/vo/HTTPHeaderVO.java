package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@EqualsAndHashCode
public class HTTPHeaderVO implements Serializable {
    @SerializedName("name")
    private String name = null;
    @SerializedName("value")
    private String value = null;
}
