# Cocktail System Exception Code

#### 100번 이하의 코드는 가급적 사용하지 않도록 한다.

| 구분 | 분류 | 코드 번호 | 코드 이름 | 오류 내용 | 비고 |
|:---------|:----|:-------:|:-------|:-------|:-------|
| Common | - | 0 | NotSpecified | 구체적 오류 번호를 밝힐 수 없음 | - |
| Common | - | 1 | Unknown | 오류를 알 수 없음 | - |
| Common | - | 2 | OtherException | 분류할 수 없는 오류 | - |
| Common | - | 4 | InvalidParameter | 잘못된 함수 호출 인자 | - |
| Common | - | 5 | NotAuthenticated | 인증되지 않은 요청 | - |
| Common | - | 6 | InvalidInputData | 외부(보통 사용자환경)에서 API를 호출하면서 전달한 정보가 올바르지 않음 | - |
| Common | - | 7 | InvalidState | 서버, 작업 등이 올바르지 않은 상태, 혹은 요청을 처리할 수 없는 상태 | - |
| Common | - | 8 | NotAuthorized | 요청에 대한 권한이 없음 | - |
| Common | - | 9 | DataNotFound | 필요한 정보를 찾을 수 없음 | - |
| Common | - | 10 | IncorrectConfig | 설정 정보가 잘못되어 있음 | - |
| Common | - | 11 | NotSupported | 지원하지 않는 기능 또는 요청 | - |
| Common | - | 12 | RequestURLNotFound | 작업을 위해 호출한 URL을 찾을 수 없음 | - |
| Common | - | 13 | ResourceNotFound | 요청한 자원을 찾을 수 없음 | - |
| Common | - | 14 | NotIncludedObject | 포함되지 않은 객체 | - |
| Common | - | 15 | CloudProviderNotMatch | 클라우드 프로바이더가 일치 하지 않음 | - |
| Common | - | 16 | CloudRegionNotMatch | 클라우드 레전이 일치 하지 않음 | - |
| Common | - | 17 | InstanceNotSupplied | 인스턴스가 공급되지 않음 | - |
| Common | - | 18 | JobFail | 작업 실패 | - |
| Common | - | 19 | InternalError | 시스템 내부 오류 | - |
| Common | - | 20 | ExternalApiFail | 외부 호출 API가 오류를 반환 또는 호출 실패 | - |
| Common | - | 21 | UserRoleMismatch | 사용자 role이 맞지 않음 | - |
| Common | - | 22 | DataAlreadyExist | 추가하려는 정보가 이미 존재하고 있음 | - |
| Common | - | 23 | HaveNothingToDO | 수행할 작업이 없음 | - |
| Common | - | 24 | ResourceAlreadyExists | 추가하려는 자원이 이미 등록되어 있음 | - |
| Common | - | 25 | InsufficientResource | 자원이 부족한 상태임 | - |
| Cocktail | 사용자 | 100 | UserIdNotFound | 사용자 Id가 등록되어 있지 않음 | - |
| Cocktail | 사용자 | 101 | UserPasswordIncorect | 사용자 암호가 맞지 않음 | - |
| Cocktail | 사용자 | 102 | UserHasNotRole | 로그인 시 지정한 사용자 role이 사용자에게 할당되어 있지 않음 | - |
| Cocktail | 사용자 | 103 | UserAlreadyExists | 추가하려는 사용자가 이미 등록되어 있음 | - |
| Cocktail | 사용자 | 104 | InactivatedUser | 사용 중지된 사용자 계정으로 접속 | - |
| Cocktail | 사용자 | 122 | UserRootAdminDontAction | Root Admin 사용자는 조작할 수 없음 | v2.6.0 - 2018.07.02 추가 |
| Cocktail | 프로바이더 | 105 | ProviderCredentialFormatInvalid | 프로바이더 credentail의 형식이 올바르지 않음 | - |
| Cocktail | 프로바이더 | 106 | ProviderCredentialExmpty | 프로바이더 credentail이 없음. | - |
| Cocktail | 프로바이더 | 107 | ProviderAccessKeyOrSecretKeyInvalid | 프로바이더 access key 또는 secret key가 올바르지 않음 | - |
| Cocktail | 프로바이더 | 108 | ProviderKeyNotMatch | 새로 입력하려는 프로바이더 키 값이 이전과 같지 않음 | - |
| Cocktail | 프로바이더 | 109 | ProviderNotFound | 프로바이더를 찾을 수 없음 | - |
| Cocktail | 프로바이더 | 119 | ProviderUsedByCluster | 해당 프로바이더를 사용하는 클러스터가 있음 | 2018.03.08 추가 |
| Cocktail | 클러스터 | 110 | ClusterAccountInvalid | 클러스터 접속 계정이 올바르지 않음 | - |
| Cocktail | 클러스터 | 120 | ClusterApiUrlAlreadyExists | 해당 마스터 URL로 등록된 클러스터가 이미 존재 | 2018.04.26 추가 |
| Cocktail | 클러스터 | 121 | InvalidClusterCertification | 클러스터 인증서가 올바르지 않음 | 2018.04.26 추가 |
| Cocktail | 클러스터 | 123 | ClusterNotConnected | 클러스터 접속이 원활하지 않음. | v2.6.0 - 2018.07.02 추가 |
| Cocktail | 클러스터, 앱맵 | 111 | ClusterHasComponent | 클러스터가 서버를 포함하고 있어 클러스터를 수정 또는 삭제할 수 없음 | - |
| Cocktail | 그룹 | 112 | GroupHasCompnent | 그룹이 서버를 포함하고 있어 클러스터를 삭제할 수 없음 | - |
| Cocktail | 그룹 | 118 | GroupNameAlreadyExists | 그룹명이 이미 존재함 | 2017.01.04 추가 |
| Cocktail | 서비스 | 113 | ServiceRegisteredAtCluster | 서비스가 클러스터에 등록되어 있음 | - |
| Cocktail | 서비스 | 114 | AppmapUseClster | 앱맵이 클러스터를 사용 중 | - |
| Cocktail | 빌드 | 116 | BuilderServiceCallFail | 빌드Job생성 오류 | 2017.01.04 추가 |
| Cocktail | 빌드 | 117 | BuilderJobDeletionFail | 빌드Job삭제 오류 | 2017.01.04 추가 |
| Cocktail | 클러스터 볼륨 | 200 | ClusterVolumeNameInvalid | 클러스터 볼륨의 이름이 없거나 올바르지 않음 | - |
| Cocktail | 클러스터 볼륨 | 201 | StorageClassNameInvalid | 클러스터 볼륨의 스토리지 클래스 이름이 없거나 올바르지 않음 | - |
| Cocktail | 클러스터 볼륨 | 202 | VolumePluginInvalid | 클러스터 볼륨의 플러그인 이름이 없음 | - |
| Cocktail | 클러스터 볼륨 | 203 | ReclaimPolicyEmpty | 클러스터 볼륨의 리크레임 정책 이름이 없음 | - |
| Cocktail | 클러스터 볼륨 | 204 | ClusterSequenceEmpty | 클러스터 볼륨의 클러스터 일련 번호가 없음 | - |
| Cocktail | 클러스터 볼륨 | 205 | ClusterVolumeNotFound | 클러스터 볼륨을 찾을 수 없음 | - |
| Cocktail | 클러스터 볼륨 | 214 | DynamicClusterVolumeAlredyExists | 이미 생성된 클러스터 볼륨의 스토리지 클래스가 있음 | 2018.03.08 추가 |
| Cocktail | 클러스터 볼륨 | 215 | InvalidClusterVolumeType | 클러스터 볼륨 유형을 찾을 수 없음 | 2018.03.08 추가 |
| Cocktail | 클러스터 볼륨 | 216 | ClusterVolumeNotDeletableState | 해당 클러스터 볼륨을 사용하고 있음 | 2018.03.08 추가 |
| Cocktail | 클러스터 볼륨 | 217 | VolumeParameterNotExists | 고정 클러스터 볼륨은 파라미터가 필요함 | 2018.03.08 추가 |
| Cocktail | 클러스터 볼륨 | 218 | SharedClusterVolumeAlredyExists | 이미 공유된 클러스터 볼륨이 존재함 | 2018.03.08 추가 |
| Cocktail | 클러스터 볼륨 | 219 | ClusterVolumeNotStatic | 해당 클러스터 볼륨은 고정 클러스터 볼륨이 아님 | 2018.03.08 추가 |
| Cocktail | 컨피그맵 | 206 | K8sConfigMapCreationFail | K8S ConfigMap 생성 실패 | - |
| Cocktail | 컨피그맵 | 207 | K8sConfigMapNotFound | 지정한 K8S ConfigMap을 찾을 수 없음 | - |
| Cocktail | 컨피그맵 | 210 | K8sConfigMapNameInvalid | 컨피그맵의 이름이 없거나 올바르지 않음 | - |
| Cocktail | 컨피그맵 | 211 | K8sConfigMapKeyInvalid | 컨피그맵의 키가 올바르지 않음 | - |
| Cocktail | 컨피그맵 | 212 | K8sConfigMapAlreadyExists | 추가하려는 컨피그맵의 이름이 이미 있음 | - |
| Cocktail | 컨피그맵 | 223 | K8sConfigMapDataInvalid | 컨피그맵 Data가 올바르지 않음 | v2.5.0 - 2018.05.31 추가 |
| Cocktail | 컨피그맵 | 224 | configMapUsed | 컨피그맵을 사용하는 서버가 존재함 | v2.5.0 - 2018.06.01 추가 |
| Cocktail | 시크릿 | 208 | K8sSecretCreationFail | K8S Secret 생성 실패 | - |
| Cocktail | 시크릿 | 209 | K8sSecretNotFound | 지정한 K8S Secret을 찾을 수 없음 | - |
| Cocktail | 시크릿 | 213 | K8sSecretNameInvalid | 시크릿의 이름이 없거나 올바르지 않음 | - |
| Cocktail | 시크릿 | 220 | SecretDataInvalid | 시크릿 Data가 올바르지 않음 | v2.5.0 - 2018.05.31 추가 |
| Cocktail | 시크릿 | 221 | SecretNameAlreadyExists | 추가하려는 시크릿의 이름이 존재함 | v2.5.0 - 2018.05.31 추가 |
| Cocktail | 시크릿 | 222 | SecretUsed | 시크릿을 사용하는 서버가 존재함 | v2.5.0 - 2018.05.31 추가 |
| Cocktail | 카탈로그 | 300 | TemplateSequenceEmpyt | 작업 대상 카탈로그의 일련 번호가 없음 | - |
| Cocktail | 카탈로그, 앱맵, 디플로이 | 301 | AppmapSequenceEmpty | 카탈로그를 적용할 기존 앱맵의 일련 번호가 없음 | - |
| Cocktail | 카탈로그 | 302 | TemplateNameAlreadyExists | 등록하려는 카탈로그의 이름이 이미 있음 | - |
| Cocktail | 카탈로그 | 303 | TemplateVersionAlreadyExists | 등록하려는 카탈로그의 버전이 이미 있음 | - |
| Cocktail | 카탈로그 | 304 | TemplateRegistrationFail | 카탈로그 등록 중 오류 발생 | - |
| Cocktail | 카탈로그 | 305 | TemplateDeletionFail | 카탈로그 삭제 중 오류 발생 | - |
| Cocktail | 카탈로그 | 306 | TemplateUpdateFail | 카탈로그 수정 중 오류 발생 | - |
| Cocktail | 카탈로그 | 307 | TemplateDeploymentFail | 카탈로그 배포 중 오류 발생 | - |
| Cocktail | 카탈로그 | 308 | TemplateClusterNotExists | 카탈로그가 사용하는 클러스터를 찾을 수 없음 | - |
| Cocktail | 카탈로그 | 309 | TemplateRegistryNotExists | 카탈로그가 사용하는 레지스트리를 찾을 수 없음 | - |
| Cocktail | 카탈로그 | 310 | TemplateClusterVolumeNotExists | 카탈로그가 사용하는 클러스터 볼륨을 찾을 수 없음 | - |
| Cocktail | 카탈로그 | 311 | TemplateNameInvalid | 등록할 카탈로그의 이름이 없거나 올바르지 않음 | - |
| Cocktail | 카탈로그, 앱맵 | 312 | AppmapNameInvalid | 카탈로그를 적용할 새 앱맵의 이름이 없거나 올바르지 않음 | - |
| Cocktail | 카탈로그 | 313 | DoNotCatalogedStaticPV | Static PersistentVolume을 사용하고 있으면 카탈로그를 등록할 수 없음 | 2018.01.31 추가 |
| Cocktail | 카탈로그 | 314 | NamespaceNameInvalid | 카탈로그를 적용할 새 네임스페이스의 이름이 없거나 올바르지 않음 | 2018.04.02 추가 |
| Cocktail | 레지스트리 | 400 | RegistryImageListingFail | 레지스트리에서 이미지 목록을 받을 수 없음 | - |
| Cocktail | 레지스트리 | 401 | RegistryLoginFail | 레지스트리에 로그인 할 수 없음 | - |
| Cocktail | 레지스트리 | 402 | RegistryImageTagListingFail | 레지스트리에서 이미지 태그 목록을 받을 수 없음 | - |
| Cocktail | 레지스트리 | 403 | RegistryAddUserFail | 레지스트리에 사용자를 등록할 수 없음 | - |
| Cocktail | 레지스트리 | 404 | RegistryUserNotFound | 레지스트리에 로그인하려는 사용자가 등록되어 있지 않음 | - |
| Cocktail | 레지스트리 | 405 | RegistryUpdateUserPasswordFail | 레지스트리 사용자의 암호를 수정할 수 없음 | - |
| Cocktail | 레지스트리 | 406 | RegistryUserDeletionFail | 레지스트리에서 등록된 사용자를 삭제할 수 없음 | - |
| Cocktail | 레지스트리 | 407 | RegistryAddUserToProjectFail | 레지스트리에 등록된 사용자를 프로젝트에 할당할 수 없음 | - |
| Cocktail | 레지스트리 | 408 | RegistryDeleteUserToProjectFail | 레지스트리에 등록된 사용자를 프로젝트에서 제외할 수 없음 | - |
| Cocktail | 레지스트리 | 409 | RegistryAddProjectFail | 레지스트리에 프로젝트를 등록할 수 없음 | v2.5.0 - 2018.05.31 추가 |
| Cocktail | 레지스트리 | 410 | RegistryProjectAlreadyExists | 레지스트리에 프로젝트가 이미 있음 | v2.5.0 - 2018.05.31 추가 |
| Cocktail | 레지스트리 | 411 | RegistryDeleteProjectFail | 레지스트리에 프로젝트를 삭제할 수 없음 | v2.5.0 - 2018.05.31 추가 |
| Cocktail | Job/Task | 500 | JobTypeInvalid | 실행하려는 job type이 올바르지 않음 | - |
| Cocktail | Job/Task | 501 | TaskStateRunning | task가 실행 중이라 새로운 task를 실행할 수 없음 | - |
| Cocktail | Job/Task | 502 | ServerInvalidState | 서버가 지정한 동작을 싫행할 수 없는 상태 | - |
| Cocktail | Job/Task | 503 | ActionNotPermitted | 현재 상태에서 실행할 수 없는 액션임 | 2017.01.04 추가 |
| Cocktail | Job/Task | 504 | JobIsRunning | 작업이 실행 중 | 2017.01.04 추가 |
| Cocktail | Job/Task | 505 | TaskNotFound | 실행할 업무(task)가 없음 | 2017.01.04 추가 |
| Cocktail | 서버 | 600 | MemoryInsufficient | 생성될 서버(들)가 요청한 메모리의 총합이 클러스터에 남은 메모리 양을 초과 | - |
| Cocktail | 서버 | 601 | CpuInsufficient | 생성될 서버(들)가 요청한 CPU의 총합이 클러스터에 남은 CPU 양을 초과 | - |
| Cocktail | 서버 | 602 | PodcountExceeded | 생성될 서버(들)가 요청한 Pod의 총합이 클러스터에 남은 생성 가능 Pod 수를 초과 | - |
| Cocktail | 서버 | 603 | ServerCreationFailOnPreparation | 서버 생성 준비 중 오류 발생 | - |
| Cocktail | 서버 | 604 | ServerIsNotUpdatableState | 서버를 수정할 수 없는 상태 | - |
| Cocktail | 서버 | 605 | ServerUpdateFailOnPreparation | 서버 수정 준비 중 오류 발생 | - |
| Cocktail | 서버 | 606 | ServerTerminationFailOnPreparation | 서버 종료 준비 중 오류 발생 | - |
| Cocktail | 서버 | 607 | ServerRecreationFailOnPreparation | 서버 재시작 준비 중 오류 발생 | - |
| Cocktail | 서버 | 608 | ServerNameAlreadyExists | 생성될 서버의 이름이 이미 사용 중 | - |
| Cocktail | 서버 | 609 | CubeLogCountInvalid | 조회하려는 로그의 갯수가 없음 | - |
| Cocktail | 서버 | 610 | CubeResourceTypeUnknown | 알 수 없는 리소스 타입 | - |
| Cocktail | 서버 | 611 | NotSupportedServerType | 지원하지 않는 서버 형식 | - |
| Cocktail | 서버 | 612 | NotSupportedVolumePlugIn | 지원하지 않는 볼륨 플러그인 | - |
| Cocktail | 서버 | 613 | K8sDeploymentCrerationTimeout | K8S Deployment 생성을 기다라던 중 timeout(대기 시간은 칵테일에서 설정) | - |
| Cocktail | 서버 | 614 | K8sPodCreationTimeout | K8S Pod 생성을 기다라던 중 timeout(대기 시간은 칵테일에서 설정) | - |
| Cocktail | 서버 | 615 | K8sDeploymentDeletionFail | K8S Deployment 삭제 실패 | - |
| Cocktail | 서버 | 616 | K8sReplicasetDeletionFail | K8S Replicatset 삭제 실패 | - |
| Cocktail | 서버 | 617 | K8sServiceCreationTimeout | K8S Service(or Load Balancer) 생성을 기다라던 중 timeout(대기 시간은 칵테일에서 설정) | - |
| Cocktail | 서버 | 618 | K8sPodNotFound | K8S Pod를 찾을 수 없음 | - |
| Cocktail | 서버 | 619 | K8sStatusNotExists | K8S Object 상태 조회 결과에 status가 없음 | - |
| Cocktail | 서버 | 620 | K8sVolumeCreationFail | K8S PersistentVolume 생성 실패 | - |
| Cocktail | 서버 | 621 | K8sVolumeNotFound | K8S PersistentVolume을 찾을 수 없음 | - |
| Cocktail | 서버 | 622 | K8sVolumeClaimCreationFail | K8S PersistentVolumeClaim 생성 실패 | - |
| Cocktail | 서버 | 623 | K8sVolumeClaimCreationTimeout | K8S PersistentVolumeClaim 생성 중 timeout(대기 시간은 칵테일에서 설정) | - |
| Cocktail | 서버 | 624 | K8sNamespaceNotFound | 지정한 K8S Namespace를 찾을 수 없음 | - |
| Cocktail | 서버 | 625 | K8sDeploymentCrerationFail | K8S Deployment 생성 실패 | - |
| Cocktail | 서버 | 626 | K8sServiceCreationFail | K8S Service 생성 실패 | - |
| Cocktail | 서버 | 627 | K8sIngressCreationFail | K8S Ingress 생성 실패 | - |
| Cocktail | 서버 | 628 | K8sHorizontalPodAutoscalerCreationFail | K8S HorizontalPodAutoscaler 생성 실패 | - |
| Cocktail | 서버 | 629 | K8sVolumeAlreadyExists | 생성하려는 이름을 가진 볼륨이 이미 있음 | - |
| Cocktail | 서버 | 630 | ServerNotFound | 지정한 서버를 찾을 수 없음 | - |
| Cocktail | 서버 | 631 | ServerNotCubeType | 서버가 Cube type이 아님 | - |
| Cocktail | 서버 | 632 | ServerUpdateFail | 서버 설정 수정에 실패 | - |
| Cocktail | 서버 | 633 | K8sResourceCheckFail | 서버 생성/수정 전 k8s 자원 검사에 실패 | - |
| Cocktail | 서버 | 634 | K8sVolumeNameInvalid | - | - |
| Cocktail | 서버 | 635 | K8sDeploymentNotFound | - | - |
| Cocktail | 서버 | 636 | ServerTypeImmutalble | 서버타입 변경불가 | 2017.01.04 추가 |
| Cocktail | 서버 | 637 | ServerHasNotContainer | 서버컨테이너 없음 | 2017.01.04 추가 |
| Cocktail | 서버 | 638 | HostPortDuplicated | 호스트 포트 중복 | 2017.01.04 추가 |
| Cocktail | 서버 | 639 | IngressPathUsed | 인그레스 경로가 이미 사용중임 | 2017.01.04 추가 |
| Cocktail | 서버 | 640 | CubeLogTypeUnknown | K8S의 로그 타입이 아님 | 2017.01.04 추가 |
| Cocktail | 서버 | 641 | ServerDeleteFailOnPreparation | 준비상태에서 서버 삭제 실패 | - |
| Cocktail | 서버 | 642 | ServerDeleteFailOnTermination | 중지상태에서 서버 삭제 실패 | - |
| Cocktail | 서버 | 643 | NodePortOutOfRange | 노드포트 지정 범위를 벗어남 | 2017.01.26 추가 |
| Cocktail | 서버 | 644 | NodePortDuplicated | 노드포트 중복 | 2017.01.26 추가 |
| Cocktail | 서버 | 645 | K8sVolumeDeletionFail | 볼륨 삭제 실패 | 2018.03.08 추가 |
| Cocktail | 서버 | 646 | K8SVolumeNotAvailable | 해당 볼륨이 사용가능한 상태가 아님 | 2018.03.08 추가 |
| Cocktail | 서버 | 647 | ContainerNameAlreadyExists | 컨테이너명 중복 | 2018.03.29 추가 |
| Cocktail | 서버 | 648 | NamespaceAlreadyExists | Namespace명 중복 | 2018.04.02 추가 |
| Cocktail | 서버 | 649 | ServerStopInvalidState | 서버 중지 가능한 상태가 아닙니다. | v2.3.2-hotfix5 - 2018.06.05 추가 | 
| Cocktail | 서버 | 650 | ServerStartInvalidState | 서버 시작 가능한 상태가 아닙니다. | v2.3.2-hotfix5 - 2018.06.05 추가 |
| Cocktail | 서버 | 651 | ServerRestartInvalidState | 서버 재시작 가능한 상태가 아닙니다. | v2.3.2-hotfix5 - 2018.06.05 추가 |
| Cocktail | 서버 | 652 | ServerRemoveInvalidState | 서버 삭제 가능한 상태가 아닙니다. | v2.3.2-hotfix5 - 2018.06.05 추가 |
| Cocktail | 서버 | 653 | K8sVolumeClaimIsUsingMount | 해당 PV가 Mount중 입니다. | v2.6.0 - 2018.07.23 추가 |
| Cocktail | 서버 | 654 | NotSupportedCluster | 지원하지 않는 클러스터 버전 | v2.6.0 - 2018.07.23 추가 |
| Cocktail | Pipeline | 1000 | ServerNotRunning | 서버가 실행 중인 상태가 아니어서 pipeline을 실행할 수 없음 | 2017.01.04 변경 |
| Cocktail | Pipeline | 1001 | PipelineRunning | Pipeline이 실행 중인 상태여서 다른 동작을 수행할 수 없음 | 2017.01.04 변경 |
| Cocktail | Pipeline | 1002 | PipelineNotFound | 지정한 Pipeline을 찾을 수 없음 | 2017.01.04 변경 |
| Cocktail | Pipeline | 1003 | PipelineCreationFail | Pipeline 생성 실패 | 2017.01.04 변경 |
| Cocktail | Pipeline | 1004 | PipelineUpdateFail | Pipeline 수정 실패 | 2017.01.04 변경 |
| Builder | Registry | 10025 | IsExistsRegistryImageName | Registry에 이미지명이 이미 존재함 | - |
| Builder | Registry | 10026 | IsExistsImageNameTag | 해당 이미지명이 이미 존재함 | - |
| Builder | Docker | 10100 | BuildServerNotConnected | 빌드 서버 접속이 원활하지 않음. | v2.5.4 - 2018.08.06 |
