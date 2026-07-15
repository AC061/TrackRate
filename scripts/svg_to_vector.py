import re
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable"


def extract_paths(svg_path: pathlib.Path):
    text = svg_path.read_text(encoding="utf-8")
    viewbox = re.search(r'viewBox="([^"]+)"', text).group(1).split()
    paths = []
    for match in re.finditer(r'<path[^>]*d="([^"]+)"[^>]*fill="([^"]+)"', text):
        paths.append((match.group(1), match.group(2)))
    return viewbox, paths


def write_vector(name: str):
    svg_path = ROOT / f"{name}.svg"
    viewbox, paths = extract_paths(svg_path)
    width = float(viewbox[2])
    height = float(viewbox[3])
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{width}dp"',
        f'    android:height="{height}dp"',
        f'    android:viewportWidth="{width}"',
        f'    android:viewportHeight="{height}">',
    ]
    for path_data, fill in paths:
        lines.extend(
            [
                "    <path",
                f'        android:fillColor="{fill}"',
                '        android:fillType="evenOdd"',
                f'        android:pathData="{path_data}" />',
            ]
        )
    lines.append("</vector>")
    output = DRAWABLE / f"{name}.xml"
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {output} ({len(paths)} paths, {output.stat().st_size} bytes)")


if __name__ == "__main__":
    for logo in ("logo_icon", "logo_full"):
        write_vector(logo)
