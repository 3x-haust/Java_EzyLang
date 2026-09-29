package io.github._3xhaust.interpreter.runtime;

import io.github._3xhaust.ezylang.ast.Ast.FunctionDecl;
import io.github._3xhaust.interpreter.Environment;
import io.github._3xhaust.interpreter.Interpreter;

public record EzyFunction(FunctionDecl decl, Environment.Scope closure, Interpreter owner, EzyInstance boundInstance, EzyClass boundClass) {
    public EzyFunction(FunctionDecl decl, Environment.Scope closure, Interpreter owner) {
        this(decl, closure, owner, null, null);
    }

    @Override
    public String toString() {
        return "<func " + decl.getName() + ">";
    }
}
