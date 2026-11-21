package fr.terresvivantes.balance;

import fr.terresvivantes.core.quality.logic.QualityLogic;
import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.CropDefinitions;
import fr.terresvivantes.crop.logic.CropLogic;
import fr.terresvivantes.crop.logic.GrowthDecision;
import fr.terresvivantes.crop.logic.GrowthModel;
import fr.terresvivantes.crop.logic.YieldModel;
import fr.terresvivantes.duck.logic.ProductionLogic;
import fr.terresvivantes.economy.logic.BasePrices;
import fr.terresvivantes.economy.logic.MarketLogic;
import fr.terresvivantes.economy.logic.PriceModel;
import fr.terresvivantes.economy.logic.ReputationLogic;
import fr.terresvivantes.health.logic.Disease;
import fr.terresvivantes.husbandry.logic.FeedLogic;
import fr.terresvivantes.husbandry.logic.FeedType;
import fr.terresvivantes.husbandry.logic.Species;
import fr.terresvivantes.health.logic.HealthModel;
import fr.terresvivantes.livestock.logic.Ailment;
import fr.terresvivantes.livestock.logic.LivestockLogic;
import fr.terresvivantes.livestock.logic.LivestockState;
import fr.terresvivantes.market.logic.ContractGenerator;
import fr.terresvivantes.market.logic.StallLogic;
import fr.terresvivantes.season.logic.Season;
import fr.terresvivantes.soil.logic.SoilData;
import fr.terresvivantes.soil.logic.SoilModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simulateur d'équilibrage (v0.8). Il exécute les classes logic réelles (croissance, sol, santé, prix, marché,
 * contrats, élevage), imprime un tableau récapitulatif et l'écrit dans {@code docs/BALANCE.md}.
 *
 * <p>Hypothèses : une journée MC de lumière suffisante pour la croissance (comme la formule de {@code growthDays}),
 * champ à 24 °C en été, 35 % de jours pluvieux, sol neuf = {@link SoilData#DEFAULT}, qualité 1 pour les récoltes,
 * une traite par vache et par jour MC (le mod ne plafonne pas la traite). Graine fixe : résultats reproductibles.
 */
final class BalanceSimulationTest {

    private static final long SEED = 20261121L;
    /** Plants tirés par culture pour les moyennes de maturation. */
    private static final int PLANTS = 1500;
    /** Au-delà, une culture est jugée « jamais mûre ». */
    private static final double MAX_DAYS = 60.0;
    private static final int FIELD_SIDE = 9;
    private static final int FIELD_PLOTS = FIELD_SIDE * FIELD_SIDE;
    private static final double RAIN_CHANCE = 0.35;
    private static final float FIELD_TEMP = 24.0f;
    private static final int SEASONS_SIMULATED = 10;
    /** Saisons répétées avec des graines différentes, pour lisser le bruit des maladies. */
    private static final int DISEASE_RUNS = 8;
    private static final int DAYS_PER_SEASON = 7;
    /** Sol épuisé : min(N, P, K) sous 20, facteur de Liebig 0.25. */
    private static final double EXHAUSTED_FACTOR = 0.25;
    private static final int MARKET_DAYS = 60;
    private static final int MARKET_SETTLED_FROM = 30;
    private static final double RTPD = CropLogic.RANDOM_TICKS_PER_DAY;
    private static final double TICKS_PER_DAY = 24000.0;
    private static final int HERD_COWS = 10;
    private static final int HERD_DAYS = 28;
    private static final int HERD_PRODUCTIVITY = 6;
    private static final Path BALANCE_DOC = Path.of("docs", "BALANCE.md");
    private static final String AVANT_BEGIN = "<!-- AVANT:BEGIN -->";
    private static final String AVANT_END = "<!-- AVANT:END -->";
    private static final String CHANGES_BEGIN = "<!-- CHANGES:BEGIN -->";
    private static final String CHANGES_END = "<!-- CHANGES:END -->";
    private static final Pattern COUNT = Pattern.compile("\"count\"\\s*:\\s*(\\d+)");
    private static final Pattern TIME = Pattern.compile("\"time\"\\s*:\\s*(\\d+)");

    /**
     * Valeurs par défaut de {@code SeasonConfig} et {@code SoilConfig}. Ces classes lisent des specs NeoForge, absentes
     * du classpath des tests unitaires : les défauts sont recopiés ici (à garder synchronisés avec les configs).
     */
    private static final float GREENHOUSE_WINTER_MULTIPLIER = 0.6f;
    private static final double CONSUMPTION_MULTIPLIER = 1.0;

    /** Espèces du champ diversifié. */
    private static final String[] FIELD_SPECIES = {
        "tomato", "lettuce", "bell_pepper", "eggplant", "cucumber", "strawberry", "barley", "corn", "soybean"
    };

    private final StringBuilder out = new StringBuilder();
    private final List<String[]> targets = new ArrayList<>();

    @Test
    void simulateBalanceAndWriteReport() throws IOException {
        Map<String, Integer> prices = BasePrices.parse(resource("/data/terresvivantes/economy/base_prices.json"));

        section("(a) Maturation (jours MC moyens, champ de jour)");
        out.append("| Culture | Optimale | Possible | Hors saison (défavorable) | Serre hiver | Sol épuisé (optimale) |\n");
        out.append("|---|---|---|---|---|---|\n");
        Map<String, Growth> growth = new LinkedHashMap<>();
        for (CropDefinition def : CropDefinitions.all()) {
            Growth row = growthOf(def);
            growth.put(def.id(), row);
            out.append(String.format("| %s | %s | %s | %s | %s | %s |%n", def.id(), days(row.optimal()),
                    days(row.possible()), days(row.unfavorable()), days(row.greenhouse()), days(row.exhausted())));
        }
        double[] optimal = growth.values().stream().mapToDouble(Growth::optimal).toArray();
        double[] greenhouse = growth.values().stream().mapToDouble(Growth::greenhouse).toArray();
        double[] unfavorable = growth.values().stream().mapToDouble(Growth::unfavorable).toArray();
        target("Maturation optimale (toutes cultures)", "1 à 2 j", range(optimal), within(optimal, 1.0, 2.0));
        target("Serre hiver (toutes cultures)", "2 à 3 j", range(greenhouse), within(greenhouse, 2.0, 3.0));
        target("Hors saison défavorable (toutes cultures)", "3 à 5 j", range(unfavorable),
                within(unfavorable, 3.0, 5.0));

        section("(b) Récoltes avant épuisement du sol (sol neuf, sans engrais)");
        out.append("| Régime | Récoltes avant épuisement (min(N,P,K) < 20) |\n|---|---|\n");
        int tomato = harvestsUntilExhausted(List.of(def("tomato")), 60);
        int corn = harvestsUntilExhausted(List.of(def("corn")), 60);
        int soy = harvestsUntilExhausted(List.of(def("soybean")), 60);
        int rotation = harvestsUntilExhausted(List.of(def("soybean"), def("tomato")), 60);
        out.append(String.format("| Monoculture tomate | %d |%n", tomato));
        out.append(String.format("| Monoculture maïs | %d |%n", corn));
        out.append(String.format("| Monoculture soja (légumineuse) | %d |%n", soy));
        out.append(String.format("| Rotation soja / tomate | %d |%n", rotation));
        target("Sol neuf, monoculture tomate et maïs", "8 à 12 récoltes", tomato + " et " + corn,
                between(tomato, 8, 12) && between(corn, 8, 12));
        target("Rotation soja / tomate vs monoculture tomate", "plus longue", rotation + " contre " + tomato,
                rotation > tomato);

        section("(c) Pertes par maladie sur une saison (champ 9x9, été, comparé au même champ sans maladie)");
        out.append("| Parcelle | Perte de rendement sur la saison |\n|---|---|\n");
        double monoTomato = diseaseLoss(new String[] {"tomato"}, new int[FIELD_PLOTS], SEED);
        double monoCorn = diseaseLoss(new String[] {"corn"}, new int[FIELD_PLOTS], SEED + 1);
        double diversified = diseaseLoss(FIELD_SPECIES, diversifiedLayout(), SEED + 2);
        out.append(String.format("| Monoculture tomate (oïdium) | %s |%n", pct(monoTomato)));
        out.append(String.format("| Monoculture maïs (rouille) | %s |%n", pct(monoCorn)));
        out.append(String.format("| Diversifiée (9 espèces) | %s |%n", pct(diversified)));
        target("Perte en monoculture (tomate)", "15 à 30 %", pct(monoTomato),
                monoTomato >= 0.15 && monoTomato <= 0.30);
        target("Perte en parcelle diversifiée", "< 8 %", pct(diversified), diversified < 0.08);

        section("(d) Revenu par jour MC (champ 9x9 vendu à l'étal, saturation incluse)");
        out.append("| Culture | Jours par cycle | Rendement moyen | Valeur de base x rendement | Pièces de cuivre / jour |\n");
        out.append("|---|---|---|---|---|\n");
        Map<String, Double> revenue = new LinkedHashMap<>();
        for (CropDefinition def : CropDefinitions.all()) {
            double cycleDays = growth.get(def.id()).optimal();
            int basePrice = BasePrices.basePrice(prices, def.id());
            double perDay = fieldRevenuePerDay(def, cycleDays, basePrice);
            revenue.put(def.id(), perDay);
            double avgYield = (def.yieldMin() + def.yieldMax()) / 2.0;
            out.append(String.format("| %s | %s | %.1f | %.1f | %.0f |%n", def.id(), days(cycleDays), avgYield,
                    avgYield * basePrice, perDay));
        }
        double median = median(revenue.values());
        double best = revenue.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        out.append(String.format("%nMédiane : %.0f. Meilleure culture / médiane = %.2f.%n", median, best / median));
        target("Aucune culture ne domine", "meilleure / médiane < 2", String.format("%.2f", best / median),
                best / median < 2.0);

        section("Chaînes transformées");
        Map<String, double[]> chains = chains(prices);
        for (Map.Entry<String, double[]> e : chains.entrySet()) {
            if (!e.getKey().equals("Lait → fromage affiné")) {
                double ratio = e.getValue()[2];
                target(e.getKey() + " (par unité brute)", "1,5 à 3 ×", String.format("%.2f ×", ratio),
                        ratio >= 1.5 && ratio <= 3.0);
            }
        }
        double cheese = chains.get("Lait → fromage affiné")[1];
        double cream = chains.get("Lait → crème")[1];
        double butter = chains.get("Lait → beurre")[1];
        double rawMilk = chains.get("Lait → crème")[0];
        target("Fromage affiné : meilleur revenu par vache", "au-dessus de la crème, du beurre et du lait",
                String.format("%.0f contre %.0f, %.0f, %.0f", cheese, cream, butter, rawMilk),
                cheese > cream && cheese > butter && cheese > rawMilk);

        int goat = BasePrices.basePrice(prices, "goat_cheese");
        int aged = BasePrices.basePrice(prices, "aged_cheese");
        target("Fromage de chèvre (base) < fromage affiné (base)", "< " + aged + " pièces", goat + " pièces",
                goat < aged);

        section("Contrats contre vente directe");
        double contractRatio = contractRatio();
        out.append(String.format("Récompense d'un contrat par unité / vente à l'étal (90 %%) : **+%.0f %%**.%n",
                (contractRatio - 1.0) * 100.0));
        target("Contrat contre vente directe", "+30 à 60 %", String.format("+%.0f %%", (contractRatio - 1.0) * 100),
                contractRatio >= 1.30 && contractRatio <= 1.60);

        section("(e) Élevage : " + HERD_COWS + " vaches sur " + HERD_DAYS + " jours MC (productivité "
                + HERD_PRODUCTIVITY + ")");
        out.append("| Régime | Satiété moyenne (fin) | Santé moyenne (fin) | Vivantes à la fin | Lait ★1 / ★2 / ★3 | Valeur lait / jour (troupeau) | Repas de foin / vache |\n");
        out.append("|---|---|---|---|---|---|---|\n");
        herd("Bien nourries (pâturage, auge en hiver)", true, prices);
        herd("Mal nourries (pâturage 1 cycle sur 3, rien en hiver)", false, prices);
        int starvation = starvationCycles();
        target("Famine : survie à satiété 0 (santé pleine)", ">= 4 j MC", String.format("%.1f j MC", starvation / 20.0),
                starvation >= 80);

        section("Bilan des cibles");
        out.append("| Cible | Attendu | Obtenu | Atteinte |\n|---|---|---|---|\n");
        for (String[] t : targets) {
            out.append("| ").append(String.join(" | ", t)).append(" |\n");
        }

        writeReport();
        // Garde-fou : une cible atteinte en v0.8 ne doit pas se dégrader sans que le bilan le signale.
        List<String> missed = new ArrayList<>();
        for (String[] t : targets) {
            if (!"oui".equals(t[3])) {
                missed.add(t[0] + " (obtenu : " + t[2] + ")");
            }
        }
        assertTrue(missed.isEmpty(), "cibles non atteintes : " + missed);
    }

    // ------------------------------------------------------------------ (a) maturation

    private record Growth(double optimal, double possible, double unfavorable, double greenhouse, double exhausted) {
    }

    private static Growth growthOf(CropDefinition def) {
        float freshSoil = SoilModel.nutrientFactor(SoilData.DEFAULT);
        float exhaustedSoil = SoilModel.nutrientFactor(new SoilData(15, 15, 15, 10));
        Season opt = first(def.optimal());
        Season pos = first(def.possible());
        Season unf = first(def.unfavorable());
        Season greenhouseAffinity = CropLogic.affinitySeason(Season.WINTER, true);
        return new Growth(
                matureDays(def, seasonMultiplier(opt), def.affinityFor(opt).getMultiplier(), freshSoil),
                matureDays(def, seasonMultiplier(pos), def.affinityFor(pos).getMultiplier(), freshSoil),
                matureDays(def, seasonMultiplier(unf), def.affinityFor(unf).getMultiplier(), freshSoil),
                matureDays(def, GREENHOUSE_WINTER_MULTIPLIER, def.affinityFor(greenhouseAffinity).getMultiplier(),
                        freshSoil),
                matureDays(def, seasonMultiplier(opt), def.affinityFor(opt).getMultiplier(), exhaustedSoil));
    }

    /** Multiplicateurs saisonniers par défaut (voir la note sur les configs ci-dessus). */
    private static float seasonMultiplier(Season season) {
        return switch (season) {
            case SPRING -> 1.0f;
            case SUMMER -> 1.2f;
            case AUTUMN -> 0.8f;
            case WINTER -> 0.15f;
        };
    }

    /** Jours MC moyens jusqu'à la maturité, par tirages de randomTicks réels ({@link GrowthModel#decide}). */
    static double matureDays(CropDefinition def, float seasonMultiplier, float affinity, float soil) {
        Random rnd = new Random(SEED ^ def.id().hashCode());
        double chance = CropLogic.attemptChance(def.growthDays());
        long maxTicks = (long) (MAX_DAYS * RTPD);
        double sum = 0;
        int matured = 0;
        for (int i = 0; i < PLANTS; i++) {
            int stage = 0;
            long ticks = 0;
            while (stage < CropLogic.GROWTH_STEPS && ticks < maxTicks) {
                ticks++;
                if (rnd.nextDouble() >= chance) {
                    continue;
                }
                float factor = GrowthModel.totalFactor(seasonMultiplier, affinity, 1.0f, soil, 1.0f);
                GrowthDecision decision = GrowthModel.decide(factor, rnd.nextDouble(), rnd.nextDouble());
                if (decision != GrowthDecision.DENY) {
                    stage = CropLogic.clampAge(stage + (decision == GrowthDecision.EXTRA_STAGE ? 2 : 1));
                }
            }
            if (stage >= CropLogic.GROWTH_STEPS) {
                sum += ticks / RTPD;
                matured++;
            }
        }
        return matured == 0 ? Double.POSITIVE_INFINITY : sum / matured;
    }

    // ------------------------------------------------------------------ (b) sol

    /** Récoltes successives (rotation cyclique) avant que min(N, P, K) ne passe sous 20. */
    static int harvestsUntilExhausted(List<CropDefinition> rotation, int maxHarvests) {
        SoilData data = SoilData.DEFAULT;
        int harvests = 0;
        int index = 0;
        while (harvests < maxHarvests) {
            if (SoilModel.nutrientFactor(data) <= EXHAUSTED_FACTOR) {
                break;
            }
            CropDefinition def = rotation.get(index++ % rotation.size());
            // Comme CropGrowth : un stade consomme (sauf N pour une légumineuse), la légumineuse fixe de l'azote.
            for (int stage = 0; stage < CropLogic.GROWTH_STEPS; stage++) {
                CropDefinition.Nutrients c = def.stageCost();
                data = SoilModel.consume(data, def.legume() ? 0 : c.n(), c.p(), c.k(), CONSUMPTION_MULTIPLIER);
                if (def.legume()) {
                    data = SoilModel.fertilize(data, CropLogic.LEGUME_NITROGEN_FIXATION, 0, 0, 0);
                }
            }
            CropDefinition.Nutrients h = def.harvestCost();
            data = SoilModel.consume(data, h.n(), h.p(), h.k(), CONSUMPTION_MULTIPLIER);
            harvests++;
        }
        return harvests;
    }

    // ------------------------------------------------------------------ (c) maladies

    /** Une espèce du champ, telle que la voit la boucle de croissance en été. */
    private record CropSim(Disease disease, double attemptChance, float baseFactor, int yieldMin, int yieldMax) {
    }

    private static CropSim species(String id) {
        CropDefinition def = def(id);
        float climate = GrowthModel.climateFactor(FIELD_TEMP, def.tempMin(), def.tempMax());
        float base = GrowthModel.totalFactor(seasonMultiplier(Season.SUMMER),
                def.affinityFor(Season.SUMMER).getMultiplier(), climate, 1.0f, 1.0f);
        return new CropSim(Disease.susceptibility(id), CropLogic.attemptChance(def.growthDays()), base,
                def.yieldMin(), def.yieldMax());
    }

    /** Disposition diversifiée : espèces réparties sur le champ en motif (r + 2c) modulo neuf. */
    private static int[] diversifiedLayout() {
        int[] layout = new int[FIELD_PLOTS];
        for (int r = 0; r < FIELD_SIDE; r++) {
            for (int c = 0; c < FIELD_SIDE; c++) {
                layout[r * FIELD_SIDE + c] = (r + 2 * c) % FIELD_SPECIES.length;
            }
        }
        return layout;
    }

    /**
     * Perte de rendement sur une saison (dix semaines de sept jours, champ 9x9 replanté après chaque récolte) :
     * 1 - rendement avec maladies / rendement de la même parcelle sans maladie. Un cycle flétri donne 0, un cycle
     * malade la moitié (HealthModel); la croissance malade est ralentie, donc moins de cycles par saison.
     */
    static double diseaseLoss(String[] ids, int[] layout, long seed) {
        double sick = 0;
        double healthy = 0;
        for (int run = 0; run < DISEASE_RUNS; run++) {
            sick += fieldYield(ids, layout, seed + 1000L * run, true);
            healthy += fieldYield(ids, layout, seed + 1000L * run, false);
        }
        return healthy == 0 ? 0 : 1.0 - sick / healthy;
    }

    /** Rendement total d'un champ sur la saison simulée, avec ou sans maladies. */
    private static double fieldYield(String[] ids, int[] layout, long seed, boolean withDiseases) {
        CropSim[] sp = new CropSim[ids.length];
        for (int i = 0; i < ids.length; i++) {
            sp[i] = species(ids[i]);
        }
        int n = FIELD_PLOTS;
        int[][] neighbours = neighbours();
        int[] specie = layout.clone();
        int[] stage = new int[n];
        Disease[] disease = new Disease[n];
        int[] diseaseTicks = new int[n];
        int[] lastHarvested = new int[n];
        int[] repeat = new int[n];
        Arrays.fill(disease, Disease.NONE);
        Arrays.fill(lastHarvested, -1);

        Random rnd = new Random(seed);
        double total = 0;
        int days = SEASONS_SIMULATED * DAYS_PER_SEASON;
        for (int day = 0; day < days; day++) {
            boolean rain = rnd.nextDouble() < RAIN_CHANCE;
            for (int p = 0; p < n; p++) {
                int ticks = rnd.nextDouble() < (RTPD - Math.floor(RTPD)) ? (int) Math.ceil(RTPD) : (int) Math.floor(RTPD);
                for (int t = 0; t < ticks; t++) {
                    CropSim s = sp[specie[p]];
                    if (rnd.nextDouble() >= s.attemptChance()) {
                        continue;
                    }
                    if (withDiseases && disease[p] == Disease.NONE && s.disease() != Disease.NONE) {
                        int same = 0;
                        for (int q : neighbours[p]) {
                            if (specie[q] == specie[p]) {
                                same++;
                            }
                        }
                        boolean mono = HealthModel.monoculture(repeat[p], lastHarvested[p] == specie[p]);
                        HealthModel.Conditions cond = new HealthModel.Conditions(rain, rain, FIELD_TEMP, same, mono, false);
                        if (HealthModel.infects(HealthModel.infectionChance(s.disease(), cond), rnd.nextDouble())) {
                            disease[p] = s.disease();
                            diseaseTicks[p] = 0;
                        }
                    }
                    float factor = s.baseFactor() * (withDiseases ? HealthModel.growthFactor(disease[p]) : 1.0f);
                    GrowthDecision decision = GrowthModel.decide(factor, rnd.nextDouble(), rnd.nextDouble());
                    if (withDiseases && disease[p] != Disease.NONE) {
                        diseaseTicks[p]++;
                        if (HealthModel.withersFromDisease(disease[p], diseaseTicks[p])) {
                            stage[p] = 0;
                            disease[p] = Disease.NONE;
                            diseaseTicks[p] = 0;
                            continue; // cycle perdu, replanté
                        }
                        if (HealthModel.spreads(rnd.nextDouble())) {
                            spread(p, specie[p], disease, diseaseTicks, neighbours[p], specie, rnd);
                        }
                    }
                    if (decision != GrowthDecision.DENY) {
                        stage[p] = CropLogic.clampAge(stage[p] + (decision == GrowthDecision.EXTRA_STAGE ? 2 : 1));
                    }
                    if (stage[p] >= CropLogic.GROWTH_STEPS) {
                        int count = s.yieldMin() + rnd.nextInt(s.yieldMax() - s.yieldMin() + 1);
                        total += YieldModel.scaledCount(count, HealthModel.yieldFactor(disease[p]), rnd.nextDouble());
                        repeat[p] = lastHarvested[p] == specie[p] ? repeat[p] + 1 : 1;
                        lastHarvested[p] = specie[p];
                        if (ids.length > 1) {
                            specie[p] = (specie[p] + 1) % ids.length; // assolement : espèce suivante
                        }
                        stage[p] = 0;
                        disease[p] = Disease.NONE;
                        diseaseTicks[p] = 0;
                    }
                }
            }
        }
        return total;
    }

    /** Contagion : une tentative de la culture malade touche un voisin identique sain. */
    private static void spread(int from, int species, Disease[] state, int[] ticks, int[] around, int[] specie,
                               Random rnd) {
        List<Integer> candidates = new ArrayList<>();
        for (int q : around) {
            if (q != from && specie[q] == species && state[q] == Disease.NONE) {
                candidates.add(q);
            }
        }
        if (!candidates.isEmpty()) {
            int target = candidates.get(rnd.nextInt(candidates.size()));
            state[target] = state[from];
            ticks[target] = 0;
        }
    }

    /** Indices des plots de la zone 5x5 autour de chaque plot (plot compris). */
    private static int[][] neighbours() {
        int[][] result = new int[FIELD_PLOTS][];
        for (int r = 0; r < FIELD_SIDE; r++) {
            for (int c = 0; c < FIELD_SIDE; c++) {
                List<Integer> list = new ArrayList<>();
                for (int dr = -2; dr <= 2; dr++) {
                    for (int dc = -2; dc <= 2; dc++) {
                        int rr = r + dr;
                        int cc = c + dc;
                        if (rr >= 0 && rr < FIELD_SIDE && cc >= 0 && cc < FIELD_SIDE) {
                            list.add(rr * FIELD_SIDE + cc);
                        }
                    }
                }
                result[r * FIELD_SIDE + c] = list.stream().mapToInt(Integer::intValue).toArray();
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ (d) revenus

    /**
     * Pièces de cuivre par jour MC pour un champ 9x9 de la culture, récoltée au rythme de sa maturation ({@code days}),
     * vendue à l'étal (90 % du prix) en saison optimale. La saturation suit {@link MarketLogic}, jour après jour.
     */
    static double fieldRevenuePerDay(CropDefinition def, double days, int basePrice) {
        if (basePrice <= 0 || Double.isInfinite(days)) {
            return 0;
        }
        double unitsPerDay = FIELD_PLOTS * (def.yieldMin() + def.yieldMax()) / 2.0 / days;
        float seasonFactor = PriceModel.seasonFactor(def.optimal(), first(def.optimal()));
        MarketLogic market = MarketLogic.empty();
        double revenue = 0;
        double carry = 0;
        for (int day = 0; day < MARKET_DAYS; day++) {
            carry += unitsPerDay;
            int sales = (int) Math.floor(carry);
            carry -= sales;
            for (int i = 0; i < sales; i++) {
                int price = PriceModel.price(basePrice, 1, seasonFactor, market.pointsAt(day));
                if (day >= MARKET_SETTLED_FROM) {
                    revenue += StallLogic.payment(price);
                }
                market = market.sold(day);
            }
        }
        return revenue / (MARKET_DAYS - MARKET_SETTLED_FROM);
    }

    /**
     * Chaînes de transformation, quantités et temps lus dans les recettes JSON. Par chaîne :
     * {@code [valeur brute d'une unité, valeur transformée par unité brute, ratio, délai en jours]}.
     */
    private Map<String, double[]> chains(Map<String, Integer> prices) {
        Map<String, double[]> result = new LinkedHashMap<>();
        int milkQuality = 2;
        int milk = unit(prices, "minecraft:milk_bucket", milkQuality);
        int[] churnCream = recipe("dairy_churn_cream");
        int[] churnButter = recipe("dairy_churn_butter");
        int[] vat = recipe("dairy_cheese_vat_curd");
        int[] young = recipe("dairy_aging_shelf_young");
        int[] aged = recipe("dairy_aging_shelf_aged");

        double creamPerMilk = (double) churnCream[1] / churnCream[0];
        double creamValue = creamPerMilk * unit(prices, "cream", milkQuality);
        result.put("Lait → crème", chainRow(milk, creamValue, creamPerMilk, churnCream[2] / TICKS_PER_DAY));

        double butterPerMilk = creamPerMilk * churnButter[1] / churnButter[0];
        double butterValue = butterPerMilk * unit(prices, "butter", milkQuality);
        result.put("Lait → beurre", chainRow(milk, butterValue, butterPerMilk,
                (churnCream[2] + churnButter[2]) / TICKS_PER_DAY));

        double agedPerMilk = (double) vat[1] / vat[0] * young[1] / young[0] * aged[1] / aged[0];
        int agedQuality = QualityLogic.ageQuality(milkQuality, 6);
        double cheeseValue = agedPerMilk * unit(prices, "aged_cheese", agedQuality);
        result.put("Lait → fromage affiné", chainRow(milk, cheeseValue, agedPerMilk,
                (vat[2] + young[2] + aged[2]) / TICKS_PER_DAY));

        int[] press = recipe("press_olive_oil");
        int olive = unit(prices, "olive", 1);
        double oilPerOlive = (double) press[1] / press[0];
        double oilValue = oilPerOlive * unit(prices, "olive_oil", 1);
        result.put("Olive → huile d'olive", chainRow(olive, oilValue, oilPerOlive, press[2] / TICKS_PER_DAY));

        int[] juice = recipe("press_apple_juice");
        int[] cider = recipe("preserving_cider");
        int apple = unit(prices, "minecraft:apple", 1);
        double ciderPerApple = (double) juice[1] / juice[0] * cider[1] / cider[0];
        double ciderValue = ciderPerApple * unit(prices, "cider", 1);
        result.put("Pomme → cidre", chainRow(apple, ciderValue, ciderPerApple, (juice[2] + cider[2]) / TICKS_PER_DAY));

        out.append(String.format("%nLait de départ ★%d ; fromage affiné après 6 jours : ★%d.%n%n", milkQuality, agedQuality));
        out.append("| Chaîne | Valeur brute (1 unité) | Valeur transformée par unité brute | Ratio | Délai (jours MC) |\n");
        out.append("|---|---|---|---|---|\n");
        for (Map.Entry<String, double[]> e : result.entrySet()) {
            double[] v = e.getValue();
            out.append(String.format("| %s | %.0f | %.1f | %.2f × | %.1f |%n", e.getKey(), v[0], v[1], v[2], v[3]));
        }
        return result;
    }

    /** Ligne d'une chaîne : {@code [valeur brute, valeur transformée, ratio, délai]}. */
    private static double[] chainRow(int rawValue, double chainValue, double perRaw, double days) {
        return new double[] {rawValue, chainValue, rawValue == 0 ? 0 : chainValue / rawValue, days};
    }

    /** Récompense moyenne par unité d'un contrat, divisée par la vente à l'étal de la même unité. */
    private static double contractRatio() {
        int price = 10;
        double total = 0;
        int samples = 300;
        for (int seed = 1; seed <= samples; seed++) {
            ContractGenerator.Generated g = ContractGenerator.generate(seed, ReputationLogic.Tier.REGULAR, 0L,
                    List.of("tomato"), List.of(), id -> price);
            total += (double) g.reward() / g.quantity();
        }
        return (total / samples) / StallLogic.payment(price);
    }

    // ------------------------------------------------------------------ (e) élevage

    private void herd(String name, boolean wellFed, Map<String, Integer> prices) {
        Random rnd = new Random(SEED);
        int[] qualityCount = new int[4];
        double fedSum = 0;
        double healthSum = 0;
        int alive = 0;
        double milkValue = 0;
        int hayMeals = 0;
        for (int cow = 0; cow < HERD_COWS; cow++) {
            LivestockState state = LivestockState.initial();
            boolean dead = false;
            for (int day = 0; day < HERD_DAYS && !dead; day++) {
                boolean winter = day % 28 >= 21;
                for (int cycle = 0; cycle < 20 && !dead; cycle++) {
                    boolean grass = wellFed ? !winter : !winter && rnd.nextInt(3) == 0;
                    state = LivestockLogic.needsStep(state, winter, grass, day);
                    if (wellFed && winter && FeedLogic.wantsFood(state.fed())) {
                        // Auge pleine de foin : un repas à chaque cycle tant que la satiété est sous 80.
                        state = state.withFed(LivestockState.clamp(state.fed()
                                + FeedLogic.points(Species.COW, FeedType.HAY)));
                        hayMeals++;
                    }
                    dead = state.health() <= 0;
                }
                if (!dead) {
                    float multiplier = LivestockLogic.productionMultiplier(HERD_PRODUCTIVITY, state.fed(),
                            state.ailment() != Ailment.NONE);
                    int quality = ProductionLogic.milkQuality(multiplier);
                    qualityCount[quality]++;
                    milkValue += PriceModel.price(BasePrices.basePrice(prices, "minecraft:milk_bucket"), quality,
                            1.0f, 0.0);
                }
            }
            if (!dead) {
                alive++;
            }
            fedSum += state.fed();
            healthSum += state.health();
        }
        out.append(String.format("| %s | %.0f / 100 | %.0f / 100 | %d / %d | %d / %d / %d | %.0f | %.1f |%n", name,
                fedSum / HERD_COWS, healthSum / HERD_COWS, alive, HERD_COWS, qualityCount[1], qualityCount[2],
                qualityCount[3], milkValue / HERD_DAYS, hayMeals / (double) HERD_COWS));
    }

    /** Cycles de besoins avant la mort d'un animal à satiété 0 et santé pleine (20 cycles par jour MC). */
    private static int starvationCycles() {
        LivestockState state = new LivestockState(0, LivestockState.clamp(100), Ailment.NONE, 0L);
        int cycles = 0;
        while (state.health() > 0 && cycles < 10_000) {
            state = LivestockLogic.needsStep(state, false, false, 0L);
            cycles++;
        }
        return cycles;
    }

    // ------------------------------------------------------------------ utilitaires

    private void target(String name, String expected, String obtained, boolean reached) {
        targets.add(new String[] {name, expected, obtained, reached ? "oui" : "non"});
    }

    private static CropDefinition def(String id) {
        return CropDefinitions.byId(id).orElseThrow();
    }

    private static Season first(Set<Season> set) {
        return set.stream().sorted().findFirst().orElse(Season.WINTER);
    }

    private static int unit(Map<String, Integer> prices, String id, int quality) {
        return PriceModel.price(BasePrices.basePrice(prices, id), quality, 1.0f, 0.0);
    }

    private static String days(double value) {
        return Double.isInfinite(value) || Double.isNaN(value) ? "jamais" : String.format("%.1f j", value);
    }

    private static String pct(double value) {
        return String.format("%.0f %%", value * 100.0);
    }

    private static String range(double[] values) {
        return String.format("%.1f à %.1f j", Arrays.stream(values).min().orElse(0),
                Arrays.stream(values).max().orElse(0));
    }

    private static boolean within(double[] values, double min, double max) {
        return Arrays.stream(values).allMatch(v -> v >= min && v <= max);
    }

    private static boolean between(int value, int min, int max) {
        return value >= min && value <= max;
    }

    private static double median(Iterable<Double> values) {
        List<Double> sorted = new ArrayList<>();
        values.forEach(sorted::add);
        sorted.sort(null);
        if (sorted.isEmpty()) {
            return 0;
        }
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 0 ? (sorted.get(mid - 1) + sorted.get(mid)) / 2.0 : sorted.get(mid);
    }

    private void section(String title) {
        out.append("\n## ").append(title).append("\n\n");
    }

    /** Recette JSON : {@code [entrée, sortie, temps en ticks]}, lues dans les fichiers de données. */
    private static int[] recipe(String name) {
        String text = resource("/data/terresvivantes/recipe/" + name + ".json");
        Matcher in = COUNT.matcher(text);
        assertTrue(in.find(), "quantité d'entrée absente : " + name);
        Matcher result = COUNT.matcher(text);
        result.region(text.indexOf("\"result\""), text.length());
        assertTrue(result.find(), "quantité de sortie absente : " + name);
        Matcher time = TIME.matcher(text);
        int timeTicks = time.find() ? Integer.parseInt(time.group(1)) : 0;
        return new int[] {Integer.parseInt(in.group(1)), Integer.parseInt(result.group(1)), timeTicks};
    }

    private static String keptBlock(String text, String begin, String end, String fallback) {
        int from = text.indexOf(begin);
        int to = text.indexOf(end);
        return from >= 0 && to > from ? text.substring(from + begin.length(), to).strip() : fallback;
    }

    private static String resource(String path) {
        try (InputStream in = BalanceSimulationTest.class.getResourceAsStream(path)) {
            assertTrue(in != null, "ressource absente : " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Écrit docs/BALANCE.md : les blocs « avant » et « changements » entre marqueurs sont conservés. */
    private void writeReport() throws IOException {
        String before = "_Aucun relevé avant enregistré._";
        String changes = "_Aucune justification enregistrée._";
        if (Files.exists(BALANCE_DOC)) {
            String existing = Files.readString(BALANCE_DOC, StandardCharsets.UTF_8);
            before = keptBlock(existing, AVANT_BEGIN, AVANT_END, before);
            changes = keptBlock(existing, CHANGES_BEGIN, CHANGES_END, changes);
        }
        String report = "# Équilibrage v0.8\n\n"
                + "Généré par `src/test/java/fr/terresvivantes/balance/BalanceSimulationTest.java` (`./gradlew test`).\n"
                + "Les tableaux « après » sont régénérés à chaque exécution; le bloc « avant » est conservé entre les marqueurs.\n\n"
                + "## Avant (valeurs v0.7)\n\n" + AVANT_BEGIN + "\n" + before + "\n" + AVANT_END + "\n\n"
                + "## Après (valeurs v0.8)\n" + out + "\n"
                + "## Changements et justifications\n\n" + CHANGES_BEGIN + "\n" + changes + "\n" + CHANGES_END + "\n";
        Files.createDirectories(BALANCE_DOC.toAbsolutePath().getParent());
        Files.writeString(BALANCE_DOC, report, StandardCharsets.UTF_8);
        System.out.println(out);
    }
}
