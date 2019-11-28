package com.mycompany.mutiprocess.ms_client.common;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

@Slf4j
public class Serializers {

    private Serializers() {
    }

    public static byte[] serialize(Object data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream os = new ObjectOutputStream(baos)) {
            os.writeObject(data);
            os.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("Serialization error, data:" + data, e);
            throw new RuntimeException("Serialization error:" + e.getMessage());
        }
    }

    public static <T> T deserialize(byte[] data, Class<T> classOfT) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream is = new ObjectInputStream(bis)) {
            Object obj = is.readObject();
            return classOfT.cast(obj);
        } catch (Exception e) {
            logger.error("DeSerialization error, classOfT:" + classOfT, e);
            throw new RuntimeException("DeSerialization error:" + e.getMessage());
        }
    }
}
