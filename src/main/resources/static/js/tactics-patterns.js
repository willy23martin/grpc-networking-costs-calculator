// Global Domain Architecture State Storage Maps
const awsSectionLoadingHistoryState = window._awsLoadedSectionsState ?? {};
let apiGatewayPricingDatabase = window._apiGatewayPricingData ?? null;
let applicationLoadBalancerPricingData = null;
let databaseBackupPricingData = null;
let cloudSecurityPricingData = null;
let cloudCostOptimizationPricingData = null;
let cacheInfrastructurePricingData = null;

// Pricing API Fallbacks Configuration
const API_GATEWAY_FALLBACK_PRICING = {
  restApiPer1MCallsMonthly: 3.50,
  httpApiPer1MCallsFirst1B: 1.00,
  httpApiPer1MCallsOver1B: 0.90,
  wsApiPer1MConnections: 0.25,
  wsApiPer1MMessages: 1.00,
  cacheHalfGbPerHour: 0.020,
  cache1GbPerHour: 0.038,
  cache1_6GbPerHour: 0.054,
  cache6_1GbPerHour: 0.200
};

// Section Loader Mapping Execution Engine
const AWS_SECTION_LOADERS = {
  alb: loadAlbSection,
  dbbackup: loadDbBackupSection,
  cloudsec: loadCloudSecSection,
  costopt: loadCostOptSection,
  caching: loadCachingSection
};

/**
 * Collapses or expands a tactical configuration card container and lazy-loads required remote pricing.
 * @param {string} categoryId
 */
function toggleCategory(categoryId) {
  const contentBody = document.getElementById(`body-${categoryId}`);
  if (!contentBody) return;

  const headerElement = contentBody.previousElementSibling;
  const isCurrentlyOpen = contentBody.classList.contains('open');
  const targetOpenState = !isCurrentlyOpen;

  contentBody.classList.toggle('open', targetOpenState);
  headerElement?.classList.toggle('active', targetOpenState);

  // When opening cloudsec, ensure the parent cloud-tactics body is also open.
  // If the user clicks the cloudsec header directly without having opened
  // cloud-tactics first, body-cloud-tactics has max-height:0 (CSS collapsed)
  // and body-cloudsec is rendered inside an overflow:hidden parent — invisible
  // regardless of its own .open class or contentContainer display:block.
  if (targetOpenState && categoryId === 'cloudsec') {
    const cloudTacticsBody = document.getElementById('body-cloud-tactics');
    if (cloudTacticsBody && !cloudTacticsBody.classList.contains('open')) {
      cloudTacticsBody.classList.add('open');
      const cloudTacticsHeader = cloudTacticsBody.previousElementSibling;
      if (cloudTacticsHeader) cloudTacticsHeader.classList.add('active');
    }
    loadCloudSecSection();
  }

  // If opening for the first time, execute lazy resource allocation
  if (targetOpenState && !awsSectionLoadingHistoryState[categoryId]) {
    awsSectionLoadingHistoryState[categoryId] = true;

    // Handle Individual Module Loading Configurations
    if (AWS_SECTION_LOADERS[categoryId]) {
      AWS_SECTION_LOADERS[categoryId]();
    } else if (categoryId === 'microservices') {
      const isApiGatewayChecked = document.getElementById('tactic-apigw')?.checked;
      if (isApiGatewayChecked && !apiGatewayPricingDatabase) {
        loadApiGwSection();
      }
    } else if (categoryId === 'cloud-tactics') {
      // Open cloudsec body so its CSS max-height constraint is lifted before
      // loadCloudSecSection writes content into it. Without this, content is
      // rendered inside a max-height:0/overflow:hidden parent and never visible.
      const cloudsecBody   = document.getElementById('body-cloudsec');
      const cloudsecHeader = cloudsecBody ? cloudsecBody.previousElementSibling : null;
      if (cloudsecBody) {
        cloudsecBody.classList.add('open');
        if (cloudsecHeader) cloudsecHeader.classList.add('active');
        const chevron = document.getElementById('chevron-cloudsec');
        if (chevron) chevron.style.transform = 'rotate(180deg)';
      }
      // Direct Group Evaluation Loop
      Object.keys(AWS_SECTION_LOADERS).forEach(subId => {
        if (!awsSectionLoadingHistoryState[subId]) {
          awsSectionLoadingHistoryState[subId] = true;
          AWS_SECTION_LOADERS[subId]();
        }
      });
    }
  }

  updateCloudTacticsBadge();
}

