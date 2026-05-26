package io.github._3xhaust.ezylang.ast;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.github._3xhaust.ezylang.exception.ParseException;
import io.github._3xhaust.ezylang.lexer.Token;
import lombok.Getter;

public class Ast {

    public interface Visitor<R> {
        R visitProgram(Program program) throws ParseException;
        R visitVariableDecl(VariableDecl variableDecl) throws ParseException;
        R visitConstantDecl(ConstantDecl constantDecl) throws ParseException;
        R visitPrintStatement(PrintStatement printStatement) throws ParseException;
        R visitBinaryExpr(BinaryExpr binaryExpr) throws ParseException;
        R visitLiteral(Literal literal);
        R visitIdentifier(Identifier identifier) throws ParseException;
        R visitArrayAccess(ArrayAccess arrayAccess) throws ParseException;
        R visitIfStatement(IfStatement ifStatement) throws ParseException;
        R visitForStatement(ForStatement forStatement) throws ParseException;
        R visitArrayLiteral(ArrayLiteral arrayLiteral) throws ParseException;
        R visitBlock(Block block) throws ParseException;
        R visitUnaryExpr(UnaryExpr unaryExpr) throws ParseException;
        R visitInterpolatedString(InterpolatedString interpolatedString) throws ParseException;
        R visitWhileStatement(WhileStatement whileStatement) throws ParseException;
        R visitAssignmentStatement(AssignmentStatement assignmentStatement) throws ParseException;
        R visitTypeCastExpr(TypeCastExpr typeCastExpr) throws ParseException;
        R visitTypeCheckExpr(TypeCheckExpr typeCheckExpr) throws ParseException;
        R visitContinueStatement(ContinueStatement continueStatement) throws ParseException;
        R visitBreakStatement(BreakStatement breakStatement) throws ParseException;
        R visitSwitchCase(SwitchCase switchCase) throws ParseException;
        R visitSwitchStatement(SwitchStatement switchStatement) throws ParseException;
        R visitFunctionDecl(FunctionDecl functionDecl) throws ParseException;
        R visitFunctionCall(FunctionCall functionCall) throws ParseException;
        R visitMethodCall(MethodCall methodCall) throws ParseException;
        R visitExpressionStatement(ExpressionStatement expressionStatement) throws ParseException;
        R visitImportStatement(ImportStatement importStatement) throws ParseException;
        R visitReturnStatement(ReturnStatement returnStatement) throws ParseException;
        R visitIncrementDecrementExpr(IncrementDecrementExpr incrementDecrementExpr) throws ParseException;
        R visitTestBlock(TestBlock testBlock) throws ParseException;
        R visitAssertStatement(AssertStatement assertStatement) throws ParseException;
        R visitClassDecl(ClassDecl classDecl) throws ParseException;
        R visitInterfaceDecl(InterfaceDecl interfaceDecl) throws ParseException;
        R visitNewExpr(NewExpr newExpr) throws ParseException;
        R visitSelfExpr(SelfExpr selfExpr) throws ParseException;
        R visitParentExpr(ParentExpr parentExpr) throws ParseException;
        R visitPropertyAccess(PropertyAccess propertyAccess) throws ParseException;
        R visitPropertyAssign(PropertyAssign propertyAssign) throws ParseException;
        R visitDecoratorDecl(DecoratorDecl decoratorDecl) throws ParseException;
        R visitEntryBlock(EntryBlock entryBlock) throws ParseException;
    }

    @Getter
    public abstract static class Node {
        protected int line;
        protected int column;
        public abstract <R> R accept(Visitor<R> visitor) throws ParseException;
    }

