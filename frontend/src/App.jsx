
import { useState, useEffect, useRef } from 'react';
import './App.css';
import { sendChatMessage } from './recipeService';
import Profile from './Profile';
import SidePanel from './SidePanel';


const startIngredients = [
    { id: 1, name: 'rice', amount: 500, unit: 'g', selected: true },
    { id: 2, name: 'pasta', amount: 250, unit: 'g', selected: false },
    { id: 3, name: 'oats', amount: 400, unit: 'g', selected: false },
    { id: 4, name: 'eggs', amount: 6, unit: 'pcs', selected: true },
    { id: 5, name: 'chicken breast', amount: 400, unit: 'g', selected: true },
    { id: 6, name: 'bell pepper', amount: 3, unit: 'pcs', selected: true }
];

// leeres Profil: kein Tagesziel, keine Einschränkung, keine Allergien
const emptyProfile = {
    calories: '',
    protein: '',
    carbs: '',
    fat: '',
    diet: 'none',
    allergies: []
};

// Dinge, die man meistens zu Hause hat (AC-06.4), ohne Mengenangabe
const startStaples = ['salt', 'pepper', 'water', 'oil', 'sugar'];

// Breite und auf/zu der Spalten, Höhe und auf/zu von "Always at home", ausgeblendete Spalten
const startLayout = {
    pantry: { width: 320, collapsed: false },
    profile: { width: 300, collapsed: false },
    staples: { height: 200, collapsed: false },
    hidden: { chat: false, pantry: false, profile: false }
};

// Schalter oben in der Kopfzeile: blenden eine Spalte ganz aus und wieder ein
const NAV_ITEMS = [
    { name: 'chat', label: '💬 Chat' },
    { name: 'pantry', label: '🧺 Pantry' },
    { name: 'profile', label: '👤 Profile' }
];

// liest einen Wert aus dem localStorage, sonst den Startwert
function loadSaved(key, fallback) {
    try {
        const saved = localStorage.getItem(key);
        return saved ? JSON.parse(saved) : fallback;
    } catch {
        return fallback;
    }
}