/**
 * Calculates selected checkbox elements and updates the visual counter badge UI indicator.
 */
function updateCloudTacticsBadge() {
  const badgeElement = document.getElementById('cloud-tactics-badge');
  if (!badgeElement) return;

  const checkboxSelectorIds = [
    'tactic-alb', 'tactic-cache', 'sec-guardduty', 'sec-inspector', 'sec-waf', 'sec-macie',
    'sec-cloudwatch', 'sec-audit', 'sec-kms', 'opt-ri-standard', 'opt-ri-3yr', 'opt-ri-convertible',
    'opt-savings-plan-1yr', 'opt-savings-plan-3yr', 'opt-trusted-advisor',
    'tactic-s3-backup', 'tactic-rds-snapshot', 'tactic-rds-multiaz', 'tactic-aurora-replica', 'tactic-dynamo-global',
    'cef-pods', 'cef-clusters', 'cef-namespaces', 'cef-k8sservices', 'cef-spot', 'cef-ri', 'cef-savings-plan',
    'cef-commitment-discount', 'cef-subscription', 'cef-req-cpu', 'cef-req-ram', 'cef-req-io', 'cef-req-vol',
    'cef-control-plane', 'cef-scheduler', 'cef-api-server', 'cef-controller-mgr', 'cef-cluster-waf',
    'cef-cluster-lb', 'cef-host-storage', 'cef-cluster-backup', 'cef-host-license', 'cef-workload-license',
    'cef-categorization', 'cef-arch-type', 'cef-underlying-resources', 'cef-orchestration',
    'cef-deployments', 'cef-replicasets', 'cef-statefulsets', 'cef-daemonsets', 'cef-cronjobs',
    'cef-sidecar', 'cef-init-containers', 'cef-adapter', 'cef-ambassador-retry', 'cef-ambassador-tls'
  ];

  const totalActiveTacticsCount = checkboxSelectorIds.filter(id => {
    const targetCheckbox = document.getElementById(id);
    return targetCheckbox && targetCheckbox.checked;
  }).length;

  if (totalActiveTacticsCount > 0) {
    badgeElement.textContent = `${totalActiveTacticsCount} active`;
    badgeElement.style.display = 'inline';
  } else {
    badgeElement.style.display = 'none';
  }
}

/**
 * Helper to extract structural numeric values or inputs safely without throwing exceptions.
 */
function getFormNumericValue(elementId, fallback = 0) {
  const targetElement = document.getElementById(elementId);
  return parseFloat(targetElement?.value) || fallback;
}

/**
 * Helper to easily switch visibility states for element wrappers.
 */
function toggleUiVisibility(elementId, shouldShow, styleDisplayType = 'block') {
  const targetElement = document.getElementById(elementId);
  if (targetElement) {
    targetElement.style.display = shouldShow ? styleDisplayType : 'none';
  }
}

function toggleCbParams(show) {
  toggleUiVisibility('cb-params', show);
}

function toggleRetry(checked) {
  const targetRetryPctInput = document.getElementById('input-retry-error-pct');
  if (targetRetryPctInput) {
    targetRetryPctInput.disabled = !checked;
  }
  toggleUiVisibility('retry-disclaimer', checked);
  recalculateRps();
  scheduleSessionSave();
}

function toggleTlsOptions() {
  const isTlsActive  = Boolean(document.getElementById('tactic-tls')?.checked);
  const isMtlsActive = Boolean(document.getElementById('tactic-mtls')?.checked);

  toggleUiVisibility('tls-params',      isTlsActive);
  toggleUiVisibility('mtls-params',     isMtlsActive);
  toggleUiVisibility('tls-byte-badge',  isTlsActive,  'inline-flex');
  toggleUiVisibility('mtls-byte-badge', isMtlsActive, 'inline-flex');

  // FIX 3a — auto-select (or deselect) cloud services driven by backend mapping
  // supportedArchitecturalDecisions from CloudSecurityArchitecturalDecisionRepository:
  //   tactic-tls  → sec-inspector, sec-waf, sec-cloudwatch, sec-audit, sec-kms, sec-acm
  //   tactic-mtls → sec-inspector, sec-waf, sec-cloudwatch, sec-audit, sec-kms, sec-acm
  if (typeof window.syncCloudServicesForTactic === 'function') {
    window.syncCloudServicesForTactic('tactic-tls',  isTlsActive);
    window.syncCloudServicesForTactic('tactic-mtls', isMtlsActive);
  }

  recalculateRps();
  scheduleSessionSave();
}

