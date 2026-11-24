
import { useEffect, useRef, useState } from 'react';
import SaladBowl from './SaladBowl';

// Text des Logos, "makro" in Grün
const LETTERS = [
    ...'my '.split('').map(char => ({ char, green: false })),
    ...'makro'.split('').map(char => ({ char, green: true })),
    ...' chef'.split('').map(char => ({ char, green: false }))
];

// Zeiten wie in icons.css: Schüssel braucht 70 % von 3,4 s für den Weg, ein Buchstabe 1,6 s
const BOWL_TIME = 3.4;
const MOVE_TIME = BOWL_TIME * 0.7;
const LETTER_TIME = 1.6;

// Logo: Salatschüssel und Name. Beim Laden und beim Darüberfahren frisst die Schüssel den Namen.
function Logo() {
    const logoRef = useRef(null);
    const playingRef = useRef(false);
    const timerRef = useRef(null);

    const [playing, setPlaying] = useState(false);
    const [timing, setTiming] = useState({ travel: 0, delays: [] });

    // misst, wo die Buchstaben stehen, und berechnet daraus,
    // wann die Schüssel bei welchem Buchstaben ankommt
    function measure() {
        const logo = logoRef.current;
        const bowl = logo.querySelector('.logo-bowl');
        const letters = [...logo.querySelectorAll('.logo-letter')];
        const last = letters[letters.length - 1];

        const bowlCenter = bowl.offsetLeft + bowl.offsetWidth / 2;
        const travel = last.offsetLeft + last.offsetWidth - bowl.offsetLeft;

        const delays = letters.map(letter => {
            const center = letter.offsetLeft + letter.offsetWidth / 2;
            const reach = center - bowlCenter - bowl.offsetWidth * 0.3;
            return Math.max(0, (reach / travel) * MOVE_TIME);
        });

        return { travel, delays };
    }

    function play() {
        if (playingRef.current || !logoRef.current) return;

        const measured = measure();
        const lastDelay = measured.delays[measured.delays.length - 1];
        const duration = Math.max(BOWL_TIME, lastDelay + LETTER_TIME);

        playingRef.current = true;
        setTiming(measured);
        setPlaying(true);

        timerRef.current = setTimeout(() => {
            playingRef.current = false;
            setPlaying(false);
        }, duration * 1000);
    }

    // einmal beim Laden abspielen, sobald die Schrift geladen ist (sonst stimmen die Abstände nicht)
    useEffect(() => {
        const fontsReady = document.fonts ? document.fonts.ready : Promise.resolve();
        fontsReady.then(play);

        return () => clearTimeout(timerRef.current);
    }, []);

    return (
        <span
            ref={logoRef}
            className={`logo ${playing ? 'is-playing' : ''}`}
            style={{ '--travel': `${timing.travel}px` }}
            onMouseEnter={play}
            aria-label="my makro chef"
        >
            <span className="logo-bowl">
                <SaladBowl size={52} withMouth />
            </span>

            <span className="logo-text" aria-hidden="true">
                {LETTERS.map((letter, index) => (
                    <span
                        key={index}
                        className={`logo-letter ${letter.green ? 'green' : ''}`}
                        style={{ animationDelay: `${timing.delays[index] ?? 0}s` }}
                    >
                        {letter.char}
                    </span>
                ))}
            </span>
        </span>
    );
}

export default Logo;
