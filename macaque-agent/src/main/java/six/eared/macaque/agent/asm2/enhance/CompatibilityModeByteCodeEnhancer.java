package six.eared.macaque.agent.asm2.enhance;

import six.eared.macaque.agent.asm2.AsmMethod;
import six.eared.macaque.agent.asm2.AsmUtil;
import six.eared.macaque.agent.asm2.ClassBuilder;
import six.eared.macaque.agent.asm2.classes.AsmMethodVisitorCaller;
import six.eared.macaque.agent.asm2.classes.ClazzDefinition;
import six.eared.macaque.agent.asm2.classes.ClazzDefinitionVisitor;
import six.eared.macaque.agent.asm2.classes.MethodDynamicStackVisitor;
import six.eared.macaque.agent.vcs.VersionChainTool;
import six.eared.macaque.asm.MethodVisitor;
import six.eared.macaque.asm.Opcodes;
import six.eared.macaque.common.util.ClassUtil;
import six.eared.macaque.common.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class CompatibilityModeByteCodeEnhancer {

    private static final ClassNameGenerator CLASS_NAME_GENERATOR = new SimpleClassNameGenerator();

    public static void enhance(List<ClazzDefinition> definitions) throws IOException, ClassNotFoundException {
        for (ClazzDefinition definition : definitions) {
            // 准备
            prepare(definition);
        }
        for (ClazzDefinition definition : definitions) {
            // 转换
            bytecodeConvert(definition);
        }
    }

    private static void prepare(ClazzDefinition definition) throws IOException, ClassNotFoundException {
        ClazzDefinition accessor = createAccessor(AsmUtil.readOriginClass(definition.getClassName()));

        ClazzDefinition lastClassVersion = VersionChainTool.findLastClassVersion(definition.getClassName(), false);
        if (lastClassVersion == null) {
            lastClassVersion = AsmUtil.readOriginClass(definition.getClassName());
        }
        assert lastClassVersion != null;
        for (AsmMethod asmMethod : definition.getAsmMethods()) {
            // 已存在的方法
            if (lastClassVersion.hasMethod(asmMethod.getMethodName(), asmMethod.getDesc())) {
                AsmMethod method = lastClassVersion.getMethod(asmMethod.getMethodName(), asmMethod.getDesc());
                // 跳过构造函数和clinit
                if (method.isConstructor() || method.isClinit()) continue;
                // 跳过非私有方法
                if (!method.isPrivate()) continue;
                if (method.getMethodBindInfo() != null) {
                    asmMethod.setMethodBindInfo(method.getMethodBindInfo().clone());
                    continue;
                }
            }

            // 私有方法或者新方法。需要建立绑定关系
            String bindMethodName = asmMethod.getMethodName();
            String bindClassName = CLASS_NAME_GENERATOR.generate(definition.getClassName(), bindMethodName);

            MethodBindInfo methodBindInfo = new MethodBindInfo();
            methodBindInfo.setBindClass(bindClassName);
            methodBindInfo.setBindMethod(bindMethodName);
            methodBindInfo.setAccessorClass(accessor.getClassName());
            methodBindInfo.setVisitorCaller(new AsmMethodVisitorCaller());
            asmMethod.setMethodBindInfo(methodBindInfo);
        }
    }

    private static ClazzDefinition bytecodeConvert(ClazzDefinition definition) {
        Map<String, MethodBindInfo> bindMethods = definition.getAsmMethods().stream()
                .filter(item -> item.getMethodBindInfo() != null)
                .peek(item -> {
                    item.getMethodBindInfo()
                            .setBindMethodDesc(AsmUtil.addArgsDesc(item.getDesc(), item.getMethodBindInfo().getAccessorClass(), false));;
                })
                .collect(Collectors.toMap(AsmMethod::getUniqueDesc, AsmMethod::getMethodBindInfo));

        // 生成bind class和bind method
        if (bindMethods.size() > 0) {
            byte[] newByteCode = generateNewByteCode(definition.getByteCode(), bindMethods);
            for (Map.Entry<String, MethodBindInfo> entry : bindMethods.entrySet()) {
                MethodBindInfo bindInfo = entry.getValue();
                AsmMethod asmMethod = definition.getMethod(entry.getKey());
                ClassBuilder classBuilder = AsmUtil.defineClass(Opcodes.ACC_PUBLIC, bindInfo.getBindClass(), null, null, null)
                        .defineMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                                bindInfo.getBindMethod(), bindInfo.getBindMethodDesc(),
                                asmMethod.getExceptions(), asmMethod.getMethodSign())
                        .accept(method -> {
//                            Type methodType = Type.getMethodType(asmMethod.getDesc());
                            bindInfo.getVisitorCaller().accept(method);
                        })
                        .end();
                CompatibilityModeClassLoader.loadClass(bindInfo.getBindClass(), classBuilder.toByteArray());
            }
            FileUtil.writeBytes(
                    new File( FileUtil.getProcessTmpPath() + File.separator + ClassUtil.toSimpleName(definition.getClassName()) + ".class"),
                    newByteCode);
            definition.setByteCode(newByteCode);
        }

        return definition;
    }

    /**
     * 生成新的字节码
     */
    private static byte[] generateNewByteCode(byte[] bytecode, Map<String, MethodBindInfo> bindMethods) {
        ClazzDefinition definition = AsmUtil.readClass(bytecode, new ClazzDefinitionVisitor() {
            private String classDesc;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                this.classDesc = name;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                String uniqueDesc = name + "#" + desc;

                MethodBindInfo bindInfo = bindMethods.get(uniqueDesc);
                // 如果是不是新方法， 只需要改变对新方法的调用就行
                if (bindInfo == null) {
                    MethodVisitor writer = super.visitMethod(access, name, desc, signature, exceptions);
                    return new InvokeCodeConvertor(classDesc, writer, bindMethods);
                }

                MethodVisitor methodVisitor = bindInfo.getVisitorCaller().createProxyObj();
                return methodVisitor;
            }
        });
        return definition.getByteArray();
    }

    /**
     * 创建访问器
     *
     * @param definition
     */
    private static ClazzDefinition createAccessor(ClazzDefinition definition) {
        // 计算深度
        int depth = 3;
        ClazzDefinition accessor = CompatibilityModeAccessorUtil.createAccessor(definition.getClassName(), CLASS_NAME_GENERATOR, depth);
        return accessor;
    }

    /**
     * 改变调用指令的字节码转换器
     *
     */
    static class InvokeCodeConvertor extends MethodDynamicStackVisitor {
        private final String classDesc;
        private final Map<String, MethodBindInfo> newMethods;

        public InvokeCodeConvertor(String classDesc, MethodVisitor write, Map<String, MethodBindInfo> bindMethods) {
            super(write);
            this.classDesc = classDesc;
            this.newMethods = bindMethods;
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack+1, maxLocals);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            String uniqueDesc = name + "#" + desc;
            if (newMethods.containsKey(uniqueDesc)) {
                if (opcode != Opcodes.INVOKESTATIC) {
                    // 需要将this弹出
                    super.visitVarInsn(Opcodes.ASTORE, 0);
                }
                MethodBindInfo bindInfo = newMethods.get(uniqueDesc);
                String accessorDesc = ClassUtil.simpleClassName2path(bindInfo.getAccessorClass());
                super.visitTypeInsn(Opcodes.NEW, accessorDesc);
                super.visitInsn(Opcodes.DUP);
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitMethodInsn(Opcodes.INVOKESPECIAL, accessorDesc, "<init>", "(L"+classDesc+";)V", false);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, ClassUtil.simpleClassName2path(bindInfo.getBindClass()), bindInfo.getBindMethod(),
                        bindInfo.getBindMethodDesc(), itf);
                return;
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
