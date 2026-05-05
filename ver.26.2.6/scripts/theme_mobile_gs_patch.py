from pathlib import Path

path = Path(r"c:\Users\dongh\Documents\Android_App\ver.26.2.6\app\src\main\java\com\example\app_01\MainActivity.kt")
lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
start = next(i for i, l in enumerate(lines) if l.startswith("private fun Mobile3dGsScreen()"))
end = next(i for i, l in enumerate(lines[start + 1 :], start + 1) if l.startswith("fun ProfileScreen("))
for i in range(start, min(start + 12, len(lines))):
    if "val context = LocalContext.current" in lines[i]:
        nxt = lines[i + 1] if i + 1 < len(lines) else ""
        if "val palette = LocalAppUiPalette.current" not in nxt:
            lines.insert(i + 1, "    val palette = LocalAppUiPalette.current\n")
            end += 1
        break
for i in range(start, end):
    line = lines[i]
    line = line.replace("AppBackgroundColor", "palette.background")
    line = line.replace("Color.White.copy(", "palette.onBackground.copy(")
    line = line.replace("Color(0xFF2A2A2A)", "palette.surfaceCard")
    line = line.replace("Color(0xFF3A3A3A)", "palette.chatComposerPillInactive")
    line = line.replace("Color(0xFF1E1E1E)", "palette.dialogSurface")
    line = line.replace("Color(0xFF9CD83B)", "palette.brand")
    line = line.replace("trackColor = Color(0xFF3A3A3A)", "trackColor = palette.progressTrack")
    line = line.replace("Color(0xFF3D5220)", "palette.mobileGsCtaEnabledBg")
    if "Color.White" in line and "copy(" not in line:
        line = line.replace("Color.White", "palette.onBackground")
    lines[i] = line
path.write_text("".join(lines), encoding="utf-8")
print("mobile3dgs", start, end)
