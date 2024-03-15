package org.pg.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pg.json.JSON;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class CodecParams {

    public Charset clientCharset = StandardCharsets.UTF_8;
    public Charset serverCharset = StandardCharsets.UTF_8;
    public ZoneId timeZone = ZoneOffset.UTC;
    public String dateStyle = "";
    public boolean integerDatetime = true;
    public ObjectMapper objectMapper = JSON.mapper;

    public static CodecParams standard () {
        return new CodecParams();
    }

}
