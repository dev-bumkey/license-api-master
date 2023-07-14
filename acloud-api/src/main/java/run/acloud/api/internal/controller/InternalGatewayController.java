package run.acloud.api.internal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.serverless.service.ServerlessService;
import run.acloud.api.serverless.vo.GatewayCertificateVO;
import run.acloud.commons.annotations.InHouse;

import java.util.List;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 3. 13.
 */
@Tag(name = "Internal Gateway", description = "내부호출용 Gateway 관련 기능을 제공한다.")
@InHouse
@Slf4j
@RestController
@RequestMapping(value = "/internal/gateway")
public class InternalGatewayController {

    @Autowired
    private ServerlessService serverlessService;

    @GetMapping("/certificates")
    @Operation(summary = "Gateway 인증서 목록 조회", description = "Gateway 인증서 목록 조회한다.")
    public List<String> getGatewayCertificates() throws Exception {
    	log.debug("[BEGIN] getGatewayCertificates");

        List<String> certificates = serverlessService.getGatewayCertificates();

        log.debug("[END  ] getGatewayCertificates");

        return certificates;
    }

    @PostMapping(value = "/certificate")
    @Operation(summary = "Gateway 인증서 저장", description = "Gateway 인증서 저장한다.")
    public void addGatewayCertificate(
            @Parameter(name = "GatewayCertificate", description = "GatewayCertificate", required = true) @Validated @RequestBody GatewayCertificateVO gatewayCertificate

    ) throws Exception {

        log.debug("[BEGIN] addGatewayCertificate");

        serverlessService.addGatewayCertificate(gatewayCertificate);

        log.debug("[END  ] addGatewayCertificate");
    }
}
