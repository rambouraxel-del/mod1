"""Sprites v0.9 : vigne, riz et races d'animaux.

- Vigne (16x16): treillis (block/trellis), vigne en croix aux stades 0 à 3 (block/grapevine_stage0..2
  communs; red_grapevine_stage3 grappes violettes, white_grapevine_stage3 grappes vert-doré), et les
  items raisin rouge/blanc, jus de raisin, vins jeune/rouge/blanc (bouteilles), bouture.
- Riz (16x16): plants dans l'eau (block/rice_stage0..3, base transparente), grains, épi, plant,
  risotto, bol de riz, galette de riz (item/rice_cake).
- Races (64x32, UV vanilla de la vache et de la poule): superpositions qui ne dessinent que les
  motifs, le reste reste transparent. Les rectangles de chaque face suivent les boîtes des modèles
  vanilla (cow.png, chicken.png, extraits du jar client 1.21.1).

Les grilles sont écrites caractère par caractère, avec la palette partagée de generate_all.py.
Aucune nouvelle couleur: les bouteilles et bols utilisent des overrides de couleur (comme
SEED_COLORS), les grilles restent identiques d'une variante à l'autre.

SPRITES_V09: liste de (dossier, nom, grille), même format que sprites.SPRITES.
BOTTLE_VARIANTS / BOWL_VARIANTS: overrides de couleur par variante.
build_breed_entities(palette): liste de (nom, image 64x32) pour textures/entity/breed/.
"""

from PIL import Image

# ---------------------------------------------------------------- treillis
TRELLIS = [
    "################",
    "#00000000000000#",
    "#0...9....9...7#",
    "#0...9....9...7#",
    "#0...9....9...7#",
    "#09998999989997#",
    "#0...9....9...7#",
    "#0...9....9...7#",
    "#0...9....9...7#",
    "#0...9....9...7#",
    "#09998999989997#",
    "#0...9....9...7#",
    "#0...9....9...7#",
    "#0...9....9...7#",
    "#77777777777777#",
    "################",
]

# ---------------------------------------------------------------- vigne en croix
# Tige au centre (colonne 7), feuilles par paires. Stades 0 à 2 communs.
GRAPEVINE_STAGE0 = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......4.........",
    "...33.4.33......",
    "..3443.4.3443...",
    "....34743.......",
    ".......7........",
    ".......7........",
    ".......7........",
    ".......8........",
]

GRAPEVINE_STAGE1 = [
    "................",
    "................",
    "................",
    "................",
    ".......7........",
    "....33.7.33.....",
    ".....34743......",
    ".......7........",
    "...33..7..33....",
    "..43...7...34...",
    ".......7........",
    ".......7........",
    ".......7........",
    ".......8........",
    ".......8........",
    "......8.8.......",
]

GRAPEVINE_STAGE2 = [
    "................",
    "................",
    "......mlm.......",
    "......343.......",
    "...3.4.7.4.3....",
    "....33.7.33.....",
    ".....34743......",
    ".......7........",
    "...33..7..33....",
    "..43...7...34...",
    ".......7........",
    ".......7........",
    ".......7........",
    ".......8........",
    ".......8........",
    "......8.8.......",
]

# Stade 3 rouge: mêmes feuilles, grappes violettes pendant de deux petits pédoncules (colonnes 5 et 9).
RED_GRAPEVINE_STAGE3 = [
    "................",
    "................",
    ".......4........",
    "......343.......",
    "...3.4.7.4.3....",
    "....33.7.33.....",
    ".....34743......",
    ".......7........",
    ".....8.7.8......",
    "....vu.7.vu.....",
    "...uut.7.uut....",
    "...tut.7.tur....",
    "....rt.7..rt....",
    ".......8........",
    ".......8........",
    "......8.8.......",
]

# Stade 3 blanc: même dessin, grappes vert-doré (vert clair en lumière, or en corps).
WHITE_GRAPEVINE_STAGE3 = [
    "................",
    "................",
    ".......4........",
    "......343.......",
    "...3.4.7.4.3....",
    "....33.7.33.....",
    ".....34743......",
    ".......7........",
    ".....8.7.8......",
    "....6l.7.6l.....",
    "...l6l.7.l6l....",
    "...kl6.7.kl6....",
    "....ki.7..ki....",
    ".......8........",
    ".......8........",
    "......8.8.......",
]

