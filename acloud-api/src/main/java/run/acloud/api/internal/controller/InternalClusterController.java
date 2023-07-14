package run.acloud.api.internal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.ClusterService;
import run.acloud.api.configuration.service.ClusterValidService;
import run.acloud.commons.annotations.InHouse;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 3. 13.
 */
@Tag(name = "Internal Cluster", description = "내부호출용 Cluster 관련 기능을 제공한다.")
@InHouse
@Slf4j
@RestController
@RequestMapping(value = "/internal/cluster")
public class InternalClusterController {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterValidService clusterValidService;

    @Operation(summary = "클러스터 버전 동기화", description = "현재 등록된 클러스터의 버전을 조회하여 동기화 처리합니다.")
    @PutMapping(value = "/{clusterSeq}/version/sync")
    public void syncClusterVersion(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "cocktailClusterVersion", description = "칵테일에 등록된 클러스터 버전", required = true) @RequestParam String cocktailClusterVersion,
            @Parameter(name = "k8sClusterVersion", description = "현재 k8s 클러스터 버전", required = true) @RequestParam String k8sClusterVersion
    ) throws Exception {
        log.debug("[BEGIN] syncClusterVersion");

        clusterService.updateClusterVersionForSync(clusterSeq, cocktailClusterVersion, k8sClusterVersion, null, null);

        log.debug("[END  ] syncClusterVersion");
    }

//    @Operation(summary = "클러스터 버전 동기화", description = "현재 등록된 클러스터의 버전을 조회하여 동기화 처리합니다.")
//    @GetMapping(value = "/incluster/test")
//    public void incluster(
//
//    ) throws Exception {
//        log.debug("[BEGIN] incluster");
//
//        clusterValidService.inclustertest();
//
//        log.debug("[END  ] incluster");
//    }
}
