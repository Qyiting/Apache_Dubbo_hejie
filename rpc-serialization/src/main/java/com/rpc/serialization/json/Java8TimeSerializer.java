package com.rpc.serialization.json;

import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.AbstractSerializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Java8TimeSerializer extends AbstractSerializer {
    @Override
    public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
        if (obj == null) {
            out.writeNull();
        } else {
            out.writeString(((LocalDateTime) obj).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }
}