package run.acloud.api.catalog.enums;

public enum LaunchType {
    NEW("N"),  // 신규 appmap을 생성
    ADD("A");  // 기존 appmap에 추가

    private String type;

    public String getType(){
        return this.type;
    }

    LaunchType(String type) {
        this.type = type;
    }
}
