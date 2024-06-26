package com.example;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DemorgansLawIntention extends PsiElementBaseIntentionAction implements IntentionAction {

    public boolean isAvailable(@NotNull Project project, Editor editor, @Nullable PsiElement element) {
        if (element == null) {
            return false;
        }

        if (element instanceof PsiJavaToken token) {
            if(token.getTokenType() == JavaTokenType.ANDAND || token.getTokenType() == JavaTokenType.OROR) {
                return token.getParent() instanceof PsiBinaryExpression;
            }
        }
        return false;
    }

    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        PsiBinaryExpression binaryExpression = (PsiBinaryExpression) element.getParent();
        PsiElement newExpression = null;
        if (element instanceof PsiJavaToken token) {
            if(binaryExpression.getROperand()==null) return;
            if (token.getTokenType() == JavaTokenType.ANDAND) {
                newExpression = JavaPsiFacade.getElementFactory(project).createExpressionFromText(
                        "!( !(" + binaryExpression.getLOperand().getText() + ") || !(" + binaryExpression.getROperand().getText() + ") )", binaryExpression);
            } else if (token.getTokenType() == JavaTokenType.OROR) {
                newExpression = JavaPsiFacade.getElementFactory(project).createExpressionFromText(
                        "!( !(" + binaryExpression.getLOperand().getText() + ") && !(" + binaryExpression.getROperand().getText() + ") )", binaryExpression);
            }
        }
        if (newExpression != null) {
            binaryExpression.replace(newExpression);
        }
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
