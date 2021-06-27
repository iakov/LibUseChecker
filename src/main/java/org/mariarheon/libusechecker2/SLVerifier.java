package org.mariarheon.libusechecker2;

import org.mariarheon.libusechecker2.models.MethodModel;
import org.mariarheon.libusechecker2.models.ResultAssignmentModel;
import org.mariarheon.libusechecker2.models.SpecModel;
import ru.spbstu.insys.libsl.parser.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SLVerifier {
    private final SpecModel model;
    private final Map<Integer, Automaton> automata;
    private final StringConsumer consumer;

    public SLVerifier(SpecModel model, StringConsumer consumer) {
        this.model = model;
        this.automata = new HashMap<>();
        this.consumer = consumer;
    }

    private void showWrongConditions(ConditionInfo[] conditions, String preOrPost, String[] stackTrace) {
        for (var cond : conditions) {
            if (!cond.isSatisfied()) {
                consumer.consumeError(String.format("[error] [%s:%s] %s", preOrPost, cond.getCondName(), cond.getFormatString()),
                        Util.combinedStackTrace(stackTrace));
            }
        }
    }

    public void methodIntercepted(MethodInfo methodInfo) throws Exception {
        var methodIndex = methodInfo.getMethodIndex();
        var returnObjectID = methodInfo.getReturnObjectId();
        var meth = model.methods[methodIndex];
        var st = methodInfo.getStackTrace();
        currentMeth = meth;
        currentMethodInfo = methodInfo;
        currentSt = st;
        consumer.consume(String.format("[info] %s %s",
                methodInfo.getSourceLocation(),
                meth.getSimpleSignature()));
        showWrongConditions(methodInfo.getPreconditions(), "precondition", st);
        showWrongConditions(methodInfo.getPostconditions(), "postcondition", st);
        // BEGIN: printing param values
        for (var param : methodInfo.getAllParamValues().entrySet()) {
            Object value = param.getValue();
            consumer.consume(param.getKey() + ": " + value);
        }
        // END: printing param values
        verifyRequiresContracts();
        changeAutomatonVariables();
        verifyEnsuresContracts();
        for (var when : meth.getWhens()) {
            String whenVar = when.getVarName();
            var paramValues = methodInfo.getAllParamValues();
            if (!paramValues.containsKey(whenVar)) {
                throw new Exception("[error] [unknown-when-var] Unknown variable in When declaration in method: " + meth.getMethodName());
            }
            var whenVarValue = paramValues.get(whenVar);
            var caseDeclOpt = when.getCases().stream().filter(x -> !x.isElse() && whenVarValue.equals(convertVal(x.getVarValue()))).findFirst();
            CaseDecl caseDecl = null;
            if (caseDeclOpt.isEmpty()) {
                var elseCaseDecl = when.getCases().stream().filter(CaseDecl::isElse).findFirst();
                if (elseCaseDecl.isPresent()) {
                    caseDecl = elseCaseDecl.get();
                }
            } else {
                caseDecl = caseDeclOpt.get();
            }
            if (caseDecl != null) {
                var actDecl = caseDecl.getActionDecl();
                var resAssDecl = caseDecl.getResultAssignmentDecl();
                if (actDecl != null) {
                    if (actDecl.getName().equals("ERROR")) {
                        consumer.consumeError(String.format("[error] [error-action] %s", actDecl.getArgs()),
                                Util.combinedStackTrace(st));
                    } else {
                        consumer.consume("unknown action: " + actDecl.getName());
                    }
                }
                if (resAssDecl != null) {
                    ResultAssignmentModel resAssModel = new ResultAssignmentModel(resAssDecl.getAutomatonName(),
                            resAssDecl.getAutomatonState(),
                            resAssDecl.getVarValues());
                    processResAssignment(meth, resAssModel, returnObjectID, methodInfo, st);
                }
            }
        }
        var resAssignment = meth.getResultAssignment();
        if (resAssignment != null) {
            processResAssignment(meth, resAssignment, returnObjectID, methodInfo, st);
        }
        Automaton automaton = getAutomaton();
        if (automaton != null) {
            automaton.checkStateAndShift(meth.getMethodName(), st);
        }
    }

    private void changeAutomatonVariables() {
        var setVarList = currentMeth.getSetVarList();
        var automaton = getAutomaton();
        if (automaton == null) {
            return;
        }
        var varValues = automaton.getVarValues();
        for (var setVarDecl : setVarList) {
            Object evaluated = eval(setVarDecl.getExpr());
            String varName = setVarDecl.getVarName();
            if (!varValues.containsKey(varName)) {
                consumer.consumeError("[error] no automaton variable name called \"" + varName + "\"", Util.combinedStackTrace(currentSt));
            } else {
                try {
                    automaton.setVarValueWithCheck(varName, evaluated);
                } catch (Exception ex) {
                    consumer.consumeError("[error] " + ex.getMessage(), Util.combinedStackTrace(currentSt));
                }
            }
        }
    }

    private IExpr currentExpr;
    private MethodModel currentMeth;
    private MethodInfo currentMethodInfo;
    private String[] currentSt;

    private void verifyRequiresContracts() {
        for (var requireDecl : currentMeth.getRequiresList()) {
            mustEvalBoolExpr(requireDecl.getExpr());
        }
    }

    private void verifyEnsuresContracts() {
        for (var ensureDecl : currentMeth.getEnsuresList()) {
            mustEvalBoolExpr(ensureDecl.getExpr());
        }
    }

    private void mustEvalBoolExpr(IExpr expr) {
        Object res = eval(expr);
        if (!(res instanceof Boolean)) {
            consumer.consume("[error] the expression should have boolean result: " + expr);
            return;
        }
        if (res.equals(true)) {
            consumer.consume("[info] expression " + expr + " returns true");
        } else {
            consumer.consumeError("[error] expression " + expr + " returns false",
                    Util.combinedStackTrace(currentSt));
        }
    }

    private Object eval(IExpr expr) {
        currentExpr = expr;
        return evalInternal(expr);
    }

    private Object evalInternal(IExpr expr) {
        if (expr instanceof BinaryExpr) {
            var binExpr = (BinaryExpr) expr;
            Object op1 = evalInternal(binExpr.getExpr1());
            Object op2 = evalInternal(binExpr.getExpr2());
            String op = binExpr.getOp();
            if (isArithmOperation(op)) {
                if (isOnlyIntOperation(op)) {
                    if (!(op1 instanceof Integer && op2 instanceof Integer)) {
                        consumer.consume("[error] for arithmetic operation \"" + op + "\" both " +
                                "operands should be integers in expression: " + expr.toString());
                        return 0;
                    }
                    return arithmResult(op, (int)op1, (int)op2);
                }
                if (!(op1 instanceof Integer && op2 instanceof Integer ||
                        op1 instanceof Double && op2 instanceof Double)) {
                    consumer.consume("[error] for arithmetic operation \"" + op + "\" both " +
                            "operands should be integers or doubles in expression: " + expr.toString());
                    return 0;
                }
                if (op1 instanceof Integer) {
                    return arithmResult(op, (int)op1, (int)op2);
                }
                return arithmResult(op, (double)op1, (double)op2);
            }
            return boolResult(op, op1, op2);
        }
        if (expr instanceof UnaryExpr) {
            var unExpr = (UnaryExpr) expr;
            String op = unExpr.getOp();
            Object op1 = evalInternal(unExpr.getExpr());
            if (op.equals("-") || op.equals("+")) {
                if (!(op1 instanceof Integer || op1 instanceof Double)) {
                    consumer.consume("[error] for arithmetic unary operation \"" + op + "\" the " +
                            "operand should be Integer or Double");
                    return 0;
                }
                if (op1 instanceof Integer) {
                    return arithmResult(op, (int) op1);
                }
                return arithmResult(op, (double) op1);
            }
            if (op.equals("~")) {
                if (!(op1 instanceof Integer)) {
                    consumer.consume("[error] for arithmetic unary operation \"" + op + "\" the " +
                            "operand should be Integer");
                    return 0;
                }
                return arithmResult(op, (int) op1);
            }
            if (op.equals("!")) {
                if (!(op1 instanceof Boolean)) {
                    consumer.consume("[error] for boolean unary operation \"" + op + "\" the " +
                            "operand should be of type Boolean");
                    return true;
                }
                return boolResult(op, (boolean) op1);
            }
            throw new RuntimeException("Unsupported operation: " + op);
        }
        if (expr instanceof PrimaryExprIdentifier || expr instanceof OldIdentifier) {
            String identifier;
            if (expr instanceof PrimaryExprIdentifier) {
                identifier = ((PrimaryExprIdentifier) expr).getIdentifier();
            } else {
                identifier = ((OldIdentifier) expr).getIdentifier();
            }
            var paramValues = currentMethodInfo.getAllParamValues();
            Map<String, Object> varValues = getAutomatonVariables();
            if ((paramValues.containsKey("result") || varValues.containsKey("result")) && identifier.equals("result")) {
                consumer.consume("[error] The \"result\" cannot be used in expression " + currentExpr + " because " +
                        "automaton variable or parameter exists with the same name. 0 value will be used instead");
                return 0;
            }
            if ((paramValues.containsKey(identifier) && varValues.containsKey(identifier))) {
                consumer.consume("[error] Identifier " + identifier + " in expression " + currentExpr +
                        " can not be used due to ambiguaty of resolving the name: the same name is used as " +
                        "automaton variable and the method parameter. Parameter value will be used instead");
                return paramValues.get(identifier);
            }
            if (paramValues.containsKey(identifier)) {
                return paramValues.get(identifier);
            }
            if (varValues.containsKey(identifier)) {
                return varValues.get(identifier);
            }
            if (identifier.equals("result")) {
                return currentMethodInfo.getResultValue();
            }
            consumer.consume("[error] Identifier " + identifier + " was not found in method parameters list and " +
                    "in automaton variables list and it's not a \"result\"-special identifier. Use one of them. " +
                    "0-value will be used instead.");
            return 0;
        }
        if (expr instanceof IntegerLiteral) {
            return ((IntegerLiteral)expr).getValue();
        }
        if (expr instanceof FloatLiteral) {
            return ((FloatLiteral) expr).getValue();
        }
        if (expr instanceof CharLiteral) {
            return ((CharLiteral) expr).getValue();
        }
        if (expr instanceof StringLiteral) {
            return ((StringLiteral) expr).getValue();
        }
        if (expr instanceof BoolLiteral) {
            return ((BoolLiteral) expr).getValue();
        }
        throw new RuntimeException("Unsupported expression type: " + expr.getClass().getName());
    }

    private Map<String, Object> getAutomatonVariables() {
        Map<String, Object> varValues = new HashMap<String, Object>();
        if (!currentMeth.isConstructor()) {
            Automaton automaton = getAutomaton();
            if (automaton != null) {
                varValues = automaton.getVarValues();
            }
        }
        return varValues;
    }

    private Automaton getAutomaton() {
        Automaton automaton = null;
        if (!currentMeth.isConstructor()) {
            synchronized (this) {
                if (automata.containsKey(currentMethodInfo.getTargetId())) {
                    automaton = automata.get(currentMethodInfo.getTargetId());
                }
            }
        }
        return automaton;
    }

    private boolean boolResult(String op, boolean op1) {
        if (op.equals("!")) {
            return !op1;
        }
        throw new RuntimeException("Unsupported boolean unary operation: " + op);
    }

    private int arithmResult(String op, int op1) {
        if (op.equals("+")) {
            return op1;
        }
        if (op.equals("-")) {
            return -op1;
        }
        if (op.equals("~")) {
            return ~op1;
        }
        throw new RuntimeException("bad operation for arithmresult: " + op);
    }

    private double arithmResult(String op, double op1) {
        if (op.equals("+")) {
            return op1;
        }
        if (op.equals("-")) {
            return -op1;
        }
        throw new RuntimeException("bad operation for arithmresult: " + op);
    }

    private int arithmResult(String op, int op1, int op2) {
        if (op.equals("+")) {
            return op1 + op2;
        }
        if (op.equals("-")) {
            return op1 - op2;
        }
        if (op.equals("*")) {
            return op1 * op2;
        }
        if (op.equals("/")) {
            return op1 / op2;
        }
        if (op.equals("%")) {
            return op1 % op2;
        }
        if (op.equals("&")) {
            return op1 & op2;
        }
        if (op.equals("^")) {
            return op1 ^ op2;
        }
        if (op.equals("|")) {
            return op1 | op2;
        }
        throw new RuntimeException("Unsupported operation: " + op);
    }

    private double arithmResult(String op, double op1, double op2) {
        if (op.equals("+")) {
            return op1 + op2;
        }
        if (op.equals("-")) {
            return op1 - op2;
        }
        if (op.equals("*")) {
            return op1 * op2;
        }
        if (op.equals("/")) {
            return op1 / op2;
        }
        if (op.equals("%")) {
            return op1 % op2;
        }
        throw new RuntimeException("Unsupported operation: " + op);
    }

    private boolean boolResult(String op, Object op1, Object op2) {
        if (op.equals("<=") || op.equals(">=") || op.equals(">") || op.equals("<")) {
            // only ints
            if (!(op1 instanceof Integer && op2 instanceof Integer ||
                    op1 instanceof Double && op2 instanceof Double)) {
                consumer.consume("[error] operation \"" + op + "\" requires integer or double operands");
                return true;
            }
            if (op1 instanceof Integer) {
                return simpleBoolResult(op, (int) op1, (int) op2);
            }
            return simpleBoolResult(op, (double) op1, (double) op2);
        } else if (op.equals("&&") || op.equals("||")) {
            // only bools
            if (!(op1 instanceof Boolean) || !(op2 instanceof Boolean)) {
                consumer.consume("[error] operation \"" + op + "\" requires boolean operands");
                return true;
            }
            return simpleBoolResult(op, (boolean) op1, (boolean) op2);
        } else {
            // ints and bools
            // "=", "!="
            if (!(op1 instanceof Boolean && op2 instanceof Boolean ||
                    op1 instanceof String && op2 instanceof String ||
                    op1 instanceof Integer && op2 instanceof Integer ||
                    op1 instanceof Double && op2 instanceof Double)) {
                consumer.consume("[error] operation \"" + op + "\" requires both operands to be the same types and be Booleans, Strings, Integers or Doubles");
                return true;
            }
            if (op1 instanceof Boolean) {
                return simpleBoolResult(op, (boolean) op1, (boolean) op2);
            }
            if (op1 instanceof String) {
                return simpleBoolResult(op, (String) op1, (String) op2);
            }
            if (op1 instanceof Integer) {
                return simpleBoolResult(op, (int) op1, (int) op2);
            }
            if (op1 instanceof Double) {
                return simpleBoolResult(op, (double) op1, (double) op2);
            }
            throw new RuntimeException("Unimplemented type of operands for operation: " + op);
        }
    }

    private boolean simpleBoolResult(String op, int op1, int op2) {
        if (op.equals("<=")) {
            return op1 <= op2;
        }
        if (op.equals(">=")) {
            return op1 >= op2;
        }
        if (op.equals(">")) {
            return op1 > op2;
        }
        if (op.equals("<")) {
            return op1 < op2;
        }
        if (op.equals("=")) {
            return op1 == op2;
        }
        if (op.equals("!=")) {
            return op1 != op2;
        }
        throw new RuntimeException("Unsupported operation (simpleBoolResult): " + op);
    }

    private boolean simpleBoolResult(String op, double op1, double op2) {
        if (op.equals("<=")) {
            return op1 <= op2;
        }
        if (op.equals(">=")) {
            return op1 >= op2;
        }
        if (op.equals(">")) {
            return op1 > op2;
        }
        if (op.equals("<")) {
            return op1 < op2;
        }
        if (op.equals("=")) {
            return Math.abs(op1 - op2) <= 0.000001;
        }
        if (op.equals("!=")) {
            return Math.abs(op1 - op2) > 0.000001;
        }
        throw new RuntimeException("Unsupported operation (simpleBoolResult): " + op);
    }

    private boolean simpleBoolResult(String op, boolean op1, boolean op2) {
        if (op.equals("&&")) {
            return op1 && op2;
        }
        if (op.equals("||")) {
            return op1 || op2;
        }
        if (op.equals("=")) {
            return op1 == op2;
        }
        if (op.equals("!=")) {
            return op1 != op2;
        }
        throw new RuntimeException("Unsupported operation (simpleBoolResult,2): " + op);
    }

    private boolean simpleBoolResult(String op, String op1, String op2) {
        if (op.equals("=")) {
            return op1.equals(op2);
        }
        if (op.equals("!=")) {
            return !op1.equals(op2);
        }
        throw new RuntimeException("Unsupported operation (simpleBoolResult,3): " + op);
    }

    // with integer result
    private boolean isArithmOperation(String op) {
        return op.equals("*") || op.equals("/") || op.equals("%")
                || op.equals("+") || op.equals("-") || op.equals("&")
                || op.equals("|") || op.equals("^");
    }

    private boolean isOnlyIntOperation(String op) {
        return op.equals("&")
                || op.equals("|") || op.equals("^");
    }

    private boolean isBoolOperation(String op) {
        return !isArithmOperation(op);
    }

    private Object convertVal(String val) {
        if (val.equals("true")) {
            return true;
        }
        if (val.equals("false")) {
            return false;
        }
        if (val.length() > 1 && val.charAt(0) == '"' && val.charAt(val.length() - 1) == '"') {
            return val.substring(1, val.length() - 1);
        }
        try {
            return Integer.parseInt(val);
        } catch (Exception ex) {
            return null;
        }
    }

    private void processResAssignment(MethodModel meth, ResultAssignmentModel resAssignment, int returnObjectID,
                                      MethodInfo methodInfo, String[] st) throws Exception {
        var automatonModel = model.automatonNameToModel.get(resAssignment.getAutomatonName());
        if (automatonModel == null) {
            throw new Exception("[error] [missing-automaton] No automaton named " + resAssignment.getAutomatonName());
        }
        synchronized (this) {
            if (!automata.containsKey(returnObjectID)) {
                var automaton = new Automaton(automatonModel, resAssignment.getAutomatonState(), consumer, returnObjectID,
                        methodInfo.getSourceLocation(), st, resAssignment.getVarValues().stream().map(this::eval).collect(Collectors.toList()));
                automata.put(returnObjectID, automaton);
            }
        }
    }

    public void killAutomaton(int targetID) {
        Automaton a;
        synchronized (this) {
            a = automata.remove(targetID);
        }
        if (a == null) {
            return;
        }
        a.verifyFinishState();
        consumer.consume("[info] Automaton #" + targetID + " removed due to GC: Finish state was verified");
    }

    public void checkEndState() {
        synchronized (this) {
            for (var targetID : automata.keySet()) {
                automata.get(targetID).verifyFinishState();
            }
            automata.clear();
        }
    }
}
