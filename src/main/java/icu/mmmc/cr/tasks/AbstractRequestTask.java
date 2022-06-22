package icu.mmmc.cr.tasks;

import icu.mmmc.cr.annotations.RequestPath;
import icu.mmmc.cr.callbacks.ProgressCallback;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 抽象请求任务
 *
 * @author shouchen
 */
public abstract class AbstractRequestTask extends AbstractTask {
    protected static final String REQ_PATH = "REQ_PATH";
    protected static final String RESULT = "RESULT";
    protected static final Map<String, Method> PATH_METHOD_MAP = new HashMap<>();

    static {
        // 反射
        Class<ResponseTask> clazz = ResponseTask.class;
        // 拿到全部方法
        Method[] methods = clazz.getDeclaredMethods();
        // 检索含有RequestPath注解的方法
        for (Method method : methods) {
            RequestPath requestPath = method.getAnnotation(RequestPath.class);
            if (requestPath != null) {
                // 设置可访问
                method.setAccessible(true);
                // 添加到路径方法map
                PATH_METHOD_MAP.put(requestPath.path().toUpperCase(), method);
            }
        }
    }

    public AbstractRequestTask(ProgressCallback callback) {
        super(callback);
    }
}
