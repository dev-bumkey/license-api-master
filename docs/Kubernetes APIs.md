# Kubernetes APIs

## API Operations

### get available resources

```
GET /api/v1
```

### list objects of kind ComponentStatus

```
GET /api/v1/componentstatuses
```

### read the specified ComponentStatus

```
GET /api/v1/componentstatuses/{name}
```

### list or watch objects of kind ConfigMap

```
GET /api/v1/configmaps
```

### list or watch objects of kind Endpoints

```
GET /api/v1/endpoints
```

### list or watch objects of kind Event

```
GET /api/v1/events
```

### list or watch objects of kind LimitRange

```
GET /api/v1/limitranges
```

### **list or watch objects of kind Namespace**

```
GET /api/v1/namespaces
```

### delete collection of Namespace

```
DELETE /api/v1/namespaces
```

### **create a Namespace**

```
POST /api/v1/namespaces
```

### create a Binding

```
POST /api/v1/namespaces/{namespace}/bindings
```

### list or watch objects of kind ConfigMap

```
GET /api/v1/namespaces/{namespace}/configmaps
```

### delete collection of ConfigMap

```
DELETE /api/v1/namespaces/{namespace}/configmaps
```

### create a ConfigMap

```
POST /api/v1/namespaces/{namespace}/configmaps
```

### read the specified ConfigMap

```
GET /api/v1/namespaces/{namespace}/configmaps/{name}
```

### replace the specified ConfigMap

```
PUT /api/v1/namespaces/{namespace}/configmaps/{name}
```

### delete a ConfigMap

```
DELETE /api/v1/namespaces/{namespace}/configmaps/{name}
```

### partially update the specified ConfigMap

```
PATCH /api/v1/namespaces/{namespace}/configmaps/{name}
```

### list or watch objects of kind Endpoints

```
GET /api/v1/namespaces/{namespace}/endpoints
```

### delete collection of Endpoints

```
DELETE /api/v1/namespaces/{namespace}/endpoints
```

### create Endpoints

```
POST /api/v1/namespaces/{namespace}/endpoints
```

### read the specified Endpoints

```
GET /api/v1/namespaces/{namespace}/endpoints/{name}
```

### replace the specified Endpoints

```
PUT /api/v1/namespaces/{namespace}/endpoints/{name}
```

### delete Endpoints

```
DELETE /api/v1/namespaces/{namespace}/endpoints/{name}
```

### partially update the specified Endpoints

```
PATCH /api/v1/namespaces/{namespace}/endpoints/{name}
```

### list or watch objects of kind Event

```
GET /api/v1/namespaces/{namespace}/events
```

### delete collection of Event

```
DELETE /api/v1/namespaces/{namespace}/events
```

### create an Event

```
POST /api/v1/namespaces/{namespace}/events
```

### read the specified Event

```
GET /api/v1/namespaces/{namespace}/events/{name}
```

### replace the specified Event

```
PUT /api/v1/namespaces/{namespace}/events/{name}
```

### delete an Event

```
DELETE /api/v1/namespaces/{namespace}/events/{name}
```

### partially update the specified Event

```
PATCH /api/v1/namespaces/{namespace}/events/{name}
```

### list or watch objects of kind LimitRange

```
GET /api/v1/namespaces/{namespace}/limitranges
```

### delete collection of LimitRange

```
DELETE /api/v1/namespaces/{namespace}/limitranges
```

### create a LimitRange

```
POST /api/v1/namespaces/{namespace}/limitranges
```

### read the specified LimitRange

```
GET /api/v1/namespaces/{namespace}/limitranges/{name}
```

### replace the specified LimitRange

```
PUT /api/v1/namespaces/{namespace}/limitranges/{name}
```

### delete a LimitRange

```
DELETE /api/v1/namespaces/{namespace}/limitranges/{name}
```

### partially update the specified LimitRange

```
PATCH /api/v1/namespaces/{namespace}/limitranges/{name}
```

### list or watch objects of kind PersistentVolumeClaim

```
GET /api/v1/namespaces/{namespace}/persistentvolumeclaims
```

### delete collection of PersistentVolumeClaim

```
DELETE /api/v1/namespaces/{namespace}/persistentvolumeclaims
```

### create a PersistentVolumeClaim

```
POST /api/v1/namespaces/{namespace}/persistentvolumeclaims
```

### read the specified PersistentVolumeClaim

```
GET /api/v1/namespaces/{namespace}/persistentvolumeclaims/{name}
```

### replace the specified PersistentVolumeClaim

```
PUT /api/v1/namespaces/{namespace}/persistentvolumeclaims/{name}
```

### delete a PersistentVolumeClaim

```
DELETE /api/v1/namespaces/{namespace}/persistentvolumeclaims/{name}
```

### partially update the specified PersistentVolumeClaim

```
PATCH /api/v1/namespaces/{namespace}/persistentvolumeclaims/{name}
```

### read status of the specified PersistentVolumeClaim

```
GET /api/v1/namespaces/{namespace}/persistentvolumeclaims/{name}/status
```

### replace status of the specified PersistentVolumeClaim

```
PUT /api/v1/namespaces/{namespace}/persistentvolumeclaims/{name}/status
```

### partially update status of the specified PersistentVolumeClaim

```
PATCH /api/v1/namespaces/{namespace}/persistentvolumeclaims/{name}/status
```

### **list or watch objects of kind Pod**

```
GET /api/v1/namespaces/{namespace}/pods
```

### **delete collection of Pod**

```
DELETE /api/v1/namespaces/{namespace}/pods
```

### **create a Pod**

```
POST /api/v1/namespaces/{namespace}/pods
```

### **read the specified Pod**

```
GET /api/v1/namespaces/{namespace}/pods/{name}
```

