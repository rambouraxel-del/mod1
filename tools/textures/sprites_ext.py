"""Sprites d'extension de Terres Vivantes: vergers, fruits, plats, eau, sante.

SPRITES_EXT: liste de (dossier, nom, grille), meme format que sprites.SPRITES.

Feuillages: demi-canevas de 8 colonnes dessine a la main, mis en miroir
horizontal pour obtenir 16 colonnes. Les fruits et fleurs sont poses sur le
demi-canevas avant miroir (placement symetrique, comme un vrai arbre).
Couleurs: palette partagee de generate_all.py (les caracteres ajoutes pour
les vergers et la sante y sont definis).
"""

from sprites import ITEM_STRAWBERRY_JAM, ITEM_CORN_BREAD, _bowl, _recolor, _overlay


def G(text):
    return [line.replace(" ", ".") for line in text.strip("\n").splitlines()]


# ---------------------------------------------------------------------------
# outils de composition
# ---------------------------------------------------------------------------

def _mirror(half):
    """Demi-canevas 8 colonnes -> 16 colonnes (miroir horizontal)."""
    return [r + r[::-1] for r in half]


def _mark(half, marks):
    """Pose des caracteres (x, y) sur un demi-canevas 8x16."""
    rows = [list(r) for r in half]
    for (x, y), ch in marks.items():
        rows[y][x] = ch
    return ["".join(r) for r in rows]


def _blob(x, y, w, h, base, hi, dark):
    """Petit fruit: haut-gauche clair, bord bas/droit sombre."""
    out = {}
    for j in range(h):
        for i in range(w):
            c = base
            if i == 0 and j == 0:
                c = hi
            elif (i == w - 1 and j == h - 1) or (w > 2 and (i == w - 1 or j == h - 1)):
                c = dark
            out[(x + i, y + j)] = c
    return out


def _merge(*dicts):
    out = {}
    for d in dicts:
        out.update(d)
    return out


def _dots(points):
    """Fleurs: liste de (x, y, caractere)."""
    return {(x, y): c for x, y, c in points}


# ---------------------------------------------------------------------------
# VERGERS: demi-canevas par espece (feuillage colore directement)
# ---------------------------------------------------------------------------

# Pommier: vert moyen, quelques ombres foncees et reflets clairs.
APPLE_HALF = [
    "23434332", "34354343", "43343434", "34434323",
    "33443434", "34343343", "24334432", "43434334",
    "34343434", "33443343", "43434332", "34334343",
    "43434332", "34323433", "32433443", "22332332",
]

# Cerisier: vert vif, plus lumineux, reflets 6 ponctuels.
CHERRY_HALF = [
    "34534343", "43435343", "35434634", "43343443",
    "34543434", "43434353", "35343434", "43534343",
    "34343543", "43434343", "53434334", "34354343",
    "43434354", "34345343", "43535443", "33434334",
]

# Oranger: vert fonce brillant, reflets glacés en 5.
ORANGE_HALF = [
    "21232122", "12313212", "21251231", "13212313",
    "21231221", "12313121", "22121312", "13212132",
    "21231221", "12313123", "21212312", "12312121",
    "21213212", "12123121", "21312212", "11212112",
]

# Olivier: gris-vert (&=sombre, @=moyen, %=clair, ==reflet).
OLIVE_HALF = [
    "@&@%@&@@", "&@%@@&@%", "@&@=%@&@", "%@&@@%@&",
    "@&@%@&@@", "&@@%@&%@", "@&%@&@@&", "@%@&@@%@",
    "&@&@%@&@", "@&@%@&@@", "%@&@&@%@", "@&@@%@&@",
    "&@%@&@@&", "@&@%@&@@", "&@&@@%@&", "@&&@&@&&",
]

# Noyer: vert sombre, feuillage dense.
WALNUT_HALF = [
    "12321231", "21232132", "13121323", "21312131",
    "12234123", "31212313", "21321232", "12312141",
    "21232123", "13121323", "21312312", "12321231",
    "21231321", "12131221", "21212312", "12122211",
]

# Pecher: vert clair jaune, feuilles plus pales.
PEACH_HALF = [
    "45434543", "54645454", "43454645", "54345434",
    "45454354", "43546454", "54343546", "45464543",
    "34545434", "54346454", "43454543", "54545434",
    "45434645", "43545354", "54354543", "34354434",
]

# Pousse: tige centrale + deux paires de feuilles (chiffres = teintes a recolorer).
SAPLING_MASTER = G("""
................
................
................
................
......2222......
.....134431.....
....13444431....
....13444431....
.....134431.....
......1331......
.......77.......
.......77.......
.......87.......
.......77.......
................
................
""")

