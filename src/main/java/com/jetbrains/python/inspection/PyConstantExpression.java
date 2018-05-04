package com.jetbrains.python.inspection;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiWhiteSpace;
import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.inspections.PyInspection;
import com.jetbrains.python.inspections.PyInspectionVisitor;
import com.jetbrains.python.psi.*;
import gherkin.lexer.No;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.soap.Node;
import java.util.ArrayList;
import java.util.List;

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

        enum NodeOperation { GT, LT, EQEQ, NE, AND, OR, NOT, PLUS, MINUS, MULT }

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

           /* if (condition instanceof PyBoolLiteralExpression) {
                //registerProblem(condition, "The condition is always " + ((PyBoolLiteralExpression) condition).getValue());
                //process BoolLiteral
                //showAlert(((PyBoolLiteralExpression) condition).getValue(), condition);
            }*/

            //baseline handler
            NodeResult result;
            result = handleIfNode(condition);
            if (result.getType() == NodeResult.NodeResultType.BOOL) {
                showAlert(result.getBoolValue(), condition);
            }

            /*if (condition instanceof PyBinaryExpression) {
                result = processBinaryExpression((PyBinaryExpression)condition);
                if (result.getType() == NodeResult.NodeResultType.BOOL) {
                    showAlert(result.getBoolValue(), condition);
                }
            }*/

            //showAlert(finalResult, condition);
        }


        private NodeResult handleIfNode(PsiElement element) {

            if (element instanceof PyBinaryExpression) {
                return processBinaryExpression((PyBinaryExpression) element);
            }
            if (element instanceof PyNumericLiteralExpression) {
                return processIntExpression((PyNumericLiteralExpression) element);
            }
            if (element instanceof PyBoolLiteralExpression) {
                return new NodeResult(((PyBoolLiteralExpression) element).getValue());
            }
            if (element instanceof PyParenthesizedExpression) {
                return processParenthesizedExpression((PyParenthesizedExpression) element);
            }
            //not support
            if (element instanceof PyPrefixExpression) {
                return processPrefixExpression((PyPrefixExpression) element);
            }

            //empty unhandled result
            return new NodeResult();
        }

        private NodeResult processBinaryExpression(PyBinaryExpression condition) {

            //only two children that can be interesting => no need in array to save memory
            NodeResult leftNode;
            NodeResult rightNode;

            //get condition expression
            leftNode = handleIfNode(condition.getLeftExpression());

            NodeOperation operation = getNodeOperation(condition);

            //always true no need in checking right child
            if (operation == NodeOperation.OR && (leftNode.getType() == NodeResult.NodeResultType.BOOL) && leftNode.getBoolValue()) {
                return new NodeResult(true);
            }

            //always false no need in checking right child
            if (operation == NodeOperation.AND && (leftNode.getType() == NodeResult.NodeResultType.BOOL) && !leftNode.getBoolValue()) {
                return new NodeResult(false);
            }
            //get right node result
            rightNode = handleIfNode(condition.getRightExpression());


            return handleFinalResult(leftNode, rightNode, condition);

        }

        private NodeResult processIntExpression(PyNumericLiteralExpression element) {
            NodeResult result = element.isIntegerLiteral() ? new NodeResult(element.getBigIntegerValue().intValue()) : new NodeResult();
            return result;
        }

        private NodeResult processParenthesizedExpression(PyParenthesizedExpression element) {

            return handleIfNode(element.getFirstChild().getNextSibling());
        }

        private NodeResult processPrefixExpression(PyPrefixExpression element) {
            //check if not expression
            if(getNodeOperation(element) == NodeOperation.NOT) {
               NodeResult result = handleIfNode(element.getLastChild());
               return handleFinalResult(new NodeResult(), result, element);
            }

            return new NodeResult();
        }

        private NodeResult handleFinalResult(NodeResult leftNode, NodeResult rightNode, PsiElement element) {

            //nothing to do
            if(rightNode.getType() == NodeResult.NodeResultType.SKIP && leftNode.getType() == NodeResult.NodeResultType.SKIP) {
                //SKIP result
                return new NodeResult();
            }
            NodeOperation operation = getNodeOperation(element);

            if (leftNode.getType() == NodeResult.NodeResultType.SKIP && rightNode.getType() == NodeResult.NodeResultType.BOOL && operation != NodeOperation.NOT) {
                showAlert(rightNode.getBoolValue(), element.getLastChild());
                //SKIP result
                return new NodeResult();
            }

            if (rightNode.getType() == NodeResult.NodeResultType.SKIP && leftNode.getType() == NodeResult.NodeResultType.BOOL) {
                showAlert(leftNode.getBoolValue(), element.getFirstChild());
                //SKIP result
                return new NodeResult();
            }

            if(operation == NodeOperation.AND) {
                if(leftNode.getType() == NodeResult.NodeResultType.BOOL && leftNode.getType() == rightNode.getType()) {
                    return new NodeResult(leftNode.getBoolValue() && rightNode.getBoolValue());
                }

                if(leftNode.getType() == NodeResult.NodeResultType.INT && leftNode.getType() == rightNode.getType()) {
                    return new NodeResult(leftNode.getIntValue()>0 && rightNode.getIntValue()>0);

                }

                if((leftNode.getType() == NodeResult.NodeResultType.BOOL || leftNode.getType() == NodeResult.NodeResultType.INT) && (rightNode.getType() == NodeResult.NodeResultType.BOOL || rightNode.getType() == NodeResult.NodeResultType.INT) && leftNode.getType() != rightNode.getType()) {
                    boolean result = leftNode.getType() == NodeResult.NodeResultType.BOOL ? leftNode.getBoolValue() && rightNode.getIntValue()>0 : rightNode.getBoolValue() && leftNode.getIntValue()>0;
                    return new NodeResult(result);
                }
            }

            if(operation == NodeOperation.OR) {
                if(leftNode.getType() == NodeResult.NodeResultType.BOOL && leftNode.getType() == rightNode.getType()) {
                    return new NodeResult(leftNode.getBoolValue() || rightNode.getBoolValue());
                }

                if(leftNode.getType() == NodeResult.NodeResultType.INT && leftNode.getType() == rightNode.getType()) {
                    return new NodeResult(leftNode.getIntValue()>0 || rightNode.getIntValue()>0);
                }

                if((leftNode.getType() == NodeResult.NodeResultType.BOOL || leftNode.getType() == NodeResult.NodeResultType.INT) && (rightNode.getType() == NodeResult.NodeResultType.BOOL || rightNode.getType() == NodeResult.NodeResultType.INT) && leftNode.getType() != rightNode.getType()) {
                    boolean result = leftNode.getType() == NodeResult.NodeResultType.BOOL ? leftNode.getBoolValue() || rightNode.getIntValue()>0 : rightNode.getBoolValue() || leftNode.getIntValue()>0;
                    return new NodeResult(result);
                }
            }

            if(operation == NodeOperation.NOT) {
                if(rightNode.getType() == NodeResult.NodeResultType.BOOL) {
                    return new NodeResult(!rightNode.getBoolValue());
                }

                if(rightNode.getType() == NodeResult.NodeResultType.INT) {
                    return new NodeResult((rightNode.getIntValue()==0));
                }
            }

            if(leftNode.getType() != NodeResult.NodeResultType.SKIP && rightNode.getType() != NodeResult.NodeResultType.SKIP) {
                //conversion
                int leftNodeValue = leftNode.getType() == NodeResult.NodeResultType.INT ? leftNode.getIntValue() : (leftNode.getBoolValue() ? 1 : 0);
                int rightNodeValue = rightNode.getType() == NodeResult.NodeResultType.INT ? rightNode.getIntValue() : (rightNode.getBoolValue() ? 1 : 0);

                //handle int operations
                if (operation == NodeOperation.GT) {
                    return new NodeResult(leftNodeValue > rightNodeValue);
                }
                if (operation == NodeOperation.LT) {
                    return new NodeResult(leftNodeValue < rightNodeValue);
                }
                if (operation == NodeOperation.EQEQ) {
                    return new NodeResult(leftNodeValue == rightNodeValue);
                }
                if (operation == NodeOperation.NE) {
                    return new NodeResult(leftNodeValue != rightNodeValue);
                }

                //to do add plus, minus, etc
                if(operation == NodeOperation.PLUS) {
                    return new NodeResult(leftNodeValue + rightNodeValue);
                }

                if(operation == NodeOperation.MINUS) {
                    return new NodeResult(leftNodeValue - rightNodeValue);
                }

                if(operation == NodeOperation.MULT) {
                    return new NodeResult(leftNodeValue - rightNodeValue);
                }

            }
            //show extra message something wrong with logic?
            return new NodeResult();
        }


        private static NodeOperation getNodeOperation(PsiElement condition) {
            String operation = "";
            if(condition instanceof PyBinaryExpression) {
                operation = ((PyBinaryExpression) condition).getOperator().toString();
            }
            if(condition instanceof  PyPrefixExpression) {
                operation = ((PyPrefixExpression) condition).getOperator().toString();
            }

            if (operation.equals("Py:GT"))
                return NodeOperation.GT;
            if (operation.equals("Py:LT"))
                return NodeOperation.LT;
            if (operation.equals("Py:EQEQ"))
                return NodeOperation.EQEQ;
            if (operation.equals("Py:NE"))
                return NodeOperation.NE;
            if (operation.equals("Py:AND_KEYWORD"))
                return NodeOperation.AND;
            if (operation.equals("Py:OR_KEYWORD"))
                return NodeOperation.OR;
            if (operation.equals("Py:NOT_KEYWORD"))
                return NodeOperation.NOT;
            //math operations
            if (operation.equals("Py:PLUS"))
                return NodeOperation.PLUS;
            if (operation.equals("Py:MINUS"))
                return NodeOperation.MINUS;
            if (operation.equals("Py:MULT"))
                return NodeOperation.MULT;

            //to do add unsupported operation exception
            return null;
        }

        private void showAlert(Boolean result, PsiElement condition) {
            if (result != null)
                registerProblem(condition, "The condition is always " + result);
        }
    }
}

