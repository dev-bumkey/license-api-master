# Harbor 설치 및 설정
[Harbor](https://github.com/vmware/harbor)는 VMWare가 개발한 Open Docker container registry 서버로서 아래의 기능을 지원한다.

## 주요 기능
1. Role base access control - 프로젝트와 사용자에 따라 이미지에 대한 권한을 다르게 적용할 수 있다
2. Policy base image replication
3. LDAP/AP 지원
4. 이미지 삭제
5. GUI 환경
6. Audit - logging
7. REST API
8. Kubernetes에도 설치 가능


## 설치
* 1.1.0 기준 
* 설치 과정은 [Installation and Configuration Guide](https://github.com/vmware/harbor/blob/master/docs/installation_guide.md#installation-and-configuration-guide)를 따름.
* host: ubunt16.06 xenial

### 1. Host prerequisites  

*  python 2.7 이상
*  docker 1.10 이상을 지원
*  docker compose 1.6 이상

### 2. Install Docker
1. ```sudo apt-get update```
2. ```sudo apt-get install curl linux-image-extra-$(uname -r) linux-image-extra-virtual```
3. ```sudo apt-get install apt-transport-https ca-certificates```
4. ```curl -s http://yum.dockerproject.org/gpg | sudo apt-key add```
5. ```apt-key fingerprint 58118E89F3A912897C070ADBF76221572C52609D```
6. ```sudo add-apt-repository "deb https://apt.dockerproject.org/repo/pool/ $(lsb_release -cs) main"```
7. ```sudo apt-get update```
8. ```sudo apt-get -y install docker-engine```
 - E: Unable to locate package docker-engine 오류가 나는 경우
 - /etc/apt/sources.list 파일에서 *deb https://apt.dockerproject.org/repo/pool/ xenial main*을 주석 처리하고 **deb https://apt.dockerproject.org/repo/ ubuntu-xenial main**을 추가한다.
 - sudo apt-get update 실행
 - 참조: https://github.com/docker/docker/issues/22182   

* docker without sudo
 - sudo gpasswd -a ${USER} docker
 - sudo service docker restart
  
### 3. Install Docker Compose   
 ```
 curl -L https://github.com/docker/compose/releases/download/1.10.0/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose
 ```
* version은 docker compose github에 가서 확인
* 위 명령은 'bin' 디렉토리에 대한 쓰기 권한이 없으면 오류를 냄으로 아래와 같이 한다.

```
 curl -L https://github.com/docker/compose/releases/download/1.10.0/docker-compose-`uname -s`-`uname -m` > docker-compose     
 chmod +x docker-compose   
 sudo mv docker-compose /usr/local/bin/
```

### 4. Download online installer  
  
```
 wget https://github.com/vmware/harbor/releases/download/v1.1.1-rc4/harbor-online-installer-v1.1.1-rc4.tgz
```  
 
### 5. Configuration
 - install의 압축을 풀면 **harbor**라는 디렉토리가 생성됨.
 - 이 디렉토리 밑의 **harbor.cfg** 파일을 수정한다. 자세한 내용은 위에 링크된 문서 참조
 - **harbo/common/templates/registry/config.yml** 파일에는 image를 저장할 위치를 지정할 수 있다. 이 설치에는 추가 볼륨을 붙여 사용하였으며 위치는 */mnt/image_storage*이다. -> **제대로 동작하지 않아 사용하지 않음**

### 6. Install  

* install.sh 실행(root 권한)
* 아래와 같은 오류가 발생하여 처리함
 * python not found: python3.x만 설치되어 발생. python2.x를 설치하든가, symbolic link를 python3.x에 걸어 해결
 *  python3를 사용하면 install.sh가 실행한 prepare 파일의 50라인(현재 설치버전)에서 'SyntaxError: invalid token' 오류가 난다. 'os.makedirs(path, mode=0600)' 인데, python3는 mode를 정확한 8진수 표기로 받는다. 0660을 0o600으로 수정


### 7. Security
#### Insecure access
 - ubuntu16.06의 경우 /etc/docker 밑에 daemon.json 파일을 만들고, **insecur-registries** 항목에 서버의 ip 주소를 넣는다.
 - daemon.json에 대한 자세한 내용은 <https://docs.docker.com/engine/reference/commandline/dockerd/> 참조

## 관리
* 중지(harbor 디렉토리 이동 후)   
```sudo docker-compose stop```
* 재기동(harbor 디렉토리 이동 후)  
```sudo docker-compose start```
* 구성 변경
 1. 중지
 2. 구성 변경
 3. ```sudo install.sh ```
* 시스템 재기동에 따른 실행
 * docker-compose 1.10.0에서 yml에는 harbor가 기동하는 6개의 container가 모두 restart: always라고 설정되어 있지만, 실제 시스템을 재기동하면 그 중 2개(log, nginx)만 기동된다. 이에 대한 오류 로그는 특별히 없으며 google로 검색해보면 관련한 질문이 있지만 명쾌한 해답은 없다.
 * 이에 yml의 restart값을 없애고(default: no) /etc/rc.local을 통해 harbor의 docker-compose.yml을 실행하여 다시 기동시킴.