package jku.pr_dke.mymakrochef.ingestion;

import jku.pr_dke.mymakrochef.ingestion.RecipePreparer.Recipe;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Lädt die aufbereiteten Rezepte aus der JSON-Datei von RecipePreparer in Supabase (Schema rezeptplaner).
 * <p>
 * Ein Rezept, das es schon gibt, wird zuerst gelöscht und dann neu geschrieben. Der Import kann
 * also beliebig oft laufen. Alles passiert in einer Transaktion: Geht etwas schief, wird nichts gespeichert.
 * <p>
 * Programmargument: {@code <recipes_sample.json>}, Umgebungsvariablen: DB_URL, DB_USER, DB_PASSWORD
 */
public class RecipeImporter {

    // Liest recipe_id aus dem JSON in das Feld recipeId usw.
    private static final JsonMapper JSON = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    public static void main(String[] args) throws SQLException {
        if (args.length != 1) {
            System.err.println("Aufruf: RecipeImporter <recipes_sample.json>");
            System.exit(1);
        }
        List<Recipe> recipes = JSON.readValue(new File(args[0]), new TypeReference<List<Recipe>>() {});
        System.out.println(recipes.size() + " Rezepte aus der JSON-Datei gelesen");

        try (Connection connection = DriverManager.getConnection(
                requireEnv("DB_URL"), requireEnv("DB_USER"), requireEnv("DB_PASSWORD"))) {
            connection.setAutoCommit(false);
            try {
                for (Recipe recipe : recipes) {
                    importRecipe(connection, recipe);
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
        System.out.println(recipes.size() + " Rezepte in rezeptplaner gespeichert");
    }

    /** Schreibt ein Rezept mit Schritten, Zutaten und Tags in die Datenbank. */
    private static void importRecipe(Connection connection, Recipe recipe) throws SQLException {
        long id = recipe.recipeId();

        // Vorhandenes Rezept löschen, Schritte und Verbindungen verschwinden per on delete cascade mit
        try (PreparedStatement delete = connection.prepareStatement(
                "delete from rezeptplaner.recipe where recipe_id = ?")) {
            delete.setLong(1, id);
            delete.executeUpdate();
        }

        try (PreparedStatement insert = connection.prepareStatement("""
                insert into rezeptplaner.recipe (recipe_id, name, description, minutes, servings, serving_size,
                    calories, fat_g, saturated_fat_g, carbohydrates_g, protein_g, sodium_mg, sugar_g,
                    source, contributor_id, submitted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            insert.setLong(1, id);
            insert.setString(2, recipe.name());
            insert.setString(3, recipe.description());
            insert.setInt(4, recipe.minutes());
            insert.setInt(5, recipe.servings());
            insert.setString(6, recipe.servingSize());
            insert.setDouble(7, recipe.calories());
            insert.setDouble(8, recipe.fatG());
            insert.setDouble(9, recipe.saturatedFatG());
            insert.setDouble(10, recipe.carbohydratesG());
            insert.setDouble(11, recipe.proteinG());
            insert.setDouble(12, recipe.sodiumMg());
            insert.setDouble(13, recipe.sugarG());
            insert.setString(14, recipe.source());
            insert.setLong(15, recipe.contributorId());
            insert.setDate(16, Date.valueOf(recipe.submitted()));
            insert.executeUpdate();
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "insert into rezeptplaner.recipe_step (recipe_id, step_no, instruction) values (?, ?, ?)")) {
            for (int i = 0; i < recipe.steps().size(); i++) {
                insert.setLong(1, id);
                insert.setInt(2, i + 1);
                insert.setString(3, recipe.steps().get(i));
                insert.executeUpdate();
            }
        }

        // Zutatenname und Originalzeile mit Menge gehören über die Position zusammen
        try (PreparedStatement insert = connection.prepareStatement(
                "insert into rezeptplaner.recipe_ingredient (recipe_id, position, ingredient_id, raw_text) values (?, ?, ?, ?)")) {
            for (int i = 0; i < recipe.ingredients().size(); i++) {
                insert.setLong(1, id);
                insert.setInt(2, i + 1);
                insert.setInt(3, findOrCreate(connection, "ingredient", recipe.ingredients().get(i)));
                insert.setString(4, recipe.ingredientsRaw().get(i));
                insert.executeUpdate();
            }
        }

        // on conflict do nothing: falls ein Tag im Rezept doppelt vorkommt
        try (PreparedStatement insert = connection.prepareStatement(
                "insert into rezeptplaner.recipe_tag (recipe_id, tag_id) values (?, ?) on conflict do nothing")) {
            for (String tag : recipe.tags()) {
                insert.setLong(1, id);
                insert.setInt(2, findOrCreate(connection, "tag", tag));
                insert.executeUpdate();
            }
        }
    }

    /**
     * Legt einen Namen in ingredient oder tag an, falls es ihn noch nicht gibt, und liefert seine id.
     * {@code table} kommt nur aus diesem Code ("ingredient" oder "tag"), nie von außen,
     * deshalb darf er direkt ins SQL.
     */
    private static int findOrCreate(Connection connection, String table, String name) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "insert into rezeptplaner." + table + " (name) values (?) on conflict (name) do nothing")) {
            insert.setString(1, name);
            insert.executeUpdate();
        }
        try (PreparedStatement select = connection.prepareStatement(
                "select " + table + "_id from rezeptplaner." + table + " where name = ?")) {
            select.setString(1, name);
            try (ResultSet result = select.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Umgebungsvariable " + name + " fehlt");
        }
        return value;
    }
}