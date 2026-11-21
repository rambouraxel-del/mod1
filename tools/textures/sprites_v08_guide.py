"""Sprite v0.8 du guide : le Manuel du fermier (item/farmers_manual).

Livre vert à reliure cuir (contour '#' et tranche '8'), tranche de pages crème à droite, et un épi
de blé doré grave sur la couverture. Grille 16x16 dessinée caractère par caractère, palette partagée
de generate_all.py (aucune nouvelle couleur). Lumière venant du haut-gauche.

SPRITES_V08: liste de (dossier, nom, grille), même format que sprites.SPRITES.
"""

# Chaque ligne de l'intérieur du livre fait 10 caractères (colonnes 3 à 12); la couverture
# est encadrée par '#' en colonnes 2 et 13.
FARMERS_MANUAL = [
    "................",
    "..############..",
    "..#83333333yz#..",
    "..#8222l222yz#..",
    "..#822klk22yz#..",
    "..#8222kl22yz#..",
    "..#822klk22yz#..",
    "..#8222kl22yz#..",
    "..#8222lk22yz#..",
    "..#82223222yz#..",
    "..#82223222yz#..",
    "..#82223222yz#..",
    "..#82223222yz#..",
    "..#81111111yz#..",
    "..############..",
    "................",
]

SPRITES_V08 = [
    ("item", "farmers_manual", FARMERS_MANUAL),
]
