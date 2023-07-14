# 파이프라인 API 구현  


## 1. 서버 생성
 * 기존 방식 ![server add v1](https://www.websequencediagrams.com/cgi-bin/cdraw?lz=dGl0bGUgU2VydmVyIEFkZCBTZXF1ZW5jZSh2MSkKCmNsaWVudC0-QVBJKGNvbnRyb2xsZXIpOgAqCGFkZCByZXF1ZXN0CgoAFg8tPlJlc291cmNlU2VydmljZTogYWRkAGUGCgoADQ8tPkRlcGxveQAfDAANBXB5bWVudAAdE0pvYgBPCXJ1bgA5BkpvYgoKABAKABURAAEfbk5leHRUYXNrADcOVGFza1J1bgBjDAAgBW5vdGUgcmlnaHQgb2YgAIEFDGFzeW5jCgAqDgCBUhFydW4KCgCBag0AgXURY3ViZQCCEwYAGREAgjAtS3ViZVdvcmtlcjogY3JlYXRlIGs4cyBvYmplY3RzCgCBGREAgX4Z&s=default)  
 * 변경할 방식 ![server add v2](https://www.websequencediagrams.com/cgi-bin/cdraw?lz=dGl0bGUgU2VydmVyIEFkZCBTZXF1ZW5jZSh2MikKCmNsaWVudC0-QVBJKGNvbnRyb2xsZXIpOgAqCGFkZCByZXF1ZXN0CgoAFg8tPlJlc291cmNlU2VydmljZTogYWRkAGUGMgoKbm90ZSByaWdodCBvZiAAUBFhc3luYwBBEgCBCwY6IHJlc3BvbnNlCgoAVg8tPkRlcGxveQBoDAANBXB5bWVudAAdEwCBDBoAShNLdWJlV3Jva2VyOiBjcmVhdGUgazhzIG9iamVjdHMAbCJ1cGRhdGUAgR0GbWVudFN0YXRlCg&s=default)  
 * 주요 변경 내용
   * 변경하는 방식은 task run을 통해 실행하지는 않지만 deployment테이블은 사용한다. 이 테이블에 항목을 추가하기 위해서는 job, task의 sequence가 필요하기 때문에 각 항목은 기존 방식을 통해 추가한다.

## 2. 서버 수정
 * 기존 방식 ![server update v1](https://www.websequencediagrams.com/cgi-bin/cdraw?lz=dGl0bGUgU2VydmVyIFVwZGF0ZSBTZXF1ZW5jZSh2MSkKCkNsaWVudC0-QVBJKGNvbnRyb2xsZXIpOgAtCHUALwZyZXF1ZXN0KGRlcGxveSkKCgAhDy0-IFJlc291cmNlU2VydmljZTogACYGCgoACg8tPkQAQAUAHwkAXQYADwZtZW50AB8TSm9iAE4JcnVuADsGSm9iCgoAEAoAFREAAR9uTmV4dFRhc2sANw5UYXNrUnVuAGMMAB8Gbm90ZSByaWdodCBvZiAAgQYMYXN5bmMKACsOAIFVEXJ1bgoKAIFtDQCBeBFjdWIAggAHABkRAIIzKkt1YmVXb3JrZXIAglIIIGs4cyBvYmplY3RzCgo&s=default)  
 * 변경할 방식 ![server update v2](https://www.websequencediagrams.com/cgi-bin/cdraw?lz=dGl0bGUgU2VydmVyIFVwZGF0ZSBTZXF1ZW5jZSh2MikKCkNsaWVudC0-QVBJKGNvbnRyb2xsZXIpOgAtCHUALwZyZXF1ZXN0KGRlcGxveSkKCgAhDy0-IFJlc291cmNlU2VydmljZTogACYGCgoACg8tPkQAQAUAHwkAXQYADwZtZW50CgoAFA0tPgAwKgBaKkt1YmVXb3JrZXIAeQggazhzIG9iamVjdHMAgQMyU3RhZQo&s=default)
 * 주요 변경 내용
   * 서버 생성의 변경 방식과 같다.

## 3. 파이프라인
* ERD
 ![pipeline_erd.pdf](http://git.acornsoft.io/cocktail/cocktail-java/blob/feature/pipeline/docs/design/pipeline_erd.pdf)
* SD
 ![all execute pipeline](http://git.acornsoft.io/cocktail/cocktail-java/blob/feature/pipeline/docs/design/SD_pipeline_all_exec_20171110.png)
* 구현
 * 생성
 * 수정
 * 삭제
 * 목록
 * 개별 실행
 * 전체 실행
* 기타
 * 서버생성 시 빌드 이미지 목록 조회
 * 파이프라인 목록 조회시 빌드 최신 이미지 조회