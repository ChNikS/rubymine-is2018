package com.jetbrains.python.inspection;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiWhiteSpace;
import com.jetbrains.python.inspections.PyInspection;
import com.jetbrains.python.inspections.PyInspectionVisitor;
import com.jetbrains.python.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PyConstantExpression extends PyInspection {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly,
                                          @NotNull LocalInspectionToolSession session) {
        return new Visitor(holder, session);
    }

    private static class Visitor extends PyInspectionVisitor {

        private Visitor(@Nullable ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
            super(holder, session);
        }

        @Override
        public void visitPyIfStatement(PyIfStatement node) {
            super.visitPyIfStatement(node);
            processIfPart(node.getIfPart());
            for (PyIfPart part : node.getElifParts()) {
                processIfPart(part);
            }
        }

        private void processIfPart(@NotNull PyIfPart pyIfPart) {
            final PyExpression condition = pyIfPart.getCondition();
            if (condition instanceof PyBoolLiteralExpression) {
                registerProblem(condition, "The condition is always " + ((PyBoolLiteralExpression) condition).getValue());
            }

            //baseline handler
            if(condition instanceof PyBinaryExpression) {
                PsiElement firstPsiElement = condition.getFirstChild();
                Integer firstElement = handleConditionIntElement(firstPsiElement);
                Integer lastElement = handleConditionIntElement(condition.getLastChild());
                handleConditionResult(firstElement, lastElement, condition);
            }
        }


        private Integer handleConditionIntElement(PsiElement element) {
            if(element instanceof PyNumericLiteralExpression) {
                //can use big int instead
                return ((PyNumericLiteralExpression) element).getBigIntegerValue().intValue();
            }
           return null;
        }

        private void handleConditionResult(Integer firstElement, Integer lastElement, PsiElement condition) {
            if(firstElement != null && lastElement != null) {
                String operation = ((PyBinaryExpression)condition).getOperator().toString();

                if(operation.equals("Py:GT")) {
                    registerProblem(condition, "The condition is always " + (lastElement > firstElement));
                }
                if(operation.equals("Py:LT")) {
                    registerProblem(condition, "The condition is always " + (lastElement < firstElement));
                }
                if(operation.equals("Py:EQEQ")) {
                    registerProblem(condition, "The condition is always " + (lastElement == firstElement));
                }
                if(operation.equals("Py:NE")) {
                    registerProblem(condition, "The condition is always " + (lastElement != firstElement));
                }

            }

        }
    }
}
