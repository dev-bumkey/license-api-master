package run.acloud.api.resource.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.resource.enums.CertIssuerScope;
import run.acloud.api.resource.service.CertManagerService;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.AuthUtils;
import run.acloud.framework.context.ContextHolder;

import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 27.
 */
@Tag(name = "Kubernetes CRD Cert-Manager Management", description = "쿠버네티스 CRD Cert-Manager에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/cert")
@RestController
@Validated
public class CertManagerController {

    @Autowired
    private CertManagerService certManagerService;


    @PostMapping("/private/issuer")
    @Operation(summary = "사설인증서 발급자을 추가한다", description = "사설인증서 발급자를 추가한다.")
    public void addCertPrivateIssuer(
            @Parameter(name = "issuer", description = "추가하려는 issuer") @RequestBody K8sCRDIssuerIntegrateVO issuer
    ) throws Exception {

        log.debug("[BEGIN] addCertPrivateIssuer");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        certManagerService.addCertPrivateIssuer(issuer);

        log.debug("[END  ] addCertPrivateIssuer");
    }

    @GetMapping("/private/issuer/template")
    @Operation(summary = "사설인증서 발급자의 yaml template을 조회", description = "사설인증서 발급자의 yaml template을 조회한다.")
    public Map<String, Map<String, Object>> getCertPrivateIssuerDefaultYamlTemplate() throws Exception {

        log.debug("[BEGIN] getCertPrivateIssuerDefaultYamlTemplate");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        Map<String, Map<String, Object>> result = certManagerService.genCertPrivateIssuerDefaultYamlTemplate();

        log.debug("[END  ] getCertPrivateIssuerDefaultYamlTemplate");

        return result;
    }

    @GetMapping("/private/issuer")
    @Operation(summary = "사설인증서 발급자 목록 조회", description = "사설인증서 발급자 목록을 조회한다.")
    public List<K8sCRDIssuerVO> getCertPrivateIssuers(
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(name = "clusterSeq", description = "cluster sequence") @RequestParam(required = false) Integer clusterSeq,
            @Parameter(name = "withEvent", description = "이벤트 정보 포함 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "withEvent", required = false, defaultValue = "false") boolean withEvent
    ) throws Exception {

        log.debug("[BEGIN] getCertPrivateIssuers");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        List<K8sCRDIssuerVO> result = certManagerService.getCertPrivateIssuers(accountSeq, clusterSeq, withEvent);

        log.debug("[END  ] getCertPrivateIssuers");

        return result;
    }

    @GetMapping("/private/cluster/{clusterSeq}/issuer/{issuerName:.+}")
    @Operation(summary = "사설인증서 클러스터 발급자 배포정보 상세 조회", description = "사설인증서 클러스터 발급자의 배포정보를 상세 조회한다.")
    public K8sCRDIssuerVO getCertPrivateClusterIssuer(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "issuerName", description = "발급자 이름", required = true) @PathVariable String issuerName
    ) throws Exception {

        log.debug("[BEGIN] getCertPrivateClusterIssuer");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        K8sCRDIssuerVO result = certManagerService.getCertPrivateIssuer(CertIssuerScope.CLUSTER, clusterSeq, null, issuerName);

        log.debug("[END  ] getCertPrivateClusterIssuer");

        return result;
    }

