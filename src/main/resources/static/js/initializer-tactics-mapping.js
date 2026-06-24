/* =================================================================
   initializer-tactics-mapping.js
   Loads security, reliability and resiliency tactic mappings from
   the backend and renders OWASP/CWE/ISO badges into the tactic rows.

   FIX 3a — Cloud-service auto-selection:
   When an architectural tactic is toggled (tls, mtls, oauth,
   server-lb) this module reads the already-cached mapping data from
   the three backend endpoints and checks every CloudService whose
   supportedArchitecturalDecisions list contains that tactic id.
   No hardcoded TACTIC_TO_CLOUD_SERVICE_MAP — the source of truth is
   always the backend.
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
        fetchMappings('/api/security/tactic-mappings',    'security'),
        fetchMappings('/api/reliability/tactic-mappings', 'reliability'),
        fetchMappings('/api/resiliency/tactic-mappings',  'resiliency')
    ]).then(() => {
        renderAllDynamicMetadata();
        wireCloudServiceVisibility();
        wireTacticToCloudServiceAutoSelect();   /* FIX 3a */
    }).catch(err => {
        console.warn('One or more tactic mapping endpoints unavailable:', err);
        renderAllDynamicMetadata();
        wireCloudServiceVisibility();
        wireTacticToCloudServiceAutoSelect();   /* FIX 3a — still wire with whatever loaded */
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

    const costFactor    = mapping.costFactor || 'Standard infrastructure tier processing rates apply';
    const vulnerability = mapping.vulnerabilityPrevented || mapping.securityRationale
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
   FIX 3a — TACTIC → CLOUD SERVICE AUTO-SELECT
   Driven entirely by the backend mapping data (supportedArchitecturalDecisions).
   No hardcoded map here — the source of truth is always the three
   /api/{security|reliability|resiliency}/tactic-mappings endpoints.

   How it works:
   1.  wireTacticToCloudServiceAutoSelect() delegates via document-level
       event delegation so it works for both statically-rendered tactic
       checkboxes AND dynamically-rendered cloud-service checkboxes.
   2.  When a tactic checkbox changes, syncCloudServicesForTactic(tacticId)
       scans the merged mapping cache for any entry whose
       supportedArchitecturalDecisions contains that tactic id.
   3.  If the tactic is being ENABLED, every matched cloud-service
       checkbox is checked (if not already checked) and its onchange
       is dispatched so recalculateSec/Alb/etc. runs normally.
   4.  If the tactic is DISABLED, cloud services are NOT auto-unchecked
       because the user may have independently selected them for other
       tactics.
   5.  If a cloud service checkbox does not exist yet (lazy panel not
       rendered), the function ensures the panel is loaded and then
       retries once via a short timeout.
================================================================= */

/**
 * All mapping entries that have at least one supportedArchitecturalDecision.
 * These are the CloudService rows in the backend — they carry the
 * supportedArchitecturalDecisions list set in CloudSecurityArchitecturalDecisionRepository
 * and CloudReliabilityArchitecturalDecisionRepository.
 */
function getAllCloudServiceMappings() {
    return [
        ...securityMappingsCache,
        ...reliabilityMappingsCache,
        ...resiliencyMappingsCache
    ].filter(m => m.supportedArchitecturalDecisions && m.supportedArchitecturalDecisions.length > 0);
}

/**
 * Checks every cloud-service checkbox whose supportedArchitecturalDecisions
 * array includes the given tacticId, if that tactic is currently enabled.
 * Safe to call multiple times (idempotent on already-checked boxes).
 *
 * @param {string} tacticId  e.g. 'tactic-tls', 'tactic-oauth', 'tactic-server-lb'
 */
function syncCloudServicesForTactic(tacticId) {
    const tacticEl  = document.getElementById(tacticId);
    const isEnabled = tacticEl && tacticEl.checked;

    /* Nothing to do when tactic is being disabled — intentionally do not
       auto-uncheck because services may be needed by other active tactics. */
    if (!isEnabled) return;

    const cloudServices = getAllCloudServiceMappings();
    let anyMissingFromDom = false;

    cloudServices.forEach(serviceMapping => {
        const deps = serviceMapping.supportedArchitecturalDecisions; // string[] of tactic IDs
        if (!deps.includes(tacticId)) return;

        const serviceEl = document.getElementById(serviceMapping.tacticId);
        if (!serviceEl) {
            /* The cloud-service panel has not been rendered yet (lazy). */
            anyMissingFromDom = true;
            return;
        }

        if (!serviceEl.checked) {
            serviceEl.checked = true;
            /* Fire onchange so recalculate* + sessionStorage + updateLiveComparison all run */
            serviceEl.dispatchEvent(new Event('change', { bubbles: true }));
            console.log(`[tactics-mapping] Auto-checked ${serviceMapping.tacticId} (required by ${tacticId})`);
        }
    });

    /* If some cloud-service checkboxes weren't in the DOM yet, ensure the
       relevant panels are loaded and then retry once. */
    if (anyMissingFromDom) {
        _ensureCloudPanelsLoaded().then(() => {
            /* Single retry after panels are rendered */
            setTimeout(() => syncCloudServicesForTactic(tacticId), 150);
        });
    }
}

/**
 * Triggers lazy loading of the cloud-service panels that may not have
 * been opened yet by the user.  Uses the loader functions already defined
 * in tactics-patterns.js (loadCloudSecSection, loadAlbSection, etc.).
 * Returns a Promise that resolves when loading has been initiated.
 */
function _ensureCloudPanelsLoaded() {
    const loaders = [];

    /* Security services panel */
    if (typeof loadCloudSecSection === 'function' &&
        !document.getElementById('sec-guardduty')) {
        loaders.push(loadCloudSecSection());
    }

    /* ALB panel */
    if (typeof loadAlbSection === 'function' &&
        !document.getElementById('tactic-alb')) {
        loaders.push(loadAlbSection());
    }

    return Promise.all(loaders).catch(err =>
        console.warn('[tactics-mapping] Panel lazy-load failed:', err)
    );
}

/**
 * Wires event delegation on the document so that when any tactic checkbox
 * (tactic-tls, tactic-mtls, tactic-oauth, tactic-server-lb, etc.) changes,
 * the cloud-service auto-select runs.
 * Uses delegation so it works for checkboxes rendered after this script loads.
 */
function wireTacticToCloudServiceAutoSelect() {
    /* The set of tactic IDs that should trigger cloud-service auto-selection.
       We derive this from the loaded mapping data — any ID that appears in
       at least one supportedArchitecturalDecisions list. */
    function getActiveTacticTriggers() {
        const triggers = new Set();
        getAllCloudServiceMappings().forEach(m => {
            m.supportedArchitecturalDecisions.forEach(depId => triggers.add(depId));
        });
        return triggers;
    }

    document.addEventListener('change', function (e) {
        const el = e.target;
        if (!el || el.type !== 'checkbox') return;

        const triggers = getActiveTacticTriggers();
        if (triggers.has(el.id)) {
            syncCloudServicesForTactic(el.id);
        }
    });

    console.log('[tactics-mapping] Tactic → cloud-service auto-select wired');
}

/* =================================================================
   CLOUD-SERVICE VISIBILITY (dim / highlight based on active tactics)
   Unchanged from original — evaluates opacity of cloud service rows
   based on whether their parent tactic is currently active.
================================================================= */
function wireCloudServiceVisibility() {
    document.addEventListener('change', function (e) {
        if (e.target && (e.target.classList.contains('tactic-check') ||
                         e.target.classList.contains('cloud-service-check'))) {
            evaluateCloudServiceRelevance();
        }
    });
    evaluateCloudServiceRelevance();
}

function evaluateCloudServiceRelevance() {
    const allMappings = [
        ...securityMappingsCache,
        ...reliabilityMappingsCache,
        ...resiliencyMappingsCache
    ];

    allMappings.forEach(mapping => {
        const deps = mapping.supportedArchitecturalDecisions;
        if (!deps || deps.length === 0) return;

        /* Find the row: first try explicit row id, then walk up from checkbox */
        let row = document.getElementById(`row-${mapping.tacticId}`);
        if (!row) {
            const cb = document.getElementById(mapping.tacticId);
            if (cb) row = cb.closest('.tactic-row');
        }
        if (!row) return;

        /* Only consider tactic (non-cloud-service) dependencies */
        const anyActive = deps.some(depId => {
            const el = document.getElementById(depId);
            return el && el.checked;
        });

        if (anyActive) {
            row.style.opacity    = '1';
            row.style.borderLeft = '3px solid var(--green, #16a34a)';
            row.style.paddingLeft = '8px';
            row.title = '';
        } else {
            row.style.opacity     = '0.55';
            row.style.borderLeft  = '';
            row.style.paddingLeft = '';
            const names = deps.map(id => {
                const label = document.querySelector(`label[for="${id}"]`);
                return label
                    ? label.textContent.trim().replace(/\s+/g, ' ').substring(0, 30)
                    : id;
            }).join(', ');
            row.title = `Recommended when: ${names}`;
        }
    });
}

/* =================================================================
   PUBLIC re-render hook — called from calculator.js / tactics-patterns.js
   after any tactic change to keep badges + cloud visibility in sync.
================================================================= */
window.refreshTacticMappingDisplay = function () {
    renderAllDynamicMetadata();
    evaluateCloudServiceRelevance();
};

/* Export evaluateCloudServiceRelevance so tactics-patterns.js can call it
   after a panel is lazy-loaded */
window.evaluateCloudServiceRelevance = evaluateCloudServiceRelevance;

/* Export syncCloudServicesForTactic so tactics-patterns.js helper
   functions (toggleTlsOptions, toggleOAuthParams, etc.) can call it
   directly when they already know which tactic changed — avoids relying
   solely on the change-event delegation path for those cases. */
window.syncCloudServicesForTactic = syncCloudServicesForTactic;