function toggleOAuthParams(show) {
  toggleUiVisibility('oauth-params',   show);
  toggleUiVisibility('jwt-byte-badge', show, 'inline-flex');

  // FIX 3a — auto-select (or deselect) cloud services driven by backend mapping
  // supportedArchitecturalDecisions from CloudSecurityArchitecturalDecisionRepository:
  //   tactic-oauth → sec-guardduty, sec-cloudtrail, sec-macie, sec-cloudwatch, sec-audit
  if (typeof window.syncCloudServicesForTactic === 'function') {
    window.syncCloudServicesForTactic('tactic-oauth', show);
  }

  recalculateRps();
  scheduleSessionSave();
}

/* =====================================================================
    AWS Remote Pricing Async Module Loaders
===================================================================== */

function loadAlbSection() {
  return fetchAwsPrice('alb-pricing')
    .then(data => {
      applicationLoadBalancerPricingData = data;

      console.warn("SUCCESS - /alb-pricing:");
      console.warn(data);

      window._albData = data;   /* expose for unit-economics.js */
      const descriptionContainer = document.getElementById('alb-price-desc');
      if (descriptionContainer) {
        descriptionContainer.innerHTML = `Fixed: <strong>$${data.fixedPerHourUsd.toFixed(4)}/hr</strong> ($${data.fixedPerMonthUsd}/mo per ALB) + <strong>$${data.lcuPerHourUsd.toFixed(4)}/hr per LCU</strong>. Source: ${data.source}`;
      }
      toggleUiVisibility('alb-loading', false);
      toggleUiVisibility('alb-content', true);
    })
    .catch(error => {
      console.error("ERROR - /alb-pricing: \n" + error);
      const loadingLabel = document.getElementById('alb-loading');
      if (loadingLabel) loadingLabel.textContent = `Could not fetch ALB pricing: ${error.message}`;
    });
}

function loadDbBackupSection() {
  fetchAwsPrice('database-backup-pricing')
    .then(data => {
      databaseBackupPricingData = data;

      console.warn("SUCCESS - /database-backup-pricing:");
      console.warn(data);

      window._dbData = data;   /* expose for unit-economics.js */
      toggleUiVisibility('dbbackup-loading', false);
      toggleUiVisibility('dbbackup-content', true);
    })
    .catch(error => {
      console.error("ERROR - /database-backup-pricing: \n" + error);
      const loadingLabel = document.getElementById('dbbackup-loading');
      if (loadingLabel) loadingLabel.textContent = `Could not fetch DB pricing: ${error.message}`;
    });
}

