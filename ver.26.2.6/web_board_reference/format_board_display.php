<?php
/**
 * 게시판 목록 표시용 (PHP) — 년·월·일·시·분, 제목 30자 생략(mbstring), 페이지 문구
 * 기존 게시판 스크립트에 함수만 복사해 사용하세요.
 *
 * 「번호」표시: 각 게시판 내부 순번이 아니라, **전체 게시판 통합** 작성 순서 번호를
 * DB/API에서 계산해 내려주세요. (아래 SQL은 예시 — 테이블·컬럼명은 프로젝트에 맞게 수정)
 *
 * 예) 작성 시각 오름차순으로 전역 번호 부여 (가장 오래된 글이 1):
 *   SELECT p.*,
 *     ROW_NUMBER() OVER (ORDER BY p.created_at ASC) AS global_sequence_number
 *   FROM posts p
 *
 * 목록에서 특정 게시판만 필터할 때도, 번호는 위 서브쿼리/뷰에서 구한 global_sequence_number를 SELECT해 표시합니다.
 */

define('BOARD_TITLE_MAX', 30);
/** 게시판 이름 한 줄(국·일 각각) 최대 글자 */
define('BOARD_NAME_MAX', 10);

/**
 * @param string|int|null $datetime DB datetime 문자열 또는 Unix timestamp
 * @return string 예: 2026.05.05 16:43
 */
function board_format_post_date_listed($datetime): string
{
    if ($datetime === null || $datetime === '') {
        return '';
    }
    $ts = is_numeric($datetime) ? (int) $datetime : strtotime((string) $datetime);
    if ($ts === false) {
        return '';
    }
    return date('Y.m.d H:i', $ts);
}

/**
 * 한 줄 최대 글자(유니코드 코드 포인트) 초과 시 말줄임
 */
function board_ellipsis_line(?string $s, int $max = BOARD_TITLE_MAX): string
{
    if ($s === null) {
        return '';
    }
    $t = trim($s);
    if ($t === '') {
        return '';
    }
    if (!function_exists('mb_strlen')) {
        return strlen($t) <= $max ? $t : substr($t, 0, $max) . '...';
    }
    if (mb_strlen($t, 'UTF-8') <= $max) {
        return $t;
    }
    return mb_substr($t, 0, $max, 'UTF-8') . '...';
}

/** 게시판명 표시용 (10자 말줄임) — 링크 텍스트에 사용 */
function board_ellipsis_board_name_line(?string $s, int $max = BOARD_NAME_MAX): string
{
    return board_ellipsis_line($s, $max);
}

/**
 * 하단: 전체 N페이지 · 현재 M페이지 (게시글 건수 없음)
 */
function board_format_pagination_summary(int $totalPages, int $currentPage): string
{
    $tp = max(0, $totalPages);
    $cp = max(1, min($currentPage, $tp > 0 ? $tp : 1));
    return "전체 {$tp}페이지 · 현재 {$cp}페이지";
}