### **replace the specified Pod**

```
PUT /api/v1/namespaces/{namespace}/pods/{name}
```

### **delete a Pod**

```
DELETE /api/v1/namespaces/{namespace}/pods/{name}
```

### **partially update the specified Pod**

```
PATCH /api/v1/namespaces/{namespace}/pods/{name}
```

### **connect GET requests to attach of Pod**

```
GET /api/v1/namespaces/{namespace}/pods/{name}/attach
```

### **connect POST requests to attach of Pod**

```
POST /api/v1/namespaces/{namespace}/pods/{name}/attach
```

### **create binding of a Binding**

```
POST /api/v1/namespaces/{namespace}/pods/{name}/binding
```

### **create eviction of an Eviction**

```
POST /api/v1/namespaces/{namespace}/pods/{name}/eviction
```

### **connect GET requests to exec of Pod**

```
GET /api/v1/namespaces/{namespace}/pods/{name}/exec
```

### **connect POST requests to exec of Pod**

```
POST /api/v1/namespaces/{namespace}/pods/{name}/exec
```

### **read log of the specified Pod**

```
GET /api/v1/namespaces/{namespace}/pods/{name}/log
```

### **connect GET requests to portforward of Pod**

```
GET /api/v1/namespaces/{namespace}/pods/{name}/portforward
```

### **connect POST requests to portforward of Pod**

```
POST /api/v1/namespaces/{namespace}/pods/{name}/portforward
```

### **connect GET requests to proxy of Pod**

```
GET /api/v1/namespaces/{namespace}/pods/{name}/proxy
```

### **connect PUT requests to proxy of Pod**

```
PUT /api/v1/namespaces/{namespace}/pods/{name}/proxy
```

### **connect DELETE requests to proxy of Pod**

```
DELETE /api/v1/namespaces/{namespace}/pods/{name}/proxy
```

### **connect POST requests to proxy of Pod**

```
POST /api/v1/namespaces/{namespace}/pods/{name}/proxy
```

### **connect GET requests to proxy of Pod**

```
GET /api/v1/namespaces/{namespace}/pods/{name}/proxy/{path}
```

### **connect PUT requests to proxy of Pod**

```
PUT /api/v1/namespaces/{namespace}/pods/{name}/proxy/{path}
```

### **connect DELETE requests to proxy of Pod**

```
DELETE /api/v1/namespaces/{namespace}/pods/{name}/proxy/{path}
```

### **connect POST requests to proxy of Pod**

```
POST /api/v1/namespaces/{namespace}/pods/{name}/proxy/{path}
```

### **read status of the specified Pod**

```
GET /api/v1/namespaces/{namespace}/pods/{name}/status
```

### **replace status of the specified Pod**

```
PUT /api/v1/namespaces/{namespace}/pods/{name}/status
```

### **partially update status of the specified Pod**

```
PATCH /api/v1/namespaces/{namespace}/pods/{name}/status
```

### list or watch objects of kind PodTemplate

```
GET /api/v1/namespaces/{namespace}/podtemplates
```

### delete collection of PodTemplate

```
DELETE /api/v1/namespaces/{namespace}/podtemplates
```

### create a PodTemplate

```
POST /api/v1/namespaces/{namespace}/podtemplates
```

### read the specified PodTemplate

```
GET /api/v1/namespaces/{namespace}/podtemplates/{name}
```

### replace the specified PodTemplate

```
PUT /api/v1/namespaces/{namespace}/podtemplates/{name}
```

### delete a PodTemplate

```
DELETE /api/v1/namespaces/{namespace}/podtemplates/{name}
```

### partially update the specified PodTemplate

```
PATCH /api/v1/namespaces/{namespace}/podtemplates/{name}
```

### list or watch objects of kind ReplicationController

```
GET /api/v1/namespaces/{namespace}/replicationcontrollers
```

### delete collection of ReplicationController

```
DELETE /api/v1/namespaces/{namespace}/replicationcontrollers
```

### create a ReplicationController

```
POST /api/v1/namespaces/{namespace}/replicationcontrollers
```

### read the specified ReplicationController

```
GET /api/v1/namespaces/{namespace}/replicationcontrollers/{name}
```

### replace the specified ReplicationController

```
PUT /api/v1/namespaces/{namespace}/replicationcontrollers/{name}
```

### delete a ReplicationController

```
DELETE /api/v1/namespaces/{namespace}/replicationcontrollers/{name}
```

### partially update the specified ReplicationController

```
PATCH /api/v1/namespaces/{namespace}/replicationcontrollers/{name}
```

### read scale of the specified Scale

```
GET /api/v1/namespaces/{namespace}/replicationcontrollers/{name}/scale
```

### replace scale of the specified Scale

```
PUT /api/v1/namespaces/{namespace}/replicationcontrollers/{name}/scale
```

### partially update scale of the specified Scale

```
PATCH /api/v1/namespaces/{namespace}/replicationcontrollers/{name}/scale
```

### read status of the specified ReplicationController

```
GET /api/v1/namespaces/{namespace}/replicationcontrollers/{name}/status
```

### replace status of the specified ReplicationController

```
PUT /api/v1/namespaces/{namespace}/replicationcontrollers/{name}/status
```

### partially update status of the specified ReplicationController

```
PATCH /api/v1/namespaces/{namespace}/replicationcontrollers/{name}/status
```

### list or watch objects of kind ResourceQuota

```
GET /api/v1/namespaces/{namespace}/resourcequotas
```

### delete collection of ResourceQuota

```
DELETE /api/v1/namespaces/{namespace}/resourcequotas
```

### create a ResourceQuota

```
POST /api/v1/namespaces/{namespace}/resourcequotas
```

### read the specified ResourceQuota

