package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.util.Objects;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
public class K8sCRDIssuerRefVO extends BaseVO{

    public static final String SERIALIZED_NAME_GROUP = "group";
    @Schema(
            description =
                    "Group of the resource being referred to.")
    @SerializedName(SERIALIZED_NAME_GROUP)
    private String group;

    public static final String SERIALIZED_NAME_KIND = "kind";
    @Schema(
            description =
                    "Kind of the resource being referred to.")
    @SerializedName(SERIALIZED_NAME_KIND)
    private String kind;

    public static final String SERIALIZED_NAME_NAME = "name";
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description =
                    "Name of the resource being referred to.")
    @SerializedName(SERIALIZED_NAME_NAME)
    private String name;


    public K8sCRDIssuerRefVO group(String group) {

        this.group = group;
        return this;
    }

    public K8sCRDIssuerRefVO kind(String kind) {

        this.kind = kind;
        return this;
    }

    public K8sCRDIssuerRefVO name(String name) {

        this.name = name;
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        K8sCRDIssuerRefVO v1CertificateSpecIssuerRef = (K8sCRDIssuerRefVO) o;
        return Objects.equals(this.group, v1CertificateSpecIssuerRef.group) &&
                Objects.equals(this.kind, v1CertificateSpecIssuerRef.kind) &&
                Objects.equals(this.name, v1CertificateSpecIssuerRef.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, kind, name);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class K8sCRDIssuerRefVO {\n");
        sb.append("    group: ").append(toIndentedString(group)).append("\n");
        sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
