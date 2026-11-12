package com.sec.oneui.clock.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Vertical Touch Scroll Wheel Picker for Hours, Minutes, Seconds, and AM/PM.
 * Supports smooth touch snapping, center item highlight, and direct keypad typing
 * by clicking directly on numbers.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    initialIndex: Int,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 3,
    itemHeight: Dp = 48.dp,
    formatItem: (T) -> String = { it.toString() },
    isNumeric: Boolean = false,
    minValue: Int = 0,
    maxValue: Int = 59
) {
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val safeInitialIndex = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current

    val currentIndex by remember {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            val firstIndex = listState.firstVisibleItemIndex
            val index = if (offset > itemHeightPx / 2f) firstIndex + 1 else firstIndex
            index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        }
    }

    // Direct Keypad Time Input State
    var isEditing by remember { mutableStateOf(false) }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = if (safeInitialIndex in items.indices) formatItem(items[safeInitialIndex]) else "",
                selection = TextRange(0)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    // External synchronization (e.g. presets, switching alarms, or 12/24h toggle)
    LaunchedEffect(initialIndex) {
        if (!listState.isScrollInProgress && !isEditing && currentIndex != initialIndex && initialIndex in items.indices) {
            listState.scrollToItem(initialIndex)
        }
    }

    // Emit selection to parent when scrolled
    LaunchedEffect(currentIndex) {
        if (currentIndex in items.indices && !isEditing) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onItemSelected(items[currentIndex])
        }
    }

    // Auto-dismiss keypad if user starts scrolling the wheel
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && isEditing) {
            isEditing = false
            keyboardController?.hide()
        }
    }

    fun applyInputValue(text: String) {
        val num = text.toIntOrNull()
        if (num != null && num in minValue..maxValue) {
            val targetIndex = items.indexOfFirst {
                (it as? Int) == num || it.toString().toIntOrNull() == num
            }
            if (targetIndex != -1) {
                coroutineScope.launch {
                    listState.scrollToItem(targetIndex)
                }
                onItemSelected(items[targetIndex])
            }
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection highlight bar in iOS glass style
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = isNumeric && !isEditing
                ) {
                    if (currentIndex in items.indices) {
                        val currentStr = formatItem(items[currentIndex])
                        textFieldValue = TextFieldValue(
                            text = currentStr,
                            selection = TextRange(0, currentStr.length)
                        )
                        isEditing = true
                    }
                }
        )

        if (!isEditing) {
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = itemHeight * (visibleItemsCount / 2)),
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(items) { index, item ->
                    val isSelected = index == currentIndex
                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = isNumeric
                            ) {
                                val currentStr = if (currentIndex in items.indices) formatItem(items[currentIndex]) else ""
                                textFieldValue = TextFieldValue(
                                    text = currentStr,
                                    selection = TextRange(0, currentStr.length)
                                )
                                isEditing = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatItem(item),
                            fontSize = if (isSelected) 36.sp else 22.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        } else {
            // Direct Keypad Input Field with Numeric Keyboard
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newVal ->
                        val digits = newVal.text.filter { it.isDigit() }.take(2)
                        textFieldValue = newVal.copy(
                            text = digits,
                            selection = TextRange(digits.length)
                        )
                        val num = digits.toIntOrNull()
                        if (num != null && num in minValue..maxValue) {
                            applyInputValue(digits)
                            if (digits.length == 2) {
                                isEditing = false
                                keyboardController?.hide()
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val num = textFieldValue.text.toIntOrNull()
                            if (num != null && num in minValue..maxValue) {
                                applyInputValue(textFieldValue.text)
                            } else {
                                if (currentIndex in items.indices) {
                                    textFieldValue = TextFieldValue(formatItem(items[currentIndex]))
                                }
                            }
                            isEditing = false
                            keyboardController?.hide()
                        }
                    ),
                    textStyle = TextStyle(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && isEditing) {
                                val num = textFieldValue.text.toIntOrNull()
                                if (num != null && num in minValue..maxValue) {
                                    applyInputValue(textFieldValue.text)
                                } else {
                                    if (currentIndex in items.indices) {
                                        textFieldValue = TextFieldValue(formatItem(items[currentIndex]))
                                    }
                                }
                                isEditing = false
                            }
                        }
                )
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }
}
