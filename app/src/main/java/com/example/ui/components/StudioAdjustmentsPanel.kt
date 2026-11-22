package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AcousticProfile
import com.example.model.AiMasteringAdvice
import com.example.model.QualityValidationReport
import com.example.model.StudioAdjustments
import com.example.model.VoicePreset
import com.example.ui.theme.StudioAccent
import com.example.ui.theme.StudioAccentContainer
import com.example.ui.theme.StudioAccentGlow
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioMonoReadout
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Post-Enhancement Studio Presets Strip.
 * Only rendered after AI Enhancement completes.
 */
@Composable
fun StudioPresetStrip(
    selectedPreset: VoicePreset?,
    onSelectPreset: (VoicePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.studio_presets_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioTextPrimary
                )
                Text(
                    text = selectedPreset?.title ?: "AI Auto Result",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioAccentGlow
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(VoicePreset.entries) { preset ->
                    val selected = preset == selectedPreset
                    Surface(
                        onClick = { onSelectPreset(preset) },
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("preset_${preset.name.lowercase()}"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) StudioAccentContainer else StudioSurfaceElevated,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) StudioAccent else StudioBorderSubtle
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.shortTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) StudioTextPrimary else StudioTextSecondary
                            )
                        }
                    }
                }
            }

            Text(
                text = selectedPreset?.subtitle
                    ?: "Automatically calibrated by AI. Select any preset to shape your voice further.",
                style = MaterialTheme.typography.bodySmall,
                color = StudioTextSecondary
            )
        }
    }
}

/**
 * Post-Enhancement Advanced Voice Adjustment Panel with all 11 studio controls.
 * Strictly hidden before initial AI enhancement completes.
 */
