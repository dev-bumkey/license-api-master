# Cocktail 연계를 위한 Kubernetes Check Points

아래 검토된 내용은 [Kubernetes Offical Site](https://kubernetes.io) 의 문서와 Google 정보를 통해서 검토한 내용들입니다.

최소한 다음의 사항들은 모두 읽어 보셔야 합니다.

- [Guides](https://kubernetes.io/docs/user-guide/): Kubernetes 기능뿐만 아니라 클러스터 관리에 대한 부분을 다룹니다.
- [Concepts](https://kubernetes.io/docs/concepts/): Kubernetes 동작 방식에 대한 이해를 다룹니다.
- [References](https://kubernetes.io/docs/reference/): kubectl 과 API 등에 대한 설명을 다룹니다.

## Restrictions

- Bare Metal 을 기반으로 한다.
- Ubuntu Cluster Infra 를 기반으로 한다.
- **Prerequisites**
  - **Step 1 : Infra 팀에서 Cluster를 구성**한다. 1 ~ N 개
  - **Step 2 : Application Containerize 수행**
  - **Step 3 : Cocktail 적용**
    - Cluster 생성할 때 Provider 를 Kubernetes 선택
    - Kubernetes Provider인 경우는 Step 1 에서 생성한 Cluster 수 만큼만 추가 가능
- **Cocktail Limits for Kubernetes**
  - VPC, Resion, Subnet 개념은 없다. 따라서 UI에서 이 부분이 반영되어야 한다.

## Kubernetes Features

- Cluster 구성
- Container 배포 (Auto-placement)
- Container 자동 복구 (Auto-restart)
- Container 추가/제거 (Scaling Up/Down)
- Container 복제 (Replication)
- Container 업데이트 (Rolling Update)
- Contaienr 롤백 (Rollback)
- Service Load Balancing (Round-robin)

## Cocktail Mapping Points

01/18일 미팅에 따라서 Cocktail 구성 요소들에 대한 매핑 가능한 요소들을 정리한 내용입니다.
아래의 표는 예상되는 매핑을 정리한 것으로 향후 추가적인 검증 과정을 거치면서 조정 또는 변경될 예정입니다.

| Items                    | Kubernetes Items                |
| ------------------------ | ------------------------------- |
| Service                  | **논리적 매핑**                      |
| Cluster                  | **Kubernates Cluster**          |
| Region                   | **대상 없음**                       |
| Subnets (Public/Private) | **대상 없음**                       |
| Server                   | **Kubernetes Service with Pod** |
| Instance                 | **Pods (Scaling 대상)**           |
| Application Container    | **Container in Pods**           |
| PaaS                     | 미정.                             |

각 항목들에 대한 검증과 가능성 여부 판단은 단 시일내에 결정되는 것은 아니기 때문에 아래와 같이 세부항목들을 검증하면서 결정될 필요가 있습니다.

----------

## Cocktail Core-UX 구현을 위한 Kubernetes 기능 및 API 정리 (없는 경우는 대체 가능성 정리)

> 하기의 정리 자료는 Mockup 0.83을 기준으로 화면의 Operation을 대상으로 정리한 내용입니다. 따라서 실제 구현할 때는 구체적인 Mapping 정보와 API 로 연계되는 Parameter의 정제 작업이 필요하며, 이를 통해서 Cocktail 구현체의 구성이 변경될 수 있습니다.

Core UX Items

- Configuration
  - Service 생성/수정
    - Cocktail 에서 처리

    - ==**Kubernetes Namspace**== 연계 가능성 검토 필요

      > ==**연계 대상 Depth 가 맞지 않으므로 의미 없음**==

    - APIs
      - Namespace
        - /api/v1/namespaces/{namespace}
        - GET/PUT/DELETE
  - Network 생성/수정
    - Region 정보 없음
    - IP Range (CIDR), Status, ...
    - Infra Team 에서 물리적으로 구현한 정보를 Cocktail 에서 Mapping
  - Subnet 생성/수정
    - Infra Team 에서 물리적으로 구현한 정보를 Cocktail 에서 Mapping

- Service

  - Create Cluster (**Map to Kubernetes Cluster**)

    - Infra Team 관리한 정보 Mapping
    - Cluster Map Tier 관리
      - Cocktail 에서 처리
    - Create Server (**Map to Kubernetes Servcie**)
      - ==**Server 생성을 위해 입력된 정보들을 Kubernetes 에서 인식할 수 있는 yml 포맷 데이터로 구성**==

      - ==**Multiple Container 정보를 1 Pod - Multi Container 정보로 구성**==

      - ==**CPU/MEMORY 제한은 Pod에 대해서 지정하고 Pod에 구성되는 Container들의 총합은 이를 넘지 못한다.**==

        > ==**Cocktail UI 에서 입력되는 CPU/MEMORY 를 Pod 에 지정하는 것으로 처리 함.**==

      - ==**Server Instance Type**==

      - APIs
        - Namespace
          - /api/v1/namespaces/{namespace}
          - GET/PUT/DELETE
        - Service
          - /api/v1/namespaces/{namespace}/services
          - GET/PUT/DELETE/PATCH
    - Check Server Status
      - ==Server Status 에 대한 정보가 어떤 항목에 어떤 정보로 연계될지 확인 필요==
      - APIs
        - Service
          - /api/v1/namespaces/{namespace}/services
          - GET/PUT/DELETE/PATCH
    - Server Monitoring - Top Page
      - ==**REST를 통해서 지원되지 않기 때문에 Heapster/InfluxDB를 활용하는 방법으로 별도 구성 필요, Cocktail에서 요구되는 정보를 모두 표현할 수 있는지는 검증이 필요함**==
    - Server Scale Up / Down
      - ==**Up / Down에 대한 지원은 문서상으로 확인되지 않음, 대체 방안으로 Rolling Update 검토 필요**==
      - APIs
        - Deployment
          - /apis/extensions/v1beta1/namespaces/{namespace}/deployments
          - GET/PUT/DELETE/PATCH
    - Server Scale In / Out
      - ==**HorizontalPodAutoScaler를 지원하지만 CPU 사용량 기준. 따라서 다른 기준이 필요한 경우라면 ReplicationController 또는 Deployment 를 통한 Rolling Update 처리 검토 필요**==
      - APIs
        - ReplicationController
          - /api/v1/namespaces/{namespace}/replicationcontrollers
          - GET/PUT/DELETE/PATCH
        - Deployment
          - /apis/extensions/v1beta1/namespaces/{namespace}/deployments
          - GET/PUT/DELETE/PATCH
    - Server Instance Scheduling
    - Application Rolling Update
      - ==**ReplicationController의 Replica 변경 또는 Deployment 정보 변경으로 대체 검토 필요**==
      - APIs
        - ReplicationController
          - /api/v1/namespaces/{namespace}/replicationcontrollers
          - GET/PUT/DELETE/PATCH
        - Deployment
          - /apis/extensions/v1beta1/namespaces/{namespace}/deployments
          - GET/PUT/DELETE/PATCH
    - Deploy, Backup Job 생성 및 실행
      - ==**Deploy, Backup 등의 실행은 Kubernetes의 Cron Job을 이용해서 지정한 시간/일자 등에 실행 가능할 것으로 예상 됨.**==
      - ==**Application Backup에 대한 내용은 Kubernetes에서는 공식적인 언급이 없는 것으로 보이며, Cocktail에서 구현되는 Backup 정책을 적용하는 부분으로 검토하거나 3rd Tool 을 Container 설정에 포함시켜서 처리해야할 것으로 판단 됨.**==
      - APIs
        - Job
          - /apis/batch/v1/namespaces/{namespace}/jobs
          - GET/PUT/DELETE/PATCH


## Kubernetes Check Points

### Kubernetes 운영을 위한 Cocktail 관리용 데이터 정리

> Kubernetes 의 Resource 이름은 다음과 같은 규칙을 가집니다.
>
> `regex [a-z0-9]([-a-z0-9]*[a-z0-9])? (e.g. 'my-name' or '123-abc')`
>
> 단, Service 의 경우는 앞에 숫자가 허용되지 않기 때문에 서비스를 기준으로 모든 리소스의 식별 정보를 구성합니다.
>
> `regex [a-z]([-a-z0-9]*[a-z0-9])? (e.g. 'my-name' or 'abc-123')`
> - 모두 소문자여야 합니다.
> - `' '` 및 `'_'` 는 허용되지 않습니다. (강제로 변환)
> - 특수문자 허용되지 않습니다. (a ~ z, 0 ~ 9)
>
> **`최종 Server의 이름은 ComponentVO 의 ComponentId 값을 기준으로 합니다.`**

**연계가 필요한 정보**

| Item                    | Info                       | Description                              |
| ----------------------- | -------------------------- | ---------------------------------------- |
| Cluster 연결정보            | https://52.79.186.103:6443 | Cluster 별 Master Server IP >> **ApiAccount ID** |
| Cluster 인증 정보           | Bearer Token               | Cluster 별 Master Bearer Token >> **ApiAccount Password** |
| Service Exposed Port 정보 | NodePort                   | Service가 생성된 후에 외부 접속을 위해서 공개되는 NodePort 정보 http://52.79.186.103:NodePort 접근 |

** 향후 연계를 위한 검토 대상**

> 2017.02.13일 회의 내용을 추가 적용한 결과입니다.

- Cocktail 식별정보 (OK - 적용)
  - 전체 Kubernetes 식별 및 운영을 위한 Namespace를 `cocktail-kubernetes` 로 일괄 적용합니다.
	  - `cocktail-kubernetest`: AWS 에 구성된 DEV Cocktail Kubernetes 클러스
		- `cocktail-kubernetest-aws`: AWS 에 구성된 STG Cocktail Kubernetes 클러스
		- `cocktail-kubernetest-azure`: Azure 에 구성된 STG Cocktail Kubernetes 클러스
		- `cocktail-kubernetest-datacenter`: AWS 에 구성된 DataCenter 용 STG Cocktail Kubernetes 클러스
		- `cocktail-kubernetest-google`: GCE 에 구성된 Cocktail Kubernetes 클러스
		- ...
  - 향후에도 이런 방식으로 적용을 할지 여부 결정 필요 (서버를 삭제하는 기준이라면 서버별로 1:1 namespace 를 생성하는 것은 어떨지?)
- Server Type 정보
  - 서비스 Type 선택을 어떻게 적용할 것인지?
    - ~~ClusterIP - Cluster 내부의 IP 대상으로 서비스 IP 배정 (외부 노출 없음)~~
    - NodePort - 외부 노출이 필요한 경우에 External Port 배정 (임의 지정 또는 자동 지정 가능)
    - ~~LoadBalancer - ClusterIP 와 NodePort 동시 적용 방식~~
    - ~~ExternalNames - External DNS 를 지정해서 적용~~

    > NodePort를 기본으로 사용한다.
    >
    > 현재 버전에는 내부적으로 NodePort 방식을 선택해서 적용 중.
    >
    > **참고: https://kubernetes.io/docs/user-guide/services/#publishing-services---service-types**

  - Container Env Vars (OK - 적용)
    - ~~Key/Value 구성이 아닌 Key/ValueFrom (File Path) 와 같은 특이 케이스는 제외하는 것인지? (Ex. mysql password file 등)~~

    > 기본 Command 만 그대로 Mapping 하는 방식 적용

  - Container Volumnes
    - ~~Volume Matching에 대한 추가 검토가 필요함.~~
    - Cocktail 은 Instance - Container로 연결되지만, Kubernetes 는 Node - Pod - Container 로 연결됨.
    - ~~Network Storage 의 경우는 어떻게 적용할 것인지? (Kubernetes 의 Persistent Volume)~~

    > Host Volume을 그대로 Container에서 사용할 수 있도록 Mapping 하는 것으로 처리

  - Service Monitoring
    - Server 에 해당하는 Service의 Monitoring 정보 (CPU, Memory, Volume) 를 찾을 수 있는가? 현재 문서 상으로 찾지 못함, Dashboard에서도 서비스는 보이지 않음.
  - Server Containers
    - ~~Server에 여러 개의 Container를 지정하는 것을 하나의 Pod로 묶는 것인가? ~~
      - ~~묶는다면 Service 로 구성하는 것은 Container 중에 하나를 선택해야 하는 경우가 발생한다. ~~
      - ~~아니라면 Container 각각을 Pod로 구성하는 것인가?~~

    > 우선적으로 단일 Container Image 만을 지정하는 것을 검토하고, 만일 여러 Image인 경우에도 단일 NodePort 를 서비스에서 특정 Container Image로 연결하는 방식이 가능하면 추가 검토.

  - Resources Naming
    - Kubernetes에서는 각 Resource에 대해서 식별을 위한 name 들을 설정한다. 이런 방식을 적용할 것인가? (미 정의시 Kubernetes에서 지정)
- Server Start / Stop
  ~~- 현재 Server Start / Stop 에 대한 처리 규정이 명확하게 정리될 필요가 있음. Server 가 `Service` 로 매치되고 내부의 Instance는 Pod 와 매치가 되는데 Pod의 Start / Stop 개념은 없어 보임. 단, 삭제는 가능하지만 Replica에 의해서 자동으로 다시 생성되므로 Start / Stop 과 같은 효과를 만들 수 있을지는 추가 검토가 필요해 보임. 단, 가능성은 부정적임.~~

  > Kubernetes의 Service 에서는 Start / Stop 개념은 적용하지 않는다.

---

### StatefulSet 개념 검토

**참고 문서**
- https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/
-

StatefulSet 은 Pod 식별을 위한 고유 ID 를 제공하는 컨트롤러로 배포화 확장의 순서를 보장하기 위한 것이다.

#### 적용이 필요한 어플리케이션

- 안정되고 유일한 네트워크 식별 (IP) 이 필요한 경우
- 안정되고 영구적인 저장소가 필요한 경우
- 배포와 확장에 순서가 필요한 경우
- 삭제와 종료에 순서가 필요한 경우

`안정적` 이라는 것은 Pod 스케줄링(재)에서 지속성을 의미하는 것이고, 순서에 대한 확실한 제공이 필요한 경우에 적용을 한다. 이와 대치되는 개념은 `Deployment` + `ReplicaSet` 으로 Stateless 컨트롤러라고 표현한다.

#### Limitations

- Beta 버전, 1.5 이전 버전 사용 불가 (이전 버전은 PetSets)
- Pod 에 배정되는 스토리지는 StorageClass 기반으로 PresistentVolume Provisioner 에 의해 생성된 볼륨이거나 관리자에 의해서 생성된 볼륨이어야 한다.
- StatefulSet 을 삭제 또는 확장한 경우에 StatefulSet 과 연계된 볼륨은 삭제되지 않는다. (데이터 보존 및 백업등의 기회??)
- StatefulSet 은 Headless Service로 구성되어야 한다.
- 1.5 버전에서 생성되어 있는 StatefulSet 의 갱신은 수동으로 처리해야 한다.
- 개념 검증을 위한 버전이지 데이터베이스 이중화(클러스터링) 등의 실제 운영을 위한 부분은 미진하다.

#### StatefulSet 기반의 어플리케이션 운영을 위한 Components

- Headless Service: 네트워크 도메인을 제어하기 위한 용도로 사용
- StatefulSet: 순서 기반의 유일하게 식별할 수 있는 Pod 를 구성하기 위한 Spec 제공
- volumeClaimTemplates: PersistentVolume Provisioner 에 의해 구성될 수 있는 PersistentVolume 템플릿 지정

구성 요소들은 아래와 같이 설정할 수 있다.

```yaml
---
apiVersion: v1
kind: Service
metadata:
  name: nginx
  labels:
    app: nginx
spec:
  ports:
  - port: 80
    name: web
  clusterIP: None
  selector:
    app: nginx
---
apiVersion: apps/v1beta1
kind: StatefulSet
metadata:
  name: web
spec:
  serviceName: "nginx"
  replicas: 3
  template:
    metadata:
      labels:
        app: nginx
    spec:
      terminationGracePeriodSeconds: 10
      containers:
      - name: nginx
        image: gcr.io/google_containers/nginx-slim:0.8
        ports:
        - containerPort: 80
          name: web
        volumeMounts:
        - name: www
          mountPath: /usr/share/nginx/html
  volumeClaimTemplates:
  - metadata:
      name: www
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 1Gi
```

#### Pod Identity

Pod 가 어떤 노드에 배치되는지와 상관없이 Pod 를 식별하기 위한 정보

- Ordinal index: [0, N) 범위 내의 정수 배정
- Stable Pod Network ID: `${statefulset name}-${ordinal index}` 포맷으로 호스트명 배정, 위의 예제 기준 "web-0", "web-1", "web-2"
- Domain: Headless Service 를 통해서 Pod Domain 관리가 되므로 `${pod name}.${service name}.${namespace}.${cluster}` 포맷이 된다. 따라서 위의 예제 기준으로 "web-0.nginx.default.svc.cluster.local" 형식이 된다.

#### Deployment and Scaling 보장

- N개의 Replica 를 가지는 StatefulSet 에서 Pod 들의 생성에 0..N-1 의 인덱스 배정
- Pod 삭제 시는 역순으로 N-1..0 로 종료 및 제거
- Pod Scaling 작업 전에 반드시 모든 Pod 들이 실행 중이거나 준비 상태여야 한다. 즉, 무 오류 상태
- Pod 종료되기 전에 후행 Pod 들은 모두 완전 종료 상태여야 한다. 즉, 무 오류 상태
- Pod에 적용되는 `spec.template.spec.terminationGracePeriodSeconds` 이 `0` 이어서는 안 된다. 강제 삭제 방식은 문제의 소지가 있다.

위의 샘플 기준으로 "web-0" 가 Running or Ready 상태가 아니면 Web-1 은 배포되지 않는다. 따라서 "web-2" 가 배포되는 시점에 "web-1" 은 Running or Ready 상태이더라도 "web-0" 가 Fail 상태면 배포되지 않는다.
또한 Replica=1 로 지정해서 Down Scaling 을 처리하면 "web-2" 부터 종료되고 "web-1" 은 "web-2" 가 완전 종료되어야 종료를 진행한다. 또한 "web-2" 가 완전 종료되었지만 "web-0" 가 Fail 상태면 "web-1" 은 "web-0" 가 Running or Ready 상태가 될 때까지 종료되지 않는다.

#### Example

Kubernetes 에서 제시하는 샘플은 MYSQL 어플리케이션이 비 동기 복제를 실행하는 Single Master 와 Many Slave 로 구성되어 있다.

- StatefulSet 컨트롤러로 복제된 MYSQL 배포
- MYSQL 트래픽 전달
- 가동 중지 시간 검증
- StatefulSet의 Scale IN/OUT

**설정**

MYSQL 과 관련된 Configuration 은 `ConfigMap` 을 이용하며 아래의 명령으로 생성할 수 있다.

```sh
$ kubectl create -f http://k8s.io/docs/tutorials/stateful-application/mysql-configmap.yaml
```

위의 파일에 구성된 내용은 다음과 같다.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mysql
  labels:
    app: mysql
data:
  master.cnf: |
    # Apply this config only on the master.
    [mysqld]
    log-bin
  slave.cnf: |
    # Apply this config only on slaves.
    [mysqld]
    super-read-only
```

위의 구성은 마스터 서버는 읽기/쓰기가 가능하고 슬래이브 서버들은 모두 읽기만 가능한 것으로 설정된 것이다.

**서비스**

MYSQL 서비스는 아래의 명령으로 생성할 수 있다.

```sh
$ kubectl create -f http://k8s.io/docs/tutorials/stateful-application/mysql-services.yaml
```

서비스 구성 내용은 다음과 같다.

```yaml
# Headless service for stable DNS entries of StatefulSet members.
apiVersion: v1
kind: Service
metadata:
  name: mysql
  labels:
    app: mysql
spec:
  ports:
  - name: mysql
    port: 3306
  clusterIP: None
  selector:
    app: mysql
---
# Client service for connecting to any MySQL instance for reads.
# For writes, you must instead connect to the master: mysql-0.mysql.
apiVersion: v1
kind: Service
metadata:
  name: mysql-read
  labels:
    app: mysql
spec:
  ports:
  - name: mysql
    port: 3306
  selector:
    app: mysql
```

StatefulSet 운영을 위한 Headless 서비스와 읽기 기능을 제공하기 위한 슬래이브용 서비스로 구성되어 있다.

**StatefulSet**

StatefulSet 은 아래의 명령으로 생성할 수 있다.

```sh
kubectl create -f http://k8s.io/docs/tutorials/stateful-application/mysql-statefulset.yaml
```

StatefulSet 의 구성은 다음과 같다.

```yaml
apiVersion: apps/v1beta1
kind: StatefulSet
metadata:
  name: mysql
spec:
  serviceName: mysql
  replicas: 3
  template:
    metadata:
      labels:
        app: mysql
      annotations:
        pod.beta.kubernetes.io/init-containers: '[
          {
            "name": "init-mysql",
            "image": "mysql:5.7",
            "command": ["bash", "-c", "
              set -ex\n
              # Generate mysql server-id from pod ordinal index.\n
              [[ `hostname` =~ -([0-9]+)$ ]] || exit 1\n
              ordinal=${BASH_REMATCH[1]}\n
              echo [mysqld] > /mnt/conf.d/server-id.cnf\n
              # Add an offset to avoid reserved server-id=0 value.\n
              echo server-id=$((100 + $ordinal)) >> /mnt/conf.d/server-id.cnf\n
              # Copy appropriate conf.d files from config-map to emptyDir.\n
              if [[ $ordinal -eq 0 ]]; then\n
                cp /mnt/config-map/master.cnf /mnt/conf.d/\n
              else\n
                cp /mnt/config-map/slave.cnf /mnt/conf.d/\n
              fi\n
            "],
            "volumeMounts": [
              {"name": "conf", "mountPath": "/mnt/conf.d"},
              {"name": "config-map", "mountPath": "/mnt/config-map"}
            ]
          },
          {
            "name": "clone-mysql",
            "image": "gcr.io/google-samples/xtrabackup:1.0",
            "command": ["bash", "-c", "
              set -ex\n
              # Skip the clone if data already exists.\n
              [[ -d /var/lib/mysql/mysql ]] && exit 0\n
              # Skip the clone on master (ordinal index 0).\n
              [[ `hostname` =~ -([0-9]+)$ ]] || exit 1\n
              ordinal=${BASH_REMATCH[1]}\n
              [[ $ordinal -eq 0 ]] && exit 0\n
              # Clone data from previous peer.\n
              ncat --recv-only mysql-$(($ordinal-1)).mysql 3307 | xbstream -x -C /var/lib/mysql\n
              # Prepare the backup.\n
              xtrabackup --prepare --target-dir=/var/lib/mysql\n
            "],
            "volumeMounts": [
              {"name": "data", "mountPath": "/var/lib/mysql", "subPath": "mysql"},
              {"name": "conf", "mountPath": "/etc/mysql/conf.d"}
            ]
          }
        ]'
    spec:
      containers:
      - name: mysql
        image: mysql:5.7
        env:
        - name: MYSQL_ALLOW_EMPTY_PASSWORD
          value: "1"
        ports:
        - name: mysql
          containerPort: 3306
        volumeMounts:
        - name: data
          mountPath: /var/lib/mysql
          subPath: mysql
        - name: conf
          mountPath: /etc/mysql/conf.d
        resources:
          requests:
            cpu: 1
            memory: 1Gi
        livenessProbe:
          exec:
            command: ["mysqladmin", "ping"]
          initialDelaySeconds: 30
          timeoutSeconds: 5
        readinessProbe:
          exec:
            # Check we can execute queries over TCP (skip-networking is off).
            command: ["mysql", "-h", "127.0.0.1", "-e", "SELECT 1"]
          initialDelaySeconds: 5
          timeoutSeconds: 1
      - name: xtrabackup
        image: gcr.io/google-samples/xtrabackup:1.0
        ports:
        - name: xtrabackup
          containerPort: 3307
        command:
        - bash
        - "-c"
        - |
          set -ex
          cd /var/lib/mysql

          # Determine binlog position of cloned data, if any.
          if [[ -f xtrabackup_slave_info ]]; then
            # XtraBackup already generated a partial "CHANGE MASTER TO" query
            # because we're cloning from an existing slave.
            mv xtrabackup_slave_info change_master_to.sql.in
            # Ignore xtrabackup_binlog_info in this case (it's useless).
            rm -f xtrabackup_binlog_info
          elif [[ -f xtrabackup_binlog_info ]]; then
            # We're cloning directly from master. Parse binlog position.
            [[ `cat xtrabackup_binlog_info` =~ ^(.*?)[[:space:]]+(.*?)$ ]] || exit 1
            rm xtrabackup_binlog_info
            echo "CHANGE MASTER TO MASTER_LOG_FILE='${BASH_REMATCH[1]}',\
                  MASTER_LOG_POS=${BASH_REMATCH[2]}" > change_master_to.sql.in
          fi

          # Check if we need to complete a clone by starting replication.
          if [[ -f change_master_to.sql.in ]]; then
            echo "Waiting for mysqld to be ready (accepting connections)"
            until mysql -h 127.0.0.1 -e "SELECT 1"; do sleep 1; done

            echo "Initializing replication from clone position"
            # In case of container restart, attempt this at-most-once.
            mv change_master_to.sql.in change_master_to.sql.orig
            mysql -h 127.0.0.1 <<EOF
          $(<change_master_to.sql.orig),
            MASTER_HOST='mysql-0.mysql',
            MASTER_USER='root',
            MASTER_PASSWORD='',
            MASTER_CONNECT_RETRY=10;
          START SLAVE;
          EOF
          fi

          # Start a server to send backups when requested by peers.
          exec ncat --listen --keep-open --send-only --max-conns=1 3307 -c \
            "xtrabackup --backup --slave-info --stream=xbstream --host=127.0.0.1 --user=root"
        volumeMounts:
        - name: data
          mountPath: /var/lib/mysql
          subPath: mysql
        - name: conf
          mountPath: /etc/mysql/conf.d
        resources:
          requests:
            cpu: 100m
            memory: 100Mi
      volumes:
      - name: conf
        emptyDir: {}
      - name: config-map
        configMap:
          name: mysql
  volumeClaimTemplates:
  - metadata:
      name: data
      annotations:
        volume.alpha.kubernetes.io/storage-class: default
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 10Gi
```

**StatefulSet Pod 초기화 이해**

StatefulSet 의 Pod Template 에 따라서 Pod 를 생성할 때 가장 먼저 `pod.beta.kubernetes.io/init-containers` annotation 으로 지정된 작업을 수행한다.

- `init-mysql` configuration 생성 작업: 0 번째 Pod 은 마스터 역할을 담당하기 때문에 생성했던 ConfigMap 에서 master.cnf 정보를 설정한다. 나머지는 slave.cnf 정보 설정
- 'clone-mysql' 데이터복제: Pod 구성의 순서가 보장되기 때문에 슬래이브 Pod 가 생성되기 전에 마스터가 이미 구성된 상태이므로 복제가 처리될 수 있다. 단, 이런 복제 작업은 MYSQL 에서 제공하지 않기 때문에 `XtraBackup` 이라는 오픈소스를 사용했다. 그리고 생성되는 슬래이브 Pod 는 바로 직전에 생성된 Pod 의 복제된 데이터를 다시 복제한다.

**Client Traffic 전달**

StatefulSet 에 의해서 MYSQL 이 제대로 수행 중인지를 검증하기 위한 것으로 `MySql 5.7` 이미지를 이용해서 트래픽을 검증한다.

```sh
kubectl run mysql-client --image=mysql:5.7 -i -t --rm --restart=Never --\
  mysql -h mysql-0.mysql <<EOF
CREATE DATABASE test;
CREATE TABLE test.messages (message VARCHAR(250));
INSERT INTO test.messages VALUES ('hello');
EOF
```

위의 작업을 통해서 테이블 생성과 데이터 삽입을 처리하고 아래의 명령으로 데이터를 조회해 볼 수 있다.

```sh
kubectl run mysql-client --image=mysql:5.7 -i -t --rm --restart=Never --\
  mysql -h mysql-read -e "SELECT * FROM test.messages"
```

`mysql-read` 서비스를 통해서 복제되어 동작하고 있는 슬래이브 들에 대한 분산 처리가 수행되는지는 아래의 명령을 이용해서 확인할 수 있다.

```sh
kubectl run mysql-client-loop --image=mysql:5.7 -i -t --rm --restart=Never --\
  bash -ic "while sleep 1; do mysql -h mysql-read -e 'SELECT @@server_id,NOW()'; done"
```

**가동 중지 시간 검증**

MySql 컨테이너는 `Readiness Probe` 를 통해서 서버가 살아있고 쿼리를 수행할 수 있는지 확인하기 위해서 `mysql -h 127.0.0.1 -e 'SELECT 1'` 명령을 사용한다. 따라서 이를 실패 상태로 만드는 방법은 아래와 같이 데이터 파일의 이름을 변경해서 동작 오류 상태를 만들어 보면 된다.

```sh
$ kubectl exec mysql-2 -c mysql -- mv /usr/bin/mysql /usr/bin/mysql.off
```

위와 같이 처리하면 몇 초가 지나고 나면 mysql-2 의 Pod 갯수가 변경된 것을 확인할 수 있다.

```
NAME      READY     STATUS    RESTARTS   AGE
mysql-2   1/2       Running   0          3m
```

만일 Pod 가 삭제되는 경우를 가정하면 StatefulSet 은 Pod 를 재 생성하게 된다. 그러나 노드의 유지보수 등의 문제로 MySql 이 배치된 노드가 중지되는 경우를 가정하면 다음과 같은 명령을 사용해서 해당 노드를 중지시킬 수 있다.

```sh
$ kubectl drain <node-name> --force --delete-local-data --ignore-daemonsets
```

위와 같이 처리하면 해당 노드에서 Pod 가 종료되면서 다른 노드에 초기화가 실행되는 것을 확인할 수 있다.

```
NAME      READY   STATUS          RESTARTS   AGE       IP            NODE
mysql-2   2/2     Terminating     0          15m       10.244.1.56   kubernetes-minion-group-9l2t
[...]
mysql-2   0/2     Pending         0          0s        <none>        kubernetes-minion-group-fjlm
mysql-2   0/2     Init:0/2        0          0s        <none>        kubernetes-minion-group-fjlm
mysql-2   0/2     Init:1/2        0          20s       10.244.5.32   kubernetes-minion-group-fjlm
mysql-2   0/2     PodInitializing 0          21s       10.244.5.32   kubernetes-minion-group-fjlm
mysql-2   1/2     Running         0          22s       10.244.5.32   kubernetes-minion-group-fjlm
mysql-2   2/2     Running         0          30s       10.244.5.32   kubernetes-minion-group-fjlm
```

**StatefulSet의 Scale IN/OUT**

Scaling 은 아래의 명령으로 처리할 수 있다.

```sh
$ kubectl scale --replicas=5 statefulset mysql
```

2개의 슬레이브 Pod 가 추가로 생성된다. 따라서 복제된 데이터를 가지고 동작하는지는 아래의 명령을 통해서 확인할 수 있다.

```sh
kubectl run mysql-client --image=mysql:5.7 -i -t --rm --restart=Never --\
  mysql -h mysql-3.mysql -e "SELECT * FROM test.messages"
```

지금까지 Kubernetes 에서 제공하는 MySql 이중화에 대해서 확인을 해 보았지만, 실제 운영에서 사용하기에는 문제가 존재한다.

- 읽기/쓰기 와 읽기전용 으로 서비스가 분리되어 있기 때문에 이를 활용하는 어플리케이션은 문제 소지가 다분하다.
- 다중 마스터 구조가 아니기 때문에 마스터가 죽으면 운영에 문제가 발생한다.
- 기반이 되는 물리적인 상태의 검증과 문제 발생 시에 복구하는 부분이 없다.
- 순서대로 처리되는 Pod 처리 작업은 생성과 장애 발생이라는 무한 반복이라는 상황에 대한 대비책이 없다.

```
NAME                        READY     STATUS        RESTARTS   AGE
[...]
mysql-0                     2/2       Running       4          4d
mysql-2                     0/2       Init:0/2      0          19h
[...]
```

위의 사례는 mysql-2 가 초기화 중에 문제가 발생했고, mysql-1 은 죽어 있는 상태를 보여주는 것이다. 따라서 mysql-1 은 mysql-2 가 정상이 아니라서 재 생성될 수 없고,
mysql-2 는 처리가 되더라도 mysql-1 이 존재하지 않기 때문에 동작하지 않는다.

---

### Persistent Volume 설정 방식

**참고문서**
- https://kubernetes.io/docs/user-guide/persistent-volumes/

#### Static 방식

- 관리자가 미리 AWS 콘솔에서 EBS 볼륨을 생성해 놓아야 한다.
- 생성될 Pod Deployment 설정의 `spec.containers[].container.volumeMounts` 정보와 `spec.volumes` 에 설정된 부분을 연결해 주면 된다.

AWS 의 EBS 를 Static 방식으로 연결한 예제는 다음과 같다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: aws-web
spec:
  containers:
    - name: web
      image: nginx
      ports:
        - name: web
          containerPort: 80
          protocol: tcp
      volumeMounts:
        - name: html-volume
          mountPath: "/usr/share/nginx/html"
  volumes:
    - name: html-volume
      awsElasticBlockStore:
        # Enter the volume ID below
        volumeID: volume_ID
        fsType: ext4
```

위의 예제는 다음과 같은 형식으로 볼륨이 처리 된다.

1. 관리자가 EBS 볼륨을 생성하면 Volume ID 가 식별된다. (AWS 콘솔)
2. Depolyment (또는 Pod) 의 `spec.volumes` 에 다음과 같이 정보를 설정한다.
  - Container 의 VolumeMounts 부분에서 사용할 수 있도록 `name` 정보 설정
  - `awsElasticBlockStore` 섹션 설정
    - `volumeID` 에 생성된 Volume ID 값 지정
    - `fsType` 에서 File System 형식 지정 (일반적으로 ext4)
3. `spec.containers[].volumeMounts` 정보를 `spec.volumes` 정보를 사용하도록 설정
  - `name` 은 volumes 에 등록해 놓은 이름을 지정
  - `mountPath` 는 Volume 이 Container에서 사용할 수 있도록 마운트될 Path 지정 (UI 의 ContainerPath)

> __Importants__ 현재 구현된 Cocktail 에서는 Kubernetes 관련해서 Volumes 정보가 HostPath 를 기준으로 구성되어 있으므로 실제 적용할 수 없습니다. 따라서 위와 같은 정보가 구성될 수 있도록 구성해 줘야 합니다.
>
> 구성되어야 하는 정보는 다음과 같습니다.
> - VolumeVO (Volume 설정 형식에 따라서 구성 필요. HostPath / Static Volume / Dynamic Volume)
> - UI 에서 VolumeVO 에 맞도록 형식 선택과 선택된 형식에 맞는 Schema 및 UI 제공 필요.
> - KubeSpecFactory에 Volume 지정 형식에 따른 Deployment 조정 필요. (JSON 구성을 위한 처리)

#### Dynamic Provisioning 방식

이 방식은 AWS 의 EBS 를 사용하는 것과 동일하지만 (기타 Provider 들에 대한 정보는 아래의 Volume 과 관련된 부분 참고) 관리자가 미리 생성해 놓은 볼륨을 사용하는 것이 아니라 사용자 (정확히는 Pod 구성에 따른 요청) 가 원하는 볼륨을 즉시 생성해서 볼륨으로 연결하는 방식을 의미한다.

이 방식은 관리자가 미리 볼륨을 생성해 놓지 않아도 되는 장점이 있지만 사용자가 사용하기 위한 몇 가지 기본적인 구성을 해 놓아야 한다.

**StorageClass (스토리지 특성 정보)**

사용자는 어떤 Provider를 통해서 어떻게 볼륨을 구성되는지 알 필요가 없다. 그러나 사용할 볼륨의 스토리지에 대한 특성은 지정할 수 있어야 하기 때문에 관리자가 미리 여러 가지의 StorageClass 를 구성해 놓아야 한다.

```yaml
kind: StorageClass
apiVersion: storage.k8s.io/v1
metadata:
  name: slow
provisioner: kubernetes.io/aws-ebs
parameters:
  type: io1
  zone: us-east-1d
  iopsPerGB: "10"
```

- `metadata.name`: 실제 사용자가 지정할 떄 사용하므로 특성을 유추할 수 있는 이름을 지정한다. ex. slow, fast, standard, ...
- `provisioner`: 스토리지를 구성하는 플러그인을 지정한다. ex. kubernetes.io/aws-ebs, kubernetes.io/gce-pd, ...
- `parameters`: 이하 `type`, 'zone', 'iopsPerGB' 등은 제공하는 Provisioner 에 따라서 지정되는 항목이 다르므로 맞춰서 지정해야 한다.

**PersistentVolumeClaim (볼륨 요청 정보)**

사용자가 원하는 스토리지 특성은 `StorageClass` 로 지정되었으므로 실제 사용하는데 필요한 `accessModes` 와 `storage` 크기 정보들을 지정해야 한다.

```yaml
kind: PersistentVolumeClaim
apiVersion: v1
metadata:
 name: claim1
spec:
 accessModes:
  -
   ReadWriteOnce
 resources:
  requests:
   storage: 3Gi
 storageClassName: slow
```

- `metadata.name`: 이름은 실제 Pod Deployment 에서 사용할 이름이다.
- `spec.accessModes`: 볼륨을 어떤 액세스 방식으로 마운트할지를 지정한다. ex. ReadWriteOnce - 단일노드가 R/W 방식으로 독점으로 마운트, ReadOnlyMany - 여러 노드에서 읽기 전용으로 마운트, ...
- `resources.requests.storages`: 필요한 볼륨 용량 지정
- `storageClass`: 볼륨 요청에 따라서 자동 생성할 때 사용할 특정 정보 지정 (위에서 생성한 StorageClass 의 이름)

위의 조건에 따라서 실제 볼륨이 생성된다. 생성된 볼륨 자체를 `PersistentVolume` 이라고 한다.

**Pod or Deployment**

실제 배포되어 운영될 Pod 또는 Deployment 에서는 볼륨을 직접 처리하는 것이 아니라 동적으로 생성될 수 있도록 PersistentVolumeClaim 정보를 지정해서 처리하게 된다.

```yaml
apiVersion: v1
kind: ReplicationController
metadata:
  name: server
spec:
  replicas: 1
  selector:
    role: server
  template:
    metadata:
      labels:
        role: server
    spec:
      containers:
      - name: server
        image: nginx
        volumeMounts:
          - mountPath: /var/lib/www/html
            name: mypvc
      volumes:
        - name: mypvc
          persistentVolumeClaim:
            claimName: claim1
```

- `spec.template.spec.volumes.name`: Container의 volumntMounts 에서 사용할 이름을 지정한다.
- `spec.template.spec.volumes.persistentVolumeClaim.claimName`: 위에서 생성한 PerssistentVolumeClaim 이름을 지정한다.
- `spec.template.spec.containers[].volumeMounts.name`: 구성한 volumes 의 이름을 지정한다.

Kubernetes 에서 Deployment 가 호출되면 Container의 volumneMounts 를 처리할 때 지정된 persistentVolumeClaim 을 찾아서 클러스터내의 볼륨을 검색한다. 그러나 조건에 맞는 (PersistentVolumeClaim 과 StorageClass에 지정된 조건) 사용 가능한 볼륨이 없다면 조건에 맞는 볼륨을 생성한다.

> __Importants__ 이 부분도 역시 위의 Static 에서 주의한 것과 같이 VO/UI/KubeSpecFactory 를 사용가능하도록 조정해서 처리해야 한다. 또한 주의할 점은 이렇게 동적으로 구성되었던 Volume 은 클러스터의 Volume 처리 정책에 따라서 재 사용이 가능한지에 대한 검토가 필요하다.

---

### Resource Quota 적용을 통한 리소스 관리

**참고문서**
- https://kubernetes.io/docs/admin/resourcequota/

Kubernetes Cluster 에서 운영되는 모든 Application 은 Cluster에 구성된 Slave Node 의 리소스 (현재는 CPU / Memory) 를 기준으로 배치와 운영이 된다. 따라서 다음과 같은 부분을 염두에 두고 운영을 해야 한다.

- 리소스 지정 또는 할당 정보가 없는 경우의 Pod 는 배치되는 Node 의 모든 가용 리소스를 사용할 수 있다는 것을 전제로 한다.
- ResourceQuota 는 Cluster 내에 존재하는 Namespace 를 대상으로 하고, 각 Namespace 별로 1개 이상의 ResourceQuota를 구성할 수 있다.
- ResourceQuota 는 유형별로 생성되는 객체의 수 제한, Compute Resource 총량 제한 등을 지정할 수 있다.
- Cluster 내에 여러 개의 Namespace 가 존재하는 경우에 각 Namespace 단위의 리소스 사용량이 합산되지 않으므로 Namespace 별로 지정된 Quota 량과 실제 사용한 량이 다를 수 있다.
- Quota 는 CPU / MEMORY 에 대한 request 와 limit 로 지정된다.
- request 는 실제 보장되어야 하는 Container의 요구량이라고 보면 된다.
- limit 는 Container 가 request를 넘어서는 리소스를 사용할 때 최대치라고 보면 된다. Memory limit 를 넘어서면 Container는 종료 된다.
- Cluster에 생성/변경되는 Pod 반드시 request / limit 에 대한 정보를 명시해야 한다. 명시가 힘들 경우는 `LimitRange` 를 정의해서 기본 값으로 대신할 수 있다.

현재까지 테스트를 통해서 정리한 구성은 다음과 같다.
`단, DEV 클러스터를 기준으로 산정한 것으로 STG 에서는 Slave 의 Capacity와 수를 기준으로 반영해서 적용하도록 한다.`

#### STG 에 적용된 Resource Quota 설정

**BestEffort 범주의 Resource Quota (besteffort.yaml)**

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: best-effort
  namespace: cocktail-kubernetes-aws
spec:
  hard:
    pods: "100"     # 리소스 미 지정 Pod 를 최대 100까지 허용, 그 이상은 생성되지 않고 오류 처리 됨.
  scopes:
  - BestEffort
```

생성과 삭제는 아래의 명령을 이용한다. (향후 API Server를 이용하려는 경우는 Cocktail API 구성 후 Kubernetes API 연계 필요)

```sh
# 생성
$ kubectl create -f besteffort.yaml
# 삭제
$ kubectl delete quota best-effort -n cocktail-kubernetes-aws
```

**NotBestEffort 범주의 Resource Quota (notbesteffort.yaml)**

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: not-best-effort
  namespace: cocktail-kubernetes-aws
spec:
  hard:
    pods: "30"
    requests.cpu: "8"
    requests.memory: "16Gi"
    limits.cpu: "9"
    limits.memory: "18Gi"
  scopes:
  - NotBestEffort
```

생성과 삭제는 아래의 명령을 이용한다. (향후 API Server를 이용하려는 경우는 Cocktail API 구성 후 Kubernetes API 연계 필요)

```sh
# 생성
$ kubectl create -f notbesteffort.yaml
# 삭제
$ kubectl delete quota not-best-effort -n cocktail-kubernetes-aws
```

#### DEV 에 적용 테스트한 설정

**Resource Quota**

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: cocktail-quota
  namespace: cocktail-kubernetes    # 실제 Namespace 적용
spec:
  hard:
    cpu: "9"
    memory: "18Gi"
    pods: "200"
    replicationcontrollers: "0"
    resourcequotas: "2"
    services: "100"
```

위의 설정을 `resourcequota.yaml` 파일로 Master Node 에 생성하고 아래의 명령으로 적용한다.

```sh
$ kubectl create -f resourcequota.yaml
```

**LimitRange**

```yaml
apiVersion: v1
kind: LimitRange
metadata:
  name: cocktail-limits
  namespace: cocktail-kubernetes    # 실제 Namespace 적용
spec:
  limits:
  - default:
      cpu: 200m
      memory: 400Mi
    defaultRequest:
      cpu: 100m
      memory: 200Mi
    type: Container
```

위의 설정을 `limitrange.yaml` 파일로 Master Node 에 생성하고 아래의 명령으로 적용한다.

```sh
$ kubectl create -f limitrange.yaml
```

수정은 제공되지 않으므로 변경해야 할 사항이 있다면 재 생성해야 한다.

```sh
$ kubectl delete quota cocktail-quota -n cocktail-kubernetes    # 실제 Namespace 적용
$ kubectl delete limits cocktail-limits -n cocktail-kubernetes  # 실제 Namespace 적용
```

> **Nodes**
>
> 위의 정의된 Quota 와 LimitRange 는 Demo에서 운영되는 어플리케이션들을 기준으로 Slave 가 Taint 되지 않는 상태를 유지하기 위한 설정이다.

Resource Quota 할당량과 실제 사용량 집계 정보는 Kubernetes Dashboard 에서 Namespace 를 선택하면 확인할 수 있다. 실제 어플리케이션들을 운영하면서 Cluster를 구성하고 있는 Node 들의 Capacity 와 Quota 및 Namespace 갯수들에 대한 평가와 정리를 통해서 적당한 값을 추적하고 설정하는 것이 좋다.

---

### 특정 Pod를 Master Node로 고정하는 방법 검토

기본적으로 Kubernetes의 Resource 상태에 따른 Pod 배치를 기본으로 하고 있다. 그러나 상황에 따라서 성능이 좋은 Node 또는 특정한 Node 를 선택해야 하는 경우가 발생하기 때문에 아래와 같이 처리가 가능하다.

- Label 지정 값에 따른 `Node Selector` 지정하여 Node를 선택하는 방법 - Master Node를 Scheduling에 포함하지 않았다면 Master는 제외, Default 설치라면 Master는 Pod Scheduling 대상이 아님
- Annotation 지정 값에 따른 `Master Node` 선택 방법

`Heaspster, InfluxDB, Dashboard` 등을 Master Node에 배치하는 경우라면 `Annotation` 방식을 사용해서 지정이 가능하다.

```json
...
    annotations:
        scheduler.alpha.kubernetes.io/tolerations: |
          [
            {
              "key": "dedicated",
              "operator": "Equal",
              "value": "master",
              "effect": "NoSchedule"
            }
          ]
...
```

`Deployment` yaml 또는 Json 파일에 상기와 같이 지정해서 Scheduler 가 해당 Pod를 무조건 Master Node에 설치하도록 지정할 수 있다.

> 단, Heapster, InfluxDB, Dashboard 는 모두 부하가 발생할 수 있는 Container 들이므로 Master의 성능이 좋아야 한다. 그렇지 않다면 전반적인 Kubernetes의 운영 상황 (Proxing, Scheduling, Controlling, Slave Status Checking, ...) 의 성능이 저하될 수 도 있다.
>
> 그러나, 상기와 같이 고정을 하게 되면 Slave Node 가 리소스 부족이나 기타 다른 상황에 의해서 서비스가 불가능해진 상태에서도 관련된 서비스는 Master가 죽지 않는다면 계속 유지 될 수 있다.

아래는 실제 구성한 Heaspter yaml 샘플이다.

```yaml
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  name: heapster
  namespace: kube-system
spec:
  replicas: 1
  template:
    metadata:
      labels:
        task: monitoring
        k8s-app: heapster
      annotations:
        scheduler.alpha.kubernetes.io/tolerations: |
          [
            {
              "key": "dedicated",
              "operator": "Equal",
              "value": "master",
              "effect": "NoSchedule"
            }
          ]
    spec:
      containers:
      - name: heapster
        image: gcr.io/google_containers/heapster-amd64:v1.3.0-beta.1
        imagePullPolicy: IfNotPresent
        command:
        - /heapster
        - --source=kubernetes:https://kubernetes.default
        - --sink=influxdb:http://monitoring-influxdb:8086
```
---

### Host Specific Path 를 Pod Volume으로 설정해서 Container에 연결하는 방법 검토

**참고문서**
- https://kubernetes.io/docs/concepts/storage/volumes/

Kubernetes 에서는 Volume 정보를 추상화해서 제공하고 있다. 따라서 Docker에서 제공하는 기본적인 Volume관리 기능보다 아래와 같은 장점이 존재한다.

- Pod에 지정된 Volume의 Life-cycle이 Container 보다 길기 때문에 Container가 종료되어도 Pod가 종료되지 않는 한 볼륨 정보가 유지된다. 물론 Pod가 종료되는 경우는 상태가 유지되지 않는다.
- Pod 내의 Container 들 간에 정보 공유가 필요한 경우도 Pod의 볼륨을 공유하는 것이 가능하다.

현재 버전 (1.5)에서 지원하고 있는 볼륨의 형식은 다음과 같다.

- emptyDir : Pod가 Node에 배치될 때 생성되는 빈 공간으로 Container들이 공유해서 쓰기/읽기가 가능하며, Memory (tmpfs) 디스크 설정도 가능하다. 주로 Cache 또는 Scrach 로 사용한다.
- **hostPath** : Node의 파일 시스템을 Pod에 마운트해서 사용한다.
- gcePersistentDisk : GCE (Google Compute Engine) 환경의 Persistent Disk (PD)
- awsElasticBlockStore (EBS) : Amazon Web Service에서의 EBS
- nfs
- iscsi
- flocker
- glusterfs
- rbd
- cephfs
- gitRepo : Git Repository
- secret : 비밀번호 등의 보안 정보를 Kubernetes API 에 등록하고 가상의 디스크처럼 Pod에서 마운트해서 사용하는 방식
- persistentVolumeClaim : PD 를 사용하기 위한 요청을 제공하는 방식
- downwardAPI
- azureFileVolume : Azure File Volume (SMB 2.1 and 3.0)
- azureDisk : Azure Data Disk
- vsphereVolume
- Quobyte

위의 다양한 볼륨 설정 방식 중에서 우선은 hostPath 방식을 이용해서 Container에서 동일한 Root 밑에 존재하는 서로 다른 폴더를 운영하는 방법을 검토한 결과 다음과 같이 동작을 설정할 수 있다.

![Kubernetes `hostPath` Volume 구성](https://drive.google.com/uc?id=0BxB40ItyDj9ZQk5zc1EyZ3BiMUk)

위의 그림과 같은 Volume 설정 및 공유를 위해서는 아래와 같이 `Deployment ConfigSet` 설정을 해 주어야 한다.

```json
{
  "kind": "Deployment",
  "apiVersion": "extensions/v1beta1",
  "metadata": {
    "name": "lampstack-site",
    "namespace": "self-test",
    "labels": {
      "app": "lampstack-site"
    }
  },
  "spec": {
    "replicas": 2,
    "template": {
      "metadata": {
        "labels": {
          "app": "lampstack-site"
        }
      },
      "spec": {
        "containers": [{
          "name": "mysql",
          "image": "mysql",
          "env": [{
            "name": "MYSQL_ROOT_PASSWORD",
            "value": "cocktail"
          }],
          "volumeMounts": [{
            "mountPath": "/var/lib/mysql",
            "name": "site-data",
            "subPath": "mysql"
          }]
        },
        {
          "name": "apache-php",
          "image": "tutum/apache-php",
          "volumeMounts": [{
            "mountPath": "/var/www/html",
            "name": "site-data",
            "subPath": "html"
          }]
        }],
        "volumes": [{
          "name": "site-data",
          "hostPath": {
            "path": "/usr/test/data"
          }
        }]
      }
    }
  }
}
```

---

### Server 생성을 위한 Configuration Set 검토

> 현재 Kubernetes 의 Namespace에 매치되는 구성이 없기 때문에 **`cocktail-kubernetes`** 라는 고정 Namespace를 이용하는 것으로 가정하고 진행합니다.

UI 에서 전달된 ServerAddVO 객체를 기준으로 다음과 같이 ConfigSet 을 생성해서 처리한다.

- 기본정보
  - Namespace
    - apiVersion : v1
    - kind : Namespace
  - Deployment
    - apiVersion : extensions/v1beta1
    - kind : Deployment
  - Service
    - apiVersion : v1
    - kind : Service
- Metadata
  - Deployment
    - name : my-server-1111
    - namespace : cocktail-kubernetes
    - labels
      - app : my-server-1111
  - Service
    - name : my-server-1111
    - namespace : cocktail-kubernetes
    - labels
      - app : my-server-1111
- Spec
  - Deployment
    - replicas : 4
    - template
      - metadata
        - labels
          - app : my-server-1111
      - spec
        - containers (Array)
          - 0
            - name : my-server-1111
            - image : nginx
            - ports (Array)
              - 0
                - hostPort : 80
                - containerPort : 80
    - strategy
  - Service
    - type : NodePort
    - ports (Array)
      - 0
        - port : 80
        - targetPort : 80
    - selector
      - app: my-server-1111

#### Namespace ConfigSet

**Create**

```json
{
    "apiVersion": "v1",
    "kind": "Namespace",
    "metadata": {
        "name": "cocktail-kubernetes",
    }
}
```

**Result**

```json
{
    "apiVersion": "v1",
    "kind": "Namespace",
    "metadata": {
        "creationTimestamp": "2017-02-07T12:03:09Z",
        "finalizers": [],
        "name": "cocktail-kubernetes",
        "ownerReferences": [],
        "resourceVersion": "656209",
        "selfLink": "/api/v1/namespacescocktail-kubernetes",
        "uid": "641f8db6-ed2d-11e6-8bda-026429da8ef7"
    },
    "spec": {
        "finalizers": [
            "kubernetes"
        ]
    },
    "status": {
        "phase": "Active"
    }
}
```

#### Deployment ConfigSet

**Create**

```json
{
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
        "name": "my-server-1111",
        "namespace": "cocktail-kubernetes",
        "labels": {
            "app": "my-server-1111"
        }
    },
    "spec": {
        "replicas": 4,
        "template": {
            "metadata": {
                "labels": {
                    "app": "my-server-1111"
                }
            },
            "spec": {
                "containers": [
                    {
                        "name": "my-server-1111",
                        "image": "nginx",
                        "ports": [
                            {
                                "port": 80,
                                "containerPort": 80
                            }
                        ]
                    }
                ]
            }
        }
    }
}
```

**Result**

```json
{
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
        "creationTimestamp": "2017-02-08T04:21:30Z",
        "finalizers": [],
        "generation": 1,
        "labels": {
            "app": "my-server-1111"
        },
        "name": "my-server-1111",
        "namespace": "cocktail-kubernetes",
        "ownerReferences": [],
        "resourceVersion": "733734",
        "selfLink": "/apis/extensions/v1beta1/namespaces/cocktail-kubernetes/deployments/my-server-1111",
        "uid": "111adc81-edb6-11e6-8bda-026429da8ef7"
    },
    "spec": {
        "replicas": 4,
        "selector": {
            "matchExpressions": [],
            "matchLabels": {
                "app": "my-server-1111"
            }
        },
        "strategy": {
            "rollingUpdate": {
                "maxSurge": 1,
                "maxUnavailable": 1
            },
            "type": "RollingUpdate"
        },
        "template": {
            "metadata": {
                "finalizers": [],
                "labels": {
                    "app": "my-server-1111"
                },
                "ownerReferences": []
            },
            "spec": {
                "containers": [
                    {
                        "args": [],
                        "command": [],
                        "env": [],
                        "image": "nginx",
                        "imagePullPolicy": "Always",
                        "name": "my-server-1111",
                        "ports": [
                            {
                                "containerPort": 80,
                                "protocol": "TCP"
                            }
                        ],
                        "resources": {},
                        "terminationMessagePath": "/dev/termination-log",
                        "volumeMounts": []
                    }
                ],
                "dnsPolicy": "ClusterFirst",
                "imagePullSecrets": [],
                "restartPolicy": "Always",
                "securityContext": {
                    "supplementalGroups": []
                },
                "terminationGracePeriodSeconds": 30,
                "volumes": []
            }
        }
    },
    "status": {}
}
```

#### Service ConfigSet

**Create**

```json
{
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
        "name": "my-server-1111",
        "namespace": "cocktail-kubernetes",
        "labels": {
            "app": "my-server-1111"
        }
    },
    "spec": {
        "type": "NodePort",
        "ports": [
            {
                "port": 80,
                "targetPort": 80
            }
        ],
        "selector": {
            "app": "my-server-1111"
        }
    }
}
```

**Result**

```json
{
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
        "creationTimestamp": "2017-02-08T04:21:30Z",
        "finalizers": [],
        "labels": {
            "app": "my-server-1111"
        },
        "name": "my-server-1111",
        "namespace": "cocktail-kubernetes",
        "ownerReferences": [],
        "resourceVersion": "733764",
        "selfLink": "/api/v1/namespaces/cocktail-kubernetes/services/my-server-1111",
        "uid": "112a608c-edb6-11e6-8bda-026429da8ef7"
    },
    "spec": {
        "clusterIP": "10.98.99.77",
        "deprecatedPublicIPs": [],
        "externalIPs": [],
        "loadBalancerSourceRanges": [],
        "ports": [
            {
                "nodePort": 30655,
                "port": 80,
                "protocol": "TCP",
                "targetPort": 80
            }
        ],
        "selector": {
            "app": "my-server-1111"
        },
        "sessionAffinity": "None",
        "type": "NodePort"
    },
    "status": {
        "loadBalancer": {
            "ingress": []
        }
    }
}
```

#### Server 생성 결과

Server의 생성 결과는 JsonObject 형식으로 반환됩니다.

```json
{
    "error": false,
    "message": "Server created normally",
    "results": {
        "Namespace": {
            "apiVersion": "v1",
            "kind": "Namespace",
            "metadata": {
                "creationTimestamp": "2017-02-07T12:03:09Z",
                "finalizers": [],
                "name": "cocktail-kubernetes",
                "ownerReferences": [],
                "resourceVersion": "656209",
                "selfLink": "/api/v1/namespacescocktail-kubernetes",
                "uid": "641f8db6-ed2d-11e6-8bda-026429da8ef7"
            },
            "spec": {
                "finalizers": [
                    "kubernetes"
                ]
            },
            "status": {
                "phase": "Active"
            }
        },
        "Deployment": {
            "apiVersion": "extensions/v1beta1",
            "kind": "Deployment",
            "metadata": {
                "creationTimestamp": "2017-02-08T04:51:36Z",
                "finalizers": [],
                "generation": 1,
                "labels": {
                    "app": "my-server-1111"
                },
                "name": "my-server-1111",
                "namespace": "cocktail-kubernetes",
                "ownerReferences": [],
                "resourceVersion": "736182",
                "selfLink": "/apis/extensions/v1beta1/namespaces/cocktail-kubernetes/deployments/my-server-1111",
                "uid": "453a93f1-edba-11e6-8bda-026429da8ef7"
            },
            "spec": {
                "replicas": 4,
                "selector": {
                    "matchExpressions": [],
                    "matchLabels": {
                        "app": "my-server-1111"
                    }
                },
                "strategy": {
                    "rollingUpdate": {
                        "maxSurge": 1,
                        "maxUnavailable": 1
                    },
                    "type": "RollingUpdate"
                },
                "template": {
                    "metadata": {
                        "finalizers": [],
                        "labels": {
                            "app": "my-server-1111"
                        },
                        "ownerReferences": []
                    },
                    "spec": {
                        "containers": [
                            {
                                "args": [],
                                "command": [],
                                "env": [],
                                "image": "nginx",
                                "imagePullPolicy": "Always",
                                "name": "my-server-1111",
                                "ports": [
                                    {
                                        "containerPort": 80,
                                        "protocol": "TCP"
                                    }
                                ],
                                "resources": {},
                                "terminationMessagePath": "/dev/termination-log",
                                "volumeMounts": []
                            }
                        ],
                        "dnsPolicy": "ClusterFirst",
                        "imagePullSecrets": [],
                        "restartPolicy": "Always",
                        "securityContext": {
                            "supplementalGroups": []
                        },
                        "terminationGracePeriodSeconds": 30,
                        "volumes": []
                    }
                }
            },
            "status": {}
        },
        "Service": {
            "apiVersion": "v1",
            "kind": "Service",
            "metadata": {
                "creationTimestamp": "2017-02-08T04:51:36Z",
                "finalizers": [],
                "labels": {
                    "app": "my-server-1111"
                },
                "name": "my-server-1111",
                "namespace": "cocktail-kubernetes",
                "ownerReferences": [],
                "resourceVersion": "736212",
                "selfLink": "/api/v1/namespaces/cocktail-kubernetes/services/my-server-1111",
                "uid": "454cc2f7-edba-11e6-8bda-026429da8ef7"
            },
            "spec": {
                "clusterIP": "10.106.42.124",
                "deprecatedPublicIPs": [],
                "externalIPs": [],
                "loadBalancerSourceRanges": [],
                "ports": [
                    {
                        "nodePort": 30274,
                        "port": 80,
                        "protocol": "TCP",
                        "targetPort": 80
                    }
                ],
                "selector": {
                    "app": "my-server-1111"
                },
                "sessionAffinity": "None",
                "type": "NodePort"
            },
            "status": {
                "loadBalancer": {
                    "ingress": []
                }
            }
        }
    }
}
```

Service의 경우에 NodePort, LoadBalaner 형식으로 외부에 노출될 경우는 `NodePort` 정보를 추출해서 관리해야 하므로 Server 생성 결과에서 아래의 같은 코드로 NodePort 값을 추출해야 합니다.

```java
KubernetesProvider provider = new KubernetesProvider();

// Kubernetes Client 생성
provier.create("https://<master server api address>:6443", "Bearer Token");

// Server 생성 호출
JsonObject jo = KubernetesProvider.createServer(serverAddVO);

// Service 의 NodePort 추출 (JsonPath 활용)
JPath jp = new JPath(jo);
int nodePort = jp.getInteger("results.Service.spec.ports[0].nodePort");
```

#### Deployment Status 결과

```json
{
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
        "annotations": {
            "deployment.kubernetes.io/revision": "1"
        },
        "creationTimestamp": "2017-02-08T11:39:55Z",
        "finalizers": [],
        "generation": 1,
        "labels": {
            "app": "my-server-1111"
        },
        "name": "my-server-1111",
        "namespace": "self-test",
        "ownerReferences": [],
        "resourceVersion": "768611",
        "selfLink": "/apis/extensions/v1beta1/namespaces/self-test/deployments/my-server-1111",
        "uid": "4fcefa85-edf3-11e6-8822-026429da8ef7"
    },
    "spec": {
        "replicas": 4,
        "selector": {
            "matchExpressions": [],
            "matchLabels": {
                "app": "my-server-1111"
            }
        },
        "strategy": {
            "rollingUpdate": {
                "maxSurge": 1,
                "maxUnavailable": 1
            },
            "type": "RollingUpdate"
        },
        "template": {
            "metadata": {
                "finalizers": [],
                "labels": {
                    "app": "my-server-1111"
                },
                "ownerReferences": []
            },
            "spec": {
                "containers": [
                    {
                        "args": [],
                        "command": [],
                        "env": [],
                        "image": "nginx",
                        "imagePullPolicy": "Always",
                        "name": "my-server-1111",
                        "ports": [
                            {
                                "containerPort": 80,
                                "protocol": "TCP"
                            }
                        ],
                        "resources": {},
                        "terminationMessagePath": "/dev/termination-log",
                        "volumeMounts": []
                    }
                ],
                "dnsPolicy": "ClusterFirst",
                "imagePullSecrets": [],
                "restartPolicy": "Always",
                "securityContext": {
                    "supplementalGroups": []
                },
                "terminationGracePeriodSeconds": 30,
                "volumes": []
            }
        }
    },
    "status": {
        "availableReplicas": 4,
        "observedGeneration": 1,
        "replicas": 4,
        "updatedReplicas": 4,
        "conditions": [
            {
                "type": "Available",
                "status": "True",
                "lastUpdateTime": "2017-02-08T11:39:59Z",
                "lastTransitionTime": "2017-02-08T11:39:59Z",
                "reason": "MinimumReplicasAvailable",
                "message": "Deployment has minimum availability."
            }
        ]
    }
}
```

위의 결과에서 `replicas` 와 `availableReplicas' 를 기준으로 인스턴스 수를 구하고, `status` 정보를 기준로 상태를 검증하면 된다.

