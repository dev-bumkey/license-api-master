package run.acloud.api.terminal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.commons.annotations.InHouse;

@InHouse
@Tag(name = "Terminal", description = "터미널 관련 기능을 제공한다.")
@Slf4j
@RestController
@Validated
@RequestMapping(value = "/api/terminal")
public class TerminalController {

	@PostMapping(value = "/{clusterSeq}/{namespace}/{podName}/{containerName}/open/log")
	@Operation(summary = "터미널 open 로그 등록", description = "터미널 open 로그 등록")
	public void addLogTerminalOnOpen(
			@Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "namespace", description = "namespace", required = true) @PathVariable String namespace,
			@Parameter(name = "podName", description = "podName", required = true) @PathVariable String podName,
			@Parameter(name = "containerName", description = "containerName") @PathVariable String containerName,
			@Parameter(name = "success", description = "성공여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "success", required = false, defaultValue = "true") boolean success
	) throws Exception {
		log.debug("[BEGIN] addLogTerminalOnOpen");
		log.debug("addLogTerminalOnOpen : clusterSeq[{}], namespace[{}], podName[{}], containerName[{}], success[{}]", clusterSeq, namespace, podName, containerName, success);
		log.debug("[END  ] addLogTerminalOnOpen");

	}

	@PostMapping(value = "/{clusterSeq}/{namespace}/{podName}/{containerName}/close/log")
	@Operation(summary = "터미널 close 로그 등록", description = "터미널 close 로그 등록")
	public void addLogTerminalOnClose(
			@Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
			@Parameter(name = "namespace", description = "namespace", required = true) @PathVariable String namespace,
			@Parameter(name = "podName", description = "podName", required = true) @PathVariable String podName,
			@Parameter(name = "containerName", description = "containerName") @PathVariable String containerName
	) throws Exception {
		log.debug("[BEGIN] addLogTerminalOnClose");
		log.debug("addLogTerminalOnClose : clusterSeq[{}], namespace[{}], podName[{}], containerName[{}]", clusterSeq, namespace, podName, containerName);
		log.debug("[END  ] addLogTerminalOnClose");

	}

}
