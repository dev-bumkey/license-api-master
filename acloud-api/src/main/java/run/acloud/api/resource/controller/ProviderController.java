package run.acloud.api.resource.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.cserver.service.ProviderService;
import run.acloud.api.resource.vo.ProviderVO;

import java.util.List;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 2. 1.
 */
@Tag(name = "Cluster Provider", description = "클러스터 Provider에 대한 관리 기능을 제공 한다.")
@Slf4j
@RestController
@RequestMapping(value = "/api/provider")
public class ProviderController {
	
	@Autowired
    private ProviderService providerService;

    @Operation(summary = "Provider 목록 조회", description = "지원하는 클러스터 Provider 목록을 응답한다.")
    @GetMapping(value = "")
    public List<ProviderVO> getProviders() {
    	log.debug("[BEGIN] getProviders");
    	
    	List<ProviderVO> providers = providerService.getProviders();
    	
    	log.debug("[END  ] getProviders");
    	return providers;
    }

    @Operation(summary = "Provider 코드 목록 조회", description = "지원하는 클러스터 Provider의 코드 목록을 응답한다.")
    @GetMapping(value = "/codes")
    public List<CodeVO> getProviderCodes() throws Exception {
        return this.providerService.getProviderCodes();
    }

}
