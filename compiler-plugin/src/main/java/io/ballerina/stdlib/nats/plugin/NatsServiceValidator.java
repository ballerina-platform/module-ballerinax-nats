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
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ServiceAttachPoint;
import io.ballerina.compiler.api.symbols.ServiceAttachPointKind;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.List;
import java.util.Optional;

import static io.ballerina.stdlib.nats.plugin.PluginUtils.getMethodSymbol;

/**
 * Nats service compilation validator.
 */
public class NatsServiceValidator {

    public void validate(SyntaxNodeAnalysisContext context) {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) context.node();
        NodeList<Node> memberNodes = serviceDeclarationNode.members();

        boolean hasRemoteFunction = serviceDeclarationNode.members().stream().anyMatch(child ->
                child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION &&
                        PluginUtils.isRemoteFunction(context, (FunctionDefinitionNode) child));

        if (serviceDeclarationNode.members().isEmpty() || !hasRemoteFunction) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.TEMPLATE_CODE_GENERATION_HINT,
                   DiagnosticSeverity.INTERNAL, serviceDeclarationNode.location()));
        }

        validateAttachPoint(context);
        FunctionDefinitionNode onMessage = null;
        FunctionDefinitionNode onRequest = null;
        FunctionDefinitionNode onError = null;

        for (Node node : memberNodes) {
            if (node.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION) {
                FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
                MethodSymbol methodSymbol = getMethodSymbol(context, functionDefinitionNode);
                Optional<String> functionName = methodSymbol.getName();
                if (functionName.isPresent()) {
                    if (functionName.get().equals(PluginConstants.ON_MESSAGE_FUNC)) {
                        onMessage = functionDefinitionNode;
                    } else if (functionName.get().equals(PluginConstants.ON_REQUEST_FUNC)) {
                        onRequest = functionDefinitionNode;
                    } else if (functionName.get().equals(PluginConstants.ON_ERROR_FUNC)) {
                        onError = functionDefinitionNode;
                    } else if (PluginUtils.isRemoteFunction(context, functionDefinitionNode)) {
                        context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.INVALID_RESOURCE_FUNCTION,
                                DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
                    }
                }
            } else if (node.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.INVALID_FUNCTION,
                        DiagnosticSeverity.ERROR, node.location()));
            }
        }
        new NatsFunctionValidator(context, onMessage, onRequest, onError).validate();
    }

    private void validateAttachPoint(SyntaxNodeAnalysisContext context) {
        SemanticModel semanticModel = context.semanticModel();
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) context.node();
        Optional<Symbol> symbol = semanticModel.symbol(serviceDeclarationNode);
        if (symbol.isPresent()) {
            ServiceDeclarationSymbol serviceDeclarationSymbol = (ServiceDeclarationSymbol) symbol.get();
            Optional<ServiceAttachPoint> serviceNameAttachPoint = serviceDeclarationSymbol.attachPoint();
            List<AnnotationSymbol> annotations = serviceDeclarationSymbol.annotations();

            // nats gets the subject name from either the service name or the
            // service config, so either one of them should be present.
            if (annotations.isEmpty() || !hasServiceConfig(annotations)) {
                if (serviceNameAttachPoint.isEmpty()) {
                    // Case 1: No service name and no annotation
                    reportError(context, CompilationErrors.NO_ANNOTATION, serviceDeclarationNode);
                } else if (serviceNameAttachPoint.get().kind() != ServiceAttachPointKind.STRING_LITERAL) {
                    // Case 2: Service name is not a string and no annotation
                    reportError(context, CompilationErrors.INVALID_SERVICE_ATTACH_POINT, serviceDeclarationNode);
                }
            }
        }
    }

    private boolean hasServiceConfig(List<AnnotationSymbol> annotationSymbols) {
        boolean flag = false;
        for (AnnotationSymbol annotationSymbol : annotationSymbols) {
            Optional<ModuleSymbol> moduleSymbolOptional = annotationSymbol.getModule();
            if (moduleSymbolOptional.isPresent()) {
                ModuleSymbol moduleSymbol = moduleSymbolOptional.get();
                if (moduleSymbol.id().orgName().equals(PluginConstants.PACKAGE_ORG) ||
                        moduleSymbol.id().moduleName().equals(PluginConstants.PACKAGE_PREFIX)) {
                    // not checking name as nats has only two annotations and only one is allowed on services.
                    flag = true;
                }
            }
        }
        return flag;
    }

    private void reportError(SyntaxNodeAnalysisContext context, CompilationErrors error, Node locationNode) {
        context.reportDiagnostic(PluginUtils.getDiagnostic(error, DiagnosticSeverity.ERROR, locationNode.location()));
    }
}
