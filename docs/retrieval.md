# Retrieval: Aufgaben und erster Umsetzungsschritt

Petra (`petrahudkova`) ist für folgende GitHub-Issues zuständig:

| Issue | Aufgabe | Stand in diesem Branch |
| --- | --- | --- |
| #10 / US-09 | Harte Filter | Nährwertgrenzen, Ernährungsform und Zeitlimit implementiert; Allergene offen |
| #11 / US-10 | Hybride Suche | Offen; benötigt Embeddings und Indizes aus #6 |
| #12 / US-11 | Reranking | Gesamtscore aus Anfrage, Vorrat und Budget umgesetzt; Budget wird vom Chat noch nicht übergeben |
| #13 / US-12 | Keine passenden Treffer | Mindestabdeckung, feste Antworten und Tests für leere bzw. ausgeschlossene Treffer umgesetzt |

## Keine passenden Treffer (#13 / US-12)

Die Änderungen liegen auf `feature/13-no-matching-recipes`. Dieser Branch baut auf
`feature/10-hard-recipe-filters` auf, damit die Filterprüfung zusammen mit dem Verhalten
ohne Treffer getestet werden kann. Ein PR für #13 kann zunächst #10 als Zielbranch
verwenden. Ein direkter Merge nach `main` würde auch die Änderungen von #10 enthalten
und ist vorerst nicht vorgesehen.

Die aktuelle Wortsuche verlangt mindestens **60 % Suchbegriff-Abdeckung**:
Anzahl der unterschiedlichen passenden Suchbegriffe geteilt durch die Anzahl aller
Suchbegriffe. Jeder Begriff zählt nur einmal; mehrfach vorkommende Zutaten erhöhen
die Abdeckung nicht. Bei einer einzigen gesuchten Zutat reicht ein exakter Worttreffer.
Bei `rice chicken onion` reicht `rice` allein nicht, `rice` plus `chicken` hingegen schon.
Die Grenze ist eine vorläufige Heuristik und muss mit dem Golden Dataset evaluiert
und bei der Einführung der hybriden Suche neu abgestimmt werden.

Die Prüfung verwendet ganze Wörter bzw. zusammenhängende Wortgruppen in den
Zutatennamen. `ham` passt deshalb nicht auf `graham flour`. Groß-/Kleinschreibung wird
ignoriert. Einfache englische Plurale werden gleich behandelt (`egg` = `eggs`,
`tomato` = `tomatoes`, `berry` = `berries`): Zutat und Suchbegriff werden dazu in Wörter
zerlegt und auf eine gemeinsame Grundform gebracht. Unregelmäßige Plurale (`leaves`),
Übersetzungen und Synonyme werden nicht aufgelöst.
Die Nachricht wird wie bisher in Wörter zerlegt und um Füllwörter bereinigt.

Enthält die Nachricht Suchbegriffe, bestimmen diese die Relevanz. Vorratszutaten können
eine unpassende konkrete Anfrage nicht zu einem Treffer machen. Enthält die Nachricht
keine Suchbegriffe, dienen die ausgewählten Vorratszutaten als Suchbegriffe.

Leere Ergebnisse erhalten eine feste englische Antwort für einen von drei Fällen:

- Keine Suchbegriffe: Zutaten eingeben oder Vorratszutaten auswählen.
- Keine ausreichend relevanten Rezepte: keine passende Empfehlung gefunden.
- Relevante Rezepte vorhanden, aber alle durch harte Filter ausgeschlossen:
  kein Rezept erfüllt alle Filter; Einschränkungen wurden nicht gelockert.

In diesen Fällen gibt es keine Rezeptkarten und keinen Modellaufruf. Der Trace enthält
die verwendeten Suchbegriffe, die feste Antwort, leere Treffer und `geminiCalled=false`.
Bei Treffern bedeutet der Trace-Score jetzt die Anzahl passender Suchbegriffe.
Die Prüfung der Relevanz und aller Filter erfolgt vor dem Ergebnislimit.

Verbindliche Einschränkungen werden weiterhin über die strukturierten Filter übergeben.
Die Ableitung von Einschränkungen aus Freitext und der noch fehlende Allergenfilter
werden durch #13 nicht ergänzt. AC-12.5 ist für die bereits unterstützten Filter getestet;
die Prüfung mit echten Allergendaten folgt nach deren Integration in #10.

## Paket `retrieval`

Die Suche liegt im Paket `jku.pr_dke.mymakrochef.retrieval` (Branch `feature/retrieval-service`).
Der `ChatService` ruft sie nur noch auf; das Verhalten ist unverändert. Andere Komponenten,
z. B. das Tool `rezept_suche` im Harness, können dieselbe Suche verwenden.

