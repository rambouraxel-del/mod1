#!/usr/bin/env python3
"""Placeholders 16x16 des pieces de monnaie (v0.7, module economy).

Usage:
    python3 tools/textures/economy_placeholder.py

Ecrit copper_coin.png, silver_coin.png, gold_coin.png dans textures/item/ UNIQUEMENT si le fichier est absent :
une vraie texture fournie par l'agent textures n'est jamais ecrasee. Sert a faire passer GeneratedResourcesTest
(textures referencees par les modeles generes) tant que les vraies textures ne sont pas fusionnees.
"""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/terresvivantes/textures/item"

COINS = {
    "copper_coin": (184, 115, 51),
    "silver_coin": (192, 192, 200),
    "gold_coin": (232, 190, 60),
}


def coin(color):
    dark = tuple(max(0, c - 70) for c in color)
    light = tuple(min(255, c + 45) for c in color)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(3, 13):
        for x in range(3, 13):
            d = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if d <= 30:
                img.putpixel((x, y), color + (255,))
            if d <= 30 and (x + y) % 5 == 0:
                img.putpixel((x, y), light + (255,))
    for y in range(3, 13):
        for x in range(3, 13):
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 > 22 and (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 30:
                img.putpixel((x, y), dark + (255,))
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, color in COINS.items():
        path = OUT / f"{name}.png"
        if path.exists():
            print(f"absent non cree (texture existante conservee) : {path.name}")
            continue
        coin(color).save(path)
        print(f"placeholder cree : {path}")


if __name__ == "__main__":
    main()
