"""Sprites v0.5 de Terres Vivantes: machines de transformation (meule, pressoir,
baratte, cuve a fromage, etagere d'affinage, tonneau de fermentation, sechoir,
fumoir, marmite), produits transformes (farines, huile, cidre, beurre, fromages,
conserves, charcuterie, plats cuisines) et la GUI de traitement.

SPRITES_V05: liste de (dossier, nom, grille), meme format que sprites.SPRITES.
Grilles 16x16 dessinees a la main, caractere par caractere, palette partagee
de generate_all.py (aucune nouvelle couleur). Lumiere venant du haut-gauche.
'.' = transparent. Les items sont dessines sur fond transparent, centres.

Les lignes d'item passent par _r(gauche, contenu): le reste de la ligne est
complete par '.' pour rester a 16 colonnes.

GUI (gui/processing.png, 256x256, panneau utile 176x166 en haut-gauche,
style coffre vanilla). Coordonnees = position de l'item (16x16) dans le GUI:
  - entree 2x2 (4 slots): (34,17) (52,17) (34,35) (52,35)
  - conteneur (1 slot):   (43,62)
  - fleche de progression vide: (79,34), 22x15, sprite plein (176,0) 22x15
  - slot de sortie 26x26: cadre en (111,30), item en (116,35)
  - inventaire joueur 3x9: x = 8 + 18*c, y = 84 + 18*r (r = 0..2)
  - barre d'action 9: x = 8 + 18*c, y = 142
Les cadres de slot sont dessines 1 px autour (18x18 pour un slot 16x16).
"""

from PIL import Image

from sprites import G


def _r(left=0, body=""):
    """Ligne d'item: `left` pixels transparents puis `body`, complete a 16."""
    row = "." * left + body
    return row + "." * (16 - len(row))


# ===========================================================================
# BLOCS: machines de transformation (16x16, faces laterales et dessus)
# ===========================================================================

# --- Meule de pierre ---------------------------------------------------------
# Dessus: disque de pierre (D, bords E clairs / C ombres) dans un carre C,
# trou central 4x4 (B) avec coins arrondis (C).
MILLSTONE_TOP = G("""
CCCCCCCCCCCCCCCC
CCCCCCCCCCCCCCCC
CCCCCEEDDDDCCCCC
CCCCEEDDDDDDCCCC
CCCEEDDDDDDDDCCC
CCEEDDDDDDDDDDCC
CCEEDDCBBCDDDDCC
CCEEDDBBBBDDDDCC
CCEEDDBBBBDDCCCC
CCEEDDCBBCDDCCCC
CCEDDDDDDDDCCCCC
CCCEDDDDDDDCCCCC
CCCCEDDDDDCCCCCC
CCCCCEDDDDCCCCCC
CCCCCCCCCCCCCCCC
CCCCCCCCCCCCCCCC
""")

# Cote: pierre en assises horizontales (joints C), ombre en bas, cerclage B.
MILLSTONE_SIDE = G("""
EEEEEEEEEEEEEEEE
DDDDDDDDDDDDDDDD
DCDDDDDDDCDDDDDD
DDDDCDDDDDDDDCDD
DDDDDDDDDDCDDDDD
DDDDDDDCDDDDDDDD
CCCCCCCCCCCCCCCC
DDDDDDDDDDDDDDDD
DDDDDDCDDDDDDDCD
DDDDDDDDDCDDDDDD
DDCDDDDDDDDDCDDD
DDDDDDDDDDDDDDDD
CCCCCCCCCCCCCCCC
DDDDDDDDDDDDDDDD
CDDDDDDDDCDDDDDD
BBBBBBBBBBBBBBBB
""")

# --- Pressoir a fruits: planches de bois + vis metal -------------------------
FRUIT_PRESS_SIDE = G("""
aaaaaaaaaaaaaaaa
0000000000000000
0900000000000090
0000000000000000
8888888888888888
aaaaaaaaaaaaaaaa
0000000000000000
0000CCCCCC000000
0000CDDDDC000000
0000CDDDDC000000
0000CCCCCC000000
0000000000000000
8888888888888888
7777777777777777
7777777777777777
8888888888888888
""")

# Dessus: planches en couronne (8/0), plateau central metal D avec vis E.
FRUIT_PRESS_TOP = G("""
8888888888888888
8aaaaaaaaaaaaaa8
8aaaaaaaaaaaaaa8
8aaaCCCCCCCCaaa8
8aaCDDDDDDDDCaa8
8aaCDDEEEEDDCaa8
8aaCDDEEEEDDCaa8
8aaCDDDDDDDDCaa8
8aaaCCCCCCCCaaa8
8aaaaaaaaaaaaaa8
8aaaaaaaaaaaaaa8
8888888888888888
7777777777777777
8888888888888888
0000000000000000
8888888888888888
""")