function App() {
    const [ingredients, setIngredients] = useState(() => {
        try {
            const saved = localStorage.getItem('mymakrochef-pantry');
            return saved ? JSON.parse(saved) : startIngredients;
        } catch {
            return startIngredients;
        }
    });

    // Profil wie der Vorrat im localStorage, damit es nach dem Neuladen noch da ist
    const [profile, setProfile] = useState(() => {
        try {
            const saved = localStorage.getItem('mymakrochef-profile');
            return saved ? { ...emptyProfile, ...JSON.parse(saved) } : emptyProfile;
        } catch {
            return emptyProfile;
        }
    });

    const [staples, setStaples] = useState(() => loadSaved('mymakrochef-staples', startStaples));
    const [newStaple, setNewStaple] = useState('');

    const [layout, setLayout] = useState(() => ({
        ...startLayout,
        ...loadSaved('mymakrochef-layout', {})
    }));

    // Layout bei jeder Änderung speichern (auch während dem Ziehen)
    useEffect(() => {
        try {
            localStorage.setItem('mymakrochef-layout', JSON.stringify(layout));
        } catch {
            // ohne localStorage geht es trotzdem, nur ohne Speichern
        }
    }, [layout]);

    const [newName, setNewName] = useState('');
    const [newAmount, setNewAmount] = useState('');
    const [newUnit, setNewUnit] = useState('g');
    const [editingId, setEditingId] = useState(null);

    const [message, setMessage] = useState('');
    const [messages, setMessages] = useState([
        { sender: 'bot', text: 'Hi! 👋 What would you like to cook today?' }
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

    function updateProfile(updated) {
        setProfile(updated);
        localStorage.setItem('mymakrochef-profile', JSON.stringify(updated));
    }

    function togglePanel(name) {
        setLayout(previous => ({
            ...previous,
            [name]: { ...previous[name], collapsed: !previous[name].collapsed }
        }));
    }

    function resizePanel(name, width) {
        setLayout(previous => ({
            ...previous,
            [name]: { ...previous[name], width }
        }));
    }

    function toggleHidden(name) {
        setLayout(previous => {
            const hidden = { ...previous.hidden, [name]: !previous.hidden[name] };

            // mindestens eine Spalte bleibt immer sichtbar
            if (Object.values(hidden).every(Boolean)) {
                return previous;
            }

            return { ...previous, hidden };
        });
    }

    function resizeHeight(name, height) {
        setLayout(previous => ({
            ...previous,
            [name]: { ...previous[name], height }
        }));
    }

    function updateStaples(updated) {
        setStaples(updated);
        localStorage.setItem('mymakrochef-staples', JSON.stringify(updated));
    }

    function addStaple(event) {
        event.preventDefault();

        const name = newStaple.trim().toLowerCase();

        if (name && !staples.includes(name)) {
            updateStaples([...staples, name]);
        }

        setNewStaple('');
    }

    function removeStaple(name) {
        updateStaples(staples.filter(item => item !== name));
    }

    // leert nur den Vorrat, "Always at home" bleibt
    function clearPantry() {
        if (ingredients.length === 0) return;

        if (window.confirm('Remove all ingredients from your pantry?')) {
            updateIngredients([]);
        }
    }

    function addIngredient(event) {
        event.preventDefault();

        const name = newName.trim();

        // leeres Mengenfeld bedeutet: genug da, gespeichert als null (keine Menge)
        const amount = newAmount === '' ? null : Number(newAmount);

        if (!name || (amount !== null && (!Number.isFinite(amount) || amount <= 0))) {
            return;
        }

        const existing = ingredients.find(
            item => item.name.toLowerCase() === name.toLowerCase()
        );

        // gleiche Zutat nochmal eingetragen: neue Menge und Einheit ersetzen die alte
        if (existing) {
            updateIngredients(ingredients.map(item =>
                item.id === existing.id
                    ? { ...item, amount, unit: newUnit, selected: true }
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

        // Feld geleert: genug da (null), ungültige Zahl: zurück auf 1
        if (item && (item.amount === '' || item.amount === null)) {
            updateIngredients(ingredients.map(ingredient =>
                ingredient.id === id ? { ...ingredient, amount: null } : ingredient
            ));
        } else if (item && (!Number.isFinite(Number(item.amount)) || Number(item.amount) <= 0)) {
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
            // "Always at home" wird wie ein ausgewählter Vorrat ohne Menge mitgeschickt
            const stapleItems = staples.map(name => ({
                name,
                amount: null,
                unit: null,
                selected: true
            }));

            const response = await sendChatMessage(
                userMessage,
                [...ingredients, ...stapleItems]
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
                    text: 'Sorry, something went wrong.'
                }
            ]);
        } finally {
            setIsLoading(false);
        }
    }


    const selectedCount = ingredients.filter(item => item.selected).length;

    // Inhalt vom Fenster "Always at home" unter der Pantry
    const staplesContent = (
        <>
        <ul className="staple-list">
            {staples.map(name => (
                <li className="staple" key={name}>
                    <span>{name}</span>
                    <button
                        type="button"
                        className="icon-button delete-button"
                        onClick={() => removeStaple(name)}
                        aria-label={`Remove ${name}`}
                        title="Remove"
                    >
                        ×
                    </button>
                </li>
            ))}
        </ul>

        <form className="staple-form" onSubmit={addStaple}>
            <input
                placeholder="add staple, e.g. garlic"
                value={newStaple}
                onChange={event => setNewStaple(event.target.value)}
            />
            <button type="submit" aria-label="Add staple">+</button>
        </form>
        </>
    );

    return (
        <div className="app">
            <header className="header">
                <h1>🥗 my makro chef</h1>
                <nav>
                    {NAV_ITEMS.map(item => (
                        <button
                            type="button"
                            key={item.name}
                            className={`nav-toggle ${layout.hidden[item.name] ? 'off' : ''}`}
                            onClick={() => toggleHidden(item.name)}
                            aria-pressed={!layout.hidden[item.name]}
                            title={layout.hidden[item.name] ? 'Show' : 'Hide'}
                        >
                            {item.label}
                        </button>
                    ))}
                </nav>
            </header>

            <main className={`layout ${layout.hidden.chat ? 'chat-hidden' : ''}`}>
                {!layout.hidden.chat && (
                    <section className="chat">
                        <h2>Your nutrition assistant</h2>


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
                                                        {recipe.calories} kcal per serving
                                                    </p>

                                                    <div className="macro-values">
                                                        <span> {recipe.protein} g protein</span>
                                                        <span> {recipe.carbs} g carbs</span>
                                                        <span> {recipe.fat} g fat</span>
                                                    </div>

                                                    <h4>Ingredients</h4>
                                                    <ul>
                                                        {recipe.ingredients.map(ingredient => (
                                                            <li key={ingredient}>{ingredient}</li>
                                                        ))}
                                                    </ul>

                                                    {recipe.steps && recipe.steps.length > 0 && (
                                                        <>
                                                            <h4>Directions</h4>
                                                            <ol className="steps">
                                                                {recipe.steps.map((step, index) => (
                                                                    <li key={index}>{step}</li>
                                                                ))}
                                                            </ol>
                                                        </>
                                                    )}

                                                    {recipe.missingIngredients.length > 0 && (
                                                        <p className="missing">
                                                            Missing ingredients: {' '}
                                                            {recipe.missingIngredients.join(', ')}
                                                        </p>
                                                    )}

                                                    {recipe.sourceUrl && (
                                                        <a
                                                            href={recipe.sourceUrl}
                                                            target="_blank"
                                                            rel="noopener noreferrer"
                                                        >
                                                            View original recipe
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
                                    ⏳ Looking for recipes ...
                                </div>
                            )}
                            <div ref={messagesEndRef} />
                        </div>

                        <form className="message-form" onSubmit={sendMessage}>
                            <input
                                value={message}
                                onChange={event => setMessage(event.target.value)}
                                placeholder="What would you like to cook today?"
                            />
                            <button type="submit">Send</button>
                        </form>
                    </section>
                )}

                {!layout.hidden.pantry && (
                    <SidePanel
                        icon="🧺"
                        title="Pantry"
                        className="pantry"
                        width={layout.pantry.width}
                        collapsed={layout.pantry.collapsed}
                        onToggle={() => togglePanel('pantry')}
                        onResize={width => resizePanel('pantry', width)}
                        bottom={{
                            icon: '🧂',
                            title: `Always at home (${staples.length})`,
                            height: layout.staples.height,
                            collapsed: layout.staples.collapsed,
                            onToggle: () => togglePanel('staples'),
                            onResize: height => resizeHeight('staples', height),
                            content: staplesContent
                        }}
                    >
                        <p className="pantry-subtitle">
                            {selectedCount} of {ingredients.length} ingredients selected
                        </p>

                        <div className="ingredient-list">
                            {ingredients.map(item => (
                                <div className="ingredient" key={item.id}>
                                    <input
                                        type="checkbox"
                                        checked={item.selected}
                                        onChange={() => toggleIngredient(item.id)}
                                        aria-label={`Select ${item.name}`}
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
                                                value={item.amount ?? ''}
                                                onChange={event => changeAmount(item.id, event.target.value)}
                                                aria-label={`Amount of ${item.name}`}
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
                        {item.amount !== null && `${item.amount} ${item.unit}`}
                      </span>
                                    )}

                                    <div className="ingredient-actions">
                                        <button
                                            type="button"
                                            className="icon-button"
                                            onClick={() => setEditingId(
                                                editingId === item.id ? null : item.id
                                            )}
                                            aria-label={`Edit ${item.name}`}
                                            title="Edit amount"
                                        >
                                            ✎
                                        </button>
                                        <button
                                            type="button"
                                            className="icon-button delete-button"
                                            onClick={() => deleteIngredient(item.id)}
                                            aria-label={`Delete ${item.name}`}
                                            title="Delete ingredient"
                                        >
                                            ×
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>

                        <h3 className="add-heading">Add another ingredient</h3>

                        <form className="pantry-form" onSubmit={addIngredient}>
                            <input
                                placeholder="Ingredient, e.g. tomatoes"
                                value={newName}
                                onChange={event => setNewName(event.target.value)}
                                required
                            />

                            <div className="amount-row">
                                <input
                                    type="number"
                                    min="0.01"
                                    step="any"
                                    placeholder="Amount (empty = enough)"
                                    value={newAmount}
                                    onChange={event => setNewAmount(event.target.value)}
                                />

                                <select
                                    value={newUnit}
                                    onChange={event => setNewUnit(event.target.value)}
                                >
                                    <option value="g">g</option>
                                    <option value="kg">kg</option>
                                    <option value="ml">ml</option>
                                    <option value="l">l</option>
                                    <option value="pcs">pcs</option>
                                </select>
                            </div>

                            <button type="submit">+ Add ingredient</button>
                        </form>

                        <button
                            type="button"
                            className="clear-button"
                            onClick={clearPantry}
                            disabled={ingredients.length === 0}
                        >
                            Clear pantry
                        </button>
                    </SidePanel>
                )}

                {!layout.hidden.profile && (
                    <SidePanel
                        icon="👤"
                        title="Profile"
                        width={layout.profile.width}
                        collapsed={layout.profile.collapsed}
                        onToggle={() => togglePanel('profile')}
                        onResize={width => resizePanel('profile', width)}
                    >
                        <Profile profile={profile} onChange={updateProfile} />
                    </SidePanel>
                )}
            </main>
        </div>
    );
}

export default App;
