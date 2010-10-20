/*
 * Copyright 2003-2010 Dave Griffith, Bas Leijdekkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ig.resources;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.HardcodedMethodConstants;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.psiutils.TypeUtils;
import com.siyeh.ig.ui.CheckBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class ChannelResourceInspection extends ResourceInspection {

    @SuppressWarnings({"PublicField"})
    public boolean insideTryAllowed = false;

    @Override
    @NotNull
    public String getID() {
        return "ChannelOpenedButNotSafelyClosed";
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return InspectionGadgetsBundle.message(
                "channel.opened.not.closed.display.name");
    }

    @Override
    @NotNull
    public String buildErrorString(Object... infos) {
        final PsiExpression expression = (PsiExpression) infos[0];
        final PsiType type = expression.getType();
        assert type != null;
        final String text = type.getPresentableText();
        return InspectionGadgetsBundle.message(
                "channel.opened.not.closed.problem.descriptor", text);
    }

    @Override
    public JComponent createOptionsPanel() {
        final JComponent panel = new JPanel(new GridBagLayout());
        final CheckBox checkBox = new CheckBox(
                InspectionGadgetsBundle.message(
                        "allow.resource.to.be.opened.inside.a.try.block"),
                this, "insideTryAllowed");

        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets.left = 4;
        constraints.insets.right = 4;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(checkBox, constraints);
        return panel;
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new ChannelResourceVisitor();
    }

    private class ChannelResourceVisitor extends BaseInspectionVisitor {

        @Override
        public void visitMethodCallExpression(
                @NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);
            if (!isChannelFactoryMethod(expression)) {
                return;
            }
            final PsiElement parent = getExpressionParent(expression);
            if (parent instanceof PsiReturnStatement) {
                return;
            }
            final PsiVariable boundVariable = getVariable(parent);
            if (isSafelyClosed(boundVariable, expression, insideTryAllowed)) {
                return;
            }
            if (isChannelFactoryClosedInFinally(expression)) {
                return;
            }
            if (isResourceEscapedFromMethod(boundVariable, expression)) {
                return;
            }
            registerError(expression, expression);
        }

        private boolean isChannelFactoryClosedInFinally(
                PsiMethodCallExpression expression) {
            final PsiReferenceExpression methodExpression =
                    expression.getMethodExpression();
            final PsiExpression qualifier =
                    methodExpression.getQualifierExpression();
            if (!(qualifier instanceof PsiReferenceExpression)) {
                return false;
            }
            final PsiReferenceExpression referenceExpression =
                    (PsiReferenceExpression) qualifier;
            final PsiElement target = referenceExpression.resolve();
            if (!(target instanceof PsiVariable)) {
                return false;
            }
            final PsiVariable variable = (PsiVariable) target;
            PsiTryStatement tryStatement =
                    PsiTreeUtil.getParentOfType(expression,
                            PsiTryStatement.class, true, PsiMember.class);
            if (tryStatement == null) {
                return false;
            }
            while (!isResourceClosedInFinally(tryStatement, variable)) {
                tryStatement =
                        PsiTreeUtil.getParentOfType(tryStatement,
                                PsiTryStatement.class, true, PsiMember.class);
                if (tryStatement == null) {
                    return false;
                }
            }
            return true;
        }

        private boolean isChannelFactoryMethod(
                PsiMethodCallExpression expression) {
            final PsiReferenceExpression methodExpression =
                    expression.getMethodExpression();
            final String methodName = methodExpression.getReferenceName();
            if (!HardcodedMethodConstants.GET_CHANNEL.equals(methodName)) {
                return false;
            }
            final PsiExpression qualifier =
                    methodExpression.getQualifierExpression();
            if (qualifier == null) {
                return false;
            }
            return TypeUtils.expressionHasTypeOrSubtype(qualifier,
                    "java.net.Socket",
                    "java.net.DatagramSocket",
                    "java.net.ServerSocket",
                    "java.io.FileInputStream",
                    "java.io.FileOutputStream",
                    "java.io.RandomAccessFile",
                    "com.sun.corba.se.pept.transport.EventHandler",
                    "sun.nio.ch.InheritedChannel") != null;
        }
    }
}