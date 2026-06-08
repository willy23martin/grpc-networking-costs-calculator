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

function toggleSagaParams(show) {
  toggleUiVisibility('saga-params', show);
  document.getElementById('saga-explainer')?.classList.toggle('visible', show);
  recalculateRps();
}

function toggleTlsOptions() {
  const isTlsActive = Boolean(document.getElementById('tactic-tls')?.checked);
  const isMtlsActive = Boolean(document.getElementById('tactic-mtls')?.checked);

  toggleUiVisibility('tls-params', isTlsActive);
  toggleUiVisibility('mtls-params', isMtlsActive);
  toggleUiVisibility('tls-byte-badge', isTlsActive, 'inline-flex');
  toggleUiVisibility('mtls-byte-badge', isMtlsActive, 'inline-flex');

  recalculateRps();
  scheduleSessionSave();
}

function toggleOAuthParams(show) {
  toggleUiVisibility('oauth-params', show);
  toggleUiVisibility('jwt-byte-badge', show, 'inline-flex');
  recalculateRps();
  scheduleSessionSave();
}

/* =====================================================================
    AWS Remote Pricing Async Module Loaders
===================================================================== */

function loadAlbSection() {
  fetchAwsPrice('alb-pricing')
    .then(data => {
      applicationLoadBalancerPricingData = data;
      const descriptionContainer = document.getElementById('alb-price-desc');
      if (descriptionContainer) {
        descriptionContainer.innerHTML = `Fixed: <strong>$${data.fixedPerHourUsd.toFixed(4)}/hr</strong> ($${data.fixedPerMonthUsd}/mo per ALB) + <strong>$${data.lcuPerHourUsd.toFixed(4)}/hr per LCU</strong>. Source: ${data.source}`;
      }
      toggleUiVisibility('alb-loading', false);
      toggleUiVisibility('alb-content', true);
    })
    .catch(error => {
      const loadingLabel = document.getElementById('alb-loading');
      if (loadingLabel) loadingLabel.textContent = `Could not fetch ALB pricing: ${error.message}`;
    });
}

function loadDbBackupSection() {
  fetchAwsPrice('database-backup-pricing')
    .then(data => {
      databaseBackupPricingData = data;
      toggleUiVisibility('dbbackup-loading', false);
      toggleUiVisibility('dbbackup-content', true);
    })
    .catch(error => {
      const loadingLabel = document.getElementById('dbbackup-loading');
      if (loadingLabel) loadingLabel.textContent = `Could not fetch DB pricing: ${error.message}`;
    });
}