```java
KubernetesProvider provider = new KubernetesProvider();

// Kubernetes Client 생성
provier.create("https://<master server api address>:6443", "Bearer Token");

// Server 생성 호출
JsonObject jo = KubernetesProvider.createServer(serverAddVO);

// Service 의 NodePort 추출 (JsonPath 활용)
JPath jp = new JPath(jo);
int replicas = jp.getInteger("results.Deployment.status.replicas");
int availableReplicas = jp.getInteger("results.Deployment.status.availableReplicas");
boolelan status = jp.getBoolean("results.Deployment.status.conditions[0].status");
```

---

### Private Registry 처리 방법

- Registry URL Base : 52.79.168.157[:port 80이 아닌 경우]
- UI Searched Image URL : cocktail-dev/nginx
- UI Searched Image Tag : latest

최종 Registry Image URL : **`52.79.168.157[:port 80이 아닌 경우]/cocktail-dev/nginx:latest`**

> Kubernetes에서는 Container의 Image Tag를 지정하지 않기 때문에 상기와 같이 모든 정보가 URL 하나로 표현되어야 합니다.
>
> 또한 Kubernetes Cluster를 구성하는 모든 Node 의 Docker Daemon에 대해서 아래와 같이 옵션을 설정해서 Private Registry를 바라 볼 수 있도록 해 줘야 합니다.
> ```json
> cat /etc/docker/daemon.json
> {
>   "insecure-registries": [
>     "52.79.168.157"
>   ]
> }
> ```

