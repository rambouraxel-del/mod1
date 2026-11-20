"""Sprites v0.4 de Terres Vivantes: auges, nichoirs, pierre a sel, livre d'elevage,
fourrage, aliments, trousse veterinaire, canard (oeuf, viande, entite).

SPRITES_V04: liste de (dossier, nom, grille), meme format que sprites.SPRITES.
Grilles 16x16 dessinees a la main, caractere par caractere, palette partagee
de generate_all.py (aucune nouvelle couleur). Lumiere venant du haut-gauche.

Entites canard (64x32): build_duck_entities(palette) produit duck.png et
duck_female.png a partir de l'atlas UV documente ci-dessous.
"""

from sprites import G


def _recolor(rows, mapping):
    """Remplace des caracteres de grille (ex. recoloration cuit / cru)."""
    return [
        "".join(mapping.get(ch, ch) for ch in row)
        for row in rows
    ]


# Cadre commun des auges et nichoirs: bords de bois clairs, interieur a la
# verticale. Les interieurs (12 colonnes) sont des listes de lignes.
def _frame(interior):
    rim_top = "aaaaaaaaaaaaaaaa"
    rim_side = "a00000000000000a"
    rows = [rim_top, rim_side]
    rows += ["80" + line + "08" for line in interior]
    rows += ["8000000000000008", "8888888888888888", "7777777777777777"]
    return rows


# ---------------------------------------------------------------------------
# AUGE (feeding_trough)
# ---------------------------------------------------------------------------

# Cote: planches horizontales separees par des joints sombres, cerclages en
# fer verticaux (colonnes 2 et 13).
FEEDING_TROUGH_SIDE = G("""
aaCaaaaaaaaaaCaa
00C0090000900C00
00C0000000000C00
00C0000000800C00
88C8888888888C88
77C7777777777C77
aaCaaaaaaaaaaCaa
00C0009000000C00
00C0000000000C00
88C8888888888C88
77C7777777777C77
aaCaaaaaaaaaaCaa
00C0000000900C00
00C0000000000C00
00C0900000000C00
77C7777777777C77
""")

# Dessus vide: rebord de planches, interieur brun sombre (eau et residus).
_INTERIOR_EMPTY = [
    "KKKKKKKKKKKK",
    "KLLLMLLLLKLL",
    "LMLLLLKLLLLL",
    "LLLKLLLMLLLL",
    "LLMLLLLLKLLL",
    "LKLLLLMLLLLL",
    "LLLLLLLLKLLL",
    "LLMLLLLLLLKL",
    "LLLLKLLMLLLL",
    "LLLLLLLLLLLL",
    "KKKKKKKKKKKK",
]
FEEDING_TROUGH_TOP = _frame(_INTERIOR_EMPTY)

# Foin: brins dores (S, T), ombres (R), quelques tiges sombres (Q).
_INTERIOR_HAY = [
    "SSTSSRSTSSRS",
    "TSRSSTSSRTSS",
    "SRSTSSRSSTSR",
    "SSTSRSSTSRSS",
    "RSSTSSRSTSSQ",
    "SSRSTSSRSSTS",
    "TSSRSTSSRSSS",
    "SRSSTQSRSTSS",
    "SSTSRSSTSRSS",
    "RSSTSSRSSTSR",
    "QSSRSTSSRSTS",
]
FEEDING_TROUGH_TOP_HAY = _frame(_INTERIOR_HAY)

# Grains: fond or (j), grains clairs (k, l, m), ombres (i).
_INTERIOR_GRAIN = [
    "jkjlkjjkljkj",
    "kjmkjjlkjmkj",
    "jlkjiikjlkjj",
    "kjjlkmjjikjl",
    "jkjjlkjmjkjl",
    "ljkjjkiljkjm",
    "jjkmjlkjjkil",
    "kjljkjjmkjlk",
    "jkjjlkjiijkl",
    "jlkjkjjlkmjk",
    "ikjjlkjmjkjj",
]
FEEDING_TROUGH_TOP_GRAIN = _frame(_INTERIOR_GRAIN)

# Ensilage: fourrage vert-brun fermente (@, &, %, Q, R).
_INTERIOR_SILAGE = [
    "@&@%@&@%@&@@",
    "&@Q@%@&@@R@&",
    "@%@&@R@&@%@@",
    "R@&@%@@&@Q@%",
    "@@%@&@R@@%@&",
    "&@@R@%@&@Q@%",
    "@%@&@@R@%&@@",
    "%@&Q@@&@%@R@",
    "@&@@%@R@&@%@",
    "&@%@R@@&@@%@",
    "@@&%@R@&@%@@",
]
FEEDING_TROUGH_TOP_SILAGE = _frame(_INTERIOR_SILAGE)

