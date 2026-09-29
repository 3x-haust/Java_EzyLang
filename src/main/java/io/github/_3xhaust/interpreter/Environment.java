package io.github._3xhaust.interpreter;

import java.util.HashMap;

public class Environment {
    public record TypeSpec(String name, boolean isArray) {}

    public record ModuleVarRef(Scope scope, String name) {}

    public static final class Scope {
        final HashMap<String, Object> variables = new HashMap<>();
        final HashMap<String, Object> constants = new HashMap<>();
        final HashMap<String, Object> constraints = new HashMap<>();
        final HashMap<String, TypeSpec> types = new HashMap<>();
        final Scope parent;

        Scope(Scope parent) {
            this.parent = parent;
        }
    }

    private final Scope global = new Scope(null);
    private Scope current = global;

    public Scope global() {
        return global;
    }

    public Scope current() {
        return current;
    }

    public boolean isGlobal() {
        return current == global;
    }

    public void enterScope() {
        current = new Scope(current);
    }

    public void exitScope() {
        if (current.parent != null) current = current.parent;
    }

    public Scope enterCall(Scope closure) {
        Scope saved = current;
        current = new Scope(closure);
        return saved;
    }

    public void restore(Scope saved) {
        current = saved;
    }

    private Scope resolveVariable(String name) {
        for (Scope s = current; s != null; s = s.parent) {
            if (s.variables.containsKey(name)) return s;
        }
        return null;
    }

    private Scope resolveConstant(String name) {
        for (Scope s = current; s != null; s = s.parent) {
            if (s.constants.containsKey(name)) return s;
        }
        return null;
    }

    public boolean hasVariable(String name) {
        return resolveVariable(name) != null;
    }

    public Object findVariable(String name) {
        Scope s = resolveVariable(name);
        if (s == null) return null;
        Object value = s.variables.get(name);
        if (value instanceof ModuleVarRef ref) return ref.scope().variables.get(ref.name());
        return value;
    }

    public boolean hasConstant(String name) {
        return resolveConstant(name) != null;
    }

    public Object findConstant(String name) {
        Scope s = resolveConstant(name);
        return s == null ? null : s.constants.get(name);
    }

    public void setVariable(String name, Object value) {
        current.variables.put(name, value);
    }

    public void setConstant(String name, Object value) {
        current.constants.put(name, value);
    }

    public boolean hasVariableInCurrentScope(String name) {
        return current.variables.containsKey(name);
    }

    public boolean hasConstantInCurrentScope(String name) {
        return current.constants.containsKey(name);
    }

    public Object findConstraint(String name) {
        Scope s = resolveVariable(name);
        if (s == null) return null;
        if (s.variables.get(name) instanceof ModuleVarRef ref) return ref.scope().constraints.get(ref.name());
        return s.constraints.get(name);
    }

    public void setConstraint(String name, Object value) {
        current.constraints.put(name, value);
    }

    public TypeSpec findType(String name) {
        Scope s = resolveVariable(name);
        if (s == null) return null;
        if (s.variables.get(name) instanceof ModuleVarRef ref) return ref.scope().types.get(ref.name());
        return s.types.get(name);
    }

    public void setType(String name, TypeSpec type) {
        current.types.put(name, type);
    }

    public void updateVariable(String name, Object value) {
        Scope s = resolveVariable(name);
        if (s == null) return;
        if (s.variables.get(name) instanceof ModuleVarRef ref) {
            ref.scope().variables.put(ref.name(), value);
            return;
        }
        s.variables.put(name, value);
    }

    public void removeVariable(String name) {
        current.variables.remove(name);
    }

    public HashMap<String, Object> getTopVariableScope() {
        return current.variables;
    }

    public HashMap<String, Object> getTopConstantScope() {
        return current.constants;
    }

    public HashMap<String, Object> getGlobalVariableScope() {
        return global.variables;
    }

    public HashMap<String, Object> getGlobalConstantScope() {
        return global.constants;
    }
}
