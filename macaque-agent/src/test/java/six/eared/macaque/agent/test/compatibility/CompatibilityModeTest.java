package six.eared.macaque.agent.test.compatibility;

import org.junit.Assert;
import org.junit.Test;
import six.eared.macaque.agent.hotswap.handler.ClassHotSwapHandler;
import six.eared.macaque.agent.test.EarlyClass;
import six.eared.macaque.agent.test.Env;
import six.eared.macaque.agent.test.asm.AsmMethodPrinter;
import six.eared.macaque.agent.test.asm.BinaryClassPrint;
import six.eared.macaque.asm.ClassVisitor;
import six.eared.macaque.common.ExtPropertyName;
import six.eared.macaque.common.util.FileUtil;
import six.eared.macaque.mbean.rmi.HotSwapRmiData;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static six.eared.macaque.agent.test.Utils.compileToClass;

public class CompatibilityModeTest extends Env {

    private ClassVisitor printer = new BinaryClassPrint(new AsmMethodPrinter());

    private EarlyClass earlyClass = new EarlyClass();

    @Test
    public void testAddNewSimpleMethod() {
        byte[] bytes = compileToClass("EarlyClass.java", FileUtil.is2bytes(CompatibilityModeTest.class.getClassLoader()
                .getResourceAsStream("AddNewSimpleMethod.java"))).get(0);
        ClassHotSwapHandler classHotSwapHandler = new ClassHotSwapHandler();
        classHotSwapHandler.handlerRequest(new HotSwapRmiData("class", bytes, compatibilityMode()));
        Assert.assertEquals(earlyClass.test2(), "_newMethod");
    }

    @Test
    public void testAddNewSimpleMethod2() throws IOException {
        byte[] bytes = compileToClass("EarlyClass.java", FileUtil.is2bytes(CompatibilityModeTest.class.getClassLoader()
                .getResourceAsStream("AddNewSimpleMethod2.java"))).get(0);
        ClassHotSwapHandler classHotSwapHandler = new ClassHotSwapHandler();
        classHotSwapHandler.handlerRequest(new HotSwapRmiData("class", bytes, compatibilityMode()));
        Assert.assertEquals(earlyClass.test2(), "_newMethod1");
    }


    @Test
    public void testAddNewStaticMethod() {
        byte[] bytes = compileToClass("EarlyClass.java", FileUtil.is2bytes(CompatibilityModeTest.class.getClassLoader()
                .getResourceAsStream("AddNewStaticMethod.java"))).get(0);
        ClassHotSwapHandler classHotSwapHandler = new ClassHotSwapHandler();
        classHotSwapHandler.handlerRequest(new HotSwapRmiData("class", bytes, compatibilityMode()));
        Assert.assertEquals(earlyClass.test3(), "test4");
    }

    @Test
    public void testInstanceMethod() {
        byte[] bytes = compileToClass("EarlyClass.java", FileUtil.is2bytes(CompatibilityModeTest.class.getClassLoader()
                .getResourceAsStream("AddNewInstanceMethod.java"))).get(0);
        ClassHotSwapHandler classHotSwapHandler = new ClassHotSwapHandler();
        classHotSwapHandler.handlerRequest(new HotSwapRmiData("class", bytes, compatibilityMode()));
        Assert.assertEquals(earlyClass.test3(), "test4");
    }

    public Map<String, String> compatibilityMode() {
        Map<String, String> extProperties = new HashMap<>();
        extProperties.put(ExtPropertyName.COMPATIBILITY_MODE, Boolean.TRUE.toString());
        return extProperties;
    }
}
