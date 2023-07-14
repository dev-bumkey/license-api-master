/**
 * Cocktail APM, Acornsoft Inc.
 */
package run.acloud.api.build.util;

import org.apache.commons.lang3.StringUtils;
import run.acloud.api.build.constant.BuildConstants;
import run.acloud.api.build.enums.AutoTagSeqType;
import run.acloud.api.build.enums.StepType;
import run.acloud.api.build.vo.BuildAddVO;
import run.acloud.api.build.vo.BuildRunVO;
import run.acloud.api.build.vo.BuildVO;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.framework.context.ContextHolder;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Pattern;

public final class BuildUtils {


	/**
	 * 빌드후 생성할 이미지의 full URL 생성.
	 * 기존
	 *
	 * @param registryUrl
	 * @param buildRun
	 * @return
	 */
	public static String getFullImageUrl(String registryUrl, BuildRunVO buildRun){
		String imageName = registryUrl.replaceAll("https?://", "") + BuildConstants.FILE_SEPARATOR + buildRun.getRegistryName() + BuildConstants.FILE_SEPARATOR +  buildRun.getImageName();

		String fullImageUrl = String.format(BuildConstants.COCKTAIL_BUILD_IMAGE_TAG_FORMAT, imageName, buildRun.getTagName());
		return fullImageUrl;
	}

	/**
	 * callback에 현재 접속한 사용자의 사용자ID, 사용자 롤, 사용자 workspace 정보 셋팅
	 *
	 * @param callbackUrl
	 * @return
	 */
	public static String addParamToCallbackURL(String callbackUrl){
		Integer userSeq = ContextHolder.exeContext().getUserSeq();
		String userRole = ContextHolder.exeContext().getUserRole();
		Integer serviceSeq = ContextHolder.exeContext().getUserServiceSeq();

		// 파라메터 구성
		StringBuilder params = new StringBuilder();
		if(userSeq != null && userSeq.intValue() > 0){
			params.append(String.format("user-id=%d", userSeq));
		}
		if(StringUtils.isNotEmpty(userRole)){
			if(StringUtils.isNotEmpty(params)) {
				params.append("&");
			}
			params.append(String.format("user-role=%s", userRole));
		}
		if(serviceSeq != null && serviceSeq.intValue() > 0){
			if(StringUtils.isNotEmpty(params)) {
				params.append("&");
			}
			params.append(String.format("user-workspace=%d", userSeq));
		}

		String newCallbackUrl = String.format("%s?%s", callbackUrl, params.toString()); // callbackUrl에 header값 추가

		return newCallbackUrl;
	}

	/**
	 * Build log를 저장할 ID 생성 메서드
	 * @update 20230530, coolingi, init-zgq0ng-18-427-1238 => zgq0ng-18.427.1238-init
	 *
	 * @param stepType
	 * @param buildSeq
	 * @param buildRunSeq
	 * @param buildStepRunSeq
	 * @return
	 */
	public static String getBuildLogId(StepType stepType, Integer buildSeq, Integer buildRunSeq, Integer buildStepRunSeq){

		String resourcePrefix = ResourceUtil.getResourcePrefix();
		String prefix= String.format(BuildConstants.SUBJECT_PRE_FIXED, resourcePrefix, buildSeq, buildRunSeq);

		return makeLogId(stepType, prefix, buildStepRunSeq);
	}

	// stepType-ResourcePrefix-BuildSeq-BuildRunSeq-BuildStepRunSeq
	private static String makeLogId(StepType stepType, String prefix, Integer buildStepRunSeq){
		String logId = String.format(BuildConstants.SUBJECT_STEP_LOG, prefix, buildStepRunSeq, stepType.getCode().toLowerCase());
		return logId;
	}

	private static String AUTO_TAG_FORMAT = "%s-%s";
	public static String generateTagNameByAutoTagInfo(String tagPrefix, String regionTimeZone, Integer buildNo, AutoTagSeqType seqType){

		String tagSeq = null;

		// 각 tag 유형에 따른 sequence 생성.
		switch (seqType){
			case DATETIME: // ZonedDateTime 객체 사용
				ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of(regionTimeZone));
				tagSeq = seqType.getSequence(zdt);
				break;
			case SEQUENCE:
				tagSeq = seqType.getSequence(buildNo);
				break;
		}