# --- Baratte: douelles verticales, cerclages metal, couvercle bois -----------
CHURN_SIDE = G("""
aaaaaaaaaaaaaaaa
9999999999999999
9000900090009000
9000900090009000
9000900090009000
DDDDDDDDDDDDDDDD
9000900090009000
9000900090009000
9000900090009000
9000900090009000
DDDDDDDDDDDDDDDD
9000900090009000
9000900090009000
9000900090009000
7777777777777777
7777777777777777
""")

CHURN_TOP = G("""
8888888888888888
8aaaaaaaaaaaaaa8
8a000000000000a8
8a000000000000a8
8a000077777700a8
8a000070000700a8
8a000070000700a8
8a000070000700a8
8a000070000700a8
8a000070000700a8
8a000077777700a8
8a000000000000a8
8a000000000000a8
8aaaaaaaaaaaaaa8
8888888888888888
8888888888888888
""")

# --- Cuve a fromage: cuivre (oranges n..q) cercle de rivets D --------------
CHEESE_VAT_SIDE = G("""
qqqqqqqqqqqqqqqq
pppppppppppppppp
oooooooooooooooo
nnnnnnnnnnnnnnnn
DDDDDDDDDDDDDDDD
oooooooooooooooo
pppppppppppppppp
oDoooooooooooDoo
oooooooooooooooo
nnnnnnnnnnnnnnnn
DDDDDDDDDDDDDDDD
oooooooooooooooo
pppppppppppppppp
oooooooooooooooo
nnnnnnnnnnnnnnnn
BBBBBBBBBBBBBBBB
""")

CHEESE_VAT_TOP = G("""
nnnnnnnnnnnnnnnn
npppppppppppppqn
npoooooooooooopn
npoAAAAAAAAAAopn
npoAAAAAAAAAAopn
npoAAAAAAAAAAopn
npoAAAAAAAAAAopn
npoAAAAAAAAAAopn
npoAAAAAAAAAAopn
npoAAAAAAAAAAopn
npoAAAAAAAAAAopn
npoooooooooooopn
npppppppppppppqn
nnnnnnnnnnnnnnnn
BBBBBBBBBBBBBBBB
BBBBBBBBBBBBBBBB
""")

CHEESE_VAT_TOP_MILK = G("""
nnnnnnnnnnnnnnnn
npppppppppppppqn
npoooooooooooopn
npozzzzzzzzzzopn
npozAAAAAAAAzopn
npozAzzzzzzAzopn
npozzzzzzzzzzopn
npozzzzyzzzzzopn
npozzzzzzzzzzopn
npozzzzzzzzzzopn
npozzyzzzzzzzopn
npozzzzzzzzzzopn
npppppppppppppqn
nnnnnnnnnnnnnnnn
BBBBBBBBBBBBBBBB
BBBBBBBBBBBBBBBB
""")

CHEESE_VAT_TOP_CURD = G("""
nnnnnnnnnnnnnnnn
npppppppppppppqn
npoooooooooooopn
npozzzzzzzzzzopn
npozzyyyzzyyzopn
npozzyLyzzzyzopn
npozzzzzzkkzzopn
npozzzkkmmkzzopn
npozzkmlmkzzzopn
npozyzkkmmkzzopn
npozzzzkkzzzzopn
npozzzzyzzzyzopn
npppppppppppppqn
nnnnnnnnnnnnnnnn
BBBBBBBBBBBBBBBB
BBBBBBBBBBBBBBBB
""")

# --- Etagere d'affinage: planches (8/9), tablettes horizontales ---------------
AGING_SHELF_SIDE = G("""
################
#88888888888888#
#99909999990999#
7777777777777777
################
#88888888888888#
#99909999990999#
7777777777777777
################
#88888888888888#
#99909999990999#
7777777777777777
################
#88888888888888#
#99909999990999#
7777777777777777
""")

AGING_SHELF_FRONT = G("""
################
#99999999999999#
#9############9#
#9#8888888888#9#
#9#8777777778#9#
#9#8799999978#9#
#9#8777777778#9#
#9#87777777j8#9#
#9#87777777k8#9#
#9#877aaaa778#9#
#9#8777777778#9#
#9#8777777778#9#
#9#8888888888#9#
#9############9#
#99999999999999#
################
""")

AGING_SHELF_FRONT_CHEESE = G("""
7777777777777777
8888888888888888
9999999999999999
9000000000000009
7777777777777777
7777777777777777
8888888888888888
9999999999999999
9000000000000009
7777777777777777
7777777777777777
8888888888888888
9999999999999999
9000000000000009
7777777777777777
7777777777777777
""")

# --- Tonneau de fermentation: douelles, cerclages D, bonde ------------------
FERMENTATION_BARREL_SIDE = G("""
aaaaaaaaaaaaaaaa
9999999999999999
9000900090009000
9000900090009000
DDDDDDDDDDDDDDDD
9000900090009000
9000900090009000
9000900090009000
DDDDDDDDDDDDDDDD
9000900090009000
9000900090009000
9000900090009000
DDDDDDDDDDDDDDDD
9000900090009000
7777777777777777
7777777777777777
""")

