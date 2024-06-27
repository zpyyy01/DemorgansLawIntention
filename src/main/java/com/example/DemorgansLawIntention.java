package com.example;

import com.github.weisj.jsvg.S;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.tree.IElementType;
import com.siyeh.ig.PsiReplacementUtil;
import com.siyeh.ig.psiutils.CommentTracker;
import com.siyeh.ig.psiutils.ComparisonUtils;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.siyeh.ig.psiutils.BoolUtils;

import static com.siyeh.ig.PsiReplacementUtil.replaceExpression;
import static com.siyeh.ig.psiutils.BoolUtils.*;


public class DemorgansLawIntention extends PsiElementBaseIntentionAction implements IntentionAction {

    public boolean isAvailable(@NotNull Project project, Editor editor, @Nullable PsiElement element) {
        if (element == null) {
            return false;
        }
        if (element instanceof PsiJavaToken token) {
            if (token.getTokenType() == JavaTokenType.ANDAND || token.getTokenType() == JavaTokenType.OROR) {
                if (token.getParent() instanceof PsiPolyadicExpression psiPolyadicExpression) {
                    /*System.out.println("isAvailable: " + token.getParent().getText());
                    if(isNegated(psiPolyadicExpression)){
                        System.out.println("Negated: " + psiPolyadicExpression.getText());
                    }
                    for(PsiElement child : token.getParent().getChildren()){
                        System.out.println("child: " + child.getText());
                        if(child instanceof PsiExpression){
                            System.out.println("PsiExpression: " + child.getText());
                        }

                    }

                    final IElementType tokenType = psiPolyadicExpression.getOperationTokenType();
                    final boolean tokenTypeAndAnd = tokenType.equals(JavaTokenType.ANDAND);
                    System.out.println("tokenTypeAndAnd: " + tokenTypeAndAnd);
                    System.out.println("tokenType: " + tokenType);*/
                    return true;
                }
            }
        }
        return false;
    }

    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        //System.out.println("invoke: " + element.getText());

        final PsiPolyadicExpression polyadicExpression = (PsiPolyadicExpression)element.getParent();
        final CommentTracker tracker = new CommentTracker();
        final String newExpression = convertConjunctionExpression(polyadicExpression, tracker);
        PsiReplacementUtil.replaceExpressionWithNegatedExpression(polyadicExpression, newExpression, tracker);
    }

    private static String convertConjunctionExpression(PsiPolyadicExpression polyadicExpression, CommentTracker tracker) {
        final IElementType tokenType = polyadicExpression.getOperationTokenType();
        final boolean tokenTypeAndAnd = tokenType.equals(JavaTokenType.ANDAND);
        final String flippedToken = tokenTypeAndAnd ? "||" : "&&";
        final StringBuilder result = new StringBuilder();
        for (PsiElement child : polyadicExpression.getChildren()) {
            if (child instanceof PsiJavaToken) {
                result.append(flippedToken);
            }
            else if (child instanceof PsiExpression) {
                result.append(convertLeafExpression((PsiExpression)child, tokenTypeAndAnd, tracker));
            }
            else {
                result.append(tracker.text(child));
            }
        }
        return result.toString();
    }


    private static String convertLeafExpression(PsiExpression expression, boolean tokenTypeAndAnd, CommentTracker tracker) {
        if (isNegation(expression)) {
            final PsiExpression negatedExpression = getNegated(expression);
            if (negatedExpression == null) {
                return "";
            }
            if (negatedExpression instanceof PsiLiteralExpression) {
                return safeText(negatedExpression);
            }
            return tracker.text(negatedExpression, tokenTypeAndAnd ? ParenthesesUtils.OR_PRECEDENCE : ParenthesesUtils.AND_PRECEDENCE);
        }
        else if (ComparisonUtils.isComparison(expression)) {
            final PsiBinaryExpression binaryExpression = (PsiBinaryExpression)expression;
            final String negatedComparison = ComparisonUtils.getNegatedComparison(binaryExpression.getOperationTokenType());
            final PsiExpression lhs = binaryExpression.getLOperand();
            final PsiExpression rhs = binaryExpression.getROperand();
            if (rhs != null) {
                final String lhsText = lhs instanceof PsiLiteralExpression ? safeText(lhs) : tracker.text(lhs);
                final String rhsText = rhs instanceof PsiLiteralExpression ? safeText(rhs) : tracker.text(rhs);
                return lhsText + negatedComparison + rhsText;
            }
        }
        if (expression instanceof PsiLiteralExpression) {
            return '!' + safeText(expression);
        }
        return '!' + tracker.text(expression, ParenthesesUtils.PREFIX_PRECEDENCE);
    }

    private static String safeText(PsiExpression expression) {
        if (!(expression instanceof PsiLiteralExpression)) {
            throw new IllegalArgumentException();
        }
        final String text = expression.getText(); // don't need CommentTracker because literal can't contain comment
        final int length = text.length();
        if (text.charAt(0) == '"') {
            if (length == 1 || !text.endsWith("\"") || endsWithEscapedQuote(text)) {
                return text + '"';
            }
        }
        return text;
    }

    private static boolean endsWithEscapedQuote(String text) {
        final int length = text.length();
        if (text.charAt(length - 1) == '"') {
            boolean escaped = false;
            for (int i = length - 2; i > 0; i--) {
                if (text.charAt(i) == '\\') escaped = !escaped;
                else break;
            }
            return escaped;
        }
        return false;
    }




    @NotNull
    public String getText() {
        return "Apply DeMorgan's Law";
    }


    @NotNull
    public String getFamilyName() {
        return "Code optimization";
    }
}
