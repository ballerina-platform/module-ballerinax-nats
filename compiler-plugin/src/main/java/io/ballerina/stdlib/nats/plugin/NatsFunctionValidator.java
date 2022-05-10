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
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IntersectionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.ParenthesisedTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.compiler.api.symbols.TypeDescKind.ANYDATA;
import static io.ballerina.compiler.api.symbols.TypeDescKind.ARRAY;
import static io.ballerina.compiler.api.symbols.TypeDescKind.BOOLEAN;
import static io.ballerina.compiler.api.symbols.TypeDescKind.DECIMAL;
import static io.ballerina.compiler.api.symbols.TypeDescKind.FLOAT;
import static io.ballerina.compiler.api.symbols.TypeDescKind.INT;
import static io.ballerina.compiler.api.symbols.TypeDescKind.JSON;
import static io.ballerina.compiler.api.symbols.TypeDescKind.MAP;
import static io.ballerina.compiler.api.symbols.TypeDescKind.NIL;
import static io.ballerina.compiler.api.symbols.TypeDescKind.OBJECT;
import static io.ballerina.compiler.api.symbols.TypeDescKind.RECORD;
import static io.ballerina.compiler.api.symbols.TypeDescKind.STRING;
import static io.ballerina.compiler.api.symbols.TypeDescKind.TABLE;
import static io.ballerina.compiler.api.symbols.TypeDescKind.TYPE_REFERENCE;
import static io.ballerina.compiler.api.symbols.TypeDescKind.UNION;
import static io.ballerina.compiler.api.symbols.TypeDescKind.XML;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ANYDATA_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ARRAY_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BOOLEAN_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BYTE_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DECIMAL_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FLOAT_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.INTERSECTION_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.INT_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.JSON_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MAP_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NIL_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PARENTHESISED_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUALIFIED_NAME_REFERENCE;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.READONLY_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SIMPLE_NAME_REFERENCE;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TABLE_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.UNION_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.XML_TYPE_DESC;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors.FUNCTION_SHOULD_BE_REMOTE;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_ANYDATA;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_ERROR;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE_OR_ANYDATA;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors.INVALID_RETURN_TYPE_ANY_DATA;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors.INVALID_RETURN_TYPE_ERROR_OR_NIL;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors.MUST_HAVE_MESSAGE_AND_ERROR;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors.MUST_HAVE_MESSAGE_OR_ANYDATA;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors.NO_ON_MESSAGE_OR_ON_REQUEST;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors.ONLY_PARAMS_ALLOWED;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors.ONLY_PARAMS_ALLOWED_ON_ERROR;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors.ON_MESSAGE_OR_ON_REQUEST;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.MESSAGE_CONTENT;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.MESSAGE_REPLY_TO;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.MESSAGE_SUBJECT;
import static io.ballerina.stdlib.nats.plugin.PluginConstants.PAYLOAD_ANNOTATION;
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
            reportErrorDiagnostic(NO_ON_MESSAGE_OR_ON_REQUEST, serviceDeclarationNode.location());
        } else if (!Objects.isNull(onMessage) && !Objects.isNull(onRequest)) {
            reportErrorDiagnostic(ON_MESSAGE_OR_ON_REQUEST, serviceDeclarationNode.location());
        }
    }

    private void validateOnMessage() {
        if (!isRemoteFunction(context, onMessage)) {
            reportErrorDiagnostic(FUNCTION_SHOULD_BE_REMOTE, onMessage.functionSignature().location());
        }
        SeparatedNodeList<ParameterNode> parameters = onMessage.functionSignature().parameters();
        validateFunctionParameters(parameters, onMessage);
        validateReturnTypeErrorOrNil(onMessage);
    }

    private void validateOnRequest() {
        if (!isRemoteFunction(context, onRequest)) {
            reportErrorDiagnostic(FUNCTION_SHOULD_BE_REMOTE, onRequest.functionSignature().location());
        }
        SeparatedNodeList<ParameterNode> parameters = onRequest.functionSignature().parameters();
        validateFunctionParameters(parameters, onRequest);
        validateOnRequestReturnType(onRequest);
    }

    private void validateOnError() {
        if (!isRemoteFunction(context, onError)) {
            reportErrorDiagnostic(FUNCTION_SHOULD_BE_REMOTE, onError.functionSignature().location());
        }
        SeparatedNodeList<ParameterNode> parameters = onError.functionSignature().parameters();
        validateOnErrorFunctionParameters(parameters, onError);
        validateReturnTypeErrorOrNil(onError);
    }

    private void validateFunctionParameters(SeparatedNodeList<ParameterNode> parameters,
                                            FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.size() == 0) {
            reportErrorDiagnostic(MUST_HAVE_MESSAGE_OR_ANYDATA, functionDefinitionNode.functionSignature().location());
        } else if (parameters.size() == 1) {
            validateSingleParam(parameters.get(0));
        } else if (parameters.size() == 2) {
            validateFirstParamInTwoParamScenario(parameters.get(0));
            validateSecondParamInTwoParamScenario(parameters.get(1));
        } else if (parameters.size() > 2) {
            reportErrorDiagnostic(ONLY_PARAMS_ALLOWED, functionDefinitionNode.functionSignature().location());
        }
    }

    private void validateOnErrorFunctionParameters(SeparatedNodeList<ParameterNode> parameters,
                                                   FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.size() > 1) {
            validateFirstParamInTwoParamScenario(parameters.get(0));
            validateErrorParam(parameters.get(1));
        }
        if (parameters.size() < 2) {
            reportErrorDiagnostic(MUST_HAVE_MESSAGE_AND_ERROR, functionDefinitionNode.functionSignature().location());
        }
        if (parameters.size() > 2) {
            reportErrorDiagnostic(ONLY_PARAMS_ALLOWED_ON_ERROR, functionDefinitionNode.functionSignature().location());
        }
    }

    private void validateSingleParam(ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        if (!isMessageParam(requiredParameterNode)) {
            if (!validatePayload(requiredParameterNode.typeName())) {
                reportErrorDiagnostic(INVALID_FUNCTION_PARAM_MESSAGE, parameterNode.location());
            }
        }
    }

    private void validateFirstParamInTwoParamScenario(ParameterNode parameterNode) {
        if (!isMessageParam((RequiredParameterNode) parameterNode)) {
            reportErrorDiagnostic(INVALID_FUNCTION_PARAM_MESSAGE_OR_ANYDATA, parameterNode.location());
        }
    }

    private void validateSecondParamInTwoParamScenario(ParameterNode parameterNode) {
        if (isMessageParam((RequiredParameterNode) parameterNode) ||
                !validatePayload(((RequiredParameterNode) parameterNode).typeName())) {
            reportErrorDiagnostic(INVALID_FUNCTION_PARAM_ANYDATA, parameterNode.location());
        }
    }

    private boolean validatePayload(Node node) {
        SyntaxKind syntaxKind = node.kind();
        if (syntaxKind == INTERSECTION_TYPE_DESC) {
            IntersectionTypeDescriptorNode intersectionNode = (IntersectionTypeDescriptorNode) node;
            if (intersectionNode.leftTypeDesc().kind() != READONLY_TYPE_DESC) {
                return validatePayload(intersectionNode.leftTypeDesc());
            } else if (intersectionNode.rightTypeDesc().kind() != READONLY_TYPE_DESC) {
                return validatePayload(intersectionNode.rightTypeDesc());
            } else {
                return false;
            }
        } else if (syntaxKind == PARENTHESISED_TYPE_DESC) {
            ParenthesisedTypeDescriptorNode parenthesisedNode = (ParenthesisedTypeDescriptorNode) node;
            return validatePayload(parenthesisedNode.typedesc());
        } else if (syntaxKind == UNION_TYPE_DESC) {
            UnionTypeDescriptorNode unionNode = (UnionTypeDescriptorNode) node;
            return validatePayload(unionNode.leftTypeDesc()) &&
                    validatePayload(unionNode.rightTypeDesc());
        } else if (syntaxKind == QUALIFIED_NAME_REFERENCE) {
            TypeReferenceTypeSymbol typeSymbol = (TypeReferenceTypeSymbol) context.semanticModel().symbol(node).get();
            return typeSymbol.typeDescriptor().typeKind() != OBJECT;
        } else if (syntaxKind == ARRAY_TYPE_DESC) {
            return validatePayload(((ArrayTypeDescriptorNode) node).memberTypeDesc());
        }
        return syntaxKind == INT_TYPE_DESC || syntaxKind == STRING_TYPE_DESC || syntaxKind == BOOLEAN_TYPE_DESC ||
                syntaxKind == FLOAT_TYPE_DESC || syntaxKind == DECIMAL_TYPE_DESC || syntaxKind == RECORD_TYPE_DESC ||
                syntaxKind == MAP_TYPE_DESC || syntaxKind == BYTE_TYPE_DESC || syntaxKind == TABLE_TYPE_DESC ||
                syntaxKind == JSON_TYPE_DESC || syntaxKind == XML_TYPE_DESC || syntaxKind == ANYDATA_TYPE_DESC ||
                syntaxKind == NIL_TYPE_DESC || syntaxKind == SIMPLE_NAME_REFERENCE;
    }

    private boolean isMessageParam(RequiredParameterNode requiredParameterNode) {
        boolean hasPayloadAnnotation = requiredParameterNode.annotations().stream()
                .anyMatch(annotationNode -> annotationNode.annotReference().toString().equals(PAYLOAD_ANNOTATION));
        if (hasPayloadAnnotation) {
            return false;
        }
        Node node;
        if (requiredParameterNode.typeName().kind() == INTERSECTION_TYPE_DESC) {
            IntersectionTypeDescriptorNode intersectionNode = (IntersectionTypeDescriptorNode) requiredParameterNode
                    .typeName();
            if (intersectionNode.leftTypeDesc().kind() == SIMPLE_NAME_REFERENCE ||
                    intersectionNode.leftTypeDesc().kind() == QUALIFIED_NAME_REFERENCE) {
                node = intersectionNode.leftTypeDesc();
            } else if (intersectionNode.rightTypeDesc().kind() == SIMPLE_NAME_REFERENCE ||
                    intersectionNode.rightTypeDesc().kind() == QUALIFIED_NAME_REFERENCE) {
                node = intersectionNode.rightTypeDesc();
            } else {
                return false;
            }
        } else if (requiredParameterNode.typeName().kind() != SIMPLE_NAME_REFERENCE &&
                requiredParameterNode.typeName().kind() != QUALIFIED_NAME_REFERENCE) {
            return false;
        } else {
            node = requiredParameterNode.typeName();
        }
        return isMessageType((TypeSymbol) context.semanticModel().symbol(node).get());
    }

    private boolean isMessageType(TypeSymbol typeSymbol) {
        RecordTypeSymbol recordTypeSymbol;
        if (typeSymbol.typeKind() == TYPE_REFERENCE) {
            if (((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor().typeKind() == RECORD) {
                recordTypeSymbol = (RecordTypeSymbol) ((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor();
            } else {
                return false;
            }
        } else {
            recordTypeSymbol = (RecordTypeSymbol) typeSymbol;
        }
        Map<String, RecordFieldSymbol> fieldDescriptors = recordTypeSymbol.fieldDescriptors();
        return validateMessageFields(fieldDescriptors);
    }

    private boolean validateMessageFields(Map<String, RecordFieldSymbol> fieldDescriptors) {
        if (fieldDescriptors.size() != 3 || !fieldDescriptors.containsKey(MESSAGE_CONTENT) ||
                !fieldDescriptors.containsKey(MESSAGE_REPLY_TO) ||
                !fieldDescriptors.containsKey(MESSAGE_SUBJECT)) {
            return false;
        }
        if (fieldDescriptors.get(MESSAGE_REPLY_TO).typeDescriptor().typeKind() != STRING) {
            return false;
        }
        if (fieldDescriptors.get(MESSAGE_SUBJECT).typeDescriptor().typeKind() != STRING) {
            return false;
        }
        if (!validateAnydataFields(fieldDescriptors.get(MESSAGE_CONTENT).typeDescriptor())) {
            return false;
        }
        return true;
    }

    private boolean validateAnydataFields(TypeSymbol typeSymbol) {
        TypeDescKind symbolTypeKind = typeSymbol.typeKind();
        return symbolTypeKind == ANYDATA || symbolTypeKind == ARRAY || symbolTypeKind == BOOLEAN ||
                symbolTypeKind == JSON || symbolTypeKind == INT || symbolTypeKind == STRING ||
                symbolTypeKind == FLOAT || symbolTypeKind == DECIMAL || symbolTypeKind == RECORD ||
                symbolTypeKind == TABLE || symbolTypeKind == XML || symbolTypeKind == UNION ||
                symbolTypeKind == MAP || symbolTypeKind == NIL || symbolTypeKind == TYPE_REFERENCE;
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
                                    reportErrorDiagnostic(INVALID_RETURN_TYPE_ERROR_OR_NIL,
                                            functionDefinitionNode.location());
                                }
                            } else if (returnType.typeKind() != TypeDescKind.ERROR) {
                                reportErrorDiagnostic(INVALID_RETURN_TYPE_ERROR_OR_NIL,
                                        functionDefinitionNode.location());
                            }
                        }
                    }
                } else if (returnTypeDesc.get().typeKind() != TypeDescKind.NIL) {
                    reportErrorDiagnostic(INVALID_RETURN_TYPE_ERROR_OR_NIL, functionDefinitionNode.location());
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
                                    reportErrorDiagnostic(INVALID_RETURN_TYPE_ANY_DATA,
                                            functionDefinitionNode.location());
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
            reportErrorDiagnostic(INVALID_RETURN_TYPE_ANY_DATA, functionDefinitionNode.location());
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
                        reportErrorDiagnostic(INVALID_FUNCTION_PARAM_ERROR, requiredParameterNode.location());
                    }
                }
            }
        } else {
            reportErrorDiagnostic(INVALID_FUNCTION_PARAM_ERROR, requiredParameterNode.location());
        }
    }

    private void reportErrorDiagnostic(CompilationErrors error, Location location) {
        context.reportDiagnostic(PluginUtils.getDiagnostic(error, DiagnosticSeverity.ERROR, location));
    }
}
