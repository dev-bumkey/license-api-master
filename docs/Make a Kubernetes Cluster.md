# AWS STG 에 Kuberntes 설치를 위한 환경입니다.

구성되어 있는 전체 Cluster 구성 Node의 OS는 `ubuntu 16.04.1 LTS` 입니다.

> **Notes**
>
>LS 산전의 POC 결과와 향후 대응을 위해서 OS 가 CentOS 로 변경될 수 있습니다.

## Cluster Instance Sizing

> **Nodes**
>
> AWS에 구성되는 Kubernetes Cluster의 Instance Sizing 은 [`Running Kubernetes on AWS EC2`](https://kubernetes.io/docs/getting-started-guides/aws/) 문서를 참고했습니다.
> - For the master, for clusters of less than 5 nodes it will use an m3.medium, for 6-10 nodes it will use an m3.large; for 11-100 nodes it will use an m3.xlarge.
> - For worker nodes, for clusters less than 50 nodes it will use a t2.micro, for clusters between 50 and 150 nodes it will use a t2.small and for clusters with greater than 150 nodes it will use a t2.medium.
>
> 상기의 기준에서 현재 사용할 수 있는 Instance Type을 기준으로 Master Node는 `M4-Large`, Slave Node는 `T2-Medium` 로 구성했습니다.

## AWS Basic (Cocktail Cluster의 Kubernetes-AWS 대응)

| Type     | Instance  | Public IP          | Private IP |
| -------- | --------- | ------------------ | ---------- |
| Master   | M4-Large  | 13.124.31.81 (EIP) | 10.2.0.128 |
| Slave #1 | T2-Medium | 13.124.19.160      | 10.2.3.117 |
| Slave #2 | T2-Medium | 13.124.23.229      | 10.2.5.98  |
| Slave #3 | T2-Medium | 13.124.45.198      | 10.2.6.49  |
| Slave #4 | T2-Medium | 13.124.7.78        | 10.2.7.90  |

**Kubernetes Cluster 운영을 위한 기본 정보**

| Type           | Value                                  | Description                                                      |
| -------------- | -------------------------------------- | ---------------------------------------------------------------- |
| Bearer Token   | 5fcddc13c9193dd1                       | API Server 연결을 위한 Token, Cocktail Provider_Account에 설정   | 
| Browser Header | Authorization: Bearer 5fcddc13c9193dd1 | Browser Header에 'Authorization: Bearer 5fcddc13c9193dd1' 설정   |
| API Server     | https://13.124.31.81:6443              | Cocktail Provider_Account에 설정                                 |
| Dashboard      | http://13.124.31.81:31447              | Kubernetes 검증을 위한 Dashboard                                 |
| InfluxDB       | http://13.124.31.81:30315              | Monitoring 정보 DB 접속을 위해, API Server Application.yaml 설정 |

## AWS BareMetal (Cocktail Cluster의 Kubernetes-DataCenter 대응)

| Type     | Instance  | Public IP          | Private IP   |
| -------- | --------- | ------------------ | ------------ |
| Master   | M4-Large  | 13.124.11.58 (EIP) | 10.2.12.139  |
| Slave #1 | T2-Medium | 13.124.24.26       | 10.2.12.160  |
| Slave #2 | T2-Medium | 13.124.54.129      | 10.2.2.37    |
| Slave #3 | T2-Medium | 13.124.58.8        | 10.2.3.28    |
| Slave #4 | T2-Medium | 13.124.27.11       | 10.2.8.5     |

**Kubernetes Cluster 운영을 위한 기본 정보**

| Type           | Value                                  | Description                                                      |
| -------------- | -------------------------------------- | ---------------------------------------------------------------- |
| Bearer Token   | 8679e4553a1e7426                       | API Server 연결을 위한 Token, Cocktail Provider_Account에 설정   | 
| Browser Header | Authorization: Bearer 8679e4553a1e7426 | Browser Header에 'Authorization: Bearer 5fcddc13c9193dd1' 설정   |
| API Server     | https://13.124.11.58:6443              | Cocktail Provider_Account에 설정                                 |
| Dashboard      | http://13.124.11.58:31447              | Kubernetes 검증을 위한 Dashboard                                 |
| InfluxDB       | http://13.124.11.58:30315              | Monitoring 정보 DB 접속을 위해, API Server Application.yaml 설정 |

## Kubernetes Cluster 설치 방법 (모든 Node 적용)

`아래의 작업은 항상 Root 권한을 기준으로 진행합니다. (sudo su)`

### 모든 Node에 적용

0. 사전 준비 작업 (재 설치)

신규 설치가 아니라면 아래와 같이 작업을 진행한다.

```sh
> ifconfig
```

위의 명령을 실행해서 아래와 같이 각 인터페이스의 네트워크 대역을 확인한다.
- docker0, 172.x.x.x (docker 설치 전이면 없을 수 있음)
- eth0 10.x.x.x (VPC 네트워트 대역, 이름은 다를 수 있음)
- lo 127.x.x.x

> 만일 위의 인터페이스 이외의 `cni0` 나 `flannel.1` 등의 인터페이스가 존재한다면 기존에 설치에 대한 정보가 존재하는 것이므로 `kubeadm reset` 을 실행하고 `docker` 의 모든 프로세스를 종료한 후에 `재 부팅`을 하여 정보가 초기화 되었는지를 확인하도록 한다.

1. Apt Key 생성 (신규 설치)

   ```sh
   curl http://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -
   ```

2. Kubernetes Source Repository 생성 (신규 설치)

   ```sh
   cat <<EOF > /etc/apt/sources.list.d/kubernetes.list
   deb http://apt.kubernetes.io/ kubernetes-xenial main
   EOF
   ```

3. Apt Repository 갱신 (신규 설치)

   ```sh
   apt-get update
   ```

4. 필수 프로그램 설치 (신규 설치)

   ```sh
   apt-get install -y docker.io
   apt-get install -y kubelet kubeadm kubectl kubernetes-cni
   ```

5. Docker Private Registry 관련 설정 (신규 설치)

   ```sh
   cd /etc/docker
   curl http://cocktail-cloud.acornsoft.io/api/kube/manifests/daemon > daemon.json
   cd /home/ubuntu
   ```

상기와 같이 Private Registery를 설정한 후에 Docker Daemon을 재 구동해 주어야 한다.

```sh
systemctl restart docker
```

### Master Node에 Cluster Intializing (신규 및 재 설치)

```sh
# AWS 인 경우
kubeadm init --token=b00af9.5fcddc13c9193dd1 --api-advertise-addresses=13.124.31.81 --pod-network-cidr=10.44.0.0/16
# DataCenter 인 경우
kubeadm init --token=07c414.8679e4553a1e7426 --api-advertise-addresses=13.124.11.58 --pod-network-cidr=10.45.0.0/16
```

위의 작업을 수행하는데 시간이 좀 소비된다 (대략 1분). 더 많은 시간이 걸리거나 끝나지 않는 상태라면 AWS의 Network 설정 등에 문제가 있는 경우이므로 관련된 AWS 설정을 AWS Console에서 확인해 보고 다시 작업하도록 한다.

> **Notes**
>
> 향후 운영을 위해서는 아래와 같은 Port 가 방화벽에 Open이 되어 있어야 한다.
> - API 연결을 위한 **`TCP 9898`**
> - API SSL 연결을 위한 **`TCP 6443`**
> - NodePort 방식의 외부 연결을 위한 **`TCP 30000 - 32767`**
> - Flannel 연결을 위한 **`UDP 8285, 8472`**
> 
> 상기 Port 이외에도 운영에 필요한 Port 가 더 존재할 수 있으므로 외부에 노출되지 않거나 운영에 문제가 있는 경우라면 Port 정보를 좀 더 검증해 봐야 한다.

### Master Node에 Network (Flannel) 구성 (신규 및 재 설치)

아래의 명령을 이용해서 Flannel을 설치하도록 한다.

```sh
# AWS인 경우
kubectl apply -f http://cocktail-cloud.acornsoft.io/api/kube/manifests/flannel?network=10.44.0.0/16
# DataCenter인 경우
kubectl apply -f http://cocktail-cloud.acornsoft.io/api/kube/manifests/flannel?network=10.45.0.0/16
```

설치 여부는 아래의 명령을 통해서 확인할 수 있다.

```sh
kubectl -n kube-system get pods
```

결과로 나오는 모든 Pod 들이 `Running` 상태로 되어 있어야 한다. `Pending` 또는 `ContainerCreating` 상태로 나오는 경우라면 좀 더 시간을 두고 모두 Running 상태로 변경되는지 확인해야 한다.

모든 Pod의 상태가 `Running` 인 경우라면 아래의 명령을 통해서 Flannel 관련 네트워크 인터페이스들이 정상적으로 설정되었는지를 확인해야 한다.

```sh
ifconfig
```

리스트로 나온 네트워크 인터페이스 중에서 아래와 같은 인터페이스가 존재하는지 IP 대역이 제대로 설정되었는지를 확인해야 한다.

- cni0 : 10.44.x.x (AWS인 경우), 10.45.x.x (DataCenter인 경우)
- flannel.1 : 10.44.x.x (AWS인 경우), 10.45.x.x (DataCenter인 경우)

만일 위와 같은 네트워크 인터페이스가 보이지 않는다면 아래 명령을 통해서 Kubernetes 환경을 다시 실행해야 한다.

```sh
systemctl restart kubelet
```

위의 명령을 통해서 재 시작을 진행했지만 네트워크 인터페이스가 없거나 다른 IP 대역을 가지고 있다면 설치 상의 문제가 발생한 것이므로 초기화 (가장 아래쪽 초기화 부분 참고)를 실행해서 다시 설치를 진행하거나 오류가 있다면 문제를 해결해 보도록 한다.

### 모든 Slave Node에 Master Node 연결 작업 처리

Master Node 가 정상적으로 초기화가 진행되었다면 Cluster 구성에 사용할 모든 Slave Node 들에 아래의 명령을 수행하도록 한다.

```sh
# AWS 인 경우
kubeadm join --token=b00af9.5fcddc13c9193dd1 13.124.31.81
# DataCenter 인 경우
kubeadm join --token=07c414.8679e4553a1e7426 13.124.11.58
```

상기의 작업이 완료되면 Master Node에 각 Slave Node가 연결되게 된다. 그러나 아직 Flannel 정보 처리가 완료되지 않았을 수 있기 때문에 아래의 명령을 통해서 Flannel 처리 여부를 확인해야 한다.

```sh
ifconfig
```

결과 리스트에 `flannel.1` 의 네트워크 인터페이스가 존재하지 않는다면 아래 명령을 실행한 후에 다시 네트워크 인터페이스를 확인해 보도록 한다.

> **Notes**
>
> Flannel 구성에 시간이 필요하기 때문에 1분 정도 후에 다시 확인해 보도록 한다.

```sh
systemctl restart kubelet
```

### Master Node에서 Slave Node 연결 여부 확인

```sh
kubectl get nodes
```

### Master Node에 Kubernetes Web UI 설치

Kubernetes 가 설치되었을 때 기본적으로 제공되지 않는다. 따라서 아래의 명령을 통해서 추가로 설치를 해야 한다.

```sh
kubectl create -f http://cocktail-cloud.acornsoft.io/api/kube/manifests/dashboard
```

이제 Kubernetes Dashboard UI 가 제대로 설정이 되었는지를 아래의 URL로 확인해 보도록 한다.

- AWS : http://13.124.31.81:31447
- DataCenter : http://13.124.11.58:31447

### Master Node에 Monitoring 설치

Kubernetes에서는 `Kubectl에 바이너리로 통합된 cAdvisor를 통해서 Pod 와 내부의 Container에 대한 모니터링 정보 (Cpu, Memory, Disk, Network, ...)가 취합`된다. 또한 `Heapster 가 기본 Monitoring Platform으로 Kubernetes에 제공`되고 있기 때문에 이를 설치해서 운영하는 설정을 추가로 해 주어야 한다.

```sh
kubectl create -f http://cocktail-cloud.acornsoft.io/api/kube/manifests/monitoring
```

InfluxDBv1.1.1 버전에 따른 데이터 처리 문제가 존재하기 때문에 설정을 추가로 해야 한다.

```sh
kubectl run influxcli --restart=Never --rm -it --image=cburki/influxdb-shell -- \
  --host=monitoring-influxdb.kube-system --port=8086 --database=k8s \
  --execute 'CREATE RETENTION POLICY "default" ON k8s DURATION 0d REPLICATION 1 DEFAULT; SHOW RETENTION POLICIES'
```

위와 같이 처리하면 해당 오류를 해결할 수 있다. 단, 일회성 처리이므로 Influxcli Container는 처리 후에 종료된다. 만일 중간에 오류가 발생하면 성공할 때까지 재 실행을 하면 된다. 

> **Notes**
>
> 단, 위와 같이 재 실행을 계속해도 문제가 해결되지 않는 경우라면 `Dashbaord 화면에서 Pod` 에서 `influxdb`와 `heapster`를 삭제하고 다시 시도 하도록 한다.

### Kubernetes Cluster 초기화

`아래의 작업은 Master Node 와 Slave Node 에서 모두 수행하여야 합니다.`

```sh
kubeadm reset
rm -rf .kube/

systemctl stop kubelet
systemctl stop docker
rm -rf /var/lib/cni/
rm -rf /var/lib/kubelet/*
rm -rf /etc/cni/

ifconfig cni0 down
ifconfig flannel.1 down
ifconfig weave down
ifconfig datapath down
ifconfig docker0 down

systemctl start docker
docker rm -v $(docker ps -aq)
docker rmi $(docker images -aq)

systemctl restart docker
systemctl start kubelet
```

위의 작업이 진행된 후에 별다른 문제가 없다면 `재 부팅`을 진행하고 Master 설치 또는 Slave Join 작업을 진행하면 된다.