# Correspondance des teintes de la pousse par espece.
SAPLING_MAP = {
    "apple": {},
    "cherry": {},
    "orange": {"1": "1", "2": "2", "3": "2", "4": "3", "5": "4"},
    "olive": {"1": "&", "2": "&", "3": "@", "4": "%", "5": "="},
    "walnut": {"1": "1", "2": "2", "3": "3", "4": "3", "5": "4"},
    "peach": {"1": "3", "2": "4", "3": "4", "4": "5", "5": "6"},
}


def _sapling(species):
    return _recolor(SAPLING_MASTER, SAPLING_MAP[species])


# Fleurs (blanc ou rose selon l'espece), fruits verts et fruits murs.
# Positions sur le demi-canevas (x 0..7, y 0..15).

APPLE_FLOWERS = _dots([
    (1, 2, "$"), (2, 2, "-"), (5, 4, "$"), (4, 4, "-"), (3, 7, "$"),
    (1, 9, "$"), (2, 9, "-"), (6, 10, "$"), (3, 12, "$"), (5, 13, "$"),
    (1, 14, "-"), (0, 6, "$"),
])
APPLE_UNRIPE = _merge(
    _blob(2, 4, 2, 2, "4", "5", "2"), _blob(5, 7, 2, 2, "4", "5", "2"),
    _blob(1, 11, 2, 2, "4", "5", "2"), _blob(4, 13, 2, 2, "4", "5", "2"),
)
APPLE_RIPE = _merge(
    _blob(1, 2, 3, 3, "e", "g", "c"), _blob(4, 6, 3, 3, "e", "g", "c"),
    _blob(1, 10, 3, 3, "e", "g", "c"), _blob(3, 13, 3, 3, "e", "g", "c"),
)

CHERRY_FLOWERS = _dots([
    (2, 3, "A"), (3, 3, "z"), (6, 5, "A"), (1, 8, "A"), (2, 8, "z"),
    (5, 10, "A"), (4, 12, "z"), (6, 13, "A"), (0, 11, "A"),
])
CHERRY_UNRIPE = _merge(
    _blob(2, 6, 2, 2, "4", "5", "3"), _blob(5, 9, 2, 2, "4", "5", "3"),
    _blob(1, 12, 2, 2, "4", "5", "3"),
)
CHERRY_RIPE = _merge(
    _blob(1, 4, 2, 2, "d", "f", "c"), _blob(3, 6, 2, 2, "d", "f", "c"),
    _blob(5, 10, 2, 2, "d", "f", "c"), _blob(2, 11, 2, 2, "d", "f", "c"),
    _blob(4, 13, 2, 2, "d", "f", "c"),
)

ORANGE_FLOWERS = _dots([
    (1, 3, "A"), (2, 3, "z"), (4, 6, "A"), (6, 5, "A"), (0, 7, "A"),
    (2, 9, "A"), (3, 9, "z"), (5, 12, "A"), (1, 13, "z"),
])
ORANGE_UNRIPE = _merge(
    _blob(3, 5, 2, 2, "4", "5", "3"), _blob(1, 10, 2, 2, "4", "5", "3"),
    _blob(4, 13, 2, 2, "4", "5", "3"),
)
ORANGE_RIPE = _merge(
    _blob(1, 2, 3, 3, "o", "q", "n"), _blob(4, 6, 3, 3, "o", "q", "n"),
    _blob(1, 10, 3, 3, "o", "q", "n"), _blob(4, 13, 3, 3, "o", "q", "n"),
)

OLIVE_FLOWERS = _dots([
    (2, 4, "z"), (5, 6, "z"), (1, 9, "y"), (4, 11, "z"), (6, 13, "z"),
    (2, 14, "y"),
])
OLIVE_UNRIPE = _merge(
    _blob(2, 5, 2, 2, "%", "=", "&"), _blob(4, 9, 2, 2, "%", "=", "&"),
    _blob(1, 12, 2, 2, "%", "=", "&"),
)
OLIVE_RIPE = _merge(
    _blob(2, 3, 2, 2, "r", "s", "K"), _blob(5, 7, 2, 2, "r", "s", "K"),
    _blob(1, 10, 2, 2, "r", "s", "K"), _blob(4, 13, 2, 2, "r", "s", "K"),
)

