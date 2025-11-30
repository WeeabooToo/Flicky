package org.jire.overwatcheat.ui

import org.jire.overwatcheat.settings.*
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

object SettingsWindow {

    private enum class SettingType { INT, FLOAT, DOUBLE, LONG, BOOLEAN, STRING, INT_ARRAY, HEX_INT_ARRAY }

    private data class SettingRow(
        val name: String,
        val label: String,
        val description: String,
        val min: Number? = null,
        val max: Number? = null,
        val examples: List<String> = emptyList(),
        val type: SettingType,
        val step: Number = 1
    )

    private val rows = listOf(
        SettingRow(
            name = "aim_key",
            label = "Aim key (virtual key code)",
            description = "Changes which key must be held for the aimbot; use a spare thumb button to avoid accidental activations.",
            min = 1, max = 254, examples = listOf("6 (thumb button)", "7 (side button)"),
            type = SettingType.INT
        ),
        SettingRow(
            name = "aim_mode",
            label = "Aim mode (0 tracking, 1 flicking)",
            description = "Switch between smooth tracking and flick behavior to match hero style.",
            min = 0, max = 1, examples = listOf("0 for Soldier/Tracer", "1 for McCree/Ashe"),
            type = SettingType.INT
        ),
        SettingRow(
            name = "sensitivity",
            label = "In-game sensitivity",
            description = "Must mirror your Overwatch sensitivity so cursor scaling stays accurate.",
            min = 1.0, max = 20.0, examples = listOf("2.4 scoped", "6.5 high-sens"),
            type = SettingType.FLOAT, step = 0.1
        ),
        SettingRow(
            name = "fps",
            label = "Capture FPS",
            description = "Higher FPS samples more often for responsiveness at the cost of CPU load.",
            min = 60.0, max = 600.0, examples = listOf("144 for low load", "300 balanced", "488 high-end"),
            type = SettingType.DOUBLE, step = 1
        ),
        SettingRow(
            name = "aim_duration_millis",
            label = "Aim duration (ms)",
            description = "Controls how long each correction lasts; shorter is snappier, longer is smoother.",
            min = 0.2, max = 6.0, examples = listOf("0.5 fast flicks", "3.0 smooth tracking"),
            type = SettingType.FLOAT, step = 0.05
        ),
        SettingRow(
            name = "aim_duration_multiplier_base",
            label = "Aim duration multiplier base",
            description = "Raises the minimum sleep window for smoother but slightly less responsive motion.",
            min = 0.5, max = 2.0, examples = listOf("0.75 extra responsive", "1.25 calmer"),
            type = SettingType.FLOAT, step = 0.05
        ),
        SettingRow(
            name = "aim_duration_multiplier_max",
            label = "Aim duration multiplier max",
            description = "Caps how slow aim updates can become; 1.0 keeps timing deterministic.",
            min = 1.0, max = 3.0, examples = listOf("1.0 consistent", "1.5 relaxed", "2.0 softer flick decel"),
            type = SettingType.FLOAT, step = 0.1
        ),
        SettingRow(
            name = "aim_kp",
            label = "Aim proportional gain",
            description = "Scales how aggressively the bot reacts to detected error.",
            min = 0.1, max = 1.0, examples = listOf("0.5 flicks", "0.25 smooth tracking"),
            type = SettingType.FLOAT, step = 0.05
        ),
        SettingRow(
            name = "aim_alpha",
            label = "Aim smoothing alpha",
            description = "Blends error across frames to dampen jitter.",
            min = 0.1, max = 1.0, examples = listOf("0.3 fast snaps", "0.65 calmer"),
            type = SettingType.FLOAT, step = 0.05
        ),
        SettingRow(
            name = "aim_max_move_pixels",
            label = "Max pixels per frame",
            description = "Limits per-frame cursor jumps to control overshoot.",
            min = 3, max = 30, examples = listOf("6 tight", "12 medium", "18 very fast"),
            type = SettingType.INT
        ),
        SettingRow(
            name = "aim_jitter_percent",
            label = "Aim jitter percent",
            description = "Adds random offset for humanization at the cost of precision.",
            min = 0, max = 20, examples = listOf("0 pure aim", "4 subtle", "10 high stealth"),
            type = SettingType.INT
        ),
        SettingRow(
            name = "aim_min_target_width",
            label = "Min target width",
            description = "Ignores detections smaller than this to cut noise.",
            min = 1, max = 16, examples = listOf("2 long-range", "8 cluttered maps"),
            type = SettingType.INT
        ),
        SettingRow(
            name = "aim_min_target_height",
            label = "Min target height",
            description = "Ignores detections shorter than this to cut noise.",
            min = 1, max = 16, examples = listOf("2 long-range", "8 brawling"),
            type = SettingType.INT
        ),
        SettingRow(
            name = "box_width",
            label = "Scan box width",
            description = "Controls horizontal scan size; larger finds more targets at higher CPU cost.",
            min = 96, max = 384, examples = listOf("128 standard", "256 4K"),
            type = SettingType.INT
        ),
        SettingRow(
            name = "box_height",
            label = "Scan box height",
            description = "Controls vertical scan size; larger increases coverage and CPU use.",
            min = 96, max = 384, examples = listOf("128 standard", "256 4K"),
            type = SettingType.INT
        ),
        SettingRow(
            name = "max_snap_divisor",
            label = "Max snap divisor",
            description = "Divides scan box to cap snap distance; lower numbers allow longer flicks.",
            min = 1.2, max = 6.0, examples = listOf("1.5 long flicks", "3.0 conservative"),
            type = SettingType.FLOAT, step = 0.1
        ),
        SettingRow(
            name = "target_colors",
            label = "Target colors (hex)",
            description = "Comma-separated RGB hex values the detector locks onto.",
            examples = listOf("d521cd,d722cf,..."),
            type = SettingType.HEX_INT_ARRAY
        ),
        SettingRow(
            name = "target_color_tolerance",
            label = "Color tolerance",
            description = "Widens acceptable shades; higher is more permissive but riskier.",
            min = 5, max = 40, examples = listOf("12-20 magenta", "25 dim lighting"),
            type = SettingType.INT
        ),
        SettingRow(
            name = "window_title_search",
            label = "Window title search",
            description = "Limits capture to windows containing this text.",
            examples = listOf("Overwatch", "Fullscreen Projector"),
            type = SettingType.STRING
        ),
        SettingRow(
            name = "mouse_id",
            label = "Mouse device ID",
            description = "Select which native mouse device receives movement.",
            min = 11, max = 20, examples = listOf("11 common Linux"),
            type = SettingType.INT
        ),
        SettingRow(
            name = "keyboard_id",
            label = "Keyboard device ID",
            description = "Select which keyboard device receives key presses.",
            min = 1, max = 10, examples = listOf("1 default"),
            type = SettingType.INT
        ),
        SettingRow(
            name = "aim_offset_x",
            label = "Aim offset X",
            description = "Shifts aim anchor horizontally; lower hugs center, higher biases right.",
            min = 0.1, max = 2.0, examples = listOf("0.1 near-center head", "1.1 balanced", "2.0 rightward body"),
            type = SettingType.FLOAT, step = 0.05
        ),
        SettingRow(
            name = "aim_offset_y",
            label = "Aim offset Y",
            description = "Shifts aim anchor vertically; lower favors head height, higher biases chest.",
            min = 0.1, max = 2.0, examples = listOf("0.1 high head", "0.9 headshots", "2.0 chest focus"),
            type = SettingType.FLOAT, step = 0.05
        ),
        SettingRow(
            name = "flick_shoot_pixels",
            label = "Flick shoot radius (px)",
            description = "Radius for accepting a flick before firing; smaller is stricter.",
            min = 2, max = 15, examples = listOf("5 precision", "9 permissive"),
            type = SettingType.INT
        ),
        SettingRow(
            name = "flick_stability_frames",
            label = "Flick stability frames",
            description = "Frames that must stay inside radius before firing to avoid misfires.",
            min = 1, max = 5, examples = listOf("1 ultra fast", "3 safer"),
            type = SettingType.INT
        ),
        SettingRow(
            name = "flick_readiness_alpha",
            label = "Flick readiness smoothing",
            description = "Exponential smoothing for flick readiness; higher calms jitter.",
            min = 0.1, max = 0.95, examples = listOf("0.4 snappy", "0.75 noisy feeds"),
            type = SettingType.FLOAT, step = 0.05
        ),
        SettingRow(
            name = "flick_pause_duration",
            label = "Flick pause duration (ms)",
            description = "Cooldown between flick shots to prevent double-firing.",
            min = 120, max = 500, examples = listOf("180 revolvers", "300 snipers", "400 Ashe"),
            type = SettingType.LONG, step = 10
        ),
        SettingRow(
            name = "toggle_in_game_ui",
            label = "Toggle in-game UI",
            description = "Automatically hide UI when aiming to reduce visual noise.",
            examples = listOf("true hide HUD", "false keep HUD"),
            type = SettingType.BOOLEAN
        ),
        SettingRow(
            name = "toggle_key_codes",
            label = "Toggle UI key codes (hex)",
            description = "Comma-separated hex virtual key codes sent to toggle UI.",
            examples = listOf("12,5A for ALT+Z"),
            type = SettingType.HEX_INT_ARRAY
        ),
        SettingRow(
            name = "aim_precise_sleeper_type",
            label = "Aim precise sleeper type",
            description = "0=YIELD balanced, 1=SPIN_WAIT fastest/high CPU, 2=SLEEP low CPU/high jitter.",
            min = 0, max = 2, examples = listOf("1 competitive", "0 balanced", "2 saving CPU"),
            type = SettingType.INT
        ),
        SettingRow(
            name = "aim_cpu_thread_affinity_index",
            label = "CPU affinity index",
            description = "Pin aim thread to a core (-1 disables pinning).",
            min = -1, max = 31, examples = listOf("-1 disable", "4 physical core on 8-thread"),
            type = SettingType.INT
        )
    )

