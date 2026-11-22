package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lightweight, zero-bloat vector icons replacing material-icons-extended
 * (which otherwise adds 42+ MB of DEX bytecode and slows cold startup on emulators).
 */
object StudioIcons {

    val Mic: ImageVector by lazy {
        icon("StudioMic") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 14f)
                curveTo(13.66f, 14f, 15f, 12.66f, 15f, 11f)
                verticalLineTo(5f)
                curveTo(15f, 3.34f, 13.66f, 2f, 12f, 2f)
                reflectiveCurveTo(9f, 3.34f, 9f, 5f)
                verticalLineTo(11f)
                curveTo(9f, 12.66f, 10.34f, 14f, 12f, 14f)
                close()
                moveTo(17.3f, 11f)
                curveTo(17.3f, 14f, 14.76f, 16.1f, 12f, 16.1f)
                reflectiveCurveTo(6.7f, 14f, 6.7f, 11f)
                horizontalLineTo(5f)
                curveTo(5f, 14.41f, 7.72f, 17.23f, 11f, 17.72f)
                verticalLineTo(21f)
                horizontalLineTo(13f)
                verticalLineTo(17.72f)
                curveTo(16.28f, 17.23f, 19f, 14.41f, 19f, 11f)
                horizontalLineTo(17.3f)
                close()
            }
        }
    }

    val Pause: ImageVector by lazy {
        icon("StudioPause") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 19f)
                horizontalLineTo(10f)
                verticalLineTo(5f)
                horizontalLineTo(6f)
                verticalLineTo(19f)
                close()
                moveTo(14f, 5f)
                verticalLineTo(19f)
                horizontalLineTo(18f)
                verticalLineTo(5f)
                horizontalLineTo(14f)
                close()
            }
        }
    }

    val Stop: ImageVector by lazy {
        icon("StudioStop") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 6f)
                horizontalLineTo(18f)
                verticalLineTo(18f)
                horizontalLineTo(6f)
                close()
            }
        }
    }

    val GraphicEq: ImageVector by lazy {
        icon("StudioGraphicEq") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(7f, 18f); horizontalLineTo(9f); verticalLineTo(6f); horizontalLineTo(7f); close()
                moveTo(11f, 22f); horizontalLineTo(13f); verticalLineTo(2f); horizontalLineTo(11f); close()
                moveTo(3f, 14f); horizontalLineTo(5f); verticalLineTo(10f); horizontalLineTo(3f); close()
                moveTo(15f, 18f); horizontalLineTo(17f); verticalLineTo(6f); horizontalLineTo(15f); close()
                moveTo(19f, 14f); horizontalLineTo(21f); verticalLineTo(10f); horizontalLineTo(19f); close()
            }
        }
    }

    val UploadFile: ImageVector by lazy {
        icon("StudioUploadFile") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(14f, 2f)
                horizontalLineTo(6f)
                curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
                verticalLineTo(20f)
                curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
                horizontalLineTo(18f)
                curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
                verticalLineTo(8f)
                lineTo(14f, 2f)
                close()
                moveTo(18f, 20f)
                horizontalLineTo(6f)
                verticalLineTo(4f)
                horizontalLineTo(13f)
                verticalLineTo(9f)
                horizontalLineTo(18f)
                verticalLineTo(20f)
                close()
                moveTo(8f, 15.01f)
                lineTo(9.41f, 16.42f)
                lineTo(11f, 14.84f)
                verticalLineTo(19f)
                horizontalLineTo(13f)
                verticalLineTo(14.84f)
                lineTo(14.59f, 16.43f)
                lineTo(16f, 15.01f)
                lineTo(12.01f, 11f)
                lineTo(8f, 15.01f)
                close()
            }
        }
    }

    val FolderOpen: ImageVector by lazy {
        icon("StudioFolderOpen") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(20f, 6f)
                horizontalLineTo(12f)
                lineTo(10f, 4f)
                horizontalLineTo(4f)
                curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
                verticalLineTo(18f)
                curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                horizontalLineTo(20f)
                curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                verticalLineTo(8f)
                curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
                close()
                moveTo(20f, 18f)
                horizontalLineTo(4f)
                verticalLineTo(8f)
                horizontalLineTo(20f)
                verticalLineTo(18f)
                close()
            }
        }
    }

    val AudioFile: ImageVector by lazy {
        icon("StudioAudioFile") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(14f, 2f)
                horizontalLineTo(6f)
                curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
                verticalLineTo(20f)
                curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
                horizontalLineTo(18f)
                curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
                verticalLineTo(8f)
                lineTo(14f, 2f)
                close()
                moveTo(16f, 13f)
                horizontalLineTo(13f)
                verticalLineTo(16.75f)
                curveTo(13f, 17.99f, 11.99f, 19f, 10.75f, 19f)
                reflectiveCurveTo(8.5f, 17.99f, 8.5f, 16.75f)
                reflectiveCurveTo(9.51f, 14.5f, 10.75f, 14.5f)
                curveTo(11.17f, 14.5f, 11.55f, 14.62f, 11.88f, 14.82f)
                verticalLineTo(11f)
                horizontalLineTo(16f)
                verticalLineTo(13f)
                close()
                moveTo(13f, 9f)
                verticalLineTo(3.5f)
                lineTo(18.5f, 9f)
                horizontalLineTo(13f)
                close()
            }
        }
    }

    val AutoAwesome: ImageVector by lazy {
        icon("StudioAutoAwesome") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 9f)
                lineTo(20.25f, 6.25f)
                lineTo(23f, 5f)
                lineTo(20.25f, 3.75f)
                lineTo(19f, 1f)
                lineTo(17.75f, 3.75f)
                lineTo(15f, 5f)
                lineTo(17.75f, 6.25f)
                lineTo(19f, 9f)
                close()
                moveTo(11.5f, 9.5f)
                lineTo(9f, 4f)
                lineTo(6.5f, 9.5f)
                lineTo(1f, 12f)
                lineTo(6.5f, 14.5f)
                lineTo(9f, 20f)
                lineTo(11.5f, 14.5f)
                lineTo(17f, 12f)
                lineTo(11.5f, 9.5f)
                close()
                moveTo(19f, 15f)
                lineTo(17.75f, 17.75f)
                lineTo(15f, 19f)
                lineTo(17.75f, 20.25f)
                lineTo(19f, 23f)
                lineTo(20.25f, 20.25f)
                lineTo(23f, 19f)
                lineTo(20.25f, 17.75f)
                lineTo(19f, 15f)
                close()
            }
        }
    }

    val Download: ImageVector by lazy {
        icon("StudioDownload") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 20f)
                horizontalLineTo(19f)
                verticalLineTo(18f)
                horizontalLineTo(5f)
                verticalLineTo(20f)
                close()
                moveTo(19f, 9f)
                horizontalLineTo(15f)
                verticalLineTo(3f)
                horizontalLineTo(9f)
                verticalLineTo(9f)
                horizontalLineTo(5f)
                lineTo(12f, 16f)
                lineTo(19f, 9f)
                close()
            }
        }
    }

    val Shield: ImageVector by lazy {
        icon("StudioShield") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 1f)
                lineTo(3f, 5f)
                verticalLineTo(11f)
                curveTo(3f, 16.55f, 6.84f, 21.74f, 12f, 23f)
                curveTo(17.16f, 21.74f, 21f, 16.55f, 21f, 11f)
                verticalLineTo(5f)
                lineTo(12f, 1f)
                close()
            }
        }
    }

    val Replay5: ImageVector by lazy {
        icon("StudioReplay5") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 5f)
                curveTo(16.42f, 5f, 20f, 8.58f, 20f, 13f)
                curveTo(20f, 17.42f, 16.42f, 21f, 12f, 21f)
                curveTo(7.58f, 21f, 4f, 17.42f, 4f, 13f)
                curveTo(4f, 10.6f, 5.05f, 8.45f, 6.72f, 6.98f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                lineTo(8f, 5.2f)
                lineTo(12f, 8.4f)
                close()
            }
            // Crisp "5" numeral inside
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 10.5f)
                horizontalLineTo(10.5f)
                verticalLineTo(13f)
                horizontalLineTo(13f)
                curveTo(14f, 13f, 14.5f, 13.7f, 14.5f, 14.7f)
                curveTo(14.5f, 15.8f, 13.7f, 16.5f, 12.2f, 16.5f)
                horizontalLineTo(10.2f)
            }
        }
    }

    val Forward5: ImageVector by lazy {
        icon("StudioForward5") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 5f)
                curveTo(7.58f, 5f, 4f, 8.58f, 4f, 13f)
                curveTo(4f, 17.42f, 7.58f, 21f, 12f, 21f)
                curveTo(16.42f, 21f, 20f, 17.42f, 20f, 13f)
                curveTo(20f, 10.6f, 18.95f, 8.45f, 17.28f, 6.98f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                lineTo(16f, 5.2f)
                lineTo(12f, 8.4f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 10.5f)
                horizontalLineTo(10.5f)
                verticalLineTo(13f)
                horizontalLineTo(13f)
                curveTo(14f, 13f, 14.5f, 13.7f, 14.5f, 14.7f)
                curveTo(14.5f, 15.8f, 13.7f, 16.5f, 12.2f, 16.5f)
                horizontalLineTo(10.2f)
            }
        }
    }

    val SwapHoriz: ImageVector by lazy {
        icon("StudioSwapHoriz") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.99f, 11f)
                lineTo(3f, 15f)
                lineTo(6.99f, 19f)
                verticalLineTo(16f)
                horizontalLineTo(14f)
                verticalLineTo(14f)
                horizontalLineTo(6.99f)
                verticalLineTo(11f)
                close()
                moveTo(21f, 9f)
                lineTo(17.01f, 5f)
                verticalLineTo(8f)
                horizontalLineTo(10f)
                verticalLineTo(10f)
                horizontalLineTo(17.01f)
                verticalLineTo(13f)
                lineTo(21f, 9f)
                close()
            }
        }
    }

    val Undo: ImageVector by lazy {
        icon("StudioUndo") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.5f, 8f)
                curveTo(9.85f, 8f, 7.45f, 8.99f, 5.6f, 10.6f)
                lineTo(2f, 7f)
                verticalLineTo(16f)
                horizontalLineTo(11f)
                lineTo(7.38f, 12.38f)
                curveTo(8.77f, 11.22f, 10.54f, 10.5f, 12.5f, 10.5f)
                curveTo(16.04f, 10.5f, 19.05f, 12.81f, 20.1f, 16f)
                lineTo(22.47f, 15.22f)
                curveTo(21.08f, 11.03f, 17.15f, 8f, 12.5f, 8f)
                close()
            }
        }
    }

    val RestartAlt: ImageVector by lazy {
        icon("StudioRestartAlt") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 5f)
                verticalLineTo(2f)
                lineTo(7f, 6.5f)
                lineTo(12f, 11f)
                verticalLineTo(7f)
                curveTo(15.31f, 7f, 18f, 9.69f, 18f, 13f)
                curveTo(18f, 16.31f, 15.31f, 19f, 12f, 19f)
                curveTo(8.69f, 19f, 6f, 16.31f, 6f, 13f)
                horizontalLineTo(4f)
                curveTo(4f, 17.42f, 7.58f, 21f, 12f, 21f)
                curveTo(16.42f, 21f, 20f, 17.42f, 20f, 13f)
                curveTo(20f, 8.58f, 16.42f, 5f, 12f, 5f)
                close()
            }
        }
    }

    val Verified: ImageVector by lazy {
        icon("StudioVerified") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(12f, 2f)
                lineTo(14.59f, 4.12f)
                lineTo(17.93f, 4.07f)
                lineTo(19.07f, 7.22f)
                lineTo(21.75f, 9.24f)
                lineTo(20.9f, 12.48f)
                lineTo(22f, 15.65f)
                lineTo(19.45f, 17.82f)
                lineTo(18.67f, 21.08f)
                lineTo(15.35f, 21.17f)
                lineTo(12.92f, 23.45f)
                lineTo(10.08f, 21.76f)
                lineTo(6.77f, 22.13f)
                lineTo(5.32f, 19.12f)
                lineTo(2.43f, 17.43f)
                lineTo(2.94f, 14.13f)
                lineTo(1.5f, 11.12f)
                lineTo(3.8f, 8.69f)
                lineTo(4.24f, 5.38f)
                lineTo(7.53f, 4.94f)
                lineTo(9.71f, 2.39f)
                lineTo(12f, 2f)
                close()
                moveTo(10.4f, 16.6f)
                lineTo(6.8f, 13f)
                lineTo(8.2f, 11.6f)
                lineTo(10.4f, 13.8f)
                lineTo(16f, 8.2f)
                lineTo(17.4f, 9.6f)
                lineTo(10.4f, 16.6f)
                close()
            }
        }
    }

    val ErrorOutline: ImageVector by lazy {
        icon("StudioErrorOutline") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveTo(6.48f, 22f, 12f, 22f)
                reflectiveCurveTo(22f, 17.52f, 22f, 12f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(12f, 20f)
                curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
                reflectiveCurveTo(7.59f, 4f, 12f, 4f)
                reflectiveCurveTo(20f, 7.59f, 20f, 12f)
                reflectiveCurveTo(16.41f, 20f, 12f, 20f)
                close()
                moveTo(11f, 15f); horizontalLineTo(13f); verticalLineTo(17f); horizontalLineTo(11f); close()
                moveTo(11f, 7f); horizontalLineTo(13f); verticalLineTo(13f); horizontalLineTo(11f); close()
            }
        }
    }

    val RemoveCircle: ImageVector by lazy {
        icon("StudioRemoveCircle") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveTo(6.48f, 22f, 12f, 22f)
                reflectiveCurveTo(22f, 17.52f, 22f, 12f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(12f, 20f)
                curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
                reflectiveCurveTo(7.59f, 4f, 12f, 4f)
                reflectiveCurveTo(20f, 7.59f, 20f, 12f)
                reflectiveCurveTo(16.41f, 20f, 12f, 20f)
                close()
                moveTo(7f, 11f); horizontalLineTo(17f); verticalLineTo(13f); horizontalLineTo(7f); close()
            }
        }
    }

    private inline fun icon(
        name: String,
        block: ImageVector.Builder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(block).build()
}
