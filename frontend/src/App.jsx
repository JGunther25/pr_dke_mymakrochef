
import { useState, useEffect, useRef } from 'react';
import './App.css';
import { sendChatMessage } from './recipeService';


const startIngredients = [
    { id: 1, name: 'Reis', amount: 500, unit: 'g', selected: true },
    { id: 2, name: 'Nudeln', amount: 250, unit: 'g', selected: false },
    { id: 3, name: 'Haferflocken', amount: 400, unit: 'g', selected: false },
    { id: 4, name: 'Eier', amount: 6, unit: 'Stück', selected: true },
    { id: 5, name: 'Hähnchenbrust', amount: 400, unit: 'g', selected: true },
    { id: 6, name: 'Paprika', amount: 3, unit: 'Stück', selected: true }
];

function App() {
    const [ingredients, setIngredients] = useState(() => {
        try {
            const saved = localStorage.getItem('mymakrochef-pantry');
            return saved ? JSON.parse(saved) : startIngredients;
        } catch {
            return startIngredients;
        }
    });

    const [newName, setNewName] = useState('');
    const [newAmount, setNewAmount] = useState('');
    const [newUnit, setNewUnit] = useState('g');
    const [editingId, setEditingId] = useState(null);

    const [message, setMessage] = useState('');
    const [messages, setMessages] = useState([
        { sender: 'bot', text: 'Hallo! 👋 Was möchtest du heute kochen?' }
    ]);

    //zeigt an ob gerade auf eine Antwort gewartet wird
    const [isLoading, setIsLoading] = useState(false);

    const messagesEndRef = useRef(null);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({
            behavior: 'smooth',
            block: 'end'
        });
    }, [messages, isLoading]);



    function updateIngredients(updated) {
        setIngredients(updated);
        localStorage.setItem('mymakrochef-pantry', JSON.stringify(updated));
    }

    function addIngredient(event) {
        event.preventDefault();

        const name = newName.trim();
        const amount = Number(newAmount);

        if (!name || !newAmount || !Number.isFinite(amount) || amount <= 0) {
            return;
        }

        const existing = ingredients.find(
            item => item.name.toLowerCase() === name.toLowerCase()
                && item.unit === newUnit
        );

        if (existing) {
            updateIngredients(ingredients.map(item =>
                item.id === existing.id
                    ? { ...item, amount: item.amount + amount, selected: true }
                    : item
            ));
        } else {
            updateIngredients([
                ...ingredients,
                {
                    id: crypto.randomUUID(),
                    name,
                    amount,
                    unit: newUnit,
                    selected: true
                }
            ]);
        }

        setNewName('');
        setNewAmount('');
        setNewUnit('g');
    }

    function toggleIngredient(id) {
        updateIngredients(ingredients.map(item =>
            item.id === id ? { ...item, selected: !item.selected } : item
        ));
    }

    function changeAmount(id, value) {
        const amount = Number(value);

        updateIngredients(ingredients.map(item =>
            item.id === id
                ? { ...item, amount: value === '' ? '' : amount }
                : item
        ));
    }

    function finishEditing(id) {
        const item = ingredients.find(ingredient => ingredient.id === id);

        if (item && (!Number.isFinite(Number(item.amount)) || Number(item.amount) <= 0)) {
            updateIngredients(ingredients.map(ingredient =>
                ingredient.id === id ? { ...ingredient, amount: 1 } : ingredient
            ));
        }

        setEditingId(null);
    }

    function deleteIngredient(id) {
        updateIngredients(ingredients.filter(item => item.id !== id));
    }


    async function sendMessage(event) {
        event.preventDefault();

        const userMessage = message.trim();

        if (!userMessage || isLoading) return;

        setMessages(previous => [
            ...previous,
            { sender: 'user', text: userMessage }
        ]);

        setMessage('');
        setIsLoading(true);

        try {
            const response = await sendChatMessage(
                userMessage,
                ingredients
            );

            setMessages(previous => [
                ...previous,
                {
                    sender: 'bot',
                    text: response.reply,
                    recipes: response.recipes
                }
            ]);
        } catch (error) {
            setMessages(previous => [
                ...previous,
                {
                    sender: 'bot',
                    text: 'Entschuldigung, es ist ein Fehler aufgetreten.'
                }
            ]);
        } finally {
            setIsLoading(false);
        }
    }


    const selectedCount = ingredients.filter(item => item.selected).length;

    return (
        <div className="app">
            <header className="header">
                <h1>🥗 my makro chef</h1>
                <nav>
                    <span>💬 Chat</span>
                    <span>🧺 Vorrat</span>
                </nav>
            </header>

            <main className="layout">
                <section className="chat">
                    <h2>Dein Ernährungsassistent</h2>


                    <div className="messages">
                        {messages.map((item, index) => (
                            <div key={index} className="chat-entry">
                                <div className={`message ${item.sender}`}>
                                    {item.text}
                                </div>

                                {item.recipes && item.recipes.length > 0 && (
                                    <div className="recipes">
                                        {item.recipes.map(recipe => (
                                            <article className="recipe" key={recipe.id}>
                                                <h3>{recipe.name}</h3>

                                                <p className="calories">
                                                    {recipe.calories} kcal pro Portion
                                                </p>

                                                <div className="macro-values">
                                                    <span> {recipe.protein} g Protein</span>
                                                    <span> {recipe.carbs} g Kohlenhydrate</span>
                                                    <span> {recipe.fat} g Fett</span>
                                                </div>

                                                <h4>Zutaten</h4>
                                                <ul>
                                                    {recipe.ingredients.map(ingredient => (
                                                        <li key={ingredient}>{ingredient}</li>
                                                    ))}
                                                </ul>

                                                {recipe.missingIngredients.length > 0 && (
                                                    <p className="missing">
                                                        Fehlende Zutaten: {' '}
                                                        {recipe.missingIngredients.join(', ')}
                                                    </p>
                                                )}

                                                {recipe.sourceUrl && (
                                                    <a
                                                        href={recipe.sourceUrl}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                    >
                                                        Originalrezept ansehen
                                                    </a>
                                                )}
                                            </article>
                                        ))}
                                    </div>
                                )}
                            </div>
                        ))}

                        {isLoading && (
                            <div className="message bot loading">
                                ⏳ Rezeptvorschläge werden gesucht ...
                            </div>
                        )}
                        <div ref={messagesEndRef} />
                    </div>

                    <form className="message-form" onSubmit={sendMessage}>
                        <input
                            value={message}
                            onChange={event => setMessage(event.target.value)}
                            placeholder="Was möchtest du heute kochen?"
                        />
                        <button type="submit">Senden</button>
                    </form>
                </section>

                <aside className="pantry">
                    <h2>🧺 Vorratsverwaltung</h2>
                    <p className="pantry-subtitle">
                        {selectedCount} von {ingredients.length} Zutaten ausgewählt
                    </p>

                    <div className="ingredient-list">
                        {ingredients.map(item => (
                            <div className="ingredient" key={item.id}>
                                <input
                                    type="checkbox"
                                    checked={item.selected}
                                    onChange={() => toggleIngredient(item.id)}
                                    aria-label={`${item.name} auswählen`}
                                />

                                <span className={item.selected ? 'ingredient-selected' : ''}>
                  {item.name}
                </span>

                                {editingId === item.id ? (
                                    <div className="edit-amount">
                                        <input
                                            type="number"
                                            min="0.01"
                                            step="any"
                                            value={item.amount}
                                            onChange={event => changeAmount(item.id, event.target.value)}
                                            aria-label={`Menge von ${item.name}`}
                                        />
                                        <small>{item.unit}</small>
                                        <button
                                            type="button"
                                            className="small-button"
                                            onClick={() => finishEditing(item.id)}
                                        >
                                            ✓
                                        </button>
                                    </div>
                                ) : (
                                    <span className="ingredient-amount">
                    {item.amount} {item.unit}
                  </span>
                                )}

                                <div className="ingredient-actions">
                                    <button
                                        type="button"
                                        className="icon-button"
                                        onClick={() => setEditingId(
                                            editingId === item.id ? null : item.id
                                        )}
                                        aria-label={`${item.name} bearbeiten`}
                                        title="Menge bearbeiten"
                                    >
                                        ✎
                                    </button>
                                    <button
                                        type="button"
                                        className="icon-button delete-button"
                                        onClick={() => deleteIngredient(item.id)}
                                        aria-label={`${item.name} löschen`}
                                        title="Zutat löschen"
                                    >
                                        ×
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>

                    <h3 className="add-heading">Weitere Zutat hinzufügen</h3>

                    <form className="pantry-form" onSubmit={addIngredient}>
                        <input
                            placeholder="Lebensmittel, z. B. Tomaten"
                            value={newName}
                            onChange={event => setNewName(event.target.value)}
                            required
                        />

                        <div className="amount-row">
                            <input
                                type="number"
                                min="0.01"
                                step="any"
                                placeholder="Menge"
                                value={newAmount}
                                onChange={event => setNewAmount(event.target.value)}
                                required
                            />

                            <select
                                value={newUnit}
                                onChange={event => setNewUnit(event.target.value)}
                            >
                                <option value="g">g</option>
                                <option value="kg">kg</option>
                                <option value="ml">ml</option>
                                <option value="l">l</option>
                                <option value="Stück">Stück</option>
                            </select>
                        </div>

                        <button type="submit">+ Zutat hinzufügen</button>
                    </form>
                </aside>
            </main>
        </div>
    );
}

export default App;