---

### Cocktail + Docker + Kubernetes Container 연계

| Item         | Cocktail     | Docker                                   | Kubernetes                          |
| ------------ | ------------ | ---------------------------------------- | ----------------------------------- |
| Command 지정   | String       | Entrypoint with args (inputed all string) | Command only (Entrypoint Overrided) |
| Command 미 지정 | Empty String | Image default Entrypoint and cmd         | Image default Entrypoint and cmd    |

> **Docker vs Kubernetes**
> | Description                         | Docker field name | Kubernetes field name |
> | ----------------------------------- | ----------------- | --------------------- |
> | The command run by the container    | Entrypoint        | command               |
> | The arguments passed to the command | Cmd               | args                  |

---

### API Server 외부 노출 문제 및 연결 문제 검토 (Java Client 연계)

Kubeadm 툴을 이용한 설치를 했을 경우는 Master Node 를 초기화할 때 `api-advertise-addresses` 또는 `api-external-dns-names` 설정을 추가해서 외부 노출을 위한 처리를 해야 합니다. 각 옵션은 여러 개를 `,`로 구분해서 지정할 수 있습니다.

> 단, 상기와 같이 여러 개의 IP 또는 DNS를 설정했을 때 Kubeadm 의 현재 버전 1.6.x-alpha 에서는 Slave Node를 등록하는 시점에 Multiple Endpoints 문제가 생기면서 오류가 발생하여 Cluster에 Slave Node 추가가 되지 않는 상태입니다. 따라서 현재는 하나만 지정해야 합니다.

