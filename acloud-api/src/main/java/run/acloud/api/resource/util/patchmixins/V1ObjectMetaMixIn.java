package run.acloud.api.resource.util.patchmixins;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.kubernetes.client.openapi.models.V1ManagedFieldsEntry;
import io.kubernetes.client.openapi.models.V1ObjectMeta;

import java.time.OffsetDateTime;
import java.util.List;

public abstract class V1ObjectMetaMixIn extends V1ObjectMeta{
    @JsonIgnore
    private OffsetDateTime creationTimestamp;
    @JsonIgnore
    private OffsetDateTime deletionTimestamp;
    @JsonIgnore
    private Long generation;
    @JsonIgnore
    private String resourceVersion;
    @JsonIgnore
    private String selfLink;
    @JsonIgnore
    private String uid;
    @JsonIgnore
    private List<V1ManagedFieldsEntry> managedFields;

    public V1ObjectMetaMixIn() {
    }

    @JsonIgnore
    public abstract OffsetDateTime getCreationTimestamp();

    @JsonIgnore
    public abstract OffsetDateTime getDeletionTimestamp();

    @JsonIgnore
    public abstract Long getGeneration();

    @JsonIgnore
    public abstract String getResourceVersion();

    @JsonIgnore
    public abstract String getSelfLink();

    @JsonIgnore
    public abstract String getUid();

    @JsonIgnore
    public abstract List<V1ManagedFieldsEntry> getManagedFields();
}
