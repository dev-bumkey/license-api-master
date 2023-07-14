package run.acloud.api.configuration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.ClusterAccessAuthService;
import run.acloud.api.configuration.vo.ClusterAccessAuthVO;
import run.acloud.commons.constants.CommonConstants;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;

@Tag(name = "Cluster", description = "클러스터에 Access하는 Addon들에 대한 인가 관리 (Key 발급 및 만료 / 검증 처리)")
@Slf4j
@RequestMapping(value = "/api/cluster")
@RestController
@Validated
public class ClusterAccessAuthController {

	@Autowired
	private ClusterAccessAuthService clusterAccessAuthService;

	@Operation(summary = "Cluster의 Addon Type에 해당하는 Secret 키를 생성하여 응답", description = "addonType : MONITORING / CONTROLLER")
	@PostMapping(value = "/{clusterSeq}/secret")
	public String createClusterAccessSecret(
		@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "addonType", description = "Addon Type", required = true, schema = @Schema(allowableValues = {"CONTROLLER","MONITORING"})) @RequestParam String addonType) throws Exception {
		log.debug("[BEGIN] createClusterAccessAuthorization");

		String authKey;
		try {
			if (StringUtils.isBlank(addonType)) {
				throw new CocktailException("addon Type is null.", ExceptionType.InvalidParameter);
			}
			if (clusterSeq == null || clusterSeq < 1) {
				throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
			}

			/**
			 * cluster의 AuthKey 정보를 조회
			 */
			authKey = clusterAccessAuthService.createClusterAccessSecret(clusterSeq, addonType);
		}
		catch (CocktailException ce) {
			throw ce;
		}
		catch (Exception e) {
			throw new CocktailException("Create cluster Access Secret List Fail!!", e, ExceptionType.ClusterConditionInquireFail, ExceptionBiz.CLUSTER);
		}

		log.debug("[END  ] createClusterAccessAuthorization");

		return authKey;
	}

	@Operation(summary = "Cluster에 생성된 전체 Secret List 조회 : Frontend의 Cluster 메뉴에서 사용 예정..", description = "addonType : MONITORING / CONTROLLER")
	@GetMapping(value = "/{clusterSeq}/secrets")
	public List<ClusterAccessAuthVO> getClusterAccessSecrets(
		@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq) throws Exception {
		log.debug("[BEGIN] getClusterAccessSecrets");

		List<ClusterAccessAuthVO> clusterAccessAuthList;
		try {
			if (clusterSeq == null || clusterSeq < 1) {
				throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
			}

			/**
			 * cluster의 AuthKey 정보를 조회
			 */
			clusterAccessAuthList = clusterAccessAuthService.getClusterAccessSecrets(clusterSeq);
		}
		catch (CocktailException ce) {
			throw ce;
		}
		catch (Exception e) {
			throw new CocktailException("Get cluster Access Secret List Fail!!", e, ExceptionType.ClusterConditionInquireFail, ExceptionBiz.CLUSTER);
		}
		log.debug("[END  ] getClusterAccessSecrets");

		return clusterAccessAuthList;
	}


	@Operation(summary = "AccessKey(Secret) 조회 : Input = cluster_seq, addon_type", description = "addonType : MONITORING / CONTROLLER")
	@GetMapping(value = "/{clusterSeq}/secret")
	public String getClusterAccessSecret(
		@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "addonType", description = "Addon Type", required = true, schema = @Schema(allowableValues = {"CONTROLLER","MONITORING"})) @RequestParam String addonType) throws Exception {
		log.debug("[BEGIN] getClusterAccessSecret");

		ClusterAccessAuthVO clusterAccessAuthVO;
		try {
			if (StringUtils.isBlank(addonType)) {
				throw new CocktailException("addon Type is null.", ExceptionType.InvalidParameter);
			}
			if (clusterSeq == null || clusterSeq < 1) {
				throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
			}

			/**
			 * cluster의 AuthKey 정보를 조회
			 */
			clusterAccessAuthVO = clusterAccessAuthService.getClusterAccessSecret(clusterSeq, addonType);
		}
		catch (CocktailException ce) {
			throw ce;
		}
		catch (Exception e) {
			throw new CocktailException("Get cluster Access Secret Fail!!", e, ExceptionType.ClusterConditionInquireFail, ExceptionBiz.CLUSTER);
		}
		log.debug("[END  ] getClusterAccessSecret");

		return clusterAccessAuthVO.getAuthKey();
	}

	@Operation(summary = "Access Key (Secret)에 대한 만료 처리 : Input = cluster_seq, addon_type", description = "addonType : MONITORING / CONTROLLER")
	@PutMapping(value = "/{clusterSeq}/secret/expire")
	public boolean expireClusterAccessSecret(
		@Parameter(name = "clusterSeq", description = "cluster SEQ", required = true) @PathVariable Integer clusterSeq,
		@Parameter(name = "addonType", description = "Addon Type", required = true, schema = @Schema(allowableValues = {"CONTROLLER","MONITORING"})) @RequestParam String addonType) throws Exception {
		log.debug("[BEGIN] expireClusterAccessSecret");

		boolean result;
		try {
			if (StringUtils.isBlank(addonType)) {
				throw new CocktailException("addon Type is null.", ExceptionType.InvalidParameter);
			}
			if (clusterSeq == null || clusterSeq < 1) {
				throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
			}

			/**
			 * cluster의 AuthKey 만료 처리
			 */
			result = clusterAccessAuthService.expireClusterAccessSecret(null, clusterSeq, addonType);
		}
		catch (CocktailException ce) {
			throw ce;
		}
		catch (Exception e) {
			throw new CocktailException("Expire cluster Access Secret Fail!!", e, ExceptionType.ClusterConditionInquireFail, ExceptionBiz.CLUSTER);
		}

		log.debug("[END  ] expireClusterAccessSecret");

		return result;
	}

	@Operation(summary = "Access Key (Secret)에 대한 만료 처리 : Input = cluster_auth_seq", description = "addonType : MONITORING / CONTROLLER")
	@PutMapping(value = "/accessauth/{clusterAuthSeq}/expire")
	public boolean expireClusterAccessSecret(
		@Parameter(name = "clusterAuthSeq", description = "cluster Auth Seq", required = true) @PathVariable Integer clusterAuthSeq) throws Exception {
		log.debug("[BEGIN] expireClusterAccessSecret");

		boolean result;
		try {
			if (clusterAuthSeq == null || clusterAuthSeq < 1) {
				throw new CocktailException("clusterAuthSeq is null.", ExceptionType.InvalidParameter);
			}

			/**
			 * cluster의 AuthKey 만료 처리
			 */
			 result = clusterAccessAuthService.expireClusterAccessSecret(clusterAuthSeq, null, null);

			log.debug("[END  ] expireClusterAccessSecret");
		}
		catch (CocktailException ce) {
			throw ce;
		}
		catch (Exception e) {
			throw new CocktailException("Expire cluster Access Secret Fail!!", e, ExceptionType.ClusterConditionInquireFail, ExceptionBiz.CLUSTER);
		}

		return result;
	}

	@Operation(summary = "Signature 생성을 테스트 하기 위한 API.", description = "놋트")
	@GetMapping(value = "/signature/make")
	public String makeClusterAccessSignature( @Parameter(description = "cluster_seq", required=true) @RequestHeader(name = "acloud-addon-seq" ) Integer clusterSeq,
	                                          @Parameter(description = "cluster_id", required=true) @RequestHeader(name = "acloud-addon-key" ) String clusterId,
	                                          @Parameter(description = "addon_type", required=true, schema = @Schema(allowableValues = {"CONTROLLER","MONITORING"})) @RequestHeader(name = "acloud-addon-type" ) String ownerType,
	                                          @Parameter(description = "서명 생성 시점의 timestamp", required=true) @RequestHeader(name = "acloud-timestamp" ) String timestamp,
	                                          @Parameter(description = "Signature 생성시 사용한 HMAC Algorithm", required=true, schema = @Schema(allowableValues = {"HmacSHA256"})) @RequestHeader(name = "acloud-signature-method" ) String sigAlg,
	                                          HttpServletRequest request) throws Exception {
		log.debug("[BEGIN] makeClusterAccessSignature");
		String signature;
		try {
			/**
			 * 모든 파라미터가 필수 정보.
			 */
			if (clusterSeq == null || clusterSeq < 1 ||
				StringUtils.isBlank(clusterId) ||
				StringUtils.isBlank(ownerType) ||
				StringUtils.isBlank(timestamp) ||
				StringUtils.isBlank(sigAlg)) {
				throw new CocktailException("Request parameters are missing.", ExceptionType.InvalidParameter);
			}

			ownerType = ownerType.toUpperCase();
			Integer clusterSeqMasking = clusterSeq *CommonConstants.CLUSTER_SEQ_MASKING_VALUE;
			String clusterIdMd5 = DigestUtils.md5Hex(clusterId).toLowerCase();

			log.debug("Request Method : " + request.getMethod());
			log.debug("clusterSeq : " + clusterSeq + " / Masking Value : " + clusterSeqMasking);
			log.debug("clusterId : " + clusterId + " / clusterId MD5 Hash : " + clusterIdMd5);
			log.debug("ownerType : " + ownerType);
			log.debug("timestamp : " + timestamp);
			log.debug("sigAlg : " + sigAlg);

			ClusterAccessAuthVO clusterAccessAuthVO = clusterAccessAuthService.getClusterAccessSecret(clusterSeq, ownerType);
			String secret = clusterAccessAuthVO.getAuthKey();

			signature = clusterAccessAuthService.makeSignature(request.getMethod(), clusterSeqMasking.toString(), clusterIdMd5, ownerType, timestamp, sigAlg, secret);

		}
		catch (CocktailException ce) {
			throw ce;
		}
		catch (Exception e) {
			throw new CocktailException("make cluster Access Secret Fail!!", e, ExceptionType.ClusterConditionInquireFail, ExceptionBiz.CLUSTER);
		}

		log.debug("[END  ] makeClusterAccessSignature");

		return signature;
	}

	@Operation(summary = "Signature에 대한 Validation 체크. : Token 인증", description = "놋트")
	@GetMapping(value = "/signature/validate")
	public ResultVO validateClusterAccessSecret(@Parameter(description = "Signature", required=true) @RequestHeader(name = "acloud-signature" ) String signature,
	                                           @Parameter(description = "cluster_seq * 합의된 숫자규격", required=true) @RequestHeader(name = "acloud-addon-seq" ) String clusterSeq,
	                                           @Parameter(description = "MD5로 해싱된 cluster_id", required=true) @RequestHeader(name = "acloud-addon-key" ) String clusterId,
	                                           @Parameter(description = "addon_type", required=true, schema = @Schema(allowableValues = {"CONTROLLER","MONITORING"})) @RequestHeader(name = "acloud-addon-type" ) String ownerType,
	                                           @Parameter(description = "서명 생성 시점의 timestamp", required=true) @RequestHeader(name = "acloud-timestamp" ) String timestamp,
	                                           @Parameter(description = "Signature 생성시 사용한 HMAC Algorithm", required=true, schema = @Schema(allowableValues = {"HmacSHA256"})) @RequestHeader(name = "acloud-signature-method" ) String sigAlg,
	                                           HttpServletRequest request) throws Exception {
//		log.debug("[BEGIN] validateClusterAccessSecret");
		ResultVO resultVO = new ResultVO();
		boolean result = false;
		try {
			/**
             * 모든 파라미터가 필수 정보.
			 */
			if (StringUtils.isBlank(signature) ||
				StringUtils.isBlank(clusterSeq) ||
				StringUtils.isBlank(clusterId) ||
				StringUtils.isBlank(ownerType) ||
				StringUtils.isBlank(timestamp) ||
				StringUtils.isBlank(sigAlg)) {
				resultVO.setResult(false);
				resultVO.setMessage("Request parameters are missing.");

				return resultVO;
			}

			ownerType = ownerType.toUpperCase();
			Integer unmaskingClusterSeq = Integer.parseInt(clusterSeq) / CommonConstants.CLUSTER_SEQ_MASKING_VALUE;

			ClusterAccessAuthVO clusterAccessAuthVO = clusterAccessAuthService.getClusterAccessSecret(unmaskingClusterSeq, ownerType);

			String secret = clusterAccessAuthVO.getAuthKey();

			String verifySignature = clusterAccessAuthService.makeSignature(request.getMethod(), clusterSeq, clusterId, ownerType, timestamp, sigAlg, secret);

//			log.debug("signature : " + signature);
//			log.debug("verifySig : " + verifySignature);

			if(verifySignature.equals(signature)) {
				result = true;
			}
			else {
				result = false;
//				/* 오류 발생시 로그를 남기지 않음. 오류 발생 원인은 Client에서 message 내용을 확인해서 판단하도록 함.
				if(log.isDebugEnabled()) {
					Integer unmaskingSeq = Integer.parseInt(clusterSeq) / CommonConstants.CLUSTER_SEQ_MASKING_VALUE;
					log.error("============================================================================");
					log.error(String.format("ValidateClusterAccessSecret Failed : %s", "Signature mismatch"));
					log.error("signature : " + signature);
					log.error("clusterSeq : " + unmaskingSeq.toString());
					log.error("clusterId : " + clusterId);
					log.error("ownerType : " + ownerType);
					log.error("timestamp : " + timestamp);
					log.error("sigAlg : " + sigAlg);
				}
//				*/
			}
		}
		catch (CocktailException ce) {
//				/* 오류 발생시 로그를 남기지 않음. 오류 발생 원인은 Client에서 message 내용을 확인해서 판단하도록 함.
			if(log.isDebugEnabled()) {
				Integer unmaskingSeq = Integer.valueOf(0);
				if (clusterSeq != null) {
					unmaskingSeq = Integer.parseInt(clusterSeq) / CommonConstants.CLUSTER_SEQ_MASKING_VALUE;
				}
				log.error("============================================================================");
				log.error(String.format("ValidateClusterAccessSecret Failed : %s", ce.getMessage()));
				log.error("signature : " + signature);
				log.error("clusterSeq : " + unmaskingSeq.toString());
				log.error("clusterId : " + clusterId);
				log.error("ownerType : " + ownerType);
				log.error("timestamp : " + timestamp);
				log.error("sigAlg : " + sigAlg);
			}
//				*/

			resultVO.setResult(false);
			resultVO.setMessage(ce.getMessage());
			return resultVO;
		}
		catch (Exception e) {
			throw new CocktailException("Validate cluster Access Secret Fail!!", e, ExceptionType.ClusterConditionInquireFail, ExceptionBiz.CLUSTER);
		}

		resultVO.setResult(result);
		resultVO.setMessage("ok");
//		log.debug("[END  ] validateClusterAccessSecret");

		return resultVO;
	}

}
