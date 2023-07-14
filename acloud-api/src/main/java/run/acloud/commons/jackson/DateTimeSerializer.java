package run.acloud.commons.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;

public class DateTimeSerializer extends StdSerializer<DateTime> {
    private static final long serialVersionUID = 2631449175586008015L;

    protected DateTimeSerializer(Class<DateTime> t) {
        super(t);
    }

    public DateTimeSerializer() {
        this(null);
    }

    @Override
    public void serialize(DateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
        gen.writeString(dtf.print(value));
    }
}