function loadCloudSecSection() {
  // Hardcoded 2025 AWS published prices — used when the backend is unavailable.
  const SEC_FALLBACK = {
    guardDutyPerGbLogs:              1.00,
    inspectorPerInstanceMonth:       1.178,
    wafWebAclPerMonth:               5.00,
    wafRulePerMonth:                 1.00,
    wafPer1MRequests:                0.60,
    maciePerGbClassified:            1.00,
    cloudwatchLogsIngestionPerGb:    0.50,
    cloudwatchLogsStoragePerGbMonth: 0.03,
    auditManagerPerAssessmentMonth:  6.00,
    kmsCmkPerMonth:                  1.00,
    kmsApiCallsPer10k:               0.03
  };

  function renderPanel(data) {
    // Always re-query the DOM inside renderPanel — do NOT close over a value
    // captured at loadCloudSecSection() call time, which may have been null if
    // the category body was not yet in the DOM (e.g. called from cloud-tactics
    // group loader before Phase 3 was rendered).
    const contentContainer = document.getElementById('cloudsec-content');
    const loadingEl        = document.getElementById('cloudsec-loading');

    // If content is already rendered INSIDE cloudsec-content, just ensure visibility.
    // IMPORTANT: check inside contentContainer only — there is a static sec-guardduty
    // checkbox in body-cloud (the old HTML structure) that document.getElementById
    // would find first, causing a false positive and skipping the render entirely.
    if (contentContainer && contentContainer.querySelector('#sec-guardduty')) {
      if (loadingEl)        loadingEl.style.display        = 'none';
      contentContainer.style.display = 'block';
      return;  // panel already rendered — nothing more to do
    }

    cloudSecurityPricingData = data;
    window._secData          = data;

    if (!contentContainer) {
      // DOM not ready yet — retry once after a short delay
      setTimeout(function() { renderPanel(data); }, 150);
      return;
    }

    const schema = [
      { id: 'sec-guardduty',  icon: 'fa-shield-halved',    owasp: 'OWASP A09',      label: 'Amazon GuardDuty',      inputId: 'input-guardduty-gb',        inputUnit: 'GB logs/mo',         desc: `Threat detection via VPC Flow Logs & CloudTrail. First 500 GB/mo free. $${data.guardDutyPerGbLogs}/GB after.` },
      { id: 'sec-inspector',  icon: 'fa-magnifying-glass', owasp: 'OWASP A06',      label: 'Amazon Inspector',      inputId: 'input-inspector-instances', inputUnit: 'EC2 instances',      desc: `CVE & network-exposure scans of EC2 and containers. $${data.inspectorPerInstanceMonth}/instance/mo.` },
      { id: 'sec-waf',        icon: 'fa-shield',           owasp: 'OWASP A03·A04', label: 'AWS WAF',           inputId: 'input-waf-rules',           inputUnit: 'custom rules',       desc: `Layer-7 firewall. Blocks SQLi, XSS, rate abuse. $${data.wafWebAclPerMonth} WebACL + $${data.wafRulePerMonth}/rule + $${data.wafPer1MRequests}/1M req.` },
      { id: 'sec-macie',      icon: 'fa-eye',              owasp: 'OWASP A02',      label: 'Amazon Macie',          inputId: 'input-macie-gb',            inputUnit: 'GB S3 data',         desc: `ML PII discovery in S3. First 1 GB/mo free. $${data.maciePerGbClassified}/GB.` },
      { id: 'sec-cloudwatch', icon: 'fa-chart-line',       owasp: 'OWASP A09',      label: 'CloudWatch Logs',       inputId: 'input-cw-gb',               inputUnit: 'GB ingested/mo',     desc: `Log ingestion & alerting. $${data.cloudwatchLogsIngestionPerGb}/GB in · $${data.cloudwatchLogsStoragePerGbMonth}/GB-mo stored.` },
      { id: 'sec-audit',      icon: 'fa-clipboard-check',  owasp: 'OWASP A09',      label: 'AWS Audit Manager',     inputId: 'input-audit-assessments',   inputUnit: 'active assessments', desc: `Compliance evidence (SOC2/PCI/ISO). $${data.auditManagerPerAssessmentMonth}/assessment/mo.` },
      { id: 'sec-kms',        icon: 'fa-key',              owasp: 'OWASP A02',      label: 'AWS KMS (Encryption)',  inputId: 'input-kms-keys',            inputUnit: 'CMKs (keys)',        desc: `Envelope encryption for S3/RDS/EBS. $${data.kmsCmkPerMonth}/CMK/mo + $${data.kmsApiCallsPer10k}/10K API calls.` }
    ];

    contentContainer.innerHTML = schema.map(svc => `
      <div class="tactic-row" id="row-${svc.id}">
        <input type="checkbox" class="tactic-check cloud-service-check" id="${svc.id}"
               onchange="recalculateSecCost(); updateCloudTacticsBadge(); if(window.refreshTacticMappingDisplay) window.refreshTacticMappingDisplay();">
        <div class="tactic-label-group">
          <label class="tactic-label" for="${svc.id}">
            <i class="fas ${svc.icon}" style="margin-right:5px;width:16px;"></i>${svc.label}
            <span class="warning-badge badge-red">+ cost</span>
            ${svc.owasp ? `<span class="warning-badge" style="background:rgba(124,58,237,.1);color:#7c3aed;font-size:.6rem;">${svc.owasp}</span>` : ''}
          </label>
          <span class="tactic-description">${svc.desc}</span>
          <div id="meta-container-${svc.id}"></div>
        </div>
        <div class="tactic-input-group">
          <input type="number" id="${svc.inputId}" min="0" placeholder="0"
                 oninput="if(document.getElementById('${svc.id}')&&document.getElementById('${svc.id}').checked) recalculateSecCost();">
          <span class="tactic-unit">${svc.inputUnit}</span>
        </div>
      </div>`).join('');

    // Hide spinner, show content
    if (loadingEl)        loadingEl.style.display        = 'none';
    contentContainer.style.display = 'block';

    // Inject OWASP/CWE badges and relevance highlighting
    if (window.refreshTacticMappingDisplay)          window.refreshTacticMappingDisplay();
    if (typeof evaluateCloudServiceRelevance === 'function') evaluateCloudServiceRelevance();
  }

  // Step 1: render immediately with fallback so the panel is NEVER blocked.
  renderPanel(SEC_FALLBACK);

  // Step 2: upgrade with live AWS prices in the background.
  // Updates pricing data and refreshes only the description text in each row
  // without rebuilding innerHTML (which would wipe checkbox states).
  fetchAwsPrice('security-services')
    .then(function(data) {
    console.warn("SUCCESS - /security-services:");
          console.warn(data);

      cloudSecurityPricingData = data;
      window._secData          = data;
      // Patch description spans with live prices — leave checkboxes untouched
      var liveDescs = {
        'sec-guardduty':  'Threat detection via VPC Flow Logs & CloudTrail. First 500 GB/mo free. $' + (data.guardDutyPerGbLogs||1.00) + '/GB after.',
        'sec-inspector':  'CVE & network-exposure scans of EC2 and containers. $' + (data.inspectorPerInstanceMonth||1.178) + '/instance/mo.',
        'sec-waf':        'Layer-7 firewall. Blocks SQLi, XSS, rate abuse. $' + (data.wafWebAclPerMonth||5.00) + ' WebACL + $' + (data.wafRulePerMonth||1.00) + '/rule + $' + (data.wafPer1MRequests||0.60) + '/1M req.',
        'sec-macie':      'ML PII discovery in S3. First 1 GB/mo free. $' + (data.maciePerGbClassified||1.00) + '/GB.',
        'sec-cloudwatch': 'Log ingestion & alerting. $' + (data.cloudwatchLogsIngestionPerGb||0.50) + '/GB in · $' + (data.cloudwatchLogsStoragePerGbMonth||0.03) + '/GB-mo stored.',
        'sec-audit':      'Compliance evidence (SOC2/PCI/ISO). $' + (data.auditManagerPerAssessmentMonth||6.00) + '/assessment/mo.',
        'sec-kms':        'Envelope encryption for S3/RDS/EBS. $' + (data.kmsCmkPerMonth||1.00) + '/CMK/mo + $' + (data.kmsApiCallsPer10k||0.03) + '/10K API calls.'
      };
      Object.keys(liveDescs).forEach(function(id) {
        var row = document.getElementById('row-' + id);
        if (!row) return;
        var descSpan = row.querySelector('.tactic-description');
        if (descSpan) descSpan.textContent = liveDescs[id];
      });
      console.log('[CloudSec] Live AWS pricing applied to descriptions.');
    })
    .catch(function(err) {
     console.error("ERROR - /security-services: \n" + error);

      console.info('[CloudSec] Using fallback pricing (' + err.message + ')');
    });

  return Promise.resolve();
}

