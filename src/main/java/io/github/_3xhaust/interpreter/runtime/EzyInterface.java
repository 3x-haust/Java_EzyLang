package io.github._3xhaust.interpreter.runtime;

import io.github._3xhaust.ezylang.ast.Ast.FunctionDecl;

import java.util.List;

public class EzyInterface {
    private final String name;
    private final List<FunctionDecl> methods;

    public EzyInterface(String name, List<FunctionDecl> methods) {
        this.name = name;
        this.methods = methods;
    }

    public String getName() { return name; }
    public List<FunctionDecl> getMethods() { return methods; }

    @Override
    public String toString() { return "interface " + name; }
}
