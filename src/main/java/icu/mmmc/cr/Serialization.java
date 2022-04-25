package icu.mmmc.cr;

import java.io.Serializable;

/**
 * 自定义Bson序列化接口
 *
 * @author shouchen
 */
public interface Serialization extends Serializable {
    /**
     * 序列化为Bson数据
     *
     * @return 序列化数据
     */
    byte[] serialize();
}