WALNUT_FLOWERS = _dots([
    (1, 2, "y"), (3, 5, "z"), (6, 7, "y"), (2, 10, "z"), (5, 13, "y"),
    (1, 14, "z"),
])
WALNUT_UNRIPE = _merge(
    _blob(2, 4, 2, 2, "4", "5", "3"), _blob(5, 8, 2, 2, "4", "5", "3"),
    _blob(1, 12, 2, 2, "4", "5", "3"),
)
WALNUT_RIPE = _merge(
    _blob(1, 3, 3, 3, "8", "0", "7"), _blob(4, 9, 3, 3, "8", "0", "7"),
    _blob(1, 12, 3, 3, "8", "0", "7"),
)

PEACH_FLOWERS = _dots([
    (1, 3, "$"), (2, 3, "-"), (5, 5, "$"), (6, 5, "-"), (3, 9, "$"),
    (4, 9, "-"), (1, 12, "+"), (2, 12, "$"), (5, 13, "$"), (6, 13, "-"),
])
PEACH_UNRIPE = _merge(
    _blob(2, 6, 2, 2, "5", "6", "4"), _blob(5, 10, 2, 2, "5", "6", "4"),
    _blob(1, 13, 2, 2, "5", "6", "4"),
)
PEACH_RIPE = _merge(
    _blob(1, 3, 3, 3, "g", "h", "p"), _blob(4, 8, 3, 3, "g", "h", "p"),
    _blob(2, 12, 3, 3, "g", "h", "p"),
)

# ---------------------------------------------------------------------------
# FRUITS (items, 16x16 dessines a la main)
# ---------------------------------------------------------------------------

ITEM_CHERRY = G("""
................
................
........7..33...
.......7....3...
......7.........
.....7...ffff...
...hfdd.fdddddd.
..hfdddd.dhdddd.
..hfddcc..dddc..
..hfdccc..dddcc.
...dccc....ccc..
....ccc.........
................
................
................
................
""")

ITEM_WALNUT = G("""
................
................
................
................
................
......77........
.....7887.......
....788098......
...78898897.....
...7089998......
...78999887.....
....778887......
......77........
................
................
................
""")

# ---------------------------------------------------------------------------
# PLATS
# ---------------------------------------------------------------------------

ITEM_CHERRY_PIE = G("""
................
................
.....iiiiii.....
...iiklmmkii....
..iklmllmllkii..
..ilmcdllddllli.
..ilmcdddddllmi.
..ilmlddddmllki.
..ilmlllmlllmki.
..iklmllmlllkii.
...iilllmmklii..
.....iiiiii.....
................
................
................
................
""")


ITEM_WALNUT_BREAD = _overlay(_recolor(ITEM_CORN_BREAD, {
    "i": "7", "l": "9", "m": "0", "k": "8", "z": "a",
}), {
    (6, 5): "7", (7, 5): "8", (10, 6): "7", (5, 8): "8", (9, 8): "7",
    (6, 9): "7", (8, 9): "8",
})

ITEM_PEACH_JAM = _recolor(ITEM_STRAWBERRY_JAM, {
    "c": "o", "d": "n", "e": "o", "f": "q", "g": "h",
})

# ---------------------------------------------------------------------------
# EAU: arroseur (cuivre, fer), cuve de pluie
# ---------------------------------------------------------------------------

BLOCK_RAIN_BARREL_SIDE = G("""
7887878898878887
7887878898878887
7999999999999997
7887878898878887
7987878898878887
7987878898878887
7887878898878887
CDDDDDDDDDDDDDDC
7887878898878887
7987878898878887
7887878898878887
7999999999999997
7887878898878887
7887878898878887
CDDDDDDDDDDDDDDC
7887878898878887
""")

BLOCK_RAIN_BARREL_TOP = G("""
7777777777777777
7898989898989897
7989999999999897
7899000000009987
7899099999909987
7890999999990987
7890999999990987
7890999999990987
7890999999990987
7890999999990987
7899099999909987
7899000000009987
7989999999999897
7898989898989897
7777777777777777
7777777777777777
""")

BLOCK_RAIN_BARREL_TOP_WATER = G("""
7777777777777777
7898989898989897
7989999999999897
7899000000009987
7899GHIJJIHG9987
7890GIJJJJIG0987
7890HJJIIJJH0987
7890IJJIIJJI0987
7890JJJJJJJJ0987
7890JIIJJIIJ0987
7899GHJJJJHG9987
7899000000009987
7989999999999897
7898989898989897
7777777777777777
7777777777777777
""")

# ---------------------------------------------------------------------------
# SANTE: fongicide, pulverisateur, spore
# ---------------------------------------------------------------------------

