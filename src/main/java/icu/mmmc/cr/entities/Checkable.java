package icu.mmmc.cr.entities;

/**
 * 可检查接口
 *
 * @author shouchen
 */
public interface Checkable {
    /**
     * 检查
     *
     * @throws Exception 检查失败，抛出异常
     */
    void check() throws Exception;
}
