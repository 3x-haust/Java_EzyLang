package io.github._3xhaust.interpreter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github._3xhaust.ezylang.ast.Ast.*;
import io.github._3xhaust.ezylang.exception.ParseException;
import io.github._3xhaust.ezylang.lexer.Lexer;
import io.github._3xhaust.ezylang.lexer.Token;
import io.github._3xhaust.ezylang.parser.Parser;
import io.github._3xhaust.interpreter.module.*;

import io.github._3xhaust.interpreter.runtime.EzyClass;
import io.github._3xhaust.interpreter.runtime.EzyFunction;
import io.github._3xhaust.interpreter.runtime.EzyInstance;
import io.github._3xhaust.interpreter.runtime.Num;
import io.github._3xhaust.interpreter.runtime.ReadOnlyList;
import io.github._3xhaust.interpreter.runtime.EzyInterface;

public class Interpreter implements Visitor<Object> {
    private final Map<String, FunctionDecl> functions = new HashMap<>();
    private final Map<String, NativeFunction> nativeFunctions = new HashMap<>();
    private final Map<String, Interpreter> loadedModules = new HashMap<>();
    private static final java.util.Set<String> BUILTIN_MODULES = java.util.Set.of("math", "str", "arr", "io", "os", "time", "http", "net", "json");
    private final String fileName;
    private final List<String> lines;
    private final Environment env = new Environment();
    private final Map<Object, Map<List<Object>, Object>> memoCache = new HashMap<>();
    private final java.util.Set<String> importedModules = new java.util.HashSet<>();
    private final Map<String, EzyClass> classes = new HashMap<>();
    private final Map<String, EzyInterface> interfaces = new HashMap<>();
    private final Map<String, DecoratorDecl> customDecorators = new HashMap<>();
    private EzyInstance currentInstance = null;
    private EzyClass currentClass = null;
    private boolean isModule = false;
    private boolean testMode = false;
    private int testsPassed = 0;
    private int testsFailed = 0;
    private int functionDepth = 0;
    private final Map<String, ClassDecl> classDecls = new HashMap<>();
    private static final java.util.Set<String> MUTATING_LIST_METHODS = java.util.Set.of("clear", "addAll", "remove", "removeAt", "reverse", "sort", "shuffle");
    private static final java.util.Set<String> BUILTIN_TYPES = java.util.Set.of("number", "string", "char", "boolean", "func", "any", "void");
    private static final java.util.Set<String> BUILTIN_FUNCTION_DECORATORS = java.util.Set.of("memo", "log", "timer", "deprecated", "guard", "test", "override");
    private static final java.util.Set<String> BUILTIN_CLASS_DECORATORS = java.util.Set.of("data", "getter", "setter", "toString");
    private Map<String, Interpreter> moduleRegistry = new HashMap<>();
    private java.util.Set<String> loadingModules = new java.util.HashSet<>();

    public Interpreter(String fileName, String sourceCode) {
        this.fileName = fileName;
        this.lines = List.of(sourceCode.split("\n"));
        registerNativeFunctions();
    }

    public void setTestMode(boolean testMode) { this.testMode = testMode; }
    public int getTestsPassed() { return testsPassed; }
    public int getTestsFailed() { return testsFailed; }

    public void interpret(Program program) throws ParseException {
        for (Node statement : program.getStatements()) {
            if (statement instanceof FunctionDecl || statement instanceof ClassDecl
                    || statement instanceof InterfaceDecl || statement instanceof DecoratorDecl) {
                statement.accept(this);
            }
        }

        for (Node statement : program.getStatements()) {
            if (statement instanceof FunctionDecl function) {
                validateFunctionDeclaration(function);
            } else if (statement instanceof ClassDecl classDecl) {
                for (Decorator decorator : classDecl.getDecorators()) {
                    if (!BUILTIN_CLASS_DECORATORS.contains(decorator.getName())) {
                        throw error(classDecl, "Unknown class decorator '@" + decorator.getName() + "'");
                    }
                }
                for (ClassField field : classDecl.getConstructorParams()) {
                    requireKnownType(field.getType().getValue(), classDecl, this);
                }
                for (ClassField field : classDecl.getFields()) {
                    requireKnownType(field.getType().getValue(), classDecl, this);
                }
                for (FunctionDecl method : classDecl.getMethods()) {
                    validateFunctionDeclaration(method);
                }
            }
        }

        for (Node statement : program.getStatements()) {
            if (!(statement instanceof FunctionDecl) && !(statement instanceof ClassDecl)
                    && !(statement instanceof InterfaceDecl) && !(statement instanceof DecoratorDecl)) {
                statement.accept(this);
            }
        }

        if (testMode) {
            for (Map.Entry<String, FunctionDecl> entry : functions.entrySet()) {
                FunctionDecl func = entry.getValue();
                if (hasDecorator(func, "test")) {
                    String testName = getDecoratorStringArg(func, "test");
                    if (testName == null) testName = entry.getKey();
                    Environment.Scope saved = env.enterCall(env.global());
                    functionDepth++;
                    try {
                        func.getBody().accept(this);
                        testsPassed++;
                        System.out.println("[PASS] " + testName);
                    } catch (ReturnException e) {
                        testsPassed++;
                        System.out.println("[PASS] " + testName);
                    } catch (ParseException e) {
                        testsFailed++;
                        System.out.println("[FAIL] " + testName + " - " + e.getMessage());
                    } finally {
                        functionDepth--;
                        env.restore(saved);
                    }
                }
            }
        }
    }

    private void validateFunctionDeclaration(FunctionDecl function) throws ParseException {
        if (function.getDecorators() != null) {
            for (Decorator decorator : function.getDecorators()) {
                if (!BUILTIN_FUNCTION_DECORATORS.contains(decorator.getName()) && !customDecorators.containsKey(decorator.getName())) {
                    throw error(function, "Unknown decorator '@" + decorator.getName() + "'");
                }
            }
        }
        for (Token type : function.getParamTypes()) {
            requireKnownType(type.getValue(), function, this);
        }
        if (function.getReturnType() != null) {
            requireKnownType(function.getReturnType().getValue(), function, this);
        }
    }

    private boolean isKnownType(String type) {
        return BUILTIN_TYPES.contains(type) || classes.containsKey(type) || interfaces.containsKey(type);
    }

    private void requireKnownType(String type, Node node, Interpreter site) throws ParseException {
        if (!isKnownType(type)) {
            throw site.error(node, "Unknown type '" + type + "'");
        }
    }

