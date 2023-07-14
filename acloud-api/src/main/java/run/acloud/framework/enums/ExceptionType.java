package run.acloud.framework.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

/**
 * Created by wschoi@acornsoft.io on 2017. 1. 11.
 *
 * @see <a href="https://docs.google.com/spreadsheets/d/15niihN9ef4H8vMFs0KIFd8kkhEHkMawoYTdDx1eCXBY/edit?usp=sharing">Cocktail System Exception Code</a>
 *
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ExceptionType {

    /**
     * ExceptionCategory.COMMON
     */
//    NotSpecified(ExceptionPolicy.POLICY_COMMON_001_000),
//    Unknown(ExceptionPolicy.POLICY_COMMON_002_000),
//    OtherException(ExceptionPolicy.POLICY_COMMON_003_000),
    InvalidParameter(ExceptionPolicy.POLICY_COMMON_004_000),
    InvalidParameter_DateFormat(ExceptionPolicy.POLICY_COMMON_004_001),
    InvalidParameter_Overflow(ExceptionPolicy.POLICY_COMMON_004_002),
    InvalidParameter_Empty(ExceptionPolicy.POLICY_COMMON_004_003),
//    NotAuthenticated(ExceptionPolicy.POLICY_COMMON_005_000),
    InvalidInputData(ExceptionPolicy.POLICY_COMMON_006_000),
    InvalidState(ExceptionPolicy.POLICY_COMMON_007_000),
    NotAuthorized(ExceptionPolicy.POLICY_COMMON_008_000),
    NotAuthorizedToRequest(ExceptionPolicy.POLICY_COMMON_009_000),
//    IncorrectConfig(ExceptionPolicy.POLICY_COMMON_010_000),
//    NotSupported(ExceptionPolicy.POLICY_COMMON_011_000),
//    RequestURLNotFound(ExceptionPolicy.POLICY_COMMON_012_000),
    ResourceNotFound(ExceptionPolicy.POLICY_COMMON_013_000),
//    NotIncludedObject(ExceptionPolicy.POLICY_COMMON_014_000),
//    CloudProviderNotMatch(ExceptionPolicy.POLICY_COMMON_015_000),
//    CloudRegionNotMatch(ExceptionPolicy.POLICY_COMMON_016_000),
//    InstanceNotSupplied(ExceptionPolicy.POLICY_COMMON_017_000),
//    JobFail(ExceptionPolicy.POLICY_COMMON_018_000),
    InternalError(ExceptionPolicy.POLICY_COMMON_019_000),
    ExternalApiFail(ExceptionPolicy.POLICY_COMMON_020_000),
    ExternalApiFail_Monitoring(ExceptionPolicy.POLICY_COMMON_020_001),
    ExternalApiFail_Metering(ExceptionPolicy.POLICY_COMMON_020_002),
    ExternalApiFail_ClusterApi(ExceptionPolicy.POLICY_COMMON_020_003),
    ExternalApiFail_PackageApi(ExceptionPolicy.POLICY_COMMON_020_004),
    ExternalApiFail_ADApi(ExceptionPolicy.POLICY_COMMON_020_005),
    ExternalApiFail_GatewayApi(ExceptionPolicy.POLICY_COMMON_020_006),
//    UserRoleMismatch(ExceptionPolicy.POLICY_COMMON_021_000),
//    DataAlreadyExist(ExceptionPolicy.POLICY_COMMON_022_000),
    HaveNothingToDO(ExceptionPolicy.POLICY_COMMON_023_000),
    ResourceAlreadyExists(ExceptionPolicy.POLICY_COMMON_024_000),
//    InsufficientResource(ExceptionPolicy.POLICY_COMMON_025_000),
    CommonFail(ExceptionPolicy.POLICY_COMMON_026_000),
    CommonCreateFail(ExceptionPolicy.POLICY_COMMON_027_000),
    CommonUpdateFail(ExceptionPolicy.POLICY_COMMON_028_000),
    CommonDeleteFail(ExceptionPolicy.POLICY_COMMON_029_000),
    CommonInquireFail(ExceptionPolicy.POLICY_COMMON_030_000),
    DatabaseProcessingFailed(ExceptionPolicy.POLICY_COMMON_031_000),
    DatabaseConnectionFailed(ExceptionPolicy.POLICY_COMMON_032_000),
    MessagingServerConnectionFailed(ExceptionPolicy.POLICY_COMMON_033_000),
    MessagingServerConnectionStatusInquireFail(ExceptionPolicy.POLICY_COMMON_034_000),
    MessagingServerConnectionCloseFailed(ExceptionPolicy.POLICY_COMMON_035_000),
    MessagePublishFailed(ExceptionPolicy.POLICY_COMMON_036_000),
    MessageRequestFailed(ExceptionPolicy.POLICY_COMMON_037_000),
    MessageResponseIsNull(ExceptionPolicy.POLICY_COMMON_038_000),
    InvalidYamlData(ExceptionPolicy.POLICY_COMMON_039_000),
    GpuNotSupported(ExceptionPolicy.POLICY_COMMON_040_000),
    SctpNotSupported(ExceptionPolicy.POLICY_COMMON_041_000),
    MultiNicNotSupported(ExceptionPolicy.POLICY_COMMON_042_000),
    SriovNotSupported(ExceptionPolicy.POLICY_COMMON_043_000),
    NotAuthorizedToResource(ExceptionPolicy.POLICY_COMMON_044_000),
    TTLAfterFinishedNotSupported(ExceptionPolicy.POLICY_COMMON_045_000),
    ExcelDownloadFail(ExceptionPolicy.POLICY_COMMON_046_000),
    InvalidTomlData(ExceptionPolicy.POLICY_COMMON_047_000),
    CommonNotFound(ExceptionPolicy.POLICY_COMMON_048_000),
    CommonNotSupported(ExceptionPolicy.POLICY_COMMON_049_000),
    FileNotFound(ExceptionPolicy.POLICY_COMMON_050_000),
    FileReadFailure(ExceptionPolicy.POLICY_COMMON_051_000),
    InvalidLicense(ExceptionPolicy.POLICY_COMMON_990_000),
    LicenseIsExpired(ExceptionPolicy.POLICY_COMMON_991_000),

    /**
     * ExceptionCategory.SYSTEM
     */
    K8sApiFail(ExceptionPolicy.POLICY_SYSTEM_001_000),
    K8sNotSupported(ExceptionPolicy.POLICY_SYSTEM_002_000),
    CryptoFail(ExceptionPolicy.POLICY_SYSTEM_003_000),
    CryptoFail_DecryptAES(ExceptionPolicy.POLICY_SYSTEM_003_001),
    CryptoFail_EncryptAES(ExceptionPolicy.POLICY_SYSTEM_003_002),
    CryptoFail_DecryptRSA(ExceptionPolicy.POLICY_SYSTEM_003_003),
    CryptoFail_EncryptRSA(ExceptionPolicy.POLICY_SYSTEM_003_004),
    CryptoFail_GetRSAPublicKeySpec(ExceptionPolicy.POLICY_SYSTEM_003_005),
    CryptoFail_EncryptSHA(ExceptionPolicy.POLICY_SYSTEM_003_006),
    CompressFail(ExceptionPolicy.POLICY_SYSTEM_004_000),
    CompressFail_Zip(ExceptionPolicy.POLICY_SYSTEM_004_001),
    CompressFail_Unzip(ExceptionPolicy.POLICY_SYSTEM_004_002),
    K8sApiWarning(ExceptionPolicy.POLICY_SYSTEM_005_000),
    AWSApiFail(ExceptionPolicy.POLICY_SYSTEM_007_000),
    AWSIAMApiFail(ExceptionPolicy.POLICY_SYSTEM_007_001),
    K8sClusterCannotEditSystemResource(ExceptionPolicy.POLICY_SYSTEM_008_000),
    K8sClusterCannotDeleteSystemResource(ExceptionPolicy.POLICY_SYSTEM_009_000),
    SendMailFail(ExceptionPolicy.POLICY_SYSTEM_010_000),
    K8sImageContentTrustFail(ExceptionPolicy.POLICY_SYSTEM_011_000),
