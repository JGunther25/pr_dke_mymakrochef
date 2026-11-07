
const demoRecipes = [
    {
        id: 1,
        name: "Hähnchen-Reis-Pfanne",
        calories: 620,
        protein: 48,
        carbs: 72,
        fat: 14,
        ingredients: [
            "150 g Hähnchenbrust",
            "80 g Reis",
            "1 Paprika",
            "10 g Olivenöl"
        ],
        missingIngredients: ["Olivenöl"],
        sourceUrl: null
    },
    {
        id: 2,
        name: "Gemüse-Omelett",
        calories: 390,
        protein: 29,
        carbs: 18,
        fat: 22,
        ingredients: [
            "3 Eier",
            "1 Paprika",
            "50 g Spinat"
        ],
        missingIngredients: ["Spinat"],
        sourceUrl: null
    }
];

// Simuliert die spätere Antwort des Java-Backends
export async function sendChatMessage(message, pantry) {
    await new Promise(resolve => setTimeout(resolve, 800));

    return {
        reply: "Hier sind zwei Beispielrezepte. Die echte Rezeptsuche wird später mit dem Backend verbunden.",
        recipes: demoRecipes
    };
}
