
import { useState, useEffect, useRef } from 'react';
import './App.css';
import { sendChatMessage } from './recipeService';
import Profile from './Profile';
import SidePanel from './SidePanel';
import Logo from './icons/Logo';
import ChatIcon from './icons/ChatIcon';
import PantryIcon from './icons/PantryIcon';
import ProfileIcon from './icons/ProfileIcon';
import HomeIcon from './icons/HomeIcon';
import BroccoliIcon from './icons/BroccoliIcon';
import './icons/icons.css';
import Login from './Login';
import * as userService from './userService';
import * as authService from './authService';


const startIngredients = [
    { id: 1, name: 'rice', amount: 500, unit: 'g', selected: true },
    { id: 2, name: 'pasta', amount: 250, unit: 'g', selected: false },
    { id: 3, name: 'oats', amount: 400, unit: 'g', selected: false },
    { id: 4, name: 'eggs', amount: 6, unit: 'pcs', selected: true },
    { id: 5, name: 'chicken breast', amount: 400, unit: 'g', selected: true },
    { id: 6, name: 'bell pepper', amount: 3, unit: 'pcs', selected: true }
];

const ALLERGEN_CODES = {
    1: 'gluten',
    2: 'crustaceans',
    3: 'eggs',
    4: 'fish',
    5: 'peanuts',
    6: 'soy',
    7: 'milk',
    8: 'tree_nuts',
    9: 'celery',
    10: 'mustard',
    11: 'sesame',
    12: 'sulphites',
    13: 'lupin',
    14: 'molluscs'
};

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
    { name: 'chat', label: 'Chat', Icon: ChatIcon },
    { name: 'pantry', label: 'Pantry', Icon: PantryIcon },
    { name: 'profile', label: 'Profile', Icon: ProfileIcon }
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