//    K8sImageContentTrustFail(ExceptionPolicy.POLICY_SYSTEM_011_000),
    K8sApiStatus400(ExceptionPolicy.POLICY_SYSTEM_400_000),
    K8sApiStatus401(ExceptionPolicy.POLICY_SYSTEM_401_000),
    K8sApiStatus403(ExceptionPolicy.POLICY_SYSTEM_403_000),
    K8sApiStatus404(ExceptionPolicy.POLICY_SYSTEM_404_000),
    K8sApiStatus405(ExceptionPolicy.POLICY_SYSTEM_405_000),
    K8sApiStatus406(ExceptionPolicy.POLICY_SYSTEM_406_000),
    K8sApiStatus407(ExceptionPolicy.POLICY_SYSTEM_407_000),
    K8sApiStatus408(ExceptionPolicy.POLICY_SYSTEM_408_000),
    K8sApiStatus409(ExceptionPolicy.POLICY_SYSTEM_409_000),
    K8sApiStatus410(ExceptionPolicy.POLICY_SYSTEM_410_000),
    K8sApiStatus411(ExceptionPolicy.POLICY_SYSTEM_411_000),
    K8sApiStatus412(ExceptionPolicy.POLICY_SYSTEM_412_000),
    K8sApiStatus413(ExceptionPolicy.POLICY_SYSTEM_413_000),
    K8sApiStatus414(ExceptionPolicy.POLICY_SYSTEM_414_000),
    K8sApiStatus415(ExceptionPolicy.POLICY_SYSTEM_415_000),
    K8sApiStatus416(ExceptionPolicy.POLICY_SYSTEM_416_000),
    K8sApiStatus417(ExceptionPolicy.POLICY_SYSTEM_417_000),
    K8sApiStatus422(ExceptionPolicy.POLICY_SYSTEM_422_000),
    K8sApiStatus423(ExceptionPolicy.POLICY_SYSTEM_423_000),
    K8sApiStatus424(ExceptionPolicy.POLICY_SYSTEM_424_000),
    K8sApiStatus426(ExceptionPolicy.POLICY_SYSTEM_426_000),
    K8sApiStatus428(ExceptionPolicy.POLICY_SYSTEM_428_000),
    K8sApiStatus429(ExceptionPolicy.POLICY_SYSTEM_429_000),
    K8sApiStatus431(ExceptionPolicy.POLICY_SYSTEM_431_000),
    K8sApiStatus451(ExceptionPolicy.POLICY_SYSTEM_451_000),

    /**
     * ExceptionCategory.ACCOUNT
     */
    AccountNotFound(ExceptionPolicy.POLICY_ACCOUNT_001_000),
    AccountAlreadyExists(ExceptionPolicy.POLICY_ACCOUNT_002_000),
    AccountNameIsNull(ExceptionPolicy.POLICY_ACCOUNT_003_000),
    UsersExistsInAccount(ExceptionPolicy.POLICY_ACCOUNT_004_000),
    ProviderAccountExistsInAccount(ExceptionPolicy.POLICY_ACCOUNT_005_000),
    AccountHasExpired(ExceptionPolicy.POLICY_ACCOUNT_006_000),
    AccountHaveWorkspace(ExceptionPolicy.POLICY_ACCOUNT_007_000),
    CubeEngineAccountCanNotHaveRegistry(ExceptionPolicy.POLICY_ACCOUNT_008_000),
    AccountHaveClusters(ExceptionPolicy.POLICY_ACCOUNT_009_000),
    CouldNotDownGradeToSingleTenancy(ExceptionPolicy.POLICY_ACCOUNT_010_000),

    /**
     * ExceptionCategory.ACCOUNT_APPLICATION
     */
    AccountApplicationNotFound(ExceptionPolicy.POLICY_ACCOUNT_APPLICATION_001_000),
    AccountApplicationAlreadyExists(ExceptionPolicy.POLICY_ACCOUNT_APPLICATION_002_000),
    AccountApplicationCannotDeleteRegistAccount(ExceptionPolicy.POLICY_ACCOUNT_APPLICATION_003_000),

    /**
     * ExceptionCategory.USER
     */
    UserIdNotFound(ExceptionPolicy.POLICY_USER_001_000),
    UserPasswordIncorect(ExceptionPolicy.POLICY_USER_002_000),
    UserHasNotRole(ExceptionPolicy.POLICY_USER_003_000),
    UserAlreadyExists(ExceptionPolicy.POLICY_USER_004_000),
    InactivatedUser(ExceptionPolicy.POLICY_USER_005_000),
    UserRootAdminDontAction(ExceptionPolicy.POLICY_USER_006_000),
    UserRoleExceed(ExceptionPolicy.POLICY_USER_007_000),
    UserRoleInvalid(ExceptionPolicy.POLICY_USER_008_000),
    NoAuthorizedServices(ExceptionPolicy.POLICY_USER_009_000),
    UserHaveWorkspace(ExceptionPolicy.POLICY_USER_010_000),
    UserHaveAccount(ExceptionPolicy.POLICY_USER_011_000),
    AccountIdInvalid(ExceptionPolicy.POLICY_USER_012_000),
    CanNotDeleteMyself(ExceptionPolicy.POLICY_USER_013_000),
    UserPasswordInvalid(ExceptionPolicy.POLICY_USER_014_000),
    CanNotUpdateInactiveStateMyself(ExceptionPolicy.POLICY_USER_015_000),
    UserAuthenticationFailed(ExceptionPolicy.POLICY_USER_016_000),
    UserPasswordIncorect2(ExceptionPolicy.POLICY_USER_017_000),

    /**
     * ExceptionCategory.PROVIDER_ACCOUNT
     */
    ProviderCredentialFormatInvalid(ExceptionPolicy.POLICY_PROVIDER_ACCOUNT_001_000),
    ProviderCredentialEmpty(ExceptionPolicy.POLICY_PROVIDER_ACCOUNT_002_000),
    ProviderAccessKeyOrSecretKeyInvalid(ExceptionPolicy.POLICY_PROVIDER_ACCOUNT_003_000),