FERMENTATION_BARREL_TOP = G("""
8888888888888888
8aaaaaaaaaaaaaa8
8a000000000000a8
8a000000000000a8
8a000000770000a8
8a000077770000a8
8a000000770000a8
8a000000000000a8
8a000000000000a8
8a000000000000a8
8aaaaaaaaaaaaaa8
8888888888888888
8888888888888888
8888888888888888
8888888888888888
8888888888888888
""")

FERMENTATION_BARREL_TOP_ACTIVE = G("""
8888888888888888
8aaaaaaaaaaaaaa8
8a000000000000a8
8a0000A0000000a8
8a000000A00000a8
8a00000000A000a8
8a000077770000a8
8a000000000000a8
8a0000000000A0a8
8a000000000000a8
8aaaaaaaaaaaaaa8
8888888888888888
8888888888888888
8888888888888888
8888888888888888
8888888888888888
""")

# --- Sechoir: montants bois, barres horizontales, tissu pale ---------------
DRYING_RACK_FRONT = G("""
################
9999999999999999
#97777777777779#
#97777777777779#
#9############9#
#9#aaaaaaaaaa#9#
#9#aaaaaaaaaa#9#
#9############9#
#97777777777779#
#9############9#
#9#aaaaaaaaaa#9#
#9#aaaaaaaaaa#9#
#9############9#
#97777777777779#
#97777777777779#
#97777777777779#
""")

DRYING_RACK_SIDE = G("""
#99#77777777#99#
#99#77777777#99#
#99##########99#
#99#aaaaaaaa#99#
#99#88888888#99#
#99##########99#
#99#77777777#99#
#99#77777777#99#
#99##########99#
#99#aaaaaaaa#99#
#99#88888888#99#
#99##########99#
#99#77777777#99#
#99##########99#
#99#aaaaaaaa#99#
#99#88888888#99#
""")

DRYING_RACK_FRONT_FULL = G("""
7777777777777777
8888888888888888
........3.......
...3....3.......
..44.....5......
.4555...3.......
..3....3.3......
.......3........
7777777777777777
8888888888888888
7...............
7.yyyyyyyyyyyy.7
7.yyyyyyyyyyyy.7
7.yyyyyyyyyyyy.7
7777777777777777
8888888888888888
""")

# --- Fumoir: planches sombres, porte, braises, dessus avec cheminee ----------
SMOKEHOUSE_SIDE = G("""
7777777777777777
8888888888888888
8800000000000088
8800000000000088
8888888888888888
7777777777777777
8888888888888888
8800000000000088
8800000000000088
8888888888888888
7777777777777777
8888888888888888
8800000000000088
8800000000000088
8888888888888888
7777777777777777
""")

SMOKEHOUSE_FRONT = G("""
7777777777777777
8888888888888888
8800000000000088
8807777777777088
8807888888887088
8807888888887088
8807888888887088
8807888888887088
8807888888887088
8807888888887088
8807888888887088
8807888888887088
8807888888887088
8807777777777088
8800000000000088
8888888888888888
""")

SMOKEHOUSE_FRONT_LIT = G("""
7777777777777777
8888888888888888
8800000000000088
8807777777777088
8807888888887088
8807888888887088
8807888888887088
8807888888887088
8807888888887088
8807888888887088
8807888888887088
8807777777777088
8807777777777088
8807ekmkkpe77088
8807kjpjkkoo7708
8888888888888888
""")

SMOKEHOUSE_TOP = G("""
8888888888888888
8777777777777778
8777777777777778
8777777777777778
8777777777777778
8777BBBBBBBB7778
8777BDDDDDDB7778
8777BDCCCCDB7778
8777BDCCCCDB7778
8777BDDDDDDB7778
8777BBBBBBBB7778
8777777777777778
8777777777777778
8777777777777778
8888888888888888
8888888888888888
""")

# --- Marmite: fonte grise (B..E), bord clair, interieur sombre --------------
COOKING_POT_SIDE = G("""
EEEEEEEEEEEEEEEE
CCCCCCCCCCCCCCCC
DDDDDDDDDDDDDDDD
DDDDDDDDDDDDDDDD
DDDCCCCCCCCDDDDD
DDDDDDDDDDDDDDDD
DDDDDDDDDDDDDDDD
DDDDDDDDDDDDDDDD
DDDDDDDDDDDDDDDD
DDDDDDDDDDDDDDDD
DDDDDDDDDDDDDDDD
DDDDDDDDDDDDDDDD
CDDDDDDDDDDDDDDC
BCCCCCCCCCCCCCCB
BBBBBBBBBBBBBBBB
BBBBBBBBBBBBBBBB
""")