```
GET /api/v1/namespaces/{namespace}/resourcequotas/{name}
```

### replace the specified ResourceQuota

```
PUT /api/v1/namespaces/{namespace}/resourcequotas/{name}
```

### delete a ResourceQuota

```
DELETE /api/v1/namespaces/{namespace}/resourcequotas/{name}
```

### partially update the specified ResourceQuota

```
PATCH /api/v1/namespaces/{namespace}/resourcequotas/{name}
```

### read status of the specified ResourceQuota

```
GET /api/v1/namespaces/{namespace}/resourcequotas/{name}/status
```

### replace status of the specified ResourceQuota

```
PUT /api/v1/namespaces/{namespace}/resourcequotas/{name}/status
```

### partially update status of the specified ResourceQuota

```
PATCH /api/v1/namespaces/{namespace}/resourcequotas/{name}/status
```

### list or watch objects of kind Secret

```
GET /api/v1/namespaces/{namespace}/secrets
```

### delete collection of Secret

```
DELETE /api/v1/namespaces/{namespace}/secrets
```

### create a Secret

```
POST /api/v1/namespaces/{namespace}/secrets
```

### read the specified Secret

```
GET /api/v1/namespaces/{namespace}/secrets/{name}
```

### replace the specified Secret

```
PUT /api/v1/namespaces/{namespace}/secrets/{name}
```

### delete a Secret

```
DELETE /api/v1/namespaces/{namespace}/secrets/{name}
```

### partially update the specified Secret

```
PATCH /api/v1/namespaces/{namespace}/secrets/{name}
```

### list or watch objects of kind ServiceAccount

```
GET /api/v1/namespaces/{namespace}/serviceaccounts
```

### delete collection of ServiceAccount

```
DELETE /api/v1/namespaces/{namespace}/serviceaccounts
```

### create a ServiceAccount

```
POST /api/v1/namespaces/{namespace}/serviceaccounts
```

### read the specified ServiceAccount

```
GET /api/v1/namespaces/{namespace}/serviceaccounts/{name}
```

### replace the specified ServiceAccount

```
PUT /api/v1/namespaces/{namespace}/serviceaccounts/{name}
```

### delete a ServiceAccount

```
DELETE /api/v1/namespaces/{namespace}/serviceaccounts/{name}
```

### partially update the specified ServiceAccount

```
PATCH /api/v1/namespaces/{namespace}/serviceaccounts/{name}
```

### list or watch objects of kind Service

```
GET /api/v1/namespaces/{namespace}/services
```

### **create a Service**

```
POST /api/v1/namespaces/{namespace}/services
```

### **read the specified Service**

```
GET /api/v1/namespaces/{namespace}/services/{name}
```

### **replace the specified Service**

```
PUT /api/v1/namespaces/{namespace}/services/{name}
```

### **delete a Service**

```
DELETE /api/v1/namespaces/{namespace}/services/{name}
```

### **partially update the specified Service**

```
PATCH /api/v1/namespaces/{namespace}/services/{name}
```

### **connect GET requests to proxy of Service**

```
GET /api/v1/namespaces/{namespace}/services/{name}/proxy
```

### **connect PUT requests to proxy of Service**

```
PUT /api/v1/namespaces/{namespace}/services/{name}/proxy
```

### **connect DELETE requests to proxy of Service**

```
DELETE /api/v1/namespaces/{namespace}/services/{name}/proxy
```

### **connect POST requests to proxy of Service**

```
POST /api/v1/namespaces/{namespace}/services/{name}/proxy
```

### **connect GET requests to proxy of Service**

```
GET /api/v1/namespaces/{namespace}/services/{name}/proxy/{path}
```

### **connect PUT requests to proxy of Service**

```
PUT /api/v1/namespaces/{namespace}/services/{name}/proxy/{path}
```

### c**onnect DELETE requests to proxy of Service**

```
DELETE /api/v1/namespaces/{namespace}/services/{name}/proxy/{path}
```

### **connect POST requests to proxy of Service**

```
POST /api/v1/namespaces/{namespace}/services/{name}/proxy/{path}
```

### **read status of the specified Service**

```
GET /api/v1/namespaces/{namespace}/services/{name}/status
```

### **replace status of the specified Service**

```
PUT /api/v1/namespaces/{namespace}/services/{name}/status
```

### **partially update status of the specified Service**

```
PATCH /api/v1/namespaces/{namespace}/services/{name}/status
```

### **read the specified Namespace**

```
GET /api/v1/namespaces/{name}
```

### **replace the specified Namespace**

```
PUT /api/v1/namespaces/{name}
```

### **delete a Namespace**

```
DELETE /api/v1/namespaces/{name}
```

### **partially update the specified Namespace**

```
PATCH /api/v1/namespaces/{name}
```

### **replace finalize of the specified Namespace**

```
PUT /api/v1/namespaces/{name}/finalize
```

### **read status of the specified Namespace**

```
GET /api/v1/namespaces/{name}/status
```

### **replace status of the specified Namespace**

```
PUT /api/v1/namespaces/{name}/status
```

### **partially update status of the specified Namespace**

```
PATCH /api/v1/namespaces/{name}/status
```

### **list or watch objects of kind Node**

```
GET /api/v1/nodes
```

### **delete collection of Node**

```
DELETE /api/v1/nodes
```

### **create a Node**

```
POST /api/v1/nodes
```

### **read the specified Node**

```
GET /api/v1/nodes/{name}
```

### **replace the specified Node**

```
PUT /api/v1/nodes/{name}
```

### **delete a Node**

```
DELETE /api/v1/nodes/{name}
```

### **partially update the specified Node**

```
PATCH /api/v1/nodes/{name}
```

### **connect GET requests to proxy of Node**

```
GET /api/v1/nodes/{name}/proxy
```

