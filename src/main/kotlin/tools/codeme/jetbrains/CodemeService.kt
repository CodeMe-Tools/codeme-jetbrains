package tools.codeme.jetbrains

import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.TimeUnit

data class CodemeDiagnostic(
    val ruleId: String,
    val level: String,
    val message: String,
    val line: Int,
    val column: Int
)

object CodemeService {
    var binaryPath: String = "codeme"

    fun runCheck(project: Project, targetPath: String): List<CodemeDiagnostic> {
        val baseDir = project.basePath?.let { File(it) } ?: File(".")
        val process = ProcessBuilder(binaryPath, "syntax", "check", "--format", "sarif", targetPath)
            .directory(baseDir)
            .apply { environment()["CODEME_NO_UPDATE_CHECK"] = "1" }
            .redirectErrorStream(false)
            .start()

        val finished = process.waitFor(15, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return emptyList()
        }

        val stdout = process.inputStream.bufferedReader().readText()
        return parseSarifMinimal(stdout)
    }

    internal fun parseSarifMinimal(raw: String): List<CodemeDiagnostic> {
        if (raw.isBlank()) return emptyList()
        val results = mutableListOf<CodemeDiagnostic>()
        val ruleRegex = Regex(""""ruleId"\s*:\s*"([^"]+)"""")
        val msgRegex = Regex(""""text"\s*:\s*"([^"]+)"""")
        val lineRegex = Regex(""""startLine"\s*:\s*(\d+)""")
        val colRegex = Regex(""""startColumn"\s*:\s*(\d+)""")

        val rules = ruleRegex.findAll(raw).map { it.groupValues[1] }.toList()
        val msgs = msgRegex.findAll(raw).map { it.groupValues[1] }.toList()
        val lines = lineRegex.findAll(raw).map { it.groupValues[1].toIntOrNull() ?: 1 }.toList()
        val cols = colRegex.findAll(raw).map { it.groupValues[1].toIntOrNull() ?: 1 }.toList()

        for (i in rules.indices) {
            results.add(
                CodemeDiagnostic(
                    ruleId = rules[i],
                    level = "error",
                    message = msgs.getOrElse(i) { "Codeme issue" },
                    line = lines.getOrElse(i) { 1 },
                    column = cols.getOrElse(i) { 1 }
                )
            )
        }
        return results
    }
}
