/**
 * 게시물 수정(/posts/:id/edit) 화면에서 국·일 텍스트가 `ï¿½`, `ë²ˆì—` 처럼 깨질 때
 * ---------------------------------------------------------------------------
 * 원인: UTF-8 바이트를 Latin-1(ISO-8859-1) 등으로 잘못 해석했거나, DB 연결·HTTP
 * 헤더·템플릿 인코딩이 UTF-8로 통일되지 않은 경우가 대부분입니다.
 *
 * 아래를 실제 스택(Rails / Next.js / Node+Express / PHP 등)에 맞게 순서대로 점검하세요.
 *
 * ── 1) HTTP 응답 ──
 * - HTML: `<meta charset="utf-8">` + 서버 `Content-Type: text/html; charset=utf-8`
 * - JSON API: `Content-Type: application/json; charset=utf-8`
 *
 * ── 2) DB 연결 (가장 흔함) ──
 * - MySQL/MariaDB: DSN 또는 연결 옵션에 `charset=utf8mb4` (또는 `utf8mb4_unicode_ci`)
 *   예: mysql2 (Node): `createPool({ ..., charset: 'utf8mb4' })`
 * - PostgreSQL: DB 자체는 UTF-8; 클라이언트에서 바이너리/Buffer를 latin1로 읽지 않도록 주의.
 *
 * ── 3) ORM / 쿼리 ──
 * - Prisma: schema datasource URL에 `?schema=public` 외에 DB가 UTF8인지 확인.
 *   Railway Postgres는 UTF-8 기본. 문제는 주로 “연결 후 클라이언트 인코딩”이 아니라
 *   이미 잘못 저장된 데이터이거나, JSON 직렬화 단계에서의 잘못된 디코딩입니다.
 *
 * ── 4) 이미 잘못 저장된 경우(더블 인코딩) ──
 * - 한 번 UTF-8 문자열을 Latin-1로 디코딩해 다시 UTF-8로 저장하면 복구가 어렵습니다.
 * - 백업에서 복원하거나, 마이그레이션 스크립트로 역변환(위험)이 필요할 수 있습니다.
 *
 * ── 5) Next.js (App Router) 편집 폼 ──
 * - `fetch`로 받은 본문을 `Buffer.from(x, 'latin1')` 등으로 건드리지 않았는지 확인.
 * - 서버 컴포넌트에서 DB row를 그대로 `defaultValue={post.title_ko}` 로 넘기는지 확인.
 * - `encodeURIComponent` 이중 적용 여부.
 *
 * ── 6) Rails ──
 * - `config.encoding = "utf-8"` , database.yml `encoding: utf8mb4`
 * - `force_encoding('UTF-8')` 를 잘못된 시점에 호출하지 않았는지 확인.
 *
 * ── 7) PHP ──
 * - PDO: `charset=utf8mb4` in DSN, `header('Content-Type: text/html; charset=UTF-8');`
 *
 * 수정 화면에만 깨지고 목록은 정상이면: “편집용 API/직렬화” 경로만 다르게 타는지
 * (예: JSON 파싱 전에 `iconv` 적용, 잘못된 middleware) 비교해 보세요.
 */

/** 에디터/레이아웃에 붙일 메타(참고) */
export const HTML_UTF8_META = '<meta charset="utf-8" />'
