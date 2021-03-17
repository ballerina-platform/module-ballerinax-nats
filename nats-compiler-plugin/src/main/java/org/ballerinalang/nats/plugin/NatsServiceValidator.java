package org.ballerinalang.nats.plugin;

import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.Optional;

/**
 * WIP-Created for testing-------- .
 */
public class NatsServiceValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        // Checking listener type
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) syntaxNodeAnalysisContext.node();
        SeparatedNodeList<ExpressionNode> expressionNodes = serviceDeclarationNode.expressions();
        Optional<ModuleSymbol> moduleType = null;
        for (ExpressionNode expressionNode : expressionNodes) {
            if (expressionNode.kind() == SyntaxKind.EXPLICIT_NEW_EXPRESSION) {
                TypeReferenceTypeSymbol symbol = (TypeReferenceTypeSymbol) (syntaxNodeAnalysisContext.semanticModel().symbol
                        (syntaxNodeAnalysisContext.currentPackage().getDefaultModule().
                                 document(syntaxNodeAnalysisContext.documentId()),
                         expressionNode.lineRange().startLine()).get());
                moduleType = symbol.typeDescriptor().getModule();
            } else if (expressionNode.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
                VariableSymbol symbol = (VariableSymbol) syntaxNodeAnalysisContext.semanticModel().symbol
                        (syntaxNodeAnalysisContext.currentPackage().getDefaultModule().
                                 document(syntaxNodeAnalysisContext.documentId()),
                         expressionNode.lineRange().startLine()).get();
                moduleType = (symbol.typeDescriptor()).getModule();
            } else {
                moduleType = null;
                // todo
            }
        }

        String moduleNameSignature = moduleType == null ? "" : moduleType.toString();
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo("NATS_101", "service declaration in type: "
                + moduleNameSignature, DiagnosticSeverity.INFO);
        Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(diagnosticInfo, serviceDeclarationNode.location());
        syntaxNodeAnalysisContext.reportDiagnostic(diagnostic);
    }
}
