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
public class K8sCRDIssuerCAVO extends BaseVO{

    public static final String SERIALIZED_NAME_CRL_DISTRIBUTION_POINTS = "crlDistributionPoints";
    @Schema(
            description =
                    "The CRL distribution points is an X.509 v3 certificate extension which identifies the location of the CRL from which the revocation of this certificate can be checked. If not set, certificates will be issued without distribution points set.")
    @SerializedName(SERIALIZED_NAME_CRL_DISTRIBUTION_POINTS)
    private List<String> crlDistributionPoints = null;

    public static final String SERIALIZED_NAME_OCSP_SERVERS = "ocspServers";
    @Schema(
            description =
                    "The OCSP server list is an X.509 v3 extension that defines a list of URLs of OCSP responders. The OCSP responders can be queried for the revocation status of an issued certificate. If not set, the certificate will be issued with no OCSP servers set. For example, an OCSP server URL could be http://ocsp.int-x3.letsencrypt.org.")
    @SerializedName(SERIALIZED_NAME_OCSP_SERVERS)
    private List<String> ocspServers = null;

    public static final String SERIALIZED_NAME_SECRET_NAME = "secretName";
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description =
                    "SecretName is the name of the secret used to sign Certificates issued by this Issuer.")
    @SerializedName(SERIALIZED_NAME_SECRET_NAME)
    private String secretName;


    public K8sCRDIssuerCAVO crlDistributionPoints(List<String> crlDistributionPoints) {

        this.crlDistributionPoints = crlDistributionPoints;
        return this;
    }

    public K8sCRDIssuerCAVO addCrlDistributionPointsItem(String crlDistributionPointsItem) {
        if (this.crlDistributionPoints == null) {
            this.crlDistributionPoints = new ArrayList<>();
        }
        this.crlDistributionPoints.add(crlDistributionPointsItem);
        return this;
    }


    public K8sCRDIssuerCAVO ocspServers(List<String> ocspServers) {

        this.ocspServers = ocspServers;
        return this;
    }

    public K8sCRDIssuerCAVO addOcspServersItem(String ocspServersItem) {
        if (this.ocspServers == null) {
            this.ocspServers = new ArrayList<>();
        }
        this.ocspServers.add(ocspServersItem);
        return this;
    }

    public K8sCRDIssuerCAVO secretName(String secretName) {

        this.secretName = secretName;
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
        K8sCRDIssuerCAVO v1IssuerSpecCa = (K8sCRDIssuerCAVO) o;
        return Objects.equals(this.crlDistributionPoints, v1IssuerSpecCa.crlDistributionPoints) &&
                Objects.equals(this.ocspServers, v1IssuerSpecCa.ocspServers) &&
                Objects.equals(this.secretName, v1IssuerSpecCa.secretName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(crlDistributionPoints, ocspServers, secretName);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class K8sCRDIssuerCAVO {\n");
        sb.append("    crlDistributionPoints: ").append(toIndentedString(crlDistributionPoints)).append("\n");
        sb.append("    ocspServers: ").append(toIndentedString(ocspServers)).append("\n");
        sb.append("    secretName: ").append(toIndentedString(secretName)).append("\n");
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
