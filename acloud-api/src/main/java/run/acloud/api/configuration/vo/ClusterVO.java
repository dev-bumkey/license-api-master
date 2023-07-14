package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.configuration.enums.ClusterTenancy;
import run.acloud.api.cserver.enums.VolumePlugIn;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.ProviderVO;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Schema(description = "클러스터 모델")
public class ClusterVO extends HasUseYnVO {
	
	@Schema(title = "클러스터 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer clusterSeq;

	@Schema(title = "프로바이더 계정 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer providerAccountSeq;

	@Schema(title = "providerAccount 정보")
	private ProviderAccountVO providerAccount;

	@Schema(title = "빌링 프로바이더 계정 번호")
	private Integer billingProviderAccountSeq;

	@Schema(title = "클러스터 타입", requiredMode = Schema.RequiredMode.REQUIRED)
	private ClusterType clusterType;
	
	@Schema(title = "클러스터 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	private String clusterName;
	
	@Schema(title = "클러스터 설명", requiredMode = Schema.RequiredMode.REQUIRED)
	private String description;
	
	@Schema(title = "리젼 코드", requiredMode = Schema.RequiredMode.REQUIRED)
	private String regionCode;

	@Schema(title = "클러스터 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String clusterId;

	@Schema(title = "클러스터 상태")
	private String clusterState;

	@Schema(title = "Project ID : providerCode가 GCP일 경우")
	private String billingGroupId;
	
	@Schema(title = "큐브 클러스터 타입")
	private CubeType cubeType;
	
	@Schema(title = "설치 유형")
	private AuthType authType;

	@Schema(title = "Master URL")
	private String apiUrl;

	@Schema(title = "User id : 설치 유형이 CERT일 경우")
	private String apiKey;

	@Schema(title = "클러스터 인증정보")
	private String apiSecret;

	@Schema(title = "클러스터 인증정보(ca)")
	private String serverAuthData;

	@Schema(title = "클러스터 인증정보(client data)")
	private String clientAuthData;

	@Schema(title = "클러스터 인증정보(client key)")
	private String clientKeyData;

	@Schema(title = "NodePort 지원 여부")
	private String nodePortSupported;
	
	@Schema(title = "Ingress 지원 여부")
	private String ingressSupported;

	@Schema(title = "LoadBalancer 지원 여부")
	private String loadbalancerSupported;

	@Schema(title = "Persistent Volume 지원 여부")
	private String persistentVolumeSupported;

	@Schema(title = "Kubernetes 버전")
	private String k8sVersion;

	/** R4.1.1 : 2020.03.13 : for AWS Cloud Access Info **/
	@Schema(title = "Public Cloud 프로바이더 계정 번호", description = "for AWS Cloud Access Info")
	private Integer cloudProviderAccountSeq;

	@Schema(title = "Public Cloud 프로바이더 계정 정보(EKS)")
	private ProviderAccountVO cloudProviderAccount;

	@Schema(title = "Public Cloud Access Information", description = "for AWS Cloud Access Info")
	private PublicCloudAccessInfoVO publicCloudAccessInfo;

	/** R4.4.0 : 2020.06.25 : for Cluster Tenancy **/
	@Schema(title = "Cluster Tenancy Type")
	private ClusterTenancy clusterTenancy;

	@Schema(title = "NodePort URL", description = "NodePort를 지원할 경우 필수")
	private String nodePortUrl;

	@Schema(title = "NodePort 대역대", description = "NodePort를 지원할 경우 필수", example = "30000-32767")
	private String nodePortRange;

	@Schema(title = "Region Name")
	private String regionName;

	public String getRegionName() {
		if (StringUtils.isNotBlank(regionCode) && providerAccount != null && providerAccount.getProviderCode() != null && CollectionUtils.isNotEmpty(providerAccount.getProviderCode().getRegionCodes())) {
			regionName = providerAccount.getProviderCode().getRegionCodes().stream().filter(rc -> (StringUtils.equalsIgnoreCase(rc.getRegionCode(), regionCode))).map(ProviderRegionCode::getRegionName).findFirst().orElseGet(() ->null);
		} else {
			regionName = null;
		}
		return regionName;
	}

	@Schema(title = "Account 정보", example = "null", accessMode = Schema.AccessMode.READ_ONLY)
	private AccountVO account;


	@Schema(title = "클러스터 볼륨 플러그인 지원 목록")
	public List<Map<String, Object>> getSupportVolumePlugIns(){

		String k8sMinorVer = ResourceUtil.getSemMinorVer(this.getK8sVersion());

		List<Map<String, Object>> supportVolumePlugIns = new ArrayList<>();

		if(this.providerAccount != null){
			ProviderVO provider = new ProviderVO();
			provider = provider.copyProperties(this.providerAccount);
			EnumSet<VolumePlugIn> supportVolumePlugInSet = EnumSet.noneOf(VolumePlugIn.class);
			List<CubeType> cubeTypes = provider.getSupportCubeTypes().stream().map(c -> (c.getCubeType())).collect(Collectors.toList());
			K8sApiVerType k8sVersion = K8sApiVerType.getApiVerType(k8sMinorVer);
			/**
			 * StorageType.STATEFUL 은 Dynamic(provisioning)되는 Plugin만 지원함.
			 * 그래서 NFSSTATIC은 미지원함
			 */
			if (VolumePlugIn.NFSDYNAMIC.getSupportApiVer().contains(k8sVersion)) {
				supportVolumePlugInSet.add(VolumePlugIn.NFSDYNAMIC);
			}
			if (VolumePlugIn.NFSSTATIC.getSupportApiVer().contains(k8sVersion)) {
				supportVolumePlugInSet.add(VolumePlugIn.NFSSTATIC);
			}
			if (VolumePlugIn.NFS_CSI.getSupportApiVer().contains(k8sVersion)) {
				supportVolumePlugInSet.add(VolumePlugIn.NFS_CSI);
			}
			if(ProviderCode.AWS == provider.getProviderCode()){
				if(cubeTypes.contains(this.cubeType)
						&& (CubeType.EKS == this.cubeType || CubeType.PROVIDER == this.cubeType)){
					if (VolumePlugIn.AWSEBS.getSupportApiVer().contains(k8sVersion)) {
						supportVolumePlugInSet.add(VolumePlugIn.AWSEBS);
					}
					if (VolumePlugIn.AWSEFS.getSupportApiVer().contains(k8sVersion)) {
						supportVolumePlugInSet.add(VolumePlugIn.AWSEFS);
					}
					if (VolumePlugIn.AWSEBS_CSI.getSupportApiVer().contains(k8sVersion)) {
						supportVolumePlugInSet.add(VolumePlugIn.AWSEBS_CSI);
					}
					if (VolumePlugIn.AWSEFS_CSI.getSupportApiVer().contains(k8sVersion)) {
						supportVolumePlugInSet.add(VolumePlugIn.AWSEFS_CSI);
					}

				}
			}else if(ProviderCode.GCP == provider.getProviderCode()){
				if(cubeTypes.contains(this.cubeType)
						&& (CubeType.GKE == this.cubeType || CubeType.PROVIDER == this.cubeType)){
					if (VolumePlugIn.GCE.getSupportApiVer().contains(k8sVersion)) {
						supportVolumePlugInSet.add(VolumePlugIn.GCE);
					}
					if (VolumePlugIn.GCE_CSI.getSupportApiVer().contains(k8sVersion)) {
						supportVolumePlugInSet.add(VolumePlugIn.GCE_CSI);
					}
				}
			}else if(ProviderCode.AZR == provider.getProviderCode()){
				if(cubeTypes.contains(this.cubeType)
						&& (CubeType.AKS == this.cubeType || CubeType.PROVIDER == this.cubeType)){
					if (VolumePlugIn.AZUREDISK.getSupportApiVer().contains(k8sVersion)) {
						supportVolumePlugInSet.add(VolumePlugIn.AZUREDISK);
					}
					if (VolumePlugIn.AZUREFILE.getSupportApiVer().contains(k8sVersion)) {
						supportVolumePlugInSet.add(VolumePlugIn.AZUREFILE);
					}
					if (VolumePlugIn.AZUREDISK_CSI.getSupportApiVer().contains(k8sVersion)) {
						supportVolumePlugInSet.add(VolumePlugIn.AZUREDISK_CSI);
					}
					if (VolumePlugIn.AZUREFILE_CSI.getSupportApiVer().contains(k8sVersion)) {
						supportVolumePlugInSet.add(VolumePlugIn.AZUREFILE_CSI);
					}
				}
			}else if(ProviderCode.VMW == provider.getProviderCode()){
				if (VolumePlugIn.VSPHEREVOLUME.getSupportApiVer().contains(k8sVersion)) {
					supportVolumePlugInSet.add(VolumePlugIn.VSPHEREVOLUME);
				}
				if (VolumePlugIn.VSPHEREVOLUME_CSI.getSupportApiVer().contains(k8sVersion)) {
					supportVolumePlugInSet.add(VolumePlugIn.VSPHEREVOLUME_CSI);
				}
			}else if(ProviderCode.NCP == provider.getProviderCode()){
				if(cubeTypes.contains(this.cubeType)
						&& (CubeType.NCPKS == this.cubeType || CubeType.PROVIDER == this.cubeType)) {
					if (VolumePlugIn.NCPBLOCK_CSI.getSupportApiVer().contains(k8sVersion)) {
						supportVolumePlugInSet.add(VolumePlugIn.NCPBLOCK_CSI);
					}
					if (VolumePlugIn.NCPNAS_CSI.getSupportApiVer().contains(k8sVersion)) {
						supportVolumePlugInSet.add(VolumePlugIn.NCPNAS_CSI);
					}
				}
			}

			/**
			 * code 테이블 대체 처리
			 */
			List<CodeVO> supportVolumePlugInCodes = VolumePlugIn.getVolumePlugInCodeList();

			if(CollectionUtils.isNotEmpty(supportVolumePlugInCodes)){
				supportVolumePlugIns = supportVolumePlugInCodes.stream().filter(c -> (supportVolumePlugInSet.contains(VolumePlugIn.valueOf(c.getCode()))))
						.map(c -> {
							Map<String, Object> objectMap = new HashMap<>();
							objectMap.put("plugin", c);

							List<Map<String, Object>> validParams = new ArrayList<>();
							Optional<VolumePlugIn> vp = supportVolumePlugInSet.stream().filter(s -> (s == VolumePlugIn.valueOf(c.getCode()))).findFirst();
							if(vp.isPresent()){
								if(CollectionUtils.isNotEmpty(vp.get().getVolumePlugInParamsToList())){
									for(Map<String, Object> vRow : vp.get().getVolumePlugInParamsToList()){
										if(CollectionUtils.isNotEmpty((List)vRow.get("supportApiVer"))){
											if(((List<String>)vRow.get("supportApiVer")).contains(k8sMinorVer)){
												validParams.add(vRow);
											}
										}
									}
								}
								objectMap.put("storageType", vp.get().getStorageType());
								objectMap.put("bindingMode", vp.get().getVolumeBindingModeToList());
								objectMap.put("addParamEnabled", vp.get().isAddParamEnabled());
								objectMap.put("addMountOptionEnabled", vp.get().isAddMountOptionEnabled());
							}
							objectMap.put("params", validParams);


							return objectMap;

						}).collect(Collectors.toList());
			}
		}

		return supportVolumePlugIns;
	}


	// 내부용
	@JsonIgnore
	private String namespaceName;

}