FEEDING_TROUGH_BOTTOM = G("""
7777777777777777
8888888888888888
8998899889988998
8888888888888888
7777777777777777
9988998899889988
8888888888888888
7777777777777777
8989898989898989
8888888888888888
7777777777777777
8998899889988998
8888888888888888
7777777777777777
8888888888888888
7777777777777777
""")

# ---------------------------------------------------------------------------
# NICHOIR (nesting_box)
# ---------------------------------------------------------------------------

# Cote: planches verticales (5 + 4 + 5 colonnes) separees par des joints.
def _plank_row(a, b, c):
    return a + "7" + b + "7" + c


NESTING_BOX_SIDE = [
    "aaaaa7aaaa7aaaaa",
    _plank_row("a0908", "a098", "a0098"),
    _plank_row("a0088", "a098", "a0908"),
    _plank_row("a0908", "a008", "a0098"),
    _plank_row("a0980", "a098", "a0008"),
    _plank_row("a0908", "a098", "a0098"),
    _plank_row("a0088", "a008", "a0908"),
    _plank_row("a0908", "a098", "a0098"),
    _plank_row("a0980", "a098", "a0008"),
    _plank_row("a0908", "a008", "a0098"),
    _plank_row("a0088", "a098", "a0908"),
    _plank_row("a0908", "a098", "a0098"),
    _plank_row("a0980", "a008", "a0008"),
    _plank_row("a0908", "a098", "a0098"),
    "8888878888788888",
    "7777777777777777",
]

NESTING_BOX_TOP = _frame(_INTERIOR_HAY)

# Face avant: planches horizontales, ouverture sombre (5 + 6 + 5) avec paille.
_FRONT_HEAD = [
    "aaaaaaaaaaaaaaaa",
    "0000000000000000",
    "0090000000900000",
    "8888888888888888",
    "aaaaaaaaaaaaaaaa",
]
_FRONT_OPENING_TOP = "0000888888880000"
_FRONT_OPENING_BOTTOM = "0000888888880000"
_FRONT_FOOT = [
    "0000000000000000",
    "0090000000900000",
    "8888888888888888",
]

NESTING_BOX_FRONT = (
    _FRONT_HEAD
    + [
        _FRONT_OPENING_TOP,
        "00008KKKKKK80000",
        "00008KKLKKK80000",
        "00008KLKKKK80000",
        "00008RSTSSR80000",
        "00008SSTSRS80000",
        "00008TSSRST80000",
        _FRONT_OPENING_BOTTOM,
    ]
    + _FRONT_FOOT
)

# Meme face avec un oeuf (z, A, x, y) pose dans la paille.
NESTING_BOX_FRONT_EGG = (
    _FRONT_HEAD
    + [
        _FRONT_OPENING_TOP,
        "00008KKKKKK80000",
        "00008KKLKKK80000",
        "00008KLzzKK80000",
        "00008RAzzyR80000",
        "00008SzzxyS80000",
        "00008TSyxST80000",
        _FRONT_OPENING_BOTTOM,
    ]
    + _FRONT_FOOT
)

# ---------------------------------------------------------------------------
# PIERRE A SEL (salt_lick): rose-blanc, usee au centre
# ---------------------------------------------------------------------------

SALT_LICK = G("""
--$--$-$--$-$--$
$$$$A$$$$$$$$$$$
$$$$$$+$$$$$$$$$
$-$$$$$$$$zz$$$$
$$$$$+$$$$$$$$$$
$$$$$$$y$$$$$$$$
$$A$$$$$$$$$$$$$
+$$$$$$$-$$$$$$$
$$$$$y$$$$$$$$$$
$$$$$$$$$zz$$$$$
$$$$+$$$$$$$$$$$
$$$$$$$$$$$$$$$+
$$-$$$$$$$$$$$$$
$$$$$$$$$$$A$$$$
+$$$$$$$$$$$$$$$
++++++++++++++++
""")

