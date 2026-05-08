package com.example.app_01

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** ARCore 업로드 ZIP 레이아웃 — 서버 `main.py` POST /upload 주석과 동일 (`images/`, 루트 `poses.json`). */
object ArcoreServerZipLayout {
    const val IMAGES_DIR = "images"
    const val POSES_JSON = "poses.json"

    fun imageEntryName(index: Int): String =
        "$IMAGES_DIR/img_${index.toString().padStart(6, '0')}.jpg"
}

/** ARCore 관련 파일 보관 (`models/arcore/`). GLB/이미지·구성 JSON 등을 수동 복사해 사용 */
object ArcoreLibrary {
    fun dir(context: Context): File {
        val d = File(ModelLibraryPaths.legacyModelsDir(context), "arcore")
        d.mkdirs()
        return d
    }

    fun listFilesSorted(context: Context): List<File> {
        val d = dir(context)
        return d.listFiles { f -> f.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * ZIP **하단**: `arcore_archive_summary.json` 다음, **가장 마지막 항목**으로 `arcore_archive_summary.txt`.
     * [jsonFileCount]는 포즈·데이터 JSON만 센다(요약 json 자체는 개수에 넣지 않음).
     */
    private fun ZipOutputStream.appendArchiveSummaryBottom(
        imageFileCount: Int,
        jsonFileCount: Int,
        extraLinesKo: List<String> = emptyList(),
        imageNames: List<String> = emptyList(),
        jsonPaths: List<String> = emptyList(),
        datasetFolderName: String? = null,
        videoIncluded: Boolean = false,
        videoFileName: String? = null,
    ) {
        val summary = JSONObject()
        summary.put("imageFileCount", imageFileCount)
        summary.put("jsonFileCount", jsonFileCount)
        summary.put(
            "descriptionKo",
            buildString {
                append("이 압축 파일에는 이미지 ${imageFileCount}장, JSON ${jsonFileCount}개가 포함되어 있습니다.")
                append(" (포즈·연동 JSON 기준; ZIP 맨 아래 요약 파일 제외)")
                extraLinesKo.forEach { append(' ').append(it) }
            },
        )
        if (datasetFolderName != null) {
            summary.put("datasetFolderName", datasetFolderName)
        }
        summary.put("videoFileIncluded", videoIncluded)
        if (videoFileName != null) {
            summary.put("videoFileName", videoFileName)
        }
        if (imageNames.isNotEmpty()) {
            summary.put("imageFiles", JSONArray(imageNames))
        }
        if (jsonPaths.isNotEmpty()) {
            summary.put("arcoreJsonZipPaths", JSONArray(jsonPaths))
            summary.put("jsonEntries", JSONArray(jsonPaths))
        }
        summary.put("arcoreJsonFileCount", jsonFileCount)
        summary.put(
            "zipLayoutKo",
            "맨 아래 항목: arcore_archive_summary.txt, 그 위: arcore_archive_summary.json",
        )

        putNextEntry(ZipEntry("arcore_archive_summary.json"))
        write(summary.toString().toByteArray(Charsets.UTF_8))
        closeEntry()

        val sep = "─".repeat(36)
        val txt = buildString {
            appendLine("ARCore 압축 파일 요약")
            appendLine(sep)
            appendLine("이미지 파일: ${imageFileCount}장")
            appendLine("JSON 파일: ${jsonFileCount}개 (포즈/연동 JSON, 요약 json 제외)")
            if (videoIncluded && videoFileName != null) {
                appendLine("동영상 파일: 포함 (${videoFileName})")
            }
            appendLine(sep)
            extraLinesKo.forEach { appendLine(it) }
            if (extraLinesKo.isNotEmpty()) appendLine(sep)
            appendLine("항목 상세: arcore_archive_summary.json")
        }
        putNextEntry(ZipEntry("arcore_archive_summary.txt"))
        write(txt.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    /** 촬영 미디어(사진) + 루트 `poses.json` — 서버 예시: `images/img_000000.jpg`, `poses.json` */
    fun savePhotoAndPosesZip(context: Context, photoFile: File, posesJson: String): File {
        val zip = File(dir(context), "arcore_capture_${System.currentTimeMillis()}.zip")
        val imageEntry = ArcoreServerZipLayout.imageEntryName(0)
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            zos.putNextEntry(ZipEntry(imageEntry))
            BufferedInputStream(FileInputStream(photoFile)).use { input ->
                input.copyTo(zos)
            }
            zos.closeEntry()
            zos.putNextEntry(ZipEntry(ArcoreServerZipLayout.POSES_JSON))
            zos.write(posesJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.appendArchiveSummaryBottom(
                imageFileCount = 1,
                jsonFileCount = 1,
                extraLinesKo = listOf(
                    "이미지: $imageEntry",
                    "포즈 JSON: ${ArcoreServerZipLayout.POSES_JSON} (ZIP 루트)",
                ),
                imageNames = listOf(imageEntry.removePrefix("${ArcoreServerZipLayout.IMAGES_DIR}/")),
                jsonPaths = listOf(ArcoreServerZipLayout.POSES_JSON),
                datasetFolderName = ArcoreServerZipLayout.IMAGES_DIR,
                videoIncluded = false,
                videoFileName = null,
            )
        }
        return zip
    }

    /**
     * 연속 촬영 배치: 서버와 동일하게 `images/img_000000.jpg` … 및 ZIP 루트 **단일** `poses.json`
     * (`frames` 배열, 각 항목 `filename`은 zip 내 이미지 파일명과 일치).
     *
     * [imageFiles]: 촬영 순서대로 ZIP에 `images/img_######.jpg`로 넣음.
     * [mergedPosesJson]: 루트 `poses.json` — `frames[]`의 `filename`이 위 이미지명과 일치해야 함.
     */
    fun saveContinuousBurstArcoreZip(
        context: Context,
        imageFiles: List<File>,
        mergedPosesJson: String,
    ): File {
        val zip = File(dir(context), "arcore_continuous_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            fun addDiskFile(entryPath: String, file: File) {
                zos.putNextEntry(ZipEntry(entryPath))
                BufferedInputStream(FileInputStream(file)).use { input ->
                    input.copyTo(zos)
                }
                zos.closeEntry()
            }

            var idx = 0
            for (img in imageFiles) {
                if (img.isFile) {
                    addDiskFile(ArcoreServerZipLayout.imageEntryName(idx), img)
                    idx++
                }
            }

            zos.putNextEntry(ZipEntry(ArcoreServerZipLayout.POSES_JSON))
            zos.write(mergedPosesJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            val imageCount = idx
            val jsonCount = 1
            val extraKo = listOf(
                "${ArcoreServerZipLayout.IMAGES_DIR}/ 이미지 ${imageCount}장 (img_######.jpg)",
                "루트 ${ArcoreServerZipLayout.POSES_JSON} (병합 frames)",
            )
            val imageNames = (0 until imageCount).map { i ->
                "img_${i.toString().padStart(6, '0')}.jpg"
            }
            zos.appendArchiveSummaryBottom(
                imageFileCount = imageCount,
                jsonFileCount = jsonCount,
                extraLinesKo = extraKo,
                imageNames = imageNames,
                jsonPaths = listOf(ArcoreServerZipLayout.POSES_JSON),
                datasetFolderName = ArcoreServerZipLayout.IMAGES_DIR,
                videoIncluded = false,
                videoFileName = null,
            )
        }
        return zip
    }

    /** ARCore 라이브러리 그리드 표시용: ZIP 안의 `arcore_archive_summary.json`에서 개수를 읽는다. */
    fun readArchiveSummaryCounts(zipFile: File): Pair<Int, Int>? {
        if (!zipFile.isFile || !zipFile.name.endsWith(".zip", ignoreCase = true)) return null
        return runCatching {
            ZipFile(zipFile).use { zf ->
                val ent = zf.getEntry("arcore_archive_summary.json") ?: return@use null
                val txt = zf.getInputStream(ent).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val jo = JSONObject(txt)
                val img = jo.optInt("imageFileCount", -1)
                val json = jo.optInt("jsonFileCount", -1)
                if (img < 0 || json < 0) null else img to json
            }
        }.getOrNull()
    }

    /**
     * 동영상 **전체** 타임라인 ARCore: MP4 + 루트 `poses.json`(서버 전처리와 동일 파일명).
     */
    fun saveVideoFullTimelineArCoreZip(
        context: Context,
        videoFile: File,
        videoArCoreJsonPretty: String,
    ): File {
        val zip = File(dir(context), "arcore_video_full_${System.currentTimeMillis()}.zip")
        val jsonEntryName = ArcoreServerZipLayout.POSES_JSON
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            zos.putNextEntry(ZipEntry(videoFile.name))
            BufferedInputStream(FileInputStream(videoFile)).use { input ->
                input.copyTo(zos)
            }
            zos.closeEntry()
            zos.putNextEntry(ZipEntry(jsonEntryName))
            zos.write(videoArCoreJsonPretty.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            zos.appendArchiveSummaryBottom(
                imageFileCount = 0,
                jsonFileCount = 1,
                extraLinesKo = listOf(
                    "동영상 파일: ${videoFile.name}",
                    "전체 타임라인 ARCore JSON(루트): $jsonEntryName",
                ),
                imageNames = emptyList(),
                jsonPaths = listOf(jsonEntryName),
                datasetFolderName = null,
                videoIncluded = true,
                videoFileName = videoFile.name,
            )
        }
        return zip
    }

    /**
     * 동영상 데이터셋: `images/img_######.jpg` + 루트 병합 `poses.json`, 선택적 MP4(루트).
     */
    fun saveVideoDatasetArcoreZip(
        context: Context,
        datasetDir: File,
        imageFiles: List<File>,
        jsonByImageFileName: Map<String, JSONObject>,
        videoFile: File?,
    ): File {
        val zip = File(dir(context), "arcore_video_dataset_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            fun addBytes(entryPath: String, bytes: ByteArray) {
                zos.putNextEntry(ZipEntry(entryPath))
                zos.write(bytes)
                zos.closeEntry()
            }
            fun addDiskFile(entryPath: String, file: File) {
                zos.putNextEntry(ZipEntry(entryPath))
                BufferedInputStream(FileInputStream(file)).use { input ->
                    input.copyTo(zos)
                }
                zos.closeEntry()
            }

            for ((i, img) in imageFiles.withIndex()) {
                addDiskFile(ArcoreServerZipLayout.imageEntryName(i), img)
            }

            val frames = JSONArray()
            for ((i, img) in imageFiles.withIndex()) {
                val jo = jsonByImageFileName[img.name] ?: continue
                val c = JSONObject(jo.toString())
                c.put("filename", "img_${i.toString().padStart(6, '0')}.jpg")
                frames.put(c)
            }
            val root = JSONObject().put("frames", frames)
            addBytes(ArcoreServerZipLayout.POSES_JSON, root.toString().toByteArray(Charsets.UTF_8))

            if (videoFile != null && videoFile.isFile) {
                addDiskFile(videoFile.name, videoFile)
            }

            val imageCount = imageFiles.size
            val jsonCount = 1
            val videoIncluded = videoFile != null && videoFile.isFile
            val extraKo = listOf(
                "${ArcoreServerZipLayout.IMAGES_DIR}/ 이미지 ${imageCount}장",
                "루트 ${ArcoreServerZipLayout.POSES_JSON} (병합 frames)",
            )
            val imageNames = (0 until imageCount).map { i ->
                "img_${i.toString().padStart(6, '0')}.jpg"
            }

            zos.appendArchiveSummaryBottom(
                imageFileCount = imageCount,
                jsonFileCount = jsonCount,
                extraLinesKo = extraKo,
                imageNames = imageNames,
                jsonPaths = listOf(ArcoreServerZipLayout.POSES_JSON),
                datasetFolderName = datasetDir.name,
                videoIncluded = videoIncluded,
                videoFileName = if (videoIncluded) videoFile!!.name else null,
            )
        }
        return zip
    }
}