//    ProviderKeyNotMatch(ExceptionPolicy.POLICY_PROVIDER_ACCOUNT_004_000),
    ProviderNotFound(ExceptionPolicy.POLICY_PROVIDER_ACCOUNT_005_000),
    ProviderUsedByCluster(ExceptionPolicy.POLICY_PROVIDER_ACCOUNT_006_000),
    ProviderAuthenticationIsInvalid(ExceptionPolicy.POLICY_PROVIDER_ACCOUNT_007_000),
    ProviderAlreadyExists(ExceptionPolicy.POLICY_PROVIDER_ACCOUNT_008_000),
    IssuedUserAccountClusterExist(ExceptionPolicy.POLICY_PROVIDER_ACCOUNT_009_000),

    /**
     * ExceptionCategory.CLUSTER
     */
    ClusterAccountInvalid(ExceptionPolicy.POLICY_CLUSTER_001_000),
    ClusterApiUrlAlreadyExists(ExceptionPolicy.POLICY_CLUSTER_002_000),
    InvalidClusterCertification(ExceptionPolicy.POLICY_CLUSTER_003_000),
//    ClusterNotConnected(ExceptionPolicy.POLICY_CLUSTER_004_000),
    ClusterHasComponent(ExceptionPolicy.POLICY_CLUSTER_005_000),
    ClusterConditionInquireFail(ExceptionPolicy.POLICY_CLUSTER_006_000),
    ClusterAppmapConditionInquireFail(ExceptionPolicy.POLICY_CLUSTER_007_000),
    ClusterVolumeConditionInquireFail(ExceptionPolicy.POLICY_CLUSTER_008_000),
    ClusterIsNotRunning(ExceptionPolicy.POLICY_CLUSTER_009_000),
    ClusterAccessKeyAlreadyExists(ExceptionPolicy.POLICY_CLUSTER_010_000),
    ClusterAccessKeyNotFound(ExceptionPolicy.POLICY_CLUSTER_011_000),
    TooManyClusterAccessKey(ExceptionPolicy.POLICY_CLUSTER_012_000),
    DoesNotExistsTheOldAddonInfoForRollback(ExceptionPolicy.POLICY_CLUSTER_013_000),
    NodeUpdateFail(ExceptionPolicy.POLICY_CLUSTER_014_000),
    ClusterNotFound(ExceptionPolicy.POLICY_CLUSTER_015_000),
    NodeNotFound(ExceptionPolicy.POLICY_CLUSTER_016_000),
    EffectOfTaintIsNull(ExceptionPolicy.POLICY_CLUSTER_017_000),
    InvalidEffectOfTaint(ExceptionPolicy.POLICY_CLUSTER_018_000),
    KeyOfTaintIsNull(ExceptionPolicy.POLICY_CLUSTER_019_000),
    ClusterRegistrationFail(ExceptionPolicy.POLICY_CLUSTER_020_000),

    /**
     * ExceptionCategory.APPMAP_GROUP
     */
    AppmapGroupHasAppmap(ExceptionPolicy.POLICY_APPMAP_GROUP_001_000),
    AppmapGroupNameAlreadyExists(ExceptionPolicy.POLICY_APPMAP_GROUP_002_000),

    /**
     * ExceptionCategory.GROUP
     */
    GroupHasComponent(ExceptionPolicy.POLICY_GROUP_001_000),
    GroupNameAlreadyExists(ExceptionPolicy.POLICY_GROUP_002_000),

    /**
     * ExceptionCategory.SERVICE
     */
    ServiceRegisteredAtCluster(ExceptionPolicy.POLICY_SERVICE_001_000),
    AppmapUseCluster(ExceptionPolicy.POLICY_SERVICE_002_000),
    ExceededMaximumWorkspaceInAccount(ExceptionPolicy.POLICY_SERVICE_003_000),
    ServiceContainsBuild(ExceptionPolicy.POLICY_SERVICE_004_000),
    WorkloadUsingImageRegistryExists(ExceptionPolicy.POLICY_SERVICE_005_000),
    MustBeAtLeastOneRegistry(ExceptionPolicy.POLICY_SERVICE_006_000),
    OnlyOneRegistryCanBeCreated(ExceptionPolicy.POLICY_SERVICE_007_000),
    RegistryToShareDoesNotExistOnTheSystem(ExceptionPolicy.POLICY_SERVICE_008_000),
    TenancyCannotBeChangedToSoft(ExceptionPolicy.POLICY_SERVICE_009_000),
    TenancyCannotBeChangedToHard(ExceptionPolicy.POLICY_SERVICE_010_000),
    InvalidClusterTenancy(ExceptionPolicy.POLICY_SERVICE_011_000),
    InvalidSoftClusterTenants(ExceptionPolicy.POLICY_SERVICE_012_000),
    InvalidHardClusterTenants(ExceptionPolicy.POLICY_SERVICE_013_000),
    ServiceHasClusters(ExceptionPolicy.POLICY_SERVICE_014_000),
    ServiceHasAppmaps(ExceptionPolicy.POLICY_SERVICE_015_000),
    WorkspaceNameAlreadyExists(ExceptionPolicy.POLICY_SERVICE_016_000),


    /**
     * ExceptionCategory.BUILD
     */
    BuilderServiceCallFail(ExceptionPolicy.POLICY_BUILD_001_000),
    BuilderJobDeletionFail(ExceptionPolicy.POLICY_BUILD_002_000),
    IsExistsRegistryImageName(ExceptionPolicy.POLICY_BUILD_003_000),
    IsExistsImageNameTag(ExceptionPolicy.POLICY_BUILD_004_000),
    BuildServerNotConnected(ExceptionPolicy.POLICY_BUILD_005_000),
    BuildInquireFail(ExceptionPolicy.POLICY_BUILD_006_000),
    BuildCreateFail(ExceptionPolicy.POLICY_BUILD_007_000),
    BuildUpdateFail(ExceptionPolicy.POLICY_BUILD_008_000),
    BuildDeleteFail_DB(ExceptionPolicy.POLICY_BUILD_009_001),
    BuildHistoryInquireFail(ExceptionPolicy.POLICY_BUILD_010_000),
    BuildAddCreateImageStepFail(ExceptionPolicy.POLICY_BUILD_011_000),
    ExceededMaximumBuildInAccount(ExceptionPolicy.POLICY_BUILD_012_000),
    ExceededMaximumParallelBuildInAccount(ExceptionPolicy.POLICY_BUILD_013_000),
    BuildHistoryOfRunningTaskDeleteFail(ExceptionPolicy.POLICY_BUILD_014_000), //실행중인 빌드의 히스토리는 삭제할 수 없습니다.
    DeleteFailBuildHistoryOfUsingInPipeline(ExceptionPolicy.POLICY_BUILD_015_000), //파이프라인에서 사용중인 빌드의 히스토리는 삭제할 수 없습니다.
    BuildExportFail(ExceptionPolicy.POLICY_BUILD_016_000),
    BuildImportFail(ExceptionPolicy.POLICY_BUILD_017_000),
    BuildImportFileInvalid(ExceptionPolicy.POLICY_BUILD_018_000),
    BuildImportFileInvalid_Extension(ExceptionPolicy.POLICY_BUILD_018_001),
    BuildImportFileInvalid_MimeType(ExceptionPolicy.POLICY_BUILD_018_002),
    BuildFileNotSupported(ExceptionPolicy.POLICY_BUILD_019_000),
    NotExistsRegistry(ExceptionPolicy.POLICY_BUILD_020_000),

    /**
     * ExceptionCategory.CLUSTER_VOLUME
     */
    ClusterVolumeNameInvalid(ExceptionPolicy.POLICY_CLUSTER_VOLUME_001_000),
    StorageClassNameInvalid(ExceptionPolicy.POLICY_CLUSTER_VOLUME_002_000),
    VolumePluginInvalid(ExceptionPolicy.POLICY_CLUSTER_VOLUME_003_000),
    ReclaimPolicyEmpty(ExceptionPolicy.POLICY_CLUSTER_VOLUME_004_000),
    ClusterSequenceEmpty(ExceptionPolicy.POLICY_CLUSTER_VOLUME_005_000),
    ClusterVolumeNotFound(ExceptionPolicy.POLICY_CLUSTER_VOLUME_006_000),
    ClusterVolumeTotalCapacitySmall(ExceptionPolicy.POLICY_CLUSTER_VOLUME_006_001),
    DynamicClusterVolumeAlreadyExists(ExceptionPolicy.POLICY_CLUSTER_VOLUME_007_000),
    InvalidClusterVolumeType(ExceptionPolicy.POLICY_CLUSTER_VOLUME_008_000),
    ClusterVolumeNotDeletableState(ExceptionPolicy.POLICY_CLUSTER_VOLUME_009_000),
    VolumeParameterNotExists(ExceptionPolicy.POLICY_CLUSTER_VOLUME_010_000),
    SharedClusterVolumeAlreadyExists(ExceptionPolicy.POLICY_CLUSTER_VOLUME_011_000),
    ClusterVolumeNotStatic(ExceptionPolicy.POLICY_CLUSTER_VOLUME_012_000),
    InvalidClusterStorageType(ExceptionPolicy.POLICY_CLUSTER_VOLUME_013_000),
    K8sStorageClassCreationFail(ExceptionPolicy.POLICY_CLUSTER_VOLUME_014_000),
    K8sStorageClassAlreadyExists(ExceptionPolicy.POLICY_CLUSTER_VOLUME_015_000),
    ProvisionerNameInvalid(ExceptionPolicy.POLICY_CLUSTER_VOLUME_016_000),
    ProvisionerIsUsed(ExceptionPolicy.POLICY_CLUSTER_VOLUME_017_000),
    ProvisionerNotFound(ExceptionPolicy.POLICY_CLUSTER_VOLUME_018_000),
    CanNotDeleteDefaultStorage(ExceptionPolicy.POLICY_CLUSTER_VOLUME_019_000),
    CanNotReleasedDefaultStorage(ExceptionPolicy.POLICY_CLUSTER_VOLUME_020_000),


    /**
     * ExceptionCategory.CONFIG_MAP
     */
    K8sConfigMapCreationFail(ExceptionPolicy.POLICY_CONFIG_MAP_001_000),
    K8sConfigMapNotFound(ExceptionPolicy.POLICY_CONFIG_MAP_002_000),
    K8sConfigMapNameInvalid(ExceptionPolicy.POLICY_CONFIG_MAP_003_000),
    K8sConfigMapKeyInvalid(ExceptionPolicy.POLICY_CONFIG_MAP_004_000),
    K8sConfigMapAlreadyExists(ExceptionPolicy.POLICY_CONFIG_MAP_005_000),
    K8sConfigMapDataInvalid(ExceptionPolicy.POLICY_CONFIG_MAP_006_000),
    ConfigMapUsed(ExceptionPolicy.POLICY_CONFIG_MAP_007_000),
    K8sConfigMapDescInvalid(ExceptionPolicy.POLICY_CONFIG_MAP_008_000),
    K8sConfigMapDescInvalid_MaxLengthLimit(ExceptionPolicy.POLICY_CONFIG_MAP_008_001),

    /**
     * ExceptionCategory.SECRET
     */
    K8sSecretCreationFail(ExceptionPolicy.POLICY_SECRET_001_000),
    K8sSecretNotFound(ExceptionPolicy.POLICY_SECRET_002_000),
    K8sSecretNameInvalid(ExceptionPolicy.POLICY_SECRET_003_000),
    SecretDataInvalid(ExceptionPolicy.POLICY_SECRET_004_000),
    SecretNameAlreadyExists(ExceptionPolicy.POLICY_SECRET_005_000),
    SecretUsed(ExceptionPolicy.POLICY_SECRET_006_000),
    K8sSecretDescInvalid(ExceptionPolicy.POLICY_SECRET_007_000),
    K8sSecretDescInvalid_MaxLengthLimit(ExceptionPolicy.POLICY_SECRET_007_001),

    /**
     * ExceptionCategory.NET_ATTACH_DEF
     */
    K8sNetAttachDefCreationFail(ExceptionPolicy.POLICY_NET_ATTACH_DEF_001_000),
    K8sNetAttachDefNotFound(ExceptionPolicy.POLICY_NET_ATTACH_DEF_002_000),
    K8sNetAttachDefNameInvalid(ExceptionPolicy.POLICY_NET_ATTACH_DEF_003_000),
    K8sNetAttachDefDataInvalid(ExceptionPolicy.POLICY_NET_ATTACH_DEF_004_000),
    K8sNetAttachDefAlreadyExists(ExceptionPolicy.POLICY_NET_ATTACH_DEF_005_000),
    K8sNetAttachDefUsed(ExceptionPolicy.POLICY_NET_ATTACH_DEF_006_000),

    /**
     * ExceptionCategory.SERVICE_SPEC
     */
    K8sServiceNameInvalid(ExceptionPolicy.POLICY_SERVICE_SPEC_001_000),
    ServiceNameAlreadyExists(ExceptionPolicy.POLICY_SERVICE_SPEC_002_000),
    K8sServiceNotFound(ExceptionPolicy.POLICY_SERVICE_SPEC_003_000),
    ReservedServiceName(ExceptionPolicy.POLICY_SERVICE_SPEC_004_000),
    ProtocolNotSupported(ExceptionPolicy.POLICY_SERVICE_SPEC_005_000),

    /**
     * ExceptionCategory.INGRESS_SPEC
     */
    K8sIngressNameInvalid(ExceptionPolicy.POLICY_INGRESS_SPEC_001_000),
    IngressNameAlreadyExists(ExceptionPolicy.POLICY_INGRESS_SPEC_002_000),
    K8sIngressNotFound(ExceptionPolicy.POLICY_INGRESS_SPEC_003_000),
    K8sIngressClassNotFound(ExceptionPolicy.POLICY_INGRESS_SPEC_004_000),

    /**
     * ExceptionCategory.CATALOG
     */
    TemplateSequenceEmpty(ExceptionPolicy.POLICY_CATALOG_001_000),
    AppmapSequenceEmpty(ExceptionPolicy.POLICY_CATALOG_002_000),
    TemplateNameAlreadyExists(ExceptionPolicy.POLICY_CATALOG_003_000),
    TemplateVersionAlreadyExists(ExceptionPolicy.POLICY_CATALOG_004_000),
    TemplateRegistrationFail(ExceptionPolicy.POLICY_CATALOG_005_000),
    TemplateDeletionFail(ExceptionPolicy.POLICY_CATALOG_006_000),
    TemplateUpdateFail(ExceptionPolicy.POLICY_CATALOG_007_000),
    TemplateDeploymentFail(ExceptionPolicy.POLICY_CATALOG_008_000),
    TemplateClusterNotExists(ExceptionPolicy.POLICY_CATALOG_009_000),