### **connect PUT requests to proxy of Node**

```
PUT /api/v1/nodes/{name}/proxy
```

### **connect DELETE requests to proxy of Node**

```
DELETE /api/v1/nodes/{name}/proxy
```

### **connect POST requests to proxy of Node**

```
POST /api/v1/nodes/{name}/proxy
```

### **connect GET requests to proxy of Node**

```
GET /api/v1/nodes/{name}/proxy/{path}
```

### **connect PUT requests to proxy of Node**

```
PUT /api/v1/nodes/{name}/proxy/{path}
```

### **connect DELETE requests to proxy of Node**

```
DELETE /api/v1/nodes/{name}/proxy/{path}
```

### **connect POST requests to proxy of Node**

```
POST /api/v1/nodes/{name}/proxy/{path}
```

### **read status of the specified Node**

```
GET /api/v1/nodes/{name}/status
```

### **replace status of the specified Node**

```
PUT /api/v1/nodes/{name}/status
```

### **partially update status of the specified Node**

```
PATCH /api/v1/nodes/{name}/status
```

### list or watch objects of kind PersistentVolumeClaim

```
GET /api/v1/persistentvolumeclaims
```

### list or watch objects of kind PersistentVolume

```
GET /api/v1/persistentvolumes
```

### delete collection of PersistentVolume

```
DELETE /api/v1/persistentvolumes
```

### create a PersistentVolume

```
POST /api/v1/persistentvolumes
```

### read the specified PersistentVolume

```
GET /api/v1/persistentvolumes/{name}
```

### replace the specified PersistentVolume

```
PUT /api/v1/persistentvolumes/{name}
```

### delete a PersistentVolume

```
DELETE /api/v1/persistentvolumes/{name}
```

### partially update the specified PersistentVolume

```
PATCH /api/v1/persistentvolumes/{name}
```

### read status of the specified PersistentVolume

```
GET /api/v1/persistentvolumes/{name}/status
```

### replace status of the specified PersistentVolume

```
PUT /api/v1/persistentvolumes/{name}/status
```

### partially update status of the specified PersistentVolume

```
PATCH /api/v1/persistentvolumes/{name}/status
```

### **list or watch objects of kind Pod**

```
GET /api/v1/pods
```

### list or watch objects of kind PodTemplate

```
GET /api/v1/podtemplates
```

### **proxy GET requests to Pod**

```
GET /api/v1/proxy/namespaces/{namespace}/pods/{name}
```

### **proxy PUT requests to Pod**

```
PUT /api/v1/proxy/namespaces/{namespace}/pods/{name}
```

### **proxy DELETE requests to Pod**

```
DELETE /api/v1/proxy/namespaces/{namespace}/pods/{name}
```

### **proxy POST requests to Pod**

```
POST /api/v1/proxy/namespaces/{namespace}/pods/{name}
```

### **proxy GET requests to Pod**

```
GET /api/v1/proxy/namespaces/{namespace}/pods/{name}/{path}
```

### **proxy PUT requests to Pod**

```
PUT /api/v1/proxy/namespaces/{namespace}/pods/{name}/{path}
```

### **proxy DELETE requests to Pod**

```
DELETE /api/v1/proxy/namespaces/{namespace}/pods/{name}/{path}
```

### **proxy POST requests to Pod**

```
POST /api/v1/proxy/namespaces/{namespace}/pods/{name}/{path}
```

### **proxy GET requests to Service**

```
GET /api/v1/proxy/namespaces/{namespace}/services/{name}
```

### **proxy PUT requests to Service**

```
PUT /api/v1/proxy/namespaces/{namespace}/services/{name}
```

### **proxy DELETE requests to Service**

```
DELETE /api/v1/proxy/namespaces/{namespace}/services/{name}
```

### **proxy POST requests to Service**

```
POST /api/v1/proxy/namespaces/{namespace}/services/{name}
```

### **proxy GET requests to Service**

```
GET /api/v1/proxy/namespaces/{namespace}/services/{name}/{path}
```

### **proxy PUT requests to Service**

```
PUT /api/v1/proxy/namespaces/{namespace}/services/{name}/{path}
```

### **proxy DELETE requests to Service**

```
DELETE /api/v1/proxy/namespaces/{namespace}/services/{name}/{path}
```

### **proxy POST requests to Service**

```
POST /api/v1/proxy/namespaces/{namespace}/services/{name}/{path}
```

### **proxy GET requests to Node**

```
GET /api/v1/proxy/nodes/{name}
```

### **proxy PUT requests to Node**

```
PUT /api/v1/proxy/nodes/{name}
```

### **proxy DELETE requests to Node**

```
DELETE /api/v1/proxy/nodes/{name}
```

### **proxy POST requests to Node**

```
POST /api/v1/proxy/nodes/{name}
```

### **proxy GET requests to Node**

```
GET /api/v1/proxy/nodes/{name}/{path}
```

### **proxy PUT requests to Node**

```
PUT /api/v1/proxy/nodes/{name}/{path}
```

### **proxy DELETE requests to Node**

```
DELETE /api/v1/proxy/nodes/{name}/{path}
```

### **proxy POST requests to Node**

```
POST /api/v1/proxy/nodes/{name}/{path}
```

### list or watch objects of kind ReplicationController

```
GET /api/v1/replicationcontrollers
```

### list or watch objects of kind ResourceQuota

```
GET /api/v1/resourcequotas
```

### list or watch objects of kind Secret

```
GET /api/v1/secrets
```

### list or watch objects of kind ServiceAccount

```
GET /api/v1/serviceaccounts
```

### **list or watch objects of kind Service**

```
GET /api/v1/services
```

### watch individual changes to a list of ConfigMap

```
GET /api/v1/watch/configmaps
```