function loadCostOptSection() {
  fetchAwsPrice('cost-optimisation')
    .then(data => {

     console.warn("SUCCESS - /cost-optimisation:");
              console.warn(data);

      cloudCostOptimizationPricingData = data;
      window._coData = data;   /* expose for recalculateCostOpt() in calculator.js */
      const contentContainer = document.getElementById('costopt-content');
      if (!contentContainer) return;

      const estimatedBaselineSpend = computeEc2BaselineSpend();

      let dynamicHtmlContent = `
        <div class="notice notice-blue" style="margin-bottom:12px;">
          <i class="fas fa-info-circle"></i>
          <div>RI &amp; Savings Plans apply to EC2/EKS compute already configured above. Spend is auto-calculated.</div>
        </div>
        <div class="tactic-row" style="background:var(--amber-bg);border-radius:var(--r);padding:8px 12px;margin-bottom:8px;border:1px solid var(--amber-border);">
          <div class="tactic-label-group">
            <label class="tactic-label" style="color:var(--amber);">
              <i class="fas fa-calculator" style="margin-right:5px;"></i>Estimated on-demand compute spend
            </label>
            <span class="tactic-description">Auto-calculated from EC2/EKS selections. Edit if needed.</span>
          </div>
          <div class="tactic-input-group">
            <input type="number" id="input-ec2-monthly-spend" min="0" placeholder="500" value="${estimatedBaselineSpend || ''}" oninput="recalculateCostOpt()">
            <span class="tactic-unit">USD/mo</span>
            <button onclick="const baseSpend = computeEc2BaselineSpend(); if(baseSpend){document.getElementById('input-ec2-monthly-spend').value=baseSpend;} recalculateCostOpt();"
                    style="margin-left:6px;padding:3px 8px;font-size:.7rem;background:var(--amber-bg);border:1px solid var(--amber-border);color:var(--amber);border-radius:4px;cursor:pointer;">
              <i class="fas fa-sync-alt"></i> Refresh
            </button>
          </div>
        </div>
      `;

      const optimizationStrategies = [
        { id: 'opt-ri-standard', label: `Standard RI (1-yr) — ~${data.reservedInstance1yrSavingsPct}% savings`, desc: data.riNote, numId: 'input-ri-count', numL: 'RIs' },
        { id: 'opt-ri-3yr', label: `Standard RI (3-yr) — ~${data.reservedInstance3yrSavingsPct}% savings`, desc: '', numId: null, numL: null },
        { id: 'opt-ri-convertible', label: `Convertible RI (1-yr) — ~${data.convertibleRi1yrSavingsPct}% savings`, desc: 'Can be exchanged for different instance families.', numId: 'input-cri-count', numL: 'CRIs' },
        { id: 'opt-savings-plan-1yr', label: `Compute Savings Plan (1-yr) — ~${data.savingsPlan1yrSavingsPct}% savings`, desc: data.savingsPlanNote, numId: 'input-sp-commitment', numL: 'USD/hr' },
        { id: 'opt-savings-plan-3yr', label: `Compute Savings Plan (3-yr) — ~${data.savingsPlan3yrSavingsPct}% savings`, desc: '', numId: null, numL: null },
        { id: 'opt-trusted-advisor', label: 'AWS Trusted Advisor (Business Support)', desc: `${data.trustedAdvisorNote} Min $${data.businessSupportMinMonthUsd}/mo or ${data.businessSupportPctMonthlyUsage}% of usage.`, numId: null, numL: null }
      ];

      optimizationStrategies.forEach(strategy => {
        dynamicHtmlContent += `
          <div class="tactic-row">
            <input type="checkbox" class="tactic-check" id="${strategy.id}" onchange="recalculateCostOpt()">
            <div class="tactic-label-group">
              <label class="tactic-label" for="${strategy.id}">${strategy.label}</label>
              ${strategy.desc ? `<span class="tactic-description">${strategy.desc}</span>` : ''}
            </div>
            ${strategy.numId ? `
              <div class="tactic-input-group">
                <input type="number" id="${strategy.numId}" min="1" placeholder="1" oninput="recalculateCostOpt()">
                <span class="tactic-unit">${strategy.numL}</span>
              </div>` : ''}
          </div>
        `;
      });

      contentContainer.innerHTML = dynamicHtmlContent;
      toggleUiVisibility('costopt-loading', false);
      contentContainer.style.display = 'block';

      // Auto-populate calculation benchmarks
      const spendInput = document.getElementById('input-ec2-monthly-spend');
      if (spendInput && (!spendInput.value || parseFloat(spendInput.value) === 0)) {
        const structuralRefresh = computeEc2BaselineSpend();
        if (structuralRefresh) {
          spendInput.value = structuralRefresh;
        }
      }
      recalculateCostOpt();
    })
    .catch(error => {
    console.error("ERROR - /cost-optimisation: \n" +error);
      const loadingLabel = document.getElementById('costopt-loading');
      if (loadingLabel) loadingLabel.textContent = `Could not fetch cost optimisation: ${error.message}`;
    });
}