COOKING_POT_TOP = G("""
BBBBBBBBBBBBBBBB
BEEEEEEEEEEEEEEB
BEDDDDDDDDDDDDCB
BEDBBBBBBBBBBDCB
BEDBBBBBBBBBBDCB
BEDBBBBBBBBBBDCB
BEDBBBBBBBBBBDCB
BEDBBBBBBBBBBDCB
BEDBBBBBBBBBBDCB
BEDBBBBBBBBBBDCB
BEDBBBBBBBBBBDCB
BEDDDDDDDDDDDDCB
BECCCCCCCCCCCCCB
BCCCCCCCCCCCCCCB
BBBBBBBBBBBBBBBB
BBBBBBBBBBBBBBBB
""")

COOKING_POT_TOP_STEW = G("""
BBBBBBBBBBBBBBBB
BEEEEEEEEEEEEEEB
BEDDDDDDDDDDDDCB
BEDNNNNNNNNNNDCB
BEDNOOONNNNNNDCB
BEDNOLOONNNNNDCB
BEDNNOOO3NNNNDCB
BEDNNNO3343NNDCB
BEDNNN3436NNNDCB
BEDNNNNO3NNNNDCB
BEDNNNNNNNOONDCB
BEDNNNNNNNNNNDCB
BEDDDDDDDDDDDDCB
BECCCCCCCCCCCCCB
BBBBBBBBBBBBBBBB
BBBBBBBBBBBBBBBB
""")


# ===========================================================================
# ITEMS: 16x16 sur fond transparent
# ===========================================================================