### watch individual changes to a list of Endpoints

```
GET /api/v1/watch/endpoints
```

### watch individual changes to a list of Event

```
GET /api/v1/watch/events
```

### watch individual changes to a list of LimitRange

```
GET /api/v1/watch/limitranges
```

### watch individual changes to a list of Namespace

```
GET /api/v1/watch/namespaces
```

### watch individual changes to a list of ConfigMap

```
GET /api/v1/watch/namespaces/{namespace}/configmaps
```

### watch changes to an object of kind ConfigMap

```
GET /api/v1/watch/namespaces/{namespace}/configmaps/{name}
```

### watch individual changes to a list of Endpoints

```
GET /api/v1/watch/namespaces/{namespace}/endpoints
```

### watch changes to an object of kind Endpoints

```
GET /api/v1/watch/namespaces/{namespace}/endpoints/{name}
```

### watch individual changes to a list of Event

```
GET /api/v1/watch/namespaces/{namespace}/events
```

### watch changes to an object of kind Event

```
GET /api/v1/watch/namespaces/{namespace}/events/{name}
```

### watch individual changes to a list of LimitRange

```
GET /api/v1/watch/namespaces/{namespace}/limitranges
```

### watch changes to an object of kind LimitRange

```
GET /api/v1/watch/namespaces/{namespace}/limitranges/{name}
```

### watch individual changes to a list of PersistentVolumeClaim

```
GET /api/v1/watch/namespaces/{namespace}/persistentvolumeclaims
```

### watch changes to an object of kind PersistentVolumeClaim

```
GET /api/v1/watch/namespaces/{namespace}/persistentvolumeclaims/{name}
```

### watch individual changes to a list of Pod

```
GET /api/v1/watch/namespaces/{namespace}/pods
```

### watch changes to an object of kind Pod

```
GET /api/v1/watch/namespaces/{namespace}/pods/{name}
```

### watch individual changes to a list of PodTemplate

```
GET /api/v1/watch/namespaces/{namespace}/podtemplates
```

### watch changes to an object of kind PodTemplate

```
GET /api/v1/watch/namespaces/{namespace}/podtemplates/{name}
```

### watch individual changes to a list of ReplicationController

```
GET /api/v1/watch/namespaces/{namespace}/replicationcontrollers
```

### watch changes to an object of kind ReplicationController

```
GET /api/v1/watch/namespaces/{namespace}/replicationcontrollers/{name}
```

### watch individual changes to a list of ResourceQuota

```
GET /api/v1/watch/namespaces/{namespace}/resourcequotas
```

### watch changes to an object of kind ResourceQuota

```
GET /api/v1/watch/namespaces/{namespace}/resourcequotas/{name}
```

### watch individual changes to a list of Secret

```
GET /api/v1/watch/namespaces/{namespace}/secrets
```

### watch changes to an object of kind Secret

```
GET /api/v1/watch/namespaces/{namespace}/secrets/{name}
```

### watch individual changes to a list of ServiceAccount

```
GET /api/v1/watch/namespaces/{namespace}/serviceaccounts
```

### watch changes to an object of kind ServiceAccount

```
GET /api/v1/watch/namespaces/{namespace}/serviceaccounts/{name}
```

### watch individual changes to a list of Service

```
GET /api/v1/watch/namespaces/{namespace}/services
```

### watch changes to an object of kind Service

```
GET /api/v1/watch/namespaces/{namespace}/services/{name}
```

### watch changes to an object of kind Namespace

```
GET /api/v1/watch/namespaces/{name}
```

### watch individual changes to a list of Node

```
GET /api/v1/watch/nodes
```

### watch changes to an object of kind Node

```
GET /api/v1/watch/nodes/{name}
```

### watch individual changes to a list of PersistentVolumeClaim

```
GET /api/v1/watch/persistentvolumeclaims
```

### watch individual changes to a list of PersistentVolume

```
GET /api/v1/watch/persistentvolumes
```

### watch changes to an object of kind PersistentVolume

```
GET /api/v1/watch/persistentvolumes/{name}
```

### watch individual changes to a list of Pod

```
GET /api/v1/watch/pods
```

### watch individual changes to a list of PodTemplate

```
GET /api/v1/watch/podtemplates
```

### watch individual changes to a list of ReplicationController

```
GET /api/v1/watch/replicationcontrollers
```

### watch individual changes to a list of ResourceQuota

```
GET /api/v1/watch/resourcequotas
```

### watch individual changes to a list of Secret

```
GET /api/v1/watch/secrets
```

### watch individual changes to a list of ServiceAccount

```
GET /api/v1/watch/serviceaccounts
```

### watch individual changes to a list of Service

```
GET /api/v1/watch/services
```

## Autoscaling API

### **get available resources**

```
GET /apis/autoscaling/v1
```

### **list or watch objects of kind HorizontalPodAutoscaler**

```
GET /apis/autoscaling/v1/horizontalpodautoscalers
```

### **list or watch objects of kind HorizontalPodAutoscaler**

```
GET /apis/autoscaling/v1/namespaces/{namespace}/horizontalpodautoscalers
```

### **delete collection of HorizontalPodAutoscaler**

```
DELETE /apis/autoscaling/v1/namespaces/{namespace}/horizontalpodautoscalers
```

### **create a HorizontalPodAutoscaler**

```
POST /apis/autoscaling/v1/namespaces/{namespace}/horizontalpodautoscalers
```

### **read the specified HorizontalPodAutoscaler**

```
GET /apis/autoscaling/v1/namespaces/{namespace}/horizontalpodautoscalers/{name}
```

### **replace the specified HorizontalPodAutoscaler**

```
PUT /apis/autoscaling/v1/namespaces/{namespace}/horizontalpodautoscalers/{name}
```

