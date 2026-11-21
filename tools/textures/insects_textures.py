#!/usr/bin/env python3
"""Textures d'entités des insectes (v0.3): papillons (4 variantes) et coccinelle.

Usage:
    python3 tools/textures/insects_textures.py

Sorties (32x32, UV des modèles Java de insects/client):
    src/main/resources/assets/terresvivantes/textures/entity/butterfly_<variante>.png
    src/main/resources/assets/terresvivantes/textures/entity/ladybug.png

Déterministe: aucun tirage aléatoire, relancer le script rend les mêmes PNG.
Les zones utilisées suivent les boîtes des modèles: corps (0..18, 0..9), ailes supérieures
(4..12, 10..14), ailes inférieures (3..9, 18..21) pour le papillon; coccinelle: corps (0..14, 0..6),
élytres (0..11, 8..14), tête (0..6, 16..18).
"""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/terresvivantes/textures/entity"
SIZE = 32


def rgb(hex_color):
    """'#rrggbb' vers un tuple RGBA opaque."""
    h = hex_color.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def fill(px, x0, y0, x1, y1, color):
    """Remplit le rectangle [x0, x1) x [y0, y1)."""
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = color


def wing(px, x0, y0, x1, y1, base, edge, spot, spots):
    """Aile: fond, bord foncé sur le pourtour, taches aux positions locales données."""
    fill(px, x0, y0, x1, y1, base)
    for x in range(x0, x1):
        px[x, y0] = edge
        px[x, y1 - 1] = edge
    for y in range(y0, y1):
        px[x0, y] = edge
        px[x1 - 1, y] = edge
    for dx, dy in spots:
        px[x0 + dx, y0 + dy] = spot


# Palette des variantes: (fond, bord, tache)
BUTTERFLY_VARIANTS = {
    # monarque: orange, bordure noire, taches blanches
    "monarch": ("#e8821e", "#1d1409", "#f7f2e6"),
    # piéride du chou: blanc crème, bord gris, taches grises
    "cabbage_white": ("#f0efe6", "#6e6c62", "#3b3a34"),
    # morpho bleu: bleu vif, bord sombre, reflet clair
    "blue_morpho": ("#2a6fd8", "#13213d", "#8fc0f2"),
    # citron: jaune, bord brun, taches orangées
    "brimstone": ("#f2d03a", "#5a4316", "#d98a1c"),
}

BODY = rgb("#2b2118")
HEAD = rgb("#1a130d")
EYE = rgb("#f3e6c8")
SPOTS = [(1, 1), (2, 2), (1, 2), (2, 1)]


def butterfly_image(base_hex, edge_hex, spot_hex):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    base, edge, spot = rgb(base_hex), rgb(edge_hex), rgb(spot_hex)
    # corps fin : faces du box 1x1x8
    fill(px, 0, 0, 18, 9, BODY)
    fill(px, 8, 0, 9, 8, HEAD)
    px[8, 8] = EYE
    # ailes supérieures : face utile 4x4 à (4, 10)
    wing(px, 4, 10, 12, 14, base, edge, spot, SPOTS)
    # ailes inférieures : face utile 3x3 à (3, 18)
    wing(px, 3, 18, 9, 21, base, edge, spot, [(1, 1)])
    return img


def ladybug_image():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    red = rgb("#c8201c")
    black = rgb("#111111")
    # corps: dôme rouge, avec des points noirs
    fill(px, 0, 0, 14, 6, red)
    for x, y in [(2, 1), (6, 2), (10, 1), (4, 4), (9, 4)]:
        px[x, y] = black
    # élytres: moitiés rouges, points noirs décalés
    fill(px, 0, 8, 11, 14, red)
    for x, y in [(2, 9), (6, 10), (8, 12), (3, 12)]:
        px[x, y] = black
    # tête: noire, avec deux yeux pâles
    fill(px, 0, 16, 6, 18, black)
    px[1, 16] = EYE
    px[4, 16] = EYE
    return img


def build_all():
    """Retourne la liste (nom, image) de toutes les textures d'insectes."""
    out = []
    for variant, (base, edge, spot) in BUTTERFLY_VARIANTS.items():
        out.append((f"butterfly_{variant}", butterfly_image(base, edge, spot)))
    out.append(("ladybug", ladybug_image()))
    return out


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, img in build_all():
        path = OUT / f"{name}.png"
        img.save(path, format="PNG", optimize=False)
        print(path)


if __name__ == "__main__":
    main()
