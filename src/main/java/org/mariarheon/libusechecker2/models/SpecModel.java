package org.mariarheon.libusechecker2.models;

import org.mariarheon.libusechecker2.interfaces.IModel;
import ru.spbstu.insys.libsl.parser.PostconditionDecl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class SpecModel implements IModel {
    public final Map<String, String> typeToClassName;
    public final MethodModel[] methods;
    public final Map<String, AutomatonModel> automatonNameToModel;

    public SpecModel(Map<String, String> typeToClassName, MethodModel[] methods,
                     Map<String, AutomatonModel> automatonNameToModel) {
        this.typeToClassName = typeToClassName;
        this.methods = methods;
        this.automatonNameToModel = automatonNameToModel;
    }

    @Override
    public void display() {
        System.out.println("Types:");
        System.out.println("======");
        for (var key : typeToClassName.keySet()) {
            System.out.println(key + " -> " + typeToClassName.get(key));
        }
        System.out.println();
        System.out.println("Methods:");
        System.out.println("========");
        for (var meth : methods) {
            if (meth.isConstructor()) {
                System.out.println("Is constructor");
                System.out.println("Class: " + meth.getClassName());
            } else {
                System.out.println("Return type: " + meth.getReturnType());
                System.out.println("Class: " + meth.getClassName());
                System.out.println("Name: " + meth.getMethodName());
            }
            for (var par : meth.getParameters()) {
                System.out.println("Par #" + par.getIndex() + ": " + par.getParName() + ": " + par.getTypeName());
            }
            var resultAssignment = meth.getResultAssignment();
            if (resultAssignment != null) {
                System.out.println("result = new " + resultAssignment.getAutomatonName()
                        + "(" + resultAssignment.getAutomatonState() + ");");
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("Automata:");
        System.out.println("=========");
        System.out.println();
        for (var autoName : automatonNameToModel.keySet()) {
            System.out.println("Automaton " + autoName + ":");
            var autoModel = automatonNameToModel.get(autoName);
            System.out.println("Has finish states = " + autoModel.hasFinishStates);
            for (var state : autoModel.stateIsFinished.keySet()) {
                if (autoModel.stateIsFinished.get(state)) {
                    System.out.print("finish");
                }
                System.out.println("state " + state);
            }
            for (var meth : autoModel.methodToStateModification.keySet()) {
                System.out.println(meth + " ->");
                var modif = autoModel.methodToStateModification.get(meth);
                for (var fromState : modif.keySet()) {
                    var toState = modif.get(fromState);
                    System.out.println("  " + fromState + " -> " + toState);
                }
            }
            System.out.println();
        }
    }

    private List<String> lines;
    private boolean traditionalSyntax;

    @Override
    public void generateAspects(Path folderPath) throws IOException {
        generateAspects(folderPath, false);
    }

    private void addPackage() {
        lines.add("package aspectpackage;");
        lines.add("");
    }

    private void addImports() {
        if (traditionalSyntax) {
            lines.add("import org.aspectj.lang.JoinPoint;");
            lines.add("import org.aspectj.lang.reflect.CodeSignature;");
        } else {
            lines.add("import org.aspectj.lang.ProceedingJoinPoint;");
            lines.add("import org.aspectj.lang.annotation.Around;");
            lines.add("import org.aspectj.lang.annotation.Aspect;");
        }
        lines.add("import java.lang.ref.Cleaner;");
        lines.add("");
    }

    private void addClass() {
        if (traditionalSyntax) {
            lines.add("aspect Interceptor {");
        } else {
            lines.add("@SuppressWarnings(\"unused\")");
            lines.add("@Aspect");
            lines.add("public class Interceptor {");
        }
        lines.add("    private static Cleaner cleaner = Cleaner.create();");
        lines.add("");
        addClassBody();
        lines.add("}");
    }

    private void addClassBody() {
        for (var method : methods) {
            addMethod(method);
        }
    }

    private String getParameterTypes(MethodModel method) {
        return method.getParameters().stream().map(ParameterModel::getTypeName).collect(Collectors.joining(", "));
    }

    private String getParameterNames(MethodModel method) {
        return method.getParameters().stream().map(ParameterModel::getParName).collect(Collectors.joining(", "));
    }

    private String getParameterTypesAndNames(MethodModel method) {
        return method.getParameters().stream().map(x -> x.getTypeName() + " " + x.getParName()).collect(Collectors.joining(", "));
    }

    private void addPreconditionLines(MethodModel method) {
        lines.add("        ConditionInfo[] postconditions = new ConditionInfo[] {};");
        lines.add("        ConditionInfo[] preconditions = new ConditionInfo[] {");
        for (var x : method.getPreconditions()) {
            lines.add(String.format(
                    "            new ConditionInfo(%s, %s, %s),", x.getJavaExpression(), sfComment(x.getComment()), x.getCondName()));
        }
        lines.add("        };");
    }

    private void addPostconditionLines(MethodModel method) {
        if (!method.getReturnType().equals("void")) {
            lines.add(String.format(
                    "            %s result = (%s) res;",
                    method.getReturnType(), method.getReturnType()));
        }
        lines.add("            postconditions = new ConditionInfo[] {");
        for (var x : method.getPostconditions()) {
            lines.add(String.format(
                    "                new ConditionInfo(%s, %s, %s),", x.getJavaExpression(), sfComment(x.getComment()), x.getCondName()));
        }
        lines.add("            };");
    }

    private String sfComment(String comment) {
        if (comment.length() > 0 && comment.charAt(0) == '(') {
            return "String.format" + comment;
        }
        return comment;
    }

    private String getPointcut(MethodModel method) {
        if (method.isConstructor()) {
            return String.format("call(public %s.new(%s)) && args(%s)",
                    method.getClassName(),
                    getParameterTypes(method),
                    getParameterNames(method));
        }
        return String.format("target(theTarget) && call(public %s %s.%s(%s)) && args(%s)",
                method.getReturnType(),
                method.getClassName(),
                method.getMethodName(),
                getParameterTypes(method),
                getParameterNames(method));
    }

    private String getAroundMethodParameters(MethodModel method) {
        var params = new ArrayList<String>();
        var parameterTypesAndNames = getParameterTypesAndNames(method);
        if (!traditionalSyntax) {
            params.add("ProceedingJoinPoint thisJoinPoint");
        }
        if (!method.isConstructor()) {
            params.add(method.getClassName() + " theTarget");
        }
        if (!parameterTypesAndNames.isEmpty()) {
            params.add(parameterTypesAndNames);
        }
        return String.join(", ", params);
    }

    private void addMethod(MethodModel method) {
        String pointcut = getPointcut(method);
        String methodPars = getAroundMethodParameters(method);
        if (traditionalSyntax) {
            lines.add(String.format("    Object around(%s): %s {", methodPars, pointcut));
        } else {
            lines.add(String.format("    @Around(\"%s\")", pointcut));
            lines.add(String.format("    public Object around%s(%s) throws Throwable {", method.getIndex(), methodPars));
        }
        addMethodBody(method);
        lines.add("    }");
        lines.add("");
    }

    private void addMethodBody(MethodModel method) {
        var parameterNames = getParameterNames(method);
        addPreconditionLines(method);
        lines.add("        Object res;");
        lines.add("        Throwable throwable = null;");
        lines.add("        try {");
        if (traditionalSyntax) {
            if (method.isConstructor()) {
                lines.add("            res = proceed(" + parameterNames + ");");
            } else {
                String maybeComma = parameterNames.isEmpty() ? "" : ", ";
                lines.add("            res = proceed(theTarget" + maybeComma + parameterNames + ");");
            }
        } else {
            lines.add("            res = thisJoinPoint.proceed();");
        }
        if (!method.getReturnType().equals("void")) {
            var returnType = method.getReturnType();
            if (returnType.equals("byte") || returnType.equals("short") || returnType.equals("long") ||
                    returnType.equals("float") || returnType.equals("double") || returnType.equals("boolean")) {
                returnType = Character.toUpperCase(returnType.charAt(0)) + returnType.substring(1);
            }
            if (returnType.equals("char")) {
                returnType = "Character";
            }
            if (returnType.equals("int")) {
                returnType = "Integer";
            }
            lines.add("            if (!(res instanceof " + returnType + ")) {");
            lines.add("                return res;");
            lines.add("            }");
        }
        addPostconditionLines(method);
        lines.add("        } catch (Throwable thr) {");
        lines.add("            throwable = thr;");
        lines.add("            res = new Object();");
        lines.add("        }");
        if (method.isConstructor()) {
            lines.add("        MethodInfo methodInfo = MethodInfo.forConstructor(" + method.getIndex()
                    + ", System.identityHashCode(res));");
        } else {
            if (method.getReturnType().equals("void")) {
                lines.add("        MethodInfo methodInfo = MethodInfo.forVoidMethod(" + method.getIndex()
                        + ", System.identityHashCode(theTarget));");
            } else {
                lines.add("        MethodInfo methodInfo = MethodInfo.forUsualMethod(" + method.getIndex()
                        + ", System.identityHashCode(theTarget), System.identityHashCode(res));");
            }
        }

        for (var param : method.getParameters()) {
            String paramName = param.getParName();
            lines.add(String.format("        methodInfo.addParamValue(\"%s\", %s);", paramName, paramName));
        }
        lines.add("        methodInfo.setResultValue(res);");

        lines.add("        methodInfo.setStackTrace(Thread.currentThread().getStackTrace());");
        lines.add("        methodInfo.setPreconditions(preconditions);");
        lines.add("        methodInfo.setPostconditions(postconditions);");
        lines.add("        final String srcLocation = thisJoinPoint.getSourceLocation().toString();");
        lines.add("        methodInfo.setSourceLocation(srcLocation);");
        lines.add("        Sender.getInst().send(methodInfo);");
        lines.add("        final int resID = System.identityHashCode(res);");
        lines.add("        if (res != null) {");
        lines.add("            cleaner.register(res, () -> {");
        lines.add("                try {");
        lines.add("                    MethodInfo methodInfo2 = MethodInfo.forDestroyedSignal(resID);");
        lines.add("                    methodInfo2.setSourceLocation(srcLocation);");
        lines.add("                    Sender.getInst().send(methodInfo2);");
        lines.add("                } catch (Exception ex) {");
        lines.add("                    System.out.println(\"[sender] error was happened during cleaner.register()\");");
        lines.add("                }");
        lines.add("            });");
        lines.add("        }");
        lines.add("        if (throwable != null) {");
        lines.add("            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}");
        if (traditionalSyntax) {
            lines.add("            if (throwable instanceof RuntimeException) {");
            lines.add("                throw (RuntimeException)throwable;");
            lines.add("            } else if (throwable instanceof Error) {");
            lines.add("                throw (Error)throwable;");
            lines.add("            } else {");
            lines.add("                Rethrower.rethrow(throwable);");
            lines.add("                throw new IllegalStateException(\"Should never get here\", throwable);");
            lines.add("            }");
        } else {
            lines.add("            throw throwable;");
        }
        lines.add("        }");
        lines.add("        return res;");
    }

    public void generateAspects(Path folderPath, boolean traditionalSyntax) throws IOException {
        this.traditionalSyntax = traditionalSyntax;
        lines = new ArrayList<String>();
        var aspectPackagePath = folderPath.resolve("aspectpackage");
        Files.createDirectories(aspectPackagePath);
        var interceptorPath = aspectPackagePath.resolve("Interceptor.java");
        addPackage();
        addImports();
        addClass();
        Files.write(interceptorPath, lines, StandardCharsets.UTF_8);
    }
}