SALT_LICK_TOP = G("""
----------------
$$$$$$$$$$$$$$$$
$$$++++++++++$$$
$$+-zzzzzzzz-+$$
$+-zyyyyyyyyz-+$
$+zyyyAyyyyyyz+$
$+yyyyyyyyyyyy+$
$+yyAyyyyyyyyy+$
$+yyyyyyyyzyyy+$
$+zyyyyyyyyyyz+$
$+yyyyyyyyyyyy+$
$$+-zzzzzzzz-+$$
$$$++++++++++$$$
$$$$$$$$$$$$$$$$
$$$$$$$$$$$$$$$$
++++++++++++++++
""")

# ---------------------------------------------------------------------------
# OBJETS (items)
# ---------------------------------------------------------------------------

# Carnet d'elevage: couverture cuir brun, page creme a droite, empreinte de sabot.
LIVESTOCK_LEDGER = G("""
................
...##########...
...#8aaaaaaz#...
...#8999999z#...
...#8999999z#...
...#8999999z#...
...#8977799z#...
...#8777779z#...
...#8797979z#...
...#8799979z#...
...#8799979z#...
...#8999999z#...
...#8999999z#...
...#8999999z#...
...##########...
................
""")

# Fourrage ensile: balle ronde en filet, bandes de plastique blanc.
_SILAGE_ROWS = [
    "@&@%@&@%@&",
    "&%@R&@@%@&",
    "%@&@R%@&@%",
    "@R%@&@%&@R",
    "zzyzzyzzyz",
    "zyzzyzzyzz",
    "&@%R&@%&@R",
    "R&@%@&R@%&",
    "@%&@R&@%&@",
    "&@%&@R&@%@",
]
SILAGE = (
    ["................", "..############.."]
    + ["..#" + row + "#.." for row in _SILAGE_ROWS]
    + ["..############..", "................", "................", "................"]
)

# Melange de grains: sac de toile beige, col lie, grains au pied.
FEED_MIX = [
    "................",
    "......####......",
    "......#00#......",
    "......#99#......",
    "...##########...",
    "...#0a0a0a08#...",
    "...#a0a0a0a8#...",
    "...#0azzzza8#...",
    "...#a0zkzka8#...",
    "...#0azzkza8#...",
    "...#a0zzzz08#...",
    "...#0a0a0a08#...",
    "...#a0a0a088#...",
    "...##########...",
    "..k..lk..k.l.m..",
    "..k.lk..l..k.k..",
]

# Trousse veterinaire: boite blanche, croix verte, anse en fer.
VETERINARY_KIT = [
    "................",
    ".....######.....",
    "....#......#....",
    "....#......#....",
    "....########....",
    "..##############",
    "..#AAAAAAAAAA#..",
    "..#zzzz33zzzy#..",
    "..#zzzz33zzzy#..",
    "..#zz333333zy#..",
    "..#zz333333zy#..",
    "..#zzzz33zzzy#..",
    "..#zzzz33zzzy#..",
    "..##############",
    "................",
    "................",
]

# Helpers de centrage pour les sprites dont la silhouette est plus etroite que 16.
def _centered(rows):
    out = []
    top = (16 - len(rows)) // 2
    out += ["." * 16] * top
    for r in rows:
        left = (16 - len(r)) // 2
        out.append("." * left + r + "." * (16 - len(r) - left))
    out += ["." * 16] * (16 - len(out))
    return out


# Oeuf de canard: ovale bleu-vert pale, contour sombre (|), reflet blanc (A),
# ombre bas-droite ()). Largeurs de haut en bas: 4, 6, 8, 10 (x5), 10, 8, 6, 4.
DUCK_EGG = _centered([
    "||||",
    "|''''|",
    "|'A''''|",
    "|'A''''''|",
    "|''''''''|",
    "|''''''''|",
    "|''''''))|",
    "|''''''))|",
    "|''''))|",
    "|''))|",
    "||||",
])


