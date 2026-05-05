/** 게시물 상세(조회) 페이지 — 레이아웃·타이포(이미지 2 수정안) */
export const postDetailPageCss = `
.post-detail-page { max-width: 900px; margin: 0 auto; padding: 0 20px 48px; }
.post-detail-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 8px rgba(0,0,0,0.06);
  padding: 24px 28px 32px;
}
.post-detail-breadcrumb {
  font-size: 0.8rem;
  color: #888;
  margin-bottom: 16px;
}
.post-detail-breadcrumb a { color: #666; text-decoration: none; }
.post-detail-breadcrumb a:hover { text-decoration: underline; }

/* 게시판 이름 블록 (상단) */
.post-detail-board-name-ko,
.post-detail-board-name-ja {
  font-size: 1.75rem;
  font-weight: 700;
  color: #111;
  line-height: 1.35;
  margin: 0;
}
.post-detail-board-name-ja { margin-top: 6px; }
.post-detail-divider {
  border: none;
  border-top: 1px solid #e0dcd4;
  margin: 20px 0 18px;
}

/* 게시물 제목: 국문·일문 동일 타이포 (크기·폰트·색 동일) — 게시판명과 동일 rem으로 통일 가능 */
.post-detail-title-ko,
.post-detail-title-ja {
  font-size: 1.75rem;
  font-weight: 700;
  color: #111;
  line-height: 1.35;
  margin: 0;
  font-family: inherit;
}
.post-detail-title-ja { margin-top: 8px; }

.post-detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 20px 0 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}
.post-detail-meta-left {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 16px;
  color: #555;
  font-size: 0.9rem;
}
.post-detail-meta-right {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}
.post-detail-views { color: #555; font-size: 0.9rem; }
/* 댓글 수 버튼: 일반 댓글 개수 텍스트와 동일 색 */
.post-detail-comment-jump {
  background: none;
  border: none;
  padding: 4px 8px;
  cursor: pointer;
  font-size: 0.9rem;
  color: #555;
  border-radius: 6px;
}
.post-detail-comment-jump:hover { background: rgba(0,0,0,0.05); }

.post-detail-body { color: #222; line-height: 1.7; font-size: 1rem; }
.post-detail-body img { max-width: 100%; height: auto; }

.post-detail-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 28px;
  justify-content: flex-end;
}
/* 수정·삭제·글쓰기 — 정사각형 버튼 */
.post-detail-actions .btn-square {
  width: 44px;
  height: 44px;
  min-width: 44px;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.75rem;
  font-weight: 700;
  box-sizing: border-box;
  padding: 0;
}
.post-detail-actions .btn-edit {
  border: 1px solid #111;
  background: #fff;
  color: #111;
}
.post-detail-actions .btn-delete {
  border: 1px solid #c62828;
  background: #fff;
  color: #c62828;
}
.post-detail-actions .btn-write {
  width: auto;
  min-width: 44px;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #111;
  background: #fff;
  color: #111;
  white-space: nowrap;
}

#post-comments-region { scroll-margin-top: 88px; }

/* 상단 사이트 헤더 — 타 페이지와 동일: 좌측 타이틀 / 우측 프로필·버튼 정렬 */
.portfolio-site-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px 20px;
  background: #f3efe8;
  border-bottom: 1px solid #e5e0d8;
}
.portfolio-site-header .header-brand {
  font-size: 0.95rem;
  font-weight: 600;
  color: #222;
}
.portfolio-site-header .header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.portfolio-site-header .header-actions button,
.portfolio-site-header .header-actions a {
  font-size: 0.85rem;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #ccc;
  background: #fff;
  cursor: pointer;
  text-decoration: none;
  color: #222;
}

/* 글쓰기 가이드 모달 */
.writing-guide-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.writing-guide-modal {
  background: #fff;
  border-radius: 12px;
  max-width: 520px;
  width: 100%;
  max-height: 80vh;
  overflow: auto;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
}
.writing-guide-modal h2 { margin: 0 0 12px; font-size: 1.2rem; }
.writing-guide-modal .guide-close {
  margin-top: 20px;
  padding: 10px 18px;
  border-radius: 8px;
  border: 1px solid #111;
  background: #111;
  color: #fff;
  cursor: pointer;
}
`