**Master 서버 초기화**

```sh
kubeadm init --api-advertise-addresses=52.79.186.103
```

**Slave Node Join to master node**

```sh
kubeadm join --token=8a01a2.0e37af2239bef06e 52.79.186.103
```

**외부에서 연결하는 경우 (Browser 또는 REST Client)**

현재 처리 방식은 인증서 방식이 아닌 Bearer Token 방식을 이용합니다. 각 Kubernetes Components (Kubelet, Api, ...) 들도 이 Token을 이용해서 운영되는 것으로 보입니다. 따라서 외부에서 연결하는 부분은 TLS 인증 등을 추가적으로 설정할 수 있으나 Kubeadm에서 Master 초기화 시점에 구성한 Secret 정보를 기준으로 연계하기에는 파악하고 테스트하는 기간이 상당히(?) 필요할 듯 하여 Bearer Token을 이용하는 것으로 적용합니다.

이를 위해서는 Http Header에 아래와 같이 Bearer Token 을 추가해 주어야 합니다.

```text
Authorization: Bearer 0e37af2239bef06e
```

그리고 API 접속을 위한 URL은 위에서 지정한 IP 를 기준으로 합니다.

https://52.79.186.103:6443

---

### Cocktail에 사용될 Provider로서 Java Client 검토