function MainApp({onLogout}) {
    const [ingredients, setIngredients] = useState([]);
    const [profile, setProfile] = useState(emptyProfile);
    const [staples, setStaples] = useState([]);

    const [isDataLoading, setIsDataLoading] = useState(true);
    const [dataError, setDataError] = useState('');

    const [saveError, setSaveError] = useState('');
    const profileSaveTimer = useRef(null);
    const profileSaveQueue = useRef(Promise.resolve());
    const pendingProfileSave = useRef(null);

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

    // Brokkoli in der Mitte: 'thinking' beim Warten, 'wink' kurz bevor die Antwort erscheint
    const [broccoliMode, setBroccoliMode] = useState('thinking');

    const messagesEndRef = useRef(null);

    // true, solange der Nutzer gerade tippt (für die Punkte im Chat-Icon)
    const [isTyping, setIsTyping] = useState(false);
    const typingTimerRef = useRef(null);

    function changeMessage(value) {
        setMessage(value);
        setIsTyping(true);

        // 1,2 Sekunden nach dem letzten Tastendruck gilt man nicht mehr als tippend
        clearTimeout(typingTimerRef.current);
        typingTimerRef.current = setTimeout(() => setIsTyping(false), 1200);
    }

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({
            behavior: 'smooth',
            block: 'end'
        });
    }, [messages, isLoading]);

    useEffect(() => {

        let cancelled = false;

        async function loadUserData() {

            setIsDataLoading(true);
            setDataError('');

            try {
                const [
                    savedProfile,
                    savedPantry,
                    savedStaples,
                    savedAllergens
                ] = await Promise.all([
                    userService.getProfile(),
                    userService.getPantry(),
                    userService.getStaples(),
                    userService.getAllergens()
                ]);

                if (cancelled) return;

                setProfile({
                    calories: savedProfile.calories ?? '',
                    protein: savedProfile.protein ?? '',
                    carbs: savedProfile.carbs ?? '',
                    fat: savedProfile.fat ?? '',
                    diet: savedProfile.diet ?? 'none',
                    allergies: savedAllergens
                        .map(id => ALLERGEN_CODES[id])
                        .filter(Boolean)
                });

                setIngredients(savedPantry.map(item => ({
                    id: item.id,
                    name: item.ingredientName,
                    amount: item.amount,
                    unit: item.unit ?? 'g',
                    selected: item.selected ?? true
                })));

                setStaples(savedStaples.map(
                    item => item.ingredientName
                ));

            } catch (error) {
                if (!cancelled) {
                    setDataError(error.message);
                }
            } finally {
                if (!cancelled) {
                    setIsDataLoading(false);
                }
            }
        }

        loadUserData();

        return () => {
            cancelled = true;
        };

    }, []);

    function toPantryItem(item) {
        return {
            ingredientName: item.name,
            amount: item.amount === '' ? null : item.amount,
            unit: item.unit,
            selected: item.selected
        };
    }

    function updateProfile(updated) {

        setProfile(updated);
        setSaveError('');

        clearTimeout(profileSaveTimer.current);

        pendingProfileSave.current = () => {

            profileSaveQueue.current = profileSaveQueue.current
                .catch(() => {})
                .then(async () => {

                    try {
                        await userService.saveProfile( {
                            calories: updated.calories === '' ? null : updated.calories,
                            protein: updated.protein === '' ? null : updated.protein,
                            carbs: updated.carbs === '' ? null : updated.carbs,
                            fat: updated.fat === '' ? null : updated.fat,
                            diet: updated.diet
                        });

                        const allergenIds = Object.entries(ALLERGEN_CODES)
                            .filter(([id, code]) =>
                                updated.allergies.includes(code)
                            )
                            .map(([id]) => Number(id));

                        await userService.saveAllergens(
                            allergenIds
                        );

                        setSaveError('');

                    } catch (error) {
                        setSaveError('Could not save your profile.');
                        throw error;
                    }
                });

            pendingProfileSave.current = null;
        };

        profileSaveTimer.current = setTimeout(() => {
            pendingProfileSave.current?.();
            profileSaveQueue.current.catch(() => {});
        }, 600);
    }

    async function logoutWithSave() {

        clearTimeout(profileSaveTimer.current);

        pendingProfileSave.current?.();

        try {
            await profileSaveQueue.current;
            await onLogout();

        } catch (error) {
            setSaveError(
                'Could not save your changes. Please try again.'
            );
        }
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

    async function addStaple(event) {
        event.preventDefault();

        const name = newStaple.trim().toLowerCase();

        if (!name || staples.includes(name)) return;

        try {
            setSaveError('');

            await userService.addStaple( {
                ingredientName: name
            });

            setStaples(previous => [...previous, name]);
            setNewStaple('');

        } catch (error) {
            console.error(error);
            setSaveError('Could not add staple.');
        }
    }

    async function removeStaple(name) {

        try {
            setSaveError('');

            const savedStaples = await userService.getStaples(
            );

            const staple = savedStaples.find(
                item => item.ingredientName === name
            );

            if (staple) {
                await userService.deleteStaple(
                    staple.id
                );
            }

            setStaples(previous =>
                previous.filter(item => item !== name)
            );

        } catch (error) {
            console.error(error);
            setSaveError('Could not delete staple.');
        }
    }

    // leert nur den Vorrat, "Always at home" bleibt
    async function clearPantry() {
        if (ingredients.length === 0) return;

        if (window.confirm('Remove all ingredients from your pantry?')) {
            try {
                setSaveError('');

                await userService.clearPantry();

                setIngredients([]);

            } catch (error) {
                console.error(error);
                setSaveError('Could not clear pantry.');
            }
        }
    }

    async function addIngredient(event) {
        event.preventDefault();

        const name = newName.trim();
        const amount = newAmount === '' ? null : Number(newAmount);

        if (!name || (amount !== null &&
            (!Number.isFinite(amount) || amount <= 0))) {
            return;
        }

        const existing = ingredients.find(
            item => item.name.toLowerCase() === name.toLowerCase()
        );

        try {
            setSaveError('');

            const item = {
                name,
                amount,
                unit: newUnit,
                selected: true
            };

            if (existing) {
                const saved = await userService.updatePantryItem(
                    existing.id,
                    toPantryItem(item)
                );

                setIngredients(previous => previous.map(ingredient =>
                    ingredient.id === existing.id
                        ? { ...item, id: saved.id }
                        : ingredient
                ));

            } else {
                const saved = await userService.addPantryItem(
                    toPantryItem(item)
                );

                setIngredients(previous => [
                    ...previous,
                    { ...item, id: saved.id }
                ]);
            }

            setNewName('');
            setNewAmount('');
            setNewUnit('g');

        } catch (error) {
            console.error(error);
            setSaveError('Could not add ingredient.');
        }
    }

    async function toggleIngredient(id) {

        const item = ingredients.find(
            ingredient => ingredient.id === id
        );

        if (!item) return;

        const updated = {
            ...item,
            selected: !item.selected
        };

        try {
            setSaveError('');

            await userService.updatePantryItem(
                id,
                toPantryItem(updated)
            );

            setIngredients(previous => previous.map(ingredient =>
                ingredient.id === id ? updated : ingredient
            ));

        } catch (error) {
            console.error(error);
            setSaveError('Could not update ingredient.');
        }
    }

    function changeAmount(id, value) {

        const amount = Number(value);

        setIngredients(previous => previous.map(item =>
            item.id === id
                ? { ...item, amount: value === '' ? '' : amount }
                : item
        ));
    }

    async function finishEditing(id) {

        const item = ingredients.find(
            ingredient => ingredient.id === id
        );

        if (!item) return;

        let amount = item.amount;

        if (amount === '' || amount === null) {
            amount = null;
        } else if (!Number.isFinite(Number(amount)) ||
            Number(amount) <= 0) {
            amount = 1;
        }

        const updated = {
            ...item,
            amount
        };

        try {
            setSaveError('');

            await userService.updatePantryItem(
                id,
                toPantryItem(updated)
            );

            setIngredients(previous => previous.map(ingredient =>
                ingredient.id === id ? updated : ingredient
            ));

            setEditingId(null);

        } catch (error) {
            console.error(error);
            setSaveError('Could not save ingredient amount.');
        }
    }

    async function deleteIngredient(id) {
        try {
            setSaveError('');

            await userService.deletePantryItem(id);

            setIngredients(previous =>
                previous.filter(item => item.id !== id)
            );

        } catch (error) {
            console.error(error);
            setSaveError('Could not delete ingredient.');
        }
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
        setIsTyping(false);
        clearTimeout(typingTimerRef.current);
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

            // Brokkoli zwinkert kurz, dann erscheint die Antwort
            setBroccoliMode('wink');
            await new Promise(resolve => setTimeout(resolve, 700));

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
            setBroccoliMode('thinking');
        }
    }



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

    if (isDataLoading) {
        return (
            <div className="login-page">
                <p>Loading your personal data...</p>
            </div>
        );
    }

    if (dataError) {
        return (
            <div className="login-page">
                <div className="login-card">
                    <p>{dataError}</p>
                    <button onClick={logoutWithSave}>
                        Back to login
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className={`app ${isTyping ? 'is-typing' : ''}`}>
            <header className="header">
                <h1><Logo /></h1>
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
                            <item.Icon size={34} /> {item.label}
                        </button>
                    ))}
                    <button
                        type="button"
                        className="logout-button"
                        onClick={onLogout}
                    >
                        Log out
                    </button>
                </nav>
            </header>

            {saveError && (
                <p role="alert" style={{ color: '#c0392b', textAlign: 'center' }}>
                    {saveError}
                </p>
            )}

            <main className={`layout ${layout.hidden.chat ? 'chat-hidden' : ''}`}>
                {!layout.hidden.chat && (
                    <section className="chat">
                        <h2>Your nutrition assistant</h2>


                        <div className="messages">
                            {messages.map((item, index) => (
                                <div key={index} className={`chat-entry ${item.sender}`}>
                                    <div className="message-row">
                                        <span className="avatar">
                                            {item.sender === 'bot'
                                                ? <BroccoliIcon size={36} />
                                                : <ProfileIcon size={36} />}
                                        </span>
                                        <div className={`message ${item.sender}`}>
                                            {item.text}
                                        </div>
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
                                <div className="thinking">
                                    <BroccoliIcon size={110} mode={broccoliMode} />
                                    <p>Looking for recipes …</p>
                                </div>
                            )}
                            <div ref={messagesEndRef} />
                        </div>

                        <form className="message-form" onSubmit={sendMessage}>
                            <input
                                value={message}
                                onChange={event => changeMessage(event.target.value)}
                                placeholder="What would you like to cook today?"
                            />
                            <button type="submit">Send</button>
                        </form>
                    </section>
                )}

                {!layout.hidden.pantry && (
                    <SidePanel
                        icon={<PantryIcon size={48} />}
                        title="Pantry"
                        className="pantry"
                        width={layout.pantry.width}
                        collapsed={layout.pantry.collapsed}
                        onToggle={() => togglePanel('pantry')}
                        onResize={width => resizePanel('pantry', width)}
                        bottom={{
                            icon: <HomeIcon size={44} />,
                            title: `Always at home (${staples.length})`,
                            height: layout.staples.height,
                            collapsed: layout.staples.collapsed,
                            onToggle: () => togglePanel('staples'),
                            onResize: height => resizeHeight('staples', height),
                            content: staplesContent
                        }}
                    >

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
                                            onClick={async () => {
                                                if (editingId !== null) {
                                                    await finishEditing(editingId);
                                                } else {
                                                    setEditingId(item.id);
                                                }
                                            }}
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
                        icon={<ProfileIcon size={48} />}
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

function App() {

    const [authenticated, setAuthenticated] = useState(false);
    const [checkingSession, setCheckingSession] = useState(true);
    const [sessionError, setSessionError] = useState('');

    useEffect(() => {

        async function checkLogin() {
            try {
                const data = await authService.checkSession();

                setAuthenticated(data.authenticated === true);

            } catch (error) {
                console.error(error);
                setAuthenticated(false);

            } finally {
                setCheckingSession(false);
            }
        }

        checkLogin();

    }, []);

    async function handleLogout() {
        try {
            setSessionError('');
            await authService.logout();

            setAuthenticated(false);

        } catch (error) {
            console.error(error);
            setSessionError('Logout failed. Please try again.');
        }
    }

    if (checkingSession) {
        return (
            <div className="login-page">
                <p>Checking session...</p>
            </div>
        );
    }

    if (!authenticated) {
        return <Login onLogin={setAuthenticated} />;
    }

    return (
        <>
            {sessionError && (
                <p role="alert">{sessionError}</p>
            )}

            <MainApp
                onLogout={handleLogout}
            />
        </>
    );
}

export default App;
