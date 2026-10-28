"""Render Timescale's clock side by side with another ratio: the same cottage, the same
real minutes, two speeds of sun.

    python scripts/preview/render_clock.py docs/images/timescale-clock.gif

What comes from the plugin, and what does not:

- **From the plugin.** The left panel uses the ratios in ``src/main/resources/config.yml``
  (``day-ratio``, ``night-ratio``; ``1.0`` each, vanilla speed). The right panel uses
  ``--day-ratio``/``--night-ratio`` (default ``3.0`` and ``1.0``, the example in
  CONFIG.md: a thirty-minute day and a ten-minute night). Both clocks are stepped one
  server tick at a time (20 a second) through a Python copy of ``ClockAccumulator``:
  ``1 / ratio`` world ticks per server tick, the ratio picked by whether the time of day
  is before or after 12000, the fraction carried forward.
- **Minecraft's sun.** Time of day 0 is sunrise, 6000 noon, 12000 sunset and 18000
  midnight; the sun crosses the sky east to west through the zenith, as in vanilla.
- **Decoration only.** The cottage, the sky colours and the lantern.

Needs Linux x86-64, ``libEGL.so.1`` with a Mesa driver (``LIBGL_ALWAYS_SOFTWARE=1
GALLIUM_DRIVER=llvmpipe`` without a GPU), NumPy, Pillow, PyYAML, and Strata's
official Python binding at the v0.6.0 tag::

    pip install numpy pillow pyyaml \\
      "strata-engine @ git+https://github.com/Preponderous-Software/strata@v0.6.0#subdirectory=bindings/python"

The library is Strata's v0.6.0 release, downloaded by the binding and checked
against the SHA-256 below. ``--frames DIR`` also writes every frame as a PNG.
"""

from __future__ import annotations

import argparse
import math
from pathlib import Path

import yaml
from PIL import Image, ImageDraw, ImageFont

import strata as S

STRATA_VERSION = "0.6.0"
STRATA_SHA256 = "3815068f90afa67b42177359a7ea918a5de321d7f7bad8706fa5275c4906af2a"  # libstrata-0.6.0-linux-x64.so, from the release's SHA256SUMS
ROOT = Path(__file__).resolve().parents[2]
CONFIG = ROOT / "src" / "main" / "resources" / "config.yml"
TICKS_PER_DAY, NIGHT_START, SERVER_TPS = 24000, 12000, 20
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


class ClockAccumulator:
    """A copy of com.dansplugins.timescale.ClockAccumulator."""

    def __init__(self, day_ratio: float, night_ratio: float):
        for name, r in (("day_ratio", day_ratio), ("night_ratio", night_ratio)):
            if not math.isfinite(r) or r <= 0:
                raise ValueError(f"{name} must be a positive, finite number but was {r}")
        self.day_ratio, self.night_ratio, self.remainder = day_ratio, night_ratio, 0.0

    def ticks_to_advance(self, world_time: int) -> int:
        ratio = self.night_ratio if world_time % TICKS_PER_DAY >= NIGHT_START else self.day_ratio
        self.remainder += 1.0 / ratio
        whole = math.floor(self.remainder)
        self.remainder -= whole
        return whole


def read_config(path: Path):
    cfg = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    return float(cfg.get("day-ratio", 1.0)), float(cfg.get("night-ratio", 1.0))


def to_sun(world_time: int):
    """Vanilla's sun: up in the east at 0, overhead at 6000, down in the west at 12000."""
    a = (world_time % TICKS_PER_DAY) / TICKS_PER_DAY * 2 * math.pi
    return (math.cos(a), math.sin(a), 0.0)


def clock_text(world_time: int) -> str:
    day = world_time // TICKS_PER_DAY + 1
    minutes = int(((world_time % TICKS_PER_DAY) / 1000 + 6) % 24 * 60)
    return f"day {day}, {minutes // 60:02d}:{minutes % 60:02d}"


def lerp(a, b, t):
    return tuple(x + (y - x) * t for x, y in zip(a, b))


def sky_for(elevation: float):
    night, dusk, day = (0.05, 0.07, 0.16), (0.93, 0.62, 0.42), (0.55, 0.74, 0.93)
    if elevation < 0:
        return lerp(dusk, night, min(1.0, -elevation / 0.25))
    return lerp(dusk, day, min(1.0, elevation / 0.35))


def cube(x, y, z, sx, sy, sz, rgb):
    return S.RenderItem(S.PRIMITIVE_CUBE, (x, y, z), (sx, sy, sz), (*rgb, 1.0))


