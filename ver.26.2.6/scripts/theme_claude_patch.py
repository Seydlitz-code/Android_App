from pathlib import Path

path = Path(r"c:\Users\dongh\Documents\Android_App\ver.26.2.6\app\src\main\java\com\example\app_01\MainActivity.kt")
lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
start = next(i for i, l in enumerate(lines) if l.startswith("fun ClaudeChatScreen("))
end = next(
    i for i, l in enumerate(lines[start + 1 :], start + 1) if l.startswith("private fun ClaudeImageSelectDialog(")
)
# insert palette after val context
for i in range(start, min(start + 15, len(lines))):
    if "val context = LocalContext.current" in lines[i]:
        nxt = lines[i + 1] if i + 1 < len(lines) else ""
        if "val palette = LocalAppUiPalette.current" not in nxt:
            lines.insert(i + 1, "    val palette = LocalAppUiPalette.current\n")
            end += 1
        break
for i in range(start, end):
    line = lines[i]
    line = line.replace("AppBackgroundColor", "palette.background")
    line = line.replace("SolidColor(Color.White)", "SolidColor(palette.onBackground)")
    line = line.replace("Color.White.copy(", "palette.onBackground.copy(")
    line = line.replace("tint = Color.White", "tint = palette.onBackground")
    line = line.replace("color = Color.White,", "color = palette.onBackground,")
    line = line.replace("color = Color.White)", "color = palette.onBackground)")
    line = line.replace("color = Color.White ", "color = palette.onBackground ")
    line = line.replace(
        "modifier = Modifier.background(Color(0xFF2A2A2A))",
        "modifier = Modifier.background(palette.dropdownMenuBg)",
    )
    line = line.replace("Color(0xFF2A2A2A)", "palette.chatComposerPill")
    line = line.replace("Color(0xFF111111)", "palette.chatInputBarBg")
    line = line.replace("Color(0xFF9CD83B)", "palette.brand")
    line = line.replace("Color(0xFF3A3A3A)", "palette.chatComposerPillInactive")
    line = line.replace("Color(0xFF888888)", "palette.placeholder")
    line = line.replace("containerColor = Color(0xFF252525)", "containerColor = palette.dialogSurface")
    line = line.replace("focusedBorderColor = Color(0xFF9CD83B)", "focusedBorderColor = palette.brand")
    line = line.replace("cursorColor = Color(0xFF9CD83B)", "cursorColor = palette.brand")
    if "Color.White" in line and "copy(" not in line and "SolidColor" not in line:
        line = line.replace("Color.White", "palette.onBackground")
    lines[i] = line
path.write_text("".join(lines), encoding="utf-8")
print("claude patched", start, end)