    @GetMapping("/private/cluster/{clusterSeq}/namespace/{namespaceName}/issuer/{issuerName:.+}")
    @Operation(summary = "사설인증서 발급자 배포정보 상세 조회", description = "사설인증서 발급자의 배포정보를 상세 조회한다.")
    public K8sCRDIssuerVO getCertPrivateIssuer(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "네임스페이스", required = true) @PathVariable String namespaceName,
            @Parameter(name = "issuerName", description = "발급자 이름", required = true) @PathVariable String issuerName
    ) throws Exception {

        log.debug("[BEGIN] getCertPrivateIssuer");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        K8sCRDIssuerVO result = certManagerService.getCertPrivateIssuer(CertIssuerScope.NAMESPACED, clusterSeq, namespaceName, issuerName);

        log.debug("[END  ] getCertPrivateIssuer");

        return result;
    }

    @GetMapping("/private/deploy/{deployType}/cluster/{clusterSeq}/issuer/{issuerName:.+}")
    @Operation(summary = "사설인증서 클러스터 발급자 설정정보 상세 조회", description = "사설인증서 클러스터 발급자의 설정정보를 상세 조회한다.")
    public K8sCRDIssuerIntegrateVO getCertPrivateClusterIssuerConfig(
            @Parameter(name = "deployType", description = "deployType", schema = @Schema(allowableValues = {DeployType.Names.GUI , DeployType.Names.YAML}), required = true) @PathVariable String deployType,
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "issuerName", description = "발급자 이름", required = true) @PathVariable String issuerName
    ) throws Exception {

        log.debug("[BEGIN] getCertPrivateClusterIssuerConfig");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        K8sCRDIssuerIntegrateVO result = certManagerService.getCertPrivateIssuerConfig(DeployType.valueOf(deployType), CertIssuerScope.CLUSTER, clusterSeq, null, issuerName);

        log.debug("[END  ] getCertPrivateClusterIssuerConfig");

        return result;
    }

    @GetMapping("/private/deploy/{deployType}/cluster/{clusterSeq}/namespace/{namespaceName}/issuer/{issuerName:.+}")
    @Operation(summary = "사설인증서 발급자 설정정보 상세 조회", description = "사설인증서 발급자의 설정정보를 상세 조회한다.")
    public K8sCRDIssuerIntegrateVO getCertPrivateIssuerConfig(
            @Parameter(name = "deployType", description = "deployType", schema = @Schema(allowableValues = {DeployType.Names.GUI , DeployType.Names.YAML}), required = true) @PathVariable String deployType,
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "네임스페이스", required = true) @PathVariable String namespaceName,
            @Parameter(name = "issuerName", description = "발급자 이름", required = true) @PathVariable String issuerName
    ) throws Exception {

        log.debug("[BEGIN] getCertPrivateIssuerConfig");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        K8sCRDIssuerIntegrateVO result = certManagerService.getCertPrivateIssuerConfig(DeployType.valueOf(deployType), CertIssuerScope.NAMESPACED, clusterSeq, namespaceName, issuerName);

        log.debug("[END  ] getCertPrivateIssuerConfig");

        return result;
    }


    @PutMapping("/private/cluster/{clusterSeq}/issuer/{issuerName:.+}")
    @Operation(summary = "사설인증서 클러스터 발급자 배포정보 수정", description = "사설인증서 클러스터 발급자의 배포정보를 수정한다.")
    public void editCertPrivateClusterIssuer(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "issuerName", description = "발급자 이름", required = true) @PathVariable String issuerName,
            @Parameter(name = "issuer", description = "수정하려는 issuer") @RequestBody K8sCRDIssuerIntegrateVO issuer
    ) throws Exception {

        log.debug("[BEGIN] editCertPrivateClusterIssuer");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        certManagerService.editCertPrivateIssuer(clusterSeq, null, issuerName, issuer);

        log.debug("[END  ] editCertPrivateClusterIssuer");

    }

    @PutMapping("/private/cluster/{clusterSeq}/namespace/{namespaceName}/issuer/{issuerName:.+}")
    @Operation(summary = "사설인증서 발급자 배포정보 수정", description = "사설인증서 발급자의 배포정보를 수정한다.")
    public void editCertPrivateIssuer(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "네임스페이스", required = true) @PathVariable String namespaceName,
            @Parameter(name = "issuerName", description = "발급자 이름", required = true) @PathVariable String issuerName,
            @Parameter(name = "issuer", description = "수정하려는 issuer") @RequestBody K8sCRDIssuerIntegrateVO issuer
    ) throws Exception {

        log.debug("[BEGIN] editCertPrivateIssuer");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        certManagerService.editCertPrivateIssuer(clusterSeq, namespaceName, issuerName, issuer);

        log.debug("[END  ] editCertPrivateIssuer");

    }

    @DeleteMapping("/private/cluster/{clusterSeq}/issuer/{issuerName:.+}")
    @Operation(summary = "사설인증서 클러스터 발급자 삭제", description = "사설인증서 클러스터 발급자를 삭제한다.")
    public void deleteCertPrivateClusterIssuer(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "issuerName", description = "발급자 이름", required = true) @PathVariable String issuerName
    ) throws Exception {

        log.debug("[BEGIN] deleteCertPrivateClusterIssuer");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        certManagerService.deleteCertPrivateIssuer(CertIssuerScope.CLUSTER, clusterSeq, null, issuerName);

        log.debug("[END  ] deleteCertPrivateClusterIssuer");

    }

    @DeleteMapping("/private/cluster/{clusterSeq}/namespace/{namespaceName}/issuer/{issuerName:.+}")
    @Operation(summary = "사설인증서 발급자 삭제", description = "사설인증서 발급자를 삭제한다.")
    public void deleteCertPrivateIssuer(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "네임스페이스", required = true) @PathVariable String namespaceName,
            @Parameter(name = "issuerName", description = "발급자 이름", required = true) @PathVariable String issuerName
    ) throws Exception {

        log.debug("[BEGIN] deleteCertPrivateIssuer");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        certManagerService.deleteCertPrivateIssuer(CertIssuerScope.NAMESPACED, clusterSeq, namespaceName, issuerName);

        log.debug("[END  ] deleteCertPrivateIssuer");

    }

    @PostMapping("/private/certificate")
    @Operation(summary = "사설인증서 인증서을 추가한다", description = "사설인증서 인증서를 추가한다.")
    public void addCertPrivateCertificate(
            @Parameter(name = "certificate", description = "추가하려는 certificate") @RequestBody K8sCRDCertificateIntegrateVO certificate
    ) throws Exception {

        log.debug("[BEGIN] addCertPrivateCertificate");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        certManagerService.addCertPrivateCertificate(certificate);

        log.debug("[END  ] addCertPrivateCertificate");
    }

    @GetMapping("/private/certificate/template")
    @Operation(summary = "사설인증서 certificate의 yaml template을 조회", description = "사설인증서 certificate의 yaml template을 조회한다.")
    public String getCertPrivateCertificateDefaultYamlTemplate() throws Exception {

        log.debug("[BEGIN] getCertPrivateCertificateDefaultYamlTemplate");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        String result = certManagerService.genCertPrivateCertificateDefaultYamlTemplate();

        log.debug("[END  ] getCertPrivateCertificateDefaultYamlTemplate");

        return result;
    }

    @GetMapping("/private/certificate")
    @Operation(summary = "사설인증서 certificate 목록 조회", description = "사설인증서 certificate 목록을 조회한다.")
    public List<K8sCRDCertificateVO> getCertPrivateCertificates(
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(name = "clusterSeq", description = "cluster sequence") @RequestParam(required = false) Integer clusterSeq,
            @Parameter(name = "withEvent", description = "이벤트 정보 포함 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "withEvent", required = false, defaultValue = "false") boolean withEvent
    ) throws Exception {

        log.debug("[BEGIN] getCertPrivateCertificates");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        List<K8sCRDCertificateVO> result = certManagerService.getCertPrivateCertificates(accountSeq, clusterSeq, withEvent);

        log.debug("[END  ] getCertPrivateCertificates");

        return result;
    }

    @GetMapping("/private/cluster/{clusterSeq}/namespace/{namespaceName}/certificate/{certificateName:.+}")
    @Operation(summary = "사설인증서 인증서 배포정보 상세 조회", description = "사설인증서 인증서의 배포정보를 상세 조회한다.")
    public K8sCRDCertificateVO getCertPrivatecCertificate(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "네임스페이스", required = true) @PathVariable String namespaceName,
            @Parameter(name = "certificateName", description = "인증서 이름", required = true) @PathVariable String certificateName,
            @Parameter(name = "withCR", description = "CertificateRequest 정보 포함 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "withCR", required = false, defaultValue = "true") boolean withCR
    ) throws Exception {

        log.debug("[BEGIN] getCertPrivatecCertificate");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        K8sCRDCertificateVO result = certManagerService.getCertPrivateCertificate(clusterSeq, namespaceName, certificateName, withCR);

        log.debug("[END  ] getCertPrivatecCertificate");

        return result;
    }

    @GetMapping("/private/deploy/{deployType}/cluster/{clusterSeq}/namespace/{namespaceName}/certificate/{certificateName:.+}")
    @Operation(summary = "사설인증서 인증서 설정정보 상세 조회", description = "사설인증서 인증서의 설정정보를 상세 조회한다.")
    public K8sCRDCertificateIntegrateVO getCertPrivateCertificateConfig(
            @Parameter(name = "deployType", description = "deployType", schema = @Schema(allowableValues = {DeployType.Names.GUI , DeployType.Names.YAML}), required = true) @PathVariable String deployType,
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "네임스페이스", required = true) @PathVariable String namespaceName,
            @Parameter(name = "certificateName", description = "인증서 이름", required = true) @PathVariable String certificateName
    ) throws Exception {

        log.debug("[BEGIN] getCertPrivateCertificateConfig");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        K8sCRDCertificateIntegrateVO result = certManagerService.getCertPrivateCertificateConfig(DeployType.valueOf(deployType), clusterSeq, namespaceName, certificateName);

        log.debug("[END  ] getCertPrivateCertificateConfig");

        return result;
    }

    @PutMapping("/private/cluster/{clusterSeq}/namespace/{namespaceName}/certificate/{certificateName:.+}")
    @Operation(summary = "사설인증서 인증서 배포정보 수정", description = "사설인증서 인증서의 배포정보를 수정한다.")
    public void editCertPrivateCertificate(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "네임스페이스", required = true) @PathVariable String namespaceName,
            @Parameter(name = "certificateName", description = "인증서 이름", required = true) @PathVariable String certificateName,
            @Parameter(name = "certificate", description = "수정하려는 certificate") @RequestBody K8sCRDCertificateIntegrateVO certificate
    ) throws Exception {

        log.debug("[BEGIN] editCertPrivateCertificate");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        certManagerService.editCertPrivateCertificate(clusterSeq, namespaceName, certificateName, certificate);

        log.debug("[END  ] editCertPrivateCertificate");

    }

    @DeleteMapping("/private/cluster/{clusterSeq}/namespace/{namespaceName}/certificate/{certificateName:.+}")
    @Operation(summary = "사설인증서 인증서 삭제", description = "사설인증서 인증서를 삭제한다.")
    public void deleteCertPrivateCertificate(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "네임스페이스", required = true) @PathVariable String namespaceName,
            @Parameter(name = "certificateName", description = "인증서 이름", required = true) @PathVariable String certificateName
    ) throws Exception {

        log.debug("[BEGIN] deleteCertPrivateCertificate");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        certManagerService.deleteCertPrivateCertificate(clusterSeq, namespaceName, certificateName);

        log.debug("[END  ] deleteCertPrivateCertificate");

    }

    @PostMapping("/public/certificate")
    @Operation(summary = "공인인증서 인증서을 추가한다", description = "공인인증서 인증서를 추가한다.")
    public void addCertPublicCertificate(
            @Parameter(name = "certificate", description = "추가하려는 certificate") @RequestBody PublicCertificateAddVO certificate
    ) throws Exception {

        log.debug("[BEGIN] addCertPublicCertificate");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        certManagerService.addCertPublicCertificate(certificate);

        log.debug("[END  ] addCertPublicCertificate");
    }

    @GetMapping("/public/certificate")
    @Operation(summary = "공인인증서 목록 조회", description = "공인인증서 목록을 조회한다.")
    public List<PublicCertificateVO> getCertPublicCertificates(
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq
    ) throws Exception {

        log.debug("[BEGIN] getCertPublicCertificates");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        List<PublicCertificateVO> result = certManagerService.getCertPublicCertificates(accountSeq);

        log.debug("[END  ] getCertPublicCertificates");

        return result;
    }

    @GetMapping("/public/certificate/{publicCertificateSeq}")
    @Operation(summary = "공인인증서 상세 조회", description = "공인인증서의 상세정보를 조회한다.")
    public PublicCertificateDetailVO getCertPublicCertificate(
            @Parameter(name = "publicCertificateSeq", description = "공인인증서 sequence", required = true) @PathVariable Integer publicCertificateSeq,
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq
    ) throws Exception {

        log.debug("[BEGIN] getCertPublicCertificate");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        PublicCertificateDetailVO result = certManagerService.getCertPublicCertificate(accountSeq, publicCertificateSeq, true, true, true);

        log.debug("[END  ] getCertPublicCertificate");

        return result;
    }

    @PutMapping("/public/certificate/{publicCertificateSeq}")
    @Operation(summary = "공인인증서 정보 수정", description = "공인인증서 정보를 수정한다.")
    public void editCertPublicCertificate(
            @Parameter(name = "publicCertificateSeq", description = "공인인증서 sequence", required = true) @PathVariable Integer publicCertificateSeq,
            @Parameter(name = "certificate", description = "수정하려는 certificate") @RequestBody PublicCertificateAddVO certificate
    ) throws Exception {

        log.debug("[BEGIN] editCertPublicCertificate");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        certificate.setPublicCertificateSeq(publicCertificateSeq);

        certManagerService.editCertPublicCertificate(certificate);

        log.debug("[END  ] editCertPublicCertificate");

    }

    @DeleteMapping("/public/certificate/{publicCertificateSeq}")
    @Operation(summary = "공인인증서 삭제", description = "공인인증서를 삭제한다.")
    public PublicCertificateDetailVO deleteCertPublicCertificate(
            @Parameter(name = "publicCertificateSeq", description = "공인인증서 sequence", required = true) @PathVariable Integer publicCertificateSeq,
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq
    ) throws Exception {

        log.debug("[BEGIN] deleteCertPublicCertificate");

        /**
         * DevOps 권한의 사용자는 수정이 불가능함.
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        PublicCertificateDetailVO result = certManagerService.deleteCertPublicCertificate(accountSeq, publicCertificateSeq);

        log.debug("[END  ] deleteCertPublicCertificate");

        return result;
    }
}