### **delete a HorizontalPodAutoscaler**

```
DELETE /apis/autoscaling/v1/namespaces/{namespace}/horizontalpodautoscalers/{name}
```

### **partially update the specified HorizontalPodAutoscaler**

```
PATCH /apis/autoscaling/v1/namespaces/{namespace}/horizontalpodautoscalers/{name}
```

### **read status of the specified HorizontalPodAutoscaler**

```
GET /apis/autoscaling/v1/namespaces/{namespace}/horizontalpodautoscalers/{name}/status
```

### **replace status of the specified HorizontalPodAutoscaler**

```
PUT /apis/autoscaling/v1/namespaces/{namespace}/horizontalpodautoscalers/{name}/status
```

### **partially update status of the specified HorizontalPodAutoscaler**

```
PATCH /apis/autoscaling/v1/namespaces/{namespace}/horizontalpodautoscalers/{name}/status
```

### **watch individual changes to a list of HorizontalPodAutoscaler**

```
GET /apis/autoscaling/v1/watch/horizontalpodautoscalers
```

### **watch individual changes to a list of HorizontalPodAutoscaler**

```
GET /apis/autoscaling/v1/watch/namespaces/{namespace}/horizontalpodautoscalers
```

### **watch changes to an object of kind HorizontalPodAutoscaler**

```
GET /apis/autoscaling/v1/watch/namespaces/{namespace}/horizontalpodautoscalers/{name}
```

## Batch API

### **get available resources**

```
GET /apis/batch/v1
```

### list or watch objects of kind Job

```
GET /apis/batch/v1/jobs
```

### list or watch objects of kind Job

```
GET /apis/batch/v1/namespaces/{namespace}/jobs
```

### delete collection of Job

```
DELETE /apis/batch/v1/namespaces/{namespace}/jobs
```

### create a Job

```
POST /apis/batch/v1/namespaces/{namespace}/jobs
```

### read the specified Job

```
GET /apis/batch/v1/namespaces/{namespace}/jobs/{name}
```

### replace the specified Job

```
PUT /apis/batch/v1/namespaces/{namespace}/jobs/{name}
```

### delete a Job

```
DELETE /apis/batch/v1/namespaces/{namespace}/jobs/{name}
```

### partially update the specified Job

```
PATCH /apis/batch/v1/namespaces/{namespace}/jobs/{name}
```

### read status of the specified Job

```
GET /apis/batch/v1/namespaces/{namespace}/jobs/{name}/status
```

### replace status of the specified Job

```
PUT /apis/batch/v1/namespaces/{namespace}/jobs/{name}/status
```

### partially update status of the specified Job

```
PATCH /apis/batch/v1/namespaces/{namespace}/jobs/{name}/status
```

### watch individual changes to a list of Job

```
GET /apis/batch/v1/watch/jobs
```

### watch individual changes to a list of Job

```
GET /apis/batch/v1/watch/namespaces/{namespace}/jobs
```

### watch changes to an object of kind Job

```
GET /apis/batch/v1/watch/namespaces/{namespace}/jobs/{name}
```

## Apps API

### get available resources

```
GET /apis/apps/v1beta1
```

### list or watch objects of kind StatefulSet

```
GET /apis/apps/v1beta1/namespaces/{namespace}/statefulsets
```

### delete collection of StatefulSet

```
DELETE /apis/apps/v1beta1/namespaces/{namespace}/statefulsets
```

### create a StatefulSet

```
POST /apis/apps/v1beta1/namespaces/{namespace}/statefulsets
```

### read the specified StatefulSet

```
GET /apis/apps/v1beta1/namespaces/{namespace}/statefulsets/{name}
```

### replace the specified StatefulSet

```
PUT /apis/apps/v1beta1/namespaces/{namespace}/statefulsets/{name}
```

### delete a StatefulSet

```
DELETE /apis/apps/v1beta1/namespaces/{namespace}/statefulsets/{name}
```

### partially update the specified StatefulSet

```
PATCH /apis/apps/v1beta1/namespaces/{namespace}/statefulsets/{name}
```

### read status of the specified StatefulSet

```
GET /apis/apps/v1beta1/namespaces/{namespace}/statefulsets/{name}/status
```

### replace status of the specified StatefulSet

```
PUT /apis/apps/v1beta1/namespaces/{namespace}/statefulsets/{name}/status
```

### partially update status of the specified StatefulSet

```
PATCH /apis/apps/v1beta1/namespaces/{namespace}/statefulsets/{name}/status
```

### list or watch objects of kind StatefulSet

```
GET /apis/apps/v1beta1/statefulsets
```

### watch individual changes to a list of StatefulSet

```
GET /apis/apps/v1beta1/watch/namespaces/{namespace}/statefulsets
```

### watch changes to an object of kind StatefulSet

```
GET /apis/apps/v1beta1/watch/namespaces/{namespace}/statefulsets/{name}
```

### watch individual changes to a list of StatefulSet

```
GET /apis/apps/v1beta1/watch/statefulsets
```

## Extendsion API

### get available resources

```
GET /apis/extensions/v1beta1
```

### list or watch objects of kind DaemonSet

```
GET /apis/extensions/v1beta1/daemonsets
```

### **list or watch objects of kind Deployments**

```
GET /apis/extensions/v1beta1/deployments
```

### **list or watch objects of kind HorizontalPodAutoscaler**

```
GET /apis/extensions/v1beta1/horizontalpodautoscalers
```

### list or watch objects of kind Ingress

```
GET /apis/extensions/v1beta1/ingresses
```

### list or watch objects of kind Job

```
GET /apis/extensions/v1beta1/jobs
```