//    TemplateRegistryNotExists(ExceptionPolicy.POLICY_CATALOG_010_000),
    TemplateClusterVolumeNotExists(ExceptionPolicy.POLICY_CATALOG_011_000),
    TemplateNameInvalid(ExceptionPolicy.POLICY_CATALOG_012_000),
    AppmapNameInvalid(ExceptionPolicy.POLICY_CATALOG_013_000),
//    DoNotCatalogedStaticPV(ExceptionPolicy.POLICY_CATALOG_014_000),
    NamespaceNameInvalid(ExceptionPolicy.POLICY_CATALOG_015_000),
    TemplateExportFail(ExceptionPolicy.POLICY_CATALOG_016_000),
    TemplateImportFail(ExceptionPolicy.POLICY_CATALOG_017_000),
    TemplateImportFileInvalid(ExceptionPolicy.POLICY_CATALOG_018_000),
    TemplateImportFileInvalid_Extension(ExceptionPolicy.POLICY_CATALOG_018_001),
    TemplateImportFileInvalid_MimeType(ExceptionPolicy.POLICY_CATALOG_018_002),

    /**
     * ExceptionCategory.SERVER
     */
    JobTypeInvalid(ExceptionPolicy.POLICY_SERVER_001_000),
    TaskStateRunning(ExceptionPolicy.POLICY_SERVER_002_000),
    ServerInvalidState(ExceptionPolicy.POLICY_SERVER_003_000),
    ActionNotPermitted(ExceptionPolicy.POLICY_SERVER_004_000),
