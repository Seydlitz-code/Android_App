#!/usr/bin/env python3
"""Remove decorative/redundant // comments; preserve non-obvious technical notes."""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "java" / "com" / "example" / "app_01"

DIVIDER = re.compile(r"^// [─=]{3,}")
REGION = re.compile(r"^// #(?:region|endregion)\b")
EQUALS_BANNER = re.compile(r"^// =+$")

KEEP_KEYWORDS = (
    "OOM",
    "oom",
    "메모리",
    "parseBody",
    "code_cache",
    "imePadding",
    "BottomNav",
    "/status",
    "callback",
    "NanoHTTPD",
    "ONNX",
    "JIT",
    "SSL",
    "multipart",
    "http://",
    "https://",
    "임계",
    "타임아웃",
    "timeout",
    "스트리밍",
    "직렬",
    "스레드",
    "메인 스레드",
    "IO ",
    "Filament",
    "OpenGL",
    "EXIF",
    "Coil",
    "ART ",
    "WebView",
    "서버",
    "pipeline",
    "ZIP ",
    "poses.json",
    "agentDebug",
    "OutOfMemory",
    "크래시",
    "불안정",
    "레거시",
    "deprecated",
    "보안",
    "cleartext",
    "Hugging",
    "ImageNet",
    "BFS",
    "ByteBuffer",
    "float32",
    "uint8",
    "워커",
    "폴링",
    "yield",
)

OBVIOUS_ONLY = re.compile(
    r"^//\s*(?:"
    r"구분선|"
    r"동영상 재생|"
    r"헤더 파싱|"
    r"상태 아이콘|"
    r"원본 크기 확인|"
    r"회전 행렬 적용|"
    r"시스템 알림|"
    r"코드 블록|"
    r"Bold:|"
    r"Italic:|"
    r"Link:|"
    r"List:|"
    r"Paragraph|"
    r"Header|"
    r"wait let me redo"
    r")\s*$",
    re.IGNORECASE,
)

SECTION_TITLE = re.compile(r"^// [─=]{2,}\s*(.+?)\s*[─=]{2,}$|^// ──\s*(.+?)\s*──")


def is_divider(line: str) -> bool:
    s = line.strip()
    return bool(DIVIDER.match(s) or EQUALS_BANNER.match(s))


def should_keep_comment(text: str) -> bool:
    body = text.strip()
    if not body.startswith("//"):
        return True
    content = body[2:].strip()
    if not content:
        return False
    if OBVIOUS_ONLY.match(body):
        return False
    if any(k in content for k in KEEP_KEYWORDS):
        return True
    # Short UI/icon labels restating the next line
    if len(content) < 40 and any(
        w in content
        for w in (
            "아이콘",
            "버튼",
            "다이얼로그",
            "오버레이",
            "표시",
            "토글",
            "스크롤",
            "그리드",
            "미리보기",
            "편집 모드",
            "옵션 바",
            "필름스트립",
            "상단 바",
            "하단",
        )
    ):
        return False
    # Map category fold labels (single short phrase)
    if content in {
            "사람",
            "전자기기",
            "용기 / 식기",
            "가구",
            "동물",
            "차량",
            "가방 / 악세서리",
            "스포츠 / 기타 도구",
            "음식",
            "식물",
            "주방가전",
            "기타",
        }:
        return False
    # Decorative section title sandwiched by dividers — handled in block pass
    return len(content) > 55 or content.count(".") >= 1


def clean_lines(lines: list[str]) -> list[str]:
    out: list[str] = []
    i = 0
    n = len(lines)
    while i < n:
        line = lines[i]
        stripped = line.strip()

        if REGION.match(stripped):
            i += 1
            continue

        if EQUALS_BANNER.match(stripped):
            i += 1
            continue

        # Triple-block: divider / title / divider
        if is_divider(stripped) and i + 2 < n:
            mid = lines[i + 1].strip()
            end = lines[i + 2].strip()
            if mid.startswith("//") and is_divider(end):
                title = mid[2:].strip()
                if not should_keep_comment(mid):
                    i += 3
                    continue

        # Pair-block: divider line + title on same style
        if is_divider(stripped):
            i += 1
            continue

        if stripped.startswith("//"):
            if "wait let me redo" in stripped:
                i += 1
                continue
            if not should_keep_comment(stripped):
                i += 1
                continue

        out.append(line)
        i += 1

    # Collapse excessive blank lines (max 2 consecutive)
    compact: list[str] = []
    blank_run = 0
    for line in out:
        if line.strip() == "":
            blank_run += 1
            if blank_run <= 2:
                compact.append(line)
        else:
            blank_run = 0
            compact.append(line)
    return compact


def main() -> None:
    changed_files: list[tuple[str, int]] = []
    for path in sorted(ROOT.rglob("*.kt")):
        original = path.read_text(encoding="utf-8")
        lines = original.splitlines(keepends=True)
        cleaned = clean_lines(lines)
        new_text = "".join(cleaned)
        if new_text != original:
            delta = len(lines) - len(cleaned)
            rel = path.relative_to(ROOT)
            changed_files.append((str(rel), delta))
            path.write_text(new_text, encoding="utf-8", newline="\n")

    print(f"Updated {len(changed_files)} files")
    for name, delta in sorted(changed_files, key=lambda x: -x[1])[:25]:
        print(f"  -{delta:4d} lines  {name}")


if __name__ == "__main__":
    main()
