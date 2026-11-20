"""Sprites v0.7 de Terres Vivantes: commerce et guilde.

Pieces (cuivre, argent, or, contrat, sceau de guilde, carnet de comptes),
blocs (panneau du marche, etal, bureau de guilde) et superposition de
profession de villageois "guild_merchant" (64x64, disposition UV vanilla).

SPRITES_V07: liste de (dossier, nom, grille), meme format que sprites.SPRITES.
Grilles 16x16 dessinees a la main, caractere par caractere, palette partagee
de generate_all.py (aucune nouvelle couleur). Lumiere venant du haut-gauche.
Les grilles sont ecrites par groupes de 4 caracteres: H() retire les espaces.

build_guild_merchant(palette) renvoie la superposition 64x64 du marchand de
guilde; generate_all.py l'ecrit aussi sous zombie_villager (copie identique).
"""


def H(text):
    """Bloc multiligne de groupes -> liste de lignes (espaces retires)."""
    return ["".join(line.split()) for line in text.strip("\n").splitlines() if line.strip()]


def _block(inner):
    """Cadre de contour '#' autour d'un interieur de 14x14 (bloc 16x16)."""
    assert len(inner) == 14 and all(len(r) == 14 for r in inner), "interieur 14x14"
    return ["#" * 16] + ["#" + r + "#" for r in inner] + ["#" * 16]


import math


def _disc(light, mid, dark, emblem, rad_fn=lambda t: 6.6, ribbon=None):
    """Disque 16x16 a contour '#', ombrage haut-gauche et motif grave (emblem)."""
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            r = rad_fn(math.atan2(dy, dx))
            if d > r + 0.5:
                ch = "."
                if ribbon and y >= 14 and (x, y) in ribbon:
                    ch = ribbon[(x, y)]
            elif d > r - 0.5:
                ch = "#"
            else:
                s = dx + dy
                ch = light if s < -4 else (mid if s < 2.5 else dark)
                e = emblem(x, y, dx, dy, d)
                if e:
                    ch = e
            row.append(ch)
        rows.append("".join(row))
    return rows


def _epi(x, y, dx, dy, d):
    """Epi de ble grave: tige centrale et grains alternes."""
    if x == 7 and 4 <= y <= 10:
        return "n"
    if x in (6, 8) and y in (5, 7, 9):
        return "n"
    return None


def _leaf(x, y, dx, dy, d):
    """Feuille gravee: losange pointu avec nervure claire."""
    t = (y - 4) / 7.0
    hw = round(2.2 * math.sin(math.pi * t)) if 4 <= y <= 11 else 0
    if hw and abs(dx) <= hw - 0.5:
        return "w" if x == 7 and 5 <= y <= 10 else "C"
    return None


def _sun(x, y, dx, dy, d):
    """Soleil grave: disque central et huit rayons."""
    if d <= 2.2:
        return "j"
    if 3.0 <= d <= 5.0 and (abs(dx) < 0.6 or abs(dy) < 0.6 or abs(abs(dx) - abs(dy)) < 0.6):
        return "i"
    return None


def _wheat(x, y, dx, dy, d):
    """Empreinte d'epi claire au centre du sceau."""
    if x == 7 and 3 <= y <= 11:
        return "l"
    if x in (6, 8) and y in (4, 6, 8, 10):
        return "l"
    return None


_seal_ribbon = {(4, 14): "d", (5, 14): "d", (10, 14): "d", (11, 14): "d",
                (3, 15): "d", (4, 15): "d", (11, 15): "d", (12, 15): "d"}


# ---------------------------------------------------------------------------
# PIECES DE MONNAIE (16x16): silhouette ronde, contour '#', lumiere haut-gauche
# ---------------------------------------------------------------------------

# Piece de cuivre: rebord orange fonce, epi de ble grave en brun-orange (n).
ITEM_COPPER_COIN = _disc("q", "p", "o", _epi)

# Piece d'argent: gris clair, feuille gravee en gris fonce (C) avec nervure (w).
ITEM_SILVER_COIN = _disc("F", "E", "D", _leaf)

# Piece d'or: or brillant, soleil grave (rayons i, disque j).
ITEM_GOLD_COIN = _disc("m", "k", "j", _sun)

