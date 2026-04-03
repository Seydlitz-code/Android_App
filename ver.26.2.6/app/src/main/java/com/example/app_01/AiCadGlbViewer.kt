package com.example.app_01

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import java.io.File

/**
 * Filament(SceneView)로 GLB 미리보기 — 다이어그램의 3D 뷰어 단계.
 * SceneView 2.3.0 Compose API (model-viewer-compose 샘플과 동일 계열).
 *
 * 주의: 샘플 코드의 HDR 경로는 앱 assets에 없으면 createHDREnvironment가 null을 돌려 강제 언래핑 시 크래시한다.
 * Scene 기본 조명(rememberEnvironment)을 쓰도록 environment를 넘기지 않는다.
 */
@Composable
fun AiCadGlbViewer(
    glbFile: File,
    modifier: Modifier = Modifier,
    /** 첫 렌더 프레임 직후(뷰가 그려지기 시작할 때) 한 번 */
    onFirstFrameRendered: () -> Unit = {}
) {
    key(glbFile.absolutePath) {
        val onReady = rememberUpdatedState(onFirstFrameRendered)
        val onFrameOnce = remember(glbFile.absolutePath) {
            val fired = booleanArrayOf(false)
            val callback: (Long) -> Unit = { _: Long ->
                if (!fired[0]) {
                    fired[0] = true
                    onReady.value.invoke()
                }
            }
            callback
        }
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val centerNode = rememberNode(engine)
        val cameraNode = rememberCameraNode(engine) {
            position = Position(y = -0.5f, z = 2.0f)
            lookAt(centerNode)
            centerNode.addChildNode(this)
        }
        Scene(
            modifier = modifier,
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = cameraNode.worldPosition,
                targetPosition = centerNode.worldPosition
            ),
            childNodes = listOf(
                centerNode,
                rememberNode(engine) {
                    ModelNode(
                        modelInstance = modelLoader.createModelInstance(glbFile),
                        scaleToUnits = 1.0f
                    )
                }
            ),
            onFrame = onFrameOnce
        )
    }
}
