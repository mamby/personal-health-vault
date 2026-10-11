"""Offline SVG conversion contracts; shared by Kit and its public hosts."""

import hashlib
import json
from pathlib import Path
import re
import sys
from decimal import Decimal
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
TOKEN = re.compile(r"[A-Za-z]|[-+]?(?:\d*\.\d+|\d+\.?\d*)(?:[eE][-+]?\d+)?")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest(text):
    return hashlib.sha256(text.replace("\r\n", "\n").encode()).hexdigest()


def path_tokens(data):
    require(not TOKEN.sub("", data).strip(" ,\t\n\r"), "Unsupported path syntax")
    return tuple(token if token.isalpha() else Decimal(token) for token in TOKEN.findall(data))


def svg_paths(svg):
    """Represent each SVG element separately, without changing path commands."""
    paths = []
    for element in svg:
        tag = element.tag.rsplit("}", 1)[-1]
        require(not list(element), f"Unsupported SVG element hierarchy: {tag}")
        require(not (set(element.attrib) - {
            "d", "cx", "cy", "r", "x", "y", "width", "height", "rx", "ry",
            "x1", "y1", "x2", "y2", "points",
        }), f"Unsupported SVG element attributes: {element.attrib}")
        values = {key: Decimal(value) for key, value in element.attrib.items()
                  if key not in {"d", "points"}}
        if tag == "path":
            data = element.attrib["d"]
        elif tag == "circle":
            x, y, radius = (values[key] for key in ("cx", "cy", "r"))
            data = f"M{x+radius} {y}A{radius} {radius} 0 1 1 {x-radius} {y}A{radius} {radius} 0 1 1 {x+radius} {y}Z"
        elif tag == "rect":
            x, y = values.get("x", Decimal(0)), values.get("y", Decimal(0))
            width, height = values["width"], values["height"]
            rx = values.get("rx", values.get("ry", Decimal(0)))
            ry = values.get("ry", rx)
            rx, ry = min(rx, width / 2), min(ry, height / 2)
            if rx and ry:
                data = (f"M{x+rx} {y}H{x+width-rx}A{rx} {ry} 0 0 1 {x+width} {y+ry}"
                        f"V{y+height-ry}A{rx} {ry} 0 0 1 {x+width-rx} {y+height}"
                        f"H{x+rx}A{rx} {ry} 0 0 1 {x} {y+height-ry}"
                        f"V{y+ry}A{rx} {ry} 0 0 1 {x+rx} {y}Z")
            else:
                data = f"M{x} {y}H{x+width}V{y+height}H{x}Z"
        elif tag == "line":
            data = f"M{values['x1']} {values['y1']}L{values['x2']} {values['y2']}"
        elif tag in {"polyline", "polygon"}:
            points = re.split(r"[ ,]+", element.attrib["points"].strip())
            require(len(points) % 2 == 0, "Unpaired SVG point")
            data = "M" + " L".join(" ".join(points[i:i+2]) for i in range(0, len(points), 2))
            if tag == "polygon":
                data += "Z"
        else:
            raise ValueError(f"Unsupported SVG element: {tag}; review conversion explicitly")
        paths.append(path_tokens(data))
    require(paths, "Empty source artwork")
    return paths


def read_source(base, source):
    source_file = base / source["file"]
    raw = source_file.read_text(encoding="utf-8")
    require(digest(raw) == source["sha256"], f"Pinned SVG changed: {source_file}")
    require(re.fullmatch(r"[0-9a-f]{40}", source["revision"]), "Source revision must be a commit SHA")
    require(source["revision"] in source["url"], "Source URL must contain the pinned revision")
    svg = ET.fromstring(raw)
    require(svg.attrib.get("viewBox") == "0 0 24 24", "Unsupported source viewport")
    require(svg.attrib.get("fill") == "none", "Unsupported source fill")
    require(svg.attrib.get("stroke") == "currentColor", "Unsupported source stroke")
    require(svg.attrib.get("stroke-width") == "2", "Unsupported source stroke width")
    require(svg.attrib.get("stroke-linecap") == "round", "Unsupported source caps")
    require(svg.attrib.get("stroke-linejoin") == "round", "Unsupported source joins")
    require(set(svg.attrib) <= {"width", "height", "viewBox", "fill", "stroke",
                               "stroke-width", "stroke-linecap", "stroke-linejoin"},
            "Unsupported SVG root attributes")
    return svg_paths(svg)