# ---------------------------------------------------------------------------
# CONTRAT ET SCEAU (16x16)
# ---------------------------------------------------------------------------

# Parchemin roule: rouleaux clairs aux extremites, ruban rouge et sceau.
ITEM_CONTRACT = H("""
................
..############..
..#xxxxxxxxxx#..
..#yyyyyyyyyy#..
..#zzzzzzzzzz#..
..#zxxxxzzxxz#..
..#zzzzzzzzzz#..
..#zxxzzzzxxz#..
..#eeeeeeeeee#..
..#ddddhcccdd#..
..#zzzzccczzz#..
..#zxxzzzzxxz#..
..#yyyyyyyyyy#..
..#xxxxxxxxxx#..
..############..
................
""")

# Sceau de guilde: disque de cire verte avec un motif grave et reflet.
ITEM_GUILD_SEAL = _disc("4", "3", "2", _wheat,
    rad_fn=lambda t: 6.4 + 0.35 * math.sin(5 * t) + 0.25 * math.cos(3 * t), ribbon=_seal_ribbon)

# Carnet de comptes: couverture brune, pages claires, lignes de compte grises.
ITEM_MARKET_LEDGER = H("""
.... .... .... ....
.#88 8888 8888 88#.
.#88 zzzz zzzz 8#..
.#98 zxzx zxzx 8#..
.#98 zzzz zzzz 8#..
.#98 xxxx xxxx 8#..
.#98 zzzz zzzz 8#..
.#98 xxxx xxxx 8#..
.#98 zzzz zzzz 8#..
.#98 xxxx xxxx 8#..
.#98 zzzz zzzz 8#..
.#98 xxxx xxxx 8#..
.#88 8888 8888 88#.
.... .... .... ....
.... .... .... ....
.... .... .... ....
""")

# ---------------------------------------------------------------------------
# BLOCS (16x16): panneau du marche, etal, bureau de guilde
# ---------------------------------------------------------------------------

# Panneau du marche, face: cadre de bois, liege ocre, trois affichettes punaisees.
_cork = [
    "aaaaaaaaaaaa",
    "azzzzazzzzaa",
    "azezzazezzaa",
    "azxzzazxzzaa",
    "azzzzazzzzaa",
    "azxzzazxzzaa",
    "aaaaaazzzzaa",
    "aaaaaaaaaaaa",
    "aazzezzaaaaa",
    "aazxzzzaaaaa",
    "aazxxzzaaaaa",
    "aazzzzzaaaaa",
]
BLOCK_MARKET_BOARD_FRONT = _block(
    ["9" * 14] + ["9" + c + "9" for c in _cork] + ["9" * 14]
)

# Panneau, cote: tranche de planche et liege en coupe.
_side_a = "9a9a9a9a9a9a99"
_side_b = "a9a9a9a9a9a9aa"
BLOCK_MARKET_BOARD_SIDE = _block(
    ["8" * 14] + [_side_a, _side_b] * 6 + ["8" * 14]
)

# Panneau, dessus: rebord de bois et dessus de liege.
_top_g = "a" * 14
_top_h = "aabaaaaaaaaaab"
BLOCK_MARKET_BOARD_TOP = _block(
    ["9" * 14] + [_top_g, _top_g, _top_h] * 4 + ["9" * 14]
)

# Etal, face: auvent raye rouge et blanc, comptoir de bois en dessous.
_stripe_a = "ddddzzzzddddzzzz"
_stripe_b = "zzzzddddzzzzdddd"
BLOCK_MARKET_STALL_FRONT = [
    _stripe_a,
    _stripe_a,
    _stripe_b,
    "################",
    "9999999999999999",
    "8888888888888888",
    "7777777777777777",
    "8888888888888888",
    "9999999999999999",
    "8888888888888888",
    "7777777777777777",
    "8888888888888888",
    "9999999999999999",
    "8888888888888888",
    "7777777777777777",
    "################",
]

