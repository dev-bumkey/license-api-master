package run.acloud.api.resource.util.patchserialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.kubernetes.client.custom.Quantity;

import java.io.IOException;

public class QuantitySerializer extends StdSerializer<Quantity> {
    public QuantitySerializer() {
        this(null);
    }

    public QuantitySerializer(Class<Quantity> t) {
        super(t);
    }

    @Override
    public void serialize(
            Quantity value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {

//        jgen.writeNumber(value.getNumber().doubleValue());
        jgen.writeString(value != null ? value.toSuffixedString() : null);
    }
}
