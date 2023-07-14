package run.acloud.api.configuration.vo;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import run.acloud.commons.vo.BaseVO;


/**
 * OAuth Token 모델
 * serialize or deserialize 시 GSON 사용
 *
 */
@Getter
@Setter
public class OAuthTokenVO extends BaseVO {

    @SerializedName(value="tokenType", alternate = {"token_type"})
    private String tokenType; // Token Type, example : Bearer

    private long expiresIn; // The remaining lifetime of the token in seconds

    @SerializedName(value="expiresOnDate", alternate = {"expiry_date"})
    private long expiresOnDate; // The date and time on which the token expires. milisends

    @SerializedName(value="accessToken", alternate = {"access_token"})
    private String accessToken; // Access Token

    @SerializedName(value="refreshToken", alternate = {"refresh_token"})
    private String refreshToken; //Refresh Token

    @SerializedName(value="scope", alternate = {"resource"})
    private String scope; //Resource for Azure, Scope for GKE

    private String tenantId; //Tenant ID for Azure

    public boolean isExpired(){
        boolean expired = false;

        //TODO, 2021-09-03, coolingi, 만료일이 존재하지 않을시 만료 안됨. 우선 AKS만 해당함.
        if(expiresOnDate == -1) {
            return false;
        } else if(expiresOnDate > 0) {
            DateTime expireTime = new DateTime(expiresOnDate, DateTimeZone.UTC);
            if( expireTime.isBeforeNow() ){
                expired = true;
            }
        } else {
            // 방어로직, 최초 등록시에 refresh token API 호출해 expiresOnDate 값을 셋팅한다.
            // 혹시 cluster 등록시 오류로 인해 expiresOnDate 값이 없을때는 expired 된 것으로 인식하고 token을 다시 가져온다.
            expired = true;
        }
        return expired;
    }

}
