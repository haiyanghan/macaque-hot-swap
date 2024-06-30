package six.eared.macaque.agent.asm2.enhance

import javassist.Modifier
import javassist.bytecode.AccessFlag
import six.eared.macaque.agent.asm2.AsmMethod
import six.eared.macaque.agent.asm2.AsmUtil
import six.eared.macaque.agent.asm2.classes.ClazzDefinition
import six.eared.macaque.agent.env.Environment
import six.eared.macaque.agent.exceptions.AccessorCreateException
import six.eared.macaque.agent.javassist.ClassBuilder
import six.eared.macaque.agent.javassist.JavaSsistUtil
import six.eared.macaque.asm.Type
import six.eared.macaque.common.util.CollectionUtil
import six.eared.macaque.common.util.StringUtil
import java.io.IOException

class CompatibilityModeAccessorUtil {

    companion object {
        private val CACHE: MutableMap<String, ClazzDefinition> = HashMap()

        /**
         * @param className          外部类类名
         * @param classNameGenerator 类名生成器
         * @param deepth             深度
         * @return
         */
        @JvmStatic
        fun createAccessor(className: String, classNameGenerator: ClassNameGenerator, deepth: Int): ClazzDefinition {
            if (CACHE[className] != null) {
                return CACHE[className]!!
            }

            var deepth = deepth
            val accessorClassName = classNameGenerator.generateInnerAccessorName(className)
            try {
                val outClazzDefinition = AsmUtil.readOriginClass(className)
                val superClassName = outClazzDefinition.superClassName
                var superAccessor: ClazzDefinition? = null
                if (deepth > 0) {
                    if (StringUtil.isNotEmpty(superClassName)
                        && !isSystemClass(superClassName)
                    ) {
                        superAccessor = createAccessor(superClassName, classNameGenerator, --deepth)
                    }
                }
                val superAccessorName: String? =
                    tryGetAccessorClassName(superClassName, classNameGenerator)
                val classBuilder: ClassBuilder =
                    generateAccessorClass(accessorClassName, outClazzDefinition, superAccessorName)

                collectAccessibleMethods(outClazzDefinition, classBuilder, superAccessor, classNameGenerator)
                collectAccessibleFields(outClazzDefinition, classBuilder, superAccessor)

                val byteArray = classBuilder.toByteArray()
                CompatibilityModeClassLoader.loadClass(classBuilder.className, byteArray)
                val definition = AsmUtil.readClass(byteArray)
                CACHE[className] = definition
                return definition
            } catch (e: Exception) {
                throw AccessorCreateException(e)
            }
        }


        /**
         * @param accessorClassName
         * @param definition
         * @param superAccessorName
         * @return
         */
        private fun generateAccessorClass(
            accessorClassName: String, definition: ClazzDefinition,
            superAccessorName: String?
        ): ClassBuilder {
            val outClassName = definition.className
            val containSupper = superAccessorName != null
            val classBuilder = JavaSsistUtil
                .defineClass(Modifier.PUBLIC, accessorClassName, superAccessorName, null)
            if (!containSupper) {
                classBuilder.defineField(Modifier.PUBLIC or AccessFlag.SYNTHETIC, "this$0", "java.lang.Object")
            }
            classBuilder.defineConstructor(
                Modifier.PUBLIC, arrayOf(outClassName),
                "{" + (if (containSupper) "super($1);" else "this.this$0 = $1;") + "}"
            )
            classBuilder.defineField("public static final MethodHandles\$Lookup LOOKUP = MethodHandles.lookup();")
            return classBuilder
        }

        /**
         * @param className
         * @param classNameGenerator
         * @return
         */
        private fun tryGetAccessorClassName(className: String, classNameGenerator: ClassNameGenerator): String? {
            val accessorName = classNameGenerator.generateInnerAccessorName(className)
            return if (CACHE[accessorName] != null) {
                accessorName
            } else null
        }

        private fun collectAccessibleMethods(
            definition: ClazzDefinition, accessorClassBuilder: ClassBuilder,
            superAccessor: ClazzDefinition?, classNameGenerator: ClassNameGenerator
        ) {
            try {
                val privateMethods: MutableSet<AsmMethod> = HashSet()
                val accessibleSuperMethods: MutableMap<String, AsmMethod> = HashMap()
                val outClassName = definition.className

                // my all method
                for (method in definition.asmMethods) {
                    if (method.isConstructor || method.isClinit) {
                        continue
                    }
                    // 私有方法
                    if (method.isPrivate) {
                        privateMethods.add(method)
                        continue
                    }
                    // 继承而来 （如果自己重写了父类的方法, 就保存父类的字节码，防止 super调用）
                    val inherited: Boolean = inherited(definition.superClassName, method.methodName, method.desc)
                    if (inherited && superAccessor != null) {
                        continue
                    }

                    // 不是继承而来的 或者 继承来的但是没有父accessor, 就生成方法调用
                    generateVirtualSpecial(method, accessorClassBuilder)
                }

                // non private method in super class
                val superClassName = definition.superClassName
                var superClassDefinition = AsmUtil.readOriginClass(superClassName)
                while (superClassDefinition != null) {
                    val items: Set<AsmMethod> = HashSet()
                    for (superMethod in superClassDefinition.asmMethods) {
                        if (superMethod.isConstructor || superMethod.isClinit) {
                            continue
                        }
                        // skip private
                        if (superMethod.isPrivate) {
                            continue
                        }
                        if (!accessibleSuperMethods.containsKey(superMethod.uniqueDesc)) {
                            accessibleSuperMethods.put(superMethod.uniqueDesc, superMethod)
                        }
                    }
                    if (superAccessor != null) {
                        break
                    }
                    if (superClassDefinition.className == "java.lang.Object" || superClassDefinition.superClassName == null) {
                        break
                    }
                    superClassDefinition = AsmUtil.readOriginClass(superClassDefinition.superClassName)
                }

                // default method in interface class
                if (Environment.getJdkVersion() > 7) {
                }

                // 对于私有方法的访问方式有两种：
                // 1. MethodHandler的方式（可维护性强）
                // 2. 将字节码绑定到新的类（性能好）
                if (CollectionUtil.isNotEmpty(privateMethods)) {
                    for (privateMethod in privateMethods) {
                        generateInvokeSpecial(accessorClassBuilder.className, privateMethod, accessorClassBuilder)
                    }
                }

                if (accessibleSuperMethods.size > 0) {
                    for ((_, superMethod) in accessibleSuperMethods) {
                        generateInvokeSpecial(accessorClassBuilder.className, superMethod, accessorClassBuilder)
                    }
                }
            } catch (e: Exception) {
                throw AccessorCreateException(e)
            }
        }

        private fun collectAccessibleFields(
            outClazzDefinition: ClazzDefinition?,
            classBuilder: ClassBuilder,
            superAccessor: ClazzDefinition?
        ) {
        }


        fun generateVirtualSpecial(method: AsmMethod, classBuilder: ClassBuilder) {
            val methodType = Type.getMethodType(method.desc)
            val methodName = method.methodName
            val rType = methodType.returnType.className
            val args = methodType.argumentTypes
            val argVars = args.indices.map { "var_${it}" }
            val argsType = args.map { it.className }

            classBuilder.defineMethod(
                """
                public $rType $methodName(${argsType.zip(argVars).joinToString(",") { it.first + " " + it.second }}) {
               ${if (rType != "void") "return ($rType)" else ""} ((${method.className}) this$0).$methodName(${argVars.joinToString(",")});
                } """
            )
        }

        fun generateInvokeSpecial(callerClass: String, method: AsmMethod, classBuilder: ClassBuilder) {
            val methodName = method.methodName
            val methodType = Type.getMethodType(method.desc)
            val rType = methodType.returnType.className
            val args = methodType.argumentTypes
            val argVars = args.indices.map { "var_${it}" }
            val argsType = args.map { it.className }

            classBuilder.defineMethod(
                """ 
            public $rType super_$methodName(${argsType.zip(argVars).joinToString(",") { it.first + " " + it.second }}) {
                MethodType type = MethodType.methodType($rType.class, new Class[]{${argsType.joinToString(",") { "$it.class" }}});
                MethodHandle mh = LOOKUP.findSpecial($callerClass.class, "$methodName", type, ${method.className}.class)
                    .bindTo(this$0);
            ${if (rType != "void") "return ($rType)" else ""} mh.invoke(new Object[] {${argVars.joinToString(",")}});
            }"""
            )
        }

        /**
         * @param superClass
         * @param methodName
         * @param methodDesc
         * @return 返回这个方法是否继承而来的方法
         */
        @Throws(ClassNotFoundException::class, IOException::class)
        private fun inherited(superClass: String, methodName: String, methodDesc: String): Boolean {
            var superClass = superClass
            while (StringUtil.isNotEmpty(superClass)
                && superClass != "java.lang.Object"
            ) {
                val definition = AsmUtil.readOriginClass(superClass)
                if (definition.hasMethod(methodName, methodDesc)) {
                    return true
                }
                superClass = definition.superClassName
            }
            return false
        }

        /**
         * 是否是系统类
         */
        fun isSystemClass(className: String): Boolean {
            if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("sun.")) {
                return true
            }
            if (className.contains(".internal.") || className.contains(".reflect.") || className.contains(".lang.")
                || className.contains(".io.") || className.contains(".net.")
            ) {
                return true
            }
            return if (className.contains("java$") || className.contains("javax$") || className.contains("sun$")) {
                true
            } else false
        }
    }
}