package run.acloud.api.resource.enums;

import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum ProviderRegionCode implements EnumCode {
	AWS_REGION_AP_NORTHEAST_1("AWS_REGION", "ap-northeast-1", "Tokyo", "Asia Pacific (Tokyo)", "Y"),
	AWS_REGION_AP_NORTHEAST_2("AWS_REGION", "ap-northeast-2", "Seoul", "Asia Pacific (Seoul)", "Y"),
	AWS_REGION_AP_NORTHEAST_3("AWS_REGION", "ap-northeast-3", "Osaka-Local", "Asia Pacific (Osaka-Local)", "Y"),
	AWS_REGION_AP_SOUTH_1    ("AWS_REGION", "ap-south-1", "Mumbai", "Asia Pacific (Mumbai)", "Y"),
	AWS_REGION_AP_SOUTHEAST_1("AWS_REGION", "ap-southeast-1", "Singapore", "Asia Pacific (Singapore)", "Y"),
	AWS_REGION_AP_SOUTHEAST_2("AWS_REGION", "ap-southeast-2", "Sydney", "Asia Pacific (Sydney)", "Y"),
	AWS_REGION_CA_CENTRAL_1  ("AWS_REGION", "ca-central-1", "Central", "Canada (Central)", "Y"),
	AWS_REGION_EU_CENTRAL_1  ("AWS_REGION", "eu-central-1", "Frankfurt", "EU (Frankfurt)", "Y"),
	AWS_REGION_EU_WEST_1     ("AWS_REGION", "eu-west-1", "Ireland", "EU (Ireland)", "Y"),
	AWS_REGION_EU_WEST_2     ("AWS_REGION", "eu-west-2", "London", "EU (London)", "Y"),
	AWS_REGION_EU_WEST_3     ("AWS_REGION", "eu-west-3", "Paris", "EU (Paris)", "Y"),
	AWS_REGION_SA_EAST_1     ("AWS_REGION", "sa-east-1", "São Paulo", "South America (São Paulo)", "Y"),
	AWS_REGION_US_EAST_1     ("AWS_REGION", "us-east-1", "N. Virginia", "US East (N. Virginia)", "Y"),
	AWS_REGION_US_EAST_2     ("AWS_REGION", "us-east-2", "Ohio", "US East (Ohio)", "Y"),
	AWS_REGION_US_WEST_1     ("AWS_REGION", "us-west-1", "N. California", "US West (N. California)", "Y"),
	AWS_REGION_US_WEST_2     ("AWS_REGION", "us-west-2", "Oregon", "US West (Oregon)", "Y"),

	AZR_REGION_AUSTRALIAEAST("AZR_REGION", "australiaeast", "New South Wales", "Australia East - region Asia", "Y"),
	AZR_REGION_AUSTRALIASOUTHEAST("AZR_REGION", "australiasoutheast", "Victoria", "Australia Southeast - region Asia", "Y"),
	AZR_REGION_BRAZILSOUTH("AZR_REGION", "brazilsouth", "Sao Paulo State", "Brazil South - region Americas", "Y"),
	AZR_REGION_CANADACENTRAL("AZR_REGION", "canadacentral", "Toronto", "Canada Central - region Americas", "Y"),
	AZR_REGION_CANADAEAST("AZR_REGION", "canadaeast", "Quebec City", "Canada East - region Americas", "Y"),
	AZR_REGION_CENTRALINDIA("AZR_REGION", "centralindia", "Pune", "Central India - region Asia", "Y"),
	AZR_REGION_CENTRALUS("AZR_REGION", "centralus", "Iowa", "Central US - region Americas", "Y"),
	AZR_REGION_CHINAEAST("AZR_REGION", "chinaeast", "Shanghai", "China East - region China", "Y"),
	AZR_REGION_CHINANORTH("AZR_REGION", "chinanorth", "Beijing", "China North - region China", "Y"),
	AZR_REGION_EASTASIA("AZR_REGION", "eastasia", "Hong Kong", "East Asia - region Asia", "Y"),
	AZR_REGION_EASTUS("AZR_REGION", "eastus", "Virginia", "East US - region Americas", "Y"),
	AZR_REGION_EASTUS2("AZR_REGION", "eastus2", "Virginia2", "East US 2 - region Americas", "Y"),
	AZR_REGION_GERMANYCENTRAL("AZR_REGION", "germanycentral", "Frankfurt", "Germany Central - region German", "Y"),
	AZR_REGION_GERMANYNORTHEAST("AZR_REGION", "germanynortheast", "Magdeburg", "Germany Northeast - region German", "Y"),
	AZR_REGION_JAPANEAST("AZR_REGION", "japaneast", "Tokyo, Saitama", "Japan East - region Asia", "Y"),
	AZR_REGION_JAPANWEST("AZR_REGION", "japanwest", "Osaka", "Japan West - region Asia", "Y"),
	AZR_REGION_KOREACENTRAL("AZR_REGION", "koreacentral", "Seoul", "Korea Central - region Asia", "Y"),
	AZR_REGION_KOREASOUTH("AZR_REGION", "koreasouth", "Busan", "Korea South - region Asia", "Y"),
	AZR_REGION_NORTHCENTRALUS("AZR_REGION", "northcentralus", "Illinois", "North Central US - region Americas", "Y"),
	AZR_REGION_NORTHEUROPE("AZR_REGION", "northeurope", "Ireland", "North Europe - region Europe", "Y"),
	AZR_REGION_SOUTHCENTRALUS("AZR_REGION", "southcentralus", "Texas", "South Central US - region Americas", "Y"),
	AZR_REGION_SOUTHEASTASIA("AZR_REGION", "southeastasia", "Singapore", "Southeast Asia - region Asia", "Y"),
	AZR_REGION_SOUTHINDIA("AZR_REGION", "southindia", "Chennai", "South India - region Asia", "Y"),
	AZR_REGION_UKSOUTH("AZR_REGION", "uksouth", "London", "UK South - region Europe", "Y"),
	AZR_REGION_UKWEST("AZR_REGION", "ukwest", "Cardiff", "UK West - region Europe", "Y"),
	AZR_REGION_WESTCENTRALUS("AZR_REGION", "westcentralus", "Wyoming", "West Central US - region Americas", "Y"),
	AZR_REGION_WESTEUROPE("AZR_REGION", "westeurope", "Netherlands", "West Europe - region Europe", "Y"),
	AZR_REGION_WESTINDIA("AZR_REGION", "westindia", "Mumbai", "West India - region Asia", "Y"),
	AZR_REGION_WESTUS("AZR_REGION", "westus", "California", "West US - region Americas", "Y"),
	AZR_REGION_WESTUS2("AZR_REGION", "westus2", "Washington", "West US 2 - region Americas", "Y"),

	GCP_REGION_ASIA_EAST1("GCP_REGION", "asia-east1", "Taiwan", "Changhua County, Taiwan - (asia-east1-a, asia-east1-b, asia-east1-c)", "Y"),
	GCP_REGION_ASIA_EAST2("GCP_REGION", "asia-east2", "Hong Kong", "Hong Kong - (asia-east1-a, asia-east1-b, asia-east1-c)", "Y"),
	GCP_REGION_ASIA_NORTHEAST1("GCP_REGION", "asia-northeast1", "Tokyo", "Tokyo, Japan - (asia-northeast1-a, asia-northeast1-b, asia-northeast1-c)", "Y"),
	GCP_REGION_ASIA_NORTHEAST2("GCP_REGION", "asia-northeast2", "Osaka", "Osaka, Japan - (asia-northeast2-a, asia-northeast2-b, asia-northeast2-c)", "Y"),
	GCP_REGION_ASIA_NORTHEAST3("GCP_REGION", "asia-northeast3", "Seoul", "Seoul, South Korea - (asia-northeast3-a, asia-northeast3-b, asia-northeast3-c)", "Y"),
	GCP_REGION_ASIA_SOUTH1("GCP_REGION", "asia-south1", "Mumbai", "Mumbai, India - (asia-south1-a, asia-south1-b, asia-south1-c)", "Y"),
	GCP_REGION_AsiaSoutheast1("GCP_REGION", "asia-southeast1", "Singapore", "Jurong West, Singapore - (asia-southeast1-a, asia-southeast1-b)", "Y"),
	GCP_REGION_AUSTRALIA_SOUTHEAST1("GCP_REGION", "australia-southeast1", "Sydney", "Sydney, Australia - (australia-southeast1-a, australia-southeast1-b, australia-southeast1-c)", "Y"),
	GCP_REGION_EUROPE_NORTH1("GCP_REGION", "europe-north1", "Hamina", "Hamina, Finland - (europe-north1-a, europe-north1-c, europe-north1-b)", "Y"),
	GCP_REGION_EUROPE_WEST1("GCP_REGION", "europe-west1", "Belgium", "St. Ghislain, Belgium - (europe-west1-b, europe-west1-c, europe-west1-d)", "Y"),
	GCP_REGION_EUROPE_WEST2("GCP_REGION", "europe-west2", "London", "London, U.K. - (europe-west2-a, europe-west2-b, europe-west2-c)", "Y"),
	GCP_REGION_EUROPE_WEST3("GCP_REGION", "europe-west3", "Frankfurt", "Frankfurt, Germany - (europe-west3-a, europe-west3-b, europe-west3-c)", "Y"),
	GCP_REGION_EUROPE_WEST4("GCP_REGION", "europe-west4", "Netherlands", "Eemshaven, Netherlands - (europe-west4-a, europe-west4-b, europe-west4-c)", "Y"),
	GCP_REGION_EUROPE_WEST6("GCP_REGION", "europe-west6", "Zürich", "Zürich, Switzerland - (europe-west6-a, europe-west6-b, europe-west6-c)", "Y"),
	GCP_REGION_NORTHAMERICA_NORTHEAST1("GCP_REGION", "northamerica-northeast1", "Montréal", "Montréal, Canada - (northamerica-northeast1-a, northamerica-northeast1-b, northamerica-northeast1-c)", "Y"),
	GCP_REGION_SOUTHAMERICA_EAST1("GCP_REGION", "southamerica-east1", "São Paulo", "São Paulo, Brazil - (southamerica-east1-a, southamerica-east1-b, southamerica-east1-c)", "Y"),
	GCP_REGION_US_CENTRAL1("GCP_REGION", "us-central1", "Iowa", "Council Bluffs, Iowa, USA - (us-central1-a, us-central1-b, us-central1-c, us-central1-f)", "Y"),
	GCP_REGION_US_EAST1("GCP_REGION", "us-east1", "South Carolina", "Moncks Corner, South Carolina, USA - (us-east1-b, us-east1-c, us-east1-d)", "Y"),
	GCP_REGION_US_EAST4("GCP_REGION", "us-east4", "Northern Virginia", "Ashburn, Virginia, USA - (us-east4-a, us-east4-b, us-east4-c)", "Y"),
	GCP_REGION_US_WEST1("GCP_REGION", "us-west1", "Oregon", "The Dalles, Oregon, USA - (us-west1-a, us-west1-b, us-west1-c)", "Y"),
	GCP_REGION_US_WEST2("GCP_REGION", "us-west2", "Los Angeles", "Los Angeles, California, USA - (us-west2-a, us-west2-b, us-west2-c)", "Y"),

	ABC_REGION_AP_NORTHEAST_1("ABC_REGION", "ap-northeast-1", "Tokyo", "Tokyo, Asia Pacific NE 1", "Y"),
	ABC_REGION_AP_SOUTH_1("ABC_REGION", "ap-south-1", "Mumbai", "Mumbai , Asia Pacific SOU 1", "Y"),
	ABC_REGION_AP_SOUTHEAST_1("ABC_REGION", "ap-southeast-1", "Singapore", "Singapore, Asia Pacific SE 1", "Y"),
	ABC_REGION_AP_SOUTHEAST_2("ABC_REGION", "ap-southeast-2", "Sydney", "Sydney, Asia Pacific SE 2", "Y"),
	ABC_REGION_AP_SOUTHEAST_3("ABC_REGION", "ap-southeast-3", "Kuala Lumpur", "Kuala Lumpur, Asia Pacific SE 3", "Y"),
	ABC_REGION_AP_SOUTHEAST_5("ABC_REGION", "ap-southeast-5", "Jakarta", "Jakarta, Asia Pacific SE 5", "Y"),
	ABC_REGION_CN_BEIJING("ABC_REGION", "cn-beijing", "Beijing", "Beijing, China North 2", "Y"),
	ABC_REGION_CN_HANGZHOU("ABC_REGION", "cn-hangzhou", "Hangzhou", "Hangzhou, China East 1", "Y"),
	ABC_REGION_CN_HONGKONG("ABC_REGION", "cn-hongkong", "Hong Kong", "Hong Kong, Hong Kong", "Y"),
	ABC_REGION_CN_HUHEHAOTE("ABC_REGION", "cn-huhehaote", "Hohhot", "Hohhot, China North 5", "Y"),
	ABC_REGION_CN_QINGDAO("ABC_REGION", "cn-qingdao", "Qingdao", "Qingdao, China North 1", "Y"),
	ABC_REGION_CN_SHANGHAI("ABC_REGION", "cn-shanghai", "Shanghai", "Shanghai, China East 2", "Y"),
	ABC_REGION_CN_SHENZHEN("ABC_REGION", "cn-shenzhen", "Shenzhen", "Shenzhen, China South 1", "Y"),
	ABC_REGION_CN_ZHANGJIAKOU("ABC_REGION", "cn-zhangjiakou", "Zhangjiakou", "Zhangjiakou, China North 3", "Y"),
	ABC_REGION_EU_CENTRAL_1("ABC_REGION", "eu-central-1", "Frankfurt", "Frankfurt, EU Central 1", "Y"),
	ABC_REGION_EU_WEST_1("ABC_REGION", "eu-west-1", "London", "London, UK (London)", "Y"),
	ABC_REGION_ME_EAST_1("ABC_REGION", "me-east-1", "Dubai", "Dubai, Middle East 1", "Y"),
	ABC_REGION_US_EAST_1("ABC_REGION", "us-east-1", "Virginia", "Virginia, US East 1", "Y"),
	ABC_REGION_US_WEST_1("ABC_REGION", "us-west-1", "Silicon Valley", "Silicon Valley, US West 1", "Y"),

	TCC_REGION_AP_BANGKOK("TCC_REGION", "ap-bangkok", "Bangkok", "Bangkok", "Y"),
	TCC_REGION_AP_BEIJING("TCC_REGION", "ap-beijing", "Beijing", "Beijing", "Y"),
	TCC_REGION_AP_BEIJING_1("TCC_REGION", "ap-beijing-1", "Beijiing Zone 1 (North China)", "Beijiing Zone 1 (North China)", "Y"),
	TCC_REGION_AP_CHENGDU("TCC_REGION", "ap-chengdu", "Chengdu (Southwest China)", "Chengdu (Southwest China)", "Y"),
	TCC_REGION_AP_GUANGZHOU("TCC_REGION", "ap-guangzhou", "Guangzhou (South China)", "Guangzhou (South China)", "Y"),
	TCC_REGION_AP_HONGKONG("TCC_REGION", "ap-hongkong", "Hong Kong", "Hong Kong", "Y"),
	TCC_REGION_AP_MUMBAI("TCC_REGION", "ap-mumbai", "Mumbai", "Mumbai", "Y"),
	TCC_REGION_AP_SEOUL("TCC_REGION", "ap-seoul", "Seoul", "Seoul", "Y"),
	TCC_REGION_AP_SHANGHAI("TCC_REGION", "ap-shanghai", "Shanghai (East China)", "Shanghai (East China)", "Y"),
	TCC_REGION_AP_SINGAPORE("TCC_REGION", "ap-singapore", "Singapore", "Singapore", "Y"),
	TCC_REGION_AP_TOKYO("TCC_REGION", "ap-tokyo", "Tokyo", "Tokyo", "Y"),
	TCC_REGION_EU_FRANKFURT("TCC_REGION", "eu-frankfurt", "Frankfurt", "Frankfurt", "Y"),
	TCC_REGION_EU_MOSCOW("TCC_REGION", "eu-moscow", "Moscow", "Moscow", "Y"),
	TCC_REGION_NA_ASHBURN("TCC_REGION", "na-ashburn", "Virginia", "Virginia", "Y"),
	TCC_REGION_NA_SILICONVALLEY("TCC_REGION", "na-siliconvalley", "Silicon Valley", "Silicon Valley", "Y"),
	TCC_REGION_NA_TORONTO("TCC_REGION", "na-toronto", "Toronto", "Toronto", "Y"),

	NCP_REGION_KR("NCP_REGION", "KR", "Korea", "Korea", "Y"),
	NCP_REGION_USWN("NCP_REGION", "USW", "US West", "US West", "Y"),
	NCP_REGION_HK("NCP_REGION", "HK", "Hong Kong", "Hong Kong", "Y"),
	NCP_REGION_SGN("NCP_REGION", "SGN", "Singapore", "Singapore", "Y"),
	NCP_REGION_JPN("NCP_REGION", "JPN", "Japan", "Japan", "Y"),
	NCP_REGION_DEN("NCP_REGION", "DEN", "Germany", "Germany", "Y")
	;

	public static class Names{
		public static final String AWS_REGION = "AWS_REGION";
		public static final String AZR_REGION = "AZR_REGION";
		public static final String GCP_REGION = "GCP_REGION";
		public static final String ABC_REGION = "ABC_REGION";
		public static final String TCC_REGION = "TCC_REGION";
		public static final String NCP_REGION = "NCP_REGION";
	}

	@Getter
	private String regionGroup;

	@Getter
	private String regionCode;

	@Getter
	private String regionName;

	@Getter
	private String description;

	@Getter
	private String useYn;

	ProviderRegionCode(
			String regionGroup,
			String regionCode,
			String regionName,
			String description,
			String useYn) {
		this.regionGroup = regionGroup;
		this.regionCode = regionCode;
		this.regionName = regionName;
		this.description = description;
		this.useYn = useYn;
	}

	public static ProviderRegionCode getRegionByCode(String regionGroup, String regionCode) {
		if (StringUtils.isNotBlank(regionGroup) && StringUtils.isNotBlank(regionCode)) {
			Optional<ProviderRegionCode> parcOptional = Arrays.stream(ProviderRegionCode.values()).filter(parc -> (StringUtils.equals(regionGroup, parc.regionGroup) && StringUtils.equalsIgnoreCase(regionCode, parc.regionCode))).findFirst();
			if (parcOptional.isPresent()) {
				return parcOptional.get();
			}
		}
		return null;
	}

	public static List<CodeVO> getCodeList(String regionGroup) {
		return Arrays.stream(ProviderRegionCode.values()).filter(parc -> (StringUtils.equals(regionGroup, parc.regionGroup) && BooleanUtils.toBoolean(parc.getUseYn()))).map(parc -> {
			CodeVO code = new CodeVO();
			code.setGroupId(parc.getRegionGroup());
			code.setCode(parc.getRegionCode());
			code.setValue(parc.getRegionName());
			code.setDescription(parc.getDescription());
			code.setUseYn(parc.getUseYn());
			return code;
		}).collect(Collectors.toList());
	}

	public static List<ProviderRegionCode> getList(String regionGroup) {
		return Arrays.stream(ProviderRegionCode.values()).filter(parc -> (StringUtils.equals(regionGroup, parc.regionGroup) && BooleanUtils.toBoolean(parc.getUseYn()))).collect(Collectors.toList());
	}

	public static EnumSet<ProviderRegionCode> getEnumSet(String regionGroup) {
		EnumSet<ProviderRegionCode> providerRegionCodeEnumSet = EnumSet.noneOf(ProviderRegionCode.class);
		if (StringUtils.isNotBlank(regionGroup)) {
			for (ProviderRegionCode prRow : ProviderRegionCode.values()) {
				if (StringUtils.equals(prRow.regionGroup, regionGroup)) {
					providerRegionCodeEnumSet.add(prRow);
				}
			}
		}

		return providerRegionCodeEnumSet;
	}

	public static boolean exists(String regionGroup, String regionCode) {
		return Arrays.stream(ProviderRegionCode.values()).filter(parc -> (StringUtils.equals(regionGroup, parc.regionGroup) && StringUtils.equalsIgnoreCase(regionCode, parc.regionCode))).findFirst().isPresent();
	}

	public String getCode() {
		return this.name();
	}
}
