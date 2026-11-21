"""Sprites v0.6 de Terres Vivantes: grelinette, semoir, faux diamant, plume de corbeau,
machines (moissonneuse, semoir a graines, silo, caisse), epouvantail, entite corbeau.

SPRITES_V06: liste de (dossier, nom, grille), meme format que sprites.SPRITES.
Grilles 16x16 dessinees a la main, caractere par caractere, palette partagee
de generate_all.py (aucune nouvelle couleur). Lumiere venant du haut-gauche.
Les grilles de blocs sont ecrites par groupes de 4 caracteres: H() retire les espaces.

Entite corbeau (64x32): build_crow_entities(palette) reutilise exactement le
placement UV du canard (sprites_v04._paint_box, meme boites que le canard).
"""

from sprites import G, ITEM_SICKLE
from sprites_v04 import _paint_box


def H(text):
    """Bloc multiligne de groupes -> liste de lignes (espaces retires)."""
    return ["".join(line.split()) for line in text.strip("\n").splitlines() if line.strip()]


def _recolor(rows, mapping):
    return ["".join(mapping.get(ch, ch) for ch in row) for row in rows]


# ---------------------------------------------------------------------------
# OUTILS ET ITEMS
# ---------------------------------------------------------------------------

# Grelinette: deux manches de bois en diagonale, cadre horizontal et 4 dents.
ITEM_BROADFORK = G("""
.............9.8
............9.8.
...........9.8..
..........9.8...
.........9.8....
........9.8.....
.......9.8......
#EEEEEEEE#......
.E.E.E.E........
.E.E.E.E........
.E.E.E.E........
.E.E.E.E........
.#.#.#.#........
................
................
................
""")

# Semoir manuel: tremie de bois remplie de graines, roue a rayons en bas a droite.
ITEM_SEEDER = G("""
................
.#######........
.#xxxxx#........
..#xxx#.........
..#888#.........
...#8#.E..##....
....##E.######..
.....E.#CCDDCC#.
.....E#CDCDDCDC#
......#CCDFFDCC#
.....##DDFFFFDD#
.....##DDFFFFDD#
......#CCDFFDCC#
......#CDCDDCDC#
.......#CCDDCC#.
........######..
""")

# Faux diamant: meme gabarit que la faucille, lame en bleu-blanc.
ITEM_DIAMOND_SICKLE = _recolor(ITEM_SICKLE, {"B": "G", "E": "I", "F": "U"})

# Plume de corbeau: noire, reflets bleus sur le bord droit, rachis clair.
ITEM_CROW_FEATHER = G("""
...........#I#..
..........#IHH#.
.........#IHBBG#
........#IHBCBG#
.......#IHBCCBG#
......#IHBBCCBG#
.....#IHBBCDCBG#
....#IHBBCDDCBG#
...#IHBBCDCBBG#.
..#HBBCDBBBG#...
.#BBCDBBBG#.....
.#BCDBBB#.......
..#BCD#.........
...#CD#.........
....#D#.........
................
""")

ITEMS_V06 = [
    ("broadfork", ITEM_BROADFORK),
    ("seeder", ITEM_SEEDER),
    ("diamond_sickle", ITEM_DIAMOND_SICKLE),
    ("crow_feather", ITEM_CROW_FEATHER),
]


# ---------------------------------------------------------------------------
# MACHINES (blocs, faces pleines)
# ---------------------------------------------------------------------------

# Moissonneuse: face avant en planches, trappe de fer. Version inactive: trappe fermee.
HARVESTER_FRONT = H("""
BBBB BBBB BBBB BBBB
BCCC CCCC CCCC CCCB
BC99 9999 9999 99CB
BC00 0000 0000 00CB
BC77 7777 7777 77CB
BC88 8CCC CCC8 88CB
BC88 8CCC CCC8 88CB
BC88 8CCC CCC8 88CB
BC88 8CCC CCC8 88CB
BC88 8CCC CCC8 88CB
BC88 8CCC CCC8 88CB
BC77 7777 7777 77CB
BC99 9999 9999 99CB
BC00 0000 0000 00CB
BCCC CCCC CCCC CCCB
BBBB BBBB BBBB BBBB
""")

# Version active: la trappe s'ouvre, les lames sont visibles (alternance E / B).
HARVESTER_FRONT_ACTIVE = H("""
BBBB BBBB BBBB BBBB
BCCC CCCC CCCC CCCB
BC99 9999 9999 99CB
BC00 0000 0000 00CB
BC77 7777 7777 77CB
BC88 8CCC CCC8 88CB
BC88 8BBB BBB8 88CB
BC88 8EEE EEE8 88CB
BC88 8BBB BBB8 88CB
BC88 8EEE EEE8 88CB
BC88 8CCC CCC8 88CB
BC77 7777 7777 77CB
BC99 9999 9999 99CB
BC00 0000 0000 00CB
BCCC CCCC CCCC CCCB
BBBB BBBB BBBB BBBB
""")

