# Acloud Java Project!

## DEV
* [환경 설정 및 실행](docs/DEV.md)

## Docker Registry
* [Harbor](docs/Harbor.md)
* [구축 방안](docs/Docker Registry 구축 방안.md)

## Kubernetes
* [Kubernetes cluster 구성](docs/Kubernetes%20Cluster%20구성%20방법.md)
* [Kubernetes check point](docs/Kubernetes Check Points.md)
* [Kubernetes API](docs/Kubernetes APIs.md)


## 개발 문서
* [개발 목록](docs/develop-list.md)
* [Exception Code](docs/design/Cocktail-System-Excetion-Code.md)

## 환경변수
* Deploy시 필요한 환경변수

| 변수명 | 유형 | 변수값 |  비고 |
|:-------|:-------|:-------|:-------|
| CMDB_DRIVER | COCKTAIL | org.mariadb.jdbc.Driver | - |
| CMDB_URL | COCKTAIL | jdbc:mariadb://cocktail-cmdb:3306/cocktail?useUnicode=true&charaterEncoding=utf-8&autoReconnect=true&zeroDateTimeBehavior=convertToNull | - |
| CMDB_USER | COCKTAIL | cocktail | - |
| CMDB_PASSWORD | COCKTAIL | ****** | - |
| K8S_RESOURCE_PREFIX | COCKTAIL | COCKTAIL-PROD | - |
| REGISTRY_URL | COMMON | https://regi.acornsoft.io | - |
| REGISTRY_USER | COMMON | admin | - |
| REGISTRY_PASSWORD | COMMON | ********* | - |
| ~~EVENT_TYPE~~ | COMMON | URL | - |
| CLIENT_HOST | COCKTAIL | http://cocktail-client-cluster-ip | - |
| CLIENT_CALLBACK_PATH | COCKTAIL | /callback/api/topic | - |
| BUILDER_HOST | COCKTAIL | http://builder-api:8080/builder | - |
| BUILD_USE_REGISTER | COCKTAIL | 1 | - |
| ~~BUILD_ADD_JOB_URL~~ | COCKTAIL | /job/BUILD | - |
| ~~BUILD_DELETE_JOB_URL~~ | COCKTAIL | /job/{job-id}/BUILD | - |
| ~~BUILD_GET_LATEST_BUILD~~_IMAGE_URL | COCKTAIL | /task/{task-id}/latest/images | - |
| ~~BUILD_RUN_BUILD_TASK_URL~~ | COCKTAIL | /task/{task-id}/BUILD | - |
| LOG_LEVEL | COMMON | INFO | - |
| APPMAP_LOG_LEVEL | COCKTAIL | INFO | 2.3.0 |
| MONITORING_HOST | COCKTAIL | http://cocktail-monitoring:9000/monitoring-api | 2.3.0 |
| GCP_BILLING_BUCKET | COCKTAIL | cc-billing-data | 2.1.3, 2.3.0 |
| GCP_BILLING_FILE_PREFIX | COCKTAIL | cc-billing- | 2.1.3, 2.3.0 |
| BUILD_DB_DRIVER | BUILDER | org.mariadb.jdbc.Driver (default value) | 2.3.1 |
| BUILD_DB_URL | BUILDER | jdbc:mariadb://cocktail-cmdb:3306/builder?useUnicode=true&charaterEncoding=utf-8&autoReconnect=true&zeroDateTimeBehavior=convertToNull&allowMultiQueries=true | 2.3.1 |
| BUILD_DB_USER | BUILDER | builder (default value) | 2.3.1 |
| BUILD_DB_PASSWORD | BUILDER | bu!1der (default value) | 2.3.1 |
| BUILDER_BUILD_API_URL | BUILDER | build-api | Build API Server host infomation 3.1.0 |
| BUILDER_BUILD_API_PORT | BUILDER | 8091 | Build API Server port infomation 3.1.0 |
| BUILD_QUEUE_URL | BUILDER | nats://build-queue:4222 (default value) | Build API Server port infomation 3.1.0 |
| BUILD_QUEUE_CID | BUILDER | build-queue-cluster | Build API Server port infomation 3.1.0 |
| NATS_USERNAME | BUILDER | 시크릿 사용 (build-queue-secret.NATS_USERNAME) | Build API Server port infomation 3.1.0 |
| NATS_PASSWORD | BUILDER | 시크릿 사용 (build-queue-secret.NATS_PASSWORD) | Build API Server port infomation 3.1.0 |
| DEFAULT_PARALLEL_BUILD_CNT | BUILDER | 2 (System별 동시빌드수) | Build API Server port infomation 3.1.0 |
| ~~SERVER_TYPE~~ | BUILDER | STG_2_3_1_001 | 2.3.1 |
| ~~DOCKER_URL~~ | BUILDER | https://regi.acornsost.io (default value) | 2.3.1 |
| ~~DOCKER_PORT~~ | BUILDER | 2376 (default value) | 2.3.1 |
| ~~BUILDER_IMAGE~~ | BUILDER | regi.acornsoft.io/cocktail-common/builder-base-image-master:2.3.0.B000006 | 2.3.1 |
| ~~CA_PEM~~ | BUILDER | BASE64 String of CA_PEM | 2.3.1 |
| ~~CERT_PEM~~ | BUILDER | BASE64 String of CERT_PEM | 2.3.1 |
| ~~KEY_PEM~~ | BUILDER | BASE64 String of KEY_PEM | 2.3.1 |
| TERMINAL_MAX_CONNECTION | COCKTAIL | Terminal max connection count | 2.3.1 |
| TERMINAL_CONNECTION_TIMEOUT | COCKTAIL | Terminal max connection timeout  | 2.3.1 |