def verify_vector(text, expected):
    vector = ET.fromstring(text)
    require(vector.tag == "vector", "Expected Android vector")
    require(set(vector.attrib) <= {ANDROID + name for name in (
        "width", "height", "viewportWidth", "viewportHeight", "autoMirrored")},
        "Unexpected vector transform or appearance override")
    for name in ("viewportWidth", "viewportHeight"):
        require(Decimal(vector.attrib[ANDROID + name]) == 24, f"Changed {name}")
    for name in ("width", "height"):
        require(vector.attrib[ANDROID + name] == "24dp", f"Changed {name}")
    actual = []
    for path in vector:
        require(path.tag == "path" and not list(path), "SVG elements must remain separate paths")
        require(set(path.attrib) <= {ANDROID + name for name in (
            "pathData", "fillColor", "strokeColor", "strokeWidth", "strokeLineCap", "strokeLineJoin")},
            "Unexpected path transform or appearance override")
        require(path.attrib.get(ANDROID + "fillColor") == "@android:color/transparent", "Changed fill")
        require(path.attrib.get(ANDROID + "strokeColor") == "@android:color/black", "Changed stroke")
        require(path.attrib.get(ANDROID + "strokeWidth") == "2", "Changed stroke width")
        require(path.attrib.get(ANDROID + "strokeLineCap") == "round", "Changed stroke cap")
        require(path.attrib.get(ANDROID + "strokeLineJoin") == "round", "Changed stroke join")
        actual.append(path_tokens(path.attrib[ANDROID + "pathData"]))
    require(actual == expected, "Changed geometry, element order, or element boundaries")


def verify(root):
    base = root / "test/icons"
    manifest = json.loads((base / "sources.json").read_text(encoding="utf-8"))
    sources = {name: read_source(base, source) for name, source in manifest["sources"].items()}
    registered = set(manifest["vectors"])
    discovered = set()
    for pattern in manifest["vector_globs"]:
        for file in root.glob(pattern):
            text = file.read_text(encoding="utf-8")
            if "Lucide " in text or "Converted from SVG:" in text or file.name.startswith("ic_lucide_"):
                discovered.add(file.relative_to(root).as_posix())
    require(discovered == registered, f"Register converted vector sources: {discovered ^ registered}")
    for target, source_name in manifest["vectors"].items():
        text = (root / target).read_text(encoding="utf-8")
        require(manifest["sources"][source_name]["revision"] in text, f"Missing revision: {target}")
        try:
            verify_vector(text, sources[source_name])
        except ValueError as error:
            raise ValueError(f"{target}: {error}") from error
    if "compose" in manifest:
        contract = manifest["compose"]
        text = (root / contract["file"]).read_text(encoding="utf-8")
        helper = text[text.index("private val LucideStroke:"):]
        require(digest(helper) == contract["renderer_sha256"],
                "Lucide renderer changed: review viewport, stroke, and separate-element rendering")
        declarations = dict(re.findall(
            r"    val (\w+): ImageVector = (.*?)(?=\n    val |\n    // Tight horizontal|\n}\n)",
            text, re.S))
        require(set(declarations) == set(contract["icons"]) | set(contract["exceptions"]),
                "Register every component icon and intentional artwork exception")
        for name, source_name in contract["icons"].items():
            block = declarations[name]
            require(block.startswith("lucideIcon("), f"{name}: use the verified Lucide renderer")
            require(re.fullmatch(r'lucideIcon\(\s*"\w+",\s*(?:"[^"]+",\s*)+(?:autoMirror = true,\s*)?\)\s*', block),
                    f"{name}: unsupported icon declaration")
            paths = re.findall(r'"([^"]+)"', block)[1:]
            require([path_tokens(data) for data in paths] == sources[source_name],
                    f"{name}: changed SVG geometry or element boundaries")
        for name, exception in contract["exceptions"].items():
            require(digest(declarations[name]) == exception["sha256"],
                    f"{name}: intentional artwork/cropped viewport changed; review at original scale")
    print(f"Icon conversions verified: {len(registered)} vectors, "
          f"{len(manifest.get('compose', {}).get('icons', {}))} Compose icons")


if __name__ == "__main__":
    try:
        verify(Path(__file__).resolve().parents[2])
    except (ValueError, KeyError, ET.ParseError) as error:
        sys.exit(f"Icon conversion contract failed: {error}")