    @Getter
    public static class Program extends Node {
        private final List<Node> statements;
        public Program(List<Node> statements) { this.statements = statements; }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitProgram(this); }
    }

    @Getter
    public static class VariableDecl extends Node {
        private final Token type;
        private final String identifier;
        private final boolean isArray;
        private final Node initializer;
        private final Constraint constraint;
        public VariableDecl(Token type, String identifier, boolean isArray, Node initializer, Constraint constraint, int line, int column) {
            this.type = type; this.identifier = identifier; this.isArray = isArray;
            this.initializer = initializer; this.constraint = constraint;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitVariableDecl(this); }
    }

    @Getter
    public static class ConstantDecl extends Node {
        private final String identifier;
        private final Token type;
        private final Node value;
        public ConstantDecl(String identifier, Token type, Node value, int line, int column) {
            this.identifier = identifier; this.type = type; this.value = value;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitConstantDecl(this); }
    }

    @Getter
    public static class PrintStatement extends Node {
        private final Node expression;
        private final boolean isPrintln;
        public PrintStatement(Node expression, boolean isPrintln, int line, int column) {
            this.expression = expression; this.isPrintln = isPrintln;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitPrintStatement(this); }
    }

    @Getter
    public static class IfStatement extends Node {
        private final Node condition;
        private final Node thenBranch;
        private final Node elseBranch;
        public IfStatement(Node condition, Node thenBranch, Node elseBranch, int line, int column) {
            this.condition = condition; this.thenBranch = thenBranch; this.elseBranch = elseBranch;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitIfStatement(this); }
    }

    @Getter
    public static class ForStatement extends Node {
        private final String identifier;
        private final Token type;
        private final Node start;
        private final Node end;
        private final Node step;
        private final Node body;
        public ForStatement(String identifier, Token type, Node start, Node end, Node step, Node body, int line, int column) {
            this.identifier = identifier; this.type = type; this.start = start;
            this.end = end; this.step = step; this.body = body;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitForStatement(this); }
    }

    @Getter
    public static class BinaryExpr extends Node {
        private final Node left;
        private final Token operator;
        private final Node right;
        public BinaryExpr(Node left, Token operator, Node right, int line, int column) {
            this.left = left; this.operator = operator; this.right = right;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitBinaryExpr(this); }
    }

    @Getter
    public static class Literal extends Node {
        private final Object value;
        private final String type;
        public Literal(Object value, String type, int line, int column) {
            this.value = value; this.type = type;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) { return visitor.visitLiteral(this); }
    }

    @Getter
    public static class Identifier extends Node {
        private final String name;
        public Identifier(String name, int line, int column) {
            this.name = name; this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitIdentifier(this); }
    }

    @Getter
    public static class ArrayAccess extends Node {
        private final String identifier;
        private final Node index;
        public ArrayAccess(String identifier, Node index, int line, int column) {
            this.identifier = identifier; this.index = index;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitArrayAccess(this); }
    }

    @Getter
    public static class ArrayLiteral extends Node {
        private final List<Node> elements;
        public ArrayLiteral(List<Node> elements, int line, int column) {
            this.elements = elements; this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitArrayLiteral(this); }
        public String getType() {
            if (elements.isEmpty()) return "unknown[]";
            if (elements.get(0) instanceof Literal literal) {
                String firstElementType = literal.getType();
                for (Node element : elements) {
                    if (element instanceof Literal literalElement) {
                        if (!literalElement.getType().equals(firstElementType)) {
                            throw new RuntimeException("Array elements must have the same type");
                        }
                    }
                }
                return firstElementType + "[]";
            }
            return null;
        }
    }

    @Getter
    public static class Block extends Node {
        private final List<Node> statements;
        public Block(List<Node> statements, int line, int column) {
            this.statements = statements; this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitBlock(this); }
        public List<Node> getStatements() { return Collections.unmodifiableList(statements); }
        @Override public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Block {\n");
            for (Node statement : statements) { sb.append("  ").append(statement.toString().replace("\n", "\n  ")).append("\n"); }
            sb.append("}");
            return sb.toString();
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return Objects.equals(statements, ((Block) o).statements);
        }
        @Override public int hashCode() { return Objects.hash(statements); }
    }

    @Getter
    public static class UnaryExpr extends Node {
        private final Token operator;
        private final Node operand;
        public UnaryExpr(Token operator, Node operand, int line, int column) {
            this.operator = operator; this.operand = operand;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitUnaryExpr(this); }
    }

    @Getter
    public static class InterpolatedString extends Node {
        private final String rawString;
        private final String baseString;
        private final List<Node> expressions;
        public InterpolatedString(String rawString, String baseString, List<Node> expressions, int line, int column) {
            this.rawString = rawString; this.baseString = baseString; this.expressions = expressions;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitInterpolatedString(this); }
    }

    @Getter
    public static class WhileStatement extends Node {
        private final Node condition;
        private final Node body;
        public WhileStatement(Node condition, Node body, int line, int column) {
            this.condition = condition; this.body = body;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitWhileStatement(this); }
    }

    @Getter
    public static class AssignmentStatement extends Node {
        private final Node target;
        private final Token operator;
        private final Node value;
        public AssignmentStatement(Node target, Token operator, Node value, int line, int column) {
            this.target = target; this.operator = operator; this.value = value;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitAssignmentStatement(this); }
    }

    @Getter
    public static class TypeCheckExpr extends Node {
        private final Node expression;
        private final Token type;
        public TypeCheckExpr(Node expression, Token type, int line, int column) {
            this.expression = expression; this.type = type;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitTypeCheckExpr(this); }
    }

    @Getter
    public static class TypeCastExpr extends Node {
        private final Node expression;
        private final Token targetType;
        public TypeCastExpr(Node expression, Token targetType, int line, int column) {
            this.expression = expression; this.targetType = targetType;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitTypeCastExpr(this); }
    }

    @Getter
    public static class BreakStatement extends Node {
        public BreakStatement(int line, int column) { this.line = line; this.column = column; }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitBreakStatement(this); }
    }

    @Getter
    public static class ContinueStatement extends Node {
        public ContinueStatement(int line, int column) { this.line = line; this.column = column; }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitContinueStatement(this); }
    }

    @Getter
    public static class SwitchStatement extends Node {
        private final Node expression;
        private final List<SwitchCase> cases;
        private final Node defaultCase;
        public SwitchStatement(Node expression, List<SwitchCase> cases, Node defaultCase, int line, int column) {
            this.expression = expression; this.cases = cases; this.defaultCase = defaultCase;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitSwitchStatement(this); }
    }

    @Getter
    public static class SwitchCase extends Node {
        private final Node value;
        private final Node body;
        private final boolean isArrowStyle;
        public SwitchCase(Node value, Node body, boolean isArrowStyle, int line, int column) {
            this.value = value; this.body = body; this.isArrowStyle = isArrowStyle;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitSwitchCase(this); }
    }

    @Getter
    public static class FunctionDecl extends Node {
        private final String name;
        private final List<String> paramNames;
        private final List<Token> paramTypes;
        private final List<Boolean> isArrayTypes;
        private final Token returnType;
        private final Boolean isReturnTypeArray;
        private final boolean isMemo;
        private final boolean isOverride;
        private final List<Decorator> decorators;
        private final Node body;
        public FunctionDecl(String name, List<String> paramNames, List<Token> paramTypes, List<Boolean> isArrayTypes, Token returnType, Boolean isReturnTypeArray, boolean isMemo, boolean isOverride, List<Decorator> decorators, Node body, int line, int column) {
            this.name = name; this.paramNames = paramNames; this.paramTypes = paramTypes;
            this.isArrayTypes = isArrayTypes; this.returnType = returnType;
            this.isReturnTypeArray = isReturnTypeArray; this.isMemo = isMemo;
            this.isOverride = isOverride; this.decorators = decorators;
            this.body = body; this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitFunctionDecl(this); }
    }

    @Getter
    public static class FunctionCall extends Node {
        private final String name;
        private final List<Node> arguments;
        public FunctionCall(String name, List<Node> arguments, int line, int column) {
            this.name = name; this.arguments = arguments;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitFunctionCall(this); }
    }

    public static class MethodCall extends Node {
        private final String objectName;
        private final String methodName;
        private final List<Node> arguments;
        private final Node objectNode;
        private boolean propertyAccess = false;
        public MethodCall(String objectName, String methodName, List<Node> arguments, Node objectNode, int line, int column) {
            this.objectName = objectName; this.methodName = methodName;
            this.arguments = arguments; this.objectNode = objectNode;
            this.line = line; this.column = column;
        }
        public String getObjectName() { return objectName; }
        public String getMethodName() { return methodName; }
        public List<Node> getArguments() { return arguments; }
        public Node getObjectNode() { return objectNode; }
        public boolean isPropertyAccess() { return propertyAccess; }
        public void setPropertyAccess(boolean propertyAccess) { this.propertyAccess = propertyAccess; }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitMethodCall(this); }
    }

    @Getter
    public static class ExpressionStatement extends Node {
        private final Node expression;
        public ExpressionStatement(Node expression, int line, int column) {
            this.expression = expression; this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitExpressionStatement(this); }
    }

    @Getter
    public static class ImportStatement extends Node {
        private final String moduleName;
        private final List<ImportItem> items;
        public ImportStatement(String moduleName, List<ImportItem> items, int line, int column) {
            this.moduleName = moduleName; this.items = items;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitImportStatement(this); }
    }

    @Getter
    public static class ImportItem {
        private final String name;
        private final String alias;
        private final boolean isConstant;
        public ImportItem(String name, String alias, boolean isConstant) {
            this.name = name; this.alias = alias; this.isConstant = isConstant;
        }
    }

    @Getter
    public static class ReturnStatement extends Node {
        private final Node value;
        public ReturnStatement(Node value, int line, int column) {
            this.value = value; this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitReturnStatement(this); }
    }

    @Getter
    public static class IncrementDecrementExpr extends Node {
        private final Node operand;
        private final Token operator;
        private final boolean isPrefix;
        public IncrementDecrementExpr(Node operand, Token operator, boolean isPrefix, int line, int column) {
            this.operand = operand; this.operator = operator; this.isPrefix = isPrefix;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitIncrementDecrementExpr(this); }
    }

    @Getter
    public static class Constraint {
        private final Node min;
        private final Node max;
        private final List<Node> allowedValues;
        public Constraint(Node min, Node max) { this.min = min; this.max = max; this.allowedValues = null; }
        public Constraint(List<Node> allowedValues) { this.min = null; this.max = null; this.allowedValues = allowedValues; }
        public boolean isRange() { return min != null; }
        public boolean isEnum() { return allowedValues != null; }
    }

    @Getter
    public static class TestBlock extends Node {
        private final String name;
        private final Node body;
        public TestBlock(String name, Node body, int line, int column) {
            this.name = name; this.body = body;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitTestBlock(this); }
    }

    @Getter
    public static class AssertStatement extends Node {
        private final Node expression;
        public AssertStatement(Node expression, int line, int column) {
            this.expression = expression; this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitAssertStatement(this); }
    }

    @Getter
    public static class ClassField {
        private final String name;
        private final Token type;
        private final boolean isArray;
        private final Node defaultValue;
        private final List<Decorator> decorators;
        public ClassField(String name, Token type, boolean isArray, Node defaultValue, List<Decorator> decorators) {
            this.name = name; this.type = type; this.isArray = isArray;
            this.defaultValue = defaultValue; this.decorators = decorators;
        }
    }

    @Getter
    public static class Decorator {
        private final String name;
        private final List<Node> arguments;
        public Decorator(String name, List<Node> arguments, int line, int column) {
            this.name = name; this.arguments = arguments;
        }
    }

    @Getter
    public static class ClassDecl extends Node {
        private final String name;
        private final List<ClassField> constructorParams;
        private final String parentClass;
        private final List<Node> parentArgs;
        private final List<String> interfaces;
        private final List<ClassField> fields;
        private final List<FunctionDecl> methods;
        private final List<Decorator> decorators;
        public ClassDecl(String name, List<ClassField> constructorParams, String parentClass, List<Node> parentArgs,
                         List<String> interfaces, List<ClassField> fields, List<FunctionDecl> methods,
                         List<Decorator> decorators, int line, int column) {
            this.name = name; this.constructorParams = constructorParams;
            this.parentClass = parentClass; this.parentArgs = parentArgs;
            this.interfaces = interfaces; this.fields = fields; this.methods = methods;
            this.decorators = decorators; this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitClassDecl(this); }
    }

    @Getter
    public static class InterfaceDecl extends Node {
        private final String name;
        private final List<FunctionDecl> methods;
        public InterfaceDecl(String name, List<FunctionDecl> methods, int line, int column) {
            this.name = name; this.methods = methods;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitInterfaceDecl(this); }
    }

    @Getter
    public static class NewExpr extends Node {
        private final String className;
        private final List<Node> arguments;
        public NewExpr(String className, List<Node> arguments, int line, int column) {
            this.className = className; this.arguments = arguments;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitNewExpr(this); }
    }

    @Getter
    public static class SelfExpr extends Node {
        private final String fieldName;
        public SelfExpr(String fieldName, int line, int column) {
            this.fieldName = fieldName; this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitSelfExpr(this); }
    }

    @Getter
    public static class ParentExpr extends Node {
        private final String methodName;
        private final List<Node> arguments;
        public ParentExpr(String methodName, List<Node> arguments, int line, int column) {
            this.methodName = methodName; this.arguments = arguments;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitParentExpr(this); }
    }

    @Getter
    public static class PropertyAccess extends Node {
        private final Node object;
        private final String property;
        public PropertyAccess(Node object, String property, int line, int column) {
            this.object = object; this.property = property;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitPropertyAccess(this); }
    }

    @Getter
    public static class PropertyAssign extends Node {
        private final Node object;
        private final String property;
        private final Token operator;
        private final Node value;
        public PropertyAssign(Node object, String property, Token operator, Node value, int line, int column) {
            this.object = object; this.property = property;
            this.operator = operator; this.value = value;
            this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitPropertyAssign(this); }
    }

    @Getter
    public static class DecoratorDecl extends Node {
        private final String name;
        private final List<String> paramNames;
        private final List<Token> paramTypes;
        private final Node body;
        public DecoratorDecl(String name, List<String> paramNames, List<Token> paramTypes, Node body, int line, int column) {
            this.name = name; this.paramNames = paramNames; this.paramTypes = paramTypes;
            this.body = body; this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitDecoratorDecl(this); }
    }

    @Getter
    public static class EntryBlock extends Node {
        private final Node body;
        public EntryBlock(Node body, int line, int column) {
            this.body = body; this.line = line; this.column = column;
        }
        @Override public <R> R accept(Visitor<R> visitor) throws ParseException { return visitor.visitEntryBlock(this); }
    }
}
