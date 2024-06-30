package six.eared.macaque.agent.asm2.enhance;


import six.eared.macaque.agent.env.Environment;
import six.eared.macaque.common.util.ClassUtil;
import six.eared.macaque.common.util.FileUtil;
import six.eared.macaque.common.util.ReflectUtil;

import java.io.File;

public class CompatibilityModeClassLoader {


    public synchronized static void loadClass(String className, byte[] bytes) {
        if (Environment.isDebug()) {
            FileUtil.writeBytes(
                    new File(FileUtil.getProcessTmpPath() + File.separator + ClassUtil.toSimpleName(className) + ".class"),
                    bytes);
        }
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        Class<?> clazz = (Class<?>) ReflectUtil.invokeMethod(systemClassLoader, "defineClass", className, bytes, 0, bytes.length);
    }
}
