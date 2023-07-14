package run.acloud.api.resource.util.patchserialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.kubernetes.client.custom.IntOrString;

import java.io.IOException;

public class IntOrStringSerializer extends StdSerializer<IntOrString> {
    public IntOrStringSerializer() {
        this(null);
    }

    public IntOrStringSerializer(Class<IntOrString> t) {
        super(t);
    }

    @Override
    public void serialize(
            IntOrString value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {

        if (value.isInteger()) {
            jgen.writeNumber(value.getIntValue());
        } else {
            jgen.writeString(value.getStrValue());
        }
    }
}