function loadCachingSection() {
  fetchAwsPrice('caching-pricing')
    .then(data => {
    console.warn("SUCCESS - /caching-pricing:");
                  console.warn(data);
      cacheInfrastructurePricingData = data;
      window._cacheData = data;   /* expose for unit-economics.js */
      const contentContainer = document.getElementById('caching-content');
      if (!contentContainer) return;

      contentContainer.innerHTML = `
        <div class="notice notice-blue" style="margin-bottom:12px;">
          <i class="fas fa-info-circle"></i>
          <div>ElastiCache reduces latency. Redis supports persistence; Memcached is simpler and scales horizontally.</div>
        </div>
        <div class="tactic-row">
          <input type="checkbox" class="tactic-check" id="tactic-cache" onchange="recalculateCaching()">
          <div class="tactic-label-group">
            <label class="tactic-label" for="tactic-cache">Include ElastiCache in TCO <span class="warning-badge badge-red">+ cost</span></label>
            <span class="tactic-description">${data.note}</span>
          </div>
        </div>
        <div id="cache-params" style="display:none;padding-left:28px;border-left:3px solid var(--gold-border);margin:4px 0 8px;">
          <div class="tactic-row">
            <div class="tactic-label-group"><label class="tactic-label">Engine</label></div>
            <div class="tactic-input-group" style="min-width:160px;">
              <select id="input-cache-engine" onchange="recalculateCaching()" style="width:160px;">
                <option value="redis">Redis</option>
                <option value="memcached">Memcached</option>
              </select>
            </div>
          </div>
          <div class="tactic-row">
            <div class="tactic-label-group"><label class="tactic-label">Node type</label></div>
            <div class="tactic-input-group" style="min-width:200px;">
              <select id="input-cache-node" onchange="recalculateCaching()" style="width:200px;">
                <option value="r6g.large">cache.r6g.large (13 GiB)</option>
                <option value="r6g.xlarge">cache.r6g.xlarge (26 GiB)</option>
                <option value="r6g.2xlarge">cache.r6g.2xlarge (52 GiB)</option>
              </select>
            </div>
          </div>
          <div class="tactic-row">
            <div class="tactic-label-group">
              <label class="tactic-label" for="input-cache-nodes">Nodes</label>
              <span class="tactic-description">Primary + replicas (Redis) or cluster nodes (Memcached).</span>
            </div>
            <div class="tactic-input-group">
              <input type="number" id="input-cache-nodes" min="1" placeholder="2" oninput="recalculateCaching()">
              <span class="tactic-unit">nodes</span>
            </div>
          </div>
          <div id="cache-result-inner" class="live-preview" style="display:none;margin-top:8px;"></div>
        </div>
      `;

      toggleUiVisibility('caching-loading', false);
      contentContainer.style.display = 'block';
    })
    .catch(error => {
    console.error("ERROR - /caching-pricing: \n" + error);
      const loadingLabel = document.getElementById('caching-loading');
      if (loadingLabel) loadingLabel.textContent = `Could not fetch caching pricing: ${error.message}`;
    });
}

