package run.acloud.commons.service;


import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.dao.IAccountRegistryMapper;
import run.acloud.api.configuration.vo.AccountRegistryVO;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.properties.CocktailRegistryProperties;

@Slf4j
@Service
public class RegistryPropertyService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private CocktailRegistryProperties registryProperty;

    public String getUrl(){

        // 플랫폼 사용자(ROLE : SYSTEM, USER) 이고, 해당 플랫폼에 등록된 레지스트리가 존재하면 해당 레지스트리 정보를 조회하여 사용한다.
        Integer accountSeq = ContextHolder.exeContext().getUserAccountSeq();
        String registryUrl = this.getUrl(accountSeq);

        return registryUrl;
    }

    public String getUrl(Integer accountSeq){
        String registryUrl = registryProperty.getUrl();

        // 플랫폼 사용자(ROLE : SYSTEM, USER) 이고, 해당 플랫폼에 등록된 레지스트리가 존재하면 해당 레지스트리 정보를 조회하여 사용한다.
        if ( accountSeq != null && accountSeq > 0) {
            IAccountRegistryMapper dao = sqlSession.getMapper(IAccountRegistryMapper.class);
            AccountRegistryVO accountRegistry = dao.getAccountRegistry(accountSeq, null);
            if (accountRegistry != null){
                registryUrl = accountRegistry.getRegistryUrl();
            }
        }

        return registryUrl;
    }

    public String getId(){

        // 플랫폼 사용자(ROLE : SYSTEM, USER) 이고, 해당 플랫폼에 등록된 레지스트리가 존재하면 해당 레지스트리 정보를 조회하여 사용한다.
        Integer accountSeq = ContextHolder.exeContext().getUserAccountSeq();
        String id = this.getId(accountSeq);

        return id;
    }

    public String getId(Integer accountSeq){
        String id = registryProperty.getId();

        if ( accountSeq != null && accountSeq > 0) {
            IAccountRegistryMapper dao = sqlSession.getMapper(IAccountRegistryMapper.class);
            AccountRegistryVO accountRegistry = dao.getAccountRegistry(accountSeq, null);
            if (accountRegistry != null){
                id = CryptoUtils.decryptAES(accountRegistry.getAccessId());
            }
        }

        return id;
    }

    public String getPassword(){

        // 플랫폼 사용자(ROLE : SYSTEM, USER) 이고, 해당 플랫폼에 등록된 레지스트리가 존재하면 해당 레지스트리 정보를 조회하여 사용한다.
        Integer accountSeq = ContextHolder.exeContext().getUserAccountSeq();
        String passwd = this.getPassword(accountSeq);

        return passwd;
    }

    public String getPassword(Integer accountSeq){
        String passwd = registryProperty.getPassword();

        if ( accountSeq != null && accountSeq > 0) {
            IAccountRegistryMapper dao = sqlSession.getMapper(IAccountRegistryMapper.class);
            AccountRegistryVO accountRegistry = dao.getAccountRegistry(accountSeq, null);
            if (accountRegistry != null){
                passwd = CryptoUtils.decryptAES(accountRegistry.getAccessSecret());
            }
        }

        return passwd;
    }

    public String getInsecureYn(){

        // 플랫폼 사용자(ROLE : SYSTEM, USER) 이고, 해당 플랫폼에 등록된 레지스트리가 존재하면 해당 레지스트리 정보를 조회하여 사용한다.
        Integer accountSeq = ContextHolder.exeContext().getUserAccountSeq();
        String insecureYn = this.getInsecureYn(accountSeq);

        return insecureYn;
    }

    public String getInsecureYn(Integer accountSeq){
        String insecureYn = registryProperty.getInsecureYn();


        if ( accountSeq != null && accountSeq > 0) {
            IAccountRegistryMapper dao = sqlSession.getMapper(IAccountRegistryMapper.class);
            AccountRegistryVO accountRegistry = dao.getAccountRegistry(accountSeq, null);
            if (accountRegistry != null){
                insecureYn = accountRegistry.getInsecureYn();
            }
        }

        return insecureYn;
    }

    public String getPrivateCertificateUseYn(){

        // 플랫폼 사용자(ROLE : SYSTEM, USER) 이고, 해당 플랫폼에 등록된 레지스트리가 존재하면 해당 레지스트리 정보를 조회하여 사용한다.
        Integer accountSeq = ContextHolder.exeContext().getUserAccountSeq();
        String privateCertificateUseYn = this.getPrivateCertificateUseYn(accountSeq);

        return privateCertificateUseYn;
    }

    public String getPrivateCertificateUseYn(Integer accountSeq ){
        String privateCertificateUseYn = registryProperty.getPrivateCertificateUseYn();

        // 플랫폼 사용자(ROLE : SYSTEM, USER) 이고, 해당 플랫폼에 등록된 레지스트리가 존재하면 해당 레지스트리 정보를 조회하여 사용한다.
        if ( accountSeq != null && accountSeq > 0) {
            IAccountRegistryMapper dao = sqlSession.getMapper(IAccountRegistryMapper.class);
            AccountRegistryVO accountRegistry = dao.getAccountRegistry(accountSeq, null);
            if (accountRegistry != null){
                privateCertificateUseYn = accountRegistry.getPrivateCertificateUseYn();
            }
        }

        return privateCertificateUseYn;
    }

    public String getPrivateCertificate(){

        // 플랫폼 사용자(ROLE : SYSTEM, USER) 이고, 해당 플랫폼에 등록된 레지스트리가 존재하면 해당 레지스트리 정보를 조회하여 사용한다.
        Integer accountSeq = ContextHolder.exeContext().getUserAccountSeq();
        String privateCertificate = this.getPrivateCertificate(accountSeq);

        return privateCertificate;
    }

    public String getPrivateCertificate(Integer accountSeq){
        String privateCertificate = registryProperty.getPrivateCertificate();

        // 플랫폼 사용자(ROLE : SYSTEM, USER) 이고, 해당 플랫폼에 등록된 레지스트리가 존재하면 해당 레지스트리 정보를 조회하여 사용한다.
        if ( accountSeq != null && accountSeq > 0) {
            IAccountRegistryMapper dao = sqlSession.getMapper(IAccountRegistryMapper.class);
            AccountRegistryVO accountRegistry = dao.getAccountRegistry(accountSeq, null);
            if (accountRegistry != null){
                privateCertificate = CryptoUtils.decryptAES(accountRegistry.getPrivateCertificate());
            }
        }

        return privateCertificate;
    }


}
