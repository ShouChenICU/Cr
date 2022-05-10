package icu.mmmc.cr.utils;

import icu.mmmc.cr.Serialization;
import org.bson.BasicBSONObject;

/**
 * 包装的BSONObject
 * 加入链式调用等特性，使其更方便使用
 *
 * @author shouchen
 */
public class BsonObject extends BasicBSONObject implements Serialization {
    public BsonObject() {
    }

    public BsonObject set(String key, Object value) {
        super.put(key, value);
        return this;
    }

    /**
     * 序列化为Bson数据
     *
     * @return 序列化数据
     */
    @Override
    public byte[] serialize() {
        return BsonUtils.serialize(this);
    }
}
