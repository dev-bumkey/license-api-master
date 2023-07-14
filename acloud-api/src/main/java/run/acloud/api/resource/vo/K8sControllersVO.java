package run.acloud.api.resource.vo;

import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.util.List;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@Schema(description = "전체 워크로드(컨트롤러) 모델")
public class K8sControllersVO extends BaseVO {
    @Schema(title = "Deployment 목록")
    private List<K8sDeploymentVO> deployments;

    @Schema(title = "ReplicaSet 목록")
    private List<K8sReplicaSetVO> replicaSets;

    @Schema(title = "DaemonSet 목록")
    private List<K8sDaemonSetVO> daemonSets;

    @Schema(title = "StatefulSet 목록")
    private List<K8sStatefulSetVO> statefulSets;

    @Schema(title = "CronJob 목록")
    private List<K8sCronJobVO> cronJobs;

    @Schema(title = "Job 목록")
    private List<K8sJobVO> jobs;

    public K8sControllersVO addDeploymentsItem(K8sDeploymentVO item) {
        if (this.deployments == null) {
            this.deployments = Lists.newArrayList();
        }
        this.deployments.add(item);
        return this;
    }

    public K8sControllersVO addAllDeploymentsItem(List<K8sDeploymentVO> items) {
        if (this.deployments == null) {
            this.deployments = Lists.newArrayList();
        }
        this.deployments.addAll(items);
        return this;
    }

    public K8sControllersVO addReplicaSetsItem(K8sReplicaSetVO item) {
        if (this.replicaSets == null) {
            this.replicaSets = Lists.newArrayList();
        }
        this.replicaSets.add(item);
        return this;
    }

    public K8sControllersVO addAllReplicaSetsItem(List<K8sReplicaSetVO> items) {
        if (this.replicaSets == null) {
            this.replicaSets = Lists.newArrayList();
        }
        this.replicaSets.addAll(items);
        return this;
    }

    public K8sControllersVO addDaemonSetsItem(K8sDaemonSetVO item) {
        if (this.daemonSets == null) {
            this.daemonSets = Lists.newArrayList();
        }
        this.daemonSets.add(item);
        return this;
    }

    public K8sControllersVO addAllDaemonSetsItem(List<K8sDaemonSetVO> items) {
        if (this.daemonSets == null) {
            this.daemonSets = Lists.newArrayList();
        }
        this.daemonSets.addAll(items);
        return this;
    }

    public K8sControllersVO addStatefulSetsItem(K8sStatefulSetVO item) {
        if (this.statefulSets == null) {
            this.statefulSets = Lists.newArrayList();
        }
        this.statefulSets.add(item);
        return this;
    }

    public K8sControllersVO addAllStatefulSetsItem(List<K8sStatefulSetVO> items) {
        if (this.statefulSets == null) {
            this.statefulSets = Lists.newArrayList();
        }
        this.statefulSets.addAll(items);
        return this;
    }

    public K8sControllersVO addCronJobsItem(K8sCronJobVO item) {
        if (this.cronJobs == null) {
            this.cronJobs = Lists.newArrayList();
        }
        this.cronJobs.add(item);
        return this;
    }

    public K8sControllersVO addAllCronJobsItem(List<K8sCronJobVO> items) {
        if (this.cronJobs == null) {
            this.cronJobs = Lists.newArrayList();
        }
        this.cronJobs.addAll(items);
        return this;
    }

    public K8sControllersVO addJobsItem(K8sJobVO item) {
        if (this.jobs == null) {
            this.jobs = Lists.newArrayList();
        }
        this.jobs.add(item);
        return this;
    }

    public K8sControllersVO addAllJobsItem(List<K8sJobVO> items) {
        if (this.jobs == null) {
            this.jobs = Lists.newArrayList();
        }
        this.jobs.addAll(items);
        return this;
    }
}