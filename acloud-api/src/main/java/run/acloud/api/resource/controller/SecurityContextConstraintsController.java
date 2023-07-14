package run.acloud.api.resource.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.resource.service.SecurityContextConstraintsService;
import run.acloud.api.resource.vo.SecurityContextConstraintsDetailVO;
import run.acloud.api.resource.vo.SecurityContextConstraintsNameVO;
import run.acloud.api.resource.vo.SecurityContextConstraintsVO;
import run.acloud.framework.exception.CocktailException;

import java.util.List;

/**
 * @author coolingi@acornsoft.io
 * Created on 2022. 11. 09.
 */
@Tag(name = "Openshift SCC", description = "Openshift SecurityContextConstraints 관련 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/scc")
@RestController
@Validated
public class SecurityContextConstraintsController {

    @Autowired
    private SecurityContextConstraintsService securityContextConstraintsService;

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}")
    @Operation(summary = "클러스터의 scc 목록을 조회한다.", description = "클러스터의 scc 목록을 조회한다.")
    public List<SecurityContextConstraintsVO> listSecurityContextConstrants(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq
    ) throws Exception {

        log.debug("[BEGIN] addSecurityContextConstrants");

        List<SecurityContextConstraintsVO> result = securityContextConstraintsService.listSecurityContextConstrants(clusterSeq);

        log.debug("[END  ] addSecurityContextConstrants");

        return result;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/scc/{sccName:.+}")
    @Operation(summary = "SCC 상세 정보을 조회한다.", description = "SCC 상세 정보을 조회한다.")
    public SecurityContextConstraintsDetailVO getSecurityContextConstrants(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "sccName", description = "sccName", required = true) @PathVariable String sccName
    ) throws CocktailException {

        log.debug("[BEGIN] addSecurityContextConstrants");

        SecurityContextConstraintsDetailVO result = securityContextConstraintsService.getSecurityContextConstrants(clusterSeq, sccName);

        log.debug("[END  ] addSecurityContextConstrants");

        return result;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}")
    @Operation(summary = "Namespace에 적용된 scc 정보을 조회한다.", description = "Namespace에 적용된 scc 정보을 조회한다.")
    public List<SecurityContextConstraintsNameVO> getSecurityContextConstrants(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq
    ) throws CocktailException {

        log.debug("[BEGIN] addSecurityContextConstrants");

        List<SecurityContextConstraintsNameVO> result = securityContextConstraintsService.getSecurityContextConstrantsInNamespce(clusterSeq, namespaceName);

        log.debug("[END  ] addSecurityContextConstrants");

        return result;
    }

    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/scc/{sccName:.+}")
    @Operation(summary = "Namespace에 선택한 SecurityContextConstrant를 추가한다.", description = "Namespace에 scc name에 해당하는 SecurityContextConstrant를 추가한다.")
    public List<SecurityContextConstraintsNameVO> addSecurityContextConstrants(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "sccName", description = "sccName", required = true) @PathVariable String sccName
    ) throws Exception {

        log.debug("[BEGIN] addSecurityContextConstrants");

        securityContextConstraintsService.addSCC(clusterSeq, namespaceName, sccName);

        // namespace의 scc 리스트 조회
        List<SecurityContextConstraintsNameVO> result = securityContextConstraintsService.getSecurityContextConstrantsInNamespce(clusterSeq, namespaceName);

        log.debug("[END  ] addSecurityContextConstrants");

        return result;
    }

    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/scc/{sccName:.+}")
    @Operation(summary = "Namespace에 SCC 를 삭제한다.", description = "Namespace에 SCC를 삭제한다.")
    public List<SecurityContextConstraintsNameVO> removeSecurityContextConstrants(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "sccName", description = "sccName", required = true) @PathVariable String sccName
    ) throws Exception {

        log.debug("[BEGIN] addSecurityContextConstrants");

        securityContextConstraintsService.deleteSCC(clusterSeq, namespaceName, sccName);

        // namespace의 scc 리스트 조회
        List<SecurityContextConstraintsNameVO> result = securityContextConstraintsService.getSecurityContextConstrantsInNamespce(clusterSeq, namespaceName);

        log.debug("[END  ] addSecurityContextConstrants");

        return result;
    }


}