- `RetrievalService.search(RetrievalRequest)`: Suchbegriffe bestimmen, Mindestabdeckung prüfen,
  harte Filter anwenden, mit dem `RecipeReranker` bewerten, höchstens 5 Treffer.
- `RetrievalRequest`: `query` (Nachricht), `pantry` (Namen der ausgewählten Vorratszutaten),
  `filters` (`RecipeFilters`, optional), `budget` (`MacroBudget`, optional).
- `RetrievalResult`: `hits`, `outcome`, `searchTerms`.
- `RetrievalHit`: `recipe` (mit `recipeId()`), `score` (0 bis 1), `matchedTerms`, `reason`.
- `RetrievalOutcome`: `FOUND`, `NO_SEARCH_TERMS`, `NOT_RELEVANT`, `FILTERS_EXCLUDED_ALL`.

Die Suche liefert nur den Grund eines leeren Ergebnisses. Die englischen Antworttexte
wählt weiterhin der `ChatService`. Der Chat zeigt die besten 3 Treffer als Karten und schickt
nur diese an Gemini; der Trace enthält alle Treffer der Suche mit `matchedTerms` als Score.

## Reranking (#12 / US-11)

Rezepte, die Mindestabdeckung und harte Filter bestanden haben, bewertet der `RecipeReranker`.
Weil erst nach den Filtern bewertet wird, kann der Score keinen Filter aufheben (AC-11.4).

**Score = (0,5 × Anfrage + 0,3 × Vorrat + 0,2 × Budget) / Summe der verwendeten Gewichte**

| Teilnote | Berechnung (0 bis 1) |
| --- | --- |
| Anfrage | passende Suchbegriffe / alle Suchbegriffe |
| Vorrat | Rezeptzutaten im Vorrat / alle Rezeptzutaten; Zutaten nur aus Salz, Pfeffer, Wasser und beschreibenden Wörtern (`fresh ground black pepper`, `boiling water`) zählen nicht |
| Budget | Durchschnitt über die Werte mit Budget: 1, wenn das Rezept passt, sonst `1 − Überschreitung / Budget`, mindestens 0; fehlender Rezeptwert = 0 |

Ist kein Vorrat bzw. kein Budget angegeben oder hat ein Rezept nur Salz, Pfeffer und Wasser,
fällt die Teilnote weg und wird nicht als 0 gezählt. Bei gleichem Score bleibt die Reihenfolge
der Kandidaten erhalten. Beispiel: Anfrage 2/2, Vorrat 2 von 3, Budget passt
→ (0,5 × 1 + 0,3 × 2/3 + 0,2 × 1) / 1,0 = 0,9.

Jeder Treffer enthält eine Begründung, z. B.
`matches 2 of 2 search terms: rice, chicken; you have 2 of 3 ingredients; fits your remaining budget`.

Offene Punkte:

- Die Gewichte sind ein Startwert und werden mit dem Golden Dataset (US-17) überprüft.
- Der Vorrat kann eine vollständig passende Anfrage überholen, wenn ein anderes relevantes
  Rezept fast ganz aus dem Vorrat kochbar ist.
- Eiweiß wird wie die anderen Werte behandelt; eine Überschreitung senkt die Note, obwohl
  mehr Eiweiß oft unproblematisch ist.
- Der Chat übergibt noch kein Budget, weil Profil und Tagesbudget nicht an `/api/chat`
  geschickt werden. Bis dahin zählen nur Anfrage und Vorrat.

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
Fehlt ein passendes Tag, wird das Rezept ausgeschlossen.

Die Tags sind nicht immer richtig: In der Datenbank enthalten z. B. als `vegetarian`
getaggte Rezepte Hühnerbrühe und als `vegan` getaggte Rezepte Butter, Milch, Ei oder Honig.
Deshalb werden zusätzlich die Zutaten geprüft:

- Fleisch und Fisch (z. B. `chicken`, `beef`, `bacon`, `fish`, `anchovy`) schließen
  vegetarische und vegane Rezepte aus, außer der Zutatenname kennzeichnet Ersatz
  (`vegetarian bacon`, `soy chorizo`).
- Tierische Produkte (z. B. `egg`, `milk`, `butter`, `cheese`, `cream`, `honey`) schließen
  vegane Rezepte aus, außer der Name kennzeichnet ein Pflanzenprodukt (`coconut milk`,
  `peanut butter`, `cream of tartar`).

