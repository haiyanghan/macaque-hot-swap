package six.eared.macaque.agent.asm2.classes;

import six.eared.macaque.agent.asm2.AsmField;
import six.eared.macaque.agent.asm2.AsmMethod;
import six.eared.macaque.agent.asm2.AsmUtil;
import six.eared.macaque.agent.definition.Definition;
import six.eared.macaque.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.List;

public class ClazzDefinition implements Cloneable, Definition {

    private String className;

    private String superClassName;

    private String[] interfaces;

    private byte[] originData;

    private byte[] byteCode;

    private List<CorrelationClazzDefinition> correlationClasses;

    private List<AsmMethod> asmMethods = new ArrayList<>();

    private List<AsmField> asmFields = new ArrayList<>();

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void addAsmMethod(AsmMethod method) {
        this.asmMethods.add(method);
    }

    public List<AsmMethod> getAsmMethods() {
        return asmMethods;
    }

    public void addAsmField(AsmField asmField) {
        asmFields.add(asmField);
    }

    public List<AsmField> getAsmFields() {
        return asmFields;
    }

    public byte[] getByteCode() {
        return byteCode;
    }

    public void setByteCode(byte[] byteCode) {
        this.byteCode = byteCode;
    }

    @Override
    public ClazzDefinition clone() {
        try {
            return (ClazzDefinition) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getOriginData() {
        return originData;
    }

    public void setOriginData(byte[] originData) {
        this.originData = originData;
    }

    public boolean hasMethod(String name, String desc) {
        return asmMethods.stream()
                .anyMatch(item -> item.getMethodName().equals(name) && item.getDesc().equals(desc));
    }

    public AsmMethod getMethod(String name, String desc) {
        return asmMethods.stream()
                .filter(item -> item.getMethodName().equals(name) && item.getDesc().equals(desc))
                .findAny().get();
    }

    public AsmMethod getMethod(String uniqueDesc) {
        return asmMethods.stream()
                .filter(item -> item.getUniqueDesc().equals(uniqueDesc))
                .findAny().get();
    }

    @Override
    public String getName() {
        return className;
    }

    @Override
    public String getFileType() {
        return "class";
    }

    @Override
    public byte[] getByteArray() {
        return byteCode;
    }

    public void setSuperClassName(String superName) {
        this.superClassName = superName;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public void setInterfaces(String[] interfaces) {
        this.interfaces = interfaces;
    }

    public String[] getInterfaces() {
        return interfaces;
    }

    public void revisit(ClassVisitor classVisitor) {
        AsmUtil.visitClass(this.byteCode, classVisitor);
    }

    public List<CorrelationClazzDefinition> getCorrelationClasses() {
        return correlationClasses;
    }

    public void putCorrelationClass(CorrelationClazzDefinition correlationClass) {
        if (this.correlationClasses == null) {
            this.correlationClasses = new ArrayList<>();
        }
        this.correlationClasses.add(correlationClass);
    }
}
