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

        enum NodeOperation { GT, LT, EQEQ, NE, AND, OR, NOT, PLUS, MINUS }

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


            leftNode = handleIfNode(condition.getLeftExpression());
            //get condition expression

            NodeOperation operation = getNodeOperation(condition);
            if (operation == NodeOperation.OR && (leftNode.getType() == NodeResult.NodeResultType.BOOL) && leftNode.getBoolValue()) {
                //always true no need in checking right child
                return new NodeResult(true);
            }
            if (operation == NodeOperation.AND && (leftNode.getType() == NodeResult.NodeResultType.BOOL) && !leftNode.getBoolValue()) {
                //always false no need in checking right child
                return new NodeResult(false);
            }
            //get right node result
            rightNode = handleIfNode(condition.getRightExpression());


            return handleFinalResult(leftNode, rightNode, condition);

        }

        private NodeResult processIntExpression(PyNumericLiteralExpression element) {
            //just get value
            return new NodeResult(element.getBigIntegerValue().intValue());
        }

        private NodeResult processParenthesizedExpression(PyParenthesizedExpression element) {

            return handleIfNode(element.getFirstChild().getNextSibling());
        }

        private NodeResult processPrefixExpression(PyPrefixExpression element) {
            //check if not expression
            if(getNodeOperation(element) == NodeOperation.NOT) {
                //get inner value
                NodeResult result = handleIfNode(element.getLastChild());
                if(result.getType() == NodeResult.NodeResultType.BOOL) {
                    return new NodeResult(!result.getBoolValue());
                }

            }

            return new NodeResult();
        }

        private NodeResult handleFinalResult(NodeResult leftNode, NodeResult rightNode, PsiElement condition) {

            if (leftNode.getType() == NodeResult.NodeResultType.SKIP && rightNode.getType() == NodeResult.NodeResultType.BOOL) {
                showAlert(rightNode.getBoolValue(), condition.getLastChild());
                //SKIP result
                return new NodeResult();
            }

            if (rightNode.getType() == NodeResult.NodeResultType.SKIP && leftNode.getType() == NodeResult.NodeResultType.BOOL) {
                showAlert(leftNode.getBoolValue(), condition.getFirstChild());
                //SKIP result
                return new NodeResult();
            }

            NodeOperation operation = getNodeOperation(condition);

            if(leftNode.getType() == NodeResult.NodeResultType.BOOL && leftNode.getType() == rightNode.getType()) {
                //handle logic operation AND, OR
                if (operation == NodeOperation.AND) {
                    return new NodeResult(leftNode.getBoolValue() && rightNode.getBoolValue());

                }
                if (operation == NodeOperation.OR) {
                    return new NodeResult(leftNode.getBoolValue() || rightNode.getBoolValue());
                }

                /* should be in separate check (have only one child)
                if (operation == NodeOperation.NOT) {
                    return nodesResult.get(0) || nodesResult.get(1);
                }*/
            }

            if(leftNode.getType() == NodeResult.NodeResultType.INT && leftNode.getType() == rightNode.getType()) {
                //handle int operations
                if (operation == NodeOperation.GT) {
                    return new NodeResult(leftNode.getIntValue() > rightNode.getIntValue());
                }
                if (operation == NodeOperation.LT) {
                    return new NodeResult(leftNode.getIntValue() < rightNode.getIntValue());
                }
                if (operation == NodeOperation.EQEQ) {
                    return new NodeResult(leftNode.getIntValue() == rightNode.getIntValue());
                }
                if (operation == NodeOperation.NE) {
                    return new NodeResult(leftNode.getIntValue() != rightNode.getIntValue());
                }

                //to do add plus, minus, etc
                if(operation == NodeOperation.PLUS) {
                    return new NodeResult(leftNode.getIntValue()+rightNode.getIntValue());
                }

            }
            //if different types it's wrong show notification ?
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

            //to do add unsupported operation exception
            return null;
        }

        private void showAlert(Boolean result, PsiElement condition) {
            if (result != null)
                registerProblem(condition, "The condition is always " + result);
        }
    }
}

