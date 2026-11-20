"""Textures provisoires v0.5 (transformation) de Terres Vivantes, produites avec Pillow.

Sorties (deterministes, relancer ne change rien) sous src/main/resources/assets/terresvivantes/textures/ :
  block/millstone_side.png, block/millstone_top.png, block/millstone_top_active.png (16x16, meule)
  item/wheat_flour.png (16x16, farine de ble)
  gui/processing.png (176x166, fond de l'ecran generique des machines)

Ce sont des versions simples en attendant les textures definitives. Le fond de GUI place les cases aux positions
du menu (ProcessingMenu) : entrées sur la rangée y=17 (4 cases au plus, de x=52 a x=106), conteneur (26,35), flèche
(79,35, 24x17), sortie (134,35), inventaire joueur a partir de y=84 et barre rapide a y=142.

Usage : python3 tools/textures/sprites_v05.py
"""

import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/terresvivantes/textures"

STONE_DARK = (92, 92, 98, 255)
STONE = (128, 128, 136, 255)
STONE_LIGHT = (164, 164, 172, 255)
FLOUR = (246, 238, 214, 255)
FLOUR_SHADE = (214, 198, 160, 255)
PANEL = (198, 198, 198, 255)
PANEL_DARK = (85, 85, 85, 255)
PANEL_LIGHT = (255, 255, 255, 255)
SLOT = (139, 139, 139, 255)
SLOT_LIGHT = (255, 255, 255, 255)
ARROW_TRACK = (120, 120, 120, 255)
ARROW_TRACK_DARK = (60, 60, 60, 255)


def millstone_side() -> Image.Image:
    img = Image.new("RGBA", (16, 16), STONE)
    draw = ImageDraw.Draw(img)
    for y in (3, 8, 12):
        draw.line([(0, y), (15, y)], fill=STONE_DARK)
    for y in (1, 6, 10, 14):
        draw.point([(x, y) for x in range(0, 16, 4)], fill=STONE_LIGHT)
    return img


def millstone_top(active: bool = False) -> Image.Image:
    img = Image.new("RGBA", (16, 16), STONE_LIGHT)
    draw = ImageDraw.Draw(img)
    draw.ellipse([0, 0, 15, 15], outline=STONE_DARK, fill=STONE)
    for angle in range(0, 360, 45):
        # Rainures radiales de la meule, du centre vers le bord.
        dx, dy = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        draw.line([(7.5 + dx * 2, 7.5 + dy * 2), (7.5 + dx * 6.5, 7.5 + dy * 6.5)], fill=STONE_DARK)
    draw.point([(7, 7), (8, 7), (7, 8), (8, 8)], fill=STONE_DARK)
    if active:
        # Poussière de farine sur les rainures quand la mouture avance.
        for x, y in ((4, 6), (11, 5), (5, 11), (10, 10), (8, 3), (3, 9)):
            draw.point((x, y), fill=FLOUR)
    return img


def wheat_flour() -> Image.Image:
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.polygon([(3, 13), (13, 13), (11, 7), (5, 7)], fill=FLOUR, outline=FLOUR_SHADE)
    draw.ellipse([5, 4, 11, 8], fill=FLOUR, outline=FLOUR_SHADE)
    draw.point([(7, 10), (9, 11), (6, 12)], fill=FLOUR_SHADE)
    return img


def _slot(draw: ImageDraw.ImageDraw, x: int, y: int) -> None:
    """Case 18x18 de l'inventaire vanilla, coin superieur gauche de la case en (x-1, y-1)."""
    draw.rectangle([x - 1, y - 1, x + 16, y + 16], fill=SLOT)
    draw.line([(x - 1, y - 1), (x + 16, y - 1)], fill=ARROW_TRACK_DARK)
    draw.line([(x - 1, y - 1), (x - 1, y + 16)], fill=ARROW_TRACK_DARK)
    draw.line([(x, y + 16), (x + 15, y + 16)], fill=SLOT_LIGHT)
    draw.line([(x + 16, y), (x + 16, y + 15)], fill=SLOT_LIGHT)


def processing_gui() -> Image.Image:
    img = Image.new("RGBA", (176, 166), PANEL)
    draw = ImageDraw.Draw(img)
    # Cadre du panneau machine (0 a 82) et de l'inventaire joueur (82 a 166).
    draw.rectangle([0, 0, 175, 165], outline=PANEL_DARK)
    draw.line([(1, 1), (174, 1)], fill=PANEL_LIGHT)
    draw.line([(1, 1), (1, 164)], fill=PANEL_LIGHT)
    draw.line([(0, 82), (175, 82)], fill=PANEL_DARK)
    draw.line([(1, 83), (174, 83)], fill=PANEL_LIGHT)
    # Entrees : quatre cases au plus, centrees sur x=88.
    for i in range(4):
        _slot(draw, 52 + 18 * i, 17)
    _slot(draw, 26, 35)  # conteneur
    _slot(draw, 134, 35)  # sortie
    # Piste de la fleche de progression (79, 35), 24 x 17.
    draw.rectangle([79, 35, 102, 51], fill=ARROW_TRACK, outline=ARROW_TRACK_DARK)
    # Inventaire joueur (3 x 9) et barre rapide.
    for row in range(3):
        for col in range(9):
            _slot(draw, 8 + col * 18, 84 + row * 18)
    for col in range(9):
        _slot(draw, 8 + col * 18, 142)
    return img


def main() -> None:
    (ROOT / "block").mkdir(parents=True, exist_ok=True)
    (ROOT / "item").mkdir(parents=True, exist_ok=True)
    (ROOT / "gui").mkdir(parents=True, exist_ok=True)
    millstone_side().save(ROOT / "block/millstone_side.png")
    millstone_top().save(ROOT / "block/millstone_top.png")
    millstone_top(active=True).save(ROOT / "block/millstone_top_active.png")
    wheat_flour().save(ROOT / "item/wheat_flour.png")
    processing_gui().save(ROOT / "gui/processing.png")
    print("textures v0.5 ecrites dans", ROOT)


if __name__ == "__main__":
    main()
