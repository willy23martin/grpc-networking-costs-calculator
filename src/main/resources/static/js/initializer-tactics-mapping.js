/* =================================================================
   initializer-tactics-mapping.js
   Loads security, reliability and resiliency tactic mappings from
   the backend and renders OWASP/CWE/ISO badges into the tactic rows.
   Also wires cloud-service checkboxes so they auto-enable/disable
   based on which architectural tactics are active (supportedArchitecturalDecisions).
================================================================= */

/* ── caches ─────────────────────────────────────────────────────── */
let securityMappingsCache    = [];
let reliabilityMappingsCache = [];
let resiliencyMappingsCache  = [];

/* =================================================================
   BOOT — fetch all three mapping sets in parallel on DOM ready
================================================================= */
document.addEventListener('DOMContentLoaded', () => {
    Promise.all([
        fetchMappings('/api/security/tactic-mappings',   'security'),
        fetchMappings('/api/reliability/tactic-mappings','reliability'),
        fetchMappings('/api/resiliency/tactic-mappings', 'resiliency')
    ]).then(() => {
        renderAllDynamicMetadata();
        wireCloudServiceVisibility();
    }).catch(err => {
        console.warn('One or more tactic mapping endpoints unavailable:', err);
        renderAllDynamicMetadata();   // render whatever loaded
        wireCloudServiceVisibility();
    });
});

/* =================================================================
   GENERIC FETCH
================================================================= */
async function fetchMappings(url, kind) {
    try {
        const res = await fetch(url);
        if (!res.ok) throw new Error(`${url} returned ${res.status}`);
        const data = await res.json();
        console.log(`[tactics-mapping] Loaded ${data.length} ${kind} entries`);
        if (kind === 'security')    securityMappingsCache    = data;
        if (kind === 'reliability') reliabilityMappingsCache = data;
        if (kind === 'resiliency')  resiliencyMappingsCache  = data;
    } catch (err) {
        console.warn(`[tactics-mapping] ${kind} fetch failed:`, err.message);
    }
}

/* =================================================================
   RENDER ALL BADGES INTO meta-container-* divs
================================================================= */
function renderAllDynamicMetadata() {
    const all = [
        ...securityMappingsCache,
        ...reliabilityMappingsCache,
        ...resiliencyMappingsCache
    ];
    all.forEach(mapping => renderMappingBadges(mapping));
}

