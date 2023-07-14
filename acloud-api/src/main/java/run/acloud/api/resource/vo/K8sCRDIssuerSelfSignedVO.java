package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
public class K8sCRDIssuerSelfSignedVO extends BaseVO{

    public static final String SERIALIZED_NAME_CRL_DISTRIBUTION_POINTS = "crlDistributionPoints";
    @Schema(
            description =
                    "The CRL distribution points is an X.509 v3 certificate extension which identifies the location of the CRL from which the revocation of this certificate can be checked. If not set certificate will be issued without CDP. Values are strings.")
    @SerializedName(SERIALIZED_NAME_CRL_DISTRIBUTION_POINTS)
    private List<String> crlDistributionPoints = null;


    public K8sCRDIssuerSelfSignedVO crlDistributionPoints(List<String> crlDistributionPoints) {

        this.crlDistributionPoints = crlDistributionPoints;
        return this;
    }

    public K8sCRDIssuerSelfSignedVO addCrlDistributionPointsItem(String crlDistributionPointsItem) {
        if (this.crlDistributionPoints == null) {
            this.crlDistributionPoints = new ArrayList<>();
        }
        this.crlDistributionPoints.add(crlDistributionPointsItem);
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
        K8sCRDIssuerSelfSignedVO v1IssuerSpecSelfSigned = (K8sCRDIssuerSelfSignedVO) o;
        return Objects.equals(this.crlDistributionPoints, v1IssuerSpecSelfSigned.crlDistributionPoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(crlDistributionPoints);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class K8sIssuerSelfSignedVO {\n");
        sb.append("    crlDistributionPoints: ").append(toIndentedString(crlDistributionPoints)).append("\n");
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
