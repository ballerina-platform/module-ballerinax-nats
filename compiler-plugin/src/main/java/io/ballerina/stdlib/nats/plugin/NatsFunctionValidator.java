/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.nats.plugin;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.stdlib.nats.plugin.PluginUtils.getMethodSymbol;
import static io.ballerina.stdlib.nats.plugin.PluginUtils.isRemoteFunction;
import static io.ballerina.stdlib.nats.plugin.PluginUtils.validateModuleId;

/**
 * Nats remote function validator.
 */
public class NatsFunctionValidator {

    private final SyntaxNodeAnalysisContext context;
    private final ServiceDeclarationNode serviceDeclarationNode;
    FunctionDefinitionNode onMessage;
    FunctionDefinitionNode onRequest;
    FunctionDefinitionNode onError;
    boolean onMessageWithDataBindingFirstParam;

    public NatsFunctionValidator(SyntaxNodeAnalysisContext context, FunctionDefinitionNode onMessage,
                                 FunctionDefinitionNode onRequest, FunctionDefinitionNode onError) {
        this.context = context;
        this.serviceDeclarationNode = (ServiceDeclarationNode) context.node();
        this.onMessage = onMessage;
        this.onRequest = onRequest;
        this.onError = onError;
    }

    public void validate() {
        validateMandatoryFunction();
        if (Objects.nonNull(onMessage)) {
            validateOnMessage();
        }
        if (Objects.nonNull(onRequest)) {
            validateOnRequest();
        }
        if (Objects.nonNull(onError)) {
            validateOnError();
        }
    }