# Etal, cote: bord d'auvent et planches du comptoir.
BLOCK_MARKET_STALL_SIDE = [
    _stripe_b,
    _stripe_a,
    _stripe_a,
    "################",
    "8888888888888888",
    "9999999999999999",
    "8888888888888888",
    "7777777777777777",
    "8888888888888888",
    "9999999999999999",
    "8888888888888888",
    "7777777777777777",
    "8888888888888888",
    "9999999999999999",
    "7777777777777777",
    "################",
]

# Etal, dessus: toile rayee vue du dessus (bandes rouges et blanches).
_stripe2 = "ddzzddzzddzzddzz"
BLOCK_MARKET_STALL_TOP = [_stripe2] * 16

# Etal, dessous: planches brutes sombres, bords en contour.
BLOCK_MARKET_STALL_BOTTOM = [
    "################",
    "#7777777777777777"[:16],
    "#8888888888888888"[:16],
    "7777777777777777",
    "8888888888888888",
    "7777777777777777",
    "8888888888888888",
    "7777777777777777",
    "8888888888888888",
    "7777777777777777",
    "8888888888888888",
    "7777777777777777",
    "8888888888888888",
    "7777777777777777",
    "8888888888888888",
    "################",
]

# Bureau de guilde, face: bois sombre, balance dorée et registre ouvert.
BLOCK_GUILD_DESK_FRONT = [
    "################",
    "7777777777777777",
    "7888888888888887",
    "7888kkkkk8nnnnn7",
    "7888888k88zzzzz7",
    "7888888k88zxzxz7",
    "7888888k88zzzzz7",
    "7888888k88xxxxx7",
    "7888888888888887",
    "7777777777777777",
    "7888888888888887",
    "7888888888888887",
    "7888888888888887",
    "7888888888888887",
    "7777777777777777",
    "################",
]

# Bureau de guilde, cote: caisson de bois sombre et tiroir.
BLOCK_GUILD_DESK_SIDE = [
    "################",
    "7777777777777777",
    "7888888888888887",
    "7888888888888887",
    "7888888888888887",
    "7777777777777777",
    "7888888888888887",
    "7888888888888887",
    "7777777777777777",
    "7888888888888887",
    "7888888888888887",
    "7888888888888887",
    "7777777777777777",
    "7888888888888887",
    "7777777777777777",
    "################",
]

# Bureau de guilde, dessus: plateau sombre avec encre et papiers.
BLOCK_GUILD_DESK_TOP = [
    "################",
    "7777777777777777",
    "8888888888888887",
    "8888zzzz88888887",
    "8888zxzx88nnn887",
    "8888zzzz88888887",
    "8888888888888887",
    "8888888888888887",
    "8888888888888887",
    "8888888888888887",
    "8888888888888887",
    "8888888888888887",
    "8888888888888887",
    "8888888888888887",
    "7777777777777777",
    "################",
]

# ---------------------------------------------------------------------------
# ASSEMBLAGE
# ---------------------------------------------------------------------------

ITEMS_V07 = [
    ("copper_coin", ITEM_COPPER_COIN),
    ("silver_coin", ITEM_SILVER_COIN),
    ("gold_coin", ITEM_GOLD_COIN),
    ("contract", ITEM_CONTRACT),
    ("guild_seal", ITEM_GUILD_SEAL),
    ("market_ledger", ITEM_MARKET_LEDGER),
]

BLOCKS_V07 = [
    ("market_board_front", BLOCK_MARKET_BOARD_FRONT),
    ("market_board_side", BLOCK_MARKET_BOARD_SIDE),
    ("market_board_top", BLOCK_MARKET_BOARD_TOP),
    ("market_stall_front", BLOCK_MARKET_STALL_FRONT),
    ("market_stall_side", BLOCK_MARKET_STALL_SIDE),
    ("market_stall_top", BLOCK_MARKET_STALL_TOP),
    ("market_stall_bottom", BLOCK_MARKET_STALL_BOTTOM),
    ("guild_desk_front", BLOCK_GUILD_DESK_FRONT),
    ("guild_desk_side", BLOCK_GUILD_DESK_SIDE),
    ("guild_desk_top", BLOCK_GUILD_DESK_TOP),
]

SPRITES_V07 = [("item", n, g) for n, g in ITEMS_V07] + [("block", n, g) for n, g in BLOCKS_V07]


