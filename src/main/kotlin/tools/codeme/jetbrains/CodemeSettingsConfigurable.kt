package tools.codeme.jetbrains

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class CodemeSettingsConfigurable : Configurable {
    private var binaryField: JTextField? = null

    override fun getDisplayName(): String = "Codeme"

    override fun createComponent(): JComponent {
        val panel = JPanel()
        panel.add(JLabel("Codeme Binary Path:"))
        binaryField = JTextField(CodemeService.binaryPath, 24)
        panel.add(binaryField)
        return panel
    }

    override fun isModified(): Boolean = binaryField?.text != CodemeService.binaryPath

    override fun apply() {
        binaryField?.text?.let { CodemeService.binaryPath = it.trim().ifEmpty { "codeme" } }
    }
}
