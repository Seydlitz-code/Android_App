/** 게시판 테이블(메인 대표 / 전체 게시물 / 단일 게시판) 공통 스타일 */
export const boardTableCommonCss = `
.board-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
.board-table th, .board-table td {
  border-bottom: 1px solid #ddd;
  padding: 10px 8px;
  text-align: left;
  vertical-align: top;
  word-break: break-word;
}
.board-table th { font-weight: 700; background: #f6f3ea; }
.col-no { width: 64px; }
.col-board { width: 140px; }
.col-title { min-width: 0; }
.col-author { width: 100px; }
.col-date { width: 130px; white-space: nowrap; }
.col-views { width: 72px; text-align: center; }
.col-comments { width: 64px; text-align: center; }
.cell-title .title-ko, .cell-board .board-ko { font-weight: 600; line-height: 1.35; }
.cell-title .title-ja, .cell-board .board-ja { font-size: 0.92em; color: #444; margin-top: 4px; line-height: 1.35; }
.cell-board a { color: inherit; text-decoration: none; }
.cell-board a:hover { text-decoration: underline; }
.board-pagination { margin-top: 16px; }
.pagination-summary { margin-bottom: 8px; }
.page-btn { margin-right: 6px; padding: 6px 10px; cursor: pointer; border: 1px solid #ccc; background: #fff; }
.page-btn.active { background: #111; color: #fff; border-color: #111; }
.home-board-spotlight { margin-bottom: 32px; }
.home-board-spotlight .board-hub-title { font-size: 1.1rem; font-weight: 700; margin: 0 0 4px 0; }
.home-board-spotlight .board-hub-title a { color: #111; text-decoration: none; }
.home-board-spotlight .board-hub-title a:hover { text-decoration: underline; }
.home-board-spotlight .board-hub-title-ja { font-size: 0.95rem; color: #333; margin: 0 0 4px 0; }
.home-board-spotlight .board-hub-sub { font-size: 0.85rem; color: #666; margin: 0 0 12px 0; }
`
