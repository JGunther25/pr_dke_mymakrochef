package jku.pr_dke.mymakrochef.ingestion;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Bereitet die Food.com-Rezepte für den Import in Supabase vor.
 * Liest RAW_recipes.csv (Zubereitungszeit, Nährwerte, Metadaten)
 * und recipes_ingredients.csv (Zutaten mit Mengen, Schritte, Portionen),
 * führt beide über die id zusammen, bereinigt sie filtert nach den Auswahlregeln
 * und schreibt eine zufällige, aber wiederholbare Stichprobe als JSON-Datei.
 * (Rohdaten werden nur gelesen, nie verändert)
 * <p>
 * Programmargumente: {@code <RAW_recipes.csv> <recipes_ingredients.csv> <Ausgabe.json>}
 */
public class RecipePreparer {

    // Auswahlregeln für die erste Stichprobe
    private static final int SAMPLE_SIZE = 20;
    private static final long RANDOM_SEED = 42;
    private static final int MIN_STEPS = 3;
    private static final int MAX_STEPS = 15;
    private static final int MIN_MINUTES = 5;
    private static final int MAX_MINUTES = 180;
    private static final int MIN_SERVINGS = 1;
    private static final int MAX_SERVINGS = 12;

    // US-Tagesreferenzwerte (FDA) vor 2016, auf denen die PDV-Angaben von Food.com vermutlich beruhen:
    // Mit diesen Werten lassen sich die Kalorien aus Fett, Kohlenhydraten und Eiweiß am besten nachrechnen.
    // Quelle der Einzelwerte noch prüfen.
    private static final double DAILY_FAT_G = 65;
    private static final double DAILY_SATURATED_FAT_G = 20;
    private static final double DAILY_CARBOHYDRATES_G = 300;
    private static final double DAILY_PROTEIN_G = 50;
    private static final double DAILY_SODIUM_MG = 2400;
    // Für Zucker gab es keinen offiziellen Wert. 25 g ergibt sich aus Rezepten für reinen Zuckersirup,
    // bei denen der Zucker den Kohlenhydraten entsprechen muss.
    private static final double DAILY_SUGAR_G = 25;

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .get();

    // Java-Felder wie recipeId werden im JSON zu recipe_id
    private static final JsonMapper JSON = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private static final TypeReference<List<Double>> DOUBLE_LIST = new TypeReference<>() {};

    /** Die Felder aus RAW_recipes.csv, die wir brauchen. */
    private record RawRecipe(int minutes, long contributorId, String submitted, List<Double> nutrition,
                             List<String> ingredientNames) {}