    private void validateMandatoryFunction() {
        if (Objects.isNull(onMessage) && Objects.isNull(onRequest)) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    CompilationErrors.NO_ON_MESSAGE_OR_ON_REQUEST,
                    DiagnosticSeverity.ERROR, serviceDeclarationNode.location()));
        } else if (!Objects.isNull(onMessage) && !Objects.isNull(onRequest)) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.ON_MESSAGE_OR_ON_REQUEST,
                    DiagnosticSeverity.ERROR, serviceDeclarationNode.location()));
        }
    }

    private void validateOnMessage() {
        if (!isRemoteFunction(context, onMessage)) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    CompilationErrors.FUNCTION_SHOULD_BE_REMOTE,
                    DiagnosticSeverity.ERROR, onMessage.functionSignature().location()));
        }
        SeparatedNodeList<ParameterNode> parameters = onMessage.functionSignature().parameters();
        validateFunctionParameters(parameters, onMessage);
        validateReturnTypeErrorOrNil(onMessage);
    }

    private void validateOnRequest() {
        if (!isRemoteFunction(context, onRequest)) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    CompilationErrors.FUNCTION_SHOULD_BE_REMOTE,
                    DiagnosticSeverity.ERROR, onRequest.functionSignature().location()));
        }
        SeparatedNodeList<ParameterNode> parameters = onRequest.functionSignature().parameters();
        validateFunctionParameters(parameters, onRequest);
        validateOnRequestReturnType(onRequest);
    }

    private void validateOnError() {
        if (!isRemoteFunction(context, onError)) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    CompilationErrors.FUNCTION_SHOULD_BE_REMOTE,
                    DiagnosticSeverity.ERROR, onError.functionSignature().location()));
        }
        SeparatedNodeList<ParameterNode> parameters = onError.functionSignature().parameters();
        validateOnErrorFunctionParameters(parameters, onError);
        validateReturnTypeErrorOrNil(onError);
    }

    private void validateFunctionParameters(SeparatedNodeList<ParameterNode> parameters,
                                            FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.size() == 0) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.MUST_HAVE_MESSAGE_OR_ANYDATA,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.functionSignature().location()));
        }

        if (parameters.size() == 1) {
            validateFirstParam(parameters.get(0));
        }

        if (parameters.size() == 2) {
            validateFirstParam(parameters.get(0));
            validateSecondParam(parameters.get(1));
        }

        if (parameters.size() > 2) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.ONLY_PARAMS_ALLOWED,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.functionSignature().location()));
        }
    }

    private void validateOnErrorFunctionParameters(SeparatedNodeList<ParameterNode> parameters,
                                                   FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.size() > 1) {
            validateFirstParamOnError(parameters.get(0));
            validateErrorParam(parameters.get(1));
        }
        if (parameters.size() < 2) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.MUST_HAVE_MESSAGE_AND_ERROR,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.functionSignature().location()));
        }
        if (parameters.size() > 2) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.ONLY_PARAMS_ALLOWED_ON_ERROR,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.functionSignature().location()));
        }
    }

    private void validateFirstParam(ParameterNode parameterNode) {
        onMessageWithDataBindingFirstParam = false;
        // first param can be nats:Message or anydata
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        SemanticModel semanticModel = context.semanticModel();
        Optional<Symbol> symbol = semanticModel.symbol(requiredParameterNode);
        if (symbol.isPresent()) {
            ParameterSymbol parameterSymbol = (ParameterSymbol) symbol.get();
            if (parameterSymbol.typeDescriptor().typeKind() == TypeDescKind.TYPE_REFERENCE) {
                if (!isValidParamTypeMessage((TypeReferenceTypeSymbol) parameterSymbol.typeDescriptor()) &&
                        !isRecordTypeReference((TypeReferenceTypeSymbol) parameterSymbol.typeDescriptor())) {
                    context.reportDiagnostic(PluginUtils.getDiagnostic(
                            CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE_OR_ANYDATA,
                            DiagnosticSeverity.ERROR, requiredParameterNode.location()));
                }
            } else if (parameterSymbol.typeDescriptor().typeKind() == TypeDescKind.INTERSECTION) {
                IntersectionTypeSymbol intersectionTypeSymbol =
                        (IntersectionTypeSymbol) parameterSymbol.typeDescriptor();
                // check if nats:Message is included in the intersection
                validateIntersectionType(intersectionTypeSymbol, requiredParameterNode);
            } else if (parameterSymbol.typeDescriptor().typeKind() == TypeDescKind.ARRAY) {
                TypeSymbol member = ((ArrayTypeSymbol) (parameterSymbol.typeDescriptor())).
                        memberTypeDescriptor();
                if (member.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                    if (((TypeDefinitionSymbol) (((TypeReferenceTypeSymbol) member).definition()))
                            .typeDescriptor().typeKind() != TypeDescKind.RECORD) {
                        context.reportDiagnostic(PluginUtils.getDiagnostic(
                                CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE_OR_ANYDATA,
                                DiagnosticSeverity.ERROR, requiredParameterNode.location()));
                    }
                } else if (!isParameterTypeAnydata(member.typeKind())) {
                    context.reportDiagnostic(PluginUtils.getDiagnostic(
                            CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE_OR_ANYDATA,
                            DiagnosticSeverity.ERROR, requiredParameterNode.location()));
                }
            } else if (!isParameterTypeAnydata(parameterSymbol.typeDescriptor().typeKind())) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(
                        CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE_OR_ANYDATA,
                        DiagnosticSeverity.ERROR, requiredParameterNode.location()));
            }
        }
    }

    private void validateFirstParamOnError(ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        SemanticModel semanticModel = context.semanticModel();
        Optional<Symbol> symbol = semanticModel.symbol(requiredParameterNode);
        if (symbol.isPresent()) {
            ParameterSymbol parameterSymbol = (ParameterSymbol) symbol.get();
            if (parameterSymbol.typeDescriptor().typeKind() == TypeDescKind.TYPE_REFERENCE) {
                if (!isValidParamTypeMessage((TypeReferenceTypeSymbol) parameterSymbol.typeDescriptor())) {
                    context.reportDiagnostic(PluginUtils.getDiagnostic(
                            CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE,
                            DiagnosticSeverity.ERROR, requiredParameterNode.location()));
                }
            } else if (parameterSymbol.typeDescriptor().typeKind() == TypeDescKind.INTERSECTION) {
                IntersectionTypeSymbol intersectionTypeSymbol =
                        (IntersectionTypeSymbol) parameterSymbol.typeDescriptor();
                // check if nats:Message is included in the intersection
                validateIntersectionType(intersectionTypeSymbol, requiredParameterNode);
            } else {
                context.reportDiagnostic(PluginUtils.getDiagnostic(
                        CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE,
                        DiagnosticSeverity.ERROR, requiredParameterNode.location()));
            }
        }
    }

    private boolean isRecordTypeReference(TypeReferenceTypeSymbol typeReferenceTypeSymbol) {
        if (typeReferenceTypeSymbol.definition() instanceof ClassSymbol) {
            return false;
        }
        TypeDefinitionSymbol typeDefinitionSymbol = (TypeDefinitionSymbol) typeReferenceTypeSymbol.definition();
        boolean isRecord = typeDefinitionSymbol.typeDescriptor().typeKind() == TypeDescKind.RECORD;
        if (isRecord) {
            onMessageWithDataBindingFirstParam = true;
        }
        return isRecord;
    }

    private boolean isParameterTypeAnydata(TypeDescKind typeDescKind) {
        boolean isAnydata = typeDescKind == TypeDescKind.INT || typeDescKind == TypeDescKind.STRING ||
                typeDescKind == TypeDescKind.BOOLEAN || typeDescKind == TypeDescKind.FLOAT ||
                typeDescKind == TypeDescKind.DECIMAL || typeDescKind == TypeDescKind.RECORD ||
                typeDescKind == TypeDescKind.MAP || typeDescKind == TypeDescKind.BYTE ||
                typeDescKind == TypeDescKind.TABLE || typeDescKind == TypeDescKind.JSON ||
                typeDescKind == TypeDescKind.XML || typeDescKind == TypeDescKind.ANYDATA;
        if (isAnydata) {
            onMessageWithDataBindingFirstParam = true;
        }
        return isAnydata;
    }

    private void validateSecondParam(ParameterNode parameterNode) {
        // second param can only be anydata
        // if the first param is also anydata, second cannot be anydata
        if (onMessageWithDataBindingFirstParam) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    CompilationErrors.DATA_BINDING_ALREADY_EXISTS,
                    DiagnosticSeverity.ERROR, parameterNode.location()));
            return;
        }
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        SemanticModel semanticModel = context.semanticModel();
        Optional<Symbol> symbol = semanticModel.symbol(requiredParameterNode);
        if (symbol.isPresent()) {
            ParameterSymbol parameterSymbol = (ParameterSymbol) symbol.get();
            if (parameterSymbol.typeDescriptor().typeKind() == TypeDescKind.TYPE_REFERENCE) {
                if (!isRecordTypeReference((TypeReferenceTypeSymbol) parameterSymbol.typeDescriptor())) {
                    context.reportDiagnostic(PluginUtils.getDiagnostic(
                            CompilationErrors.INVALID_FUNCTION_PARAM_ANYDATA,
                            DiagnosticSeverity.ERROR, requiredParameterNode.location()));
                }
            } else if (parameterSymbol.typeDescriptor().typeKind() == TypeDescKind.INTERSECTION) {
                IntersectionTypeSymbol intersectionTypeSymbol =
                        (IntersectionTypeSymbol) parameterSymbol.typeDescriptor();
                // check if nats:Message is included in the intersection
                validateIntersectionTypeForSecondParam(intersectionTypeSymbol, requiredParameterNode);
            } else if (parameterSymbol.typeDescriptor().typeKind() == TypeDescKind.ARRAY) {
                TypeSymbol member = ((ArrayTypeSymbol) (parameterSymbol.typeDescriptor())).
                        memberTypeDescriptor();
                if (member.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                    if (((TypeDefinitionSymbol) (((TypeReferenceTypeSymbol) member).definition()))
                            .typeDescriptor().typeKind() != TypeDescKind.RECORD) {
                        context.reportDiagnostic(PluginUtils.getDiagnostic(
                                CompilationErrors.INVALID_FUNCTION_PARAM_ANYDATA,
                                DiagnosticSeverity.ERROR, requiredParameterNode.location()));
                    }
                } else if (!isParameterTypeAnydata(member.typeKind())) {
                    context.reportDiagnostic(PluginUtils.getDiagnostic(
                            CompilationErrors.INVALID_FUNCTION_PARAM_ANYDATA,
                            DiagnosticSeverity.ERROR, requiredParameterNode.location()));
                }
            } else if (!isParameterTypeAnydata(parameterSymbol.typeDescriptor().typeKind())) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(
                        CompilationErrors.INVALID_FUNCTION_PARAM_ANYDATA,
                        DiagnosticSeverity.ERROR, requiredParameterNode.location()));
            }
        }
    }

    private void validateIntersectionTypeForSecondParam(IntersectionTypeSymbol intersectionTypeSymbol,
                                                        RequiredParameterNode requiredParameterNode) {
        // (readonly & nats:Message - valid, readonly & nats:Client - invalid)
        // (readonly & string - invalid)
        TypeReferenceTypeSymbol typeReferenceTypeSymbol;
        int hasType = 0;
        List<TypeSymbol> intersectionMembers = intersectionTypeSymbol.memberTypeDescriptors();
        for (TypeSymbol typeSymbol : intersectionMembers) {
            if (typeSymbol.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) typeSymbol;
                if (((TypeDefinitionSymbol) typeReferenceTypeSymbol.definition()).typeDescriptor().typeKind() !=
                        TypeDescKind.RECORD) {
                    context.reportDiagnostic(PluginUtils.getDiagnostic(
                            CompilationErrors.INVALID_FUNCTION_PARAM_ANYDATA,
                            DiagnosticSeverity.ERROR, requiredParameterNode.location()));
                }
                hasType++;
            } else if (typeSymbol.typeKind() == TypeDescKind.ARRAY
                    && isParameterTypeAnydata(((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor().typeKind())) {
                hasType++;
            } else if (isParameterTypeAnydata(typeSymbol.typeKind())) {
                hasType++;
            }
        }

        if (hasType != 1) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    CompilationErrors.INVALID_FUNCTION_PARAM_ANYDATA,
                    DiagnosticSeverity.ERROR, requiredParameterNode.location()));
        }
    }

    private void validateIntersectionType(IntersectionTypeSymbol intersectionTypeSymbol,
                                          RequiredParameterNode requiredParameterNode) {
        // (readonly & nats:Message - valid, readonly & nats:Client - invalid)
        // (readonly & string - invalid)
        TypeReferenceTypeSymbol typeReferenceTypeSymbol = null;
        int hasType = 0;
        List<TypeSymbol> intersectionMembers = intersectionTypeSymbol.memberTypeDescriptors();
        for (TypeSymbol typeSymbol : intersectionMembers) {
            if (typeSymbol.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) typeSymbol;
                hasType++;
            } else if (typeSymbol.typeKind() == TypeDescKind.ARRAY
                    && isParameterTypeAnydata(((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor().typeKind())) {
                hasType++;
            } else if (isParameterTypeAnydata(typeSymbol.typeKind())) {
                hasType++;
            }
        }

        if (hasType != 1) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE_OR_ANYDATA,
                    DiagnosticSeverity.ERROR, requiredParameterNode.location()));
        } else {
            if (typeReferenceTypeSymbol != null && !isValidParamTypeMessage(typeReferenceTypeSymbol)) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(
                        CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE_OR_ANYDATA,
                        DiagnosticSeverity.ERROR, requiredParameterNode.location()));
            }
        }
    }

    private boolean isValidParamTypeMessage(TypeReferenceTypeSymbol typeReferenceTypeSymbol) {
        boolean validFlag = false;
        Optional<ModuleSymbol> moduleSymbol = typeReferenceTypeSymbol.getModule();
        if (moduleSymbol.isPresent()) {
            if (!validateModuleId(moduleSymbol.get())) {
                if (typeReferenceTypeSymbol.typeDescriptor().typeKind() == TypeDescKind.TYPE_REFERENCE) {
                    TypeReferenceTypeSymbol typeReferenceTypeSymbolNext =
                            (TypeReferenceTypeSymbol) typeReferenceTypeSymbol.typeDescriptor();
                    return isValidParamTypeMessage(typeReferenceTypeSymbolNext);
                }
            } else {
                if (typeReferenceTypeSymbol.getName().isPresent()) {
                    String paramName = typeReferenceTypeSymbol.getName().get();
                    if (paramName.equals(PluginConstants.MESSAGE)) {
                        validFlag = true;
                    }
                }
            }
        }
        return validFlag;
    }

    private void validateReturnTypeErrorOrNil(FunctionDefinitionNode functionDefinitionNode) {
        MethodSymbol methodSymbol = getMethodSymbol(context, functionDefinitionNode);
        if (methodSymbol != null) {
            Optional<TypeSymbol> returnTypeDesc = methodSymbol.typeDescriptor().returnTypeDescriptor();
            if (returnTypeDesc.isPresent()) {
                if (returnTypeDesc.get().typeKind() == TypeDescKind.UNION) {
                    List<TypeSymbol> returnTypeMembers =
                            ((UnionTypeSymbol) returnTypeDesc.get()).memberTypeDescriptors();
                    for (TypeSymbol returnType : returnTypeMembers) {
                        if (returnType.typeKind() != TypeDescKind.NIL) {
                            if (returnType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                                if (!returnType.signature().equals(PluginConstants.ERROR) &&
                                        !validateModuleId(returnType.getModule().get())) {
                                    context.reportDiagnostic(PluginUtils.getDiagnostic(
                                            CompilationErrors.INVALID_RETURN_TYPE_ERROR_OR_NIL,
                                            DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
                                }
                            } else if (returnType.typeKind() != TypeDescKind.ERROR) {
                                context.reportDiagnostic(PluginUtils.getDiagnostic(
                                        CompilationErrors.INVALID_RETURN_TYPE_ERROR_OR_NIL,
                                        DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
                            }
                        }
                    }
                } else if (returnTypeDesc.get().typeKind() != TypeDescKind.NIL) {
                    context.reportDiagnostic(PluginUtils.getDiagnostic(
                            CompilationErrors.INVALID_RETURN_TYPE_ERROR_OR_NIL,
                            DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
                }
            }
        }
    }

    private void validateOnRequestReturnType(FunctionDefinitionNode functionDefinitionNode) {
        MethodSymbol methodSymbol = getMethodSymbol(context, functionDefinitionNode);
        if (methodSymbol != null) {
            Optional<TypeSymbol> returnTypeDesc = methodSymbol.typeDescriptor().returnTypeDescriptor();
            if (returnTypeDesc.isPresent()) {
                if (returnTypeDesc.get().typeKind() == TypeDescKind.UNION) {
                    List<TypeSymbol> returnTypeMembers =
                            ((UnionTypeSymbol) returnTypeDesc.get()).memberTypeDescriptors();
                    for (TypeSymbol returnType : returnTypeMembers) {
                        if (returnType.typeKind() != TypeDescKind.NIL) {
                            if (returnType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                                TypeReferenceTypeSymbol returnTypeRef = (TypeReferenceTypeSymbol) returnType;
                                String returnTypeName = returnTypeRef.definition().getName().isPresent() ?
                                        returnTypeRef.definition().getName().get() : "";
                                if (!returnTypeName.equalsIgnoreCase(PluginConstants.ERROR) ||
                                        !validateModuleId(returnType.getModule().get())) {
                                    context.reportDiagnostic(PluginUtils.getDiagnostic(
                                            CompilationErrors.INVALID_RETURN_TYPE_ANY_DATA,
                                            DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
                                }
                            } else if (returnType.typeKind() != TypeDescKind.ERROR) {
                                validateAnyDataReturnType(returnType.signature(), functionDefinitionNode);
                            }
                        }
                    }
                } else if (returnTypeDesc.get().typeKind() != TypeDescKind.NIL) {
                    validateAnyDataReturnType(returnTypeDesc.get().signature(), functionDefinitionNode);
                }
            }
        }
    }

    private void validateAnyDataReturnType(String returnType, FunctionDefinitionNode functionDefinitionNode) {
        if (!Arrays.asList(PluginConstants.ANY_DATA_RETURN_VALUES).contains(returnType)) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    CompilationErrors.INVALID_RETURN_TYPE_ANY_DATA,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
        }
    }

    private void validateErrorParam(ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        Node parameterTypeNode = requiredParameterNode.typeName();
        if (parameterTypeNode.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
            QualifiedNameReferenceNode errorNode = (QualifiedNameReferenceNode) parameterTypeNode;
            SemanticModel semanticModel = context.semanticModel();
            Optional<Symbol> paramSymbol = semanticModel.symbol(errorNode);
            if (paramSymbol.isPresent()) {
                String paramName = paramSymbol.get().getName().isPresent() ? paramSymbol.get().getName().get() : "";
                Optional<ModuleSymbol> moduleSymbol = paramSymbol.get().getModule();
                if (moduleSymbol.isPresent()) {
                    if (!validateModuleId(moduleSymbol.get()) ||
                            !(paramName.equals(PluginConstants.ERROR_PARAM))) {
                        context.reportDiagnostic(PluginUtils.getDiagnostic(
                                CompilationErrors.INVALID_FUNCTION_PARAM_ERROR,
                                DiagnosticSeverity.ERROR, requiredParameterNode.location()));
                    }
                }
            }
        } else {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    CompilationErrors.INVALID_FUNCTION_PARAM_ERROR,
                    DiagnosticSeverity.ERROR, requiredParameterNode.location()));
        }
    }
}