# ---------------------------------------------------------------- items raisin et bouture
RED_GRAPES = [
    "........7.......",
    "........7.......",
    "......vuvu......",
    "......tutu......",
    "....vuvuvu......",
    "....tututu......",
    "...vuvuvuvu.....",
    "...tutututu.....",
    "....vuvuvu......",
    "....tututu......",
    "......vuvu......",
    "......tutu......",
    ".......rr.......",
    "................",
    "................",
    "................",
]

# Même grappe, en vert-doré (v -> 6, u -> 5, t -> k, r -> i) et pédoncule vert.
WHITE_GRAPES = [row.translate(str.maketrans("vutr", "65ki")).replace("7", "4") for row in RED_GRAPES]

GRAPEVINE_CUTTING = [
    "...........33...",
    "..........3443..",
    "..........7.....",
    ".........77.....",
    "........77......",
    ".......77.......",
    "......77........",
    ".....77.........",
    "....77..........",
    "...77...........",
    "..77............",
    ".77.............",
    "77..............",
    "................",
    "................",
    "................",
]

# Bouteille: gabarit partagé, overrides par variante (X verre, H reflet, Y vin, Z étiquette, C bouchon).
BOTTLE_TEMPLATE = [
    "......CC........",
    "......CC........",
    ".....#CC#.......",
    ".....#XXXX#.....",
    "....#XXXXXX#....",
    "...#XHXXXXXX#...",
    "...#XXXXXXXX#...",
    "...#ZZZZZZZZ#...",
    "...#ZZZZZZZZ#...",
    "...#ZZZZZZZZ#...",
    "...#ZZZZZZZZ#...",
    "...#YYYYYYYY#...",
    "...#YYYYYYYY#...",
    "...#YYYYYYYY#...",
    "...#YYYYYYYY#...",
    "...##########...",
]

BOTTLE_VARIANTS = {
    "grape_juice": {"X": (34, 27, 38), "H": (88, 50, 100), "Y": (93, 38, 98),
                    "Z": (239, 232, 214), "C": (160, 110, 58)},
    "young_wine": {"X": (35, 67, 26), "H": (74, 138, 42), "Y": (201, 154, 208),
                   "Z": (239, 232, 214), "C": (160, 110, 58)},
    "red_wine": {"X": (45, 51, 56), "H": (80, 88, 95), "Y": (142, 27, 21),
                 "Z": (239, 232, 214), "C": (160, 110, 58)},
    "white_wine": {"X": (35, 67, 26), "H": (74, 138, 42), "Y": (251, 233, 142),
                   "Z": (239, 232, 214), "C": (160, 110, 58)},
}

# ---------------------------------------------------------------- riz
RICE_STAGE0 = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "..4.............",
    "..4.4.....4.....",
    "..4.4.....4.4...",
    "..3.4.....3.4...",
    "..3.4.4...3.3...",
    "..3.3.4...3.3...",
    "................",
]

RICE_STAGE1 = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......4........",
    ".......4.4......",
    "....4..4.4......",
    "....4..4.4.4....",
    "....4..4.4.4....",
    "....3..3.3.3....",
    "....3..3.3.3....",
    "................",
]

RICE_STAGE2 = [
    "................",
    "................",
    "................",
    ".......6........",
    ".......5........",
    ".......5.5......",
    "....5..5.5......",
    "....5..5.5.5....",
    "....4..4.4.4....",
    "....4..4.4.4....",
    "....4..4.4.4....",
    "....4..4.4.4....",
    "....3..3.3.3....",
    "....3..3.3.3....",
    "....3..3.3.3....",
    "................",
]

