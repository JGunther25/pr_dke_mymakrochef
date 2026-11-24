
// Haus (Always at home): beim Darüberfahren geht jemand mit Einkaufstaschen hinein
function HomeIcon({ size = 24 }) {
    return (
        <svg
            className="mmc-icon mmc-home"
            width={size}
            height={size}
            viewBox="0 0 100 100"
            aria-hidden="true"
        >
            <circle className="mmc-smoke" cx="69" cy="11" r="4" fill="#c9cfd2"/>
            <circle className="mmc-smoke mmc-smoke2" cx="72" cy="11" r="5" fill="#dde2e5"/>
            <rect x="64" y="18" width="10" height="20" fill="#b8442c"/>
            <rect x="71" y="18" width="3" height="20" fill="#a33b26"/>
            <path d="M64 24 H74 M64 30 H74 M69 18 V24 M67 24 V30 M71 30 V36" stroke="#9a3a24" strokeWidth="1.2"/>
            <rect x="62" y="15" width="14" height="5" rx="1" fill="#8a3220"/>
            <ellipse cx="50" cy="93" rx="46" ry="5" fill="#7cc68a"/>
            <rect x="20" y="46" width="60" height="47" fill="#f6e8cc"/>
            <rect x="73" y="46" width="7" height="47" fill="#ead6ae"/>
            <rect x="20" y="54" width="60" height="4" fill="#ead6ae"/>
            <path d="M20 64 H80 M20 76 H80 M20 88 H80" stroke="#efdcb6" strokeWidth="1.1"/>
            <path d="M10 50 L50 16 L90 50 Q91 54 87 54 H13 Q9 54 10 50 Z" fill="#e0533d"/>
            <path d="M50 16 L90 50 Q91 54 87 54 H83 L50 22 Z" fill="#c94834"/>
            <path d="M33.5 30 H66.5 M21.8 40 H78.2 M13 50 H87" stroke="#c94834" strokeWidth="1.6"/>
            <path d="M40 30 V40 M54 30 V40 M30 40 V50 M46 40 V50 M62 40 V50 M74 40 V50" stroke="#d44c37" strokeWidth="1.2"/>
            <path d="M22 44 L50 20 L56 25" fill="none" stroke="#ef7a63" strokeWidth="2.5" strokeLinecap="round"/>
            <circle cx="50" cy="38" r="5" fill="#8fd0f0"/><path d="M50 33 V43 M45 38 H55" stroke="#ffffff" strokeWidth="1.3"/>
            <path d="M42 93 V73 Q50 62 58 73 V93 Z" fill="#3b2a1e"/>
            <path d="M44 93 V75 Q50 68 56 75 V93 Z" fill="#f2c14e" opacity="0.35"/>
            <g className="mmc-door"><path d="M42 93 V73 Q50 62 58 73 V93 Z" fill="#8a5a33"/><rect x="44.5" y="80" width="11" height="10" rx="1.5" fill="#7a4e2b"/><rect x="46.5" y="71" width="7" height="6" rx="1.5" fill="#8fd0f0"/><path d="M43 92 V73.5 Q45 69 48 67.5" fill="none" stroke="#a8764a" strokeWidth="1.2" strokeLinecap="round"/><circle cx="55" cy="83" r="1.6" fill="#f2c14e"/></g>
            <rect x="40" y="92" width="20" height="3" rx="1" fill="#b9b2a5"/>
            <rect x="24" y="61" width="13" height="13" rx="1.5" fill="#8fd0f0"/><path d="M30.5 61 V74 M24 67.5 H37" stroke="#ffffff" strokeWidth="1.6"/>
            <rect x="23" y="73.5" width="15" height="3" rx="1" fill="#ffffff" stroke="#e6d1a8" strokeWidth=".6"/>
            <g fill="#e5533d"><circle cx="26" cy="77.5" r="1.6"/><circle cx="30.5" cy="77.5" r="1.6"/><circle cx="35" cy="77.5" r="1.6"/></g>
            <rect x="63" y="61" width="13" height="13" rx="1.5" fill="#8fd0f0"/><path d="M69.5 61 V74 M63 67.5 H76" stroke="#ffffff" strokeWidth="1.6"/>
            <rect x="62" y="73.5" width="15" height="3" rx="1" fill="#ffffff" stroke="#e6d1a8" strokeWidth=".6"/>
            <g fill="#f2c14e"><circle cx="65" cy="77.5" r="1.6"/><circle cx="69.5" cy="77.5" r="1.6"/><circle cx="74" cy="77.5" r="1.6"/></g>
            <circle cx="86" cy="89" r="6" fill="#358a31"/><circle cx="80" cy="88" r="4.5" fill="#4caa43"/>
            <g transform="translate(6 95)"><g className="mmc-walker"><g className="mmc-bob">
            <rect className="mmc-leg1" x="-3.6" y="-6" width="2.8" height="6.5" rx="1.4" fill="#2c3e50"/>
            <rect className="mmc-leg2" x=".8" y="-6" width="2.8" height="6.5" rx="1.4" fill="#34495e"/>
            <rect x="-5" y="-17" width="10" height="12" rx="4" fill="#2e86c1"/>
            <rect x="1" y="-17" width="4" height="12" rx="2" fill="#2874a6"/>
            <circle cx="0" cy="-21.5" r="4.6" fill="#f7c58f"/>
            <path d="M-4.6 -22 Q-4 -27 0 -26.5 Q4.5 -27 4.6 -22 Q2 -24.5 -1 -24 Q-3 -24 -4.6 -22 Z" fill="#6b4a2e"/>
            <circle cx="1.6" cy="-21.5" r=".8" fill="#3b2a1e"/>
            <path d="M-15 -14 H-6 L-7 -3 H-14 Z" fill="#d39a5b"/><path d="M-9 -14 H-6 L-7 -3 H-9.5 Z" fill="#b97b40"/>
            <path d="M-13 -14 Q-14 -20 -11 -21 Q-11 -17 -11 -14 Z" fill="#3f9b3a"/><path d="M-10 -14 Q-8 -20 -5.5 -19 Q-7 -16 -8 -14 Z" fill="#5bb84f"/><circle cx="-9" cy="-15" r="2.2" fill="#e5533d"/>
            <path d="M6 -14 H15 L14 -3 H7 Z" fill="#c98a4b"/><path d="M12 -14 H15 L14 -3 H11.5 Z" fill="#a86d35"/>
            <path d="M9 -14 L11 -24 Q12 -25.5 13.2 -24.4 L12 -14 Z" fill="#e3a75a"/><path d="M7.5 -14 Q7 -19 9 -19.5 Q9.5 -16 9.5 -14 Z" fill="#f7d04a"/>
            <circle cx="-5.6" cy="-12" r="1.6" fill="#f7c58f"/><circle cx="5.6" cy="-12" r="1.6" fill="#eab07a"/>
            </g></g></g>
            <circle cx="14" cy="89" r="6" fill="#3f9b3a"/><circle cx="20" cy="87" r="5" fill="#5bb84f"/>
        </svg>
    );
}

export default HomeIcon;