### list or watch objects of kind DaemonSet

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/daemonsets
```

### delete collection of DaemonSet

```
DELETE /apis/extensions/v1beta1/namespaces/{namespace}/daemonsets
```

### create a DaemonSet

```
POST /apis/extensions/v1beta1/namespaces/{namespace}/daemonsets
```

### read the specified DaemonSet

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/daemonsets/{name}
```

### replace the specified DaemonSet

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/daemonsets/{name}
```

### delete a DaemonSet

```
DELETE /apis/extensions/v1beta1/namespaces/{namespace}/daemonsets/{name}
```

### partially update the specified DaemonSet

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/daemonsets/{name}
```

### read status of the specified DaemonSet

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/daemonsets/{name}/status
```

### replace status of the specified DaemonSet

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/daemonsets/{name}/status
```

### partially update status of the specified DaemonSet

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/daemonsets/{name}/status
```

### **list or watch objects of kind Deployment**

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/deployments
```

### **delete collection of Deployment**

```
DELETE /apis/extensions/v1beta1/namespaces/{namespace}/deployments
```

### **create a Deployment**

```
POST /apis/extensions/v1beta1/namespaces/{namespace}/deployments
```

### **read the specified Deployment**

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/deployments/{name}
```

### **replace the specified Deployment**

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/deployments/{name}
```

### **delete a Deployment**

```
DELETE /apis/extensions/v1beta1/namespaces/{namespace}/deployments/{name}
```

### **partially update the specified Deployment**

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/deployments/{name}
```

### **create rollback of a DeploymentRollback**

```
POST /apis/extensions/v1beta1/namespaces/{namespace}/deployments/{name}/rollback
```

### **read scale of the specified Scale**

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/deployments/{name}/scale
```

### **replace scale of the specified Scale**

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/deployments/{name}/scale
```

### **partially update scale of the specified Scale**

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/deployments/{name}/scale
```

### **read status of the specified Deployment**

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/deployments/{name}/status
```

### **replace status of the specified Deployment**

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/deployments/{name}/status
```

### **partially update status of the specified Deployment**

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/deployments/{name}/status
```

### **list or watch objects of kind HorizontalPodAutoscaler**

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/horizontalpodautoscalers
```

### delete collection of HorizontalPodAutoscaler

```
DELETE /apis/extensions/v1beta1/namespaces/{namespace}/horizontalpodautoscalers
```

### **create a HorizontalPodAutoscaler**

```
POST /apis/extensions/v1beta1/namespaces/{namespace}/horizontalpodautoscalers
```

### **read the specified HorizontalPodAutoscaler**

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/horizontalpodautoscalers/{name}
```

### **replace the specified HorizontalPodAutoscaler**

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/horizontalpodautoscalers/{name}
```

### **delete a HorizontalPodAutoscaler**

```
DELETE /apis/extensions/v1beta1/namespaces/{namespace}/horizontalpodautoscalers/{name}
```

### **partially update the specified HorizontalPodAutoscaler**

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/horizontalpodautoscalers/{name}
```

### **read status of the specified HorizontalPodAutoscaler**

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/horizontalpodautoscalers/{name}/status
```

### **replace status of the specified HorizontalPodAutoscaler**

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/horizontalpodautoscalers/{name}/status
```

### **partially update status of the specified HorizontalPodAutoscaler**

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/horizontalpodautoscalers/{name}/status
```

### list or watch objects of kind Ingress

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/ingresses
```

### delete collection of Ingress

```
DELETE /apis/extensions/v1beta1/namespaces/{namespace}/ingresses
```

### create an Ingress

```
POST /apis/extensions/v1beta1/namespaces/{namespace}/ingresses
```

### read the specified Ingress

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/ingresses/{name}
```

### replace the specified Ingress

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/ingresses/{name}
```

### delete an Ingress

```
DELETE /apis/extensions/v1beta1/namespaces/{namespace}/ingresses/{name}
```

### partially update the specified Ingress

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/ingresses/{name}
```

### read status of the specified Ingress

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/ingresses/{name}/status
```

### replace status of the specified Ingress

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/ingresses/{name}/status
```

### partially update status of the specified Ingress

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/ingresses/{name}/status
```

### list or watch objects of kind Job

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/jobs
```

### delete collection of Job

```
DELETE /apis/extensions/v1beta1/namespaces/{namespace}/jobs
```

### create a Job

```
POST /apis/extensions/v1beta1/namespaces/{namespace}/jobs
```

### read the specified Job

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/jobs/{name}
```

### replace the specified Job

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/jobs/{name}
```

### delete a Job

```
DELETE /apis/extensions/v1beta1/namespaces/{namespace}/jobs/{name}
```

### partially update the specified Job

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/jobs/{name}
```

### read status of the specified Job

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/jobs/{name}/status
```

### replace status of the specified Job

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/jobs/{name}/status
```

### partially update status of the specified Job

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/jobs/{name}/status
```

### list or watch objects of kind NetworkPolicy

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/networkpolicies
```

### delete collection of NetworkPolicy

```
DELETE /apis/extensions/v1beta1/namespaces/{namespace}/networkpolicies
```

### create a NetworkPolicy

```
POST /apis/extensions/v1beta1/namespaces/{namespace}/networkpolicies
```

### read the specified NetworkPolicy

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/networkpolicies/{name}
```

### replace the specified NetworkPolicy

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/networkpolicies/{name}
```

### delete a NetworkPolicy

```
DELETE /apis/extensions/v1beta1/namespaces/{namespace}/networkpolicies/{name}
```

### partially update the specified NetworkPolicy

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/networkpolicies/{name}
```

### **list or watch objects of kind ReplicaSet**

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/replicasets
```

### **delete collection of ReplicaSet**

```
DELETE /apis/extensions/v1beta1/namespaces/{namespace}/replicasets
```

### **create a ReplicaSet**

```
POST /apis/extensions/v1beta1/namespaces/{namespace}/replicasets
```

### **read the specified ReplicaSet**

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/replicasets/{name}
```

