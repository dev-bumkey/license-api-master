package run.acloud.api.resource.enums;

import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

public enum KubeResourceTypes {
	None,
    Node,
    Namespace,
    Service,
    Deployment,
    ReplicaSet,
    Pod,
    EndPoint,
    Event,
    Ingress,
    PersistentVolume,
    PersistentVolumeClaim,
    Secret,
    StorageClass,
    ConfigMap,
    HeadlessService,
    StatefulSet,
    HorizontalPodAutoscaler,
    ;

	public static KubeResourceTypes codeOf(String value, Boolean throwException) throws Exception {
	    value = value.toLowerCase();
	    switch (value) {
            case "node":
                return Node;
            case "namespace":
                return Namespace;
            case "service":
                return Service;
            case "deployment":
                return Deployment;
            case "replicaset":
                return ReplicaSet;
            case "pod":
                return Pod;
            case "endpoint":
                return EndPoint;
            case "event":
                return Event;
            case "ingress":
                return Ingress;
            case "persistentvolume":
                return PersistentVolume;
            case "persistentvolumeclaim":
                return PersistentVolumeClaim;
            case "secret":
                return Secret;
            case "strageclass":
                return StorageClass;
            case "configmap":
                return ConfigMap;
            default:
                if (throwException) {
                    throw new CocktailException(String.format("Unknown resource type: %s", value),
                            ExceptionType.CubeResourceTypeUnknown);
                } else {
                    return None;
                }
        }
    }
}
