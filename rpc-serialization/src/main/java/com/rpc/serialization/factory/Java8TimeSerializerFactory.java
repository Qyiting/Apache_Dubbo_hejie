package com.rpc.serialization.factory;


import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.Serializer;
import com.caucho.hessian.io.SerializerFactory;
import com.rpc.serialization.json.Java8TimeDeserializer;
import com.rpc.serialization.json.Java8TimeSerializer;

import java.time.LocalDateTime;

public class Java8TimeSerializerFactory extends SerializerFactory {

    @Override
    public Serializer getSerializer(Class cl) throws HessianProtocolException {
        if (LocalDateTime.class.isAssignableFrom(cl)) {
            return new Java8TimeSerializer();
        }
        return super.getSerializer(cl);
    }

    @Override
    public Deserializer getDeserializer(Class cl) throws HessianProtocolException {
        if (LocalDateTime.class.isAssignableFrom(cl)) {
            return new Java8TimeDeserializer();
        }
        return super.getDeserializer(cl);
    }
}
