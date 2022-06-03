/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package dev.choreo.policyvalidator;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.projects.Module;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.CompilationAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.List;

public class ValidationTask implements AnalysisTask<CompilationAnalysisContext> {

    private static final String IN_FLOW = "InFlow";
    private static final String OUT_FLOW = "OutFlow";
    private static final String FAULT_FLOW = "FaultFlow";
    private static final String ORG_NAME = "choreo";
    private static final String PACKAGE_NAME = "policy_validator";

    private boolean policyFound = false;

    @Override
    public void perform(CompilationAnalysisContext ctx) {
        for (Module module : ctx.currentPackage().modules()) {
            SemanticModel semanticModel = module.getCompilation().getSemanticModel();

            for (Symbol symbol : semanticModel.moduleSymbols()) {
                if (symbol.kind() == SymbolKind.FUNCTION) {
                    FunctionSymbol func = (FunctionSymbol) symbol;
                    boolean isPolicy = isAPolicy(func.annotations());

                    if (!isPolicy) {
                        continue;
                    }

                    // if (policyFound) {
                    //     ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(
                    //             new DiagnosticInfo("GWPOLICY002", "there can only be one policy per package",
                    //                                DiagnosticSeverity.ERROR), func.getLocation().get()));
                    // }

                    if (!func.qualifiers().contains(Qualifier.PUBLIC)) {
                        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(
                                new DiagnosticInfo("GWPOLICY001", "a policy should be 'public'",
                                                   DiagnosticSeverity.ERROR), func.getLocation().get()));
                    }

                    validateReturnType(func.typeDescriptor().returnTypeDescriptor().get());
                    validateParameters(func.typeDescriptor().params().orElse(List.of()),
                                       func.typeDescriptor().restParam().orElse(null));
                    policyFound = true;
                }
            }
        }
    }

    private boolean isAPolicy(List<AnnotationSymbol> annots) {
        for (AnnotationSymbol annot : annots) {
            String annotName = annot.getName().get();
            if (!IN_FLOW.equals(annotName) && !OUT_FLOW.equals(annotName) && !FAULT_FLOW.equals(annotName)) {
                continue;
            }

            if (annot.getModule().isEmpty()) {
                continue;
            }

            ModuleID moduleID = annot.getModule().get().id();
            if (ORG_NAME.equals(moduleID.orgName()) && PACKAGE_NAME.equals(moduleID.packageName())) {
                return true;
            }
        }
        return false;
    }

    private void validateReturnType(TypeSymbol type) {

    }

    private void validateParameters(List<ParameterSymbol> params, ParameterSymbol restParam) {

    }
}
