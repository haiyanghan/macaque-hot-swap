package six.eared.macaque.agent.javassist;

import javassist.*;

import java.io.IOException;

public class ClassBuilder {
    private ClassPool pool;
    private CtClass ctClass;

    public ClassBuilder(ClassPool pool, CtClass ctClass) {
        this.pool = pool;
        this.ctClass = ctClass;
    }

    public String getClassName() {
        return ctClass.getName();
    }

    public ClassBuilder defineField(String src) throws CannotCompileException, NotFoundException {
        this.ctClass.addField(CtField.make(src, this.ctClass));
        return this;
    }

    public ClassBuilder defineField(int modifier, String fieldName, String fieldType) throws CannotCompileException, NotFoundException {
        CtField field = new CtField(pool.get(fieldType), fieldName, this.ctClass);
        field.setModifiers(modifier);
        this.ctClass.addField(field);
        return this;
    }

    public ClassBuilder defineConstructor(String src) throws CannotCompileException, NotFoundException {
        this.ctClass.addConstructor(CtNewConstructor.make(src, ctClass));
        return this;
    }

    public ClassBuilder defineConstructor(int modifier, String[] parameters, String body) throws CannotCompileException, NotFoundException {
        CtConstructor constructor = new CtConstructor(null, this.ctClass);
        constructor.setModifiers(modifier);
        if (parameters != null) {
            for (String paramClass : parameters) {
                constructor.addParameter(pool.get(paramClass));
            }
        }
        constructor.setBody(body);

        this.ctClass.addConstructor(constructor);
        return this;
    }

    public ClassBuilder defineMethod(String src) throws CannotCompileException {
        this.ctClass.addMethod(CtMethod.make(src, this.ctClass));
        return this;
    }

    public byte[] toByteArray() throws IOException, CannotCompileException {
        try {
            return this.ctClass.toBytecode();
        } finally {
            this.ctClass.detach();
        }
    }
}