ITEM_FUNGICIDE = G("""
................
......BBBB......
......BCCB......
.......VV.......
.......VV.......
......VVVV......
.....V|((V......
.....V|((V......
....V|((()V.....
....V|(())V.....
....V|(())V.....
....V|((())V....
....V|(()))V....
.....VVVVVV.....
................
................
""")

PARTICLE_SPORE = G("""
................
................
................
................
.....mmmm.......
....mzzzzm......
....mzAzzl......
....mzzzzl......
....mlzzzl......
.....llll.......
................
................
................
................
................
................
""")

def _r(indent, body):
    """Ligne de 16 caracteres: indentation, corps, puis transparence."""
    row = "." * indent + body
    assert len(row) <= 16, row
    return row.ljust(16, ".")


ITEM_ORANGE_JUICE = [
    _r(0, ""), _r(3, "V" * 10), _r(3, "V" + "U" * 8 + "V"), _r(4, "VqUUUUUV"),
    _r(4, "VoooooV"), _r(4, "VoooooV"), _r(4, "VoqqooV"), _r(4, "VoooppV"),
    _r(4, "VoooooV"), _r(4, "VoooooV"), _r(4, "VnnnnnnV"), _r(4, "V" * 8),
    _r(0, ""), _r(0, ""), _r(0, ""), _r(0, ""),
]


def _plate(edge, hi, mid, lo, ring, nozzle, hole):
    """Plaque carree 16x16 avec buse centrale (ombre en bas a droite)."""
    g = [[mid] * 16 for _ in range(16)]
    for i in range(16):
        g[0][i] = g[15][i] = g[i][0] = g[i][15] = edge
    for i in range(1, 15):
        g[1][i] = g[i][1] = hi
        g[14][i] = g[i][14] = lo
    for y in range(5, 11):
        for x in range(5, 11):
            g[y][x] = ring
    for y in range(6, 10):
        for x in range(6, 10):
            g[y][x] = nozzle
    for y in (7, 8):
        for x in (7, 8):
            g[y][x] = hole
    return ["".join(r) for r in g]


ITEM_ORANGE = [
    _r(0, ""), _r(7, "7"), _r(7, "7..33"), _r(6, "nnnnnn"),
    _r(4, "nnqqoonn"), _r(3, "nqqoooooon"),
    _r(2, "nq" + "o" * 8 + "pn"), _r(2, "nq" + "o" * 8 + "pn"),
    _r(2, "n" + "o" * 9 + "pn"), _r(2, "n" + "o" * 9 + "pn"),
    _r(2, "n" + "o" * 8 + "ppn"), _r(3, "n" + "o" * 7 + "pn"),
    _r(4, "n" + "o" * 5 + "pn"), _r(6, "nnppnn"), _r(0, ""), _r(0, ""),
]

ITEM_OLIVE = [
    _r(0, ""), _r(0, ""), _r(7, "7"), _r(7, "&&"), _r(6, "&%@&"),
    _r(5, "&%@@@&"), _r(4, "&%@@@@&"), _r(4, "&%@@@@&"), _r(4, "&%@@@@&"),
    _r(4, "&&@@@&&"), _r(5, "&&@&&"), _r(6, "&&&&"), _r(0, ""), _r(0, ""),
    _r(0, ""), _r(0, ""),
]

ITEM_PEACH = [
    _r(0, ""), _r(0, ""), _r(0, ""), _r(7, "4"), _r(7, "3.44"),
    _r(5, "hhhhhhhh"), _r(3, "hgppppppgh"), _r(2, "hgppppppppgh"),
    _r(2, "h" + "p" * 10 + "g"), _r(2, "g" + "p" * 10 + "g"),
    _r(2, "g" + "p" * 10 + "g"), _r(3, "g" + "p" * 8 + "gg"),
    _r(4, "g" + "p" * 6 + "gg"), _r(5, "ggggggg"), _r(0, ""), _r(0, ""),
]

ITEM_SPRAYER = G("""
................
..##########....
DD#CEECCCCC#....
DD##########....
.....#BBB#.##...
.....#BBB#.#B...
V..###########..
.V.#JJJIIIIHH#..
..V#JFJIIIIHH#..
V..#JJAAAAAHH#..
...#JJzjjzzHH#..
...#JJzkzzzHH#..
...#JJzzzzzHH#..
...#JJJIIIIHH#..
...#JJJIIIIHH#..
...###########..
""")

