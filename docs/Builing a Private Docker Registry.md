# Docker Registry 구축 방안  

## Registry Server
VMWare의 [Harbor](https://github.com/vmware/harbor)를 사용한다.
설치는 [Harbor 설치 및 설정](Harbor.md) 참조
## Harbor 기능에 따른 고려 사항
1. HTTPS
 * 내부 망에서만 접근한다면 굳이 HTTPS를 사용할 이유는 없다.
 * 인증서 설치 및 인증에 대한 세부 매뉴얼 구축이 필요하다.
2. User
 * Role(*admin, developer, guest*)에 따라 권한이 다르다. **admin은 설치 시 자동 생성되며 암호를 설치 설정 파일에 기록하므로 칵테일의 설치 과정에서 통제가 쉬울 것으로 판단**
 * 규모가 큰 조직에서는 부서나 서비스 별로 이미지에 대한 접근을 통제하기를 바랄 수 있다. 이에 대응하기 위한 사용자 관리 API가 구현되어 있음

3. Project
 * Harbor는 어떤 조직 또는 서비스에 관련된 이미지를 그룹으로 관리하고 접근을 제어할 수 있도록 *Project*라는 개념을 갖고 있으며 모든 이미지는 하나의 프로젝트에 속하게 된다.
 * Project는 이미지의 관점에서는 _repository name_을 의미한다. 예를 들어, cocktail-dev라는 프로젝트에 저장된 nginx 이미지의 repository name은 /cocktail-dev/nginx이다.
 * 칵테일 설치 과정을 통해 새 project를 만들거나, 기본으로 설치된 project인 _library_를 이용한다. **기본 설치된 project를 활용하는 것을 추천**.
 * User와 마찬가지로 Project를 관리하기 위한 API는 제공된다.

4. Image List/Search
 * Project 별 이미지 목록, 태그, 검색 기능은 API를 통해 제공된다.

## Harbor 설치
1. 독립된 인스턴스에 설치한다면 harbor online installer에 다음에 대한 수정을 고려해야 한다.
 * harbor admin password - 설치 중 사용자로부터 입력을 받을 것인지, 아니면 단순히 기존값을 수정할 것인지 결정해야 한다.
 * mysql root password - 위와 같다.
 * docker-compose.yml에서 restart option 제거 - default 값(no) 사용
 * rc.local에 docker compose 실행 추가