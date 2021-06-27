package org.mariarheon.libusechecker2;

import org.mariarheon.libusechecker2.interfaces.IModel;
import org.mariarheon.libusechecker2.interfaces.ISLReader;
import org.mariarheon.libusechecker2.models.*;
import ru.spbstu.insys.libsl.parser.LibraryDecl;
import ru.spbstu.insys.libsl.parser.ModelParser;
import ru.spbstu.insys.libsl.parser.VarDecl;

import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class SLReader implements ISLReader {
    @Override
    public IModel read(InputStream inputStream) throws Exception {
        var modelParser = new ModelParser();
        var libDecl = modelParser.parse(inputStream);
        var types = getTypes(libDecl);
        var methods = getMethods(libDecl, types);
        var automata = getAutomata(libDecl);
        return new SpecModel(types, methods, automata);
    }

    private Map<String, AutomatonModel> getAutomata(LibraryDecl decl) throws Exception {
        var res = new HashMap<String, AutomatonModel>();
        for (var automaton : decl.getAutomata()) {
            boolean hasFinishStates = false;
            var stateIsFinished = new HashMap<String, Boolean>();
            var methodToStateModification = new HashMap<String, Map<String, String>>();
            var automatonName = automaton.getName().getTypeName();
            if (res.containsKey(automatonName)) {
                throw new Exception("More than one automaton with the same name declared: " + automatonName);
            }
            for (var state : automaton.getStates()) {
                var stateName = state.getName();
                if (stateName.equals("any") || stateName.equals("self")) {
                    throw new Exception("State with the name " + stateName + " cannot be declared. Automaton " + automatonName);
                }
                if (stateIsFinished.containsKey(stateName)) {
                    throw new Exception("The state " + stateName + " in automaton " + automatonName + " declared in the code more than once");
                }
                stateIsFinished.put(stateName, false);
            }
            for (var state : automaton.getFinishStates()) {
                hasFinishStates = true;
                var stateName = state.getName();
                if (stateName.equals("any") || stateName.equals("self")) {
                    throw new Exception("State with the name " + stateName + " cannot be declared. Automaton " + automatonName);
                }
                if (stateIsFinished.containsKey(stateName)) {
                    throw new Exception("The state " + stateName + " in automaton " + automatonName + " declared in the code more than once");
                }
                stateIsFinished.put(stateName, true);
            }
            for (var shift : automaton.getShifts()) {
                for (var func : shift.getFunctions()) {
                    if (!methodToStateModification.containsKey(func)) {
                        methodToStateModification.put(func, new HashMap<String, String>());
                    }
                    var stateModification = methodToStateModification.get(func);
                    List<String> fromList = new ArrayList<>();
                    if (shift.getFrom().equals("any")) {
                        fromList = new ArrayList<>(stateIsFinished.keySet());
                    } else {
                        fromList.add(shift.getFrom());
                    }
                    for (var fromItem : fromList) {
                        var toItem = shift.getTo();
                        if (shift.getTo().equals("self")) {
                            toItem = fromItem;
                        }
                        if (stateModification.containsKey(fromItem)) {
                            throw new Exception("In automaton " + automatonName + " from the state " + fromItem +
                                    " during function " + func + " execution there can be different destination states, " +
                                    "which is bad.");
                        }
                        if (!stateIsFinished.containsKey(fromItem)) {
                            throw new Exception("In automaton " + automatonName + " the state " +
                                    fromItem + " was not declared but used in transition " + fromItem + "->" + toItem + ".");
                        }
                        if (!stateIsFinished.containsKey(toItem)) {
                            throw new Exception("In automaton " + automatonName + " the state " +
                                    toItem + " was not declared but used in transition " + fromItem + "->" + toItem + ".");
                        }
                        stateModification.put(fromItem, toItem);
                    }
                }
            }
            var vars = automaton.getVars();
            var varTypes = new HashMap<String, VarTypeModel>();
            var varsOrder = vars.stream().map(VarDecl::getName).collect(Collectors.toList());
            for (var var1 : vars) {
                VarTypeModel varType;
                var typeName = var1.getTypeName();
                if (typeName.equals("int")) {
                    varType = VarTypeModel.INT;
                } else if (typeName.equals("boolean")) {
                    varType = VarTypeModel.BOOLEAN;
                } else if (typeName.equals("string")) {
                    varType = VarTypeModel.STRING;
                } else if (typeName.equals("double")) {
                    varType = VarTypeModel.DOUBLE;
                } else {
                    throw new Exception("[error] [bad-var-type] In automaton " + automatonName + " bad unsupported type is used for variable " + var1.getName());
                }
                varTypes.put(var1.getName(), varType);
            }
            var autoModel = new AutomatonModel(automatonName, hasFinishStates, stateIsFinished, methodToStateModification,
                    varTypes, varsOrder);
            res.put(automatonName, autoModel);
        }
        return res;
    }

    private Map<String, String> getTypes(LibraryDecl decl) {
        var res = new HashMap<String, String>();
        for (var typ : decl.getTypes()) {
            var semType = typ.getSemanticType().getTypeName();
            var codType = typ.getCodeType().getTypeName();
            res.put(semType, codType);
        }
        return res;
    }

    private MethodModel[] getMethods(LibraryDecl decl, Map<String, String> types) throws Exception {
        var methods = new ArrayList<MethodModel>();
        for (int i = 0; i < decl.getFunctions().size(); i++) {
            var func = decl.getFunctions().get(i);
            var retVal = func.getReturnValue();
            var isConstructor = func.isConstructor();
            var returnType = retVal == null ? "void" : retVal.getType().toString();
            if (!returnType.equals("void")) {
                if (!types.containsKey(returnType)) {
                    throw new Exception("Failed to find return type \"" + returnType + "\" in 'types' section");
                }
                returnType = types.get(returnType);
            }
            var className = func.getEntity().getType().toString();
            if (!types.containsKey(className)) {
                throw new Exception("Failed to find type for class \"" + className + "\" in 'types' section");
            }
            className = types.get(className);
            var methodName = func.getName();
            var parameters = new ArrayList<ParameterModel>();
            for (int j = 0; j < func.getArgs().size(); j++) {
                var par = func.getArgs().get(j);
                var typeName = par.getType().getTypeName();
                if (!types.containsKey(typeName)) {
                    throw new Exception("Failed to find parameter type \"" + typeName + "\" in 'types' section");
                }
                typeName = types.get(typeName);
                var parName = par.getName();
                parameters.add(new ParameterModel(j, typeName, parName));
            }
            if (func.getResultAssignments().size() > 1) {
                throw new Exception("There should not be more than one statement of the form \"result = new AutomatonName(automatonState);\" Method "
                        + func.getName());
            }
            ResultAssignmentModel resultAssignmentModel = null;
            if (func.getResultAssignments().size() == 1) {
                var resultAssignment = func.getResultAssignments().get(0);
                resultAssignmentModel = new ResultAssignmentModel(resultAssignment.getAutomatonName(),
                        resultAssignment.getAutomatonState(),
                        resultAssignment.getVarValues());
            }
            var methodModel = new MethodModel(i, isConstructor, returnType, className, methodName, parameters,
                    resultAssignmentModel, func.getPreconditions(), func.getPostconditions(), func.getWhens(),
                    func.getRequiresList(), func.getEnsuresList(), func.getSetVarList());
            methods.add(methodModel);
        }
        var methodsArray = new MethodModel[methods.size()];
        methods.toArray(methodsArray);
        return methodsArray;
    }
}
