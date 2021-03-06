/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.dmn.validation.dtanalysis;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.kie.dmn.feel.codegen.feel11.DefaultedVisitor;
import org.kie.dmn.feel.lang.FEELProfile;
import org.kie.dmn.feel.lang.ast.ASTNode;
import org.kie.dmn.feel.lang.ast.BaseNode;
import org.kie.dmn.feel.lang.ast.BooleanNode;
import org.kie.dmn.feel.lang.ast.FunctionInvocationNode;
import org.kie.dmn.feel.lang.ast.NameRefNode;
import org.kie.dmn.feel.lang.ast.NumberNode;
import org.kie.dmn.feel.lang.ast.SignedUnaryNode;
import org.kie.dmn.feel.lang.ast.SignedUnaryNode.Sign;
import org.kie.dmn.feel.lang.ast.StringNode;
import org.kie.dmn.feel.lang.impl.FEELImpl;
import org.kie.dmn.feel.util.EvalHelper;

public class DMNDTAnalyserValueFromNodeVisitor extends DefaultedVisitor<Comparable<?>> {

    private final FEELImpl FEEL;

    public DMNDTAnalyserValueFromNodeVisitor(List<FEELProfile> feelProfiles) {
        FEEL = (FEELImpl) org.kie.dmn.feel.FEEL.newInstance(feelProfiles);
    }

    @Override
    public Comparable<?> defaultVisit(ASTNode n) {
        throw new UnsupportedOperationException("valueFromNode: " + n);
    }

    @Override
    public Boolean visit(BooleanNode n) {
        return n.getValue();
    }

    @Override
    public BigDecimal visit(NumberNode n) {
        return n.getValue();
    }

    @Override
    public String visit(StringNode n) {
        return EvalHelper.unescapeString(n.getText());
    }

    @Override
    public Comparable<?> visit(SignedUnaryNode n) {
        BaseNode signedExpr = n.getExpression();
        if (signedExpr instanceof NumberNode) {
            BigDecimal valueFromNode = (BigDecimal) signedExpr.accept(this);
            if (n.getSign() == Sign.NEGATIVE) {
                return BigDecimal.valueOf(-1).multiply(valueFromNode);
            } else {
                return valueFromNode;
            }
        } else {
            return defaultVisit(n);
        }
    }

    @Override
    public Comparable<?> visit(FunctionInvocationNode n) {
        if (n.accept(new SupportedConstantValueVisitor())) {
            return blankEvaluate(n);
        } else {
            return defaultVisit(n);
        }
    }
    
    private static class SupportedConstantValueVisitor extends DefaultedVisitor<Boolean> {

        public boolean areAllSupported(List<BaseNode> nodes) {
            return nodes.stream().allMatch(n -> n.accept(this));
        }

        @Override
        public Boolean defaultVisit(ASTNode n) {
            return false;
        }
        
        @Override
        public Boolean visit(BooleanNode n) {
            return true;
        }

        @Override
        public Boolean visit(NumberNode n) {
            return true;
        }

        @Override
        public Boolean visit(StringNode n) {
            return true;
        }

        @Override
        public Boolean visit(SignedUnaryNode n) {
            BaseNode signedExpr = n.getExpression();
            if (signedExpr instanceof NumberNode) {
                return true;
            } else {
                return defaultVisit(n);
            }
        }

        @Override
        public Boolean visit(FunctionInvocationNode n) {
            String fnName = null;
            if (n.getName() instanceof NameRefNode) {
                // simple name
                fnName = n.getName().getText();
            }
            List<BaseNode> params = n.getParams().getElements();
            switch (fnName) {
                case "date":
                    if (params.size() == 1 || params.size() == 3) {
                        return areAllSupported(params);
                    }
                    break;
                case "date and time":
                    if (params.size() == 2 || params.size() == 1) {
                        return areAllSupported(params);
                    }
                    break;
                case "time":
                    if (params.size() == 1 || params.size() == 4) {
                        return areAllSupported(params);
                    }
                    break;
                case "number":
                    if (params.size() == 3) {
                        return areAllSupported(params);
                    }
                    break;
                case "string":
                    if (params.size() == 1) {
                        return areAllSupported(params);
                    }
                    break;
                case "duration":
                    if (params.size() == 1) {
                        return areAllSupported(params);
                    }
                case "years and months duration":
                    if (params.size() == 2) {
                        return areAllSupported(params);
                    }
                default:
                    return false;
            }
            return false;
        }
    }

    private Comparable<?> blankEvaluate(FunctionInvocationNode n) {
        return (Comparable<?>) n.evaluate(FEEL.newEvaluationContext(Collections.emptyList(), Collections.emptyMap()));
    }
    
}