WHEAT_FLOUR = [
    _r(3, "#####"), _r(2, "#zzzzz#"), _r(1, "#zyzzzyz#"), _r(1, "#zzzyzzz#"),
    _r(1, "#yzzzzzy#"), _r(1, "#zzyzzzz#"), _r(1, "#zzzzzyz#"), _r(1, "#zyzzzzz#"),
    _r(1, "#zzzzyzy#"), _r(1, "#yzzzzzz#"), _r(2, "#yyyyy#"), _r(3, "#####"),
]
BARLEY_FLOUR = [
    _r(3, "#####"), _r(2, "#bbbbb#"), _r(1, "#baabbab#"), _r(1, "#bbabbab#"),
    _r(1, "#babbbab#"), _r(1, "#bbbabbb#"), _r(1, "#bbabbab#"), _r(1, "#baabbbb#"),
    _r(1, "#bbabbab#"), _r(1, "#babbbb#"), _r(2, "#aaaaa#"), _r(3, "#####"),
]
CORN_MEAL = [
    _r(3, "#####"), _r(2, "#kkkkk#"), _r(1, "#kllkkllk#"), _r(1, "#kkllkkkl#"),
    _r(1, "#kklkkllk#"), _r(1, "#kkkkllkk#"), _r(1, "#kllkkkkl#"), _r(1, "#kkkllkkk#"),
    _r(1, "#kkllkkkk#"), _r(1, "#jjjjjjj#"), _r(2, "#jjjjj#"), _r(3, "#####"),
]
DOUGH = [
    _r(4, "######"), _r(3, "#yzzzy#"), _r(2, "#yzzzzzy#"), _r(2, "#yzyzzzy#"),
    _r(1, "#yzzzzzzyy#"), _r(1, "#yzzyzzzyy#"), _r(1, "#yyzzzzzyy#"), _r(1, "#yyyyyyyyy#"),
    _r(2, "#xxxxxxx#"), _r(4, "#####"),
]
OLIVE_OIL = [
    _r(6, "77"), _r(6, "7777"), _r(5, "######"), _r(5, "#UUUU#"),
    _r(5, "#UVVV#"), _r(4, "#UVVVVV#"), _r(4, "#UVVVVV#"), _r(4, "#UVVVVV#"),
    _r(4, "#UVVVVV#"), _r(4, "#UVVVVV#"), _r(4, "#UVVVVV#"), _r(4, "#######"),
]
CIDER = [
    _r(6, "88"), _r(6, "8888"), _r(5, "######"), _r(5, "#UUUU#"),
    _r(5, "#UVVV#"), _r(4, "#UkkkkV#"), _r(4, "#UkllkV#"), _r(4, "#UkkkkV#"),
    _r(4, "#UkkkkV#"), _r(4, "#UkkkkV#"), _r(4, "#UkkkkV#"), _r(4, "#######"),
]
VINEGAR = [
    _r(6, "88"), _r(6, "8888"), _r(5, "######"), _r(5, "#UUUU#"),
    _r(5, "#UVVV#"), _r(4, "#UjjjjV#"), _r(4, "#UjiijV#"), _r(4, "#UjjjjV#"),
    _r(4, "#UiiiiV#"), _r(4, "#UiiiiV#"), _r(4, "#UiiiiV#"), _r(4, "#######"),
]
BUTTER = [
    _r(2, "##########"), _r(1, "#kkkkkkkkkk#"), _r(1, "#kllllllllk#"),
    _r(1, "#klmmmmmmlk#"), _r(1, "#klmAAAAAmlk#"), _r(1, "#klmmmmmmlk#"),
    _r(1, "#kllllllllk#"), _r(1, "#kkkkkkkkkk#"), _r(1, "#jjjjjjjjjj#"),
    _r(2, "##########"),
]
CREAM = [
    _r(4, "##"), _r(3, "#UU#"), _r(2, "##UUUU##"), _r(2, "#UUVVVVU#"),
    _r(2, "#UAAAAAU#"), _r(2, "#UAAAAAU#"), _r(2, "#UzzzzzU#"), _r(2, "#UzzzzzU#"),
    _r(2, "#UzzzzzU#"), _r(2, "#UyyyyyU#"), _r(2, "#UyyyyyU#"), _r(2, "########"),
]
CHEESE_CURD = [
    _r(3, "####"), _r(2, "#zzz#"), _r(1, "#zyzz#"), _r(1, "#zzyzzz#"),
    _r(1, "#yzzzzyz#"), _r(1, "#zzyzzzz#"), _r(1, "#zzzyzyz#"), _r(1, "#yzzzzzy#"),
    _r(1, "#zzyzzzz#"), _r(2, "#yyyyy#"), _r(2, "#xxxxx#"), _r(3, "#####"),
]
YOUNG_CHEESE = [
    _r(1, "##"), _r(1, "#mm#"), _r(1, "#mmm##"), _r(1, "#mlmmm#"),
    _r(1, "#mlkmmm#"), _r(1, "#mmmkmmm##"), _r(1, "#mlmmmmkm#"), _r(1, "#mmkmmmmmm#"),
    _r(1, "#mmmmmlmmm#"), _r(1, "#mmmmmmmmmm#"), _r(1, "#jjjjjjjjjj#"),
]
AGED_CHEESE = [
    _r(2, "#######"), _r(1, "#iiiiiii#"), _r(1, "#ijjjjjji#"), _r(1, "#ijkkkkkji#"),
    _r(1, "#ijkkkkkkkj#"), _r(1, "#ijkkkkkkkj#"), _r(1, "#ijkkkkkkkj#"), _r(1, "#ijkkkkkkkj#"),
    _r(1, "#ijjjjjjjj#"), _r(1, "#iiiiiiii#"), _r(2, "#######"),
]
GOAT_CHEESE = [
    _r(4, "####"), _r(3, "#zzz#"), _r(2, "#zAzzz#"), _r(2, "#zAzzzzz#"),
    _r(1, "#zzAzzzzzz#"), _r(1, "#xzzzzzzzzx#"), _r(1, "#xxxxxxxxxx#"), _r(2, "#xxxxxxxx#"),
    _r(2, "#xxxxxx#"), _r(4, "####"),
]
CHEESE_SLICE = [
    _r(1, "#mmmmmmmmmm#"), _r(1, "#mlmmmmmmkm#"), _r(1, "#mmmkmmmmmm#"),
    _r(2, "#mmlmkmmm#"), _r(2, "#mmmmmmm#"), _r(3, "#mkmmm#"), _r(3, "#mmmm#"),
    _r(4, "#mm#"), _r(5, "##"),
]
SAUERKRAUT = [
    _r(5, "######"), _r(4, "#U####U#"), _r(4, "#UyzyzU#"), _r(4, "#UzxzyU#"),
    _r(4, "#UyzzxU#"), _r(4, "#UzyzzU#"), _r(4, "#UxzyzU#"), _r(4, "#UyzzyU#"),
    _r(4, "#UzyxzU#"), _r(4, "#UUUUUU#"), _r(4, "########"),
]
PICKLES = [
    _r(5, "######"), _r(4, "#U####U#"), _r(4, "#U2222U#"), _r(4, "#U3344U#"),
    _r(4, "#U2334U#"), _r(4, "#U3242U#"), _r(4, "#U4334U#"), _r(4, "#U2343U#"),
    _r(4, "#U3423U#"), _r(4, "#UUUUUU#"), _r(4, "########"),
]
DRIED_FRUIT = G("""
................
................
................
......######....
....##qpppo##...
...#qpnopnoo#...
..#qpnnnooop#...
..#pnonnoopp#...
.#qpnoonpqpo#...
.#pnoqpnoonn#...
.#nnooqpnnoo#...
..#onpnoqpnoo#..
..#nooqnpoo#....
...########.....
................
................
""")
JERKY = G("""
................
................
................
...#eddd#.......
....#dddcc#.....
.....#ddeed#....
......#ddecc#...
.......#dddd#...
........#cdde#..
................
...#eeedd#......
....#ddcdd#.....
.....#ddeed#....
......#dddd#....
.......##.......
................
""")
SMOKED_HAM = [
    _r(2, "########"), _r(1, "#eeeeeee#"), _r(1, "#efeeeyee#"), _r(1, "#eeyeeeeee#"),
    _r(1, "#eeeeeeefe#"), _r(1, "#efeeeyeeee#"), _r(1, "#eeeeeeeee#"), _r(1, "#cccccccc#"),
    _r(2, "#####"),
]
SMOKED_FISH = G("""
................
................
...............#
..............#N
...###PN###..#NN
.##PNPPNPPN##NN#
##OBNOONOONONN#.
#OOONOONOONOO#..
#OOOAOONOONOO#..
##OONOONOONONN#.
.##NNNNNNNN##NN#
...###NN###..#NN
..............#N
...............#
................
................
""")
PIZZA_SLICE = G("""
................
................
................
..#bbbbbbbbbb#..
..#aaaaaaaaaa#..
...#eeeeeeee#...
....#mmmmmm#....
....#mmcmcm#....
.....#mmmmm#....
.....#mmcmm#....
......#mmm#.....
......#mmm#.....
.......#m#......
........#.......
................
................
""")
QUICHE = [
    _r(2, "##########"), _r(1, "#bbbbbbbbbb#"), _r(1, "#bmmmmmmmmb#"),
    _r(1, "#bmkmmmmkmb#"), _r(1, "#bmmmmmmmmb#"), _r(1, "#bmmmkmmmmb#"),
    _r(1, "#bbbbbbbbbb#"), _r(2, "##########"),
]
CHEESE_OMELETTE = [
    _r(2, "#########"), _r(1, "#jjjjjjjjj#"), _r(1, "#jmmmmmmmmj#"),
    _r(1, "#jmyymmmmmj#"), _r(1, "#jmmmmmyyjj#"), _r(1, "#jmmmmmmmmj#"),
    _r(2, "#jjjjjjjj#"),
]
APPLE_TART = [
    _r(2, "##########"), _r(1, "#aaaaaaaaaa#"), _r(1, "#aeeeeeeeea#"),
    _r(1, "#aefffefffea#"), _r(1, "#aeffffeeea#"), _r(1, "#aeefeffeea#"),
    _r(1, "#aaaaaaaaaa#"), _r(2, "##########"),
]
CHEESE_SANDWICH = [
    _r(2, "##########"), _r(1, "#bbbbbbbbbb#"), _r(1, "#bbbbbbbbbb#"),
    _r(1, "#mmmmmmmmmm#"), _r(1, "#mkmmmkmmmm#"), _r(1, "#mmmmmmmmmm#"),
    _r(1, "#bbbbbbbbbb#"), _r(1, "#bbbbbbbbbb#"), _r(2, "##########"),
]
OLIVE_BREAD = [
    _r(2, "##########"), _r(1, "#aaaaaaaaaa#"), _r(1, "#abbbbbbbba#"),
    _r(1, "#a1bbbb2bba#"), _r(1, "#abbbbbbbba#"), _r(1, "#abb1bbbb2a#"),
    _r(1, "#aaaaaaaaaa#"), _r(2, "##########"),
]
DUCK_CONFIT = [
    _r(3, "#####"), _r(2, "#kkkk#"), _r(1, "#kOOOOk#"), _r(1, "#kOPOOOk#"),
    _r(0, "#OOPOOOO#"), _r(0, "#OOOOOOOOO#"), _r(0, "#OOOOOOOOO#"), _r(0, "#OOOOOO#"),
    _r(1, "#####"),
]
CORN_TORTILLA = [
    _r(2, "#######"), _r(1, "#kkkkkkk#"), _r(1, "#kmmkkkkk#"), _r(0, "#kmkkkkkkk#"),
    _r(0, "#kkkkjjkkk#"), _r(0, "#kkkkkkkkk#"), _r(0, "#kkkkkkkk#"), _r(1, "#jjjjj#"),
]
PANCAKES = [
    _r(2, "########"), _r(1, "#bbbbbbbb#"), _r(1, "#bbbbbbbbb#"), _r(0, "#iiiiiiiiii#"),
    _r(0, "#aaaaaaaaaa#"), _r(0, "#aaaaaaaaaa#"), _r(0, "#iiiiiiiiii#"), _r(0, "#aaaaaaaaaa#"),
    _r(1, "#aaaaaaaa#"), _r(2, "######"),
]
VEGETABLE_GRATIN = [
    _r(2, "##########"), _r(1, "#zzzzzzzzzz#"), _r(1, "#kkkkkkkkkk#"),
    _r(1, "#kpkkppkkkk#"), _r(1, "#kkkkkkpkkk#"), _r(1, "#kkpkkkkkkk#"),
    _r(1, "#zzzzzzzzzz#"), _r(2, "##########"),
]
FRENCH_ONION_SOUP = [
    _r(2, "##########"), _r(1, "#yyyyyyyyyy#"), _r(1, "#yyzyyyzyyy#"),
    _r(1, "#OOOOOOOOOO#"), _r(1, "#OOOOOOOOOO#"), _r(1, "#OOOOOOOOOO#"),
    _r(1, "#zzzzzzzzzz#"), _r(2, "##########"),
]
BUTTER_COOKIE = [
    _r(3, "######"), _r(2, "#bbbbbb#"), _r(1, "#bbaabbbb#"), _r(1, "#bbkkbbbb#"),
    _r(1, "#bbbbkkbb#"), _r(1, "#bbaabkbb#"), _r(1, "#bbbbbbbb#"), _r(2, "######"),
]

