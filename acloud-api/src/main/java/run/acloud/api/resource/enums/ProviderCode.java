package run.acloud.api.resource.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.configuration.vo.CubeTypeVO;
import run.acloud.api.resource.vo.ProviderVO;
import run.acloud.api.resource.vo.RegionVO;
import run.acloud.commons.enums.EnumCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Schema(name = "ProviderCode", description = "ProviderCode")
public enum ProviderCode implements EnumCode {
	@Schema(name = "AWS", description = "AWS")
	AWS(
			EnumSet.of(ProviderAccountType.USER, ProviderAccountType.METERING),
			EnumSet.of(ClusterType.CUBE),
			EnumSet.of(CubeType.EKS, CubeType.PROVIDER, CubeType.MANAGED),
			"Amazon Web Service",
			getRegionCodeByGroup("AWS_REGION"),
			"Y"
	),
	@Schema(name = "AZR", description = "AZR")
	AZR(
			EnumSet.of(ProviderAccountType.USER, ProviderAccountType.METERING),
			EnumSet.of(ClusterType.CUBE),
			EnumSet.of(CubeType.AKS, CubeType.PROVIDER, CubeType.MANAGED),
			"Microsoft Azure",
			getRegionCodeByGroup("AZR_REGION"),
			"Y"
	),
	@Schema(name = "GCP", description = "GCP")
	GCP(
			EnumSet.of(ProviderAccountType.USER, ProviderAccountType.METERING),
			EnumSet.of(ClusterType.CUBE),
			EnumSet.of(CubeType.GKE, CubeType.PROVIDER, CubeType.MANAGED),
			"Google Cloud Platform",
			getRegionCodeByGroup("GCP_REGION"),
			"Y"
	),
	@Schema(name = "NCP", description = "NCP")
	NCP(
			EnumSet.of(ProviderAccountType.USER),
			EnumSet.of(ClusterType.CUBE),
			EnumSet.of(CubeType.NCPKS, CubeType.PROVIDER, CubeType.MANAGED),
			"Naver Cloud Platform",
			getRegionCodeByGroup("NCP_REGION"),
			"Y"
	),
	@Schema(name = "VMW", description = "VMW")
	VMW(
			EnumSet.of(ProviderAccountType.USER),
			EnumSet.of(ClusterType.CUBE),
			EnumSet.of(CubeType.MANAGED),
			"VMware",
			null,
			"Y"
	),
	@Schema(name = "ABC", description = "ABC")
	ABC(
			EnumSet.of(ProviderAccountType.USER, ProviderAccountType.METERING),
			EnumSet.of(ClusterType.CUBE),
			EnumSet.of(CubeType.PROVIDER, CubeType.MANAGED),
			"Alibaba Cloud",
			getRegionCodeByGroup("ABC_REGION"),
			"Y"
	),
	@Schema(name = "TCC", description = "TCC")
	TCC(
			EnumSet.of(ProviderAccountType.USER, ProviderAccountType.METERING),
			EnumSet.of(ClusterType.CUBE),
			EnumSet.of(CubeType.TKE, CubeType.MANAGED),
			"Tencent Cloud",
			getRegionCodeByGroup("TCC_REGION"),
			"Y"
	),
	@Schema(name = "RVS", description = "RVS")
	RVS(
			EnumSet.of(ProviderAccountType.USER),
			EnumSet.of(ClusterType.CUBE),
			EnumSet.of(CubeType.MANAGED),
			"Rovius Cloud",
			null,
			"Y"
	),
	@Schema(name = "OPM", description = "OPM")
	OPM(
			EnumSet.of(ProviderAccountType.USER),
			EnumSet.of(ClusterType.CUBE),
			EnumSet.of(CubeType.OCP, CubeType.OKE, CubeType.MANAGED),
			"On-Premise",
			null,
			"Y"
	),
	@Schema(name = "IDC", description = "IDC")
	IDC(
			EnumSet.of(ProviderAccountType.USER),
			EnumSet.of(ClusterType.CUBE),
			EnumSet.of(CubeType.OCP, CubeType.OKE, CubeType.MANAGED),
			"Datacenter",
			null,
			"Y"
	)
	;

