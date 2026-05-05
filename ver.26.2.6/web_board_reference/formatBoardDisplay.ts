/**
 * 게시판 목록 표시용 유틸 (년·월·일·시·분, 제목 30자 생략, 페이지 문구)
 * 실제 프로젝트의 날짜 필드(ISO 문자열, Date, Unix ms)에 맞게 fromInput을 조정해 사용하세요.
 */

const TITLE_MAX = 30

/** 게시판 이름 한 줄 최대 글자(초과 시 …) — 메인·전체 게시물 목록의 게시판 열 */
export const BOARD_NAME_MAX = 10

/** 목록용: YYYY.MM.DD HH:mm */
export function formatPostDateListed(input: string | number | Date | null | undefined): string {
  if (input == null || input === '') return ''
  let d: Date
  if (input instanceof Date) {
    d = input
  } else if (typeof input === 'number') {
    d = new Date(input)
  } else {
    const s = String(input).trim()
    const parsed = new Date(s)
    d = Number.isNaN(parsed.getTime()) ? new Date(NaN) : parsed
  }
  if (Number.isNaN(d.getTime())) return ''
  const y = d.getFullYear()
  const mo = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${y}.${mo}.${day} ${h}:${mi}`
}

/** 한 줄 최대 글자 수 초과 시 말줄임 (유니코드 기준 Array.from 길이) */
export function ellipsisLine(s: string, max: number = TITLE_MAX): string {
  const t = (s ?? '').trim()
  if (t.length === 0) return ''
  const chars = Array.from(t)
  if (chars.length <= max) return t
  return chars.slice(0, max).join('') + '...'
}

/** 게시판명 한 줄(국문/일문 각각 최대 10자) */
export function ellipsisBoardNameLine(s: string, max: number = BOARD_NAME_MAX): string {
  return ellipsisLine(s ?? '', max)
}

export type PostRowInput = {
  /** 행 식별용 PK(목록 번호와 별개) */
  postId: number | string
  /**
   * 「번호」란에 표시할 값 — **전체 게시판** 기준 작성 순서(통합 일련번호).
   * 개별 게시판 내 순번이 아니라, 서버에서 전 posts 기준으로 계산한 값이어야 합니다.
   * (예: 작성일 오름차순으로 세면 가장 오래된 글이 1, 또는 정책에 맞게 내림차순 번호 부여)
   */
  globalSequenceNumber: number
  /** 국문 제목 (첫 줄) */
  titleKo: string
  /** 일본어 제목 (둘째 줄) — 없으면 빈 문자열 */
  titleJa?: string
  authorName: string
  /** ISO 8601 권장, 예: "2026-05-05T16:43:00+09:00" */
  createdAt: string | number | Date
  views: number
  commentCount: number
}

export function mapPostForTable(p: PostRowInput) {
  return {
    postId: p.postId,
    /** 목록의 번호 열 — 전체 게시판 통합 순번 */
    listNumber: p.globalSequenceNumber,
    titleKoLine: ellipsisLine(p.titleKo),
    titleJaLine: ellipsisLine(p.titleJa ?? ''),
    authorName: p.authorName,
    createdAtLabel: formatPostDateListed(p.createdAt),
    views: p.views,
    commentCount: p.commentCount,
  }
}

/** `/posts` 전체 게시물 목록 한 행 — 게시판 열(링크·10자 말줄임) 포함 */
export type AllPostsRowInput = PostRowInput & {
  boardId: string | number
  boardNameKo: string
  boardNameJa?: string
  /** 해당 게시판 메인 (예: `/boards/test-1`) */
  boardMainHref: string
}

export function mapAllPostsRow(p: AllPostsRowInput) {
  const base = mapPostForTable(p)
  return {
    ...base,
    boardId: p.boardId,
    boardMainHref: p.boardMainHref,
    boardKoLine: ellipsisBoardNameLine(p.boardNameKo),
    boardJaLine: ellipsisBoardNameLine(p.boardNameJa ?? ''),
  }
}

/** 하단 페이지 안내: 전체/현재 페이지만 (게시글 건수 문구 제외) */
export function formatPaginationSummary(totalPages: number, currentPage: number): string {
  const tp = Math.max(0, Math.trunc(Number(totalPages)) || 0)
  const rawCp = Math.trunc(Number(currentPage)) || 1
  const cp = tp > 0 ? Math.max(1, Math.min(rawCp, tp)) : 1
  return `전체 ${tp}페이지 · 현재 ${cp}페이지`
}