# Items: (nom, grille). Toutes les grilles ci-dessus sont dessinees a la main
# ou composees par _r(); _r complete les lignes a 16 colonnes.
_ITEMS = [
    ("wheat_flour", WHEAT_FLOUR), ("barley_flour", BARLEY_FLOUR),
    ("corn_meal", CORN_MEAL), ("dough", DOUGH),
    ("olive_oil", OLIVE_OIL), ("cider", CIDER), ("vinegar", VINEGAR),
    ("butter", BUTTER), ("cream", CREAM),
    ("cheese_curd", CHEESE_CURD), ("young_cheese", YOUNG_CHEESE),
    ("aged_cheese", AGED_CHEESE), ("goat_cheese", GOAT_CHEESE),
    ("cheese_slice", CHEESE_SLICE),
    ("sauerkraut", SAUERKRAUT), ("pickles", PICKLES),
    ("dried_fruit", DRIED_FRUIT), ("jerky", JERKY),
    ("smoked_ham", SMOKED_HAM), ("smoked_fish", SMOKED_FISH),
    ("pizza_slice", PIZZA_SLICE), ("quiche", QUICHE),
    ("cheese_omelette", CHEESE_OMELETTE), ("apple_tart", APPLE_TART),
    ("cheese_sandwich", CHEESE_SANDWICH), ("olive_bread", OLIVE_BREAD),
    ("duck_confit", DUCK_CONFIT), ("corn_tortilla", CORN_TORTILLA),
    ("pancakes", PANCAKES), ("vegetable_gratin", VEGETABLE_GRATIN),
    ("french_onion_soup", FRENCH_ONION_SOUP), ("butter_cookie", BUTTER_COOKIE),
]

