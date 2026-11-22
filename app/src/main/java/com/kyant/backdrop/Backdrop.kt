/*
 * Copyright (c) 2025 Kyant0
 * Ported from Kyant0/AndroidLiquidGlass (Backdrop 1.0.2)
 * Licensed under the Apache License, Version 2.0
 */
package com.kyant.backdrop

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density

interface Backdrop {

    val isCoordinatesDependent: Boolean

    fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)? = null
    )
}
