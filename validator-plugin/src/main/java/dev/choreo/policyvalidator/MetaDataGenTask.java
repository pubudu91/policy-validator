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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.plugins.GeneratorTask;
import io.ballerina.projects.plugins.SourceGeneratorContext;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class MetaDataGenTask implements GeneratorTask<SourceGeneratorContext> {

    private static final String BUILTIN_POLICY_ORG = "pubudu";
    private static final String POLICY_VALIDATOR_PKG = "policy_validator";
    private static final String POLICY_IN_FLOW_ANNOT = "InFlow";
    private static final String POLICY_OUT_FLOW_ANNOT = "OutFlow";
    private static final String POLICY_FAULT_FLOW_ANNOT = "FaultFlow";

    @Override
    public void generate(SourceGeneratorContext sourceGeneratorContext) {
        System.out.println("Generating 'policy-meta.json' file...");
        JsonObject policyMeta = new JsonObject();
        SemanticModel model =
                sourceGeneratorContext.currentPackage().getDefaultModule().getCompilation()
                        .getSemanticModel();
        List<FunctionSymbol> publicFns = model.moduleSymbols().stream()
                .filter(sym -> sym.kind() == SymbolKind.FUNCTION
                        && ((FunctionSymbol) sym).qualifiers().contains(Qualifier.PUBLIC))
                .map(fn -> (FunctionSymbol) fn)
                .collect(Collectors.toList());
        PackageDescriptor desc = sourceGeneratorContext.currentPackage().descriptor();

        policyMeta.addProperty("org", desc.org().value());
        policyMeta.addProperty("name", desc.name().value());
        policyMeta.addProperty("version", desc.version().toString());


        for (FunctionSymbol fn : publicFns) {
            generateFunctionMeta(fn, policyMeta);
        }

        Gson gson = new Gson();
        sourceGeneratorContext.addResourceFile(gson.toJson(policyMeta).getBytes(StandardCharsets.UTF_8),
                                               "policy-meta.json");
    }

    private void generateFunctionMeta(FunctionSymbol funcSymbol, JsonObject policyMeta) {
        JsonObject func = new JsonObject();
        func.addProperty("name", funcSymbol.getName().get());
        func.add("params", generateParamList(funcSymbol.typeDescriptor().params().get()));

        if (isPolicyFunction(funcSymbol.annotations(), POLICY_IN_FLOW_ANNOT)) {
            policyMeta.add("inflow", func);
        } else if (isPolicyFunction(funcSymbol.annotations(), POLICY_OUT_FLOW_ANNOT)) {
            policyMeta.add("outflow", func);
        } else if (isPolicyFunction(funcSymbol.annotations(), POLICY_FAULT_FLOW_ANNOT)) {
            policyMeta.add("faultflow", func);
        }

        // ignore regular functions
    }

    private JsonArray generateParamList(List<ParameterSymbol> params) {
        JsonArray arr = new JsonArray();
        for (ParameterSymbol param : params) {
            arr.add(generateParamMeta(param));
        }
        return arr;
    }

    private JsonObject generateParamMeta(ParameterSymbol paramSymbol) {
        JsonObject param = new JsonObject();
        param.addProperty("name", paramSymbol.getName().get());
        param.add("type", generateTypeMeta(paramSymbol.typeDescriptor()));
        param.add("isConfigurable", new JsonPrimitive(isConfigurable(paramSymbol.annotations())));
        return param;
    }

    private JsonObject generateTypeMeta(TypeSymbol typeSymbol) {
        JsonObject type = new JsonObject();
        type.addProperty("name", typeSymbol.getName().orElse(typeSymbol.signature()));
        type.addProperty("kind", typeSymbol.typeKind().toString());

        if (typeSymbol.getModule().isEmpty()) {
            return type;
        }

        ModuleID id = typeSymbol.getModule().get().id();
        JsonObject pkg = new JsonObject();
        pkg.addProperty("org", id.orgName());
        pkg.addProperty("name", id.packageName());
        pkg.addProperty("version", id.version());

        type.add("package", pkg);
        return type;
    }

    private boolean isPolicyFunction(List<AnnotationSymbol> annots, String policyKind) {
        for (AnnotationSymbol annot : annots) {
            ModuleID id = annot.getModule().get().id();

            if (BUILTIN_POLICY_ORG.equals(id.orgName())
                    && POLICY_VALIDATOR_PKG.equals(id.packageName())
                    && policyKind.equals(annot.getName().get())) {
                return true;
            }
        }

        return false;
    }

    private boolean isConfigurable(List<AnnotationSymbol> annots) {
        for (AnnotationSymbol annot : annots) {
            ModuleID id = annot.getModule().get().id();

            if (BUILTIN_POLICY_ORG.equals(id.orgName())
                    && POLICY_VALIDATOR_PKG.equals(id.packageName())
                    && "Config".equals(annot.getName().get())) {
                return true;
            }
        }

        return false;
    }
}