# ---------------------------------------------------------------------------
# MARCHAND DE GUILDE (entite villageois, 64x64, disposition UV vanilla)
# ---------------------------------------------------------------------------
# Seules les zones vetement sont dessinees; la tete (0..32 x 0..16) reste
# transparente. Boites (disposition cuboid vanilla, meme que le villageois):
#   chapeau       : 32..64 x 0..16   (dessus 40..48 x 0..8)
#   corps         : 16..44 x 20..38  (face 22..30 x 26..38)
#   bras croises  : 40..60 x 38..46  (face 44..52 x 42..46)
#   bras pendants : 44..56 x 22..38
#   jupe (robe)   : 0..28 x 38..62   (face 6..14 x 44..62)


def _rect(px, x0, y0, x1, y1, col, palette):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = palette[col] + (255,)


def build_guild_merchant(palette):
    """Retourne [(nom, image 64x64)]: superposition de profession du marchand de guilde."""
    from PIL import Image

    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()

    # Chapeau de feutre: calotte bombee claire au centre, bande de cuir, bord large.
    _rect(px, 32, 0, 64, 16, "7", palette)       # feutre (flancs)
    _rect(px, 40, 0, 48, 8, "8", palette)        # calotte
    _rect(px, 42, 0, 46, 2, "9", palette)        # reflet de la calotte
    _rect(px, 32, 10, 64, 12, "9", palette)      # bande de cuir
    _rect(px, 32, 14, 64, 15, "8", palette)      # bord large
    _rect(px, 32, 15, 64, 16, "7", palette)      # dessous du bord

    # Corps: tunique vert sombre, tablier brun, ceinture de cuir.
    _rect(px, 22, 20, 30, 26, "2", palette)      # dessus
    _rect(px, 30, 20, 38, 26, "2", palette)      # dessous
    _rect(px, 16, 26, 22, 38, "2", palette)      # cote droit
    _rect(px, 22, 26, 30, 38, "2", palette)      # face
    _rect(px, 30, 26, 36, 38, "1", palette)      # cote gauche (ombre)
    _rect(px, 36, 26, 44, 38, "1", palette)      # arriere
    _rect(px, 24, 26, 28, 28, "3", palette)      # col clair
    _rect(px, 24, 30, 28, 38, "8", palette)      # tablier brun
    _rect(px, 24, 30, 25, 38, "9", palette)      # reflet du tablier
    _rect(px, 22, 36, 30, 37, "7", palette)      # ceinture de cuir
    _rect(px, 17, 30, 21, 35, "9", palette)      # sacoche (hanche droite)
    _rect(px, 17, 30, 21, 31, "0", palette)      # rabat de la sacoche
    _rect(px, 17, 34, 21, 35, "7", palette)      # sangle

    # Bras croises: manches vert sombre, dessus plus clair.
    _rect(px, 44, 38, 52, 42, "3", palette)      # dessus
    _rect(px, 52, 38, 60, 42, "1", palette)      # dessous
    _rect(px, 40, 42, 44, 46, "1", palette)      # cote droit
    _rect(px, 44, 42, 52, 46, "2", palette)      # face
    _rect(px, 52, 42, 56, 46, "1", palette)      # cote gauche

    # Bras pendants: manches vert sombre.
    _rect(px, 44, 22, 56, 38, "2", palette)
    _rect(px, 44, 22, 48, 26, "3", palette)
    _rect(px, 52, 30, 56, 38, "1", palette)

    # Jupe / robe: vert sombre, ourlet brun.
    _rect(px, 6, 38, 14, 44, "2", palette)       # dessus
    _rect(px, 14, 38, 22, 44, "1", palette)      # dessous
    _rect(px, 0, 44, 6, 62, "1", palette)        # cote droit
    _rect(px, 6, 44, 14, 62, "2", palette)       # face
    _rect(px, 14, 44, 20, 62, "1", palette)      # cote gauche
    _rect(px, 20, 44, 28, 62, "1", palette)      # arriere
    _rect(px, 6, 60, 28, 62, "7", palette)       # ourlet brun

    return [("guild_merchant", img)]
