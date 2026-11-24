
// Grenzen für die Breite einer Spalte in Pixel
const MIN_WIDTH = 220;
const MAX_WIDTH = 600;

// Grenzen für die Höhe des unteren Fensters in Pixel
const MIN_BOTTOM_HEIGHT = 80;
const MIN_TOP_HEIGHT = 150;

// Folgt der Maus, bis losgelassen wird, und meldet jede Bewegung an onMove
function followPointer(cursorClass, onMove) {
    function stop() {
        window.removeEventListener('pointermove', onMove);
        window.removeEventListener('pointerup', stop);
        document.body.classList.remove(cursorClass);
    }

    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', stop);
    document.body.classList.add(cursorClass);
}

// Eine Spalte rechts neben dem Chat (Pantry oder Profile).
// Überschrift klicken = zu- bzw. aufklappen, Leiste am linken Rand ziehen = Breite ändern.
// Optional gibt es ein zweites Fenster darunter (bottom), gleich breit, Höhe per Leiste dazwischen.
function SidePanel({ icon, title, className = '', width, collapsed, onToggle, onResize, children, bottom }) {

    function startResize(event) {
        event.preventDefault();

        const startX = event.clientX;
        const startWidth = width;

        followPointer('resizing', moveEvent => {
            // die Leiste sitzt links, also macht Ziehen nach links die Spalte breiter
            const newWidth = startWidth + (startX - moveEvent.clientX);
            onResize(Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, newWidth)));
        });
    }

    function startHeightResize(event) {
        if (bottom.collapsed) return;

        event.preventDefault();

        const startY = event.clientY;
        const startHeight = bottom.height;
        const columnHeight = event.currentTarget.parentElement.clientHeight;
        const maxHeight = Math.max(MIN_BOTTOM_HEIGHT, columnHeight - MIN_TOP_HEIGHT - 20);

        followPointer('resizing-rows', moveEvent => {
            // nach oben ziehen macht das untere Fenster höher
            const newHeight = startHeight + (startY - moveEvent.clientY);
            bottom.onResize(Math.min(maxHeight, Math.max(MIN_BOTTOM_HEIGHT, newHeight)));
        });
    }

    if (collapsed) {
        return (
            <aside className="side-panel collapsed">
                <button
                    type="button"
                    className="panel-strip"
                    onClick={onToggle}
                    title={`Open ${title}`}
                    aria-label={`Open ${title}`}
                >
                    <span>{icon}</span>
                    <span className="strip-label">{title}</span>
                </button>
            </aside>
        );
    }

    return (
        <aside className="side-column" style={{ width }}>
            <div
                className="resize-handle"
                onPointerDown={startResize}
                title="Drag to resize"
            />

            <section className={`side-panel ${className}`}>
                <button
                    type="button"
                    className="panel-heading"
                    onClick={onToggle}
                    title={`Collapse ${title}`}
                >
                    <h2>{icon} {title}</h2>
                    <span className="panel-arrow">›</span>
                </button>

                <div className="panel-body">
                    {children}
                </div>
            </section>

            {bottom && (
                <>
                    <div
                        className={`height-handle ${bottom.collapsed ? 'disabled' : ''}`}
                        onPointerDown={startHeightResize}
                        title={bottom.collapsed ? undefined : 'Drag to change height'}
                    />

                    <section
                        className={`side-panel side-panel-bottom ${bottom.collapsed ? 'bottom-collapsed' : ''}`}
                        style={bottom.collapsed ? undefined : { height: bottom.height }}
                    >
                        <button
                            type="button"
                            className="panel-heading"
                            onClick={bottom.onToggle}
                            title={bottom.collapsed ? `Open ${bottom.title}` : `Collapse ${bottom.title}`}
                            aria-expanded={!bottom.collapsed}
                        >
                            <h2>{bottom.icon} {bottom.title}</h2>
                            <span className="panel-arrow">{bottom.collapsed ? '‹' : '›'}</span>
                        </button>

                        {!bottom.collapsed && (
                            <div className="panel-body">
                                {bottom.content}
                            </div>
                        )}
                    </section>
                </>
            )}
        </aside>
    );
}

export default SidePanel;