# Moissonneuse, cote: planches horizontales, montants de fer verticaux.
HARVESTER_SIDE = H("""
CCCC CCCC CCCC CCCC
999D 9999 9999 D999
888D 8888 8888 D888
777D 7777 7777 D777
999D 9999 9999 D999
999D 9999 9999 D999
888D 8888 8888 D888
777D 7777 7777 D777
999D 9999 9999 D999
888D 8888 8888 D888
999D 9999 9999 D999
777D 7777 7777 D777
999D 9999 9999 D999
888D 8888 8888 D888
BBBB BBBB BBBB BBBB
BBBB BBBB BBBB BBBB
""")

# Moissonneuse, dessus: cadre de bois, plaque de fer, lames en croix.
HARVESTER_TOP = H("""
7777 7777 7777 7777
9999 9999 9999 9999
9CCC CCCC CCCC CCC9
9CDD DDDE EDDD DDC9
9CDD DDDE EDDD DDC9
9CDD DDDE EDDD DDC9
9CDD DDDE EDDD DDC9
9CEE EEEB BEEE EEC9
9CEE EEEB BEEE EEC9
9CDD DDDE EDDD DDC9
9CDD DDDE EDDD DDC9
9CDD DDDE EDDD DDC9
9CDD DDDE EDDD DDC9
9CCC CCCC CCCC CCC9
9999 9999 9999 9999
7777 7777 7777 7777
""")

# Semoir a graines, face avant: planches, fenetre de graines, bec de sortie en fer.
PLANTER_FRONT = H("""
8888 8888 8888 8888
9999 9999 9999 9999
9999 BBBB BBBB 9999
9999 Bxyx xyxB 9999
9999 Bxyx xyxB 9999
9999 Bxyx xyxB 9999
9999 BBBB BBBB 9999
9999 9999 9999 9999
7777 7777 7777 7777
9999 999B B999 9999
8888 8888 8888 8888
9999 9999 9999 9999
8888 8888 8888 8888
9999 9999 9999 9999
7777 7777 7777 7777
7777 7777 7777 7777
""")

# Semoir a graines, cote: planches verticales (joints sombres).
PLANTER_SIDE = H("""
BBBB BBBB BBBB BBBB
7998 7998 7998 7998
7998 7998 7998 7998
7098 7098 7098 7098
7998 7998 7998 7998
7998 7998 7998 7998
7098 7098 7098 7098
7998 7998 7998 7998
7998 7998 7998 7998
7098 7098 7098 7098
7998 7998 7998 7998
7998 7998 7998 7998
7098 7098 7098 7098
7998 7998 7998 7998
7998 7998 7998 7998
BBBB BBBB BBBB BBBB
""")

# Semoir a graines, dessus: trémie remplie de graines (creme, speckle).
PLANTER_TOP = H("""
8888 8888 8888 8888
9999 9999 9999 9999
9xyz xzyx yxzy xzx9
9zxy xyzx zyxy xyz9
9yxz wxyz xzyx zyx9
9xzy xyzx yzxy xzy9
9zyx xzyw xyzx yzx9
9yxz xyzx zxyz xzy9
9xzy yxzx xyzy zxy9
9zxy xyzx zyxw xzy9
9yzx xzyx yzxy xyz9
9xyz xzyx yxzx zxy9
9zxy yxzx xyzy xzy9
9yxz xzyx zyxy xyz9
9999 9999 9999 9999
8888 8888 8888 8888
""")

# Silo, paroi: toles verticales (colonnes ECDC) cerclees de fer.
SILO_SIDE = H("""
BBBB BBBB BBBB BBBB
ECDC ECDC ECDC ECDC
ECDC ECDC ECDC ECDC
ECDC ECDC ECDC ECDC
BBBB BBBB BBBB BBBB
ECDC ECDC ECDC ECDC
ECDC ECDC ECDC ECDC
ECDC ECDC ECDC ECDC
ECDC ECDC ECDC ECDC
BBBB BBBB BBBB BBBB
ECDC ECDC ECDC ECDC
ECDC ECDC ECDC ECDC
ECDC ECDC ECDC ECDC
BBBB BBBB BBBB BBBB
ECDC ECDC ECDC ECDC
BBBB BBBB BBBB BBBB
""")

