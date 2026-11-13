package tools.codeme.jetbrains

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

class CodemeExternalAnnotator : ExternalAnnotator<PsiFile, List<CodemeDiagnostic>>() {
    override fun collectInformation(file: PsiFile): PsiFile = file

    override fun doAnnotate(collectedInfo: PsiFile): List<CodemeDiagnostic> {
        val vFile = collectedInfo.virtualFile ?: return emptyList()
        return CodemeService.runCheck(collectedInfo.project, vFile.path)
    }

    override fun apply(file: PsiFile, annotationResult: List<CodemeDiagnostic>, holder: AnnotationHolder) {
        val doc = file.viewProvider.document ?: return
        for (diag in annotationResult) {
            val lineIdx = (diag.line - 1).coerceIn(0, (doc.lineCount - 1).coerceAtLeast(0))
            val start = doc.getLineStartOffset(lineIdx)
            val end = doc.getLineEndOffset(lineIdx)
            val range = TextRange(start, end.coerceAtLeast(start))
            holder.newAnnotation(HighlightSeverity.ERROR, "[${diag.ruleId}] ${diag.message}")
                .range(range)
                .create()
        }
    }
}
