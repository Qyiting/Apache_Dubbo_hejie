package com.rpc.serialization.json;

import com.caucho.hessian.io.AbstractDeserializer;
import com.caucho.hessian.io.AbstractHessianInput;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Java8TimeDeserializer extends AbstractDeserializer {
    @Override
    public Object readObject(AbstractHessianInput in) throws IOException {
        String text = (String) in.readObject(String.class);
        return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    @Override
    public Class<?> getType() {
        return LocalDateTime.class;
    }
}