# Épis dorés penchés: têtes en k/l/m, tiges vertes sous les épis.
RICE_STAGE3 = [
    "................",
    "....mlmlm.......",
    "....lkmkl.......",
    ".....klk........",
    ".......4........",
    ".......4.4......",
    "....4..4.4......",
    "....4..4.4.4....",
    "....4..4.4.4....",
    "....4..4.4.4....",
    "....4..4.4.4....",
    "....4..4.4.4....",
    "....3..3.3.3....",
    "....3..3.3.3....",
    "....3..3.3.3....",
    "................",
]

RICE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......zAz.......",
    ".....zAzAz......",
    "....AzAzAyz.....",
    "...zAzAzyzAy....",
    "..yzAzyAzAzyz...",
    ".xyzAzyAzyzxy...",
    "xyzxyAzyzxyzxyzx",
    "xyxwxyzxyxwxyzxy",
    "wxyxwxyxwxyxwxyw",
    "xwxyxwxwxyxwxyxw",
]

RICE_PANICLE = [
    "............m...",
    "..........lkm...",
    ".........lklk...",
    ".........kmlk...",
    "........kmlkm...",
    "........lkmk....",
    "......0.........",
    ".....0..........",
    ".....9..........",
    "....9...........",
    "....9...........",
    "...9............",
    "...9............",
    "..9.............",
    "..8.............",
    ".8..............",
]

RICE_SEEDLING = [
    "................",
    "................",
    "................",
    "........4.......",
    ".....4..4.......",
    ".....4..4..4....",
    ".....4..4..4....",
    ".....4..4..4....",
    ".....4..4..4....",
    ".....3..3..3....",
    ".....3..3..3....",
    ".....7..7..7....",
    "................",
    "................",
    "................",
    "................",
]

# Bol: gabarit partagé (M contenu, G grains, R bord, B corps). Overrides par plat.
BOWL_TEMPLATE = [
    "................",
    "................",
    "................",
    "....MMMMMMMM....",
    "...MMGMMMMGMM...",
    "..MMMMGMMMMMMM..",
    ".#RRRRRRRRRRRR#.",
    "..#BBBBBBBBBB#..",
    "...#BBBBBBBB#...",
    "....#BBBBBB#....",
    ".....#BBBB#.....",
    "......######....",
    "................",
    "................",
    "................",
    "................",
]

BOWL_VARIANTS = {
    "risotto": {"M": (240, 205, 85), "G": (239, 232, 214), "R": (251, 248, 239), "B": (212, 203, 180)},
    "rice_bowl": {"M": (251, 248, 239), "G": (239, 232, 214), "R": (42, 104, 145), "B": (158, 208, 238)},
}

# Galette de riz: disque doré, grains en couches concentriques.
RICE_CAKE = [
    "................",
    "................",
    "................",
    "....jjjjjjjj....",
    "...jkkkkkkkkj...",
    "..jkllllllllkj..",
    ".jklmmmmmmmmlkj.",
    ".jlmmmmmmmmmmlj.",
    ".jlmmmiimmmmmlj.",
    ".jlmmmmmmmmmmlj.",
    ".jklmmmmmmmmlkj.",
    "..jkllllllllkj..",
    "...jkkkkkkkkj...",
    "....jjjjjjjj....",
    "................",
    "................",
]

SPRITES_V09 = [
    ("block", "trellis", TRELLIS),
    ("block", "grapevine_stage0", GRAPEVINE_STAGE0),
    ("block", "grapevine_stage1", GRAPEVINE_STAGE1),
    ("block", "grapevine_stage2", GRAPEVINE_STAGE2),
    ("block", "red_grapevine_stage3", RED_GRAPEVINE_STAGE3),
    ("block", "white_grapevine_stage3", WHITE_GRAPEVINE_STAGE3),
    ("item", "red_grapes", RED_GRAPES),
    ("item", "white_grapes", WHITE_GRAPES),
    ("item", "grapevine_cutting", GRAPEVINE_CUTTING),
    ("block", "rice_stage0", RICE_STAGE0),
    ("block", "rice_stage1", RICE_STAGE1),
    ("block", "rice_stage2", RICE_STAGE2),
    ("block", "rice_stage3", RICE_STAGE3),
    ("item", "rice", RICE),
    ("item", "rice_panicle", RICE_PANICLE),
    ("item", "rice_seedling", RICE_SEEDLING),
    ("item", "rice_cake", RICE_CAKE),
]