function renderMappingBadges(mapping) {
    const container = document.getElementById(`meta-container-${mapping.tacticId}`);
    if (!container) return;

    /* ── OWASP badges ─────────────────────────────────────────── */
    let owaspBadges = '';
    if (mapping.owaspTop10 && mapping.owaspTop10.length > 0) {
        mapping.owaspTop10.forEach((code, idx) => {
            const label = (mapping.owaspLabels && mapping.owaspLabels[idx])
                ? ` (${mapping.owaspLabels[idx]})` : '';
            owaspBadges += `<span style="display:inline-block;background:#7c3aed;color:#fff;
                font-size:11px;padding:2px 6px;border-radius:4px;margin-right:5px;margin-top:4px;">
                <i class="fas fa-shield-halved"></i> ${code}${label}</span>`;
        });
    }

    /* ── CWE badges ───────────────────────────────────────────── */
    let cweBadges = '';
    if (mapping.cweIds && mapping.cweIds.length > 0) {
        mapping.cweIds.forEach(cwe => {
            cweBadges += `<span style="display:inline-block;background:#3D4451;color:#fff;
                font-size:11px;padding:2px 6px;border-radius:4px;margin-right:5px;margin-top:4px;">
                <i class="fas fa-bug"></i> ${cwe}</span>`;
        });
    }

    /* ── ISO 25010 badges ─────────────────────────────────────── */
    let isoBadges = '';
    if (mapping.iso25010Attributes && mapping.iso25010Attributes.length > 0) {
        mapping.iso25010Attributes.forEach(attr => {
            isoBadges += `<span style="display:inline-block;background:#2563EB;color:#fff;
                font-size:11px;padding:2px 6px;border-radius:4px;margin-right:5px;margin-top:4px;">
                <i class="fas fa-bookmark"></i> ISO: ${attr}</span>`;
        });
    }

    /* ── tradeoff / reliability badges ───────────────────────── */
    let tradeoffBadges = '';
    if (mapping.tradeoffs && mapping.tradeoffs.length > 0) {
        mapping.tradeoffs.forEach(t => {
            tradeoffBadges += `<span style="display:inline-block;background:#0891b2;color:#fff;
                font-size:11px;padding:2px 6px;border-radius:4px;margin-right:5px;margin-top:4px;">
                <i class="fas fa-scale-balanced"></i> ${t}</span>`;
        });
    }

    const costFactor     = mapping.costFactor || 'Standard infrastructure tier processing rates apply';
    const vulnerability  = mapping.vulnerabilityPrevented || mapping.securityRationale
                        || mapping.reliabilityRationale   || mapping.resiliencyRationale || 'N/A';

    container.innerHTML = `
        <div class="dynamic-meta-inner" style="margin-top:8px;padding:10px;
             background:#F8F7F4;border-left:3px solid #7c3aed;border-radius:4px;
             font-size:12px;color:#3D4451;line-height:1.5;font-weight:normal;text-transform:none;">
            <div style="margin-bottom:4px;font-weight:600;color:#1A3A5C;">Mitigation Controls:</div>
            <div style="margin-bottom:6px;">${owaspBadges} ${cweBadges} ${isoBadges} ${tradeoffBadges}</div>
            <div style="margin-top:4px;">
                <strong>Vulnerability Protection:</strong> ${vulnerability}
            </div>
            <div style="margin-top:3px;font-style:italic;color:#6B7280;">
                <strong>Cost Factor:</strong> ${costFactor}
            </div>
        </div>`;
}

/* =================================================================
   CLOUD-SERVICE VISIBILITY
   Each cloud service checkbox (sec-guardduty, sec-inspector …) has
   supportedArchitecturalDecisions in the security mapping JSON.
   When those referenced tactics are NOT checked, dim the cloud row
   and show a tooltip. When at least one is checked, highlight it.
================================================================= */
function wireCloudServiceVisibility() {
    /* re-evaluate whenever a tactic checkbox changes */
    document.querySelectorAll('.tactic-check').forEach(cb => {
        cb.addEventListener('change', evaluateCloudServiceRelevance);
    });
    evaluateCloudServiceRelevance();
}

function evaluateCloudServiceRelevance() {
    const allMappings = [...securityMappingsCache, ...reliabilityMappingsCache, ...resiliencyMappingsCache];

    allMappings.forEach(mapping => {
        const deps = mapping.supportedArchitecturalDecisions;
        if (!deps || deps.length === 0) return;

        const row = document.getElementById(mapping.tacticId)?.closest('.tactic-row');
        if (!row) return;

        const anyActive = deps.some(depId => {
            const el = document.getElementById(depId);
            return el && el.checked;
        });

        if (anyActive) {
            row.style.opacity = '1';
            row.style.borderLeft = '3px solid var(--green, #16a34a)';
            row.style.paddingLeft = '8px';
            row.title = '';
        } else {
            row.style.opacity = '0.55';
            row.style.borderLeft = '';
            row.style.paddingLeft = '';
            const names = deps.map(id => {
                const label = document.querySelector(`label[for="${id}"]`);
                return label ? label.textContent.trim().replace(/\s+/g, ' ').substring(0, 30) : id;
            }).join(', ');
            row.title = `Recommended when: ${names}`;
        }
    });
}

window.refreshTacticMappingDisplay = function() {
    renderAllDynamicMetadata();
    evaluateCloudServiceRelevance();
};