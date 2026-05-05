/**
 * 게시물 상세(조회) 페이지 참고 구현 — 이미지 2 수정안
 * - 국·일 게시물 제목: 동일 font-size / font-weight / color
 * - 메타: 좌측 닉네임·작성(수정)시각, 우측 조회수·댓글 수 버튼(클릭 시 댓글 영역으로 스크롤)
 * - 수정·삭제: 정사각형 버튼 / 글쓰기: 클릭 시 해당 게시판 글쓰기 가이드 모달
 * - 상단바: 좌 브랜드 / 우 액션 정렬 (타 페이지 공통 헤더와 동일 패턴)
 */

import React, { useCallback, useState } from 'react'
import { postDetailPageCss } from './postDetailPageCss'

export type PortfolioSiteHeaderProps = {
  brandText?: string
  userNickname?: string
  myPageHref?: string
  onLogout?: () => void
}

/** 다른 페이지와 동일한 상단 정렬: 좌측 브랜드 · 우측 프로필/버튼 */
export function PortfolioSiteHeader({
  brandText = 'Donghawan Lee / @lilip',
  userNickname = '릴리프',
  myPageHref = '/mypage',
  onLogout,
}: PortfolioSiteHeaderProps) {
  return (
    <header className="portfolio-site-header">
      <span className="header-brand">{brandText}</span>
      <div className="header-actions">
        <span>{userNickname}</span>
        <a href={myPageHref}>마이페이지</a>
        {onLogout != null ? (
          <button type="button" onClick={onLogout}>
            로그아웃
          </button>
        ) : (
          <a href="/logout">로그아웃</a>
        )}
      </div>
    </header>
  )
}

export type PostDetailPageProps = {
  /** 브레드크럼 등 */
  breadcrumbLabel?: string
  allPostsHref?: string
  boardNameKo: string
  boardNameJa?: string
  boardHref?: string
  /** 게시물 제목 — 국·일 동일 스타일(.post-detail-title-ko / -ja) */
  titleKo: string
  titleJa?: string
  authorNickname: string
  createdAtLabel: string
  updatedAtLabel?: string
  hasBeenEdited?: boolean
  viewCount: number
  commentCount: number
  contentHtml?: string
  children?: React.ReactNode
  editHref: string
  newPostInBoardHref: string
  onDelete?: () => void
  writingGuideTitle?: string
  writingGuideContent?: React.ReactNode
}

export function PostDetailPage({
  breadcrumbLabel = '전체 게시물',
  allPostsHref = '/posts',
  boardNameKo,
  boardNameJa,
  boardHref = '#',
  titleKo,
  titleJa,
  authorNickname,
  createdAtLabel,
  updatedAtLabel,
  hasBeenEdited = false,
  viewCount,
  commentCount,
  contentHtml,
  children,
  editHref,
  newPostInBoardHref: _newPostInBoardHref,
  onDelete,
  writingGuideTitle = '글쓰기 안내',
  writingGuideContent,
}: PostDetailPageProps) {
  void _newPostInBoardHref
  const [guideOpen, setGuideOpen] = useState(false)

  const scrollToComments = useCallback(() => {
    document.getElementById('post-comments-region')?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    })
  }, [])

  const defaultGuide = (
    <ul style={{ margin: 0, paddingLeft: '1.2rem', lineHeight: 1.7 }}>
      <li>이 게시판 주제에 맞는 내용만 작성해 주세요.</li>
      <li>국문·일문 제목을 모두 입력하면 목록에서 두 줄로 표시됩니다.</li>
      <li>이미지·동영상은 용량 제한을 확인해 주세요.</li>
    </ul>
  )

  return (
    <>
      <article className="post-detail-page">
        <div className="post-detail-card">
          <nav className="post-detail-breadcrumb" aria-label="breadcrumb">
            <a href={allPostsHref}>{breadcrumbLabel}</a>
            {' / '}
            <a href={boardHref}>{boardNameKo}</a>
          </nav>

          <p className="post-detail-board-name-ko">{boardNameKo}</p>
          {boardNameJa != null && boardNameJa !== '' && (
            <p className="post-detail-board-name-ja">{boardNameJa}</p>
          )}

          <hr className="post-detail-divider" />

          <h1 className="post-detail-title-ko">{titleKo}</h1>
          {titleJa != null && titleJa !== '' && <h1 className="post-detail-title-ja">{titleJa}</h1>}

          <div className="post-detail-meta">
            <div className="post-detail-meta-left">
              <span>{authorNickname}</span>
              <span>
                {hasBeenEdited === true && updatedAtLabel != null && updatedAtLabel !== ''
                  ? `${updatedAtLabel} (수정됨)`
                  : createdAtLabel}
              </span>
            </div>
            <div className="post-detail-meta-right">
              <span className="post-detail-views">조회수 {viewCount}</span>
              <button type="button" className="post-detail-comment-jump" onClick={scrollToComments}>
                댓글 {commentCount}
              </button>
            </div>
          </div>

          <div
            className="post-detail-body"
            {...(contentHtml != null
              ? { dangerouslySetInnerHTML: { __html: contentHtml } }
              : { children })}
          />

          <div className="post-detail-actions">
            <a className="btn-square btn-edit" href={editHref} title="수정">
              수정
            </a>
            {onDelete != null ? (
              <button type="button" className="btn-square btn-delete" onClick={onDelete} title="삭제">
                삭제
              </button>
            ) : null}
            <button
              type="button"
              className="btn-write"
              onClick={() => setGuideOpen(true)}
              title="이 게시판에서 새 글 작성"
            >
              글쓰기
            </button>
          </div>
        </div>

        <section id="post-comments-region">{/* 댓글 UI 삽입 */}</section>
      </article>

      {guideOpen ? (
        <div
          className="writing-guide-overlay"
          role="dialog"
          aria-modal="true"
          aria-labelledby="writing-guide-heading"
          onClick={() => setGuideOpen(false)}
        >
          <div className="writing-guide-modal" onClick={(e) => e.stopPropagation()}>
            <h2 id="writing-guide-heading">{writingGuideTitle}</h2>
            <div>{writingGuideContent ?? defaultGuide}</div>
            <button type="button" className="guide-close" onClick={() => setGuideOpen(false)}>
              닫기
            </button>
          </div>
        </div>
      ) : null}

      <style>{postDetailPageCss}</style>
    </>
  )
}

export function PostDetailLayoutWithHeader(
  props: PostDetailPageProps & { header?: React.ReactNode }
) {
  const { header, ...rest } = props
  return (
    <>
      {header ?? <PortfolioSiteHeader />}
      <PostDetailPage {...rest} />
    </>
  )
}
