package run.acloud.api.resource.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.springframework.stereotype.Component;
import run.acloud.api.resource.util.patchmixins.V1ObjectMetaMixIn;
import run.acloud.api.resource.util.patchserialization.IntOrStringSerializer;
import run.acloud.api.resource.util.patchserialization.QuantitySerializer;

@Component
public class PatchObjectMapper extends ObjectMapper{

    public PatchObjectMapper() {
        this.addMixIn(V1ObjectMeta.class, V1ObjectMetaMixIn.class);
        this.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.registerModule(JavaTimeModuleUtils.getModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(Quantity.class, new QuantitySerializer());
        module.addSerializer(IntOrString.class, new IntOrStringSerializer());
        this.registerModule(module);
    }
}
