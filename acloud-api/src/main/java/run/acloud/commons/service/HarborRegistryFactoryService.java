package run.acloud.commons.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.vo.AccountRegistryVO;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.vo.HarborGeneralInfoVO;


@Slf4j
@Service
public class HarborRegistryFactoryService {

    @Autowired
    private HarborRegistryV1Service v1Service;

    @Autowired
    private HarborRegistryV2Service v2Service;

    public HarborRegistryV1Service getV1(){
        return v1Service;
    }
    public HarborRegistryV2Service getV2(){
        return v2Service;
    }

    public IHarborRegistryService get(ApiVersionType apiVer) {
        switch(apiVer) {
            case V2:
                return this.getV2();
            case V1:
                return this.getV1();
            default:
                return this.getV2();
        }
    }

    public IHarborRegistryService getService(){
        ApiVersionType apiVer = null;
        HarborGeneralInfoVO infoV2 = this.getV2().getSystemGeneralInfo();
        if (infoV2 != null) {
            apiVer = ApiVersionType.V2;
        } else {
            HarborGeneralInfoVO infoV1 = this.getV1().getSystemGeneralInfo();
            if (infoV1 != null) {
                apiVer = ApiVersionType.V1;
            }
        }

        return this.get(apiVer);
    }

    public IHarborRegistryService getService(Integer accountSeq){
        ApiVersionType apiVer = null;
        HarborGeneralInfoVO infoV2 = this.getV2().getSystemGeneralInfo(accountSeq);
        if (infoV2 != null) {
            apiVer = ApiVersionType.V2;
        } else {
            HarborGeneralInfoVO infoV1 = this.getV1().getSystemGeneralInfo(accountSeq);
            if (infoV1 != null) {
                apiVer = ApiVersionType.V1;
            }
        }

        return this.get(apiVer);
    }

    public IHarborRegistryService getService(AccountRegistryVO accountRegistry){
        ApiVersionType apiVer = null;
        HarborGeneralInfoVO infoV2 = this.getV2().getSystemGeneralInfo(accountRegistry);
        if (infoV2 != null) {
            apiVer = ApiVersionType.V2;
        } else {
            HarborGeneralInfoVO infoV1 = this.getV1().getSystemGeneralInfo(accountRegistry);
            if (infoV1 != null) {
                apiVer = ApiVersionType.V1;
            }
        }

        return this.get(apiVer);
    }
}
