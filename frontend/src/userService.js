const API_URL = 'http://localhost:8080/api/user';

async function request(endpoint, method = 'GET', body) {

    const response = await fetch(`${API_URL}${endpoint}`, {
        method,
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json'
        },
        body: body === undefined
            ? undefined
            : JSON.stringify(body)
    });

    if (response.status === 401) {
        throw new Error('Session expired. Please log in again.');
    }

    if (!response.ok) {
        throw new Error(`Request failed: ${response.status}`);
    }

    const text = await response.text();

    return text ? JSON.parse(text) : null;
}

// PROFILE

export function getProfile() {
    return request('/profile');
}

export function saveProfile(profile) {
    return request('/profile', 'PUT', profile);
}

// PANTRY

export function getPantry() {
    return request('/pantry');
}

export function addPantryItem(item) {
    return request('/pantry', 'POST', item);
}

export function updatePantryItem(id, item) {
    return request(`/pantry/${id}`, 'PUT', item);
}

export function deletePantryItem(id) {
    return request(`/pantry/${id}`, 'DELETE');
}

export function clearPantry() {
    return request('/pantry', 'DELETE');
}

// ALWAYS AT HOME

export function getStaples() {
    return request('/staples');
}

export function addStaple(staple) {
    return request('/staples', 'POST', staple);
}

export function deleteStaple(id) {
    return request(`/staples/${id}`, 'DELETE');
}

// ALLERGIES

export function getAllergens() {
    return request('/allergens');
}

export function saveAllergens(allergenIds) {
    return request('/allergens', 'PUT', allergenIds);
}