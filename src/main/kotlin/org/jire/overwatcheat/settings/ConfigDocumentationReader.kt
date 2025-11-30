package org.jire.overwatcheat.settings

import java.io.File

data class SettingDocumentation(
    val description: String,
    val ranges: List<String> = emptyList(),
    val recommended: List<String> = emptyList()
)

class ConfigDocumentationReader(private val file: File = File(Settings.DEFAULT_FILE)) {

    fun read(): Map<String, SettingDocumentation> {
        if (!file.exists()) return emptyMap()

        val lines = file.readLines()
        val documentation = HashMap<String, SettingDocumentation>()

        val comments = mutableListOf<String>()
        for (line in lines) {
            if (line.startsWith('#') || line.startsWith('.')) {
                comments += line.trimStart('#', ' ', '\t', '.')
                continue
            }

            if (line.isBlank()) {
                comments.clear()
                continue
            }

            if (line.contains('=')) {
                val settingName = line.substringBefore('=')
                documentation[settingName] = parseDocumentation(comments)
                comments.clear()
            }
        }

        return documentation
    }

    private fun parseDocumentation(lines: List<String>): SettingDocumentation {
        val descriptionParts = mutableListOf<String>()
        val ranges = mutableListOf<String>()
        val recommended = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Range:", ignoreCase = true) -> ranges += trimmed.removePrefix("Range:").trim()
                trimmed.startsWith("Recommended:", ignoreCase = true) -> recommended += trimmed.removePrefix("Recommended:").trim()
                trimmed.startsWith("Effect:", ignoreCase = true) -> descriptionParts += trimmed.removePrefix("Effect:").trim()
                trimmed.isNotEmpty() -> descriptionParts += trimmed
            }
        }

        return SettingDocumentation(
            description = descriptionParts.joinToString(" ").trim(),
            ranges = ranges,
            recommended = recommended
        )
    }
}