//    JobIsRunning(ExceptionPolicy.POLICY_SERVER_005_000),
    TaskNotFound(ExceptionPolicy.POLICY_SERVER_006_000),
    MemoryInsufficient(ExceptionPolicy.POLICY_SERVER_007_000),
    CpuInsufficient(ExceptionPolicy.POLICY_SERVER_008_000),
    PodcountExceeded(ExceptionPolicy.POLICY_SERVER_009_000),
    ServerCreationFailOnPreparation(ExceptionPolicy.POLICY_SERVER_010_000),
    ServerIsNotUpdatableState(ExceptionPolicy.POLICY_SERVER_011_000),
    ServerUpdateFailOnPreparation(ExceptionPolicy.POLICY_SERVER_012_000),
    ServerTerminationFailOnPreparation(ExceptionPolicy.POLICY_SERVER_013_000),
    ServerRecreationFailOnPreparation(ExceptionPolicy.POLICY_SERVER_014_000),
    ServerNameAlreadyExists(ExceptionPolicy.POLICY_SERVER_015_000),
    CubeLogCountInvalid(ExceptionPolicy.POLICY_SERVER_016_000),
    CubeResourceTypeUnknown(ExceptionPolicy.POLICY_SERVER_017_000),
    NotSupportedServerType(ExceptionPolicy.POLICY_SERVER_018_000),
    NotSupportedVolumePlugIn(ExceptionPolicy.POLICY_SERVER_019_000),
    K8sDeploymentCreationTimeout(ExceptionPolicy.POLICY_SERVER_020_000),
    K8sPodCreationTimeout(ExceptionPolicy.POLICY_SERVER_021_000),
    K8sDeploymentDeletionFail(ExceptionPolicy.POLICY_SERVER_022_000),
    K8sReplicasetDeletionFail(ExceptionPolicy.POLICY_SERVER_023_000),
    K8sServiceCreationTimeout(ExceptionPolicy.POLICY_SERVER_024_000),
    K8sPodNotFound(ExceptionPolicy.POLICY_SERVER_025_000),
    K8sStatusNotExists(ExceptionPolicy.POLICY_SERVER_026_000),
    K8sVolumeCreationFail(ExceptionPolicy.POLICY_SERVER_027_000),
    K8sVolumeUpdateFail(ExceptionPolicy.POLICY_SERVER_027_001),
    K8sVolumeNotFound(ExceptionPolicy.POLICY_SERVER_028_000),
    K8sVolumeClaimCreationFail(ExceptionPolicy.POLICY_SERVER_029_000),
    K8sVolumeClaimUpdateFail(ExceptionPolicy.POLICY_SERVER_029_001),
    K8sVolumeClaimCreationTimeout(ExceptionPolicy.POLICY_SERVER_030_000),
    K8sNamespaceNotFound(ExceptionPolicy.POLICY_SERVER_031_000),
    K8sDeploymentCreationFail(ExceptionPolicy.POLICY_SERVER_032_000),
    K8sServiceCreationFail(ExceptionPolicy.POLICY_SERVER_033_000),
    K8sIngressCreationFail(ExceptionPolicy.POLICY_SERVER_034_000),
    K8sHorizontalPodAutoscalerCreationFail(ExceptionPolicy.POLICY_SERVER_035_000),
    ServerVolumeAlreadyExists(ExceptionPolicy.POLICY_SERVER_036_000),
    ServerNotFound(ExceptionPolicy.POLICY_SERVER_037_000),
    ServerNotCubeType(ExceptionPolicy.POLICY_SERVER_038_000),
    ServerUpdateFail(ExceptionPolicy.POLICY_SERVER_039_000),
    K8sResourceCheckFail(ExceptionPolicy.POLICY_SERVER_040_000),
