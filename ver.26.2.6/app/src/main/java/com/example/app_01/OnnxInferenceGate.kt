package com.example.app_01

/**
 * ONNX Runtime 세션은 다중 스레드에서 동시 [OrtSession.run] 시 일부 기기에서 불안정할 수 있어
 * 배경 제거(U²-Net) 등 모든 온디바이스 ONNX 추론을 직렬화한다.
 */
object OnnxInferenceGate {
    val lock = Any()
}
