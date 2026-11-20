#!/usr/bin/env python3
"""Regenere toutes les textures pixel art 16x16 de Terres Vivantes.

Usage:
    python3 tools/textures/generate_all.py

Idempotent: relancer le script reecrit les memes PNG octet pour octet.
Les sprites sont des grilles de caracteres explicites (voir sprites.py),
chaque caractere designe une couleur de PALETTE ci-dessous. '.' = transparent.
Aucun bruit aleatoire, aucun anti-aliasing.

Sorties:
    src/main/resources/assets/terresvivantes/textures/item/*.png
    src/main/resources/assets/terresvivantes/textures/block/*.png
    src/main/resources/assets/terresvivantes/textures/entity/*.png  (insectes, 32x32, insects_textures.py)
    tools/textures/preview.png  (planche contact x8 avec les noms, sprites 16x16 seulement)
"""

import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from sprites import SPRITES, SEED_TEMPLATE, SEED_COLORS  # noqa: E402
from sprites_ext import SPRITES_EXT  # noqa: E402
from sprites_v03 import SPRITES_V03  # noqa: E402
import insects_textures  # noqa: E402

ROOT = HERE.parents[1]
TEX_ROOT = ROOT / "src/main/resources/assets/terresvivantes/textures"
PREVIEW_PATH = HERE / "preview.png"
SIZE = 16
ENTITY_SIZE = 32
SCALE = 8

# Palette partagee. Lumiere venant du haut-gauche: les clairs sont en haut a
# gauche, les ombres en bas a droite. Chaque famille a 3 a 6 nuances.
PALETTE = {
    "#": (43, 28, 16),        # contour brun tres sombre (objets bois/metal)
    # verts feuillage
    "1": (35, 67, 26), "2": (51, 97, 31), "3": (74, 138, 42),
    "4": (111, 177, 63), "5": (155, 209, 90), "6": (198, 232, 149),
    # bruns (tiges, bois, terre claire)
    "7": (62, 38, 18), "8": (94, 59, 28), "9": (127, 84, 40),
    "0": (160, 110, 58), "a": (196, 146, 88), "b": (224, 187, 132),
    # rouges
    "c": (94, 15, 12), "d": (142, 27, 21), "e": (184, 43, 32),
    "f": (217, 73, 58), "g": (239, 122, 98), "h": (249, 183, 160),
    # jaunes / ors
    "i": (143, 98, 8), "j": (185, 131, 18), "k": (220, 170, 38),
    "l": (240, 205, 85), "m": (251, 233, 142),
    # oranges
    "n": (154, 61, 12), "o": (201, 92, 24), "p": (232, 132, 46),
    "q": (247, 181, 106),
    # violets
    "r": (60, 23, 64), "s": (93, 38, 98), "t": (127, 61, 136),
    "u": (164, 96, 168), "v": (201, 154, 208),
    # creme / blanc
    "w": (126, 117, 102), "x": (170, 160, 142), "y": (212, 203, 180),
    "z": (239, 232, 214), "A": (251, 248, 239),
    # gris metal
    "B": (45, 51, 56), "C": (80, 88, 95), "D": (125, 134, 141),
    "E": (170, 178, 184), "F": (216, 222, 226),
    # bleus (eau, verre)
    "G": (23, 61, 92), "H": (42, 104, 145), "I": (79, 155, 201),
    "J": (158, 208, 238),
    # terre / compost
    "K": (31, 21, 12), "L": (51, 35, 15), "M": (77, 52, 24),
    "N": (110, 75, 37), "O": (143, 102, 54), "P": (180, 138, 87),
    # sec / fane
    "Q": (74, 61, 30), "R": (118, 98, 48), "S": (156, 138, 78),
    "T": (191, 174, 115),
    # verre pale
    "U": (207, 232, 234), "V": (156, 199, 204),
    # vergers: olivier gris-vert, feuillage colore directement
    "&": (74, 84, 60), "@": (112, 122, 86), "%": (148, 156, 116),
    "=": (182, 188, 146),
    # sante: fongicide bleu-vert
    "|": (20, 94, 90), "(": (34, 140, 128), ")": (98, 190, 176),
    "'": (182, 232, 220),
    # fleurs de pecher / pommier
    "$": (245, 164, 192), "+": (214, 110, 150), "-": (252, 214, 226),
}


