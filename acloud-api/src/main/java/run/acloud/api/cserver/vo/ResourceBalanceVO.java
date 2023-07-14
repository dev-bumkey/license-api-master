package run.acloud.api.cserver.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 11. 16.
 */
@Getter
@Setter
public class ResourceBalanceVO {
    private Double totalCpu; // 전체 cpu 잔량

    private Double totalMemory; // 전체 memory 잔량

    private Integer totalPod; // 전체 pod 잔량

    private List<Double> individualCpu; // 각 노드의 cpu 잔량

    private List<Double> individualMemory; // 각 노드의 memory 잔량

    //private List<Integer> individualPod; // 각 노드에 적용할 수 있는 pod 잔량

    private List<Integer> thisPodCount; // 각 노드에 있는 depoly하려는 서버의 pod 갯수. 생성할 때 또는 서버 이름을 알려주지 않으면 null이거나 모두 0.
}
