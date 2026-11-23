
// Die 14 EU-Allergene, gleiche Codes wie in der Tabelle allergen in Supabase
const ALLERGENS = [
    { code: 'gluten', name: 'Gluten' },
    { code: 'crustaceans', name: 'Crustaceans' },
    { code: 'eggs', name: 'Eggs' },
    { code: 'fish', name: 'Fish' },
    { code: 'peanuts', name: 'Peanuts' },
    { code: 'soy', name: 'Soy' },
    { code: 'milk', name: 'Milk' },
    { code: 'tree_nuts', name: 'Tree nuts' },
    { code: 'celery', name: 'Celery' },
    { code: 'mustard', name: 'Mustard' },
    { code: 'sesame', name: 'Sesame' },
    { code: 'sulphites', name: 'Sulphites' },
    { code: 'lupin', name: 'Lupin' },
    { code: 'molluscs', name: 'Molluscs' }
];

// Tagesziele, leer bedeutet: kein Ziel gesetzt
const GOALS = [
    { field: 'calories', label: 'Calories', unit: 'kcal' },
    { field: 'protein', label: 'Protein', unit: 'g' },
    { field: 'carbs', label: 'Carbs', unit: 'g' },
    { field: 'fat', label: 'Fat', unit: 'g' }
];

// Zeigt das Profil an und meldet jede Änderung über onChange an App.jsx
function Profile({ profile, onChange }) {

    function changeField(field, value) {
        onChange({ ...profile, [field]: value });
    }

    function changeGoal(field, value) {
        const amount = Number(value);

        if (value !== '' && (!Number.isFinite(amount) || amount < 0)) {
            return;
        }

        changeField(field, value === '' ? '' : amount);
    }

    function toggleAllergy(code) {
        const allergies = profile.allergies.includes(code)
            ? profile.allergies.filter(item => item !== code)
            : [...profile.allergies, code];

        changeField('allergies', allergies);
    }

    return (
        <div className="profile">
            <p className="pantry-subtitle">Saved in this browser</p>

            <h3 className="add-heading">Daily goals</h3>
            <div className="goal-list">
                {GOALS.map(goal => (
                    <label className="goal" key={goal.field}>
                        <span>{goal.label}</span>
                        <input
                            type="number"
                            min="0"
                            step="any"
                            placeholder="no goal"
                            value={profile[goal.field]}
                            onChange={event => changeGoal(goal.field, event.target.value)}
                        />
                        <small>{goal.unit}</small>
                    </label>
                ))}
            </div>

            <h3 className="add-heading">Diet</h3>
            <select
                className="diet-select"
                value={profile.diet}
                onChange={event => changeField('diet', event.target.value)}
            >
                <option value="none">No restriction</option>
                <option value="vegetarian">Vegetarian</option>
                <option value="vegan">Vegan</option>
            </select>

            <h3 className="add-heading">Allergies</h3>
            <div className="allergy-list">
                {ALLERGENS.map(allergen => (
                    <label className="allergy" key={allergen.code}>
                        <input
                            type="checkbox"
                            checked={profile.allergies.includes(allergen.code)}
                            onChange={() => toggleAllergy(allergen.code)}
                        />
                        {allergen.name}
                    </label>
                ))}
            </div>

            <p className="allergy-note">
                Allergen information is a hint, not a guarantee.
            </p>
        </div>
    );
}

export default Profile;