//    K8sVolumeNameInvalid(ExceptionPolicy.POLICY_SERVER_041_000),
    K8sDeploymentNotFound(ExceptionPolicy.POLICY_SERVER_042_000),
    ServerTypeImmutable(ExceptionPolicy.POLICY_SERVER_043_000),
    ServerHasNotContainer(ExceptionPolicy.POLICY_SERVER_044_000),
    HostPortDuplicated(ExceptionPolicy.POLICY_SERVER_045_000),
    IngressPathUsed(ExceptionPolicy.POLICY_SERVER_046_000),
    CubeLogTypeUnknown(ExceptionPolicy.POLICY_SERVER_047_000),
    ServerDeleteFailOnPreparation(ExceptionPolicy.POLICY_SERVER_048_000),
//    ServerDeleteFailOnTermination(ExceptionPolicy.POLICY_SERVER_049_000),
    NodePortOutOfRange(ExceptionPolicy.POLICY_SERVER_050_000),
    NodePortDuplicated(ExceptionPolicy.POLICY_SERVER_051_000),
//    K8sVolumeDeletionFail(ExceptionPolicy.POLICY_SERVER_052_000),
    K8SVolumeNotAvailable(ExceptionPolicy.POLICY_SERVER_053_000),
    ContainerNameAlreadyExists(ExceptionPolicy.POLICY_SERVER_054_000),
    NamespaceAlreadyExists(ExceptionPolicy.POLICY_SERVER_055_000),
    ServerStopInvalidState(ExceptionPolicy.POLICY_SERVER_056_000),
    ServerStartInvalidState(ExceptionPolicy.POLICY_SERVER_057_000),
    ServerRestartInvalidState(ExceptionPolicy.POLICY_SERVER_058_000),
    ServerRemoveInvalidState(ExceptionPolicy.POLICY_SERVER_059_000),
    K8sVolumeClaimIsUsingMount(ExceptionPolicy.POLICY_SERVER_060_000),
//    NotSupportedCluster(ExceptionPolicy.POLICY_SERVER_061_000),
    K8sCocktailCloudInquireFail(ExceptionPolicy.POLICY_SERVER_062_000),
    K8sCocktailCloudCreateFail(ExceptionPolicy.POLICY_SERVER_063_000),
    K8sCocktailCloudUpdateFail(ExceptionPolicy.POLICY_SERVER_064_000),
    K8sCocktailCloudDeleteFail(ExceptionPolicy.POLICY_SERVER_065_000),
    ServerPortInvalid(ExceptionPolicy.POLICY_SERVER_066_000),
    ServerPortRangeInvalid(ExceptionPolicy.POLICY_SERVER_067_000),
    ServerPortRangeFormatInvalid(ExceptionPolicy.POLICY_SERVER_068_000),
    ServerContainerNHostPortRangeNotSame(ExceptionPolicy.POLICY_SERVER_069_000),
    K8sClusterResourceLimitInquireFail(ExceptionPolicy.POLICY_SERVER_070_000),
    ServiceNotFound(ExceptionPolicy.POLICY_SERVER_071_000),
    ServerRunFail(ExceptionPolicy.POLICY_SERVER_072_000),
    ServerVolumeConfigInvalid(ExceptionPolicy.POLICY_SERVER_073_000),
    ServerVolumeConfigInvalid_VolumeNameIsNull(ExceptionPolicy.POLICY_SERVER_073_001),
    ServerVolumeConfigInvalid_VolumeNameIsNullInMount(ExceptionPolicy.POLICY_SERVER_073_002),
    ServerVolumeConfigInvalid_VolumeNameIsNotExistsInMount(ExceptionPolicy.POLICY_SERVER_073_003),
    ServerVolumeConfigInvalid_LinkedPersistentVolumeClaimNameIsNull(ExceptionPolicy.POLICY_SERVER_073_004),
    ServerPortRangeDoNotConfigAppointedNodePort(ExceptionPolicy.POLICY_SERVER_074_000),
    ServerAppointedNodePortIsNull(ExceptionPolicy.POLICY_SERVER_075_000),
    ServerRemoveFailOnPreparation(ExceptionPolicy.POLICY_SERVER_076_000),
    K8sVolumeAlreadyExists(ExceptionPolicy.POLICY_SERVER_077_000),
    K8sVolumeAlreadyExists_PVC(ExceptionPolicy.POLICY_SERVER_077_001),
    K8sVolumeAlreadyExists_PV(ExceptionPolicy.POLICY_SERVER_077_002),
    K8sVolumeNotExists_PVC(ExceptionPolicy.POLICY_SERVER_077_003),
    ServerStickySessionTimeoutEmpty(ExceptionPolicy.POLICY_SERVER_078_000),
    ServerStickySessionTimeoutOutOfRange(ExceptionPolicy.POLICY_SERVER_079_000),
    IngressPathTooLong(ExceptionPolicy.POLICY_SERVER_080_000),
    IngressHostTooLong(ExceptionPolicy.POLICY_SERVER_081_000),
    IngressHostUsed(ExceptionPolicy.POLICY_SERVER_082_000),
    K8sDaemonSetCreationFail(ExceptionPolicy.POLICY_SERVER_083_000),
    K8sDaemonSetDeletionFail(ExceptionPolicy.POLICY_SERVER_084_000),
    K8sJobCreationFail(ExceptionPolicy.POLICY_SERVER_085_000),
    K8sJobDeletionFail(ExceptionPolicy.POLICY_SERVER_086_000),
    K8sCronJobCreationFail(ExceptionPolicy.POLICY_SERVER_087_000),
    K8sCronJobDeletionFail(ExceptionPolicy.POLICY_SERVER_088_000),
    K8sStatefulSetCreationFail(ExceptionPolicy.POLICY_SERVER_089_000),
    K8sStatefulSetDeletionFail(ExceptionPolicy.POLICY_SERVER_090_000),
    ServerEditInvalidState(ExceptionPolicy.POLICY_SERVER_091_000),
    StatefulSetMustBeExistsHeadlessService(ExceptionPolicy.POLICY_SERVER_092_000),
    HorizontalPodAutoscalerNameIsEmpty(ExceptionPolicy.POLICY_SERVER_093_000),
    HorizontalPodAutoscalerNameMismatch(ExceptionPolicy.POLICY_SERVER_094_000),
    HorizontalPodAutoscalerNameAlreadyExists(ExceptionPolicy.POLICY_SERVER_095_000),

    /**
     * ExceptionCategory.PIPELINE
     */
    PipelineCreationFail(ExceptionPolicy.POLICY_PIPELINE_001_000),
    PipelineCreationFail_ParameterInvalid(ExceptionPolicy.POLICY_PIPELINE_001_001),
    PipelineUpdateFail(ExceptionPolicy.POLICY_PIPELINE_002_000),
    PipelineUpdateFail_ParameterInvalid(ExceptionPolicy.POLICY_PIPELINE_002_001),
    PipelineNotFound(ExceptionPolicy.POLICY_PIPELINE_003_000),
