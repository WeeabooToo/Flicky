package org.jire.overwatcheat.settings.ui

import org.jire.overwatcheat.aimbot.AimBotThread
import org.jire.overwatcheat.aimbot.AimColorMatcher
import org.jire.overwatcheat.aimbot.AimMode
import org.jire.overwatcheat.settings.*
import org.jire.overwatcheat.util.PreciseSleeper
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class SettingsWindow(
    private val aimColorMatcher: AimColorMatcher,
    private val aimBotThread: AimBotThread,
) {

    private val documentation = ConfigDocumentationReader().read()
    private val settingControls = mutableListOf<SettingControl>()
    private val statusLabel = JLabel(" ")

    fun show() {
        SwingUtilities.invokeLater { buildFrame() }
    }

    private fun buildFrame() {
        val frame = JFrame("Overwatcheat Settings")
        frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        frame.layout = BorderLayout()

        val tabs = JTabbedPane()
        tabs.addTab("Input", createTabPanel(inputSettings()))
        tabs.addTab("Aim Behavior", createTabPanel(aimBehaviorSettings()))
        tabs.addTab("Targeting", createTabPanel(targetingSettings()))
        tabs.addTab("System", createTabPanel(systemSettings()))
        frame.add(tabs, BorderLayout.CENTER)

        frame.add(buildApplyBar(), BorderLayout.SOUTH)

        frame.pack()
        frame.setSize(950, 720)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }

    private fun buildApplyBar(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(8, 8, 8, 8)

        val applyButton = JButton("Apply Changes")
        applyButton.addActionListener { applyChanges() }

        statusLabel.foreground = Color(0x35, 0x8e, 0x28)
        panel.add(applyButton, BorderLayout.EAST)
        panel.add(statusLabel, BorderLayout.CENTER)
        return panel
    }

    private fun createTabPanel(settings: List<Setting>): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = EmptyBorder(12, 12, 12, 12)

        settings.forEach { setting ->
            val row = createSettingRow(setting)
            panel.add(row)
            panel.add(Box.createRigidArea(Dimension(0, 6)))
        }

        panel.add(Box.createVerticalGlue())
        return JScrollPane(panel)
    }

    private fun createSettingRow(setting: Setting): JComponent {
        val container = JPanel(GridBagLayout())
        container.border = EmptyBorder(6, 6, 6, 6)
        val gc = GridBagConstraints()
        gc.insets = Insets(4, 4, 4, 4)
        gc.fill = GridBagConstraints.HORIZONTAL

        gc.gridx = 0
        gc.gridy = 0
        gc.weightx = 0.1
        container.add(JLabel(setting.name), gc)

        gc.gridx = 1
        gc.weightx = 0.4
        val component = createComponent(setting)
        container.add(component, gc)

        documentation[setting.name]?.let { doc ->
            gc.gridx = 2
            gc.weightx = 0.5
            container.add(buildDocPanel(doc), gc)
        }

        return container
    }

    private fun buildDocPanel(doc: SettingDocumentation): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = EmptyBorder(4, 12, 4, 4)

        if (doc.description.isNotBlank()) {
            val descriptionLabel = JLabel("<html><body style='width: 320px'>${doc.description}</body></html>")
            panel.add(descriptionLabel)
        }

        if (doc.ranges.isNotEmpty()) {
            panel.add(JLabel("Range: ${doc.ranges.joinToString("; ")}"))
        }

        if (doc.recommended.isNotEmpty()) {
            panel.add(JLabel("Recommended: ${doc.recommended.joinToString("; ")}"))
        }

        return panel
    }

    private fun createComponent(setting: Setting): JComponent {
        return when (setting) {
            is BooleanSetting -> JCheckBox().apply {
                isSelected = setting.value
                settingControls += SettingControl(setting) { setting.value = isSelected }
            }

            is IntSetting -> createIntComponent(setting)
            is LongSetting -> createSpinner(setting.value.toDouble()) { settingControls += SettingControl(setting) { setting.value = (value as Number).toLong() } }
            is FloatSetting -> createSpinner(setting.value.toDouble()) { settingControls += SettingControl(setting) { setting.value = (value as Number).toFloat() } }
            is DoubleSetting -> createSpinner(setting.value) { settingControls += SettingControl(setting) { setting.value = (value as Number).toDouble() } }

            is StringSetting -> JTextField(setting.value).apply {
                settingControls += SettingControl(setting) { setting.value = text }
            }

            is IntArraySetting -> JTextField(setting.value.joinToString(",")).apply {
                settingControls += SettingControl(setting) { setting.parse(text) }
            }

            is HexIntArraySetting -> JTextField(setting.value.joinToString(",") { Integer.toHexString(it).uppercase() }).apply {
                settingControls += SettingControl(setting) { setting.parse(text) }
            }

            else -> JTextField().apply {
                settingControls += SettingControl(setting) {}
            }
        }
    }

    private fun createIntComponent(setting: IntSetting): JComponent {
        return when (setting.name) {
            "aim_mode" -> {
                val modes = AimMode.values().associateBy({ it.type }, { it.name })
                JComboBox(modes.values.toTypedArray()).apply {
                    selectedItem = modes[setting.value]
                    settingControls += SettingControl(setting) {
                        val selectedType = modes.entries.firstOrNull { it.value == selectedItem }?.key ?: setting.value
                        setting.value = selectedType
                    }
                }
            }

            "aim_precise_sleeper_type" -> {
                val sleepers = PreciseSleeper.values().associateBy({ it.type }, { it.name })
                JComboBox(sleepers.values.toTypedArray()).apply {
                    selectedItem = sleepers[setting.value]
                    settingControls += SettingControl(setting) {
                        val selectedType = sleepers.entries.firstOrNull { it.value == selectedItem }?.key ?: setting.value
                        setting.value = selectedType
                    }
                }
            }

            else -> createIntSpinner(setting.value) {
                settingControls += SettingControl(setting) { setting.value = (value as Number).toInt() }
            }
        }
    }

    private fun createIntSpinner(initial: Int, onCreate: JSpinner.() -> Unit): JSpinner {
        val spinner = JSpinner(SpinnerNumberModel(initial, -Int.MAX_VALUE, Int.MAX_VALUE, 1))
        spinner.onCreate()
        return spinner
    }

    private fun createSpinner(initial: Double, onCreate: JSpinner.() -> Unit): JSpinner {
        val spinner = JSpinner(SpinnerNumberModel(initial, -1_000_000.0, 1_000_000.0, 0.05))
        spinner.onCreate()
        return spinner
    }

    private fun applyChanges() {
        try {
            settingControls.forEach { it.applyValue() }
            aimColorMatcher.refreshMatchSet()
            aimBotThread.refreshFromSettings()
            Settings.write()
            statusLabel.foreground = Color(0x35, 0x8e, 0x28)
            statusLabel.text = "Applied at ${java.time.LocalTime.now()}"
        } catch (e: Exception) {
            statusLabel.text = "Failed to apply: ${e.message}".also {
                statusLabel.foreground = Color(0xC0, 0x39, 0x2B)
            }
        }
    }

    private fun inputSettings() = listOf(
        Settings.nameToSetting["aim_key"]!!,
        Settings.nameToSetting["aim_mode"]!!,
        Settings.nameToSetting["aim_offset_x"]!!,
        Settings.nameToSetting["aim_offset_y"]!!,
        Settings.nameToSetting["aim_precise_sleeper_type"]!!,
        Settings.nameToSetting["aim_cpu_thread_affinity_index"]!!,
        Settings.nameToSetting["mouse_id"]!!,
        Settings.nameToSetting["keyboard_id"]!!,
        Settings.nameToSetting["toggle_in_game_ui"]!!,
        Settings.nameToSetting["toggle_key_codes"]!!,
    )

    private fun aimBehaviorSettings() = listOf(
        Settings.nameToSetting["sensitivity"]!!,
        Settings.nameToSetting["fps"]!!,
        Settings.nameToSetting["aim_duration_millis"]!!,
        Settings.nameToSetting["aim_duration_multiplier_base"]!!,
        Settings.nameToSetting["aim_duration_multiplier_max"]!!,
        Settings.nameToSetting["aim_kp"]!!,
        Settings.nameToSetting["aim_alpha"]!!,
        Settings.nameToSetting["aim_max_move_pixels"]!!,
        Settings.nameToSetting["aim_jitter_percent"]!!,
        Settings.nameToSetting["aim_min_target_width"]!!,
        Settings.nameToSetting["aim_min_target_height"]!!,
        Settings.nameToSetting["max_snap_divisor"]!!,
        Settings.nameToSetting["flick_shoot_pixels"]!!,
        Settings.nameToSetting["flick_stability_frames"]!!,
        Settings.nameToSetting["flick_readiness_alpha"]!!,
        Settings.nameToSetting["flick_pause_duration"]!!,
    )

    private fun targetingSettings() = listOf(
        Settings.nameToSetting["box_width"]!!,
        Settings.nameToSetting["box_height"]!!,
        Settings.nameToSetting["target_colors"]!!,
        Settings.nameToSetting["target_color_tolerance"]!!,
    )

    private fun systemSettings() = listOf(
        Settings.nameToSetting["window_title_search"]!!,
    )

    private data class SettingControl(val setting: Setting, val applyValue: () -> Unit)
}