@Composable
fun AdvancedVoiceAdjustmentsPanel(
    adjustments: StudioAdjustments,
    aiBaseline: StudioAdjustments,
    isUpdatingPreview: Boolean,
    onUpdate: ((StudioAdjustments) -> StudioAdjustments) -> Unit,
    onResetControl: ((StudioAdjustments, StudioAdjustments) -> StudioAdjustments) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("advanced_adjustments_panel"),
        shape = RoundedCornerShape(18.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.studio_adjustments_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = stringResource(R.string.studio_adjustments_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextMuted
                    )
                }

                AnimatedVisibility(visible = isUpdatingPreview) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(StudioAccentContainer)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = StudioAccentGlow,
                            strokeWidth = 1.8.dp
                        )
                        Text(
                            text = stringResource(R.string.studio_updating_preview),
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioAccentGlow
                        )
                    }
                }
            }

            // Group 1: Core Vocal Clarity & Balance
            AdjustmentGroupLabel("Clarity & Character")

            StudioSliderControl(
                label = stringResource(R.string.adj_voice_clarity),
                description = stringResource(R.string.adj_voice_clarity_desc),
                value = adjustments.voiceClarity,
                baseline = aiBaseline.voiceClarity,
                valueRange = 0f..1f,
                formattedValue = "${(adjustments.voiceClarity * 100f).roundToInt()}%",
                testTag = "slider_voice_clarity",
                onValueChange = { v -> onUpdate { it.copy(voiceClarity = v) } },
                onReset = { onResetControl { cur, base -> cur.copy(voiceClarity = base.voiceClarity) } }
            )

            StudioSliderControl(
                label = stringResource(R.string.adj_voice_balance),
                description = stringResource(R.string.adj_voice_balance_desc),
                value = adjustments.voiceBalance,
                baseline = aiBaseline.voiceBalance,
                valueRange = 0f..1f,
                formattedValue = "${(adjustments.voiceBalance * 100f).roundToInt()}%",
                testTag = "slider_voice_balance",
                onValueChange = { v -> onUpdate { it.copy(voiceBalance = v) } },
                onReset = { onResetControl { cur, base -> cur.copy(voiceBalance = base.voiceBalance) } }
            )

            StudioSliderControl(
                label = stringResource(R.string.adj_warmth),
                description = stringResource(R.string.adj_warmth_desc),
                value = adjustments.warmthDb,
                baseline = aiBaseline.warmthDb,
                valueRange = -3.0f..4.0f,
                formattedValue = formatSignedDb(adjustments.warmthDb),
                testTag = "slider_warmth",
                onValueChange = { v -> onUpdate { it.copy(warmthDb = v) } },
                onReset = { onResetControl { cur, base -> cur.copy(warmthDb = base.warmthDb) } }
            )

            StudioSliderControl(
                label = stringResource(R.string.adj_presence),
                description = stringResource(R.string.adj_presence_desc),
                value = adjustments.presenceDb,
                baseline = aiBaseline.presenceDb,
                valueRange = -3.0f..4.5f,
                formattedValue = formatSignedDb(adjustments.presenceDb),
                testTag = "slider_presence",
                onValueChange = { v -> onUpdate { it.copy(presenceDb = v) } },
                onReset = { onResetControl { cur, base -> cur.copy(presenceDb = base.presenceDb) } }
            )

            HorizontalDivider(color = StudioBorderSubtle)

            // Group 2: Noise, Sibilance & Room Space
            AdjustmentGroupLabel("Noise & Room Cleanup")

            StudioSliderControl(
                label = stringResource(R.string.adj_noise_reduction),
                description = stringResource(R.string.adj_noise_reduction_desc),
                value = adjustments.noiseReduction,
                baseline = aiBaseline.noiseReduction,
                valueRange = 0f..0.94f,
                formattedValue = "${(adjustments.noiseReduction * 100f).roundToInt()}%",
                testTag = "slider_noise_reduction",
                onValueChange = { v -> onUpdate { it.copy(noiseReduction = v) } },
                onReset = { onResetControl { cur, base -> cur.copy(noiseReduction = base.noiseReduction) } }
            )

            StudioSliderControl(
                label = stringResource(R.string.adj_de_esser),
                description = stringResource(R.string.adj_de_esser_desc),
                value = adjustments.deEsser,
                baseline = aiBaseline.deEsser,
                valueRange = 0f..1f,
                formattedValue = "${(adjustments.deEsser * 100f).roundToInt()}%",
                testTag = "slider_de_esser",
                onValueChange = { v -> onUpdate { it.copy(deEsser = v) } },
                onReset = { onResetControl { cur, base -> cur.copy(deEsser = base.deEsser) } }
            )

            StudioSliderControl(
                label = stringResource(R.string.adj_reverb_reduction),
                description = stringResource(R.string.adj_reverb_reduction_desc),
                value = adjustments.reverbReduction,
                baseline = aiBaseline.reverbReduction,
                valueRange = 0f..1f,
                formattedValue = "${(adjustments.reverbReduction * 100f).roundToInt()}%",
                testTag = "slider_reverb_reduction",
                onValueChange = { v -> onUpdate { it.copy(reverbReduction = v) } },
                onReset = { onResetControl { cur, base -> cur.copy(reverbReduction = base.reverbReduction) } }
            )

            HorizontalDivider(color = StudioBorderSubtle)

            // Group 3: Tone EQ, Dynamics & Output Level
            AdjustmentGroupLabel("EQ, Dynamics & Volume")

            StudioSliderControl(
                label = stringResource(R.string.adj_voice_volume),
                description = stringResource(R.string.adj_voice_volume_desc),
                value = adjustments.voiceVolumeDb,
                baseline = aiBaseline.voiceVolumeDb,
                valueRange = -4.0f..4.0f,
                formattedValue = formatSignedDb(adjustments.voiceVolumeDb),
                testTag = "slider_voice_volume",
                onValueChange = { v -> onUpdate { it.copy(voiceVolumeDb = v) } },
                onReset = { onResetControl { cur, base -> cur.copy(voiceVolumeDb = base.voiceVolumeDb) } }
            )

            StudioSliderControl(
                label = stringResource(R.string.adj_bass),
                description = stringResource(R.string.adj_bass_desc),
                value = adjustments.bassDb,
                baseline = aiBaseline.bassDb,
                valueRange = -4.0f..4.0f,
                formattedValue = formatSignedDb(adjustments.bassDb),
                testTag = "slider_bass",
                onValueChange = { v -> onUpdate { it.copy(bassDb = v) } },
                onReset = { onResetControl { cur, base -> cur.copy(bassDb = base.bassDb) } }
            )

            StudioSliderControl(
                label = stringResource(R.string.adj_treble),
                description = stringResource(R.string.adj_treble_desc),
                value = adjustments.trebleDb,
                baseline = aiBaseline.trebleDb,
                valueRange = -4.0f..4.0f,
                formattedValue = formatSignedDb(adjustments.trebleDb),
                testTag = "slider_treble",
                onValueChange = { v -> onUpdate { it.copy(trebleDb = v) } },
                onReset = { onResetControl { cur, base -> cur.copy(trebleDb = base.trebleDb) } }
            )

            StudioSliderControl(
                label = stringResource(R.string.adj_compression),
                description = stringResource(R.string.adj_compression_desc),
                value = adjustments.compression,
                baseline = aiBaseline.compression,
                valueRange = 0f..1f,
                formattedValue = "${(adjustments.compression * 100f).roundToInt()}%",
                testTag = "slider_compression",
                onValueChange = { v -> onUpdate { it.copy(compression = v) } },
                onReset = { onResetControl { cur, base -> cur.copy(compression = base.compression) } }
            )
        }
    }
}