    fun launch() {
        SwingUtilities.invokeLater {
            val frame = JFrame("Overwatcheat Live Settings")
            frame.defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
            val container = JPanel()
            container.layout = BoxLayout(container, BoxLayout.Y_AXIS)

            rows.forEach { row ->
                container.add(buildRow(row))
            }

            val scrollPane = JScrollPane(container)
            frame.contentPane.add(scrollPane, BorderLayout.CENTER)
            frame.setSize(980, 900)
            frame.isVisible = true
        }
    }

    private fun buildRow(row: SettingRow): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createTitledBorder(row.label)

        val descriptionLabel = JLabel(buildDescription(row))
        val editorPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val editor = createEditor(row)
        val applyButton = JButton("Apply")
        val statusLabel = JLabel("Current: ${Settings.currentValue(row.name)}")

        applyButton.addActionListener {
            val rawValue = readValue(row, editor)
            try {
                Settings.update(row.name, rawValue)
                statusLabel.text = "Live: ${Settings.currentValue(row.name)}"
            } catch (e: Exception) {
                statusLabel.text = "Error: ${e.message}"
            }
        }

        editorPanel.add(JLabel("Value:"))
        editorPanel.add(editor)
        editorPanel.add(applyButton)
        editorPanel.add(statusLabel)

