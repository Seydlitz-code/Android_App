/**
 * 메인 페이지 하단 — 게시판별 대표 게시물 구역 (이미지 3 하단「특정 게시판」 형식).
 * - 섹션 제목(국·일)을 클릭하면 해당 게시판 메인으로 이동.
 * - 표는 게시판 열 없음: 게시물 번호 | 제목 | 작성자 | 작성 시간 | 조회수 | 댓글수
 */

import React from 'react'
import { boardTableCommonCss } from './boardTableCommonCss'
import { mapPostForTable, type PostRowInput } from './formatBoardDisplay'

export type MainBoardSpotlightSection = {
  boardId: string | number
  /** 허브 제목(긴 프로젝트명 등) — 말줄임 없이 전체 표시 가능 */
  boardTitleKo: string
  boardTitleJa?: string
  boardSubtitle?: string
  /** 게시판 메인 URL */
  boardMainHref: string
  representativePosts: PostRowInput[]
}

export function MainBoardSpotlightSections({ sections }: { sections: MainBoardSpotlightSection[] }) {
  return (
    <div className="home-board-spotlights">
      {sections.map((sec) => (
        <section key={String(sec.boardId)} className="home-board-spotlight">
          <h3 className="board-hub-title">
            <a href={sec.boardMainHref}>{sec.boardTitleKo}</a>
          </h3>
          {sec.boardTitleJa != null && sec.boardTitleJa !== '' && (
            <p className="board-hub-title-ja">{sec.boardTitleJa}</p>
          )}
          {sec.boardSubtitle != null && sec.boardSubtitle !== '' && (
            <p className="board-hub-sub">{sec.boardSubtitle}</p>
          )}

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
              {sec.representativePosts.map((raw) => {
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
        </section>
      ))}
      <style>{boardTableCommonCss}</style>
    </div>
  )
}