function loadCloudSecSection() {
  fetchAwsPrice('security-services')
    .then(data => {
      cloudSecurityPricingData = data;
      const contentContainer = document.getElementById('cloudsec-content');
      if (!contentContainer) return;

      // Declarative Infrastructure Security Map Structure
      const securityServicesSchema = [
        { id: 'sec-guardduty', icon: 'fa-shield-halved', owasp: 'OWASP A09', label: 'Amazon GuardDuty', inputId: 'input-guardduty-gb', inputUnit: 'GB logs/mo', desc: `Threat detection via VPC Flow Logs & CloudTrail. First 500 GB/mo free. $${data.guardDutyPerGbLogs}/GB after.` },
        { id: 'sec-inspector', icon: 'fa-magnifying-glass', label: 'Amazon Inspector', owasp: 'OWASP A06', inputId: 'input-inspector-instances', inputUnit: 'EC2 instances', desc: `CVE & network-exposure scans of EC2 and containers. $${data.inspectorPerInstanceMonth}/instance/mo.` },
        { id: 'sec-waf', icon: 'fa-shield', label: 'AWS WAF', owasp: 'OWASP A03·A04', inputId: 'input-waf-rules', inputUnit: 'custom rules', desc: `Layer-7 firewall for ALB/API GW. Blocks SQLi, XSS, rate abuse. $${data.wafWebAclPerMonth} WebACL + $${data.wafRulePerMonth}/rule + $${data.wafPer1MRequests}/1M req.` },
        { id: 'sec-macie', icon: 'fa-eye', label: 'Amazon Macie', owasp: 'OWASP A02', inputId: 'input-macie-gb', inputUnit: 'GB S3 data', desc: `ML PII & credential discovery in S3. First 1 GB/mo free. $${data.maciePerGbClassified}/GB.` },
        { id: 'sec-cloudwatch', icon: 'fa-chart-line', label: 'CloudWatch Logs', owasp: 'OWASP A09', inputId: 'input-cw-gb', inputUnit: 'GB ingested/mo', desc: `Log ingestion & alerting. $${data.cloudwatchLogsIngestionPerGb}/GB in · $${data.cloudwatchLogsStoragePerGbMonth}/GB-mo stored.` },
        { id: 'sec-audit', icon: 'fa-clipboard-check', label: 'AWS Audit Manager', owasp: 'OWASP A09', inputId: 'input-audit-assessments', inputUnit: 'active assessments', desc: `SOC2/PCI/ISO compliance evidence. $${data.auditManagerPerAssessmentMonth}/assessment/mo.` },
        { id: 'sec-kms', icon: 'fa-key', label: 'AWS KMS (Encryption)', owasp: 'OWASP A02', inputId: 'input-kms-keys', inputUnit: 'CMKs (keys)', desc: `Envelope encryption for S3/RDS/EBS. $${data.kmsCmkPerMonth}/CMK/mo + $${data.kmsApiCallsPer10k}/10K API calls. Typically 1–3 CMKs per service.` }
      ];

      contentContainer.innerHTML = securityServicesSchema.map(service => `
        <div class="tactic-row">
          <input type="checkbox" class="tactic-check" id="${service.id}" onchange="recalculateSecCost()">
          <div class="tactic-label-group">
            <label class="tactic-label" for="${service.id}">
              <i class="fas ${service.icon}" style="margin-right:5px;width:16px;"></i>${service.label}
              <span class="warning-badge badge-red">+ cost</span>
              ${service.owasp ? `<span class="warning-badge" style="background:rgba(124,58,237,.1);color:#7c3aed;font-size:.6rem;">${service.owasp}</span>` : ''}
            </label>
            <span class="tactic-description">${service.desc}</span>
          </div>
          <div class="tactic-input-group">
            <input type="number" id="${service.inputId}" min="0" placeholder="0" oninput="recalculateSecCost()">
            <span class="tactic-unit">${service.inputUnit}</span>
          </div>
        </div>
      `).join('');

      toggleUiVisibility('cloudsec-loading', false);
      contentContainer.style.display = 'block';
    })
    .catch(error => {
      const loadingLabel = document.getElementById('cloudsec-loading');
      if (loadingLabel) loadingLabel.textContent = `Could not fetch security pricing: ${error.message}`;
    });
}

function loadCostOptSection() {
  fetchAwsPrice('cost-optimisation')
    .then(data => {
      cloudCostOptimizationPricingData = data;
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
      const loadingLabel = document.getElementById('costopt-loading');
      if (loadingLabel) loadingLabel.textContent = `Could not fetch cost optimisation: ${error.message}`;
    });
}

function loadCachingSection() {
  fetchAwsPrice('caching-pricing')
    .then(data => {
      cacheInfrastructurePricingData = data;
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
      apiGatewayPricingDatabase = data || API_GATEWAY_FALLBACK_PRICING;
      renderApiGwContent();
    })
    .catch(() => {
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
  .catch(function(error) { console.warn('recalculateApiGw failed:', error.message); });
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
    sagaEnabled:                      isChecked('tactic-saga'),
    sagaExternalVpc:                  isChecked('tactic-saga-external-vpc'),
    sagaCompensatableSteps:           intValue('input-saga-compensatable'),
    sagaRetriableSteps:               intValue('input-saga-retriable'),
    sagaPivotSteps:                   intValue('input-saga-pivot'),
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
    window._lastTacticContributions = data.contributions;
    return data.contributions;
  })
  .catch(function(err) {
    console.warn('collectTacticContributions backend call failed:', err.message);
    return [];
  });
}

function fetchAwsPrice(endpoint) {
  return fetch('/api/aws/' + endpoint).then(function (response) {
    if (!response.ok) throw new Error('HTTP ' + response.status);
    return response.json();
  });
}