	private EnumSet<ProviderAccountType> supportProviderAccountTypes;
	
	private EnumSet<ClusterType> supportClusterTypes;

	private EnumSet<CubeType> supportCubeTypes;

	@Getter
	private String description;

	@Getter
	private EnumSet<ProviderRegionCode> regionCodes;

	@Getter
	private String useYn;

	ProviderCode(
			EnumSet<ProviderAccountType> supportProviderAccountTypes,
			EnumSet<ClusterType> supportClusterTypes,
			EnumSet<CubeType> supportCubeTypes,
			String description,
			EnumSet<ProviderRegionCode> regionCodes,
			String useYn) {
		this.supportProviderAccountTypes = supportProviderAccountTypes;
		this.supportClusterTypes = supportClusterTypes;
		this.supportCubeTypes = supportCubeTypes;
		this.description = description;
		this.regionCodes = regionCodes;
		this.useYn = useYn;
	}

	public boolean isBaremetal(){
		return EnumSet.of(IDC, OPM).contains(this);
	}
	public boolean isNotCloud(){
		return EnumSet.of(IDC, RVS, OPM, VMW).contains(this);
	}

	public boolean canMetering(){
		return EnumSet.of(AWS, GCP).contains(this);
	}

	public boolean canInternalLB(){
		return EnumSet.of(AWS, GCP, AZR, NCP).contains(this);
	}

	public EnumSet<ProviderAccountType> getSupportProviderAccountTypes() {
		return this.supportProviderAccountTypes;
	}
	
	public EnumSet<ClusterType> getSupportClusterTypes() {
		return this.supportClusterTypes;
	}
	
	public List<CubeTypeVO> getSupportCubeTypes() {
		if (this.supportCubeTypes == null) {
			return null;
		}
		
		List<CubeTypeVO> cubeTypes = new ArrayList<>();
		
		for (CubeType supportCubeType : supportCubeTypes) {
			CubeTypeVO cubeType = new CubeTypeVO();
			cubeType.setCubeType(supportCubeType);
			cubeType.setSupported(this); // cubeType에 따라 cluster 지원여부 셋팅
			cubeTypes.add(cubeType);
		}
		
		return cubeTypes;
	}

	public static EnumSet<ProviderRegionCode> getRegionCodeByGroup(String regionGroup) {
		return ProviderRegionCode.getEnumSet(regionGroup);
	}

	public static ProviderCode codeOf(String code) {
		return ProviderCode.valueOf(code);
	}

	public static List<CodeVO> getCodeList() {
		return Arrays.stream(ProviderCode.values()).filter(pc -> (BooleanUtils.toBoolean(pc.getUseYn()))).map(pc -> {
			CodeVO code = new CodeVO();
			code.setGroupId("PROVIDER");
			code.setCode(pc.getCode());
			code.setValue(pc.getDescription());
			code.setDescription(pc.getDescription());
			code.setUseYn(pc.getUseYn());
			return code;
		}).collect(Collectors.toList());
	}

	public static List<ProviderVO> getProviderList() {
		return Arrays.stream(ProviderCode.values()).filter(pc -> (BooleanUtils.toBoolean(pc.getUseYn()))).map(pc -> {
			ProviderVO provider = new ProviderVO();
			provider.setProviderCode(pc);
			provider.setProviderName(pc.getDescription());
			provider.setDescription(pc.getDescription());
			if (CollectionUtils.isNotEmpty(pc.getRegionCodes())) {
				provider.setRegions(pc.getRegionCodes().stream().filter(rc -> (BooleanUtils.toBoolean(rc.getUseYn()))).map(rc -> {
					RegionVO region = new RegionVO();
					region.setRegionCode(rc.getRegionCode());
					region.setRegionName(rc.getRegionName());
					region.setDescription(rc.getDescription());
					return region;
				}).collect(Collectors.toList()));
			}
			return provider;
		}).collect(Collectors.toList());
	}

	public static boolean exists(String code) {
		return Arrays.stream(ProviderCode.values()).filter(pc -> (StringUtils.isNotBlank(code) && StringUtils.equals(code, pc.getCode()))).findFirst().isPresent();
	}

	public String getCode() {
		return this.name();
	}
}
