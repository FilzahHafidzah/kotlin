package org.jetbrains.jet.plugin.completion.handlers;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DescriptorsUtils;
import org.jetbrains.jet.lang.descriptors.NamedFunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.completion.JetLookupObject;
import org.jetbrains.jet.plugin.quickfix.ImportClassHelper;

/**
 * Inserts '()' after function proposal insert.
 *
 * @author Nikolay Krasko
 */
public class JetFunctionInsertHandler implements InsertHandler<LookupElement> {

    public enum CaretPosition { IN_BRACKETS, AFTER_BRACKETS }

    private final CaretPosition caretPosition;

    public JetFunctionInsertHandler(CaretPosition caretPosition) {
        this.caretPosition = caretPosition;
    }

    @Override
    public void handleInsert(InsertionContext context, LookupElement item) {
        if (context.getCompletionChar() == '(') {
            context.setAddCompletionChar(false);
        }

        int startOffset = context.getStartOffset();
        int lookupStringLength = item.getLookupString().length();
        int endOffset = startOffset + lookupStringLength;

        context.getDocument().insertString(endOffset, "()");

        Editor editor = context.getEditor();
        if (caretPosition == CaretPosition.IN_BRACKETS) {
            editor.getCaretModel().moveToOffset(editor.getCaretModel().getOffset() + 1);
        } else {
            editor.getCaretModel().moveToOffset(editor.getCaretModel().getOffset() + 2);
        }

        PsiDocumentManager.getInstance(context.getProject()).commitDocument(context.getDocument());

        // Should be done after all string insertions and document commitment.
        addImport(context, item);
    }

    private static void addImport(InsertionContext context, LookupElement item) {
        if (context.getFile() instanceof JetFile && item.getObject() instanceof JetLookupObject) {
            final DeclarationDescriptor descriptor = ((JetLookupObject) item.getObject()).getDescriptor();
            if (descriptor instanceof NamedFunctionDescriptor) {

                JetFile file = (JetFile) context.getFile();
                NamedFunctionDescriptor functionDescriptor = (NamedFunctionDescriptor) descriptor;

                if (DescriptorsUtils.isTopLevelFunction(functionDescriptor)) {
                    ImportClassHelper.addImportDirective(DescriptorsUtils.getFQName(functionDescriptor), file);
                }
            }
        }
    }
}