def render(rows, overrides=None):
    """Convertit une grille de 16 lignes de 16 caracteres en image RGBA."""
    if len(rows) != SIZE:
        raise ValueError(f"grille: {len(rows)} lignes au lieu de 16")
    pal = dict(PALETTE)
    if overrides:
        pal.update(overrides)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        if len(row) != SIZE:
            raise ValueError(f"ligne {y}: {len(row)} caracteres au lieu de 16: {row!r}")
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if ch not in pal:
                raise ValueError(f"caractere inconnu {ch!r} a ({x},{y})")
            px[x, y] = pal[ch] + (255,)
    return img


def build_all():
    """Retourne une liste (dossier, nom, image) pour toutes les textures."""
    out = []
    for folder, name, rows in SPRITES + SPRITES_EXT + SPRITES_V03:
        out.append((folder, name, render(rows)))
    # Sachets de graines: meme gabarit, couleurs propres a chaque culture.
    for crop, (seed_col, band_col) in SEED_COLORS.items():
        overrides = {"X": seed_col, "Y": band_col}
        out.append(("item", f"{crop}_seeds", render(SEED_TEMPLATE, overrides)))
    return out


def write_textures(entries):
    written = []
    for folder, name, img in entries:
        path = TEX_ROOT / folder / f"{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path, format="PNG", optimize=False)
        written.append(path)
    return written


def write_entity_textures():
    """Textures d'entités des insectes (32x32), écrites dans textures/entity."""
    written = []
    insects_textures.OUT.mkdir(parents=True, exist_ok=True)
    for name, img in insects_textures.build_all():
        path = insects_textures.OUT / f"{name}.png"
        img.save(path, format="PNG", optimize=False)
        written.append((path, img))
    return written


def verify_entity(written):
    errors = []
    for path, img in written:
        if img.size != (ENTITY_SIZE, ENTITY_SIZE):
            errors.append(f"taille {img.size} au lieu de 32x32: {path}")
        if not path.is_file():
            errors.append(f"manquant: {path}")
    return errors


def verify(entries):
    errors = []
    for folder, name, _ in entries:
        path = TEX_ROOT / folder / f"{name}.png"
        if not path.is_file():
            errors.append(f"manquant: {path}")
            continue
        with Image.open(path) as im:
            if im.size != (SIZE, SIZE):
                errors.append(f"taille {im.size}: {path}")
            if im.mode != "RGBA":
                errors.append(f"mode {im.mode}: {path}")
    return errors


def build_preview(entries):
    """Planche contact: chaque texture agrandie x8 avec son nom."""
    cell = SIZE * SCALE
    label_h = 18
    pad = 10
    cols = 12
    rows_n = (len(entries) + cols - 1) // cols
    width = cols * (cell + pad) + pad
    height = rows_n * (cell + label_h + pad) + pad
    sheet = Image.new("RGBA", (width, height), (58, 52, 46, 255))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.load_default(size=11)
    except TypeError:
        font = ImageFont.load_default()
    for i, (folder, name, img) in enumerate(entries):
        cx = pad + (i % cols) * (cell + pad)
        cy = pad + (i // cols) * (cell + label_h + pad)
        # fond damier pour voir la transparence
        for ty in range(0, cell, 8):
            for tx in range(0, cell, 8):
                shade = (96, 90, 84) if ((tx // 8) + (ty // 8)) % 2 == 0 else (120, 114, 108)
                draw.rectangle([cx + tx, cy + ty, cx + tx + 7, cy + ty + 7], fill=shade)
        big = img.resize((cell, cell), Image.NEAREST)
        sheet.alpha_composite(big, (cx, cy))
        label = f"{folder}/{name}"
        draw.text((cx, cy + cell + 2), label, fill=(240, 232, 220, 255), font=font)
    sheet.save(PREVIEW_PATH, format="PNG")
    return PREVIEW_PATH


def main():
    entries = build_all()
    written = write_textures(entries)
    errors = verify(entries)
    entity = write_entity_textures()
    errors += verify_entity(entity)
    if errors:
        for e in errors:
            print("ERREUR", e)
        sys.exit(1)
    build_preview(entries)
    print(f"{len(written)} textures ecrites dans {TEX_ROOT}")
    print(f"{len(entity)} textures d'entites (32x32) ecrites dans {insects_textures.OUT}")
    print(f"preview: {PREVIEW_PATH}")


if __name__ == "__main__":
    main()