    private Interpreter loadModule(String moduleName, Node importNode) throws ParseException {
        if (loadedModules.containsKey(moduleName)) {
            return loadedModules.get(moduleName);
        }

        if (BUILTIN_MODULES.contains(moduleName)) {
            Interpreter moduleInterpreter = new Interpreter(moduleName + ".ezy", "");
            moduleInterpreter.isModule = true;
            registerModuleNatives(moduleName, moduleInterpreter);
            loadedModules.put(moduleName, moduleInterpreter);
            return moduleInterpreter;
        }

        Path currentDir = Paths.get(fileName).toAbsolutePath().getParent();
        Path localPath = (currentDir != null ? currentDir.resolve(moduleName + ".ezy") : Paths.get(moduleName + ".ezy")).toAbsolutePath().normalize();
        String key = localPath.toString();

        Interpreter cached = moduleRegistry.get(key);
        if (cached != null) {
            loadedModules.put(moduleName, cached);
            return cached;
        }
        if (loadingModules.contains(key)) {
            throw error(importNode, "Circular import of module '" + moduleName + "'");
        }
        if (!Files.exists(localPath)) {
            throw error(importNode, "Module '" + moduleName + "' not found");
        }

        String sourceCode;
        try {
            sourceCode = new String(Files.readAllBytes(localPath), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw error(importNode, "Failed to read module '" + moduleName + "': " + e.getMessage());
        }

        String modulePath = localPath.toString();
        Program program = new Parser(modulePath, sourceCode, new Lexer(sourceCode).scanTokens()).parse();

        Interpreter moduleInterpreter = new Interpreter(modulePath, sourceCode);
        moduleInterpreter.isModule = true;
        moduleInterpreter.moduleRegistry = moduleRegistry;
        moduleInterpreter.loadingModules = loadingModules;
        loadingModules.add(key);
        try {
            moduleInterpreter.interpret(program);
        } finally {
            loadingModules.remove(key);
        }
        moduleRegistry.put(key, moduleInterpreter);
        loadedModules.put(moduleName, moduleInterpreter);
        return moduleInterpreter;
    }

    private void registerNativeFunctions() {
    }

    private void registerModuleNatives(String moduleName, Interpreter moduleInterpreter) {
        switch (moduleName) {
            case "math" -> {
                MathModule.register(moduleInterpreter.nativeFunctions);
                MathModule.registerConstants(moduleInterpreter.env.getGlobalConstantScope());
            }
            case "str" -> StrModule.register(moduleInterpreter.nativeFunctions);
            case "arr" -> ArrModule.register(moduleInterpreter.nativeFunctions);
            case "io" -> IoModule.register(moduleInterpreter.nativeFunctions);
            case "os" -> {
                OsModule.register(moduleInterpreter.nativeFunctions);
                OsModule.registerConstants(moduleInterpreter.env.getGlobalConstantScope());
            }
            case "time" -> TimeModule.register(moduleInterpreter.nativeFunctions);
            case "http" -> {
                HttpModule.register(moduleInterpreter.nativeFunctions);
                HttpModule.setDispatcher(args -> {
                    String funcName = String.valueOf(args.get(0));
                    FunctionDecl func = functions.get(funcName);
                    if (func == null) throw error(null, "Handler function '" + funcName + "' not found");
                    Environment.Scope saved = env.enterCall(env.global());
                    functionDepth++;
                    try {
                        List<String> paramNames = func.getParamNames();
                        for (int i = 0; i < paramNames.size(); i++) {
                            setVariable(paramNames.get(i), i + 1 < args.size() ? args.get(i + 1) : null);
                        }
                        func.getBody().accept(this);
                    } catch (ReturnException e) {
                        return e.getValue();
                    } finally {
                        functionDepth--;
                        env.restore(saved);
                    }
                    return null;
                });
            }
            case "net" -> NetModule.register(moduleInterpreter.nativeFunctions);
            case "json" -> JsonModule.register(moduleInterpreter.nativeFunctions);
        }
    }

    private String formatValue(Object value) {
        if (value == null) return "null";
        if (Num.isNumber(value)) return Num.format(value);
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatValue(list.get(i)));
            }
            return sb.append("]").toString();
        }
        return String.valueOf(value);
    }

    private boolean matchesType(String type, boolean isArray, Object value) {
        if (value == null) return true;
        if (isArray) {
            if (!(value instanceof List<?> list)) return false;
            for (Object element : list) {
                if (!matchesType(type, false, element)) return false;
            }
            return true;
        }
        if (type.equals("any")) return true;
        return switch (type) {
            case "number" -> Num.isNumber(value);
            case "string" -> value instanceof String;
            case "char" -> value instanceof String s && s.codePointCount(0, s.length()) == 1;
            case "boolean" -> value instanceof Boolean;
            case "func" -> value instanceof EzyFunction || value instanceof NativeFunction;
            default -> {
                EzyClass cls = classes.get(type);
                if (cls != null) yield value instanceof EzyInstance instance && instance.isInstanceOf(cls);
                if (interfaces.containsKey(type)) yield value instanceof EzyInstance instance && instance.isInstanceOfInterface(type);
                yield false;
            }
        };
    }

    private static String typeLabel(String type, boolean isArray) {
        return isArray ? type + "[]" : type;
    }

    private boolean valuesEqual(Object left, Object right) {
        if (left == null || right == null) return left == right;
        if (Num.isNumber(left) && Num.isNumber(right)) return Num.equal(left, right);
        if (left instanceof List<?> l && right instanceof List<?> r) {
            if (l.size() != r.size()) return false;
            for (int i = 0; i < l.size(); i++) {
                if (!valuesEqual(l.get(i), r.get(i))) return false;
            }
            return true;
        }
        return left.equals(right);
    }

    private int indexOfValue(List<?> list, Object target, boolean last) {
        if (last) {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (valuesEqual(list.get(i), target)) return i;
            }
            return -1;
        }
        for (int i = 0; i < list.size(); i++) {
            if (valuesEqual(list.get(i), target)) return i;
        }
        return -1;
    }

    private int compareValues(Object left, Object right, Token operator, Node node) throws ParseException {
        if (Num.isNumber(left) && Num.isNumber(right)) return Num.compare(left, right);
        if (left instanceof String l && right instanceof String r) return l.compareTo(r);
        throw error(node, "Operands of '" + operator.getValue() + "' must be two numbers or two strings");
    }

    private boolean compareOp(Token operator, Object left, Object right, Node node) throws ParseException {
        Token.TokenType kind = operator.getToken();
        if (kind != Token.TokenType.EQUAL_EQUAL && kind != Token.TokenType.NOT_EQUAL && (Num.isNaN(left) || Num.isNaN(right))) {
            compareValues(left, right, operator, node);
            return false;
        }
        return switch (kind) {
            case EQUAL_EQUAL -> valuesEqual(left, right);
            case NOT_EQUAL -> !valuesEqual(left, right);
            case LESS_THAN -> compareValues(left, right, operator, node) < 0;
            case GREATER_THAN -> compareValues(left, right, operator, node) > 0;
            case LESS_THAN_OR_EQUAL -> compareValues(left, right, operator, node) <= 0;
            case GREATER_THAN_OR_EQUAL -> compareValues(left, right, operator, node) >= 0;
            default -> throw error(node, "Unknown comparison operator: " + operator.getValue());
        };
    }

    private void requireNumbers(Object left, Object right, Token operator, Node node) throws ParseException {
        if (!Num.isNumber(left) || !Num.isNumber(right)) {
            throw error(node, "Operands of '" + operator.getValue() + "' must be numbers");
        }
    }

    private int toIndex(Object value, Node node) throws ParseException {
        if (!(value instanceof Long l)) {
            throw error(node, "Index must be an integer, got " + (Num.isNumber(value) ? formatValue(value) : getTypeName(value)));
        }
        if (l < 0 || l > Integer.MAX_VALUE) return -1;
        return (int) (long) l;
    }

    private String charAtCodePoint(String str, int index, Node node) throws ParseException {
        int count = str.codePointCount(0, str.length());
        if (index < 0 || index >= count) {
            throw error(node, "String index out of bounds");
        }
        int offset = str.offsetByCodePoints(0, index);
        return new String(Character.toChars(str.codePointAt(offset)));
    }

    private Object indexInto(Object container, Object index, Node node) throws ParseException {
        if (container instanceof String str) {
            return charAtCodePoint(str, toIndex(index, node), node);
        }
        if (container instanceof List<?> list) {
            int idx = toIndex(index, node);
            if (idx < 0 || idx >= list.size()) {
                throw error(node, "Array index out of bounds");
            }
            return list.get(idx);
        }
        throw error(node, "Value of type '" + getTypeName(container) + "' cannot be indexed");
    }

    private Object callNative(NativeFunction nativeFunction, List<Object> args, String name, Node node) throws ParseException {
        try {
            return Num.deepNorm(nativeFunction.execute(args));
        } catch (ParseException e) {
            if (e.getLine() > 0) throw e;
            throw error(node, e.getMessage());
        } catch (ReturnException e) {
            throw e;
        } catch (ClassCastException e) {
            throw error(node, "Invalid argument type for '" + name + "'");
        } catch (IndexOutOfBoundsException e) {
            throw error(node, "Invalid arguments for '" + name + "'");
        } catch (NullPointerException e) {
            throw error(node, "Unexpected null argument for '" + name + "'");
        } catch (RuntimeException e) {
            throw error(node, "'" + name + "' failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @Override
    public Object visitIndexExpr(IndexExpr indexExpr) throws ParseException {
        Object container = indexExpr.getTarget().accept(this);
        Object index = indexExpr.getIndex().accept(this);
        return indexInto(container, index, indexExpr);
    }

    private void enterScope() { env.enterScope(); }
    private void exitScope() { env.exitScope(); }
    private Object findVariable(String name) { return env.findVariable(name); }
    private Object findConstant(String name) { return env.findConstant(name); }
    private void setVariable(String name, Object value) { env.setVariable(name, value); }
    private void setConstant(String name, Object value) { env.setConstant(name, value); }

    @Override
    public Object visitProgram(Program program) throws ParseException {
        Object result = null;
        for (Node statement : program.getStatements()) {
            result = statement.accept(this);
        }
        return result;
    }

    @Override
    public Object visitVariableDecl(VariableDecl variableDecl) throws ParseException {
        String identifier = variableDecl.getIdentifier();
        Object value = null;
        if (env.hasVariableInCurrentScope(identifier)) {
            throw error(variableDecl, "Variable '" + identifier + "' is already defined in this scope");
        }
        if (variableDecl.getInitializer() != null) {
            value = variableDecl.getInitializer().accept(this);
        }

        String typeName = variableDecl.getType().getValue();
        boolean isArray = variableDecl.isArray();
        requireKnownType(typeName, variableDecl, this);
        if (!matchesType(typeName, isArray, value)) {
            throw error(variableDecl, "Type mismatch: expected " + typeLabel(typeName, isArray) + " but got " + getTypeName(value));
        }

        if (variableDecl.getConstraint() != null) {
            Constraint c = variableDecl.getConstraint();
            if (c.isRange()) {
                double min = Num.toDouble(c.getMin().accept(this));
                double max = Num.toDouble(c.getMax().accept(this));
                env.setConstraint(identifier, new double[]{min, max});
                if (value != null) validateConstraint(identifier, value, variableDecl);
            } else if (c.isEnum()) {
                List<Object> allowed = new ArrayList<>();
                for (Node node : c.getAllowedValues()) {
                    allowed.add(node.accept(this));
                }
                env.setConstraint(identifier, allowed);
                if (value != null) validateConstraint(identifier, value, variableDecl);
            }
        }

        setVariable(identifier, value);
        env.setType(identifier, new Environment.TypeSpec(typeName, isArray));
        return null;
    }

    private void validateConstraint(String varName, Object value, Node errorNode) throws ParseException {
        Object constraint = env.findConstraint(varName);
        if (constraint == null) return;

        if (constraint instanceof double[] range) {
            if (!Num.isNumber(value)) throw error(errorNode, "Expected number for constrained variable '" + varName + "'");
            double v = Num.toDouble(value);
            if (v < range[0] || v > range[1]) {
                throw error(errorNode, "Value " + formatValue(value) + " is out of range " + Num.format(range[0]) + ".." + Num.format(range[1]) + " for variable '" + varName + "'");
            }
        } else if (constraint instanceof List<?> allowedValues) {
            boolean allowed = false;
            for (Object candidate : allowedValues) {
                if (valuesEqual(candidate, value)) allowed = true;
            }
            if (!allowed) {
                throw error(errorNode, "Value '" + value + "' is not allowed for variable '" + varName + "'. Allowed: " + allowedValues);
            }
        }
    }

    @Override
    public Object visitConstantDecl(ConstantDecl constantDecl) throws ParseException {
        String identifier = constantDecl.getIdentifier();
        if (env.hasConstantInCurrentScope(identifier)) {
            throw error(constantDecl, "Constant '" + identifier + "' is already defined in this scope");
        }
        Object value = constantDecl.getValue().accept(this);
        String typeName = constantDecl.getType().getValue();
        requireKnownType(typeName, constantDecl, this);
        if (!matchesType(typeName, constantDecl.isArray(), value)) {
            throw error(constantDecl, "Type mismatch: expected " + typeLabel(typeName, constantDecl.isArray()) + " but got " + getTypeName(value));
        }
        if (value instanceof List<?> list && !(value instanceof ReadOnlyList)) {
            value = new ReadOnlyList((List<Object>) list);
        }
        setConstant(identifier, value);
        return null;
    }

    @Override
    public Object visitPrintStatement(PrintStatement printStatement) throws ParseException {
        Object result = printStatement.getExpression().accept(this);
        String output = formatValue(result);
        if (printStatement.isPrintln()) {
            System.out.println(output);
        } else {
            System.out.print(output);
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr) throws ParseException {
        Token.TokenType op = binaryExpr.getOperator().getToken();
        if (op == Token.TokenType.AND || op == Token.TokenType.OR) {
            Object leftValue = binaryExpr.getLeft().accept(this);
            if (!(leftValue instanceof Boolean l)) {
                throw error(binaryExpr, "Operands of '" + binaryExpr.getOperator().getValue() + "' must be booleans");
            }
            if (op == Token.TokenType.AND ? !l : l) return l;
            Object rightValue = binaryExpr.getRight().accept(this);
            if (!(rightValue instanceof Boolean)) {
                throw error(binaryExpr, "Operands of '" + binaryExpr.getOperator().getValue() + "' must be booleans");
            }
            return rightValue;
        }
        Object left = binaryExpr.getLeft().accept(this);
        Object right = binaryExpr.getRight().accept(this);

        return switch (binaryExpr.getOperator().getToken()) {
            case PLUS -> {
                if (left instanceof String || right instanceof String) {
                    yield formatValue(left) + formatValue(right);
                }
                if (Num.isNumber(left) && Num.isNumber(right)) {
                    yield Num.add(left, right);
                }
                throw error(binaryExpr, "Operands must be two numbers or two strings");
            }
            case MINUS -> {
                requireNumbers(left, right, binaryExpr.getOperator(), binaryExpr);
                yield Num.sub(left, right);
            }
            case ASTERISK -> {
                requireNumbers(left, right, binaryExpr.getOperator(), binaryExpr);
                yield Num.mul(left, right);
            }
            case SLASH -> {
                requireNumbers(left, right, binaryExpr.getOperator(), binaryExpr);
                if (Num.isZero(right)) throw error(binaryExpr, "Division by zero");
                yield Num.div(left, right);
            }
            case PERCENT -> {
                requireNumbers(left, right, binaryExpr.getOperator(), binaryExpr);
                if (Num.isZero(right)) throw error(binaryExpr, "Modulo by zero");
                yield Num.mod(left, right);
            }
            case EQUAL_EQUAL, NOT_EQUAL, LESS_THAN, GREATER_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL ->
                    compareOp(binaryExpr.getOperator(), left, right, binaryExpr);
            default -> throw error(binaryExpr, "Unknown binary operator: " + binaryExpr.getOperator());
        };
    }

    @Override
    public Object visitChainedComparison(ChainedComparison chain) throws ParseException {
        List<Node> operands = chain.getOperands();
        List<Token> operators = chain.getOperators();
        Object left = operands.get(0).accept(this);
        for (int i = 0; i < operators.size(); i++) {
            Object right = operands.get(i + 1).accept(this);
            if (!compareOp(operators.get(i), left, right, chain)) return false;
            left = right;
        }
        return true;
    }

    @Override
    public Object visitFunctionExpr(FunctionExpr functionExpr) {
        return new EzyFunction(functionExpr.getFunction(), env.current(), this, currentInstance, currentClass);
    }

    @Override
    public Object visitCallExpr(CallExpr callExpr) throws ParseException {
        Object callee = callExpr.getCallee().accept(this);
        List<Object> args = new ArrayList<>();
        for (Node arg : callExpr.getArguments()) {
            args.add(arg.accept(this));
        }
        return callValue(callee, args, callExpr);
    }

    private Object callValue(Object callee, List<Object> args, Node callNode) throws ParseException {
        if (callee instanceof EzyFunction function) {
            Interpreter owner = function.owner();
            if (function.boundInstance() == null) {
                return owner.invokeFunction(function.decl().getName(), function.decl(), function.closure(), args, callNode, this);
            }
            EzyInstance prevInstance = owner.currentInstance;
            EzyClass prevClass = owner.currentClass;
            owner.currentInstance = function.boundInstance();
            owner.currentClass = function.boundClass();
            try {
                return owner.invokeFunction(function.decl().getName(), function.decl(), function.closure(), args, callNode, this);
            } finally {
                owner.currentInstance = prevInstance;
                owner.currentClass = prevClass;
            }
        }
        if (callee instanceof NativeFunction nativeFunction) {
            return callNative(nativeFunction, args, "function", callNode);
        }
        throw error(callNode, "Value of type '" + getTypeName(callee) + "' is not callable");
    }

    @Override
    public Object visitLiteral(Literal literal) {
        return literal.getValue();
    }

    @Override
    public Object visitIdentifier(Identifier identifier) throws ParseException {
        String name = identifier.getName();
        if (env.hasVariable(name)) return findVariable(name);
        if (env.hasConstant(name)) return findConstant(name);
        FunctionDecl function = functions.get(name);
        if (function != null) return new EzyFunction(function, env.global(), this);
        NativeFunction nativeFunction = nativeFunctions.get(name);
        if (nativeFunction != null) return nativeFunction;
        throw error(identifier, "Undefined variable '" + name + "'");
    }

    @Override
    public Object visitArrayAccess(ArrayAccess arrayAccess) throws ParseException {
        Object index = arrayAccess.getIndex().accept(this);
        String identifier = arrayAccess.getIdentifier();
        Object array = findVariable(identifier);
        if (array == null) {
            array = findConstant(identifier);
        }
        if (array == null) {
            throw error(arrayAccess, "Undefined variable '" + identifier + "'");
        }
        if (!(array instanceof List<?>) && !(array instanceof String)) {
            throw error(arrayAccess, "Variable '" + identifier + "' is not an array or string");
        }
        if (!Num.isNumber(index)) {
            throw error(arrayAccess, "Index must be a number");
        }
        int idx = toIndex(index, arrayAccess);
        if (array instanceof String str) {
            return charAtCodePoint(str, idx, arrayAccess);
        }
        List<?> list = (List<?>) array;
        if (idx < 0 || idx >= list.size()) {
            throw error(arrayAccess, "Array index out of bounds");
        }
        return list.get(idx);
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement) throws ParseException {
        Object condition = ifStatement.getCondition().accept(this);

        if (!(condition instanceof Boolean)) {
            throw error(ifStatement.getCondition(), "Condition must be a boolean expression");
        }

        if ((Boolean) condition) {
            return ifStatement.getThenBranch().accept(this);
        } else if (ifStatement.getElseBranch() != null) {
            return ifStatement.getElseBranch().accept(this);
        }

        return null;
    }

    @Override
    public Object visitSwitchStatement(SwitchStatement switchStatement) throws ParseException {
        Object switchExpr = switchStatement.getExpression().accept(this);
        List<SwitchCase> cases = switchStatement.getCases();
        int defaultIndex = switchStatement.getDefaultCase() == null ? -1 : switchStatement.getDefaultIndex();

        List<Node> bodies = new ArrayList<>();
        List<Node> labels = new ArrayList<>();
        List<Boolean> arrows = new ArrayList<>();
        for (int i = 0; i <= cases.size(); i++) {
            if (i == defaultIndex) {
                bodies.add(switchStatement.getDefaultCase());
                labels.add(null);
                arrows.add(false);
            }
            if (i < cases.size()) {
                bodies.add(cases.get(i).getBody());
                labels.add(cases.get(i).getValue());
                arrows.add(cases.get(i).isArrowStyle());
            }
        }

        int start = -1;
        for (int i = 0; i < labels.size() && start < 0; i++) {
            Node label = labels.get(i);
            if (label != null && valuesEqual(switchExpr, label.accept(this))) start = i;
        }
        if (start < 0) {
            for (int i = 0; i < labels.size(); i++) {
                if (labels.get(i) == null) start = i;
            }
        }
        if (start < 0) return null;

        try {
            for (int i = start; i < bodies.size(); i++) {
                bodies.get(i).accept(this);
                if (arrows.get(i)) break;
            }
        } catch (BreakException ignored) {
        }
        return null;
    }

    @Override
    public Object visitFunctionDecl(FunctionDecl functionDecl) throws ParseException {
        String functionName = functionDecl.getName();
        if (!env.isGlobal()) {
            if (env.hasVariableInCurrentScope(functionName)) {
                throw error(functionDecl, "Function '" + functionName + "' is already defined in this scope");
            }
            validateFunctionDeclaration(functionDecl);
            setVariable(functionName, new EzyFunction(functionDecl, env.current(), this, currentInstance, currentClass));
            env.setType(functionName, new Environment.TypeSpec("func", false));
            return null;
        }
        if (functions.containsKey(functionName)) {
            throw error(functionDecl, "Function '" + functionName + "' is already defined");
        }
        functions.put(functionDecl.getName(), functionDecl);
        return null;
    }

    @Override
    public Object visitFunctionCall(FunctionCall functionCall) throws ParseException {
        String name = functionCall.getName();
        List<Object> evaluatedArgs = new ArrayList<>();
        for (Node arg : functionCall.getArguments()) {
            evaluatedArgs.add(arg.accept(this));
        }

        if (env.hasVariable(name) || env.hasConstant(name)) {
            Object bound = env.hasVariable(name) ? findVariable(name) : findConstant(name);
            if (bound instanceof NativeFunction nativeBound) {
                return callNative(nativeBound, evaluatedArgs, name, functionCall);
            }
            if (bound instanceof EzyFunction) {
                return callValue(bound, evaluatedArgs, functionCall);
            }
            throw error(functionCall, "'" + name + "' is not a function");
        }

        FunctionDecl function = functions.get(name);
        if (function != null) {
            return invokeFunction(name, function, env.global(), evaluatedArgs, functionCall, this);
        }
        NativeFunction nativeFunction = nativeFunctions.get(name);
        if (nativeFunction != null) {
            return callNative(nativeFunction, evaluatedArgs, name, functionCall);
        }
        throw error(functionCall, "Undefined function '" + name + "'");
    }

    private Object invokeFunction(String name, FunctionDecl function, Environment.Scope closure, List<Object> evaluatedArgs, Node functionCall, Interpreter site) throws ParseException {
        List<Decorator> custom = new ArrayList<>();
        if (function.getDecorators() != null) {
            for (Decorator decorator : function.getDecorators()) {
                if (customDecorators.containsKey(decorator.getName())) custom.add(decorator);
            }
        }
        if (custom.isEmpty()) {
            return invokeCore(name, function, closure, evaluatedArgs, functionCall, site);
        }
        Object result = runDecorated(custom, 0, name, function, closure, evaluatedArgs, functionCall, site);
        checkReturn(name, function, result, true, true, functionCall, site);
        return result;
    }

    private Object runDecorated(List<Decorator> custom, int index, String name, FunctionDecl function, Environment.Scope closure, List<Object> evaluatedArgs, Node functionCall, Interpreter site) throws ParseException {
        if (index == custom.size()) {
            return invokeCore(name, function, closure, evaluatedArgs, functionCall, site);
        }
        Decorator usage = custom.get(index);
        DecoratorDecl decorator = customDecorators.get(usage.getName());
        List<Object> decoratorArgs = new ArrayList<>();
        for (Node arg : usage.getArguments()) {
            decoratorArgs.add(arg.accept(this));
        }
        List<String> decoratorParams = decorator.getParamNames();
        if (decoratorArgs.size() != decoratorParams.size()) {
            throw site.error(functionCall, "Decorator '@" + usage.getName() + "' expects " + decoratorParams.size() + " arguments but got " + decoratorArgs.size());
        }
        for (int i = 0; i < decoratorParams.size(); i++) {
            String type = decorator.getParamTypes().get(i).getValue();
            requireKnownType(type, functionCall, site);
            if (!matchesType(type, false, decoratorArgs.get(i))) {
                throw site.error(functionCall, "Decorator '@" + usage.getName() + "' expects '" + type + "' for '" + decoratorParams.get(i) + "' but got '" + getTypeName(decoratorArgs.get(i)) + "'");
            }
        }
        Environment.Scope saved = env.enterCall(env.global());
        functionDepth++;
        try {
            List<String> params = decorator.getParamNames();
            for (int i = 0; i < params.size(); i++) {
                setVariable(params.get(i), i < decoratorArgs.size() ? decoratorArgs.get(i) : null);
            }
            NativeFunction next = args -> runDecorated(custom, index + 1, name, function, closure, args.isEmpty() ? evaluatedArgs : args, functionCall, site);
            setVariable("call", next);
            setVariable("args", new ArrayList<>(evaluatedArgs));
            try {
                decorator.getBody().accept(this);
                return null;
            } catch (ReturnException e) {
                return e.getValue();
            } catch (BreakException | ContinueException e) {
                throw e.detach();
            }
        } finally {
            functionDepth--;
            env.restore(saved);
        }
    }

    private Object invokeCore(String name, FunctionDecl function, Environment.Scope closure, List<Object> evaluatedArgs, Node functionCall, Interpreter site) throws ParseException {
        List<String> paramNames = function.getParamNames();
        List<Token> paramTypes = function.getParamTypes();
        List<Boolean> isArrayTypes = function.getIsArrayTypes();

        if (evaluatedArgs.size() != paramNames.size()) {
            throw site.error(functionCall, "Expected " + paramNames.size() + " arguments but got " + evaluatedArgs.size());
        }

        boolean isMemo = function.isMemo() || hasDecorator(function, "memo");
        boolean isLog = hasDecorator(function, "log");
        boolean isTimer = hasDecorator(function, "timer");

        if (hasDecorator(function, "deprecated")) {
            String msg = getDecoratorStringArg(function, "deprecated");
            System.out.println("[WARNING] " + name + " is deprecated" + (msg != null ? ": " + msg : ""));
        }

        handleGuardDecorators(function, evaluatedArgs, functionCall, site);

        Object memoKey = currentInstance == null ? function : Arrays.asList(function, currentInstance);
        List<Object> memoArgs = isMemo ? (List<Object>) Num.canonical(evaluatedArgs) : null;
        if (isMemo) {
            Map<List<Object>, Object> cache = memoCache.computeIfAbsent(memoKey, k -> new HashMap<>());
            if (cache.containsKey(memoArgs)) {
                return cache.get(memoArgs);
            }
        }

        long startTime = isTimer ? System.currentTimeMillis() : 0;

        Environment.Scope saved = env.enterCall(closure);
        functionDepth++;
        try {
            for (int i = 0; i < paramNames.size(); i++) {
                Object value = evaluatedArgs.get(i);
                String expectedType = paramTypes.get(i).getValue();
                boolean isArray = isArrayTypes.get(i);

                requireKnownType(expectedType, functionCall, site);
                if (!matchesType(expectedType, isArray, value)) {
                    throw site.error(functionCall, "Expected '" + typeLabel(expectedType, isArray) + "' but got '" + getTypeName(value) + "'");
                }
                setVariable(paramNames.get(i), value);
                env.setType(paramNames.get(i), new Environment.TypeSpec(expectedType, isArray));
            }

            Object result = null;
            boolean returned = false;
            boolean hasValue = false;
            try {
                function.getBody().accept(this);
            } catch (ReturnException returnEx) {
                result = returnEx.getValue();
                returned = true;
                hasValue = returnEx.hasValue();
            } catch (BreakException | ContinueException e) {
                throw e.detach();
            }
            checkReturn(name, function, result, returned, hasValue, functionCall, site);
            if (isMemo) memoCache.computeIfAbsent(memoKey, k -> new HashMap<>()).put(memoArgs, result);
            if (isLog) printLog(name, evaluatedArgs, result);
            if (isTimer) printTimer(name, startTime);
            return result;
        } catch (StackOverflowError e) {
            throw site.error(functionCall, "Stack overflow: recursion too deep in '" + name + "'");
        } finally {
            functionDepth--;
            env.restore(saved);
        }
    }

    private void checkReturn(String name, FunctionDecl function, Object result, boolean returned, boolean hasValue, Node node, Interpreter site) throws ParseException {
        Token returnType = function.getReturnType();
        if (returnType == null) return;
        String type = returnType.getValue();
        boolean isArray = Boolean.TRUE.equals(function.getIsReturnTypeArray());
        if (type.equals("void")) {
            if (result != null) {
                throw site.error(node, "Function '" + name + "' is declared void but returned " + getTypeName(result));
            }
            return;
        }
        if (!returned || !hasValue) {
            throw site.error(node, "Function '" + name + "' must return a value of type " + typeLabel(type, isArray));
        }
        if (!matchesType(type, isArray, result)) {
            throw site.error(node, "Function '" + name + "' must return " + typeLabel(type, isArray) + " but returned " + getTypeName(result));
        }
    }

    private boolean hasDecorator(FunctionDecl func, String name) {
        if (func.getDecorators() == null) return false;
        return func.getDecorators().stream().anyMatch(d -> d.getName().equals(name));
    }

    private String getDecoratorStringArg(FunctionDecl func, String decName) {
        if (func.getDecorators() == null) return null;
        for (Decorator d : func.getDecorators()) {
            if (d.getName().equals(decName) && !d.getArguments().isEmpty()) {
                Node arg = d.getArguments().get(0);
                if (arg instanceof Literal lit) return String.valueOf(lit.getValue());
            }
        }
        return null;
    }

    private void handleGuardDecorators(FunctionDecl func, List<Object> args, Node errorNode, Interpreter site) throws ParseException {
        if (func.getDecorators() == null) return;
        for (Decorator dec : func.getDecorators()) {
            if (!dec.getName().equals("guard") || dec.getArguments().size() < 2) continue;
            Node first = dec.getArguments().get(0);
            Node second = dec.getArguments().get(1);
            boolean stringForm = first instanceof Literal firstLiteral && firstLiteral.getValue() instanceof String
                    && second instanceof Literal secondLiteral && secondLiteral.getValue() instanceof String;
            Environment.Scope saved = env.enterCall(env.global());
            try {
                List<String> paramNames = func.getParamNames();
                for (int i = 0; i < paramNames.size(); i++) {
                    setVariable(paramNames.get(i), i < args.size() ? args.get(i) : null);
                }
                if (stringForm) {
                    String paramName = (String) ((Literal) first).getValue();
                    String conditionSource = (String) ((Literal) second).getValue();
                    if (!paramNames.contains(paramName)) {
                        throw site.error(errorNode, "@guard refers to unknown parameter '" + paramName + "'");
                    }
                    Node conditionNode;
                    try {
                        conditionNode = new Parser(fileName, conditionSource, new Lexer(conditionSource).scanTokens()).parseStandaloneExpression();
                    } catch (ParseException | RuntimeException e) {
                        throw site.error(errorNode, "Invalid @guard condition: \"" + conditionSource + "\"");
                    }
                    Object condition = conditionNode.accept(this);
                    if (!(condition instanceof Boolean passed)) {
                        throw site.error(errorNode, "@guard condition must be a boolean: " + conditionSource);
                    }
                    if (!passed) {
                        throw site.error(errorNode, "Guard failed: " + conditionSource + " (" + paramName + " = " + formatValue(findVariable(paramName)) + ")");
                    }
                } else {
                    Object condition = first.accept(this);
                    if (condition instanceof Boolean passed && !passed) {
                        Object msg = second.accept(this);
                        throw site.error(errorNode, String.valueOf(msg));
                    }
                }
            } finally {
                env.restore(saved);
            }
        }
    }

    private void printLog(String name, List<Object> args, Object result) {
        StringBuilder sb = new StringBuilder("[LOG] " + name + "(");
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(formatValue(args.get(i)));
        }
        sb.append(") → ").append(formatValue(result));
        System.out.println(sb);
    }

    private void printTimer(String name, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("[TIMER] " + name + ": " + elapsed + "ms");
    }

    private static class ReturnException extends RuntimeException {
        private final Object value;
        private final boolean hasValue;

        ReturnException(Object value, boolean hasValue) {
            super(null, null, false, false);
            this.value = value;
            this.hasValue = hasValue;
        }

        public Object getValue() {
            return value;
        }

        public boolean hasValue() {
            return hasValue;
        }
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement) throws ParseException {
        if (functionDepth == 0) {
            throw error(returnStatement, "'return' outside of function");
        }
        boolean hasValue = returnStatement.getValue() != null;
        Object value = hasValue ? returnStatement.getValue().accept(this) : null;
        throw new ReturnException(value, hasValue);
    }

    @Override
    public Object visitIncrementDecrementExpr(IncrementDecrementExpr expr) throws ParseException {
        Node operand = expr.getOperand();
        Object currentValue;
        java.util.function.Consumer<Object> store;
        String label;

        if (operand instanceof Identifier identifier) {
            String varName = identifier.getName();
            if (!env.hasVariable(varName)) {
                if (env.hasConstant(varName)) {
                    throw error(expr, "Cannot increment/decrement constant '" + varName + "'");
                }
                throw error(expr, "Undefined variable '" + varName + "'");
            }
            currentValue = findVariable(varName);
            label = varName;
            store = null;
        } else if (operand instanceof ArrayAccess || operand instanceof IndexExpr) {
            Object container;
            Object indexValue;
            if (operand instanceof ArrayAccess arrayAccess) {
                String name = arrayAccess.getIdentifier();
                if (!env.hasVariable(name) && !env.hasConstant(name)) {
                    throw error(expr, "Undefined array '" + name + "'");
                }
                container = env.hasVariable(name) ? findVariable(name) : findConstant(name);
                indexValue = arrayAccess.getIndex().accept(this);
            } else {
                IndexExpr indexExpr = (IndexExpr) operand;
                container = indexExpr.getTarget().accept(this);
                indexValue = indexExpr.getIndex().accept(this);
            }
            List<Object> list = requireMutableList(container, expr);
            int index = toIndex(indexValue, expr);
            if (index < 0 || index >= list.size()) {
                throw error(expr, "Array index out of bounds");
            }
            currentValue = list.get(index);
            label = null;
            store = v -> list.set(index, v);
        } else if (operand instanceof PropertyAccess || operand instanceof SelfExpr) {
            EzyInstance instance;
            String property;
            if (operand instanceof PropertyAccess propertyAccess) {
                Object object = propertyAccess.getObject().accept(this);
                if (!(object instanceof EzyInstance inst)) {
                    throw error(expr, "Cannot increment/decrement property on non-object");
                }
                instance = inst;
                property = propertyAccess.getProperty();
            } else {
                if (currentInstance == null) {
                    throw error(expr, "'self' can only be used inside a class method");
                }
                instance = currentInstance;
                property = ((SelfExpr) operand).getFieldName();
            }
            if (!instance.hasField(property)) {
                throw error(expr, "Property '" + property + "' not found on " + instance.getEzyClass().getName());
            }
            currentValue = instance.getField(property);
            label = null;
            EzyInstance target = instance;
            store = v -> target.setField(property, v);
        } else {
            throw error(expr, "Invalid operand for increment/decrement operator");
        }

        if (!Num.isNumber(currentValue)) {
            throw error(expr, "Cannot increment/decrement non-numeric value");
        }

        Object newValue = expr.getOperator().getToken() == Token.TokenType.PLUS_PLUS
                ? Num.add(currentValue, 1L)
                : Num.sub(currentValue, 1L);

        if (label != null) {
            validateConstraint(label, newValue, expr);
            env.updateVariable(label, newValue);
        } else {
            store.accept(newValue);
        }

        return expr.isPrefix() ? newValue : currentValue;
    }

    private List<Object> requireMutableList(Object container, Node node) throws ParseException {
        if (container instanceof ReadOnlyList) {
            throw error(node, "Cannot modify constant array");
        }
        if (!(container instanceof List<?>)) {
            throw error(node, "Value of type '" + getTypeName(container) + "' is not an array");
        }
        return (List<Object>) container;
    }

    private Environment.TypeSpec elementTypeOf(Node target) throws ParseException {
        if (target instanceof Identifier identifier) {
            Environment.TypeSpec spec = env.findType(identifier.getName());
            return spec != null && spec.isArray() ? spec : null;
        }
        if (target instanceof PropertyAccess propertyAccess) {
            Object object = propertyAccess.getObject().accept(this);
            if (object instanceof EzyInstance instance) {
                ClassField field = findFieldDecl(instance.getEzyClass(), propertyAccess.getProperty());
                if (field != null && field.isArray()) return new Environment.TypeSpec(field.getType().getValue(), true);
            }
            return null;
        }
        if (target instanceof SelfExpr self && self.getFieldName() != null && currentInstance != null) {
            ClassField field = findFieldDecl(currentInstance.getEzyClass(), self.getFieldName());
            if (field != null && field.isArray()) return new Environment.TypeSpec(field.getType().getValue(), true);
        }
        return null;
    }

    private String getTypeName(Object value) {
        if (value == null) return "null";
        if (Num.isNumber(value)) return "number";
        if (value instanceof String) return "string";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof List<?>) return "array";
        if (value instanceof EzyFunction || value instanceof NativeFunction) return "func";
        if (value instanceof EzyInstance instance) return instance.getEzyClass().getName();
        return "unknown";
    }

    @Override
    public Object visitMethodCall(MethodCall methodCall) throws ParseException {
        String objectName = methodCall.getObjectName();
        String methodName = methodCall.getMethodName();
        List<Node> arguments = methodCall.getArguments();
        Node objectNode = methodCall.getObjectNode();

        if (objectName != null && importedModules.contains(objectName)) {
            Interpreter module = loadedModules.get(objectName);
            if (module == null) {
                throw error(methodCall, "Module '" + objectName + "' not loaded");
            }
            if (methodCall.isPropertyAccess()) {
                Object constant = module.findConstant(methodName);
                if (constant != null) return constant;
                throw error(methodCall, "'" + methodName + "' not found in module '" + objectName + "'");
            }
            List<Object> args = new ArrayList<>();
            for (Node arg : arguments) {
                args.add(arg.accept(this));
            }
            NativeFunction nf = module.nativeFunctions.get(methodName);
            if (nf != null) return callNative(nf, args, methodName, methodCall);
            FunctionDecl func = module.functions.get(methodName);
            if (func != null) {
                return module.invokeFunction(methodName, func, module.env.global(), args, methodCall, this);
            }
            Object constant = module.findConstant(methodName);
            if (constant != null) return constant;
            throw error(methodCall, "'" + methodName + "' not found in module '" + objectName + "'");
        }

        Object object;
        if (objectName != null) {
            object = findVariable(objectName);
            if (object == null) {
                object = findConstant(objectName);
            }
            if (object == null) {
                throw error(methodCall, "Undefined variable '" + objectName + "'");
            }
        } else if (objectNode != null) {
            object = objectNode.accept(this);
        } else {
            throw error(methodCall, "No object specified for method call");
        }

        if (object instanceof EzyInstance instance) {
            List<Object> args = new ArrayList<>();
            for (Node arg : arguments) {
                args.add(arg.accept(this));
            }

            if (methodName.equals("copy") && instance.getEzyClass().hasDecoratorInHierarchy("data")) {
                return instance.copy();
            }

            if (methodName.startsWith("get") && methodName.length() > 3) {
                String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                EzyClass cls = instance.getEzyClass();
                if ((cls.hasDecoratorInHierarchy("getter") || cls.hasDecoratorInHierarchy("data")) && instance.hasField(fieldName) && args.isEmpty()) {
                    return instance.getField(fieldName);
                }
            }
            if (methodName.startsWith("set") && methodName.length() > 3 && args.size() == 1) {
                String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                EzyClass cls = instance.getEzyClass();
                if ((cls.hasDecoratorInHierarchy("setter") || cls.hasDecoratorInHierarchy("data")) && instance.hasField(fieldName)) {
                    setFieldChecked(instance, fieldName, args.get(0), methodCall);
                    return null;
                }
            }

            if (methodName.equals("toString") && (instance.getEzyClass().hasDecoratorInHierarchy("data") || instance.getEzyClass().hasDecoratorInHierarchy("toString"))) {
                return instance.toString();
            }

            FunctionDecl method = instance.getEzyClass().findMethod(methodName);
            if (method != null) {
                return callMethod(instance, method, args, instance.getEzyClass().findDeclaringClass(methodName), methodCall);
            }
            if (instance.hasField(methodName)) {
                Object fieldValue = instance.getField(methodName);
                if (fieldValue instanceof EzyFunction || fieldValue instanceof NativeFunction) {
                    return callValue(fieldValue, args, methodCall);
                }
                throw error(methodCall, "Field '" + methodName + "' of " + instance.getEzyClass().getName() + " is not a function");
            }
            throw error(methodCall, "Undefined method '" + methodName + "' on " + instance.getEzyClass().getName());

        }

        if (object instanceof ReadOnlyList && MUTATING_LIST_METHODS.contains(methodName)) {
            throw error(methodCall, "Cannot modify constant array" + (objectName != null ? " '" + objectName + "'" : ""));
        }
        if (methodName.equals("length") && (object instanceof List<?> || object instanceof String)) {
            if (!arguments.isEmpty()) {
                throw error(methodCall, "Method 'length' does not take any arguments");
            }
            return (long) (object instanceof List<?> list ? list.size() : ((String) object).codePointCount(0, ((String) object).length()));
        }
        if (object instanceof List<?> list && (methodName.equals("map") || methodName.equals("filter") || methodName.equals("forEach"))) {
            if (arguments.size() != 1) {
                throw error(methodCall, "Method '" + methodName + "' takes exactly one function argument");
            }
            Object fn = arguments.get(0).accept(this);
            List<Object> result = new ArrayList<>();
            for (Object item : new ArrayList<>(list)) {
                Object value = callValue(fn, new ArrayList<>(Collections.singletonList(item)), methodCall);
                if (methodName.equals("map")) {
                    result.add(value);
                } else if (methodName.equals("filter")) {
                    if (!(value instanceof Boolean keep)) {
                        throw error(methodCall, "Function passed to 'filter' must return a boolean");
                    }
                    if (keep) result.add(item);
                }
            }
            return methodName.equals("forEach") ? null : result;
        }
        if (methodName.equals("reduce") && object instanceof List<?> list) {
            if (arguments.size() != 2) {
                throw error(methodCall, "Method 'reduce' takes a function and an initial value");
            }
            Object fn = arguments.get(0).accept(this);
            Object acc = arguments.get(1).accept(this);
            for (Object item : new ArrayList<>(list)) {
                acc = callValue(fn, new ArrayList<>(Arrays.asList(acc, item)), methodCall);
            }
            return acc;
        }
        if (methodName.equals("repeat") && object instanceof String) {
            if (arguments.size() != 1) {
                throw error(methodCall, "Method 'repeat' takes exactly one argument");
            }
            Object arg = arguments.get(0).accept(this);
            if (!Num.isNumber(arg)) {
                throw error(methodCall, "Argument must be a number");
            }
            int count = toIndex(arg, methodCall);
            if (count < 0) {
                throw error(methodCall, "Repeat count must be a non-negative integer");
            }
            return String.valueOf(object).repeat(count);
        }
        if (methodName.equals("charAt") && object instanceof String) {
            if (arguments.size() != 1) {
                throw error(methodCall, "Method 'charAt' takes exactly one argument");
            }
            Object arg = arguments.get(0).accept(this);
            if (!Num.isNumber(arg)) {
                throw error(methodCall, "Argument must be a number");
            }
            return charAtCodePoint((String) object, toIndex(arg, methodCall), methodCall);
        }
        if (methodName.equals("contains") && object instanceof List<?>) {
            if (arguments.size() != 1) {
                throw error(methodCall, "Method 'contains' takes exactly one argument");
            }
            Object arg = arguments.get(0).accept(this);
            return indexOfValue((List<?>) object, arg, false) >= 0;
        }
        if (methodName.equals("indexOf") && object instanceof List<?>) {
            if (arguments.size() != 1) {
                throw error(methodCall, "Method 'indexOf' takes exactly one argument");
            }
            Object arg = arguments.get(0).accept(this);
            return (long) indexOfValue((List<?>) object, arg, false);
        }
        if (methodName.equals("lastIndexOf") && object instanceof List<?>) {
            if (arguments.size() != 1) {
                throw error(methodCall, "Method 'lastIndexOf' takes exactly one argument");
            }
            Object arg = arguments.get(0).accept(this);
            return (long) indexOfValue((List<?>) object, arg, true);
        }
        if (methodName.equals("isEmpty") && object instanceof List<?>) {
            if (!arguments.isEmpty()) {
                throw error(methodCall, "Method 'isEmpty' does not take any arguments");
            }
            return ((List<?>) object).isEmpty();
        }
        if (methodName.equals("isNotEmpty") && object instanceof List<?>) {
            if (!arguments.isEmpty()) {
                throw error(methodCall, "Method 'isNotEmpty' does not take any arguments");
            }
            return !((List<?>) object).isEmpty();
        }
        if (methodName.equals("clear") && object instanceof List<?>) {
            if (!arguments.isEmpty()) {
                throw error(methodCall, "Method 'clear' does not take any arguments");
            }
            ((List<?>) object).clear();
            return null;
        }
        if (methodName.equals("addAll") && object instanceof List<?>) {
            if (arguments.size() != 1) {
                throw error(methodCall, "Method 'addAll' takes exactly one argument");
            }
            Object arg = arguments.get(0).accept(this);
            if (!(arg instanceof List<?>)) {
                throw error(methodCall, "Argument must be an array");
            }
            Environment.TypeSpec spec = objectName != null
                    ? elementTypeOf(new Identifier(objectName, methodCall.getLine(), methodCall.getColumn()))
                    : elementTypeOf(objectNode);
            if (spec != null && !matchesType(spec.name(), true, arg)) {
                throw error(methodCall, "Type mismatch: cannot add those elements to an array of type " + typeLabel(spec.name(), true));
            }
            ((List<Object>) object).addAll((List<?>) arg);
            return null;
        }
        if (methodName.equals("remove") && object instanceof List<?>) {
            if (arguments.size() != 1) {
                throw error(methodCall, "Method 'remove' takes exactly one argument");
            }
            Object arg = arguments.get(0).accept(this);
            int position = indexOfValue((List<?>) object, arg, false);
            if (position < 0) return false;
            ((List<?>) object).remove(position);
            return true;
        }
        if (methodName.equals("removeAt") && object instanceof List<?>) {
            if (arguments.size() != 1) {
                throw error(methodCall, "Method 'removeAt' takes exactly one argument");
            }
            Object arg = arguments.get(0).accept(this);
            if (!Num.isNumber(arg)) {
                throw error(methodCall, "Argument must be a number");
            }
            int index = toIndex(arg, methodCall);
            if (index < 0 || index >= ((List<?>) object).size()) {
                throw error(methodCall, "Index out of bounds");
            }
            return ((List<?>) object).remove(index);
        }
        if (methodName.equals("reverse") && object instanceof List<?>) {
            if (!arguments.isEmpty()) {
                throw error(methodCall, "Method 'reverse' does not take any arguments");
            }
            Collections.reverse((List<?>) object);
            return null;
        }
        if (methodName.equals("sort") && object instanceof List<?>) {
            if (!arguments.isEmpty()) {
                throw error(methodCall, "Method 'sort' does not take any arguments");
            }
            if (((List<?>) object).isEmpty()) {
                return null;
            }
            List<Object> items = (List<Object>) object;
            if (items.stream().allMatch(Num::isNumber)) {
                items.sort(Num::compare);
            } else if (items.stream().allMatch(item -> item instanceof String)) {
                items.sort((a, b) -> ((String) a).compareTo((String) b));
            } else {
                throw error(methodCall, "Cannot sort array with mixed or unsupported element types");
            }
            return null;
        }
        if (methodName.equals("shuffle") && object instanceof List<?>) {
            if (!arguments.isEmpty()) {
                throw error(methodCall, "Method 'shuffle' does not take any arguments");
            }
            Collections.shuffle((List<?>) object);
            return null;
        }
        if (methodName.equals("join") && object instanceof List<?>) {
            if (arguments.size() != 1) {
                throw error(methodCall, "Method 'join' takes exactly one argument");
            }
            Object arg = arguments.get(0).accept(this);
            if (!(arg instanceof String)) {
                throw error(methodCall, "Argument must be a string");
            }
            List<String> parts = new ArrayList<>();
            for (Object item : (List<?>) object) {
                parts.add(formatValue(item));
            }
            return String.join((String) arg, parts);
        }
        if (methodName.equals("split") && object instanceof String) {
            if (arguments.size() != 1) {
                throw error(methodCall, "Method 'split' takes exactly one argument");
            }
            Object arg = arguments.get(0).accept(this);
            if (!(arg instanceof String)) {
                throw error(methodCall, "Argument must be a string");
            }
            return new ArrayList<Object>(Arrays.asList(((String) object).split(java.util.regex.Pattern.quote((String) arg))));
        }

        throw error(methodCall, "Undefined method '" + methodName + "'");
    }

    @Override
    public Object visitSwitchCase(SwitchCase switchCase) throws ParseException {
        return switchCase.getBody().accept(this);
    }

    @Override
    public Object visitForStatement(ForStatement forStatement) throws ParseException {
        String identifier = forStatement.getIdentifier();
        String typeName = forStatement.getType().getValue();
        Object iterable = forStatement.getStart().accept(this);
        List<Object> items = new ArrayList<>();
        boolean isRange = forStatement.getEnd() != null;
        if (!isRange) {
            if (iterable instanceof List<?> list) {
                items.addAll(list);
            } else if (iterable instanceof String str) {
                str.codePoints().forEach(cp -> items.add(new String(Character.toChars(cp))));
            } else {
                throw error(forStatement, "Cannot iterate over value of type '" + getTypeName(iterable) + "'");
            }
        }

        env.enterScope();
        try {
            env.setType(identifier, new Environment.TypeSpec(typeName, false));
            if (!isRange) {
                for (Object element : items) {
                    if (!runLoopIteration(forStatement, identifier, typeName, element)) break;
                }
                return null;
            }
            Object end = forStatement.getEnd().accept(this);
            Object step = forStatement.getStep() != null ? forStatement.getStep().accept(this) : 1L;
            if (!Num.isNumber(iterable) || !Num.isNumber(end) || !Num.isNumber(step)) {
                throw error(forStatement, "Range bounds and step must be numbers");
            }
            if (Num.compare(step, 0L) <= 0) {
                throw error(forStatement.getStep(), "Step must be positive, got: " + formatValue(step));
            }
            for (Object i = iterable; Num.compare(i, end) <= 0; i = Num.add(i, step)) {
                if (!runLoopIteration(forStatement, identifier, typeName, i)) break;
            }
        } finally {
            env.exitScope();
        }
        return null;
    }

    private boolean runLoopIteration(ForStatement forStatement, String identifier, String typeName, Object element) throws ParseException {
        requireKnownType(typeName, forStatement, this);
        if (!matchesType(typeName, false, element)) {
            throw error(forStatement, "Type mismatch: loop variable '" + identifier + "' of type " + typeName + " got " + getTypeName(element));
        }
        setVariable(identifier, element);
        try {
            forStatement.getBody().accept(this);
        } catch (ContinueException ignored) {
        } catch (BreakException e) {
            return false;
        }
        return true;
    }

    @Override
    public Object visitArrayLiteral(ArrayLiteral arrayLiteral) throws ParseException {
        List<Node> elements = arrayLiteral.getElements();
        List<Object> evaluatedElements = new ArrayList<>();

        for (Node element : elements) {
            Object evaluated = element.accept(this);
            evaluatedElements.add(evaluated);
        }

        return evaluatedElements;
    }

    @Override
    public Object visitBlock(Block block) throws ParseException {
        enterScope();
        Object result = null;
        try {
            for (Node statement : block.getStatements()) {
                result = statement.accept(this);
            }
        } finally {
            exitScope();
        }
        return result;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr) throws ParseException {
        Object operand = unaryExpr.getOperand().accept(this);

        return switch (unaryExpr.getOperator().getToken()) {
            case MINUS -> {
                if (!Num.isNumber(operand)) throw error(unaryExpr.getOperand(), "Operand must be a number");
                yield Num.negate(operand);
            }
            case PLUS -> {
                if (!Num.isNumber(operand)) throw error(unaryExpr.getOperand(), "Operand must be a number");
                yield operand;
            }
            case BANG -> {
                if (!(operand instanceof Boolean)) throw error(unaryExpr.getOperand(), "Operand must be a boolean");
                yield !(Boolean) operand;
            }
            default -> throw error(unaryExpr, "Unknown unary operator: " + unaryExpr.getOperator());
        };
    }

    @Override
    public Object visitInterpolatedString(InterpolatedString interpolatedString) throws ParseException {
        StringBuilder result = new StringBuilder();
        List<Node> expressions = interpolatedString.getExpressions();
        List<String> segments = interpolatedString.getSegments();
        for (int i = 0; i < expressions.size(); i++) {
            result.append(segments.get(i));
            result.append(formatValue(expressions.get(i).accept(this)));
        }
        result.append(segments.get(segments.size() - 1));
        return result.toString();
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement) throws ParseException {
        try {
            while (true) {
                Object condition = whileStatement.getCondition().accept(this);

                if (!(condition instanceof Boolean)) {
                    throw error(whileStatement.getCondition(), "Condition must be a boolean expression");
                }

                if (!(Boolean) condition) {
                    break;
                }

                try {
                    whileStatement.getBody().accept(this);
                } catch (ContinueException ignored) {
                }
            }
        } catch (BreakException ignored) {
        }

        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement) throws ParseException {
        Node target = assignmentStatement.getTarget();
        Token operator = assignmentStatement.getOperator();

        if (target instanceof Identifier identifierNode) {
            String identifier = identifierNode.getName();
            if (env.hasConstant(identifier) && !env.hasVariable(identifier)) {
                throw error(assignmentStatement, "Cannot reassign to constant '" + identifier + "'");
            }
            if (!env.hasVariable(identifier)) {
                throw error(assignmentStatement, "Undefined variable '" + identifier + "'");
            }
            Object value = assignmentStatement.getValue().accept(this);

            Object currentValue = findVariable(identifier);
            Object newValue = operator.getToken() == Token.TokenType.EQUAL
                    ? value
                    : computeCompoundAssignment(currentValue, operator, value, assignmentStatement);

            Environment.TypeSpec type = env.findType(identifier);
            if (type != null && !matchesType(type.name(), type.isArray(), newValue)) {
                throw error(assignmentStatement, "Type mismatch: cannot assign " + getTypeName(newValue) + " to '" + identifier + "' of type " + typeLabel(type.name(), type.isArray()));
            }

            validateConstraint(identifier, newValue, assignmentStatement);

            env.updateVariable(identifier, newValue);
            return null;
        }

        Object container;
        Object indexObj;
        Environment.TypeSpec elementType;
        String targetLabel;
        if (target instanceof ArrayAccess arrayAccess) {
            String identifier = arrayAccess.getIdentifier();
            if (!env.hasVariable(identifier) && !env.hasConstant(identifier)) {
                throw error(assignmentStatement, "Undefined array '" + identifier + "'");
            }
            container = env.hasVariable(identifier) ? findVariable(identifier) : findConstant(identifier);
            indexObj = arrayAccess.getIndex().accept(this);
            elementType = elementTypeOf(new Identifier(identifier, arrayAccess.getLine(), arrayAccess.getColumn()));
            targetLabel = identifier;
        } else if (target instanceof IndexExpr indexExpr) {
            container = indexExpr.getTarget().accept(this);
            indexObj = indexExpr.getIndex().accept(this);
            elementType = elementTypeOf(indexExpr.getTarget());
            targetLabel = "array";
        } else {
            throw error(assignmentStatement, "Invalid assignment target");
        }

        List<Object> list = requireMutableList(container, assignmentStatement);
        int index = toIndex(indexObj, assignmentStatement);
        if (index < 0 || index >= list.size()) {
            throw error(assignmentStatement, "Array index out of bounds");
        }
        Object value = assignmentStatement.getValue().accept(this);
        Object newValue = operator.getToken() == Token.TokenType.EQUAL
                ? value
                : computeCompoundAssignment(list.get(index), operator, value, assignmentStatement);
        if (elementType != null && !matchesType(elementType.name(), false, newValue)) {
            throw error(assignmentStatement, "Type mismatch: cannot assign " + getTypeName(newValue) + " to element of '" + targetLabel + "' of type " + typeLabel(elementType.name(), true));
        }
        list.set(index, newValue);
        return null;
    }

    @Override
    public Object visitExpressionStatement(ExpressionStatement expressionStatement) throws ParseException {
        Object result = expressionStatement.getExpression().accept(this);
        return result;
    }

    @Override
    public Object visitImportStatement(ImportStatement importStatement) throws ParseException {
        String moduleName = importStatement.getModuleName();
        Interpreter module = loadModule(moduleName, importStatement);
        List<ImportItem> items = importStatement.getItems();

        if (items.isEmpty()) {
            if (env.hasVariable(moduleName) || env.hasConstant(moduleName) || functions.containsKey(moduleName)) {
                throw error(importStatement, "Name conflict: '" + moduleName + "' is already defined");
            }
            importedModules.add(moduleName);
            return null;
        }

        for (ImportItem item : items) {
            String name = item.getName();
            String alias = item.getAlias() != null ? item.getAlias() : name;

            if (name.equals("*")) {
                Map<String, Object> currentVars = env.getTopVariableScope();
                for (Map.Entry<String, Object> entry : module.env.getGlobalVariableScope().entrySet()) {
                    if (isNameTaken(entry.getKey())) {
                        throw error(importStatement, "Name conflict: '" + entry.getKey() + "' is already defined. Use 'as' alias to resolve: from " + moduleName + " import " + entry.getKey() + " as <alias>");
                    }
                    currentVars.put(entry.getKey(), new Environment.ModuleVarRef(module.env.global(), entry.getKey()));
                }
                Map<String, Object> currentConsts = env.getTopConstantScope();
                for (Map.Entry<String, Object> entry : module.env.getGlobalConstantScope().entrySet()) {
                    if (currentConsts.containsKey(entry.getKey())) {
                        throw error(importStatement, "Name conflict: '" + entry.getKey() + "' is already defined. Use 'as' alias to resolve: from " + moduleName + " import $" + entry.getKey() + " as <alias>");
                    }
                    currentConsts.put(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<String, FunctionDecl> entry : module.functions.entrySet()) {
                    if (isNameTaken(entry.getKey())) {
                        throw error(importStatement, "Name conflict: '" + entry.getKey() + "' is already defined. Use 'as' alias to resolve: from " + moduleName + " import " + entry.getKey() + " as <alias>");
                    }
                    currentVars.put(entry.getKey(), new EzyFunction(entry.getValue(), module.env.global(), module));
                }
                for (Map.Entry<String, NativeFunction> entry : module.nativeFunctions.entrySet()) {
                    if (isNameTaken(entry.getKey())) {
                        throw error(importStatement, "Name conflict: '" + entry.getKey() + "' is already imported. Use 'as' alias to resolve: from " + moduleName + " import " + entry.getKey() + " as <alias>");
                    }
                    nativeFunctions.put(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<String, EzyClass> entry : module.classes.entrySet()) {
                    if (classes.containsKey(entry.getKey()) || interfaces.containsKey(entry.getKey())) {
                        throw error(importStatement, "Name conflict: type '" + entry.getKey() + "' is already defined");
                    }
                    classes.put(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<String, EzyInterface> entry : module.interfaces.entrySet()) {
                    if (classes.containsKey(entry.getKey()) || interfaces.containsKey(entry.getKey())) {
                        throw error(importStatement, "Name conflict: type '" + entry.getKey() + "' is already defined");
                    }
                    interfaces.put(entry.getKey(), entry.getValue());
                }
            } else {
                if (isNameTaken(alias) || classes.containsKey(alias) || interfaces.containsKey(alias)) {
                    throw error(importStatement, "Name conflict: '" + alias + "' is already defined. Use 'as' alias to resolve: from " + moduleName + " import " + name + " as <alias>");
                }
                if (module.env.getGlobalVariableScope().containsKey(name)) {
                    setVariable(alias, new Environment.ModuleVarRef(module.env.global(), name));
                } else if (module.classes.containsKey(name)) {
                    classes.put(alias, module.classes.get(name));
                } else if (module.interfaces.containsKey(name)) {
                    interfaces.put(alias, module.interfaces.get(name));
                } else if (module.env.getGlobalConstantScope().containsKey(name)) {
                    setConstant(alias, module.env.getGlobalConstantScope().get(name));
                } else if (module.functions.containsKey(name)) {
                    setVariable(alias, new EzyFunction(module.functions.get(name), module.env.global(), module));
                } else if (module.nativeFunctions.containsKey(name)) {
                    nativeFunctions.put(alias, module.nativeFunctions.get(name));
                } else {
                    throw error(importStatement, "Item '" + name + "' not found in module '" + moduleName + "'");
                }
            }
        }

        return null;
    }

    private boolean isNameTaken(String name) {
        return env.hasVariableInCurrentScope(name) || env.hasConstantInCurrentScope(name)
                || functions.containsKey(name) || nativeFunctions.containsKey(name) || importedModules.contains(name);
    }

    @Override
    public Object visitTypeCheckExpr(TypeCheckExpr expr) throws ParseException {
        Object value = expr.getExpression().accept(this);
        String targetType = expr.getType().getValue();

        return switch (targetType.toLowerCase()) {
            case "number" -> Num.isNumber(value);
            case "string" -> value instanceof String;
            case "boolean" -> value instanceof Boolean;
            case "char" -> value instanceof String s && s.codePointCount(0, s.length()) == 1;
            case "array" -> value instanceof List;
            case "func" -> value instanceof EzyFunction || value instanceof NativeFunction;
            case "null" -> value == null;
            case "any" -> true;
            default -> {
                EzyClass cls = classes.get(targetType);
                if (cls != null) yield value instanceof EzyInstance instance && instance.isInstanceOf(cls);
                if (interfaces.containsKey(targetType)) yield value instanceof EzyInstance instance && instance.isInstanceOfInterface(targetType);
                throw error(expr, "Unknown type: " + targetType);
            }
        };
    }

    @Override
    public Object visitTypeCastExpr(TypeCastExpr expr) throws ParseException {
        Object value = expr.getExpression().accept(this);
        String targetType = expr.getTargetType().getValue();

        try {
            return switch (targetType.toLowerCase()) {
                case "number" -> {
                    if (value instanceof String str) {
                        yield Num.parse(str);
                    } else if (Num.isNumber(value)) {
                        yield value;
                    } else if (value instanceof Boolean b) {
                        yield b ? 1L : 0L;
                    }
                    throw error(expr, "Cannot cast " + getTypeName(value) + " to number");
                }
                case "string" -> formatValue(value);
                case "boolean" -> {
                    if (value instanceof String str) {
                        yield Boolean.parseBoolean(str);
                    } else if (Num.isNumber(value)) {
                        yield !Num.isZero(value);
                    } else if (value instanceof Boolean b) {
                        yield b;
                    }
                    throw error(expr, "Cannot cast " + getTypeName(value) + " to boolean");
                }
                case "char" -> {
                    if (value instanceof String str && str.codePointCount(0, str.length()) == 1) {
                        yield str;
                    } else if (Num.isNumber(value)) {
                        yield new String(Character.toChars(Num.toInt(value)));
                    }
                    throw error(expr, "Cannot cast " + getTypeName(value) + " to char");
                }
                case "any" -> value;
                default -> {
                    if (!classes.containsKey(targetType) && !interfaces.containsKey(targetType)) {
                        throw error(expr, "Unknown type: " + targetType);
                    }
                    if (value == null || matchesType(targetType, false, value)) yield value;
                    throw error(expr, "Cannot cast " + getTypeName(value) + " to " + targetType);
                }
            };
        } catch (Exception e) {
            throw error(expr, "Cast failed: " + e.getMessage());
        }
    }

    private ParseException error(Node node, String message) {
        int line = node.getLine();
        int column = node.getColumn();
        String errorLine = getErrorLine(line);

        return new ParseException(
                fileName,
                message,
                line,
                column,
                errorLine
        );
    }

    private String getErrorLine(int line) {
        if (line <= 0 || line > lines.size()) {
            return "";
        }
        return lines.get(line - 1);
    }

    @Override
    public Object visitBreakStatement(BreakStatement breakStatement) throws ParseException {
        throw new BreakException(fileName, breakStatement, getErrorLine(breakStatement.getLine()));
    }

    @Override
    public Object visitContinueStatement(ContinueStatement continueStatement) throws ParseException {
        throw new ContinueException(fileName, continueStatement, getErrorLine(continueStatement.getLine()));
    }

    private static class BreakException extends ParseException {
        BreakException(String fileName, BreakStatement breakStatement, String errorLine) {
            super(fileName, "Break statement outside of loop", breakStatement.getLine(), breakStatement.getColumn(), errorLine);
        }
    }

    private static class ContinueException extends ParseException {
        ContinueException(String fileName, ContinueStatement continueStatement, String errorLine) {
            super(fileName, "Continue statement outside of loop", continueStatement.getLine(), continueStatement.getColumn(), errorLine);
        }
    }

    private static class AssertionFailedException extends ParseException {
        AssertionFailedException(String fileName, String message, int line, int column, String errorLine) {
            super(fileName, message, line, column, errorLine);
        }
    }

    @Override
    public Object visitTestBlock(TestBlock testBlock) throws ParseException {
        if (!testMode) return null;

        enterScope();
        try {
            testBlock.getBody().accept(this);
            testsPassed++;
            System.out.println("[PASS] " + testBlock.getName());
        } catch (ParseException e) {
            testsFailed++;
            System.out.println("[FAIL] " + testBlock.getName() + " - " + e.getMessage());
        } finally {
            exitScope();
        }
        return null;
    }

    @Override
    public Object visitAssertStatement(AssertStatement assertStatement) throws ParseException {
        Object result = assertStatement.getExpression().accept(this);
        if (!(result instanceof Boolean) || !(Boolean) result) {
            throw new AssertionFailedException(
                    fileName,
                    "Assertion failed at line " + assertStatement.getLine(),
                    assertStatement.getLine(),
                    assertStatement.getColumn(),
                    getErrorLine(assertStatement.getLine())
            );
        }
        return null;
    }


    @Override
    public Object visitClassDecl(ClassDecl classDecl) throws ParseException {
        String name = classDecl.getName();
        EzyClass parentEzyClass = null;
        if (classes.containsKey(name) || interfaces.containsKey(name)) {
            throw error(classDecl, "Type '" + name + "' is already defined");
        }
        java.util.Set<String> methodNames = new java.util.HashSet<>();
        for (FunctionDecl method : classDecl.getMethods()) {
            if (!methodNames.add(method.getName())) {
                throw error(method, "Method '" + method.getName() + "' is already defined in class '" + name + "'");
            }
        }

        if (classDecl.getParentClass() != null) {
            parentEzyClass = classes.get(classDecl.getParentClass());
            if (parentEzyClass == null) {
                throw error(classDecl, "Undefined parent class '" + classDecl.getParentClass() + "'");
            }
        }

        for (String ifaceName : classDecl.getInterfaces()) {
            if (!interfaces.containsKey(ifaceName)) {
                throw error(classDecl, "Undefined interface '" + ifaceName + "'");
            }
        }

        EzyClass ezyClass = new EzyClass(name, classDecl.getConstructorParams(), parentEzyClass,
                classDecl.getInterfaces(), classDecl.getFields(), classDecl.getMethods(),
                classDecl.getDecorators());

        for (FunctionDecl method : classDecl.getMethods()) {
            boolean isOverride = method.isOverride() || hasDecorator(method, "override");
            if (isOverride) {
                FunctionDecl parentMethod = parentEzyClass == null ? null : parentEzyClass.findMethod(method.getName());
                if (parentMethod == null) {
                    throw error(method, "Method '" + method.getName() + "' is marked @override but no parent method found");
                }
                if (!sameSignature(parentMethod, method)) {
                    throw error(method, "Method '" + method.getName() + "' does not match the signature of the parent method it overrides");
                }
            } else if (parentEzyClass != null && parentEzyClass.findMethod(method.getName()) != null) {
                throw error(method, "'" + method.getName() + "' already exists in parent class '" + parentEzyClass.getName() + "'. Use '@override' to redefine.");
            }
        }

        for (String ifaceName : classDecl.getInterfaces()) {
            EzyInterface iface = interfaces.get(ifaceName);
            for (FunctionDecl ifaceMethod : iface.getMethods()) {
                FunctionDecl implementation = ezyClass.findMethod(ifaceMethod.getName());
                if (implementation == null) {
                    throw error(classDecl, "Class '" + name + "' must implement method '" + ifaceMethod.getName() + "' from interface '" + ifaceName + "'");
                }
                if (!sameSignature(ifaceMethod, implementation)) {
                    throw error(classDecl, "Method '" + ifaceMethod.getName() + "' in class '" + name + "' does not match the signature declared in interface '" + ifaceName + "'");
                }
            }
        }

        ezyClass.setOwner(this);
        classes.put(name, ezyClass);
        classDecls.put(name, classDecl);
        return null;
    }

    @Override
    public Object visitInterfaceDecl(InterfaceDecl interfaceDecl) throws ParseException {
        if (classes.containsKey(interfaceDecl.getName()) || interfaces.containsKey(interfaceDecl.getName())) {
            throw error(interfaceDecl, "Type '" + interfaceDecl.getName() + "' is already defined");
        }
        interfaces.put(interfaceDecl.getName(), new EzyInterface(interfaceDecl.getName(), interfaceDecl.getMethods()));
        return null;
    }

    @Override
    public Object visitNewExpr(NewExpr newExpr) throws ParseException {
        String className = newExpr.getClassName();
        EzyClass ezyClass = classes.get(className);
        if (ezyClass == null) {
            throw error(newExpr, "Undefined class '" + className + "'");
        }

        List<Object> args = new ArrayList<>();
        for (Node arg : newExpr.getArguments()) {
            args.add(arg.accept(this));
        }

        Interpreter owner = ezyClass.getOwner() instanceof Interpreter o ? o : this;
        return owner.instantiateClass(ezyClass, args, newExpr, this);
    }

    private static boolean sameSignature(FunctionDecl expected, FunctionDecl actual) {
        if (expected.getParamNames().size() != actual.getParamNames().size()) return false;
        for (int i = 0; i < expected.getParamNames().size(); i++) {
            if (!expected.getParamTypes().get(i).getValue().equals(actual.getParamTypes().get(i).getValue())) return false;
            if (!expected.getIsArrayTypes().get(i).equals(actual.getIsArrayTypes().get(i))) return false;
        }
        String expectedReturn = expected.getReturnType() == null ? "void" : expected.getReturnType().getValue();
        String actualReturn = actual.getReturnType() == null ? "void" : actual.getReturnType().getValue();
        return expectedReturn.equals(actualReturn)
                && Boolean.TRUE.equals(expected.getIsReturnTypeArray()) == Boolean.TRUE.equals(actual.getIsReturnTypeArray());
    }

    private EzyInstance instantiateClass(EzyClass ezyClass, List<Object> args, Node errorNode, Interpreter site) throws ParseException {
        EzyInstance instance = new EzyInstance(ezyClass);
        initInstance(instance, ezyClass, args, errorNode, site);
        return instance;
    }

    private void initInstance(EzyInstance instance, EzyClass cls, List<Object> args, Node errorNode, Interpreter site) throws ParseException {
        if (cls.getOwner() instanceof Interpreter owner && owner != this) {
            owner.initInstance(instance, cls, args, errorNode, site);
            return;
        }
        List<ClassField> params = cls.getConstructorParams();
        int requiredParams = 0;
        for (ClassField p : params) {
            if (p.getDefaultValue() == null) requiredParams++;
        }

        if (args.size() < requiredParams || args.size() > params.size()) {
            throw site.error(errorNode, "Expected " + (requiredParams == params.size() ? String.valueOf(requiredParams) : requiredParams + "-" + params.size())
                    + " arguments but got " + args.size());
        }

        List<Object> values = new ArrayList<>();
        Environment.Scope defaultsScope = env.enterCall(env.global());
        try {
            for (int i = 0; i < params.size(); i++) {
                ClassField param = params.get(i);
                Object value = i < args.size() ? args.get(i) : param.getDefaultValue().accept(this);
                checkFieldType(cls, param, value, errorNode, site);
                values.add(value);
                setVariable(param.getName(), value);
            }
        } finally {
            env.restore(defaultsScope);
        }

        EzyClass parent = cls.getParentClass();
        if (parent != null) {
            ClassDecl decl = classDecls.get(cls.getName());
            List<Object> parentArgs = new ArrayList<>();
            Environment.Scope saved = env.enterCall(env.global());
            try {
                for (int i = 0; i < params.size(); i++) {
                    setVariable(params.get(i).getName(), values.get(i));
                }
                if (decl != null) {
                    for (Node arg : decl.getParentArgs()) {
                        parentArgs.add(arg.accept(this));
                    }
                }
            } finally {
                env.restore(saved);
            }
            initInstance(instance, parent, parentArgs, errorNode, site);
        }

        for (int i = 0; i < params.size(); i++) {
            instance.setField(params.get(i).getName(), values.get(i));
        }

        EzyInstance prevInstance = currentInstance;
        EzyClass prevClass = currentClass;
        currentInstance = instance;
        currentClass = cls;
        try {
            for (ClassField field : cls.getFields()) {
                Object value = field.getDefaultValue() != null ? field.getDefaultValue().accept(this) : null;
                checkFieldType(cls, field, value, errorNode, site);
                instance.setField(field.getName(), value);
            }
        } finally {
            currentInstance = prevInstance;
            currentClass = prevClass;
        }
    }

    private void checkFieldType(EzyClass cls, ClassField field, Object value, Node errorNode, Interpreter site) throws ParseException {
        String type = field.getType().getValue();
        requireKnownType(type, errorNode, site);
        if (!matchesType(type, field.isArray(), value)) {
            throw site.error(errorNode, "Type mismatch: field '" + field.getName() + "' of " + cls.getName() + " expects " + typeLabel(type, field.isArray()) + " but got " + getTypeName(value));
        }
    }

    private ClassField findFieldDecl(EzyClass cls, String name) {
        for (EzyClass c = cls; c != null; c = c.getParentClass()) {
            for (ClassField f : c.getConstructorParams()) {
                if (f.getName().equals(name)) return f;
            }
            for (ClassField f : c.getFields()) {
                if (f.getName().equals(name)) return f;
            }
        }
        return null;
    }

    private void setFieldChecked(EzyInstance instance, String property, Object value, Node node) throws ParseException {
        if (!instance.hasField(property)) {
            throw error(node, "Property '" + property + "' not found on " + instance.getEzyClass().getName());
        }
        ClassField field = findFieldDecl(instance.getEzyClass(), property);
        if (field != null) {
            checkFieldType(instance.getEzyClass(), field, value, node, this);
        }
        instance.setField(property, value);
    }

    @Override
    public Object visitSelfExpr(SelfExpr selfExpr) throws ParseException {
        if (currentInstance == null) {
            throw error(selfExpr, "'self' can only be used inside a class method");
        }
        if (selfExpr.getFieldName() != null) {
            if (!currentInstance.hasField(selfExpr.getFieldName())) {
                throw error(selfExpr, "Property '" + selfExpr.getFieldName() + "' not found on " + currentInstance.getEzyClass().getName());
            }
            return currentInstance.getField(selfExpr.getFieldName());
        }
        return currentInstance;
    }

    @Override
    public Object visitParentExpr(ParentExpr parentExpr) throws ParseException {
        if (currentInstance == null || currentClass == null) {
            throw error(parentExpr, "'parent' can only be used inside a class method");
        }
        EzyClass parent = currentClass.getParentClass();
        if (parent == null) {
            throw error(parentExpr, "Class '" + currentClass.getName() + "' has no parent class");
        }

        String methodName = parentExpr.getMethodName();
        FunctionDecl method = parent.findMethod(methodName);
        if (method == null) {
            throw error(parentExpr, "Method '" + methodName + "' not found in parent class '" + parent.getName() + "'");
        }

        List<Object> args = new ArrayList<>();
        for (Node arg : parentExpr.getArguments()) {
            args.add(arg.accept(this));
        }

        return callMethod(currentInstance, method, args, parent.findDeclaringClass(methodName), parentExpr);
    }

    @Override
    public Object visitPropertyAccess(PropertyAccess propertyAccess) throws ParseException {
        if (propertyAccess.getObject() instanceof Identifier id && importedModules.contains(id.getName())) {
            String moduleName = id.getName();
            Interpreter module = loadedModules.get(moduleName);
            if (module == null) {
                throw error(propertyAccess, "Module '" + moduleName + "' not loaded");
            }
            String property = propertyAccess.getProperty();
            if (module.env.getGlobalConstantScope().containsKey(property)) {
                return module.env.getGlobalConstantScope().get(property);
            }
            if (module.env.getGlobalVariableScope().containsKey(property)) {
                return module.env.getGlobalVariableScope().get(property);
            }
            throw error(propertyAccess, "Property '" + property + "' not found in module '" + moduleName + "'");
        }

        Object object = propertyAccess.getObject().accept(this);
        String property = propertyAccess.getProperty();

        if (object instanceof EzyInstance instance) {
            if (instance.hasField(property)) {
                return instance.getField(property);
            }
            throw error(propertyAccess, "Property '" + property + "' not found on " + instance.getEzyClass().getName());
        }

        throw error(propertyAccess, "Cannot access property '" + property + "' on non-object");
    }

    @Override
    public Object visitPropertyAssign(PropertyAssign propertyAssign) throws ParseException {
        Object object = propertyAssign.getObject().accept(this);
        String property = propertyAssign.getProperty();
        Object value = propertyAssign.getValue().accept(this);
        Token operator = propertyAssign.getOperator();

        EzyInstance instance;
        if (object instanceof EzyInstance inst) {
            instance = inst;
        } else {
            throw error(propertyAssign, "Cannot assign property on non-object");
        }

        if (operator.getToken() != Token.TokenType.EQUAL) {
            Object currentValue = instance.getField(property);
            value = computeCompoundAssignment(currentValue, operator, value, propertyAssign);
        }

        setFieldChecked(instance, property, value, propertyAssign);
        return null;
    }

    private Object computeCompoundAssignment(Object current, Token operator, Object value, Node errorNode) throws ParseException {
        String symbol = operator.getValue();
        if (operator.getToken() == Token.TokenType.PLUS_EQUAL && (current instanceof String || value instanceof String)) {
            return formatValue(current) + formatValue(value);
        }
        if (!Num.isNumber(current) || !Num.isNumber(value)) {
            throw error(errorNode, "Invalid operands for '" + symbol + "' operator");
        }
        return switch (operator.getToken()) {
            case PLUS_EQUAL -> Num.add(current, value);
            case MINUS_EQUAL -> Num.sub(current, value);
            case ASTERISK_EQUAL -> Num.mul(current, value);
            case SLASH_EQUAL -> {
                if (Num.isZero(value)) throw error(errorNode, "Division by zero");
                yield Num.div(current, value);
            }
            case PERCENT_EQUAL -> {
                if (Num.isZero(value)) throw error(errorNode, "Modulo by zero");
                yield Num.mod(current, value);
            }
            default -> throw error(errorNode, "Invalid assignment operator");
        };
    }

    @Override
    public Object visitDecoratorDecl(DecoratorDecl decoratorDecl) throws ParseException {
        if (customDecorators.containsKey(decoratorDecl.getName())) {
            throw error(decoratorDecl, "Decorator '" + decoratorDecl.getName() + "' is already defined");
        }
        customDecorators.put(decoratorDecl.getName(), decoratorDecl);
        return null;
    }

    @Override
    public Object visitEntryBlock(EntryBlock entryBlock) throws ParseException {
        if (!isModule) {
            entryBlock.getBody().accept(this);
        }
        return null;
    }

    private Object callMethod(EzyInstance instance, FunctionDecl method, List<Object> args, EzyClass methodClass, Node errorNode) throws ParseException {
        Interpreter owner = methodClass.getOwner() instanceof Interpreter o ? o : this;
        EzyInstance prevInstance = owner.currentInstance;
        EzyClass prevClass = owner.currentClass;
        owner.currentInstance = instance;
        owner.currentClass = methodClass;
        try {
            return owner.invokeFunction(method.getName(), method, owner.env.global(), args, errorNode, this);
        } finally {
            owner.currentInstance = prevInstance;
            owner.currentClass = prevClass;
        }
    }
}