def _drumstick():
    """Cuisse de canard: viande en ellipse inclinee (X clair, Y, W sombre),
    os blanc (z, y) avec deux noeuds, contour # sur tout le pourtour."""
    import math

    cx, cy, a, b = 10.0, 5.5, 5.0, 3.6
    th = math.radians(-45)
    ct, st = math.cos(th), math.sin(th)
    meat = set()
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            u = dx * ct + dy * st
            v = -dx * st + dy * ct
            if (u / a) ** 2 + (v / b) ** 2 <= 1.0:
                meat.add((x, y))

    def seg_dist(px, py, x0, y0, x1, y1):
        vx, vy = x1 - x0, y1 - y0
        t = max(0.0, min(1.0, ((px - x0) * vx + (py - y0) * vy) / (vx * vx + vy * vy)))
        return math.hypot(px - (x0 + t * vx), py - (y0 + t * vy))

    bone = set()
    for y in range(16):
        for x in range(16):
            if (seg_dist(x, y, 6.5, 8.5, 2.5, 13.0) <= 1.2
                    or math.hypot(x - 2.0, y - 13.5) <= 1.9
                    or math.hypot(x - 4.5, y - 14.8) <= 1.3):
                bone.add((x, y))

    shape = meat | bone
    grid = [["."] * 16 for _ in range(16)]
    for (x, y) in shape:
        if (x, y) in bone:
            ch = "y" if x + y >= 17 else "z"
        else:
            t = (x - cx) + (y - cy)
            ch = "W" if t > 3 else ("X" if t < -2 else "Y")
        edge = any((x + dx, y + dy) not in shape for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        grid[y][x] = "#" if edge else ch
    return ["".join(r) for r in grid]


_DRUMSTICK = _drumstick()
RAW_DUCK = _recolor(_DRUMSTICK, {"X": "h", "Y": "g", "W": "e"})
COOKED_DUCK = _recolor(_DRUMSTICK, {"X": "p", "Y": "o", "W": "n"})


SPRITES_V04 = []

for _name, _grid in [
    ("feeding_trough_side", FEEDING_TROUGH_SIDE),
    ("feeding_trough_top", FEEDING_TROUGH_TOP),
    ("feeding_trough_top_hay", FEEDING_TROUGH_TOP_HAY),
    ("feeding_trough_top_grain", FEEDING_TROUGH_TOP_GRAIN),
    ("feeding_trough_top_silage", FEEDING_TROUGH_TOP_SILAGE),
    ("feeding_trough_bottom", FEEDING_TROUGH_BOTTOM),
    ("nesting_box_side", NESTING_BOX_SIDE),
    ("nesting_box_top", NESTING_BOX_TOP),
    ("nesting_box_front", NESTING_BOX_FRONT),
    ("nesting_box_front_egg", NESTING_BOX_FRONT_EGG),
    ("salt_lick", SALT_LICK),
    ("salt_lick_top", SALT_LICK_TOP),
]:
    SPRITES_V04.append(("block", _name, _grid))

for _name, _grid in [
    ("livestock_ledger", LIVESTOCK_LEDGER),
    ("silage", SILAGE),
    ("feed_mix", FEED_MIX),
    ("veterinary_kit", VETERINARY_KIT),
    ("duck_egg", DUCK_EGG),
    ("raw_duck", RAW_DUCK),
    ("cooked_duck", COOKED_DUCK),
]:
    SPRITES_V04.append(("item", _name, _grid))


# ---------------------------------------------------------------------------
# ENTITE CANARD (64x32) - atlas UV
#
# Deplie de boite Minecraft standard pour (u, v, W, H, D) = (origine U, origine V,
# largeur X, hauteur Y, profondeur Z): largeur 2(W+D), hauteur D+H.
#   dessus (u+D, v) W x D        | dessous (u+D+W, v) W x D
#   cote x- (u, v+D) D x H       | avant z- (u+D, v+D) W x H  (cote du bec)
#   cote x+ (u+D+W, v+D) D x H   | arriere z+ (u+2D+W, v+D) W x H
#
# Boites (pixels occupes, bornes incluses):
#   corps        6x5x8  en (0, 0)     -> x 0..27,  y 0..12   (28x13)
#   tete         4x5x3  en (0, 16)    -> x 0..13,  y 16..23  (14x8)
#   bec          4x1x2  en (16, 16)   -> x 16..27, y 16..18  (12x3)
#   aile gauche  1x4x6  en (30, 0)    -> x 30..43, y 0..9    (14x10)
#   aile droite  1x4x6  en (30, 12)   -> x 30..43, y 12..21  (14x10)
#   patte gauche 3x2x1  en (46, 0)    -> x 46..53, y 0..2    (8x3)
#   patte droite 3x2x1  en (46, 4)    -> x 46..53, y 4..6    (8x3)
#
# Aucun chevauchement entre ilots. Les deux pattes partagent la boite 3x2x1
# (une par ilot, meme apparence), comme la texture vanilla des pattes.


def _faces(box):
    u, v, w, h, d = box
    return {
        "top": (u + d, v, u + d + w, v + d),
        "bottom": (u + d + w, v, u + d + 2 * w, v + d),
        "side_a": (u, v + d, u + d, v + d + h),
        "front": (u + d, v + d, u + d + w, v + d + h),
        "side_b": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


def _speckle(x, y, k):
    """Motif deterministe (pas de hasard) pour les taches du plumage."""
    return (x * 5 + y * 3 + k) % 7 == 0


# Canard colvert (male): tete verte, anneau blanc, corps gris-brun, bec jaune.
MALLARD_STYLE = {
    "body": {
        "top": "x", "bottom": "y", "side_a": "w", "front": "w", "side_b": "w", "back": "w",
        "detail": lambda f, x, y, w, h: "8" if f in ("side_a", "side_b") and _speckle(x, y, 1) else None,
    },
    "head": {
        "top": "3", "bottom": "1", "side_a": "2", "front": "2", "side_b": "2", "back": "2",
        "detail": lambda f, x, y, w, h: (
            "#" if f in ("side_a", "side_b") and x == 1 and y == 2
            else "z" if f == "front" and y == h - 1
            else None
        ),
    },
    "bill": {
        "top": "l", "bottom": "j", "side_a": "k", "front": "k", "side_b": "k", "back": "k",
        "detail": None,
    },
    "legs": {
        "top": "o", "bottom": "#", "side_a": "n", "front": "n", "side_b": "n", "back": "n",
        "detail": None,
    },
    "wing": {
        "top": "w", "bottom": "w", "side_a": "w", "front": "8", "side_b": "w", "back": "8",
        "detail": lambda f, x, y, w, h: "8" if f in ("side_a", "side_b") and y % 2 == 0 else None,
    },
}

# Canard colvert femelle: brun moucheté, bec orange-brun, pas de collier.
FEMALE_STYLE = {
    "body": {
        "top": "0", "bottom": "a", "side_a": "9", "front": "9", "side_b": "9", "back": "9",
        "detail": lambda f, x, y, w, h: "7" if _speckle(x, y, 3) and f != "bottom" else None,
    },
    "head": {
        "top": "9", "bottom": "8", "side_a": "8", "front": "8", "side_b": "8", "back": "8",
        "detail": lambda f, x, y, w, h: (
            "#" if f in ("side_a", "side_b") and x == 1 and y == 2
            else "7" if f in ("side_a", "side_b") and y == 1
            else None
        ),
    },
    "bill": {
        "top": "i", "bottom": "i", "side_a": "j", "front": "j", "side_b": "j", "back": "j",
        "detail": None,
    },
    "legs": {
        "top": "o", "bottom": "#", "side_a": "n", "front": "n", "side_b": "n", "back": "n",
        "detail": None,
    },
    "wing": {
        "top": "8", "bottom": "8", "side_a": "8", "front": "7", "side_b": "8", "back": "7",
        "detail": lambda f, x, y, w, h: "7" if f in ("side_a", "side_b") and _speckle(x, y, 2) else None,
    },
}


def _paint_box(px, box, style, palette):
    for face, (x0, y0, x1, y1) in _faces(box).items():
        base = style[face]
        detail = style.get("detail")
        for y in range(y0, y1):
            for x in range(x0, x1):
                ch = base
                if detail is not None:
                    over = detail(face, x - x0, y - y0, x1 - x0, y1 - y0)
                    if over:
                        ch = over
                px[x, y] = palette[ch] + (255,)


def _duck_image(style, palette):
    from PIL import Image

    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    _paint_box(px, (0, 0, 6, 5, 8), style["body"], palette)
    _paint_box(px, (0, 16, 4, 5, 3), style["head"], palette)
    _paint_box(px, (16, 16, 4, 1, 2), style["bill"], palette)
    _paint_box(px, (46, 0, 3, 2, 1), style["legs"], palette)
    _paint_box(px, (46, 4, 3, 2, 1), style["legs"], palette)
    _paint_box(px, (30, 0, 1, 4, 6), style["wing"], palette)
    _paint_box(px, (30, 12, 1, 4, 6), style["wing"], palette)
    return img


def build_duck_entities(palette):
    """Retourne [(nom, image 64x32)] pour le canard colvert et la femelle."""
    return [
        ("duck", _duck_image(MALLARD_STYLE, palette)),
        ("duck_female", _duck_image(FEMALE_STYLE, palette)),
    ]