def scene(night: float):
    window = lerp((0.25, 0.30, 0.38), (1.0, 0.82, 0.45), night)
    items = [cube(0, -0.5, 0, 40, 1, 40, (0.38, 0.64, 0.26)),
             cube(0, 1.5, 0, 6, 3, 5, (0.72, 0.58, 0.40)),
             cube(0, 3.4, 0, 6.6, 0.8, 5.6, (0.52, 0.24, 0.18)),
             cube(0, 4.1, 0, 5.0, 0.6, 4.0, (0.52, 0.24, 0.18)),
             cube(1.5, 4.6, -1.0, 0.8, 1.6, 0.8, (0.45, 0.45, 0.48)),
             cube(3.01, 1.0, 1.0, 0.1, 2.0, 1.0, (0.30, 0.20, 0.12)),
             cube(3.01, 1.8, -1.2, 0.1, 0.9, 0.9, window),
             cube(1.0, 1.8, 2.51, 1.0, 0.9, 0.1, window),
             cube(5.5, 1.0, 3.0, 0.2, 2.0, 0.2, (0.30, 0.20, 0.12)),   # lantern post
             cube(5.5, 2.15, 3.0, 0.4, 0.4, 0.4, lerp((0.35, 0.33, 0.28), (1.0, 0.85, 0.45), night)),
             cube(-1.0, 1.0, 7.0, 0.2, 2.0, 0.2, (0.45, 0.30, 0.18))]  # a pole, for its shadow
    for tx, tz in ((-7.0, -5.0), (6.5, -6.0), (-6.0, 5.5)):
        items += [cube(tx, 1.5, tz, 0.8, 3.0, 0.8, (0.40, 0.28, 0.16)),
                  cube(tx, 3.6, tz, 3.2, 2.0, 3.2, (0.24, 0.52, 0.20)),
                  cube(tx, 4.9, tz, 2.0, 1.0, 2.0, (0.24, 0.52, 0.20))]
    for i in range(6):  # a path from the door
        items.append(cube(3.6 + i, 0.01, 1.0, 1.0, 0.04, 1.0, (0.62, 0.55, 0.42)))
    return items


def render_panel(eng, lib, world_time: int):
    sun = to_sun(world_time)
    night = float(min(1.0, max(0.0, (0.15 - sun[1]) / 0.3)))
    eng.set_sun_direction(sun if sun[1] > -0.05 else (sun[0], 0.02, 0.3))
    eng.set_point_lights([S.PointLight((5.5, 2.4, 3.0), 9.0, (1.0, 0.75, 0.4), 2.5 * night, 0),
                          S.PointLight((3.6, 1.8, -1.2), 6.0, (1.0, 0.8, 0.45), 1.5 * night, 0)])
    sky = sky_for(sun[1])
    for _ in range(2):  # shadows settle on the second frame
        eng.begin_frame(clear_color=(*sky, 1.0))
        eng.submit_primitives(scene(night))
        eng.end_frame()
    rgba, w, h = eng.read_pixels()
    img = Image.frombytes("RGBA", (w, h), rgba).convert("RGB")
    if sun[1] < 0:  # vanilla nights are dark; Strata's sun mode keeps some ambient
        dim = Image.new("RGB", img.size, (8, 12, 30))
        img = Image.blend(img, dim, 0.55 * night)
    return img


