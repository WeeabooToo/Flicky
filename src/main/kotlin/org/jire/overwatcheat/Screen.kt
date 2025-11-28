/*
 * Free, open-source undetected color cheat for Overwatch!
 * Copyright (C) 2017  Thomas G. Nappo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jire.overwatcheat

import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Toolkit

object Screen {

    private val PRIMARY_DISPLAY = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice

    /**
     * Prefer the primary display's mode so the capture box remains centered on a
     * single monitor (e.g., 2560x1440) even when the virtual desktop spans
     * multiple screens. Falls back to the virtual screen size if no mode info
     * is available.
     */
    private val DIMENSION: Dimension = PRIMARY_DISPLAY.displayMode?.let { Dimension(it.width, it.height) }
        ?: Toolkit.getDefaultToolkit().screenSize

    val WIDTH = DIMENSION.width
    val HEIGHT = DIMENSION.height

    const val OVERLAY_OFFSET = 1
    val OVERLAY_WIDTH = WIDTH - OVERLAY_OFFSET
    val OVERLAY_HEIGHT = HEIGHT - OVERLAY_OFFSET

}