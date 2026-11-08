
const API_URL = 'http://localhost:8080/api/chat';

// Schickt Nachricht und Vorrat an das Java-Backend und liefert { reply, recipes } zurück
export async function sendChatMessage(message, pantry) {
    const response = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message, pantry })
    });

    if (!response.ok) {
        throw new Error(`Backend antwortet mit Status ${response.status}`);
    }

    return response.json();
}