# ---------------------------------------------------------------- races (64x32)
# Boîtes UV vanilla: (u, v, largeur, hauteur) par face. Vache: tête 8x8x6 en (0,0), corps 12x18x10 en
# (18,4), pattes 4x12x4 en (0,16). Poule: tête 4x6x3 en (0,0), corps 6x8x6 en (0,9), aile 1x4x6 en (24,13).
COW_HEAD = {"top": (6, 0, 8, 6), "bottom": (14, 0, 8, 6), "right": (0, 6, 6, 8),
            "front": (6, 6, 8, 8), "left": (14, 6, 6, 8), "back": (20, 6, 8, 8)}
COW_BODY = {"top": (28, 4, 12, 10), "bottom": (40, 4, 12, 10), "right": (18, 14, 10, 18),
            "front": (28, 14, 12, 18), "left": (40, 14, 10, 18), "back": (50, 14, 12, 18)}
COW_LEG = {"top": (4, 16, 4, 4), "bottom": (8, 16, 4, 4), "right": (0, 20, 4, 12),
           "front": (4, 20, 4, 12), "left": (8, 20, 4, 12), "back": (12, 20, 4, 12)}
CHICKEN_HEAD = {"top": (3, 0, 4, 3), "bottom": (7, 0, 4, 3), "right": (0, 3, 3, 6),
                "front": (3, 3, 4, 6), "left": (7, 3, 3, 6), "back": (10, 3, 4, 6)}
CHICKEN_BODY = {"top": (6, 9, 6, 6), "bottom": (12, 9, 6, 6), "right": (0, 15, 6, 8),
                "front": (6, 15, 6, 8), "left": (12, 15, 6, 8), "back": (18, 15, 6, 8)}
CHICKEN_WING = {"top": (30, 13, 1, 6), "bottom": (31, 13, 1, 6), "right": (24, 19, 6, 4),
                "front": (30, 19, 1, 4), "left": (31, 19, 1, 4), "back": (37, 19, 6, 4)}

# Une face: (caractère de fond, [(x0, y0, x1, y1, caractère), ...]) en coordonnées locales à la face.
COW_HOLSTEIN = {
    "head": {
        "top": ("K", []), "bottom": ("A", []), "right": ("K", []), "left": ("K", []), "back": ("K", []),
        "front": ("K", [(0, 5, 8, 8, "A")]),
    },
    "body": {
        "top": ("A", [(0, 0, 7, 4, "K"), (8, 6, 12, 10, "K")]),
        "bottom": ("A", []),
        "right": ("A", [(0, 4, 7, 8, "K"), (2, 14, 8, 18, "K")]),
        "left": ("A", [(0, 4, 7, 8, "K"), (2, 14, 8, 18, "K")]),
        "front": ("A", [(0, 0, 5, 6, "K"), (7, 5, 12, 11, "K"), (1, 13, 6, 18, "K")]),
        "back": ("A", [(3, 2, 9, 10, "K"), (0, 12, 4, 18, "K")]),
    },
    "leg": "K",
}

COW_CHAROLAIS = {
    "head": {
        "top": ("z", []), "bottom": ("y", []), "right": ("y", []), "left": ("y", []), "back": ("y", []),
        "front": ("z", [(2, 5, 6, 8, "y")]),
    },
    "body": {
        "top": ("z", [(0, 0, 12, 2, "A")]),
        "bottom": ("y", []),
        "right": ("y", []),
        "left": ("y", []),
        "front": ("z", [(0, 0, 12, 2, "A")]),
        "back": ("y", []),
    },
    "leg": "y",
}