### **replace the specified ReplicaSet**

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/replicasets/{name}
```

### **delete a ReplicaSet**

```
DELETE /apis/extensions/v1beta1/namespaces/{namespace}/replicasets/{name}
```

### **partially update the specified ReplicaSet**

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/replicasets/{name}
```

### **read scale of the specified Scale**

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/replicasets/{name}/scale
```

### **replace scale of the specified Scale**

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/replicasets/{name}/scale
```

### **partially update scale of the specified Scale**

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/replicasets/{name}/scale
```

### **read status of the specified ReplicaSet**

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/replicasets/{name}/status
```

### **replace status of the specified ReplicaSet**

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/replicasets/{name}/status
```

### **partially update status of the specified ReplicaSet**

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/replicasets/{name}/status
```

### **read scale of the specified Scale**

```
GET /apis/extensions/v1beta1/namespaces/{namespace}/replicationcontrollers/{name}/scale
```

### **replace scale of the specified Scale**

```
PUT /apis/extensions/v1beta1/namespaces/{namespace}/replicationcontrollers/{name}/scale
```

### **partially update scale of the specified Scale**

```
PATCH /apis/extensions/v1beta1/namespaces/{namespace}/replicationcontrollers/{name}/scale
```

### list or watch objects of kind NetworkPolicy

```
GET /apis/extensions/v1beta1/networkpolicies
```

### **list or watch objects of kind ReplicaSet**

```
GET /apis/extensions/v1beta1/replicasets
```

### list or watch objects of kind ThirdPartyResource

```
GET /apis/extensions/v1beta1/thirdpartyresources
```

### delete collection of ThirdPartyResource

```
DELETE /apis/extensions/v1beta1/thirdpartyresources
```

### create a ThirdPartyResource

```
POST /apis/extensions/v1beta1/thirdpartyresources
```

### read the specified ThirdPartyResource

```
GET /apis/extensions/v1beta1/thirdpartyresources/{name}
```

### replace the specified ThirdPartyResource

```
PUT /apis/extensions/v1beta1/thirdpartyresources/{name}
```

### delete a ThirdPartyResource

```
DELETE /apis/extensions/v1beta1/thirdpartyresources/{name}
```

### partially update the specified ThirdPartyResource

```
PATCH /apis/extensions/v1beta1/thirdpartyresources/{name}
```

### watch individual changes to a list of DaemonSet

```
GET /apis/extensions/v1beta1/watch/daemonsets
```

### **watch individual changes to a list of Deployment**

```
GET /apis/extensions/v1beta1/watch/deployments
```

### **watch individual changes to a list of HorizontalPodAutoscaler**

```
GET /apis/extensions/v1beta1/watch/horizontalpodautoscalers
```

### watch individual changes to a list of Ingress

```
GET /apis/extensions/v1beta1/watch/ingresses
```

### watch individual changes to a list of Job

```
GET /apis/extensions/v1beta1/watch/jobs
```

### watch individual changes to a list of DaemonSet

```
GET /apis/extensions/v1beta1/watch/namespaces/{namespace}/daemonsets
```

### watch changes to an object of kind DaemonSet

```
GET /apis/extensions/v1beta1/watch/namespaces/{namespace}/daemonsets/{name}
```

### **watch individual changes to a list of Deployment**

```
GET /apis/extensions/v1beta1/watch/namespaces/{namespace}/deployments
```

### **watch changes to an object of kind Deployment**

```
GET /apis/extensions/v1beta1/watch/namespaces/{namespace}/deployments/{name}
```

### **watch individual changes to a list of HorizontalPodAutoscaler**

```
GET /apis/extensions/v1beta1/watch/namespaces/{namespace}/horizontalpodautoscalers
```

### **watch changes to an object of kind HorizontalPodAutoscaler**

```
GET /apis/extensions/v1beta1/watch/namespaces/{namespace}/horizontalpodautoscalers/{name}
```

### watch individual changes to a list of Ingress

```
GET /apis/extensions/v1beta1/watch/namespaces/{namespace}/ingresses
```

### watch changes to an object of kind Ingress

```
GET /apis/extensions/v1beta1/watch/namespaces/{namespace}/ingresses/{name}
```

### watch individual changes to a list of Job

```
GET /apis/extensions/v1beta1/watch/namespaces/{namespace}/jobs
```

### watch changes to an object of kind Job

```
GET /apis/extensions/v1beta1/watch/namespaces/{namespace}/jobs/{name}
```

### watch individual changes to a list of NetworkPolicy

```
GET /apis/extensions/v1beta1/watch/namespaces/{namespace}/networkpolicies
```

### watch changes to an object of kind NetworkPolicy

```
GET /apis/extensions/v1beta1/watch/namespaces/{namespace}/networkpolicies/{name}
```

### watch individual changes to a list of ReplicaSet

```
GET /apis/extensions/v1beta1/watch/namespaces/{namespace}/replicasets
```

### watch changes to an object of kind ReplicaSet

```
GET /apis/extensions/v1beta1/watch/namespaces/{namespace}/replicasets/{name}
```

### watch individual changes to a list of NetworkPolicy

```
GET /apis/extensions/v1beta1/watch/networkpolicies
```

### watch individual changes to a list of ReplicaSet

```
GET /apis/extensions/v1beta1/watch/replicasets
```

### watch individual changes to a list of ThirdPartyResource

```
GET /apis/extensions/v1beta1/watch/thirdpartyresources
```

### watch changes to an object of kind ThirdPartyResource

```
GET /apis/extensions/v1beta1/watch/thirdpartyresources/{name}
```