@Composable
private fun AdjustmentGroupLabel(text: String) {
    Text(
        text = text.uppercase(Locale.US),
        style = MaterialTheme.typography.labelSmall,
        color = StudioAccentGlow
    )
}

@Composable
private fun StudioSliderControl(
    label: String,
    description: String,
    value: Float,
    baseline: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    formattedValue: String,
    testTag: String,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    val isModified = abs(value - baseline) > 0.02f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = StudioTextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedValue,
                    style = StudioMonoReadout,
                    color = if (isModified) StudioAccentGlow else StudioTextSecondary
                )
                IconButton(
                    onClick = onReset,
                    enabled = isModified,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("reset_$testTag")
                ) {
                    Icon(
                        imageVector = StudioIcons.RestartAlt,
                        contentDescription = stringResource(R.string.cd_reset_control),
                        tint = if (isModified) StudioTextPrimary else StudioTextMuted.copy(alpha = 0.32f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = SliderDefaults.colors(
                thumbColor = StudioTextPrimary,
                activeTrackColor = StudioAccent,
                inactiveTrackColor = StudioSurfaceElevated
            )
        )
    }
}

private fun formatSignedDb(db: Float): String {
    val rounded = (db * 10f).roundToInt() / 10f
    return when {
        rounded > 0.05f -> String.format(Locale.US, "+%.1f dB", rounded)
        rounded < -0.05f -> String.format(Locale.US, "%.1f dB", rounded)
        else -> "0.0 dB"
    }
}

/**
 * Clean collapsible acoustic diagnostics card shown discreetly at the bottom of the Studio screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EngineDiagnosticsCard(
    originalProfile: AcousticProfile?,
    validationReport: QualityValidationReport?,
    aiAdvice: AiMasteringAdvice?,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (originalProfile == null && aiAdvice == null) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .testTag("toggle_engine_details"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Acoustic Analysis & Verification",
                        style = MaterialTheme.typography.titleSmall,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = aiAdvice?.headline ?: "Adaptive on-device studio report",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextMuted
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = StudioTextSecondary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = StudioBorderSubtle)

                    val chips = aiAdvice?.detectedNoiseSources ?: originalProfile?.detectedIssues.orEmpty()
                    if (chips.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            chips.forEach { issue ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StudioSurfaceElevated)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = issue,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudioTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    if (validationReport != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "Output Quality Verification",
                                style = MaterialTheme.typography.labelMedium,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = "• Exact duration sync: ${if (validationReport.durationExactMatch) "Passed" else "Adjusted"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioSuccess
                            )
                            Text(
                                text = "• Peak distortion check: ${if (validationReport.peakClipFree) "Clip-free (-1.0 dBTP)" else "Attenuated"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioSuccess
                            )
                            Text(
                                text = "• Speech & dynamics: ${validationReport.summary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioSurfaceElevated)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = StudioTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.ai_firebase_notice_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = stringResource(R.string.ai_firebase_notice_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
