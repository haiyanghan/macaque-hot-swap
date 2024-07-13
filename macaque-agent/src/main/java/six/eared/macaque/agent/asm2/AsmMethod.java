package six.eared.macaque.agent.asm2;


import lombok.Data;
import org.objectweb.asm.Opcodes;
import six.eared.macaque.agent.enhance.MethodBindInfo;

import java.util.Objects;


/**
 *
 */
@Data
public class AsmMethod {

    private String className;

    private int modifier;

    private String methodName;

    private String methodSign;

    private String desc;

    private String[] exceptions;

    private MethodBindInfo bindInfo;

    public boolean isPrivate() {
        return (this.modifier & Opcodes.ACC_PRIVATE) > 0;
    }

    public boolean isStatic() {
        return (this.modifier & Opcodes.ACC_STATIC) > 0;
    }

    public boolean isConstructor() {
        return this.methodName.equals("<init>");
    }

    public boolean isClinit() {
        return this.methodName.equals("<clinit>");
    }

    public String getUniqueDesc() {
        return this.methodName+"#"+this.desc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AsmMethod asmMethod = (AsmMethod) o;
        return Objects.equals(methodName, asmMethod.methodName) && Objects.equals(desc, asmMethod.desc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName, desc);
    }

    public static final class AsmMethodBuilder {
        private String className;
        private int modifier;
        private String methodName;
        private String methodSign;
        private String desc;
        private String[] exceptions;

        private AsmMethodBuilder() {
        }

        public static AsmMethodBuilder builder() {
            return new AsmMethodBuilder();
        }

        public AsmMethodBuilder className(String className) {
            this.className = className;
            return this;
        }

        public AsmMethodBuilder modifier(int modifier) {
            this.modifier = modifier;
            return this;
        }

        public AsmMethodBuilder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public AsmMethodBuilder methodSign(String methodSign) {
            this.methodSign = methodSign;
            return this;
        }

        public AsmMethodBuilder desc(String desc) {
            this.desc = desc;
            return this;
        }

        public AsmMethodBuilder exceptions(String[] exceptions) {
            this.exceptions = exceptions;
            return this;
        }

        public AsmMethod build() {
            AsmMethod asmMethod = new AsmMethod();
            asmMethod.className = this.className;
            asmMethod.modifier = this.modifier;
            asmMethod.methodName = this.methodName;
            asmMethod.methodSign = this.methodSign;
            asmMethod.desc = this.desc;
            asmMethod.exceptions = this.exceptions;
            return asmMethod;
        }
    }
}