_BLOCKS = [
    ("millstone_side", MILLSTONE_SIDE), ("millstone_top", MILLSTONE_TOP),
    ("fruit_press_side", FRUIT_PRESS_SIDE), ("fruit_press_top", FRUIT_PRESS_TOP),
    ("churn_side", CHURN_SIDE), ("churn_top", CHURN_TOP),
    ("cheese_vat_side", CHEESE_VAT_SIDE), ("cheese_vat_top", CHEESE_VAT_TOP),
    ("cheese_vat_top_milk", CHEESE_VAT_TOP_MILK),
    ("cheese_vat_top_curd", CHEESE_VAT_TOP_CURD),
    ("aging_shelf_side", AGING_SHELF_SIDE), ("aging_shelf_front", AGING_SHELF_FRONT),
    ("aging_shelf_front_cheese", AGING_SHELF_FRONT_CHEESE),
    ("fermentation_barrel_side", FERMENTATION_BARREL_SIDE),
    ("fermentation_barrel_top", FERMENTATION_BARREL_TOP),
    ("fermentation_barrel_top_active", FERMENTATION_BARREL_TOP_ACTIVE),
    ("drying_rack_front", DRYING_RACK_FRONT), ("drying_rack_side", DRYING_RACK_SIDE),
    ("drying_rack_front_full", DRYING_RACK_FRONT_FULL),
    ("smokehouse_side", SMOKEHOUSE_SIDE), ("smokehouse_front", SMOKEHOUSE_FRONT),
    ("smokehouse_front_lit", SMOKEHOUSE_FRONT_LIT), ("smokehouse_top", SMOKEHOUSE_TOP),
    ("cooking_pot_side", COOKING_POT_SIDE), ("cooking_pot_top", COOKING_POT_TOP),
    ("cooking_pot_top_stew", COOKING_POT_TOP_STEW),
]

# --- Jus de pomme et jus de pêche (même gabarit que le jus d'orange, teintes du fruit) ---------
_ITEM_APPLE_JUICE = [
    _r(0, ""), _r(3, "V" * 10), _r(3, "V" + "U" * 8 + "V"), _r(4, "VmUUUUUV"),
    _r(4, "VkkkkkV"), _r(4, "VkkkkkV"), _r(4, "VkmmkkV"), _r(4, "VkkkllV"),
    _r(4, "VkkkkkV"), _r(4, "VkkkkkV"), _r(4, "VjjjjjjV"), _r(4, "V" * 8),
    _r(0, ""), _r(0, ""), _r(0, ""), _r(0, ""),
]
_ITEM_PEACH_JUICE = [
    _r(0, ""), _r(3, "V" * 10), _r(3, "V" + "U" * 8 + "V"), _r(4, "VqUUUUUV"),
    _r(4, "VpppppV"), _r(4, "VpppppV"), _r(4, "VpqqppV"), _r(4, "VpppqqV"),
    _r(4, "VpppppV"), _r(4, "VpppppV"), _r(4, "VaaaaaaV"), _r(4, "V" * 8),
    _r(0, ""), _r(0, ""), _r(0, ""), _r(0, ""),
]
_ITEMS += [("apple_juice", _ITEM_APPLE_JUICE), ("peach_juice", _ITEM_PEACH_JUICE)]


