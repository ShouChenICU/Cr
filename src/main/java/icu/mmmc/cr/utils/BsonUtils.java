package icu.mmmc.cr.utils;

import org.bson.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bson工具类，主要负责序列化和反序列化Bson对象
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public final class BsonUtils {
    private static final Map<Long, BSONEncoder> encoderMap;
    private static final Map<Long, BSONDecoder> decoderMap;

    static {
        encoderMap = new ConcurrentHashMap<>();
        decoderMap = new ConcurrentHashMap<>();
    }

    private static BSONEncoder getEncoder() {
        long id = Thread.currentThread().getId();
        BSONEncoder encoder = encoderMap.get(id);
        if (encoder == null) {
            encoder = new BasicBSONEncoder();
            encoderMap.put(id, encoder);
        }
        return encoder;
    }

    private static BSONDecoder getDecoder() {
        long id = Thread.currentThread().getId();
        BSONDecoder decoder = decoderMap.get(id);
        if (decoder == null) {
            decoder = new BasicBSONDecoder();
            decoderMap.put(id, decoder);
        }
        return decoder;
    }

    /**
     * 序列化Bson对象
     *
     * @param object Bson对象
     * @return 序列化数据
     */
    public static byte[] serialize(BSONObject object) {
        return getEncoder().encode(object);
    }

    /**
     * 反序列化Bson对象
     *
     * @param data 序列化数据
     * @return Bson对象
     */
    public static BSONObject deserialize(byte[] data) {
        return getDecoder().readObject(data);
    }
}
