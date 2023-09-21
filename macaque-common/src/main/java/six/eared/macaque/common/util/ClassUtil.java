package six.eared.macaque.common.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClassUtil {
    public static String className2path(String clazz) {
        return clazz.replace('.', '/') + ".class";
    }

    public static String classpath2name(String clazz) {
        return clazz.replace('/', '.');
    }

    /**
     * 根据包名扫描class
     * @param packageName
     * @return
     */
    public static List<Class> scanClass(String packageName) throws IOException, ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace(".", "/");
        Enumeration<URL> resources = classLoader.getResources(path);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());
            if (directory.exists()) {
                // 获取目录下所有的文件和子目录
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(".class")) {
                            // 获取类名
                            String className = packageName + "." + file.getName().replace(".class", "");
                            Class<?> clazz = Class.forName(className);
                            classes.add(clazz);
                        }
                    }
                }
            }
        }
        return classes;
    }
}
