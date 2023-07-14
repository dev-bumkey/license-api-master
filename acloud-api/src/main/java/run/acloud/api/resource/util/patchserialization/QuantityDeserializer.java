package run.acloud.api.resource.util.patchserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.kubernetes.client.custom.Quantity;

import java.io.IOException;

public class QuantityDeserializer extends StdDeserializer<Quantity> {
    public QuantityDeserializer() {
        this(null);
    }

    public QuantityDeserializer(Class<Quantity> t) {
        super(t);
    }

    @Override
    public Quantity deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return Quantity.fromString(p.nextTextValue());
    }
}
