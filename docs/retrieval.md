# Retrieval: Aufgaben und erster Umsetzungsschritt

Petra (`petrahudkova`) ist für folgende GitHub-Issues zuständig:

| Issue | Aufgabe | Stand in diesem Branch |
| --- | --- | --- |
| #10 / US-09 | Harte Filter | Nährwertgrenzen, Ernährungsform und Zeitlimit implementiert; Allergene offen |
| #11 / US-10 | Hybride Suche | Offen; benötigt Embeddings und Indizes aus #6 |
| #12 / US-11 | Reranking | Offen |
| #13 / US-12 | Keine passenden Treffer | Bestehende leere Antwort bleibt erhalten; Relevanzschwelle offen |

## Strukturierte Filter für `/api/chat`

```json
{
  "message": "rice",
  "pantry": [],
  "filters": {
    "diet": "VEGAN",
    "calories": { "min": 200, "max": 500 },
    "protein": { "min": 20 },
    "carbs": { "max": 60 },
    "fat": { "max": 15 },
    "maxMinutes": 30
  }
}
```

Alle Filter und einzelne Grenzen sind optional. Bestehende Anfragen ohne `filters`
funktionieren weiter. Grenzen sind inklusive und werden mit UND kombiniert.
Nährwerte werden direkt mit den gespeicherten Werten verglichen: Kalorien in kcal,
Makronährstoffe in g. Die Bezugsgröße pro Portion muss im Rahmen der Datenqualität
bestätigt werden; der Filter rechnet sie nicht um.

`diet` erlaubt `VEGETARIAN` oder `VEGAN`. Die Prüfung verwendet vorhandene
Food.com-Tags: `vegan` erfüllt beide Anforderungen, `vegetarian` nur die vegetarische.
Fehlt ein passendes Tag, wird das Rezept ausgeschlossen. Das ist eine Prüfung der
Datensatzkennzeichnung, keine zusätzliche Verifikation der Zutaten.

Fehlen Werte für einen aktiven Nährwert- oder Zeitfilter, wird das Rezept ausgeschlossen.
Negative oder nicht endliche Grenzwerte, vertauschte Unter-/Obergrenzen und negative
Zeitlimits werden mit HTTP 400 abgelehnt. Die Filter greifen vor Ranking und Ergebnislimit.
Wenn kein Treffer übrig bleibt, wird die bestehende Antwort ohne Rezeptkarten und ohne
Modellaufruf verwendet. Einschränkungen werden nicht gelockert.

Die Filter werden aktuell nur als strukturierte API-Felder ausgewertet. Eine Ableitung
aus Freitext und die Eingabe im Frontend sind noch nicht angebunden.

## Offene Abhängigkeiten

- #5 / US-04 (Kerstin): normalisierte Allergendaten. Das aktuelle Datenmodell hat
  keine Allergenkennzeichnung; AC-09.1 ist deshalb noch nicht umgesetzt.
- #6 / US-05 (Kerstin): Embeddings, pgvector und Volltextindex für #11.
- #7 / US-06 (Luisa): Profil und Vorrat als Quelle für strukturierte Filter.
- #8 / US-07 (Luisa): Suchoberfläche und Anbindung an den API-Vertrag.
- #17–#20: gemeinsame Aufgaben zu Golden Dataset, Evaluation, Tracing und Integration.

Die Suche lädt für den kleinen Prototyp weiterhin alle Rezepte. Die Verlagerung in
die Datenbanksuche folgt mit #11; die verbindlichen Filter müssen dort erhalten bleiben.

## Tests

Im Backend-Verzeichnis:

```sh
mvn -Dtest=RecipeHardFilterTest test
```

Die Tests benötigen weder Datenbank noch Gemini. Sie prüfen Grenzen, unbekannte Werte,
Ernährungsformen, kombinierte Filter, JSON-Kompatibilität und die Anwendung vor dem
Ergebnislimit sowie den Fall ohne zulässige Treffer.
