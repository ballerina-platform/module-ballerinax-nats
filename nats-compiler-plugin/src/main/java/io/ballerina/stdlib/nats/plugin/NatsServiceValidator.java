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

import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.nats.plugin.PluginConstants.CompilationErrors;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.Optional;

import static io.ballerina.stdlib.nats.plugin.PluginUtils.getMethodSymbol;
import static io.ballerina.stdlib.nats.plugin.PluginUtils.isRemoteFunction;

/**
 * Nats service compilation validator.
 */
public class NatsServiceValidator {
    private final SyntaxNodeAnalysisContext context;
    private final NodeList<Node> memberNodes;

    public NatsServiceValidator(SyntaxNodeAnalysisContext context) {
        this.context = context;
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) context.node();
        this.memberNodes = serviceDeclarationNode.members();
    }

    public void validate() {
        validateAnnotation(this.context);
        FunctionDefinitionNode onMessage = null;
        FunctionDefinitionNode onRequest = null;
        FunctionDefinitionNode onError = null;

        if (!memberNodes.isEmpty()) {
            for (Node node : memberNodes) {
                FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
                if (node.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION) {
                    MethodSymbol methodSymbol = getMethodSymbol(context, functionDefinitionNode);
                    Optional<String> functionName = methodSymbol.getName();
                    if (functionName.isPresent()) {
                        if (functionName.get().equals(PluginConstants.ON_MESSAGE_FUNC)) {
                            onMessage = functionDefinitionNode;
                        } else if (functionName.get().equals(PluginConstants.ON_REQUEST_FUNC)) {
                            onRequest = functionDefinitionNode;
                        } else if (functionName.get().equals(PluginConstants.ON_ERROR_FUNC)) {
                            onError = functionDefinitionNode;
                        } else {
                            validateNonNatsFunction(functionDefinitionNode);
                        }
                    }
                }
            }
        }
        new NatsFunctionValidator(context, onMessage, onRequest, onError).validate();
    }

    public void validateNonNatsFunction(FunctionDefinitionNode functionDefinitionNode) {
        if (isRemoteFunction(context, functionDefinitionNode)) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.INVALID_REMOTE_FUNCTION,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
        }
    }

    private void validateAnnotation(SyntaxNodeAnalysisContext context) {
    }
}