BLOCK_COPPER_SPRINKLER = G("""
nnnnnnn77nnnnnnn
nooooonppnoooonn
nooooonppnoooonn
nooooonppnoooonn
noonqqqqqqqnnonn
nonnqqooooqnnnnn
nnnnqooooonnnnnn
nqnnqooooonnnnnp
npnnqooooonnnnnp
nnnnqooooonnnnnn
nonnqqooooqnnnnn
noonnnnnnnnnnonn
nooooooooooooonn
nooooooooooooonn
nnnnnnnnnnnnnnnn
nnnnnnnnnnnnnnnn
""")
BLOCK_IRON_SPRINKLER = G("""
BBBBBBB77BBBBBBB
BDDDDDBEEBDDDDCB
BDDDDDBEEBDDDDCB
BDDDDDBEEBDDDDCB
BDDBFFFFFFFCBDCB
BDBBFFDDDDFCBBCB
BBBBFDDDDDCCBBCB
CFBBFDDDDDCCBBCE
CEBBFDDDDDCCBBCE
BBBBFDDDDDCCBBCB
BDBBFFDDDDFCBBCB
BDDBBBBBBBBCBDCB
BDDDDDDDDDDDDDCB
BDDDDDDDDDDDDDCB
BCCCCCCCCCCCCCCB
BBBBBBBBBBBBBBBB
""")

ITEM_FRUIT_SALAD = _bowl([
    _r(3, "ec4c3e2qc"),
    _r(2, "qcq3ce4q4cn"),
    _r(3, "c4n3q2c1l"),
    _r(4, "4lo2"),
])


# ---------------------------------------------------------------------------
# registre
# ---------------------------------------------------------------------------

_LEAF_SPECIES = {
    "apple": (APPLE_HALF, APPLE_FLOWERS, APPLE_UNRIPE, APPLE_RIPE),
    "cherry": (CHERRY_HALF, CHERRY_FLOWERS, CHERRY_UNRIPE, CHERRY_RIPE),
    "orange": (ORANGE_HALF, ORANGE_FLOWERS, ORANGE_UNRIPE, ORANGE_RIPE),
    "olive": (OLIVE_HALF, OLIVE_FLOWERS, OLIVE_UNRIPE, OLIVE_RIPE),
    "walnut": (WALNUT_HALF, WALNUT_FLOWERS, WALNUT_UNRIPE, WALNUT_RIPE),
    "peach": (PEACH_HALF, PEACH_FLOWERS, PEACH_UNRIPE, PEACH_RIPE),
}

SPRITES_EXT = []
for _sp, (_half, _fl, _un, _ri) in _LEAF_SPECIES.items():
    SPRITES_EXT.append(("block", f"{_sp}_sapling", _sapling(_sp)))
    SPRITES_EXT.append(("block", f"{_sp}_leaves", _mirror(_half)))
    SPRITES_EXT.append(("block", f"{_sp}_leaves_flowering", _mirror(_mark(_half, _fl))))
    SPRITES_EXT.append(("block", f"{_sp}_leaves_unripe", _mirror(_mark(_half, _un))))
    SPRITES_EXT.append(("block", f"{_sp}_leaves_ripe", _mirror(_mark(_half, _ri))))

for _name, _grid in [
    ("cherry", ITEM_CHERRY),
    ("orange", ITEM_ORANGE),
    ("olive", ITEM_OLIVE),
    ("walnut", ITEM_WALNUT),
    ("peach", ITEM_PEACH),
    ("cherry_pie", ITEM_CHERRY_PIE),
    ("orange_juice", ITEM_ORANGE_JUICE),
    ("fruit_salad", ITEM_FRUIT_SALAD),
    ("walnut_bread", ITEM_WALNUT_BREAD),
    ("peach_jam", ITEM_PEACH_JAM),
    ("fungicide", ITEM_FUNGICIDE),
    ("sprayer", ITEM_SPRAYER),
]:
    SPRITES_EXT.append(("item", _name, _grid))

for _name, _grid in [
    ("copper_sprinkler", BLOCK_COPPER_SPRINKLER),
    ("iron_sprinkler", BLOCK_IRON_SPRINKLER),
    ("rain_barrel_side", BLOCK_RAIN_BARREL_SIDE),
    ("rain_barrel_top", BLOCK_RAIN_BARREL_TOP),
    ("rain_barrel_top_water", BLOCK_RAIN_BARREL_TOP_WATER),
]:
    SPRITES_EXT.append(("block", _name, _grid))

SPRITES_EXT.append(("particle", "spore", PARTICLE_SPORE))
