package com.example.app_01

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 게시판 목록 표시용 유틸 — 기존 `web_board_reference/formatBoardDisplay.ts`와 동일한 규칙.
 * (년·월·일·시·분, 제목·게시판명 말줄임, 페이지 안내 문구)
 */
object BoardDisplayFormat {

    const val TITLE_MAX = 30

    /** 게시판 이름 한 줄 최대 글자(초과 시 …) */
    const val BOARD_NAME_MAX = 10

    /** 목록용: YYYY.MM.DD HH:mm (기기 로컬 타임존) */
    fun formatPostDateListed(input: Any?): String {
        val cal = parseToCalendar(input) ?: return ""
        val y = cal.get(Calendar.YEAR)
        val mo = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val mi = cal.get(Calendar.MINUTE)
        return String.format(Locale.getDefault(), "%d.%02d.%02d %02d:%02d", y, mo, day, h, mi)
    }

    /** 한 줄 최대 글자 수(유니코드 코드포인트 기준) 초과 시 말줄임 */
    fun ellipsisLine(s: String?, max: Int = TITLE_MAX): String {
        val t = s?.trim() ?: ""
        if (t.isEmpty()) return ""
        val cps = t.codePointCount(0, t.length)
        if (cps <= max) return t
        var endIndex = 0
        var count = 0
        var i = 0
        while (i < t.length && count < max) {
            val cp = t.codePointAt(i)
            endIndex = i + Character.charCount(cp)
            i = endIndex
            count++
        }
        return t.substring(0, endIndex) + "..."
    }

    fun ellipsisBoardNameLine(s: String?, max: Int = BOARD_NAME_MAX): String =
        ellipsisLine(s ?: "", max)

    private fun mapPostRowBase(
        postId: Any,
        globalSequenceNumber: Int,
        titleKo: String,
        titleJa: String,
        authorName: String,
        createdAt: Any?,
        views: Int,
        commentCount: Int,
    ): MappedPostTableRow =
        MappedPostTableRow(
            postId = postId,
            listNumber = globalSequenceNumber,
            titleKoLine = ellipsisLine(titleKo),
            titleJaLine = ellipsisLine(titleJa),
            authorName = authorName,
            createdAtLabel = formatPostDateListed(createdAt),
            views = views,
            commentCount = commentCount,
        )

    fun mapPostForTable(p: PostRowInput): MappedPostTableRow =
        mapPostRowBase(
            postId = p.postId,
            globalSequenceNumber = p.globalSequenceNumber,
            titleKo = p.titleKo,
            titleJa = p.titleJa,
            authorName = p.authorName,
            createdAt = p.createdAt,
            views = p.views,
            commentCount = p.commentCount,
        )

    fun mapAllPostsRow(p: AllPostsRowInput): MappedAllPostsTableRow {
        val base = mapPostRowBase(
            postId = p.postId,
            globalSequenceNumber = p.globalSequenceNumber,
            titleKo = p.titleKo,
            titleJa = p.titleJa,
            authorName = p.authorName,
            createdAt = p.createdAt,
            views = p.views,
            commentCount = p.commentCount,
        )
        return MappedAllPostsTableRow(
            postId = base.postId,
            listNumber = base.listNumber,
            titleKoLine = base.titleKoLine,
            titleJaLine = base.titleJaLine,
            authorName = base.authorName,
            createdAtLabel = base.createdAtLabel,
            views = base.views,
            commentCount = base.commentCount,
            boardId = p.boardId,
            boardMainHref = p.boardMainHref,
            boardKoLine = ellipsisBoardNameLine(p.boardNameKo),
            boardJaLine = ellipsisBoardNameLine(p.boardNameJa),
        )
    }

    /** 하단 페이지 안내: 전체/현재 페이지만 */
    fun formatPaginationSummary(totalPages: Int, currentPage: Int): String {
        val tp = maxOf(0, totalPages)
        val rawCp = currentPage.takeIf { it != 0 } ?: 1
        val cp = if (tp > 0) rawCp.coerceIn(1, tp) else 1
        return "전체 ${tp}페이지 · 현재 ${cp}페이지"
    }

    private fun parseToCalendar(input: Any?): Calendar? {
        when (input) {
            null -> return null
            is Date -> return Calendar.getInstance().apply { time = input }
            is Number ->
                return Calendar.getInstance().apply { timeInMillis = input.toLong() }
            is String -> {
                val s = input.trim()
                if (s.isEmpty()) return null
                val digitsOnly = s.all { it.isDigit() } ||
                    (s.startsWith('-') && s.length > 1 && s.drop(1).all { it.isDigit() })
                if (digitsOnly) {
                    return runCatching {
                        Calendar.getInstance().apply { timeInMillis = s.toLong() }
                    }.getOrNull()
                }
                for (pattern in ISO_PATTERNS) {
                    val cal = runCatching {
                        val sdf = SimpleDateFormat(pattern, Locale.US)
                        sdf.isLenient = false
                        val d = sdf.parse(s) ?: return@runCatching null
                        Calendar.getInstance().apply { time = d }
                    }.getOrNull()
                    if (cal != null) return cal
                }
                return null
            }
            else -> return null
        }
    }

    private val ISO_PATTERNS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
    )
}

data class PostRowInput(
    val postId: Any,
    val globalSequenceNumber: Int,
    val titleKo: String,
    val titleJa: String = "",
    val authorName: String,
    val createdAt: Any?,
    val views: Int,
    val commentCount: Int,
)

data class AllPostsRowInput(
    val postId: Any,
    val globalSequenceNumber: Int,
    val titleKo: String,
    val titleJa: String = "",
    val authorName: String,
    val createdAt: Any?,
    val views: Int,
    val commentCount: Int,
    val boardId: Any,
    val boardNameKo: String,
    val boardNameJa: String = "",
    val boardMainHref: String,
)

data class MappedPostTableRow(
    val postId: Any,
    val listNumber: Int,
    val titleKoLine: String,
    val titleJaLine: String,
    val authorName: String,
    val createdAtLabel: String,
    val views: Int,
    val commentCount: Int,
)

data class MappedAllPostsTableRow(
    val postId: Any,
    val listNumber: Int,
    val titleKoLine: String,
    val titleJaLine: String,
    val authorName: String,
    val createdAtLabel: String,
    val views: Int,
    val commentCount: Int,
    val boardId: Any,
    val boardMainHref: String,
    val boardKoLine: String,
    val boardJaLine: String,
)
