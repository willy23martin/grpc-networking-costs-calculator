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
        const res = await fetch(url, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json'
          }
        });

        if (!res.ok) {
           console.error(`[tactics-mapping] ${url} returned status: ${res.status} ${res.statusText}`);

           // If it's a 401/403, the session really is dead or unauthenticated
           if (res.status === 401 || res.status === 403) {
               window.location.href = '/login';
           }
           return;
        }

        const data = await res.json();
        console.log(`[tactics-mapping] Loaded ${data.length} ${kind} entries`);
        if (kind === 'security')    securityMappingsCache    = data;
        if (kind === 'reliability') reliabilityMappingsCache = data;
        if (kind === 'resiliency')  resiliencyMappingsCache  = data;

    } catch (err) {
        console.error(`[tactics-mapping] ${kind} fetch failed:`, err.message);
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
 * Checks or unchecks every cloud-service checkbox whose
 * supportedArchitecturalDecisions array includes tacticId.
 *
 * When ENABLING a tactic  → auto-checks all dependent services.
 * When DISABLING a tactic → auto-unchecks dependent services ONLY if no
 *   other currently-active tactic also requires them (shared services such
 *   as sec-cloudwatch, needed by both oauth AND tls, stay checked when
 *   only one of the two tactics is removed).
 *
 * @param {string}  tacticId   e.g. 'tactic-tls', 'tactic-oauth', 'tactic-server-lb'
 * @param {boolean} [isEnabled] the tactic's new state; if omitted, reads the DOM
 */
// ─── Tactic → Cloud Service auto-select ──────────────────────────────────────
//
// Design: SIMPLE AND CORRECT.
//
// When a security/reliability tactic checkbox changes, we look at the cached
// backend mapping data to find which cloud-service checkboxes have that tactic
// in their supportedArchitecturalDecisions list.
//
// Rules:
//  • We ONLY call syncCloudServicesForTactic for the ARCHITECTURAL TACTIC IDs
//    (tactic-tls, tactic-mtls, tactic-oauth, tactic-server-lb).
//    We NEVER call it for cloud-service IDs (sec-inspector, sec-waf, …).
//    This prevents the cascade where clicking one service activates others.
//  • We set .checked SILENTLY (no dispatchEvent) to avoid triggering
//    recalculateSecCost with default input values.
//  • We call recalculateSecCost() ONCE after all boxes are set.
//  • loadCloudSecSection() is idempotent (guards against re-render).
//  • On disable: uncheck dependent services not needed by another active tactic.

// The ONLY tactic IDs that may trigger cloud-service auto-select.
// These are the architectural tactics from the domain model — NOT cloud service IDs.
var ARCHITECTURAL_TACTIC_TRIGGER_IDS = new Set([
    'tactic-tls', 'tactic-mtls', 'tactic-oauth', 'tactic-server-lb'
]);

/**
 * Sync cloud-service checkboxes for a given architectural tactic.
 * @param {string}  tacticId   Must be an architectural tactic id (tactic-tls etc.)
 * @param {boolean} isEnabled  New state of the tactic
 */
function syncCloudServicesForTactic(tacticId, isEnabled) {
    // Guard: only run for architectural tactic IDs, never for cloud-service IDs.
    // This prevents cascades like "clicking sec-inspector activates all others".
    if (!ARCHITECTURAL_TACTIC_TRIGGER_IDS.has(tacticId)) return;

    if (isEnabled === undefined) {
        var el = document.getElementById(tacticId);
        isEnabled = !!(el && el.checked);
    }

    var cloudServices = getAllCloudServiceMappings();
    if (!cloudServices.length) return;

    function applySync() {
        var anyChanged = false;
        // For sec-* IDs, prefer dynamic checkbox inside cloudsec-content over the static
        // hidden one in body-cloud which getElementById finds first and is always unchecked.
        var _sp = document.getElementById('cloudsec-content');
        function getCb(id) {
            if (_sp) { var el = _sp.querySelector('#' + id); if (el) return el; }
            return document.getElementById(id);
        }

        if (isEnabled) {
            cloudServices.forEach(function(svc) {
                if (!svc.supportedArchitecturalDecisions.includes(tacticId)) return;
                var cb = getCb(svc.tacticId);
                if (cb && !cb.checked) { cb.checked = true; anyChanged = true; }
            });
        } else {
            cloudServices.forEach(function(svc) {
                if (!svc.supportedArchitecturalDecisions.includes(tacticId)) return;
                var cb = getCb(svc.tacticId);
                if (!cb || !cb.checked) return;
                var stillNeeded = svc.supportedArchitecturalDecisions
                    .filter(function(d) { return d !== tacticId; })
                    .some(function(d) { var dep = getCb(d); return dep && dep.checked; });
                if (!stillNeeded) { cb.checked = false; anyChanged = true; }
            });
        }

        if (anyChanged) {
            // Recalculate costs ONCE after all boxes settled
            if (typeof recalculateSecCost    === 'function') recalculateSecCost();
            if (typeof updateCloudTacticsBadge === 'function') updateCloudTacticsBadge();
            if (typeof evaluateCloudServiceRelevance === 'function') evaluateCloudServiceRelevance();
        }
    }

    // If the cloud-sec panel is already in the DOM, sync immediately.
    // Otherwise load the panel first (it is idempotent), then sync.
    if (document.getElementById('sec-guardduty')) {
        applySync();
    } else if (isEnabled && typeof loadCloudSecSection === 'function') {
        // loadCloudSecSection is now idempotent and returns a Promise
        loadCloudSecSection().then(applySync).catch(function() {
            // Panel failed to load — nothing to sync
        });
    }
}

function wireTacticToCloudServiceAutoSelect() {
    // Single document-level listener.
    // ONLY fires syncCloudServicesForTactic for the whitelisted architectural tactic IDs.
    document.addEventListener('change', function(e) {
        var el = e.target;
        if (!el || el.type !== 'checkbox') return;
        if (ARCHITECTURAL_TACTIC_TRIGGER_IDS.has(el.id)) {
            syncCloudServicesForTactic(el.id, el.checked);
        }
    });
    console.log('[tactics-mapping] auto-select wired (tactic IDs only)');
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