# Silo, dessus: dome de tole, anneaux concentriques.
SILO_TOP = H("""
BBBB BBBB BBBB BBBB
BCCC CCCC CCCC CCCB
BCDD DDDD DDDD DDCB
BCDE EEEE EEEE EDCB
BCDE FFFF FFFF EDCB
BCDE FEEE EEEF EDCB
BCDE FEDD DDEF EDCB
BCDE FEDD DDEF EDCB
BCDE FEDD DDEF EDCB
BCDE FEDD DDEF EDCB
BCDE FEEE EEEF EDCB
BCDE FFFF FFFF EDCB
BCDE EEEE EEEE EDCB
BCDD DDDD DDDD DDCB
BCCC CCCC CCCC CCCB
BBBB BBBB BBBB BBBB
""")

# Silo, dessous: anneau de fer et trappe de vidange sombre.
SILO_BOTTOM = H("""
BBBB BBBB BBBB BBBB
BCCC CCCC CCCC CCCB
BCCC CCCC CCCC CCCB
BCCC CCCC CCCC CCCB
BCCC BBBB BBBB CCCB
BCCB KKKK KKKK BCCB
BCCB KKKK KKKK BCCB
BCCB KKKK KKKK BCCB
BCCB KKKK KKKK BCCB
BCCB KKKK KKKK BCCB
BCCB BBBB BBBB BCCB
BCCC CCCC CCCC CCCB
BCCC CCCC CCCC CCCB
BCCC CCCC CCCC CCCB
BCCC CCCC CCCC CCCB
BBBB BBBB BBBB BBBB
""")

# Caisse, cote: planches horizontales, clous aux angles.
CRATE_SIDE = H("""
7777 7777 7777 7777
0000 0000 0000 0000
99E9 9999 9999 E999
8888 8888 8888 8888
7777 7777 7777 7777
0000 0000 0000 0000
9999 9999 9999 9999
8888 8888 8888 8888
7777 7777 7777 7777
0000 0000 0000 0000
9999 9999 9999 9999
8888 8888 8888 8888
99E9 9999 9999 E999
8888 8888 8888 8888
7777 7777 7777 7777
0000 0000 0000 0000
""")

# Caisse, dessus: couvercle a planches verticales, traverse centrale.
CRATE_TOP = H("""
7098 7098 7098 7098
7098 7098 7098 7098
E098 7098 7098 709E
7098 7098 7098 7098
7098 7098 7098 7098
7888 8888 8888 8887
7098 7098 7098 7098
7098 7098 7098 7098
7098 7098 7098 7098
7098 7098 7098 7098
7888 8888 8888 8887
7098 7098 7098 7098
E098 7098 7098 709E
7098 7098 7098 7098
7098 7098 7098 7098
7098 7098 7098 7098
""")

# Caisse, face: planches et etiquette vierge creme au centre.
CRATE_FRONT = H("""
7777 7777 7777 7777
0000 0000 0000 0000
99E9 9999 9999 E999
8888 8888 8888 8888
7777 xxxx xxxx 7777
0000 xzzz zzzx 0000
9999 xzzz zzzx 9999
8888 xzzz zzzx 8888
7777 xxxx xxxx 7777
0000 0000 0000 0000
9999 9999 9999 9999
8888 8888 8888 8888
99E9 9999 9999 E999
8888 8888 8888 8888
7777 7777 7777 7777
0000 0000 0000 0000
""")


# ---------------------------------------------------------------------------
# EPOUVANTAIL
# ---------------------------------------------------------------------------

# Paille: brins dores et bruns, diagonales irregulieres.
SCARECROW_STRAW = H("""
TSTT SRTS TTSR TSTT
STSR TTSS RTTS STRT
TSRT TSTS STRT TSSR
RTSS TTRS TSTT RSTS
SKTT SRTS TTSK TSRT
TSRT TJTS RTST TSTK
TSST RTSS TJST RTTS
SRTT STRS TTSR TSTT
TTSK TSRT TSTT SRTS
RSTT SSTR KTTS TSRT
TSRT TSTS RTTJ SSTR
STTS RTSS TSTK TRST
SRTT TSRS TTSR STTK
TSSR TTSR STRS TTSS
RSTT SRTS STTR SSTR
TSTS RTTS SRTT TSRS
""")

# Sac de jute: toile beige, couture, visage cousu (yeux et bouche en points noirs).
SCARECROW_SACK = H("""
7777777777777777
7aabbaaa0aabaaa7
7a0aaabaaaaa0aa7
7aaayyyyyyaaaaa7
7aayyyyyyyyyyya7
7ayyKyKyyKyKyyy7
7ayyyKyyyyKyyyy7
7ayyKyKyyKyKyyy7
7ayyyyyyyyyyyyy7
7ayyKyKyKyKyyyy7
7aayyKyKyKyKyya7
7aaayyyyyyyyyya7
7a0a0a0a0a0a0aa7
7aaaaaaaaaaaaaa7
7777777777777777
7777777777777777
""")

