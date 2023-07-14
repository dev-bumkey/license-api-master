package run.acloud.api.resource.enums;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 11. 15.
 */
public enum ResourceState {
    Normal, // 정상
    TotalPod, // 생성할 수 있는 전제 pod 수를 넘김
    MaxCpu, // 생성 요청 TotalPod 중 최대 CPU 사용 요청을 충족할 수 없음
    TotalCpu, // 생성 요청 전체 Pod의 CPU 사용 요청을 충족할 수 없음
    MaxMemory, // 생성 요청 TotalPod 중 최대 Memory 사용 요청을 충족할 수 없음
    TotalMemory, // 생성 요청 전체 Pod의 Memory 사용 요청을 충족할 수 없음
    Volume //
}