/* =====================================================================
    AWS API GATEWAY MODULE SPECIFIC
===================================================================== */

function loadApiGwSection() {
  if (apiGatewayPricingDatabase) {
    renderApiGwContent();
    return;
  }

  fetch('/api/aws/api-gateway-pricing')
    .then(response => response.ok ? response.json() : null)
    .then(data => {

    console.warn("SUCCESS - /api/aws/api-gateway-pricing:");
    console.warn(data);

      apiGatewayPricingDatabase = data || API_GATEWAY_FALLBACK_PRICING;
      renderApiGwContent();
    })
    .catch(() => {
    console.error("ERROR - /api/aws/api-gateway-pricing:");
      apiGatewayPricingDatabase = API_GATEWAY_FALLBACK_PRICING;
      renderApiGwContent();
    });
}

function renderApiGwContent() {
  toggleUiVisibility('apigw-loading', false);
  toggleUiVisibility('apigw-content', true);
  recalculateApiGw();
}

function recalculateApiGw() {
  if (!apiGatewayPricingDatabase) return;

  var apiGatewayType        = (document.getElementById('input-apigw-type') || { value: 'rest' }).value || 'rest';
  var callsPerMonthMillions = parseFloat((document.getElementById('input-apigw-calls-per-month') || { value: '0' }).value) || 0;
  var cacheHourlyRate       = parseFloat((document.getElementById('input-apigw-cache') || { value: '0' }).value) || 0;

  var requestBody = {
    apiGatewayType:           apiGatewayType,
    callsPerMonthMillions:    callsPerMonthMillions,
    cacheHourlyRate:          cacheHourlyRate,
    restApiPer1MCallsMonthly: apiGatewayPricingDatabase.restApiPer1MCallsMonthly || 3.50,
    httpApiPer1MCallsFirst1B: apiGatewayPricingDatabase.httpApiPer1MCallsFirst1B || 1.00,
    wsApiPer1MConnections:    apiGatewayPricingDatabase.wsApiPer1MConnections    || 0.25
  };

  fetch('/api/cost/api-gateway', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(requestBody)
  })
  .then(function(response) { return response.ok ? response.json() : null; })
  .then(function(data) {
    if (!data) return;

    console.warn("SUCCESS - /api/cost/api-gateway:");
        console.warn(data);

    var resultContainer = document.getElementById('apigw-result');
    if (resultContainer) {
      var executionSummary = callsPerMonthMillions > 0
        ? data.breakdownLines.join(' + ')
        : '';
      var costDisplay = data.monthlyTotalUsd > 0
        ? (executionSummary ? executionSummary + ' = ' : '')
          + '<span style="color:var(--red);">$' + data.monthlyTotalUsd.toFixed(2) + '/mo</span>'
        : '&nbsp;enter call volume above';

      resultContainer.innerHTML = '<i class="fas fa-calculator" style="margin-right:6px;"></i>'
        + '<strong>API Gateway (' + apiGatewayType.toUpperCase() + '): ' + costDisplay + '</strong>';
    }

    if (data.monthlyTotalUsd > 0) {
      sessionStorage.setItem('tco_apigw_cost', data.monthlyTotalUsd.toFixed(4));
    } else {
      sessionStorage.removeItem('tco_apigw_cost');
    }

    var currentBaseRps = parseInt((document.getElementById('requestsPerSecond') || { value: '0' }).value) || 0;
    if (currentBaseRps) updateLiveComparison(currentBaseRps, currentBaseRps);
  })
  .catch(function(error) { console.error('ERROR recalculateApiGw failed:', error.message); });
}