# Chemise a carreaux usee: rouge et creme, quelques trous et reprises.
SCARECROW_CLOTH = H("""
eeee zzzz eeee zzzz
eeee zzzz eeee zzzz
eeee zzzz eeee zzzz
dddd zzzz eeee zzzz
zzzz eeee zzzz eeee
zzzz eeee zzzz eeee
zzzz eexz zzzz eeee
dddd dddd zzzz eeee
eeee zzzz eeee zzzz
eeee zzzz eeee zzzz
eeee zzzz eeee zzzz
dddd zzzz eeee zzzz
zzzz eeee zzzz eeee
zzzz eeee zzzz eeee
zzzz xeee zzzz eeee
zzzz eeee zzzz dddd
""")

# Poteau: bois vertical, fil dans le sens de la hauteur, un noeud.
SCARECROW_POST = H("""
8978 7989 8978 9879
9887 8798 7989 8789
8798 9878 9887 9978
7989 8789 8798 8897
8897 9879 7989 8788
9878 8798 9878 9789
8978 7989 8897 8979
9789 8978 7898 7897
8898 9787 8989 7878
7889 7898 9787 8978
9878 9889 8978 7989
8789 7897 9878 9887
9878 8979 7889 8798
7989 8788 9897 8979
8797 9878 7988 9789
9878 7897 8979 7887
""")

BLOCKS_V06 = [
    ("harvester_front", HARVESTER_FRONT),
    ("harvester_front_active", HARVESTER_FRONT_ACTIVE),
    ("harvester_side", HARVESTER_SIDE),
    ("harvester_top", HARVESTER_TOP),
    ("planter_front", PLANTER_FRONT),
    ("planter_side", PLANTER_SIDE),
    ("planter_top", PLANTER_TOP),
    ("silo_side", SILO_SIDE),
    ("silo_top", SILO_TOP),
    ("silo_bottom", SILO_BOTTOM),
    ("crate_side", CRATE_SIDE),
    ("crate_top", CRATE_TOP),
    ("crate_front", CRATE_FRONT),
    ("scarecrow_straw", SCARECROW_STRAW),
    ("scarecrow_sack", SCARECROW_SACK),
    ("scarecrow_cloth", SCARECROW_CLOTH),
    ("scarecrow_post", SCARECROW_POST),
]

SPRITES_V06 = [("item", n, g) for n, g in ITEMS_V06] + [("block", n, g) for n, g in BLOCKS_V06]


# ---------------------------------------------------------------------------
# ENTITE CORBEAU (64x32), meme atlas UV que le canard (sprites_v04.py)
# ---------------------------------------------------------------------------

# Corbeau: plumage noir (B), reflets bleus (G, H) sur le dessus et les flancs,
# bec gris fonce (C, D), pattes sombres, oeil clair (F) sur la tete.
CROW_STYLE = {
    "body": {
        "top": "G", "bottom": "B", "side_a": "B", "front": "B", "side_b": "B", "back": "B",
        "detail": lambda f, x, y, w, h: "H" if f in ("top", "side_a", "side_b") and (x + 2 * y) % 6 == 0 else None,
    },
    "head": {
        "top": "G", "bottom": "C", "side_a": "B", "front": "B", "side_b": "B", "back": "B",
        "detail": lambda f, x, y, w, h: (
            "F" if f in ("side_a", "side_b") and x == 1 and y == 2
            else "H" if f == "top" and (x + y) % 4 == 0
            else None
        ),
    },
    "bill": {
        "top": "D", "bottom": "B", "side_a": "C", "front": "C", "side_b": "C", "back": "C",
        "detail": None,
    },
    "legs": {
        "top": "C", "bottom": "#", "side_a": "B", "front": "B", "side_b": "B", "back": "B",
        "detail": None,
    },
    "wing": {
        "top": "G", "bottom": "B", "side_a": "B", "front": "H", "side_b": "B", "back": "H",
        "detail": lambda f, x, y, w, h: "H" if f in ("side_a", "side_b") and y % 2 == 0 else None,
    },
}


def build_crow_entities(palette):
    """Retourne [(nom, image 64x32)] pour le corbeau, avec le placement UV du canard."""
    from PIL import Image

    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    style = CROW_STYLE
    _paint_box(px, (0, 0, 6, 5, 8), style["body"], palette)
    _paint_box(px, (0, 16, 4, 5, 3), style["head"], palette)
    _paint_box(px, (16, 16, 4, 1, 2), style["bill"], palette)
    _paint_box(px, (46, 0, 3, 2, 1), style["legs"], palette)
    _paint_box(px, (46, 4, 3, 2, 1), style["legs"], palette)
    _paint_box(px, (30, 0, 1, 4, 6), style["wing"], palette)
    _paint_box(px, (30, 12, 1, 4, 6), style["wing"], palette)
    return [("crow", img)]
