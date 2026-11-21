package fr.terresvivantes.guide.logic;

/**
 * Une page du manuel : titre, texte et icône d'item optionnelle (identifiant {@code espace:chemin}, ou {@code null}).
 */
public record GuidePage(String title, String text, String icon) {
}