/* =====================================================================
    TACTIC CONTRIBUTIONS (gRPC MACH TELEMETRY MODEL)
===================================================================== */

function collectTacticContributions(baseRequestsPerSecond, backendResult) {
  var tlsOverheadBytes            = (backendResult && backendResult.tlsOverheadBytes) || 0;
  var jwtOverheadBytes            = (backendResult && backendResult.jwtOverheadBytes) || 0;
  var protoResponseSizeBytes      = (backendResult && backendResult.responseSizeEff)  || 0;

  var isChecked  = function(id) { var el = document.getElementById(id); return !!(el && el.checked); };
  var intValue   = function(id) { var el = document.getElementById(id); return parseInt((el || { value: '0' }).value, 10) || 0; };
  var floatValue = function(id, fallback) {
    var el = document.getElementById(id);
    return parseFloat((el || { value: String(fallback) }).value) || fallback;
  };

  var requestBody = {
    baseRps:                          baseRequestsPerSecond,
    protoResponseSizeEffectiveBytes:  protoResponseSizeBytes,
    tlsOverheadBytesFromBackend:      tlsOverheadBytes,
    jwtOverheadBytesFromBackend:      jwtOverheadBytes,
    clientSideLoadBalancingEnabled:   isChecked('tactic-client-lb'),
    serverSideLoadBalancingEnabled:   isChecked('tactic-server-lb'),
    circuitBreakerEnabled:            isChecked('tactic-cb'),
    basicAuthEnabled:                 isChecked('tactic-basic-auth'),
    timeoutEnabled:                   isChecked('tactic-timeout'),
    timeoutMs:                        intValue('input-timeout'),
    retryEnabled:                     isChecked('tactic-retry'),
    retryErrorRatePct:                floatValue('input-retry-error-pct', 5),
    tlsEnabled:                       isChecked('tactic-tls'),
    mtlsEnabled:                      isChecked('tactic-mtls'),
    tlsReconnectsPerHour:             intValue('input-tls-reconnects'),
    oauthEnabled:                     isChecked('tactic-oauth'),
    tokenValidationMode:              (document.getElementById('input-token-validation') || { value: 'LOCAL' }).value || 'LOCAL',
    tokenTtlSeconds:                  intValue('input-token-ttl') || 3600,
    concurrentClients:                intValue('input-concurrent-clients') || 1
  };

  return fetch('/api/cost/tactic-contributions', {
    method:  'POST',
    headers: { 'Content-Type': 'application/json' },
    body:    JSON.stringify(requestBody)
  })
  .then(function(response) { return response.ok ? response.json() : null; })
  .then(function(data) {
    if (!data) return [];

    console.warn("SUCCESS - /api/cost/tactic-contributions:");
    console.warn(data);

    window._lastTacticContributions = data.contributions;
    return data.contributions;
  })
  .catch(function(err) {
    console.error('ERROR - /api/cost/tactic-contributions collectTacticContributions backend call failed \n:', err.message);
    return [];
  });
}

function fetchAwsPrice(endpoint) {
  return fetch('/api/aws/' + endpoint).then(function (response) {
    if (!response.ok) throw new Error('HTTP ' + response.status);
    return response.json();
  });
}