        panel.add(descriptionLabel)
        panel.add(editorPanel)
        return panel
    }

    private fun buildDescription(row: SettingRow): String {
        val parts = mutableListOf(row.description)
        val range = when {
            row.min != null && row.max != null -> "Range: ${row.min} - ${row.max}"
            row.min != null -> "Min: ${row.min}"
            row.max != null -> "Max: ${row.max}"
            else -> null
        }
        if (range != null) parts.add(range)
        if (row.examples.isNotEmpty()) parts.add("Examples: ${row.examples.joinToString(", ")}")
        return "<html>${parts.joinToString("<br/>")}</html>"
    }

    private fun createEditor(row: SettingRow): JComponent {
        val current = Settings.currentValue(row.name)
        return when (row.type) {
            SettingType.INT -> JSpinner(
                SpinnerNumberModel(
                    current.toIntOrNull() ?: 0,
                    row.min?.toInt() ?: Int.MIN_VALUE,
                    row.max?.toInt() ?: Int.MAX_VALUE,
                    row.step.toInt().coerceAtLeast(1)
                )
            )
            SettingType.LONG -> JSpinner(
                SpinnerNumberModel(
                    current.toLongOrNull() ?: 0L,
                    row.min?.toLong() ?: Long.MIN_VALUE,
                    row.max?.toLong() ?: Long.MAX_VALUE,
                    row.step.toLong().coerceAtLeast(1L)
                )
            )
            SettingType.FLOAT -> JSpinner(
                SpinnerNumberModel(
                    current.toDoubleOrNull() ?: 0.0,
                    row.min?.toDouble() ?: -Double.MAX_VALUE,
                    row.max?.toDouble() ?: Double.MAX_VALUE,
                    row.step.toDouble()
                )
            )
            SettingType.DOUBLE -> JSpinner(
                SpinnerNumberModel(
                    current.toDoubleOrNull() ?: 0.0,
                    row.min?.toDouble() ?: -Double.MAX_VALUE,
                    row.max?.toDouble() ?: Double.MAX_VALUE,
                    row.step.toDouble()
                )
            )
            SettingType.BOOLEAN -> JCheckBox("Enabled", current.toBoolean())
            SettingType.STRING, SettingType.INT_ARRAY, SettingType.HEX_INT_ARRAY -> JTextField(current, 35)
        }
    }

    private fun readValue(row: SettingRow, component: JComponent): String = when (row.type) {
        SettingType.INT, SettingType.LONG, SettingType.FLOAT, SettingType.DOUBLE ->
            (component as JSpinner).value.toString()
        SettingType.BOOLEAN -> (component as JCheckBox).isSelected.toString()
        SettingType.STRING, SettingType.INT_ARRAY, SettingType.HEX_INT_ARRAY -> (component as JTextField).text.trim()
    }
}
