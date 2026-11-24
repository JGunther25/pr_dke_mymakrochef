const API_URL = 'http://localhost:8080/api/auth';

async function authRequest(endpoint, method = 'GET', body) {

    const response = await fetch(`${API_URL}/${endpoint}`, {
        method,
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json'
        },
        body: body === undefined
            ? undefined
            : JSON.stringify(body)
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.message || 'Authentication failed.');
    }

    return data;
}

export function login(email, password) {
    return authRequest('login', 'POST', { email, password });
}

export function register(email, password) {
    return authRequest('register', 'POST', { email, password });
}

export function checkSession() {
    return authRequest('session');
}

export function logout() {
    return authRequest('logout', 'POST');
}