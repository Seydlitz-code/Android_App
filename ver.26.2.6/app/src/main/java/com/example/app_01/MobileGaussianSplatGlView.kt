package com.example.app_01

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix as GLMatrix
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt

/**
 * OpenGL ES 2.0 **가우시안 포인트 스플랫** 뷰어 (gl_PointSize + gl_PointCoord 기반).
 *
 * 지원 제스처:
 *  - 단일 손가락 드래그 → 회전 (rotX, rotY)
 *  - 두 손가락 핀치 → 줌 인/아웃 (cameraZ 조절)
 *  - [zoomIn] / [zoomOut] / [resetZoom] 메서드 → 버튼 연동
 */
class MobileGaussianSplatGlView(context: Context) : GLSurfaceView(context) {

    private val renderer = MobileGaussianSplatRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun setScene(scene: MobileSplatScene) {
        queueEvent { renderer.setScene(scene) }
    }

    /** 버튼으로 줌 인 (카메라를 씬에 가깝게) */
    fun zoomIn()   { renderer.zoom(-ZOOM_STEP) }
    /** 버튼으로 줌 아웃 (카메라를 씬에서 멀리) */
    fun zoomOut()  { renderer.zoom(+ZOOM_STEP) }
    /** 기본 줌 복원 */
    fun resetZoom(){ renderer.resetZoom() }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        renderer.onTouch(event)
        return true
    }

    // ─────────────────────────────────────────────────────────────────────────

    private class MobileGaussianSplatRenderer : GLSurfaceView.Renderer {

        private var program = 0
        private var aPos   = 0
        private var aColor = 0
        private var aSize  = 0
        private var uMvp   = 0
        private var uZoomScale = 0

        private var posBuf: FloatBuffer? = null
        private var colBuf: FloatBuffer? = null
        private var sizBuf: FloatBuffer? = null
        private var splatCount = 0

        private var aspect = 1f

        // ── 회전 ──────────────────────────────────────────────────────────
        @Volatile private var rotX = 0f
        @Volatile private var rotY = 0f
        private var lastX = 0f
        private var lastY = 0f

        // ── 줌 (카메라 Z 거리) ────────────────────────────────────────────
        @Volatile private var cameraZ = DEFAULT_CAM_Z
        private var lastPinchDist = 0f
        private var isTwoFinger = false

        // ── 공개 줌 제어 ──────────────────────────────────────────────────
        fun zoom(delta: Float) {
            cameraZ = (cameraZ + delta).coerceIn(MIN_CAM_Z, MAX_CAM_Z)
        }
        fun resetZoom() { cameraZ = DEFAULT_CAM_Z }

        // ── 씬 설정 ───────────────────────────────────────────────────────
        fun setScene(scene: MobileSplatScene) {
            splatCount = scene.splatCount
            posBuf = toBuffer(scene.positions)
            colBuf = toBuffer(scene.colors)
            sizBuf = toBuffer(scene.sizes)
        }

        private fun toBuffer(data: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer().also { it.put(data); it.position(0) }

        // ── 터치 처리 ─────────────────────────────────────────────────────
        fun onTouch(event: MotionEvent) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x; lastY = event.y
                    isTwoFinger = false
                    lastPinchDist = 0f
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount >= 2) {
                        isTwoFinger = true
                        lastPinchDist = pinchDist(event)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isTwoFinger && event.pointerCount >= 2) {
                        val dist = pinchDist(event)
                        if (lastPinchDist > 0f) {
                            // 핀치 아웃(dist 증가) → 줌 인(cameraZ 감소)
                            val ratio = lastPinchDist / dist
                            cameraZ = (cameraZ * ratio).coerceIn(MIN_CAM_Z, MAX_CAM_Z)
                        }
                        lastPinchDist = dist
                    } else if (!isTwoFinger) {
                        val dx = event.x - lastX
                        val dy = event.y - lastY
                        rotY += dx * 0.45f
                        rotX += dy * 0.45f
                        rotX = rotX.coerceIn(-89f, 89f)
                        lastX = event.x; lastY = event.y
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    isTwoFinger = false
                    lastPinchDist = 0f
                    // 남은 포인터의 위치 기억 (회전 점프 방지)
                    val remaining = if (event.actionIndex == 0 && event.pointerCount > 1) 1 else 0
                    if (remaining < event.pointerCount) {
                        lastX = event.getX(remaining); lastY = event.getY(remaining)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isTwoFinger = false
                    lastPinchDist = 0f
                }
            }
        }

        private fun pinchDist(e: MotionEvent): Float {
            val dx = e.getX(0) - e.getX(1)
            val dy = e.getY(0) - e.getY(1)
            return sqrt(dx * dx + dy * dy)
        }

        // ── OpenGL 렌더링 ─────────────────────────────────────────────────
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0.04f, 0.04f, 0.06f, 1f)
            program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            aPos       = GLES20.glGetAttribLocation(program,  "aPos")
            aColor     = GLES20.glGetAttribLocation(program,  "aColor")
            aSize      = GLES20.glGetAttribLocation(program,  "aSize")
            uMvp       = GLES20.glGetUniformLocation(program, "uMvp")
            uZoomScale = GLES20.glGetUniformLocation(program, "uZoomScale")
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            aspect = if (height > 0) width.toFloat() / height.toFloat() else 1f
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            val p = posBuf ?: return
            val c = colBuf ?: return
            val s = sizBuf ?: return
            if (splatCount <= 0) return

            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)

            GLES20.glUseProgram(program)

            val proj  = FloatArray(16)
            val view  = FloatArray(16)
            val model = FloatArray(16)
            val mvp   = FloatArray(16)
            val z = cameraZ
            GLMatrix.perspectiveM(proj, 0, 50f, aspect, 0.05f, 40f)
            GLMatrix.setLookAtM(view, 0, 0f, 0f, z, 0f, 0f, 0f, 0f, 1f, 0f)
            GLMatrix.setIdentityM(model, 0)
            GLMatrix.rotateM(model, 0, rotX, 1f, 0f, 0f)
            GLMatrix.rotateM(model, 0, rotY, 0f, 1f, 0f)
            GLMatrix.multiplyMM(mvp, 0, view, 0, model, 0)
            GLMatrix.multiplyMM(mvp, 0, proj, 0, mvp, 0)

            GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
            // 원근 보정 기준 거리: 카메라 기본 Z 거리를 전달
            // 셰이더에서 gl_Position.w(실제 클립 깊이)로 나눠 원근 올바른 포인트 크기 계산
            GLES20.glUniform1f(uZoomScale, DEFAULT_CAM_Z)

            GLES20.glEnableVertexAttribArray(aPos)
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, p)
            GLES20.glEnableVertexAttribArray(aColor)
            GLES20.glVertexAttribPointer(aColor, 3, GLES20.GL_FLOAT, false, 0, c)
            GLES20.glEnableVertexAttribArray(aSize)
            GLES20.glVertexAttribPointer(aSize, 1, GLES20.GL_FLOAT, false, 0, s)

            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, splatCount)

            GLES20.glDisableVertexAttribArray(aPos)
            GLES20.glDisableVertexAttribArray(aColor)
            GLES20.glDisableVertexAttribArray(aSize)
        }

        private fun loadShader(type: Int, code: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, code)
            GLES20.glCompileShader(shader)
            return shader
        }

        private fun createProgram(vs: String, fs: String): Int {
            val v = loadShader(GLES20.GL_VERTEX_SHADER,   vs)
            val f = loadShader(GLES20.GL_FRAGMENT_SHADER, fs)
            val p = GLES20.glCreateProgram()
            GLES20.glAttachShader(p, v)
            GLES20.glAttachShader(p, f)
            GLES20.glLinkProgram(p)
            return p
        }

        companion object {
            private const val DEFAULT_CAM_Z = 2.4f
            private const val MIN_CAM_Z     = 0.3f
            private const val MAX_CAM_Z     = 12f

            private const val VERTEX_SHADER = """
                uniform mat4  uMvp;
                uniform float uZoomScale;
                attribute vec3  aPos;
                attribute vec3  aColor;
                attribute float aSize;
                varying vec3 vColor;
                void main() {
                    vColor      = aColor;
                    gl_Position = uMvp * vec4(aPos, 1.0);
                    // 원근 올바른 포인트 크기: 기준 거리(uZoomScale=DEFAULT_CAM_Z)를
                    // 클립 깊이(gl_Position.w)로 나눠 가까운 점은 크게, 먼 점은 작게
                    gl_PointSize = aSize * uZoomScale / max(gl_Position.w, 0.1);
                }
            """

            private const val FRAGMENT_SHADER = """
                precision mediump float;
                varying vec3 vColor;
                void main() {
                    vec2  d  = gl_PointCoord - vec2(0.5);
                    float r2 = dot(d, d);
                    if (r2 > 0.25) discard;
                    float alpha = exp(-r2 * 22.0);
                    gl_FragColor = vec4(vColor, alpha);
                }
            """
        }
    }

    companion object {
        private const val ZOOM_STEP = 0.4f
    }
}
