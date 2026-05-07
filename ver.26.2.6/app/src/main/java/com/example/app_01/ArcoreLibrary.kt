package com.example.app_01

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

    /** 촬영 미디어(사진) + `poses.json`(UTF-8)을 ZIP으로 ARCore 라이브러리에 저장 */
    fun savePhotoAndPosesZip(context: Context, photoFile: File, posesJson: String): File {
        val zip = File(dir(context), "arcore_capture_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            zos.putNextEntry(ZipEntry(photoFile.name))
            BufferedInputStream(FileInputStream(photoFile)).use { input ->
                input.copyTo(zos)
            }
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("poses.json"))
            zos.write(posesJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.appendArchiveSummaryBottom(
                imageFileCount = 1,
                jsonFileCount = 1,
                extraLinesKo = listOf("이미지: ${photoFile.name}", "포즈 JSON: poses.json"),
                imageNames = listOf(photoFile.name),
                jsonPaths = listOf("poses.json"),
                datasetFolderName = null,
                videoIncluded = false,
                videoFileName = null,
            )
        }
        return zip
    }

    /**
     * 동영상 **전체** 타임라인 ARCore: MP4 + 단일 `video_arcore.json`, 하단 요약.
     */
    fun saveVideoFullTimelineArCoreZip(
        context: Context,
        videoFile: File,
        videoArCoreJsonPretty: String,
    ): File {
        val zip = File(dir(context), "arcore_video_full_${System.currentTimeMillis()}.zip")
        val jsonEntryName = "video_arcore.json"
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
                    "전체 타임라인 ARCore JSON: $jsonEntryName",
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
     * 동영상 데이터셋: 스크린샷 JPG, 이미지별 ARCore JSON(`arcore/`), 선택적 MP4,
     * **하단**에 요약 JSON + **가장 마지막**에 요약 TXT.
     */
    fun saveVideoDatasetArcoreZip(
        context: Context,
        datasetDir: File,
        imageFiles: List<File>,
        jsonByImageFileName: Map<String, JSONObject>,
        videoFile: File?,
    ): File {
        val zip = File(dir(context), "arcore_video_dataset_${System.currentTimeMillis()}.zip")
        val jsonEntryNames = ArrayList<String>()
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

            for (img in imageFiles) {
                addDiskFile("dataset/${img.name}", img)
            }
            for (img in imageFiles) {
                val jo = jsonByImageFileName[img.name] ?: continue
                val jsonName = img.name.substringBeforeLast('.') + ".json"
                val path = "arcore/$jsonName"
                addBytes(path, jo.toString().toByteArray(Charsets.UTF_8))
                jsonEntryNames.add(path)
            }
            if (videoFile != null && videoFile.isFile) {
                addDiskFile(videoFile.name, videoFile)
            }

            val imageCount = imageFiles.size
            val jsonCount = jsonEntryNames.size
            val videoIncluded = videoFile != null && videoFile.isFile

            val extraKo = buildList {
                add("dataset/ 이미지 ${imageCount}장")
                add("arcore/ JSON ${jsonCount}개")
            }

            zos.appendArchiveSummaryBottom(
                imageFileCount = imageCount,
                jsonFileCount = jsonCount,
                extraLinesKo = extraKo,
                imageNames = imageFiles.map { it.name },
                jsonPaths = jsonEntryNames,
                datasetFolderName = datasetDir.name,
                videoIncluded = videoIncluded,
                videoFileName = if (videoIncluded) videoFile!!.name else null,
            )
        }
        return zip
    }
}
