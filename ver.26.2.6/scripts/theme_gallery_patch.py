from pathlib import Path

path = Path(r"c:\Users\dongh\Documents\Android_App\ver.26.2.6\app\src\main\java\com\example\app_01\MainActivity.kt")
lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
g_start = next(i for i, l in enumerate(lines) if l.startswith("fun GalleryScreen("))
g_end = next(
    i for i, l in enumerate(lines[g_start + 1 :], g_start + 1) if l.startswith("fun MediaDetailScreen(")
)
inserted = False
for i in range(g_start, min(g_start + 35, len(lines))):
    if "val context = LocalContext.current" in lines[i]:
        nxt = lines[i + 1] if i + 1 < len(lines) else ""
        if "val palette = LocalAppUiPalette.current" not in nxt:
            lines.insert(i + 1, "    val palette = LocalAppUiPalette.current\n")
            inserted = True
            g_end += 1
        break
print("insert palette:", inserted, "range", g_start, g_end)
for i in range(g_start, g_end):
    line = lines[i]
    if line.strip().startswith("//"):
        continue
    line = line.replace(
        "BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)",
        "BorderStroke(1.dp, palette.divider",
    )
    line = line.replace(
        "Color.White.copy(alpha = 0.3f), RoundedCornerShape",
        "palette.divider, RoundedCornerShape",
    )
    line = line.replace("Color.White.copy(", "palette.onBackground.copy(")
    line = line.replace("tint = Color.White", "tint = palette.onBackground")
    line = line.replace("color = Color.White,", "color = palette.onBackground,")
    line = line.replace("color = Color.White)", "color = palette.onBackground)")
    line = line.replace("color = Color.White ", "color = palette.onBackground ")
    line = line.replace(".background(Color(0xFF2A2A2A))", ".background(palette.surfaceCard)")
    line = line.replace(".background(Color(0xFF2A2A2A),", ".background(palette.surfaceCard,")
    line = line.replace(
        ".background(Color(0xFF2F2F2F), RoundedCornerShape",
        ".background(palette.surfaceCard, RoundedCornerShape",
    )
    line = line.replace("Color(0xFF2F2F2F)", "palette.surfaceCard")
    lines[i] = line
for i in range(g_start, g_end):
    line = lines[i]
    if "Color.White" in line and "copy(" not in line:
        line = line.replace("Color.White", "palette.onBackground")
    lines[i] = line
path.write_text("".join(lines), encoding="utf-8")
print("done")
