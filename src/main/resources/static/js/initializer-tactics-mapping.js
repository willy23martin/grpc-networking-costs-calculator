document.addEventListener("DOMContentLoaded", () => {
        fetchSecurityMappings();
    });

let securityMappingsCache = [];

async function fetchSecurityMappings() {
    try {
        console.log("Requesting dynamic FinOps and security telemetry from backend...");
        const response = await fetch('/api/security/tactic-mappings');

        if (!response.ok) {
            throw new Error("API Connection Fault. Received Status Code: " + response.status);
        }

        securityMappingsCache = await response.json();
        console.log("Successfully cached " + securityMappingsCache.length + " entries from controller.");
        console.log("Security tactics \n" + securityMappingsCache);
        renderDynamicSecurityTelemetry();
    } catch (err) {
        console.warn("Backend unavailable. Initializing client-side fallback configuration mappings:", err);
    }
}

function renderDynamicSecurityTelemetry() {
    if (!securityMappingsCache || securityMappingsCache.length === 0) return;

    securityMappingsCache.forEach(mapping => {
        console.log("Mapping \n" +  mapping);
        const placementContainer = document.getElementById(`meta-container-${mapping.tacticId}`);
        if (!placementContainer) return;

        let owaspBadges = '';
        if (mapping.owaspTop10 && mapping.owaspTop10.length > 0) {
            mapping.owaspTop10.forEach((code, index) => {
                const label = (mapping.owaspLabels && mapping.owaspLabels[index]) ? ` (${mapping.owaspLabels[index]})` : '';
                owaspBadges += `<span style="display:inline-block; background:#7c3aed; color:#fff; font-size:11px; padding:2px 6px; border-radius:4px; margin-right:5px; margin-top:4px;"><i class="fas fa-shield-halved"></i> ${code}${label}</span>`;
            });
        }

        let cweBadges = '';
        if (mapping.cweIds && mapping.cweIds.length > 0) {
            mapping.cweIds.forEach(cwe => {
                cweBadges += `<span style="display:inline-block; background:#3D4451; color:#fff; font-size:11px; padding:2px 6px; border-radius:4px; margin-right:5px; margin-top:4px;"><i class="fas fa-bug"></i> ${cwe}</span>`;
            });
        }

        let isoBadges = '';
        if (mapping.iso25010Attributes && mapping.iso25010Attributes.length > 0) {
            mapping.iso25010Attributes.forEach(attr => {
                isoBadges += `<span style="display:inline-block; background:#2563EB; color:#fff; font-size:11px; padding:2px 6px; border-radius:4px; margin-right:5px; margin-top:4px;"><i class="fas fa-bookmark"></i> ISO: ${attr}</span>`;
            });
        }

        placementContainer.innerHTML = `
                <div class="dynamic-meta-inner" style="margin-top: 8px; padding: 10px; background: #F8F7F4; border-left: 3px solid #7c3aed; border-radius: 4px; font-size: 12px; color: #3D4451; line-height: 1.5; font-weight: normal; text-transform: none;">
                    <div style="margin-bottom: 4px; font-weight: 600; color: #1A3A5C;">Mitigation Controls:</div>
                    <div style="margin-bottom: 6px;">${owaspBadges} ${cweBadges} ${isoBadges}</div>
                    <div style="margin-top: 4px;"><strong>Vulnerability Protection:</strong> ${mapping.vulnerabilityPrevented || mapping.securityRationale || 'N/A'}</div>
                    <div style="margin-top: 3px; font-style: italic; color: #6B7280;"><strong>Cost Tradeoff:</strong> ${mapping.costImpactNote || 'Standard infrastructure tier processing rates apply'}</div>
                </div>
            `;
    });
}