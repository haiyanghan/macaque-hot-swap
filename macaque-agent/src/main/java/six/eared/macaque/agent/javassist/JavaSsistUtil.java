package six.eared.macaque.agent.javassist;


import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

public class JavaSsistUtil {

    static ClassPool pool = ClassPool.getDefault();

    static {
        pool.importPackage("java.lang.invoke");
    }

    public static ClassBuilder defineClass(int modifier, String className, String superClass, String[] interfaces)
            throws NotFoundException, CannotCompileException {
        CtClass ctClass = pool.makeClass(className);

        ctClass.setModifiers(modifier);
        if (superClass != null) {
            ctClass.setSuperclass(pool.get(superClass));
        }
        if (interfaces != null && interfaces.length > 0) {
            CtClass[] ctClasses = new CtClass[interfaces.length];
            for (int i = 0; i < interfaces.length; i++) {
                ctClasses[i] = pool.get(interfaces[i]);
            }
            ctClass.setInterfaces(ctClasses);
        }
        return new ClassBuilder(pool, ctClass);
    }
}