		return String.format(AUTO_TAG_FORMAT, tagPrefix, tagSeq);

	}

	public static void setBuildServerTLSFromBuildToBuildAdd(BuildAddVO buildAdd, BuildVO prevBuildVO){

		// TLS 사용이고, 수정전에도 TLS 사용이었으면, 없는 값들은 이전 데이터에서 가져온다.
		if( "Y".equals(buildAdd.getBuildServerTlsVerify()) && buildAdd.getBuildServerHost() != null ){

			if( StringUtils.isNotBlank(buildAdd.getBuildServerCacrt()) ){
				buildAdd.setBuildServerCacrt(CryptoUtils.encryptAES(buildAdd.getBuildServerCacrt()));
			} else if( StringUtils.isNotBlank(prevBuildVO.getBuildServerCacrt()) ){
				buildAdd.setBuildServerCacrt(prevBuildVO.getBuildServerCacrt());
			}

			if( StringUtils.isNotBlank(buildAdd.getBuildServerClientCert()) ){
				buildAdd.setBuildServerCacrt(CryptoUtils.encryptAES(buildAdd.getBuildServerClientCert()));
			} else if( StringUtils.isNotBlank(prevBuildVO.getBuildServerClientCert()) ){
				buildAdd.setBuildServerCacrt(prevBuildVO.getBuildServerClientCert());
			}

			if( StringUtils.isNotBlank(buildAdd.getBuildServerClientKey()) ){
				buildAdd.setBuildServerCacrt(CryptoUtils.encryptAES(buildAdd.getBuildServerClientKey()));
			} else if( StringUtils.isNotBlank(prevBuildVO.getBuildServerClientKey()) ){
				buildAdd.setBuildServerCacrt(prevBuildVO.getBuildServerClientKey());
			}
		}
	}

	/**
	 * TLS 사용이고, 수정전에도 TLS 사용이었으면, 없는 값들은 이전 데이터에서 가져온다.
	 *
	 * @param buildAdd
	 * @param provBuildRunVO
	 */
	public static void setBuildServerTLSFromBuildRunToBuildAdd(BuildAddVO buildAdd, BuildRunVO provBuildRunVO){

		// TLS 사용이고, 수정전에도 TLS 사용이었으면, 없는 값들은 이전 데이터에서 가져온다.
		if( "Y".equals(buildAdd.getBuildServerTlsVerify()) && buildAdd.getBuildServerHost() != null ){

			if( StringUtils.isNotBlank(buildAdd.getBuildServerCacrt()) ){
				buildAdd.setBuildServerCacrt(CryptoUtils.encryptAES(buildAdd.getBuildServerCacrt()));
			} else if( StringUtils.isNotBlank(provBuildRunVO.getBuildServerCacrt()) ){
				buildAdd.setBuildServerCacrt(provBuildRunVO.getBuildServerCacrt());
			}

			if( StringUtils.isNotBlank(buildAdd.getBuildServerClientCert()) ){
				buildAdd.setBuildServerCacrt(CryptoUtils.encryptAES(buildAdd.getBuildServerClientCert()));
			} else if( StringUtils.isNotBlank(provBuildRunVO.getBuildServerClientCert()) ){
				buildAdd.setBuildServerCacrt(provBuildRunVO.getBuildServerClientCert());
			}

			if( StringUtils.isNotBlank(buildAdd.getBuildServerClientKey()) ){
				buildAdd.setBuildServerCacrt(CryptoUtils.encryptAES(buildAdd.getBuildServerClientKey()));
			} else if( StringUtils.isNotBlank(provBuildRunVO.getBuildServerClientKey()) ){
				buildAdd.setBuildServerCacrt(provBuildRunVO.getBuildServerClientKey());
			}
		}
	}

	public static final Pattern BUILD_IMAGE_NAME = Pattern.compile("^[a-z0-9]([a-z0-9-._]*[a-z0-9])*$");

    public static boolean isValidImageName(String imageName) {
        if (StringUtils.isNotBlank(imageName)
				&& !StringUtils.endsWith(imageName, "/")
				&& !StringUtils.startsWith(imageName, "/")
		) {
			for (String s : imageName.split("/")) {
				if (!isValidName(s)) {
					return false;
				}
			}

			return true;
        } else {
			return false;
		}
    }

    public static boolean isValidName(String name) {
        if (StringUtils.isNotBlank(name)) {
			if (BUILD_IMAGE_NAME.matcher(name).matches()) {
				return true;
			}
        }

		return false;
    }
}