    /**
     * Ein aufbereitetes Rezept, so wie es in der JSON-Datei landet.
     * <p>
     * Nährwerte vermutlich pro Portion: Kalorien in kcal, Natrium in mg, alle anderen in Gramm
     * (aus Prozent des Tagesbedarfs umgerechnet, siehe DAILY_*).
     */
    public record Recipe(
            long recipeId,
            String name,
            String description,
            int minutes,
            int servings,
            String servingSize,
            List<String> ingredients,
            List<String> ingredientsRaw,
            List<String> steps,
            List<String> tags,
            double calories,
            double fatG,
            double saturatedFatG,
            double carbohydratesG,
            double proteinG,
            double sodiumMg,
            double sugarG,
            String source,
            long contributorId,
            String submitted) {}

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Aufruf: RecipePreparer <RAW_recipes.csv> <recipes_ingredients.csv> <Ausgabe.json>");
            System.exit(1);
        }
        Path rawRecipesPath = Path.of(args[0]);
        Path recipesIngredientsPath = Path.of(args[1]);
        Path outputPath = Path.of(args[2]);

        Map<Long, RawRecipe> rawRecipes = readRawRecipes(rawRecipesPath);
        System.out.println("RAW_recipes.csv: " + rawRecipes.size() + " Rezepte gelesen");

        Map<String, Integer> rejected = new LinkedHashMap<>();
        List<Recipe> candidates = readCandidates(recipesIngredientsPath, rawRecipes, rejected);
        int total = candidates.size() + rejected.values().stream().mapToInt(Integer::intValue).sum();
        System.out.println("recipes_ingredients.csv: " + total + " Rezepte gelesen");

        System.out.println("Aussortiert (jeweils nach der ersten verletzten Regel):");
        rejected.forEach((reason, count) -> System.out.println("  " + count + "  " + reason));
        System.out.println("Passend: " + candidates.size());

        List<Recipe> sample = drawSample(candidates);
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        JSON.writerWithDefaultPrettyPrinter().writeValue(outputPath, sample);
        System.out.println("Stichprobe: " + sample.size() + " Rezepte nach " + outputPath + " geschrieben");
    }

    /** Liest RAW_recipes.csv in eine Map, damit jedes Rezept schnell über seine id gefunden wird. */
    private static Map<Long, RawRecipe> readRawRecipes(Path path) throws IOException {
        Map<Long, RawRecipe> recipes = new HashMap<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSV_FORMAT.parse(reader)) {
            for (CSVRecord record : parser) {
                long id = Long.parseLong(record.get("id"));
                recipes.put(id, new RawRecipe(
                        Integer.parseInt(record.get("minutes")),
                        Long.parseLong(record.get("contributor_id")),
                        record.get("submitted"),
                        // nutrition steht als [51.5, 0.0, ...] im Text, das ist gültiges JSON
                        JSON.readValue(record.get("nutrition"), DOUBLE_LIST),
                        // Die Zutatennamen hier sind sauberer als die in recipes_ingredients.csv,
                        // stehen aber im Python-Format: ['honey', 'butter']
                        parsePythonList(record.get("ingredients"))));
            }
        }
        return recipes;
    }

    /**
     * Liest recipes_ingredients.csv Zeile für Zeile, verbindet jede Zeile mit RAW_recipes.csv
     * und behält nur Rezepte, die alle Regeln erfüllen. Für jedes aussortierte Rezept wird der
     * Grund in {@code rejected} mitgezählt.
     */
    private static List<Recipe> readCandidates(Path path, Map<Long, RawRecipe> rawRecipes,
                                               Map<String, Integer> rejected) throws IOException {
        List<Recipe> candidates = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSV_FORMAT.parse(reader)) {
            for (CSVRecord record : parser) {
                long id = Long.parseLong(record.get("id"));
                RawRecipe raw = rawRecipes.get(id);
                if (raw == null) {
                    reject(rejected, "nicht in RAW_recipes.csv (keine Nährwerte und Zeit)");
                    continue;
                }
                if (raw.minutes() < MIN_MINUTES || raw.minutes() > MAX_MINUTES) {
                    reject(rejected, "Zubereitungszeit nicht zwischen " + MIN_MINUTES + " und " + MAX_MINUTES + " Minuten");
                    continue;
                }
                int servings = parseServings(record.get("servings"));
                if (servings < MIN_SERVINGS || servings > MAX_SERVINGS) {
                    reject(rejected, "Portionen nicht zwischen " + MIN_SERVINGS + " und " + MAX_SERVINGS);
                    continue;
                }
                if (raw.nutrition().size() != 7) {
                    reject(rejected, "Nährwerte unvollständig");
                    continue;
                }

                String name = clean(record.get("name"));
                List<String> ingredients;
                List<String> ingredientsRaw;
                List<String> steps;
                List<String> tags;
                try {
                    ingredients = cleanList(raw.ingredientNames());
                    ingredientsRaw = cleanList(JSON.readValue(record.get("ingredients_raw"), STRING_LIST));
                    steps = cleanList(JSON.readValue(record.get("steps"), STRING_LIST));
                    tags = cleanList(JSON.readValue(record.get("tags"), STRING_LIST));
                } catch (JacksonException e) {
                    reject(rejected, "Listen nicht lesbar");
                    continue;
                }

                if (name.isEmpty()) {
                    reject(rejected, "kein Name");
                    continue;
                }
                if (ingredientsRaw.isEmpty()) {
                    reject(rejected, "keine Zutaten mit Mengen");
                    continue;
                }
                if (ingredients.size() != ingredientsRaw.size()) {
                    reject(rejected, "Zutatennamen unvollständig");
                    continue;
                }
                if (steps.size() < MIN_STEPS || steps.size() > MAX_STEPS) {
                    reject(rejected, "nicht zwischen " + MIN_STEPS + " und " + MAX_STEPS + " Schritte");
                    continue;
                }

                // Reihenfolge laut Datensatz: kcal, Fett, Zucker, Natrium, Eiweiß, gesättigte Fettsäuren, Kohlenhydrate
                List<Double> n = raw.nutrition();
                candidates.add(new Recipe(
                        id, name, clean(record.get("description")), raw.minutes(), servings,
                        clean(record.get("serving_size")), ingredients, ingredientsRaw, steps, tags,
                        n.get(0),
                        fromPdv(n.get(1), DAILY_FAT_G),
                        fromPdv(n.get(5), DAILY_SATURATED_FAT_G),
                        fromPdv(n.get(6), DAILY_CARBOHYDRATES_G),
                        fromPdv(n.get(4), DAILY_PROTEIN_G),
                        fromPdv(n.get(3), DAILY_SODIUM_MG),
                        fromPdv(n.get(2), DAILY_SUGAR_G),
                        "food.com", raw.contributorId(), raw.submitted()));
            }
        }
        return candidates;
    }

    /**
     * Mischt die Kandidaten mit festem Startwert und nimmt die ersten {@link #SAMPLE_SIZE}.
     * Gleiche Daten und gleiche Regeln ergeben so bei jedem Lauf dieselbe Stichprobe.
     */
    private static List<Recipe> drawSample(List<Recipe> candidates) {
        List<Recipe> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, new Random(RANDOM_SEED));
        List<Recipe> sample = new ArrayList<>(shuffled.subList(0, Math.min(SAMPLE_SIZE, shuffled.size())));
        sample.sort(Comparator.comparingLong(Recipe::recipeId));
        return sample;
    }

    /** Liefert die Portionen als ganze Zahl, oder -1 wenn der Wert fehlt oder keine ganze Zahl ist. */
    private static int parseServings(String value) {
        if (value == null || value.isBlank()) {
            return -1;
        }
        double servings = Double.parseDouble(value);
        return servings == Math.floor(servings) ? (int) servings : -1;
    }

    /** Rechnet einen PDV-Wert in die Einheit des Referenzwerts um, gerundet auf eine Nachkommastelle. */
    private static double fromPdv(double pdv, double dailyValue) {
        return Math.round(pdv / 100 * dailyValue * 10) / 10.0;
    }

    /**
     * Liest eine Liste im Python-Format wie {@code ['honey', "baker's chocolate"]}.
     * <p>
     * Python schreibt Texte mit einfachen Anführungszeichen, außer der Text enthält selbst ein
     * Apostroph, dann mit doppelten. Ein einfaches Ersetzen von ' durch " würde solche Texte
     * zerstören, deshalb wird Zeichen für Zeichen gelesen. Mit \ maskierte Zeichen werden
     * übernommen.
     */
    private static List<String> parsePythonList(String text) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0; // 0 = gerade außerhalb eines Textes
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote == 0) {
                if (c == '\'' || c == '"') {
                    quote = c;
                }
            } else if (c == '\\' && i + 1 < text.length()) {
                current.append(text.charAt(++i));
            } else if (c == quote) {
                values.add(current.toString());
                current.setLength(0);
                quote = 0;
            } else {
                current.append(c);
            }
        }
        return values;
    }

    /** Ersetzt HTML-Reste wie &amp;amp; und fasst mehrfache Leerzeichen und Zeilenumbrüche zusammen. */
    private static String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("\\s+", " ")
                .strip();
    }

    /** Bereinigt jeden Eintrag einer Liste und lässt leere Einträge weg. */
    private static List<String> cleanList(List<String> values) {
        List<String> cleaned = new ArrayList<>();
        for (String value : values) {
            String text = clean(value);
            if (!text.isEmpty()) {
                cleaned.add(text);
            }
        }
        return cleaned;
    }

    private static void reject(Map<String, Integer> rejected, String reason) {
        rejected.merge(reason, 1, Integer::sum);
    }
}
