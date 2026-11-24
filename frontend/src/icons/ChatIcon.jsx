
// Sprechblase (Chat): die Punkte tippen, solange der Nutzer schreibt
function ChatIcon({ size = 24 }) {
    return (
        <svg
            className="mmc-icon mmc-chat"
            width={size}
            height={size}
            viewBox="10 6 84 80"
            aria-hidden="true"
        >
            <path d="M14 22 Q14 10 26 10 H74 Q86 10 86 22 V56 Q86 68 74 68 H42 L25 82 Q23 83 23.5 81 L28 68 H26 Q14 68 14 56 Z" fill="#e3f2e8" stroke="#a9cfb6" strokeWidth="1.5" strokeLinejoin="round"/>
            <path d="M86 30 V56 Q86 68 74 68 H42 L38 71 H74 Q90 70 90 54 V30 Z" fill="#c9e3d2" opacity="0.8"/>
            <path d="M20 26 Q20 16 30 16 H52" fill="none" stroke="#ffffff" strokeWidth="3" strokeLinecap="round"/>
            <circle className="mmc-dot" cx="35" cy="39" r="5.2" fill="#3aa864"/>
            <circle className="mmc-dot mmc-dt2" cx="50" cy="39" r="5.2" fill="#3aa864"/>
            <circle className="mmc-dot mmc-dt3" cx="65" cy="39" r="5.2" fill="#3aa864"/>
        </svg>
    );
}

export default ChatIcon;
