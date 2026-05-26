package io.github._3xhaust.interpreter.runtime;

import io.github._3xhaust.ezylang.ast.Ast.ClassField;
import io.github._3xhaust.ezylang.ast.Ast.Decorator;
import io.github._3xhaust.ezylang.ast.Ast.FunctionDecl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EzyClass {
    private final String name;
    private final List<ClassField> constructorParams;
    private final EzyClass parentClass;
    private final List<String> interfaces;
    private final List<ClassField> fields;
    private final Map<String, FunctionDecl> methods = new HashMap<>();
    private final List<Decorator> decorators;

    public EzyClass(String name, List<ClassField> constructorParams, EzyClass parentClass,
                    List<String> interfaces, List<ClassField> fields, List<FunctionDecl> methods,
                    List<Decorator> decorators) {
        this.name = name;
        this.constructorParams = constructorParams;
        this.parentClass = parentClass;
        this.interfaces = interfaces;
        this.fields = fields;
        this.decorators = decorators;
        for (FunctionDecl method : methods) {
            this.methods.put(method.getName(), method);
        }
    }

    public String getName() { return name; }
    public List<ClassField> getConstructorParams() { return constructorParams; }
    public EzyClass getParentClass() { return parentClass; }
    public List<String> getInterfaces() { return interfaces; }
    public List<ClassField> getFields() { return fields; }
    public Map<String, FunctionDecl> getMethods() { return methods; }
    public List<Decorator> getDecorators() { return decorators; }

    public FunctionDecl findMethod(String name) {
        FunctionDecl method = methods.get(name);
        if (method != null) return method;
        if (parentClass != null) return parentClass.findMethod(name);
        return null;
    }

    public boolean hasDecorator(String name) {
        return decorators.stream().anyMatch(d -> d.getName().equals(name));
    }

    public boolean isSubclassOf(EzyClass other) {
        if (this == other) return true;
        if (parentClass != null) return parentClass.isSubclassOf(other);
        return false;
    }

    public boolean implementsInterface(String interfaceName) {
        if (interfaces.contains(interfaceName)) return true;
        if (parentClass != null) return parentClass.implementsInterface(interfaceName);
        return false;
    }

    @Override
    public String toString() { return "class " + name; }
}