Die Wortlisten stehen in `RecipeHardFilter`. Die Prüfung ergänzt das Tag, ersetzt es aber
nicht: Ohne Tag bleibt ein Rezept ausgeschlossen. Versteckte tierische Bestandteile, etwa
Sardellen in `worcestershire sauce` oder Milch in Schokolade, erkennt sie nicht.

Fehlen Werte für einen aktiven Nährwert- oder Zeitfilter, wird das Rezept ausgeschlossen.
Negative oder nicht endliche Grenzwerte, vertauschte Unter-/Obergrenzen und negative
Zeitlimits werden mit HTTP 400 abgelehnt. Die Filter greifen vor Ranking und Ergebnislimit.
Wenn kein Treffer übrig bleibt, wird die bestehende Antwort ohne Rezeptkarten und ohne
Modellaufruf verwendet. Einschränkungen werden nicht gelockert.

Die Filter werden aktuell nur als strukturierte API-Felder ausgewertet. Eine Ableitung
aus Freitext und die Eingabe im Frontend sind noch nicht angebunden.

## Offene Abhängigkeiten

Die Änderungen zu #10 bleiben vorerst getrennt von `main`. Issue #10 ist noch nicht vollständig
umgesetzt und soll durch einen späteren Pull Request nicht automatisch geschlossen werden.

Vor der Implementierung von AC-09.1 muss mit Kerstin der Datenvertrag geklärt werden:

- Welche stabilen Bezeichner werden für die Allergene verwendet?
- Werden Allergene an Zutaten oder direkt an Rezepten gespeichert, und über welche
  Tabellen bzw. Felder sind sie abrufbar?
- Wie unterscheiden wir geprüfte Rezepte ohne erkannte Allergene von Rezepten mit
  noch unbekannter oder unvollständiger Allergenkennzeichnung?
- Welche Beispieldaten stehen für positive, ausgeschlossene und unbekannte Fälle bereit?

Bis zur Klärung wird keine Allergenstruktur vorausgesetzt. Ein fehlender Allergeneintrag
darf später nicht automatisch als allergenfrei gelten. Die Abstimmung ist noch offen;
es wurde keine Nachricht an Kerstin gesendet.

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
mvn -Dtest=RecipeHardFilterTest,RecipeRelevanceTest,RecipeRerankerTest,RetrievalServiceTest,ChatFilteringApiTest test
```

Die Tests benötigen weder Datenbank noch Gemini. Sie prüfen Grenzen, unbekannte Werte,
Ernährungsformen, kombinierte Filter, JSON-Kompatibilität und die Anwendung vor dem
Ergebnislimit sowie den Fall ohne zulässige Treffer. Die MockMvc-Tests prüfen zusätzlich
den HTTP-Vertrag einschließlich HTTP 400 bei ungültigen Filtern. Sie verwenden den echten
Controller und ChatService mit kontrollierten Rezeptdaten. Ein Test mit echter Datenbank
und ein vollständiger End-to-End-Test bleiben noch aus.

Die zusätzlichen Tests für #13 prüfen die 60-%-Grenze, Teilwort-Fehlmatches,
mehrfach vorkommende Zutaten, leere Suchanfragen, den Vorrats-Fallback und den
Vorrang konkreter Suchanfragen. Bei abgelehnten Treffern prüfen sie auch, dass
kein Modell aufgerufen wird und der Trace zur Antwort passt.

`RetrievalServiceTest` prüft die Suche direkt, ohne Controller und Gemini: alle vier
Outcomes, Reihenfolge, Limit, Score und Begründung sowie den Vorrats-Fallback.

Die Tests für Zutatenprüfung und Plurale decken falsch getaggte Rezepte, pflanzliche
Zutaten mit ähnlichem Namen (`eggplant`, `butternut squash`, `coconut milk`) und
einfache Pluralformen ab.

`RecipeRerankerTest` prüft die Formel mit von Hand nachgerechneten Werten, weggelassene
Teilnoten, Budgetüberschreitung, das Ignorieren von Salz, Pfeffer und Wasser sowie die
Reihenfolge. `RetrievalServiceTest` prüft zusätzlich, dass Vorrat und Budget die Reihenfolge
beeinflussen, aber kein Rezept mit hohem Score einen harten Filter umgehen kann.

Letzter lokaler Lauf: **101 Tests bestanden**, keine Fehler oder übersprungenen Tests
(35 Filter-/Service-Tests, 21 Relevanz-Testfälle, 18 Reranking-Tests, 9 Retrieval-Tests
und 18 API-Testfälle).
