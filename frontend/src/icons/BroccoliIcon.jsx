
// Brokkoli (Chat-Assistent).
// mode = 'static': still, neben den Nachrichten
// mode = 'thinking': wiegt sich, schaut nach oben, Denkblasen (während auf die Antwort gewartet wird)
// mode = 'wink': zwinkert einmal und lächelt, kurz bevor die Antwort erscheint
function BroccoliIcon({ size = 36, mode = 'static' }) {
    return (
        <svg
            className={`mmc-icon mmc-broccoli mmc-broccoli--${mode}`}
            width={size}
            height={size}
            viewBox="0 0 100 100"
            aria-hidden="true"
        >
            <g className="mmc-sway">
                <path d="M40 92 C42 76 42 66 37 54 H63 C58 66 58 76 60 92 Q50 96 40 92 Z" fill="#a6d98a"/>
                <path d="M52 54 H63 C58 66 58 76 60 92 Q56 94 52 94 Z" fill="#8cc472"/>
                <ellipse cx="50" cy="92.5" rx="10" ry="2.6" fill="#d8efc2"/>
                <path d="M44 76 Q45 66 43 60 M47 82 V62" fill="none" stroke="#c3e6a8" strokeWidth="1.5" strokeLinecap="round"/>
                <path d="M44 62 L34 52 M56 62 L66 52 M50 64 V48" stroke="#8cc472" strokeWidth="4" strokeLinecap="round"/>
                <circle cx="27" cy="46" r="15" fill="#3f9b3a"/>
                <circle cx="73" cy="46" r="15" fill="#2f7f2c"/>
                <circle cx="38" cy="32" r="14" fill="#3f9b3a"/>
                <circle cx="62" cy="32" r="14" fill="#358a31"/>
                <circle cx="50" cy="28" r="15" fill="#46a541"/>
                <circle cx="38" cy="56" r="12" fill="#358a31"/>
                <circle cx="62" cy="56" r="12" fill="#2a7428"/>
                <circle cx="50" cy="46" r="14" fill="#46a541"/>
                <g fill="#5bb84f">
                    <circle cx="24" cy="41" r="7"/>
                    <circle cx="35" cy="27" r="7"/>
                    <circle cx="47" cy="22" r="8"/>
                    <circle cx="34" cy="51" r="5"/>
                </g>
                <g fill="#7fcf6e">
                    <circle cx="45" cy="19" r="3.5"/>
                    <circle cx="33" cy="24" r="3"/>
                    <circle cx="21" cy="38" r="3"/>
                </g>
                <g fill="#2a7428">
                    <circle cx="31" cy="45" r="1.8"/>
                    <circle cx="75" cy="50" r="1.8"/>
                    <circle cx="62" cy="28" r="1.8"/>
                    <circle cx="68" cy="58" r="1.8"/>
                    <circle cx="40" cy="60" r="1.6"/>
                </g>

                <ellipse cx="44" cy="43" rx="2.6" ry="3.4" fill="#1f3a22"/>
                <circle className="mmc-pupil" cx="44.9" cy="41.8" r=".9" fill="#ffffff"/>
                <g className="mmc-wink">
                    <ellipse cx="56" cy="43" rx="2.6" ry="3.4" fill="#1f3a22"/>
                    <circle className="mmc-pupil" cx="56.9" cy="41.8" r=".9" fill="#ffffff"/>
                </g>

                <ellipse cx="40" cy="49" rx="3" ry="1.8" fill="#f4a39a" opacity="0.7"/>
                <ellipse cx="60" cy="49" rx="3" ry="1.8" fill="#f4a39a" opacity="0.7"/>

                {mode === 'thinking' ? (
                    // beim Nachdenken ein kleines, ruhiges Lächeln
                    <path d="M46 49.5 Q50 52.5 54 49.5" fill="none" stroke="#1f3a22" strokeWidth="2" strokeLinecap="round"/>
                ) : (
                    <path className="mmc-smile" d="M45 49 Q50 54 55 49" fill="none" stroke="#1f3a22" strokeWidth="2" strokeLinecap="round"/>
                )}
            </g>

            {mode === 'thinking' && (
                <g>
                    <circle className="mmc-think1" cx="74" cy="26" r="2.6" fill="#ffffff" stroke="#b9d8c3" strokeWidth="1.2"/>
                    <circle className="mmc-think2" cx="82" cy="16" r="3.8" fill="#ffffff" stroke="#b9d8c3" strokeWidth="1.2"/>
                    <g className="mmc-think3">
                        <ellipse cx="96" cy="2" rx="9" ry="6.5" fill="#ffffff" stroke="#b9d8c3" strokeWidth="1.2"/>
                        <circle cx="92" cy="2" r="1.2" fill="#3aa864"/>
                        <circle cx="96" cy="2" r="1.2" fill="#3aa864"/>
                        <circle cx="100" cy="2" r="1.2" fill="#3aa864"/>
                    </g>
                </g>
            )}
        </svg>
    );
}

export default BroccoliIcon;