def label(img, title, sub, world_time, scale):
    d = ImageDraw.Draw(img, "RGBA")
    s = lambda v: int(round(v * scale))
    bold, small = ImageFont.truetype(FONT_BOLD, s(17)), ImageFont.truetype(FONT, s(14))
    big = ImageFont.truetype(FONT_BOLD, s(24))
    d.rounded_rectangle((s(10), s(10), img.width - s(10), s(110)), radius=s(9), fill=(18, 22, 30, 185))
    d.text((s(22), s(18)), title, font=bold, fill=(240, 220, 150))
    d.text((s(22), s(42)), sub, font=small, fill=(225, 229, 236))
    d.text((s(22), s(66)), clock_text(world_time), font=big, fill=(255, 255, 255))
    return img


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("out", help="output .gif")
    ap.add_argument("--day-ratio", type=float, default=3.0)
    ap.add_argument("--night-ratio", type=float, default=1.0)
    ap.add_argument("--minutes", type=float, default=40.0, help="real minutes the clip covers")
    ap.add_argument("--frame-count", type=int, default=150)
    ap.add_argument("--panel", nargs=2, type=int, default=[480, 540])
    ap.add_argument("--poster", help="also write this still, from the right panel's first evening")
    ap.add_argument("--frames", help="also write every frame as a PNG into this directory")
    ap.add_argument("--gif-width", type=int, default=640)
    ap.add_argument("--gif-ms", type=int, default=120)
    ap.add_argument("--strata", help="a local libstrata instead of the v0.6.0 release")
    args = ap.parse_args(argv)

    left_ratios = read_config(CONFIG)
    right_ratios = (args.day_ratio, args.night_ratio)
    clocks = [ClockAccumulator(*left_ratios), ClockAccumulator(*right_ratios)]
    times = [0, 0]
    total_ticks = int(args.minutes * 60 * SERVER_TPS)
    per_frame = total_ticks / args.frame_count
    lib = S.load(args.strata) if args.strata else S.load_release(STRATA_VERSION, sha256=STRATA_SHA256)
    pw, ph = args.panel
    scale = pw / 480
    titles = [(f"day-ratio {left_ratios[0]:g}, night-ratio {left_ratios[1]:g}", "config.yml defaults: vanilla, 10 min day + 10 min night"),
              (f"day-ratio {right_ratios[0]:g}, night-ratio {right_ratios[1]:g}", "CONFIG.md example: 30 min day + 10 min night")]
    frames, poster, done = [], None, 0
    with S.Engine(lib, pw, ph) as eng:
        light = lib.lighting_defaults(S.LIGHTING_SUN)
        light.cascade_count, light.shadow_map_size, light.shadow_distance, light.fog = 4, 2048, 50.0, 0
        eng.set_lighting(light)
        eng.set_camera(lib.look_at((16.0, 9.0, 16.0), (0.0, 1.6, 0.0), (0.0, 1.0, 0.0), 50.0, pw / ph, 0.5, 150.0))
        for f in range(args.frame_count + 1):
            target = round(f * per_frame)
            for _ in range(target - done):
                for k in (0, 1):
                    times[k] += clocks[k].ticks_to_advance(times[k])
            done = target
            panels = [label(render_panel(eng, lib, times[k]), *titles[k], times[k], scale) for k in (0, 1)]
            frame = Image.new("RGB", (pw * 2 + 4, ph), (18, 22, 30))
            frame.paste(panels[0], (0, 0))
            frame.paste(panels[1], (pw + 4, 0))
            d = ImageDraw.Draw(frame, "RGBA")
            s = lambda v: int(round(v * scale))
            cap = ImageFont.truetype(FONT, s(14))
            real = done / SERVER_TPS
            d.rectangle((0, ph - s(30), frame.width, ph), fill=(18, 22, 30, 200))
            d.text((s(12), ph - s(24)),
                   f"Same real time on both: {int(real // 60):02d}:{int(real % 60):02d} of {int(args.minutes):02d}:00."
                   f"  Clocks stepped through Timescale's ClockAccumulator.  Rendered with Strata {STRATA_VERSION}",
                   font=cap, fill=(235, 238, 243))
            frames.append(frame)
            if poster is None and 9000 <= times[1] % TICKS_PER_DAY < 11000:
                poster = frame
    if args.frames:
        fd = Path(args.frames)
        fd.mkdir(parents=True, exist_ok=True)
        for i, im in enumerate(frames):
            im.save(fd / f"f{i:04d}.png")
    if args.poster:
        Path(args.poster).parent.mkdir(parents=True, exist_ok=True)
        (poster or frames[0]).save(args.poster, optimize=True)
    write_gif(frames, Path(args.out), args.gif_width, args.gif_ms)
    print(f"{len(frames)} frames; after {args.minutes:g} real minutes: left {clock_text(times[0])}, right {clock_text(times[1])}")
    return 0


def write_gif(images, out: Path, width: int, ms: int) -> None:
    if images[0].width > width:
        hh = round(images[0].height * width / images[0].width)
        images = [im.resize((width, hh), Image.LANCZOS) for im in images]
    step = max(1, len(images) // 24)
    sheet = Image.new("RGB", (images[0].width, images[0].height * len(images[::step])))
    for k, im in enumerate(images[::step]):
        sheet.paste(im, (0, k * im.height))
    palette = sheet.quantize(colors=255, method=Image.Quantize.MEDIANCUT)
    pal = [im.quantize(palette=palette, dither=Image.Dither.NONE) for im in images]
    out.parent.mkdir(parents=True, exist_ok=True)
    pal[0].save(out, save_all=True, append_images=pal[1:], duration=ms, loop=0, optimize=True)


if __name__ == "__main__":
    raise SystemExit(main())
