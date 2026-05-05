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
import kotlin.math.tan

/**
 * OpenGL ES 2.0 모바일 3DGS 뷰어 — **Brush** (`brush-main`, MIT)
 * `crates/brush-render` WGSL과 동일한 수학 축을 GLES로 이식한 렌더 코어.
 *
 * 참조 소스 (직접 실행 아님 · 수식 이식):
 * - [helpers.wgsl]: `calc_cam_J`, `calc_cov2d`, `inverse`, `COV_BLUR`, `compute_bbox_extent`
 * - [rasterize.wgsl]: `sigma = 0.5*(conic.x*δx²+conic.z*δy²)+conic.y*δx*δy`, `alpha = min(0.999, opac*exp(-sigma))`
 *
 * 입력 형식은 변경 없음: [MobileSplatScene] 위치·RGB·스칼라 크기 — 단위 쿼터니언·등방 스케일로 3D 공분산을 근사합니다.
 */
class MobileGaussianSplatGlView(context: Context) : GLSurfaceView(context) {

    private val renderer = BrushGaussianRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun setScene(scene: MobileSplatScene) {
        queueEvent { renderer.setScene(scene) }
    }

    fun zoomIn() = renderer.zoom(-ZOOM_STEP)
    fun zoomOut() = renderer.zoom(+ZOOM_STEP)
    fun resetZoom() = renderer.resetZoom()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        renderer.onTouch(event)
        return true
    }

    private class BrushGaussianRenderer : GLSurfaceView.Renderer {

        private var program = 0
        private var aPos = 0
        private var aColor = 0
        private var aSize = 0
        private var uMvp = 0
        private var uModelView = 0
        private var uViewport = 0
        private var uFocal = 0
        private var uPixelCenter = 0
        private var uSigmaScale = 0
        private var uOpacity = 0

        private var posBuf: FloatBuffer? = null
        private var colBuf: FloatBuffer? = null
        private var sizBuf: FloatBuffer? = null
        private var splatCount = 0

        private var surfaceW = 1
        private var surfaceH = 1
        private var aspect = 1f

        @Volatile private var rotX = 0f
        @Volatile private var rotY = 0f
        private var lastX = 0f
        private var lastY = 0f

        @Volatile private var cameraZ = DEFAULT_CAM_Z
        private var lastPinchDist = 0f
        private var isTwoFinger = false

        fun zoom(delta: Float) {
            cameraZ = (cameraZ + delta).coerceIn(MIN_CAM_Z, MAX_CAM_Z)
        }

        fun resetZoom() {
            cameraZ = DEFAULT_CAM_Z
        }

        fun setScene(scene: MobileSplatScene) {
            splatCount = scene.splatCount
            posBuf = toBuffer(scene.positions)
            colBuf = toBuffer(scene.colors)
            sizBuf = toBuffer(scene.sizes)
        }

        private fun toBuffer(data: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer().also { it.put(data); it.position(0) }

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

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0.04f, 0.04f, 0.06f, 1f)
            program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            aPos = GLES20.glGetAttribLocation(program, "aPos")
            aColor = GLES20.glGetAttribLocation(program, "aColor")
            aSize = GLES20.glGetAttribLocation(program, "aSize")
            uMvp = GLES20.glGetUniformLocation(program, "uMvp")
            uModelView = GLES20.glGetUniformLocation(program, "uModelView")
            uViewport = GLES20.glGetUniformLocation(program, "uViewport")
            uFocal = GLES20.glGetUniformLocation(program, "uFocal")
            uPixelCenter = GLES20.glGetUniformLocation(program, "uPixelCenter")
            uSigmaScale = GLES20.glGetUniformLocation(program, "uSigmaScale")
            uOpacity = GLES20.glGetUniformLocation(program, "uOpacity")
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            surfaceW = maxOf(1, width)
            surfaceH = maxOf(1, height)
            aspect = surfaceW.toFloat() / surfaceH.toFloat()
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

            val proj = FloatArray(16)
            val view = FloatArray(16)
            val model = FloatArray(16)
            val modelView = FloatArray(16)
            val mvp = FloatArray(16)
            val z = cameraZ
            GLMatrix.perspectiveM(proj, 0, FOV_Y_DEG, aspect, 0.05f, 40f)
            GLMatrix.setLookAtM(view, 0, 0f, 0f, z, 0f, 0f, 0f, 0f, 1f, 0f)
            GLMatrix.setIdentityM(model, 0)
            GLMatrix.rotateM(model, 0, rotX, 1f, 0f, 0f)
            GLMatrix.rotateM(model, 0, rotY, 0f, 1f, 0f)
            GLMatrix.multiplyMM(modelView, 0, view, 0, model, 0)
            GLMatrix.multiplyMM(mvp, 0, proj, 0, modelView, 0)

            val tany = tan(Math.toRadians(FOV_Y_DEG.toDouble()) * 0.5).toFloat()
            val tanx = tany * aspect
            val fx = surfaceW / (2f * tanx)
            val fy = surfaceH / (2f * tany)

            GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
            GLES20.glUniformMatrix4fv(uModelView, 1, false, modelView, 0)
            GLES20.glUniform2f(uViewport, surfaceW.toFloat(), surfaceH.toFloat())
            GLES20.glUniform2f(uFocal, fx, fy)
            GLES20.glUniform2f(uPixelCenter, surfaceW / 2f, surfaceH / 2f)
            GLES20.glUniform1f(uSigmaScale, SIGMA_WORLD_SCALE)
            GLES20.glUniform1f(uOpacity, SPLAT_OPACITY)

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
            val v = loadShader(GLES20.GL_VERTEX_SHADER, vs)
            val f = loadShader(GLES20.GL_FRAGMENT_SHADER, fs)
            val pr = GLES20.glCreateProgram()
            GLES20.glAttachShader(pr, v)
            GLES20.glAttachShader(pr, f)
            GLES20.glLinkProgram(pr)
            return pr
        }

        companion object {
            private const val DEFAULT_CAM_Z = 2.4f
            private const val MIN_CAM_Z = 0.3f
            private const val MAX_CAM_Z = 12f
            private const val FOV_Y_DEG = 50f
            private const val SIGMA_WORLD_SCALE = 0.052f
            /** Brush rasterize: raw opacity 근사 (sigmoid 거치지 않은 고정값) */
            private const val SPLAT_OPACITY = 0.92f

            /**
             * Brush `calc_cam_J` + `calc_cov2d` + `inverse` + `COV_BLUR` + bbox extent.
             * 카메라 깊이는 GL 뷰 공간에서 양의 전방 깊이로 `max(-mvPos.z, ε)` 사용.
             */
            private const val VERTEX_SHADER = """
                uniform mat4 uMvp;
                uniform mat4 uModelView;
                uniform vec2 uViewport;
                uniform vec2 uFocal;
                uniform vec2 uPixelCenter;
                uniform float uSigmaScale;
                attribute vec3 aPos;
                attribute vec3 aColor;
                attribute float aSize;
                varying vec3 vColor;
                varying vec3 vConic;
                varying float vPointSize;

                vec2 brushApplyJ(vec3 v, vec2 focal, float rz, vec2 uvClip) {
                    vec2 duv = focal * rz;
                    return vec2(
                        duv.x * v.x - duv.x * uvClip.x * v.z,
                        duv.y * v.y - duv.y * uvClip.y * v.z
                    );
                }

                void main() {
                    vec4 mvPos4 = uModelView * vec4(aPos, 1.0);
                    vec3 mc = vec3(mvPos4.x, mvPos4.y, max(-mvPos4.z, 1e-4));
                    vec2 focal = uFocal;
                    vec2 img = uViewport;
                    vec2 pc = uPixelCenter;
                    float rz = 1.0 / mc.z;
                    vec2 lims_pos = (1.15 * img - pc) / focal;
                    vec2 lims_neg = (-0.15 * img - pc) / focal;
                    vec2 uv_clip = clamp(mc.xy * rz, lims_neg, lims_pos);

                    float s = aSize * uSigmaScale;
                    vec3 n0 = uModelView[0].xyz * s;
                    vec3 n1 = uModelView[1].xyz * s;
                    vec3 n2 = uModelView[2].xyz * s;

                    vec2 vn0 = brushApplyJ(n0, focal, rz, uv_clip);
                    vec2 vn1 = brushApplyJ(n1, focal, rz, uv_clip);
                    vec2 vn2 = brushApplyJ(n2, focal, rz, uv_clip);

                    float cov00 = vn0.x * vn0.x + vn1.x * vn1.x + vn2.x * vn2.x;
                    float cov01 = vn0.x * vn0.y + vn1.x * vn1.y + vn2.x * vn2.y;
                    float cov11 = vn0.y * vn0.y + vn1.y * vn1.y + vn2.y * vn2.y;

                    const float COV_BLUR = 0.3;
                    cov00 += COV_BLUR;
                    cov11 += COV_BLUR;

                    float det = cov00 * cov11 - cov01 * cov01;
                    vColor = aColor;
                    gl_Position = uMvp * vec4(aPos, 1.0);

                    if (det <= 1e-8) {
                        vConic = vec3(1.0, 0.0, 1.0);
                        gl_PointSize = 2.0;
                        vPointSize = 2.0;
                        return;
                    }

                    float invDet = 1.0 / det;
                    float inv00 = cov11 * invDet;
                    float inv01 = -cov01 * invDet;
                    float inv11 = cov00 * invDet;
                    vConic = vec3(inv00, inv01, inv11);

                    float opac = 0.92;
                    float powerThresh = log(255.0 * opac);
                    float detC = vConic.x * vConic.z - vConic.y * vConic.y;
                    if (detC <= 1e-8) {
                        gl_PointSize = 2.0;
                        vPointSize = 2.0;
                        return;
                    }
                    vec2 ext = vec2(
                        sqrt(max(0.0, 2.0 * powerThresh * vConic.z / detC)),
                        sqrt(max(0.0, 2.0 * powerThresh * vConic.x / detC))
                    );
                    float ps = clamp(2.2 * max(ext.x, ext.y), 2.0, 520.0);
                    gl_PointSize = ps;
                    vPointSize = ps;
                }
            """

            /** Brush `rasterize.wgsl` 와 동일 σ·α (픽셀 오프셋 δ) */
            private const val FRAGMENT_SHADER = """
                precision mediump float;
                uniform float uOpacity;
                varying vec3 vColor;
                varying vec3 vConic;
                varying float vPointSize;

                void main() {
                    vec2 delta = (gl_PointCoord.xy - vec2(0.5)) * vPointSize;
                    float sigma = 0.5 * (vConic.x * delta.x * delta.x + vConic.z * delta.y * delta.y)
                        + vConic.y * delta.x * delta.y;
                    if (!(sigma >= 0.0)) discard;
                    float alpha = min(0.999, uOpacity * exp(-sigma));
                    if (alpha < 0.00392) discard;
                    gl_FragColor = vec4(max(vColor, vec3(0.0)), alpha);
                }
            """
        }
    }

    companion object {
        private const val ZOOM_STEP = 0.4f
    }
}
