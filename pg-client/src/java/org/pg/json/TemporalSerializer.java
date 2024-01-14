package org.pg.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.temporal.Temporal;

public class TemporalSerializer extends StdSerializer<Temporal> {

    public TemporalSerializer() {
        super(Temporal.class);
    }

    public void serialize(Temporal temporal,
                          JsonGenerator jsonGenerator,
                          SerializerProvider ignored) throws IOException {
        jsonGenerator.writeString(temporal.toString());
    }
}
