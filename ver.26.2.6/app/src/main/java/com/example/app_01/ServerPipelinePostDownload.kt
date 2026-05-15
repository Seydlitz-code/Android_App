package com.example.app_01

import android.content.Context
import android.util.Log
import java.util.concurrent.Executors

/**
 * 서버 파이프라인 결과 저장 직후 무거운 후처리(JSON 라이브러리 복사, 오래된 task 폴더 정리)를
 * 다운로드 suspend 경로·Compose와 분리하기 위한 전용 큐.
 *
 * 대용량 파일 복사와 동시에 완료 다이얼로그·메인 스레드 갱신이 겹치며 발생하는 OOM/지연을 줄이기 위해
 * 단일 백그라운드 스레드에서 약간 지연 후 실행합니다.
 */
object ServerPipelinePostDownload {
    private const val TAG = "ServerPipelinePostDownload"

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "server-pipeline-post").apply { isDaemon = true }
    }

    fun writeManifestIfNeeded(bundle: ServerPipelineResultBundle) {
        runCatching {
            writeServerTaskArtifactManifest(bundle.directory, bundle.taskId, bundle.filesByKey)
        }.onFailure { t ->
            Log.w(TAG, "서버 결과 매니페스트 저장 실패", t)
        }
    }

    fun scheduleDeferredHeavyWork(context: Context, bundle: ServerPipelineResultBundle) {
        val app = context.applicationContext
        executor.execute {
            try {
                Thread.sleep(200L)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            runCatching {
                JsonLibrary.ingestFromPipelineOutputDir(
                    app,
                    bundle.directory,
                    bundle.taskId,
                    bundle.filesByKey,
                )
            }.onFailure { t ->
                Log.w(TAG, "JSON 라이브러리 반영 실패", t)
            }
            runCatching {
                pruneOldServerTaskDirs(app, keepLatest = 8, protectedTaskId = bundle.taskId)
            }.onFailure { t ->
                Log.w(TAG, "서버 결과 폴더 정리 실패", t)
            }
        }
    }
}