def _center(rows):
    """Centre verticalement une liste de lignes d'item sur 16 lignes."""
    top = (16 - len(rows)) // 2
    return [_r()] * top + list(rows) + [_r()] * (16 - top - len(rows))


SPRITES_V05 = [("block", n, g) for n, g in _BLOCKS] + [
    ("item", n, _center(g)) for n, g in _ITEMS
]


# ===========================================================================
# GUI: gui/processing.png (256x256), panneau 176x166 en haut-gauche
# ===========================================================================

GUI_SIZE = 256
_GUI_BG = (198, 198, 198, 255)
_GUI_LIGHT = (255, 255, 255, 255)
_GUI_DARK = (85, 85, 85, 255)
_GUI_SLOT = (139, 139, 139, 255)
_GUI_BLACK = (0, 0, 0, 255)


def _rect(px, x0, y0, x1, y1, col):
    """Rectangle plein, bornes incluses."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = col


def _slot(px, x, y, size=16):
    """Slot biseaute: cadre sombre haut-gauche, clair bas-droite, interieur gris."""
    fx0, fy0 = x - 1, y - 1
    fx1, fy1 = x + size, y + size
    _rect(px, fx0, fy0, fx1, fy1, _GUI_LIGHT)
    _rect(px, fx0, fy0, fx1 - 1, fy1 - 1, _GUI_SLOT)
    _rect(px, x, y, x + size - 1, y + size - 1, _GUI_SLOT)
    # interieur: ombre legere en haut-gauche (vanilla)
    _rect(px, x, y, x + size - 1, y, _GUI_DARK)
    _rect(px, x, y, x, y + size - 1, _GUI_DARK)


def _arrow(px, x, y, col, edge):
    """Fleche 22x15: corps horizontal + pointe triangulaire a droite."""
    for dy in range(15):
        for dx in range(22):
            # corps: bande centrale (y 4..10) jusqu'a x=12
            inside = False
            if dx <= 12 and 4 <= dy <= 10:
                inside = True
            # pointe: triangle de (12,0) a (21,7) a (12,14)
            if dx >= 12:
                half = (21 - dx) * 7 / 9.0
                if abs(dy - 7) <= half:
                    inside = True
            if inside:
                px[x + dx, y + dy] = col
    # contour simple (pixels de bord)
    for dy in range(15):
        for dx in range(22):
            if px[x + dx, y + dy] == col:
                edge_px = False
                for ddx, ddy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = dx + ddx, dy + ddy
                    if not (0 <= nx < 22 and 0 <= ny < 15) or px[x + nx, y + ny] != col:
                        edge_px = True
                        break
                if edge_px:
                    px[x + dx, y + dy] = edge


def build_processing_gui():
    """Image RGBA 256x256 de gui/processing.png (voir l'en-tete pour les coords)."""
    img = Image.new("RGBA", (GUI_SIZE, GUI_SIZE), (0, 0, 0, 0))
    px = img.load()
    W, H = 176, 166
    # cadre exterieur noir, biseau clair haut-gauche, sombre bas-droite
    _rect(px, 0, 0, W - 1, H - 1, _GUI_BLACK)
    _rect(px, 1, 1, W - 2, H - 2, _GUI_BG)
    _rect(px, 1, 1, W - 2, 1, _GUI_LIGHT)
    _rect(px, 1, 1, 1, H - 2, _GUI_LIGHT)
    _rect(px, 1, H - 2, W - 2, H - 2, _GUI_DARK)
    _rect(px, W - 2, 1, W - 2, H - 2, _GUI_DARK)
    # entree 2x2 (34,17) (52,17) (34,35) (52,35)
    for x, y in ((34, 17), (52, 17), (34, 35), (52, 35)):
        _slot(px, x, y)
    # conteneur (43,62)
    _slot(px, 43, 62)
    # sortie 26x26: cadre en (111,30) (= interieur 24x24 en (112,31)), item en (116,35)
    _slot(px, 112, 31, size=24)
    # fleche vide (79,34) et pleine (176,0)
    _arrow(px, 79, 34, _GUI_SLOT, _GUI_DARK)
    _arrow(px, 176, 0, (220, 170, 38, 255), (143, 98, 8, 255))
    # inventaire joueur 3x9 (y=84,102,120) et barre (y=142)
    for r in range(3):
        for c in range(9):
            _slot(px, 8 + 18 * c, 84 + 18 * r)
    for c in range(9):
        _slot(px, 8 + 18 * c, 142)
    return img
