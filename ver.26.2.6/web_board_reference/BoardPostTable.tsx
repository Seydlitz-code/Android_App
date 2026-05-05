/**
 * 단일 게시판 목록 페이지 — 이미지 3 하단과 동일 열(게시판 열 없음).
 * 전체 게시물과 동일 스타일은 `boardTableCommonCss` + `AllPostsBoardTable` 참고.
 */

import React from 'react'
import { boardTableCommonCss } from './boardTableCommonCss'
import { formatPaginationSummary, mapPostForTable, type PostRowInput } from './formatBoardDisplay'

export type BoardPostTableProps = {
  posts: PostRowInput[]
  totalPages: number
  currentPage: number
  onPageChange?: (page: number) => void
}

export function BoardPostTable({ posts, totalPages, currentPage, onPageChange }: BoardPostTableProps) {
  return (
    <div className="board-wrap">
      <table className="board-table">
        <thead>
          <tr>
            <th className="col-no">게시물 번호</th>
            <th className="col-title">게시물 제목</th>
            <th className="col-author">작성자</th>
            <th className="col-date">작성 시간</th>
            <th className="col-views">조회수</th>
            <th className="col-comments">댓글수</th>
          </tr>
        </thead>
        <tbody>
          {posts.map((raw) => {
            const p = mapPostForTable(raw)
            return (
              <tr key={String(p.postId)}>
                <td>{p.listNumber}</td>
                <td className="cell-title">
                  <div className="title-ko">{p.titleKoLine}</div>
                  {p.titleJaLine.length > 0 && <div className="title-ja">{p.titleJaLine}</div>}
                </td>
                <td>{p.authorName}</td>
                <td>{p.createdAtLabel}</td>
                <td>{p.views}</td>
                <td>{p.commentCount}</td>
              </tr>
            )
          })}
        </tbody>
      </table>

      <footer className="board-pagination">
        <p className="pagination-summary">{formatPaginationSummary(totalPages, currentPage)}</p>
        <nav className="page-numbers" aria-label="페이지">
          {Array.from({ length: Math.max(1, totalPages) }, (_, i) => i + 1).map((page) => (
            <button
              key={page}
              type="button"
              className={page === currentPage ? 'page-btn active' : 'page-btn'}
              onClick={() => onPageChange?.(page)}
            >
              {page}
            </button>
          ))}
        </nav>
      </footer>

      <style>{boardTableCommonCss}</style>
    </div>
  )
}