- Github : https://github.com/kubernetes/community/blob/master/contributors/devel/client-libraries.md
- Github : https://github.com/fabric8io/kubernetes-client
- 참고 : http://fabric8.io/guide/index.html

---

### AWS 구성 검토 (1 Master - 5 Nodes)

[Trello 카드](https://trello.com/c/bZiGWW5o/69-s0107-kubernetes-aws) 의 Attachment (Kubernetes Cluster 구성방법.md) 참고

- [Deployed Sample App (Sock-Shop)](http://52.79.186.103:30001/)
- [Kubernetes WEB UI (Dashboard)](http://52.79.186.103:32050/)

---

### Service에 고정 IP 지정이 가능한지 여부 : **[가능]**

> **서비스 생성 요청 Command 에서 직접 지정 또는 FILE (Yaml 포맷) 을 통해서 `spec.clusterIP` 필드에 지정**이 가능합니다.

단, Master의 API Server에 플래그로 지정된 `service-cluster-ip-range` CIDR (Classes Inter-Domain Routing) 범위내의 유효한 주소여야 합니다. 유효하지 않은 경우는 API Server에서 `422 Http Status` 코드를 반환합니다.

참고 : [Kubernetes - Services][https://kubernetes.io/docs/user-guide/services/]

---

### 구성된 Cluster에 Node In/Out에 대한 확인 : **[Cloud 가 아닌 경우는 수동으로 가능]**

> Node 추가인 경우는 **`Kubelet`, `Kube-proxy` 등의 설치 작업 후 환경 설정을 통해서 Master Server에 등록**이 되면 사용이 가능해 진다.
>
> Node 제거의 경우는 **`--pod-eviction-timeout (기본값 5min)` 에 설정된 시간이상 연결이 되지 않는 Node는 Master 의 Node-Controller가 해당 Node에 연결된 Pods 들을 모두 제거하고 다른 Node에 Pods 들이 동작할 수 있도록 Replica-Controller를 조정**한다.

참고로 GCE (Google Compute Engine), GKE (Google Container Engine)에서는 Node Group Management를 통해서 Cluster Autoscaling이 지원된다.

참고 : [Kubernetes Nodes](https://kubernetes.io/docs/admin/node/), [Kubernetes Cluster Management](https://kubernetes.io/docs/admin/cluster-management/)

---

### L4와 Sticky Session 처리 방법 확인 : **[설정방식으로 가능]**

각 Node에 기본 제공되는 **kube-proxy는 L3, Round-robin 이 기본적인 설정 (Proxy-mode : userspace)**입니다.

![Services overview diagram for userspace proxy](https://kubernetes.io/images/docs/services-userspace-overview.svg)

Sticky Session 처리는 **Proxy-mode를  iptables 방식으로 전환하면 최초 연결이 성립한 Client IP 기준으로 동일한 연결을 유지**할 수 있도록 전환할 수 있습니다.

![Services overview diagram for iptables proxy](https://kubernetes.io/images/docs/services-iptables-overview.svg)

> **서비스 생성 요청 Command에서 직접 지정 또는 FILE (Yaml 포맷)을 통해서 `service.spec.sessionAffinity` 설정을 `"ClientIP" (기본값 "None")` 으로 변경**하면 적용됩니다.

**단, 이 경우에 연결이 성립된 Node 또는 Pod 가 종료되어 서비스가 불가능한 상태라면 자동으로 다른 것으로 재 시도를 하지 않고 오류로 처리되기 때문에 추가적인 작업이 필요합니다.**

참고 : [Kubernetes - Services][https://kubernetes.io/docs/user-guide/services/]

> **L4 적용은 kube-proxy 만으로는 적용이 어렵고 `Ingress (Beta 버전)` 설정을 추가**하면 L4 와 같은 형식의 지원이 가능하다고 문서 상에 명시되어 있으며, 향후에는 L4 와 L7 Combind를 지원한다고 합니다.

참고 : [Kubernetes - Ingress](https://kubernetes.io/docs/user-guide/ingress/)

---

### Cluster 구성 Node 상태 검증 방법 : **(Kubectl Command 로 가능)**

> Cluster를 구성하고 있는 Node의 리스트는 다음과 같은 명령으로 확인이 가능하다.

```sh
# list of all nodes
$ kubectl get nodes
# external IPs of all nodes using jsonpath
$ kubectl get nodes -o jsonpath='{.items[*].status.addresses[?(@.type=ExternalIP)].address}'
# check with nodes are ready using jsonpath
$ kubectl get nodes -o jsonpath='{range .items[*]}{@.metadata.name}:{range @.status.conditions[*]}{@.type}={@.status};{end}{end}'| tr ';' "\n"  | grep "Ready=True"
# mark specific node as unschedulable
$ kubectl cordon mynode
# drain specific node in preparation for maintenance
$ kubectl drain mynode
# mark specific node as schedulable
$ kubectl uncordon mynode
# show metric for a specific node
$ kubectl top node mynode
# show detail infomation for a specific node
$ kubectl describe node mynode
...
```

상기와 같이 Kubectl 을 통해서 Node 와 관련된 정보를 `JsonPath` 표현식을 통해서 출력할 수 있다. Node에 대한 Monitoring은 아래에 설명하는 Monitoring 방식을 따른다.

참고: [Kubectl Overview](https://kubernetes.io/docs/user-guide/kubectl-overview/), [Kubectl get command](https://kubernetes.io/docs/user-guide/kubectl/kubectl_get/)

---

### Pods/Container Monitoring 인 cAdvisor의 대상 데이터 확인

> 각 Node에서 동작하고 있는 Pods/Containers 에 대한 Monitoring은 cAdvisor에 의해서 수집되며, 수집된 데이터는 InfluxDB와 Grafana를 통해서 보여질 수 있으며, 기본적으로는 Heapster 플랫폼으로 모니터링 정보와 이벤트 관련 정보를 취합해서 레이블들을 기준으로 파드들의 정보를 그룹화해서 처리할 수 있다.

전체적인 Monitoring Architecture는 다음과 같다.

![overall monitoring architecture](https://kubernetes.io/images/docs/monitoring-architecture.png)

cAdvisor를 통해서 수집되는 정보는 다음과 같다.

- CPU Usage
- Memory Usage
- File System Usage
- Network Usage
- Overall Usage of Node???

참고 :

- Kubernetes Monitoring Platform [Heapster](https://github.com/kubernetes/heapster) - Pod로 동작하며 Storage, Visualizer 구성 필요.
- Kubelet with [cAdvisor](https://github.com/google/cadvisor) - 각 Node에 존재하여 Container 사용량 정보 수집 및 Pod 단위 통계 처리
- Storage Backend [InfluxDB](http://influxdb.com/) (with [Grafana](http://grafana.org/) for visualization) 또는 [Google Cloud Monitoring](https://cloud.google.com/monitoring/)

---
> Written by Morris (<ccambo@acornsoft.io>).