COW_HIGHLAND = {
    "head": {
        "top": ("7", []), "bottom": ("7", []), "right": ("8", [(0, 4, 6, 8, "7")]),
        "left": ("8", [(0, 4, 6, 8, "7")]), "back": ("8", [(0, 5, 8, 8, "7")]),
        "front": ("8", [(0, 0, 8, 2, "7"), (0, 5, 8, 8, "7")]),
    },
    "body": {
        "top": ("9", [(0, 0, 12, 4, "8")]),
        "bottom": ("7", []),
        "right": ("8", [(0, 12, 10, 18, "7")]),
        "left": ("8", [(0, 12, 10, 18, "7")]),
        "front": ("8", [(0, 0, 12, 3, "9"), (0, 13, 12, 18, "7")]),
        "back": ("8", [(0, 13, 12, 18, "7")]),
    },
    "leg": "7",
}

CHICKEN_MARANS = {
    "head": {
        "top": ("n", []), "bottom": ("c", []), "right": ("n", []), "left": ("n", []),
        "back": ("c", []), "front": ("n", [(0, 4, 4, 6, "c")]),
    },
    "body": {
        "top": ("n", []), "bottom": ("c", []), "right": ("n", [(0, 4, 6, 8, "c")]),
        "left": ("n", [(0, 4, 6, 8, "c")]), "front": ("n", [(0, 6, 6, 8, "c")]),
        "back": ("K", [(0, 0, 6, 2, "c")]),
    },
    "wing": "c",
}

CHICKEN_LEGHORN = {
    "head": {
        "top": ("A", []), "bottom": ("y", []), "right": ("z", []), "left": ("z", []),
        "back": ("z", []), "front": ("A", []),
    },
    "body": {
        "top": ("A", []), "bottom": ("y", []), "right": ("z", []), "left": ("z", []),
        "front": ("A", []), "back": ("z", []),
    },
    "wing": "z",
}

CHICKEN_SUSSEX = {
    "head": {
        "top": ("A", []), "bottom": ("K", []), "right": ("A", [(0, 3, 3, 6, "K")]),
        "left": ("A", [(0, 3, 3, 6, "K")]), "back": ("A", [(0, 3, 4, 6, "K")]),
        "front": ("A", [(0, 3, 4, 6, "K")]),
    },
    "body": {
        "top": ("A", []), "bottom": ("z", []), "right": ("A", []), "left": ("A", []),
        "front": ("A", [(0, 0, 6, 3, "K")]), "back": ("A", [(0, 0, 6, 3, "K")]),
    },
    "wing": "z",
}


def _paint_face(px, rect, base, marks, palette):
    u, v, w, h = rect
    for y in range(h):
        for x in range(w):
            px[u + x, v + y] = palette[base] + (255,)
    for x0, y0, x1, y1, ch in marks:
        for y in range(y0, y1):
            for x in range(x0, x1):
                px[u + x, v + y] = palette[ch] + (255,)


def _paint_model(px, boxes, spec, palette, part):
    for face, rect in boxes.items():
        base, marks = spec[part][face]
        _paint_face(px, rect, base, marks, palette)


def _paint_uniform(px, boxes, ch, palette):
    for rect in boxes.values():
        _paint_face(px, rect, ch, [], palette)


def _cow(spec, palette):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    _paint_model(px, COW_HEAD, spec, palette, "head")
    _paint_model(px, COW_BODY, spec, palette, "body")
    _paint_uniform(px, COW_LEG, spec["leg"], palette)
    return img


def _chicken(spec, palette):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    _paint_model(px, CHICKEN_HEAD, spec, palette, "head")
    _paint_model(px, CHICKEN_BODY, spec, palette, "body")
    _paint_uniform(px, CHICKEN_WING, spec["wing"], palette)
    return img


def build_breed_entities(palette):
    """Superpositions de race (64x32, UV vanilla) : liste de (nom, image)."""
    return [
        ("cow_holstein", _cow(COW_HOLSTEIN, palette)),
        ("cow_highland", _cow(COW_HIGHLAND, palette)),
        ("cow_charolais", _cow(COW_CHAROLAIS, palette)),
        ("chicken_marans", _chicken(CHICKEN_MARANS, palette)),
        ("chicken_leghorn", _chicken(CHICKEN_LEGHORN, palette)),
        ("chicken_sussex", _chicken(CHICKEN_SUSSEX, palette)),
    ]
