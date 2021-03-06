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

import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;
import io.ballerina.projects.plugins.CodeGenerator;
import io.ballerina.projects.plugins.CodeGeneratorContext;
import io.ballerina.projects.plugins.CompilerPlugin;
import io.ballerina.projects.plugins.CompilerPluginContext;

public class ValidatorCompilerPlugin extends CompilerPlugin {

    @Override
    public void init(CompilerPluginContext compilerPluginContext) {
        compilerPluginContext.addCodeAnalyzer(new CodeAnalyzer() {
            @Override
            public void init(CodeAnalysisContext codeAnalysisContext) {
                codeAnalysisContext.addCompilationAnalysisTask(new ValidationTask());
            }
        });
        compilerPluginContext.addCodeGenerator(new CodeGenerator() {
            @Override
            public void init(CodeGeneratorContext codeGeneratorContext) {
                codeGeneratorContext.addSourceGeneratorTask(new MetaDataGenTask());
            }
        });
    }
}