//    ServerNotRunning(ExceptionPolicy.POLICY_PIPELINE_004_000),
    PipelineRunning(ExceptionPolicy.POLICY_PIPELINE_005_000),
    PipelineSameBuildTaskRunFail(ExceptionPolicy.POLICY_PIPELINE_006_000),
    PipelineRunningFail(ExceptionPolicy.POLICY_PIPELINE_007_000),

    /**
     * ExceptionCategory.REGISTRY
     */
    RegistryImageListingFail(ExceptionPolicy.POLICY_REGISTRY_001_000),
    RegistryLoginFail(ExceptionPolicy.POLICY_REGISTRY_002_000),
    RegistryImageTagListingFail(ExceptionPolicy.POLICY_REGISTRY_003_000),
    RegistryAddUserFail(ExceptionPolicy.POLICY_REGISTRY_004_000),
    RegistryUserNotFound(ExceptionPolicy.POLICY_REGISTRY_005_000),
    RegistryUpdateUserPasswordFail(ExceptionPolicy.POLICY_REGISTRY_006_000),
//    RegistryUserDeletionFail(ExceptionPolicy.POLICY_REGISTRY_007_000),
    RegistryAddUserToProjectFail(ExceptionPolicy.POLICY_REGISTRY_008_000),
    RegistryDeleteUserToProjectFail(ExceptionPolicy.POLICY_REGISTRY_009_000),
    RegistryAddProjectFail(ExceptionPolicy.POLICY_REGISTRY_010_000),
    RegistryProjectAlreadyExists(ExceptionPolicy.POLICY_REGISTRY_011_000),
    RegistryDeleteProjectFail(ExceptionPolicy.POLICY_REGISTRY_012_000),
    RegistryProjectListingFail(ExceptionPolicy.POLICY_REGISTRY_013_000),
    RegistryProjectNotFound(ExceptionPolicy.POLICY_REGISTRY_014_000),
    RegistryApiFail(ExceptionPolicy.POLICY_REGISTRY_015_000),
    RegistryContainsBuild(ExceptionPolicy.POLICY_REGISTRY_016_000),

    // Account Registry 등록 & 수정
    RegistryConnectionFail(ExceptionPolicy.POLICY_REGISTRY_017_000),
    InvalidRegistryCertification(ExceptionPolicy.POLICY_REGISTRY_018_000),
    ExistsAlreadyRegistry(ExceptionPolicy.POLICY_REGISTRY_019_000),

    /**
     * ExceptionCategory.PACKAGE_SERVER
     */
    PackageEventInquireFail(ExceptionPolicy.POLICY_PACKAGE_001_000),
    PackageResourceInquireFail(ExceptionPolicy.POLICY_PACKAGE_002_000),
    PackageDetailInquireFail(ExceptionPolicy.POLICY_PACKAGE_003_000),
    StorageClassInquireFailForToml(ExceptionPolicy.POLICY_PACKAGE_004_000),
    PackageStateInquireFail(ExceptionPolicy.POLICY_PACKAGE_005_000),
    PackageControllerStateIsInvalid(ExceptionPolicy.POLICY_PACKAGE_006_000),
    PackageInstallFail(ExceptionPolicy.POLICY_PACKAGE_007_000),
    PackageUpgradeFail(ExceptionPolicy.POLICY_PACKAGE_008_000),
    PackageRollbackFail(ExceptionPolicy.POLICY_PACKAGE_009_000),
    PackageUninstallFail(ExceptionPolicy.POLICY_PACKAGE_010_000),
    PackageListInquireFail(ExceptionPolicy.POLICY_PACKAGE_011_000),
    PackageStatusInquireFail(ExceptionPolicy.POLICY_PACKAGE_012_000),
    PackageHistoryInquireFail(ExceptionPolicy.POLICY_PACKAGE_013_000),
    PackageNameAlreadyExists(ExceptionPolicy.POLICY_PACKAGE_014_000),
    ChartInquireFail(ExceptionPolicy.POLICY_PACKAGE_015_000),
    // Addon
    AddonNameAlreadyExists(ExceptionPolicy.POLICY_PACKAGE_016_000),
    AddonAlreadyInstalled(ExceptionPolicy.POLICY_PACKAGE_017_000),
    AddonConfigmapAlreadyExists(ExceptionPolicy.POLICY_PACKAGE_018_000),
    AddonCanNoLonngerBeInstalled(ExceptionPolicy.POLICY_PACKAGE_019_000),
    AddonReleaseNameMissing(ExceptionPolicy.POLICY_PACKAGE_020_000),
    AddonChartNameMissing(ExceptionPolicy.POLICY_PACKAGE_021_000),
    AddonChartVersionMissing(ExceptionPolicy.POLICY_PACKAGE_022_000),
    InvalidAddonConfigurationFile(ExceptionPolicy.POLICY_PACKAGE_023_000),
    InvalidAddonConfigurationValue(ExceptionPolicy.POLICY_PACKAGE_024_000),
    InvalidAddonName_FixedName(ExceptionPolicy.POLICY_PACKAGE_025_000),
    AddonIstioInitRequired(ExceptionPolicy.POLICY_PACKAGE_026_000),

    /**
     * ExceptionCategory.BILLING
     */
    BillingSequenceInvalid(ExceptionPolicy.POLICY_BILLING_001_000),
    BillingDataNotExist(ExceptionPolicy.POLICY_BILLING_002_000),

    /**
     * ExceptionCategory.CLUSTER_ROLE
     */
    K8sClusterRoleCreationFail(ExceptionPolicy.POLICY_CLUSTER_ROLE_001_000),
    K8sClusterRoleNotFound(ExceptionPolicy.POLICY_CLUSTER_ROLE_002_000),
    K8sClusterRoleNameInvalid(ExceptionPolicy.POLICY_CLUSTER_ROLE_003_000),
    ClusterRoleNameAlreadyExists(ExceptionPolicy.POLICY_CLUSTER_ROLE_004_000),
    CanNotDeleteClusterRoleWithBinding(ExceptionPolicy.POLICY_CLUSTER_ROLE_005_000),

    /**
     * ExceptionCategory.CLUSTER_ROLE_BINDING
     */
    K8sClusterRoleBindingCreationFail(ExceptionPolicy.POLICY_CLUSTER_ROLE_BINDING_001_000),
    K8sClusterRoleBindingNotFound(ExceptionPolicy.POLICY_CLUSTER_ROLE_BINDING_002_000),
    K8sClusterRoleBindingNameInvalid(ExceptionPolicy.POLICY_CLUSTER_ROLE_BINDING_003_000),
    ClusterRoleBindingNameAlreadyExists(ExceptionPolicy.POLICY_CLUSTER_ROLE_BINDING_004_000),

    /**
     * ExceptionCategory.ROLE
     */
    K8sRoleCreationFail(ExceptionPolicy.POLICY_ROLE_001_000),
    K8sRoleNotFound(ExceptionPolicy.POLICY_ROLE_002_000),
    K8sRoleNameInvalid(ExceptionPolicy.POLICY_ROLE_003_000),
    RoleNameAlreadyExists(ExceptionPolicy.POLICY_ROLE_004_000),
    CanNotDeleteRoleWithBinding(ExceptionPolicy.POLICY_ROLE_005_000),

    /**
     * ExceptionCategory.ROLE_BINDING
     */
    K8sRoleBindingCreationFail(ExceptionPolicy.POLICY_ROLE_BINDING_001_000),
    K8sRoleBindingNotFound(ExceptionPolicy.POLICY_ROLE_BINDING_002_000),
    K8sRoleBindingNameInvalid(ExceptionPolicy.POLICY_ROLE_BINDING_003_000),
    RoleBindingNameAlreadyExists(ExceptionPolicy.POLICY_ROLE_BINDING_004_000),

    /**
     * ExceptionCategory.SERVICE_ACCOUNT
     */
    K8sServiceAccountCreationFail(ExceptionPolicy.POLICY_SERVICE_ACCOUNT_001_000),
    K8sServiceAccountNotFound(ExceptionPolicy.POLICY_SERVICE_ACCOUNT_002_000),
    K8sServiceAccountNameInvalid(ExceptionPolicy.POLICY_SERVICE_ACCOUNT_003_000),
    ServiceAccountNameAlreadyExists(ExceptionPolicy.POLICY_SERVICE_ACCOUNT_004_000),

    /**
     * ExceptionCategory.CUSTOM_OBJECT
     */
    K8sCustomObjectCreationFail(ExceptionPolicy.POLICY_CUSTOM_OBJECT_001_000),
    K8sCustomObjectNotFound(ExceptionPolicy.POLICY_CUSTOM_OBJECT_002_000),
    K8sCustomObjectNameInvalid(ExceptionPolicy.POLICY_CUSTOM_OBJECT_003_000),
    CustomObjectNameAlreadyExists(ExceptionPolicy.POLICY_CUSTOM_OBJECT_004_000),
    K8sCustomObjectUpdateFail(ExceptionPolicy.POLICY_CUSTOM_OBJECT_005_000),
    K8sCustomObjectNameImmutable(ExceptionPolicy.POLICY_CUSTOM_OBJECT_006_000),

    /**
     * ExceptionCategory.POD_SECURITY_POLICY
     */
    K8sPspNameInvalid(ExceptionPolicy.POLICY_POD_SECURITY_POLICY_001_000),
    PspNameAlreadyExists(ExceptionPolicy.POLICY_POD_SECURITY_POLICY_002_000),
    K8sPspNotFound(ExceptionPolicy.POLICY_POD_SECURITY_POLICY_003_000),
    PspCanNotDeleteHasBindingResource(ExceptionPolicy.POLICY_POD_SECURITY_POLICY_004_000),

    /**
     * ExceptionCategory.LIMIT_RANGE
     */
    K8sLimitRangeCreationFail  (ExceptionPolicy.POLICY_LIMIT_RANGE_001_000),
    K8sLimitRangeNotFound      (ExceptionPolicy.POLICY_LIMIT_RANGE_002_000),
    K8sLimitRangeNameInvalid   (ExceptionPolicy.POLICY_LIMIT_RANGE_003_000),
    LimitRangeNameAlreadyExists(ExceptionPolicy.POLICY_LIMIT_RANGE_004_000),

    /**
     * ExceptionCategory.RESOURCE_QUOTA
     */
    K8sResourceQuotaCreationFail  (ExceptionPolicy.POLICY_RESOURCE_QUOTA_001_000),
    K8sResourceQuotaNotFound      (ExceptionPolicy.POLICY_RESOURCE_QUOTA_002_000),
    K8sResourceQuotaNameInvalid   (ExceptionPolicy.POLICY_RESOURCE_QUOTA_003_000),
    ResourceQuotaNameAlreadyExists(ExceptionPolicy.POLICY_RESOURCE_QUOTA_004_000),

    /**
     * ExceptionCategory.NETWORK_POLICY
     */
    K8sNetworkPolicyCreationFail  (ExceptionPolicy.POLICY_NETWORK_POLICY_001_000),
    K8sNetworkPolicyNotFound      (ExceptionPolicy.POLICY_NETWORK_POLICY_002_000),
    K8sNetworkPolicyNameInvalid   (ExceptionPolicy.POLICY_NETWORK_POLICY_003_000),
    NetworkPolicyNameAlreadyExists(ExceptionPolicy.POLICY_NETWORK_POLICY_004_000),

    /**
     * ExceptionCategory.ALERT
     */
    AlertRuleIdAlreadyExists        (ExceptionPolicy.POLICY_ALERT_001_000),

    /**
     * ExceptionCategory.EXTERNAL_REGISTRY
     */
    ExternalRegistryConnectionFail              (ExceptionPolicy.POLICY_EXTERNAL_REGISTRY_001_000),
    InvalidExternalRegistryCertification        (ExceptionPolicy.POLICY_EXTERNAL_REGISTRY_002_000),
    CannotDeleteExternalRegistryUsingWorkspace  (ExceptionPolicy.POLICY_EXTERNAL_REGISTRY_003_000),

    /**
     * "," 누락으로 인한 오류 방지용 Dummy Code
     */
    Dummy(ExceptionPolicy.POLICY_DUMMY_001_000)
    ;


    @Getter
    private ExceptionPolicy exceptionPolicy;

    ExceptionType(ExceptionPolicy exceptionPolicy) {
        this.exceptionPolicy = exceptionPolicy;
    }

    public String getErrorCode(){
        return this.exceptionPolicy.getErrorCode();
    }

    public String getCode() {
        return this.name();
    }

}
