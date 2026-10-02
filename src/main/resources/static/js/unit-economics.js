/* =======================================================================
   unit-economics.js
   Phase 4 — TCO Breakdown + Unit Economics + ROI

   All cost computations use two existing backend endpoints:
     POST /api/cost/cloud-infra-total  (UnitEconomicsController)
     POST /api/cost/unit-economics     (UnitEconomicsController)

   Security service costs are read from per-service sessionStorage keys
   (written by recalculateSecCost() which uses /api/cloud/security-services
   pricing data) — one row per service, only when selected and cost > 0.
   ======================================================================= */

var runningTotalCost = 0;
var hoursInMonth = 730;
var billingMonthsInYear = 12;

var tacticMissionCriticalCheckbox = document.getElementById('tactic-mission-critical');
var missionCriticalNoteElement    = document.getElementById('p4-mission-critical-note');
var finopsNotesElement            = document.getElementById('p4-finops-notes');

function populateReportSummary() {

  if (missionCriticalNoteElement && tacticMissionCriticalCheckbox) {
    missionCriticalNoteElement.style.display = tacticMissionCriticalCheckbox.checked ? 'flex' : 'none';
  }
  if (finopsNotesElement) {
    finopsNotesElement.style.display = 'block';
  }

  var summaryGrid = document.getElementById('reportServiceSummary');
  if (!summaryGrid) return;

  var serviceNameInput = document.getElementById('serviceName');
  var servicePurposeInput = document.getElementById('servicePurpose');
  var requestsPerSecondInput = document.getElementById('requestsPerSecond');

  var serviceName = ((serviceNameInput && serviceNameInput.value.trim()) || sessionStorage.getItem('svc_name') || '').trim() || '\u2014';
  var servicePurpose = ((servicePurposeInput && servicePurposeInput.value.trim()) || sessionStorage.getItem('svc_purpose') || '').trim() || '\u2014';
  var requestsPerSecond = (requestsPerSecondInput && requestsPerSecondInput.value) || sessionStorage.getItem('svc_rps') || '0';

  var numConsumersElement = document.getElementById('numConsumers');
  var consumerCount = (numConsumersElement && numConsumersElement.value) ? numConsumersElement.value : (sessionStorage.getItem('svc_consumers') || '\u2014');

  var consumerTypeElement = document.getElementById('consumerType');
  var consumerType = (consumerTypeElement && consumerTypeElement.value) ? consumerTypeElement.value : (sessionStorage.getItem('svc_consumer_type') || 'SERVICES');

  if (!selectedBUC && sessionStorage.getItem('svc_buc')) {
    selectedBUC = sessionStorage.getItem('svc_buc');
  }

  var businessUseCase = selectedBUC ? (selectedBUC.indexOf('CUSTOM:') === 0 ? selectedBUC.replace('CUSTOM:', '') : selectedBUC) : '\u2014';
  var protobufFile = window._lastProtoFile ? window._lastProtoFile.name : '\u2014';

  var consumerTypeLabelMap = { SERVICES: 'Microservices / APIs', USERS: 'End Users', BOTH: 'Mixed' };
  var consumerTypeLabel = consumerTypeLabelMap[consumerType] || consumerType;

  var tacticIds = ['tactic-client-lb', 'tactic-server-lb', 'tactic-timeout', 'tactic-retry', 'tactic-cb', 'tactic-tls', 'tactic-mtls', 'tactic-oauth', 'tactic-basic-auth'];
  var tacticLabelsMap = {
    'tactic-client-lb': 'Client-side LB',
    'tactic-server-lb': 'Server-side LB',
    'tactic-timeout': 'Timeout',
    'tactic-retry': 'Retry',
    'tactic-cb': 'Circuit Breaker',
    'tactic-tls': 'TLS',
    'tactic-mtls': 'mTLS',
    'tactic-oauth': 'OAuth 2.0 + JWT',
    'tactic-basic-auth': 'Basic Auth'
  };

  var activeTactics = tacticIds.filter(function (id) {
    var element = document.getElementById(id);
    return element && element.checked;
  }).map(function (id) {
    return tacticLabelsMap[id] || id;
  });

  // ── RPS & byte impact details for Phase 4 summary card ─────────────
  // Use backend-authoritative values cached on window by recalculateRps()
  // so Retry shows the correct extra req/s (= pct × effectiveBaseRps, not just baseRps).
  var rpsImpacts = [];
  var numericRps = parseInt(requestsPerSecond) || 0;

  var retryIsActive = !!(document.getElementById('tactic-retry') && document.getElementById('tactic-retry').checked);
  if (retryIsActive) {
    var retryErrorPercentage = parseFloat(
      (document.getElementById('input-retry-error-pct') || { value: '5' }).value
    ) || 5;
    // Prefer backend-computed extra (accounts for OAuth introspection expanding effective base).
    // Fall back to local estimate only when no backend response has been received yet.
    var authorizedRetryExtra = (window._lastRetryExtra > 0)
      ? window._lastRetryExtra
      : Math.round(numericRps * retryErrorPercentage / 100);
    // The base used by the backend = effectiveRps before retry = effectiveRps - retryExtra
    var retryBase = (window._lastEffectiveRps > 0)
      ? (window._lastEffectiveRps - authorizedRetryExtra)
      : numericRps;
    rpsImpacts.push(
      'Retry: +' + authorizedRetryExtra.toLocaleString()
      + ' req/s (' + retryErrorPercentage + '% of '
      + retryBase.toLocaleString() + ' eff. RPS)'
    );
  }

  var tlsIsActive  = !!(document.getElementById('tactic-tls')  && document.getElementById('tactic-tls').checked);
  var mtlsIsActive = !!(document.getElementById('tactic-mtls') && document.getElementById('tactic-mtls').checked);
  if (tlsIsActive) {
    var tlsHandshakeExtra = window._lastHandshakeExtra || 0;
    rpsImpacts.push(
      'TLS: +29 B/frame on responses (RFC 8446 AES-GCM) → increases egress GB cost'
      + (tlsHandshakeExtra > 0 ? ', +' + tlsHandshakeExtra + ' handshake req/s' : '')
    );
  }
  if (mtlsIsActive) {
    var mtlsHandshakeExtra = window._lastHandshakeExtra || 0;
    rpsImpacts.push(
      'mTLS: +29 B/frame on responses (RFC 8446) + 5 handshake msgs/reconnect'
      + (mtlsHandshakeExtra > 0 ? ', +' + mtlsHandshakeExtra + ' handshake req/s' : '')
    );
  }

  var oauthIsActive = !!(document.getElementById('tactic-oauth') && document.getElementById('tactic-oauth').checked);
  if (oauthIsActive) {
    var tokenTtlSeconds     = parseInt((document.getElementById('input-token-ttl')           || { value: '3600' }).value) || 3600;
    var concurrentClients   = parseInt((document.getElementById('input-concurrent-clients')  || { value: '1'    }).value) || 1;
    var tokenValidationMode = (document.getElementById('input-token-validation') || { value: 'LOCAL' }).value || 'LOCAL';

    // Prefer backend-authoritative values; local calc as fallback
    var tokenAcqPerSecond       = (window._lastTokenAcqExtra > 0)
      ? window._lastTokenAcqExtra
      : Math.round(numericRps / (tokenTtlSeconds * concurrentClients));
    var remoteIntrospPerSecond  = (window._lastIntrospExtra > 0)
      ? window._lastIntrospExtra
      : (tokenValidationMode === 'REMOTE_INTROSPECTION' ? numericRps : 0);

    var oauthImpactParts = ['+650 B/req JWT header (RFC 7519, request-side — AWS inbound free)'];
    if (tokenAcqPerSecond    > 0) oauthImpactParts.push('+' + tokenAcqPerSecond.toLocaleString()    + ' token acq/s');
    if (remoteIntrospPerSecond > 0) oauthImpactParts.push('+' + remoteIntrospPerSecond.toLocaleString() + ' remote introspection/s');

    rpsImpacts.push('OAuth2+JWT: ' + oauthImpactParts.join(', '));
  }

  var rpsImpactDetailsHtml = rpsImpacts.length
    ? ('<div style="margin-top:10px;padding:10px 14px;background:rgba(255,60,60,.15);border-radius:var(--r);border:1px solid rgba(255,100,100,.3);">'
      + '<div style="font-size:.68rem;font-weight:700;text-transform:uppercase;color:rgba(255,200,200,.85);margin-bottom:5px;">RPS &amp; Byte Impacting Tactics</div>'
      + rpsImpacts.map(function (impactStr) { return '<div style="font-size:.76rem;color:#fff;margin-top:3px;">• ' + impactStr + '</div>'; }).join('') + '</div>')
    : '';

  summaryGrid.innerHTML = '<div style="background:linear-gradient(135deg,var(--blue-deep),#0F2540);border-radius:var(--r-lg);padding:24px 28px;margin-bottom:20px;color:#fff;">'
    + '<div style="font-size:.7rem;font-weight:700;text-transform:uppercase;letter-spacing:.1em;color:rgba(255,255,255,.55);margin-bottom:4px;">Service under analysis</div>'
    + '<div style="font-family:\'DM Serif Display\',serif;font-size:1.5rem;color:#F5ECD7;margin-bottom:6px;">' + serviceName + '</div>'
    + '<div style="font-size:.88rem;color:rgba(255,255,255,.8);line-height:1.6;max-width:620px;">' + servicePurpose + '</div>'
    + '<div style="display:flex;gap:16px;flex-wrap:wrap;margin-top:14px;">'
    + '<span style="background:rgba(255,255,255,.12);border-radius:999px;padding:4px 12px;font-size:.75rem;font-weight:600;"><i class="fas fa-diagram-project" style="margin-right:5px;"></i>' + businessUseCase + '</span>'
    + '<span style="background:rgba(255,255,255,.12);border-radius:999px;padding:4px 12px;font-size:.75rem;font-weight:600;"><i class="fas fa-bolt" style="margin-right:5px;"></i>' + parseInt(requestsPerSecond).toLocaleString() + ' base RPS</span>'
    + '<span style="background:rgba(255,255,255,.12);border-radius:999px;padding:4px 12px;font-size:.75rem;font-weight:600;"><i class="fas fa-users" style="margin-right:5px;"></i>' + consumerCount + ' ' + consumerTypeLabel + '</span>'
    + '<span style="background:rgba(255,255,255,.12);border-radius:999px;padding:4px 12px;font-size:.75rem;font-weight:600;font-family:monospace;"><i class="fas fa-file-code" style="margin-right:5px;"></i>' + protobufFile + '</span>'
    + '</div></div>'
    + '<div class="summary-grid" style="margin-bottom:0;"><div class="summary-item" style="grid-column:1/-1;">'
    + '<div class="summary-item-label">Applied Architecture Tactics</div>'
    + '<div class="tag-list" style="margin-top:6px;">'
    + (activeTactics.length ? activeTactics.map(function (tactic) { return '<span class="tag">' + tactic + '</span>'; }).join('') : '<span style="font-size:.8rem;color:var(--ink-light);">None selected \u2014 base configuration</span>')
    + '</div></div></div>';

  // Re-render the TCO Breakdown + Unit Economics every time Phase 4 is opened,
  // not just immediately after the proto-upload form submits. Without this call,
  // cloud service costs (ALB, security services, DB backup, API Gateway, etc.)
  // that the user selected after the initial calculation would never appear in
  // the TCO Breakdown table, since populateUnitEconomics was previously only
  // invoked one time at form-submission. Calling with no arguments makes it
  // derive egress cost and effective RPS from window/sessionStorage state.
  populateUnitEconomics();
}

/* =======================================================================
   computeCloudInfraCost  — local fallback only
   Reads sessionStorage keys written by backend-driven recalculate* fns.
   Guards every key against its DOM checkbox so stale keys are ignored.
   ======================================================================= */
function computeCloudInfraCost() {
  function checkedCost(cbId, key) {
    var cb = document.getElementById(cbId);
    if (!cb || !cb.checked) return 0;
    return parseFloat(sessionStorage.getItem(key) || '0');
  }
  var total = 0;
  total += checkedCost('tactic-alb',   'tco_alb_cost');
  total += checkedCost('tactic-cache', 'tco_cache_cost');
  total += checkedCost('tactic-apigw', 'tco_apigw_cost');
  var dbKeys = ['tactic-s3-backup','tactic-rds-snapshot','tactic-rds-multiaz',
                'tactic-aurora-replica','tactic-dynamo-global'];
  if (dbKeys.some(function(id){ var cb=document.getElementById(id); return cb&&cb.checked; })) {
    total += parseFloat(sessionStorage.getItem('tco_db_cost') || '0');
  }
  var cefKeys = ['cef-clusters','cef-cluster-lb','cef-host-storage','cef-workload-license'];
  if (cefKeys.some(function(id){ var cb=document.getElementById(id); return cb&&cb.checked; })) {
    total += parseFloat(sessionStorage.getItem('tco_container_cost') || '0');
  }
  ['sec-guardduty','sec-inspector','sec-waf','sec-macie',
   'sec-cloudwatch','sec-audit','sec-kms','sec-cloudtrail','sec-acm'
  ].forEach(function(id) { total += checkedCost(id, 'tco_sec_' + id); });
  var saving = parseFloat(sessionStorage.getItem('tco_finops_saving') || '0');
  runningTotalCost = Math.max(0, Math.round((total - saving) * 100) / 100);
  return runningTotalCost;
}

/* =======================================================================
   populateUnitEconomics
   Called when entering Phase 4. Collects all costs from sessionStorage
   (written by backend-driven recalculate* functions), posts to:
     POST /api/cost/cloud-infra-total  — server-side aggregation
     POST /api/cost/unit-economics     — unit costs + ROI
   Then renders the TCO table and Unit Economics grid.
   ======================================================================= */
function populateUnitEconomics(transferCostUsd, effectiveRps, requestsPerMonthRaw) {
  var unitEconGridElement = document.getElementById('unitEconGrid');
  if (!unitEconGridElement) return;

  // When called with no transferCostUsd (e.g. from populateReportSummary every time
  // Phase 4 is opened, not just right after form submission), derive the egress cost
  // and effective RPS from the values already stashed by the live comparison engine
  // in Phase 3. This makes the function safely re-callable on every Phase 4 visit so
  // cloud service costs (ALB, security, DB, API Gateway, etc.) are always reflected,
  // not just the one time the proto-upload results table happened to be on the page.
  if (transferCostUsd === undefined || transferCostUsd === null) {
    // Use the egress cost stored by Phase 3's renderComparisonFromBackend
    // so Phase 4 always shows the same base networking cost as Phase 3
    transferCostUsd = parseFloat(sessionStorage.getItem('tco_phase3_egress')||'0')
                   || window._lastEgressCostUsd || 0;
  }
  if (effectiveRps === undefined || effectiveRps === null) {
    effectiveRps = window._lastEffectiveRps
      || parseInt(sessionStorage.getItem('tco_effective_rps') || '0', 10) || 0;
  }

  var egressCost   = parseFloat(transferCostUsd) || 0;
  window._lastEgressCostUsd = egressCost;
  var baseRps      = parseInt((document.getElementById('requestsPerSecond')||{value:'0'}).value)||0;
  // numConsumers/consumerType: prefer the live DOM input (Phase 2), but fall back to the
  // sessionStorage value saved by schedulePhase2Save() — the DOM input may report an
  // empty value here if Phase 2 hasn't been re-rendered, which previously caused the
  // ROI/ARPU section to silently default to 1 consumer regardless of what the user set.
  var numConsumersInput = document.getElementById('numConsumers');
  var numConsumers = parseInt((numConsumersInput && numConsumersInput.value) || sessionStorage.getItem('svc_consumers') || '1', 10) || 1;
  var consumerTypeInput = document.getElementById('consumerType');
  var consumerType = (consumerTypeInput && consumerTypeInput.value) || sessionStorage.getItem('svc_consumer_type') || 'SERVICES';
  var revenue      = parseFloat((document.getElementById('revenuePerTransaction')||{value:''}).value)
                   || parseFloat(sessionStorage.getItem('svc_revenue_per_tx')||'0') || 0;
  var monthlyReqs  = effectiveRps*2592000;

  // ── Read costs from the tco_snapshot written by writeTcoSnapshot() ───────
  // writeTcoSnapshot() is called in calculator.js after every recalculate*
  // function and stores a complete JSON blob so Phase 4 always sees the latest
  // values regardless of DOM state, panel visibility, or checkbox scope issues.
  var _snap = {};
  try { _snap = JSON.parse(sessionStorage.getItem('tco_snapshot') || '{}'); } catch(e) {}

  var albCost       = parseFloat(_snap.alb)       || 0;
  var cacheCost     = parseFloat(_snap.cache)      || 0;
  var apiGwCost     = parseFloat(_snap.apigw)      || 0;
  var containerCost = parseFloat(_snap.container)  || 0;
  var dbCost        = parseFloat(_snap.db)         || 0;
  var ec2Cost       = parseFloat(_snap.ec2)        || 0;
  // Use snapshot value if available, fall back to direct sessionStorage read
  var finopsSaving   = parseFloat(_snap.finops)        || parseFloat(sessionStorage.getItem('tco_finops_saving')||'0');
  var finopsPct      = parseFloat(_snap.finopsPct)     || parseFloat(sessionStorage.getItem('tco_finops_pct')||'0');
  var finopsStrategy = (_snap.finopsStrategy || sessionStorage.getItem('tco_finops_strategy') || 'RI / Savings Plan');
  var spotSaving     = parseFloat(_snap.spot)          || parseFloat(sessionStorage.getItem('tco_spot_saving')||'0');
  var spotPct        = parseFloat(_snap.spotPct)       || parseFloat(sessionStorage.getItem('tco_spot_pct')||'0');
  var finopsPct      = parseFloat(_snap.finopsPct) || parseFloat(sessionStorage.getItem('tco_finops_pct')||'0');
  var finopsStrategy = (_snap.finopsStrategy)      || sessionStorage.getItem('tco_finops_strategy') || 'RI / Savings Plan';

  var secCostByService = _snap.sec || {};
  var secCostTotal = Object.keys(secCostByService).reduce(function(s, k) {
    return s + (parseFloat(secCostByService[k]) || 0);
  }, 0);

  // ec2Cost now read from tco_snapshot (set by recalculateReplicas via writeTcoSnapshot)

  var grossInfra   = albCost+cacheCost+apiGwCost+containerCost+dbCost+secCostTotal+ec2Cost;
  var totalSavings = finopsSaving + spotSaving;
  var netInfra     = Math.max(0, grossInfra - totalSavings);
  var localTco   = Math.round((egressCost + netInfra)*100)/100;

  // ── POST to existing backend endpoints ─────────────────────────────────
  var infraReqBody = {
    albMonthlyCostUsd       : albCost,
    cacheMonthlyCostUsd     : cacheCost,
    databaseMonthlyCostUsd  : dbCost,
    securityMonthlyCostUsd  : secCostTotal,
    containerMonthlyCostUsd : containerCost,
    apiGatewayMonthlyCostUsd: apiGwCost,
    ec2ReplicaMonthlyCostUsd: ec2Cost,
    finopsMonthlySavingUsd  : finopsSaving
  };
  var ueReqBody = {
    egressTransferCostUsd  : egressCost,
    cloudInfraCostUsd      : netInfra,
    finopsSavingUsd        : finopsSaving,
    effectiveRps           : effectiveRps,
    consumerCount          : numConsumers,
    revenuePerUserPerMonth : revenue
  };

  Promise.all([
    fetch('/api/cost/cloud-infra-total',{method:'POST',
      headers:{'Content-Type':'application/json'},body:JSON.stringify(infraReqBody)})
      .then(function(r){return r.ok?r.json():null;}).catch(function(){return null;}),
    fetch('/api/cost/unit-economics',{method:'POST',
      headers:{'Content-Type':'application/json'},body:JSON.stringify(ueReqBody)})
      .then(function(r){return r.ok?r.json():null;}).catch(function(){return null;})
  ]).then(function(results){
    var infraResp = results[0];
    var ueResp    = results[1];
    // Prefer Phase 3 stored total for consistency; fall back to backend then local
    // authTco = sum of all cost components — compute directly rather than
    // trusting tco_phase3_total which only contains the networking portion.
    // localTco = egressCost + netInfra is always the correct full total.
    var authTco   = localTco > 0 ? localTco
                  : (ueResp && ueResp.totalMonthlyTcoUsd > 0) ? ueResp.totalMonthlyTcoUsd
                  : parseFloat(sessionStorage.getItem('tco_phase3_total')||'0');
    window._lastComputedTco = authTco;

    _renderTcoBreakdownTable(egressCost, albCost, cacheCost, dbCost,
      secCostByService, containerCost, apiGwCost, ec2Cost,
      finopsSaving, finopsPct, finopsStrategy, spotSaving, spotPct, authTco, effectiveRps||baseRps);

    _renderUnitEconomicsGrid(unitEconGridElement, ueResp, authTco, egressCost, netInfra,
      numConsumers, consumerType, effectiveRps||baseRps, monthlyReqs, revenue);
  });
}

/* =======================================================================
   _renderTcoBreakdownTable — Phase 4 TCO cost breakdown table
   One row per cost component. Security services: one row per service.
   ======================================================================= */
function _renderTcoBreakdownTable(
  egressCost, albCost, cacheCost, dbCost,
  secCostByService, containerCost, apiGwCost, ec2Cost,
  finopsSaving, finopsPct, finopsStrategy, spotSaving, spotPct, authTco, effectiveRps
) {
  var container = document.getElementById('cloudTcoBreakdown');
  if (!container) return;

  var rows = [];
  var sumPositive = 0;
  function addRow(label, cat, badge, cost, detail, isSaving, isInfo) {
    rows.push({label:label, cat:cat, badge:badge||'badge-warn', cost:cost,
               detail:detail||'', isSaving:!!isSaving, isInfo:!!isInfo});
    if (!isSaving && !isInfo && cost > 0) sumPositive += cost;
  }

  // Show tactic networking overhead (TLS/retry byte cost) separately from base egress
  var _tacticOverhead = parseFloat(sessionStorage.getItem('tco_tactic_overhead')||'0');
  var _baseEgress = parseFloat(sessionStorage.getItem('tco_phase3_base_egress')||'0');
  // Use stored effectiveRps so the label is correct even when called with no args
  var _storedEffRps = parseInt(sessionStorage.getItem('tco_effective_rps')||'0');
  var _effRpsLabel = effectiveRps || _storedEffRps || baseRps || 0;
  addRow('AWS Egress (Response Transfer)', 'Networking', 'badge-bytes', egressCost,
    _effRpsLabel + ' eff. RPS · AWS data-out tiers'
    + (_tacticOverhead > 0.01 && _baseEgress > 0
      ? ' (base $' + _baseEgress.toFixed(2) + ' + $' + _tacticOverhead.toFixed(2) + ' tactic overhead incl. retry+TLS)'
      : ''));

  var isTls  = !!(document.getElementById('tactic-tls')  && document.getElementById('tactic-tls').checked);
  var isMtls = !!(document.getElementById('tactic-mtls') && document.getElementById('tactic-mtls').checked);
  var isOauth= !!(document.getElementById('tactic-oauth') && document.getElementById('tactic-oauth').checked);
  // TLS/mTLS and JWT overhead are included in the egress total.
  // Show the actual overhead cost as informational with real amount.
  // Show them as informational rows with the actual overhead amount for clarity.
  if ((isTls||isMtls) && _tacticOverhead > 0) {
    var _tlsCost = _tacticOverhead > 0 ? Math.round(_tacticOverhead * 100) / 100 : 0;
    var _tlsDetail = _baseEgress > 0
      ? 'Base egress (no tactics): $' + _baseEgress.toFixed(2) + '/mo. '
        + 'Tactic overhead: $' + _tacticOverhead.toFixed(2) + '/mo (retry extra RPS + '
        + (isMtls ? 'mTLS 30 B/frame RFC 8446' : 'TLS 30 B/frame RFC 8446') + '). '
        + 'Total egress: $' + egressCost.toFixed(2) + '/mo. Overhead included in AWS Egress row.'
      : 'Included in AWS Egress row above.';
    rows.push({ label: (isMtls?'mTLS (Mutual TLS)':'TLS') + ' frame overhead (RFC 8446 § 5.2 — 30 B/frame)',
      cat: 'Security', badge: 'badge-dr', cost: 0,
      detail: _tlsDetail, isSaving: false, isInfo: true,
      infoLabel: 'incl. in egress'
    });
  }
  if (isOauth) {
    rows.push({ label: 'JWT header overhead (RFC 7519 — inbound, free)',
      cat: 'Security', badge: 'badge-dr', cost: 0,
      detail: 'AWS inbound data transfer is free. JWT headers add bytes to requests only.',
      isSaving: false, isInfo: true, infoLabel: '$0 (inbound free)'
    });
  }

  if (albCost > 0)       addRow('Application Load Balancer (ALB)', 'Cloud Infra', 'badge-warn', albCost, '$'+albCost.toFixed(2)+'/mo · /api/cloud/alb-pricing');
  if (cacheCost > 0)     addRow('Amazon ElastiCache', 'Cloud Infra', 'badge-warn', cacheCost, '$'+cacheCost.toFixed(2)+'/mo · /api/cloud/caching-pricing');
  if (dbCost > 0)        addRow('Database / Backup / DR', 'Cloud Infra', 'badge-warn', dbCost, '$'+dbCost.toFixed(2)+'/mo · /api/cloud/database-backup-pricing');

  var secLabels = {
    'sec-guardduty':'Amazon GuardDuty', 'sec-inspector':'Amazon Inspector',
    'sec-waf':'AWS WAF', 'sec-macie':'Amazon Macie', 'sec-cloudwatch':'CloudWatch Logs',
    'sec-audit':'AWS Audit Manager', 'sec-kms':'AWS KMS', 'sec-cloudtrail':'AWS CloudTrail',
    'sec-acm':'AWS ACM'
  };
  Object.keys(secCostByService).forEach(function(id){
    var c = secCostByService[id];
    if (c > 0) addRow(secLabels[id]||id, 'Security', 'badge-dr', c,
      '$'+c.toFixed(2)+'/mo · /api/cloud/security-services');
  });

  if (containerCost > 0) addRow('Containerized Cluster (EKS)', 'Cloud Infra', 'badge-warn', containerCost, '$'+containerCost.toFixed(2)+'/mo');
  if (apiGwCost > 0)     addRow('API Gateway', 'Cloud Infra', 'badge-warn', apiGwCost, '$'+apiGwCost.toFixed(2)+'/mo · /api/cloud/api-gateway-pricing');
  if (ec2Cost > 0)       addRow('EC2 Compute Replicas', 'Cloud Infra', 'badge-warn', ec2Cost, '$'+ec2Cost.toFixed(2)+'/mo');
  if (spotSaving > 0)    addRow('EC2 Spot Instances (~' + (spotPct||0) + '% discount)', 'Cost Reduction', 'badge-bc',
    -spotSaving, 'AWS EC2 Spot Advisor — up to 90% vs on-demand · aws.amazon.com/ec2/spot/instance-advisor', true);
  if (finopsSaving > 0)  {
     addRow((finopsStrategy || 'FinOps Optimisation') + (finopsPct > 0 ? ' (' + finopsPct.toFixed(1) + '% off)' : ''), 'Cost Reduction', 'badge-bc',-finopsSaving, 'Discount applied to compute spend · /api/finops/ri-prices', true);
     sumPositive -= finopsSaving;
  }

  // ROI Impact % = how much each row moves the ROI needle numerically.
  // For cost rows: roi% = -(row_cost / authTco * 100) — spending this reduces margin.
  // For saving rows: roi% = +(saving / authTco * 100) — saving this improves margin.
  // Revenue used to compute ROI: if not available, we express impact vs TCO only.
  var _revMonth = parseFloat(sessionStorage.getItem('svc_revenue_per_tx')||'0')
                * (parseInt(sessionStorage.getItem('svc_consumers')||'1')||1);

  function roiImpactPct(rowCost, isSaving, isInfo, totalRevenue, authTco) {
    if (isInfo) return null;
    if (authTco <= 0) return null;

    // 1. Calculate the current baseline ROI
    // Formula: currentRoi = (totalRevenue - authTco) / authTco
    var currentProfit = totalRevenue - authTco;
    var currentRoi = currentProfit / authTco;

    // 2. Simulate TCO if this specific item is removed (What-if analysis)
    // Formula:
    // - If it's a cost, removing it decreases TCO: newTco = authTco - |rowCost|
    // - If it's a saving, removing it increases TCO: newTco = authTco + |rowCost|
    var newTco = isSaving ? (authTco + Math.abs(rowCost)) : (authTco - Math.abs(rowCost));

    if (newTco <= 0) return null; // Prevent division by zero

    // 3. Calculate the new profit and new ROI without this item
    // Formula: newRoi = (totalRevenue - newTco) / newTco
    var newProfit = totalRevenue - newTco;
    var newRoi = newProfit / newTco;

    // 4. Return the difference in percentage points
    // Formula: roiDelta = (newRoi - currentRoi) * 100
    var roiDelta = (newRoi - currentRoi) * 100;

    return roiDelta;
  }

  var rowsHtml = rows.map(function(c){
    var pct = (sumPositive > 0 && c.cost>0 && !c.isSaving && !c.isInfo)
      ? (c.cost/sumPositive*100).toFixed(1)+'%'
      : (c.isSaving ? '<span style="color:var(--green);">saving</span>'
        : (c.isInfo ? '<span style="color:var(--green);font-size:.73rem;">free</span>' : '—'));
    var costCell = c.isSaving
      ? '<span style="font-family:monospace;color:var(--green);font-weight:700;">-$'+Math.abs(c.cost).toFixed(2)+' saved</span>'
      : c.isInfo
        ? '<span style="color:var(--green);font-size:.73rem;font-style:italic;">$0 (free)</span>'
        : (infoCostCell || '<span style="font-family:monospace;color:var(--blue-deep);">$'+c.cost.toFixed(2)+'</span>');
    var detailHtml = c.detail ? '<div style="font-size:.72rem;color:var(--ink-light);margin-top:2px;">'+c.detail+'</div>' : '';
    var roiPct = roiImpactPct(c.cost, c.isSaving, c.isInfo);
    var roiCell;
    if (roiPct === null) {
      roiCell = '<span style="font-size:.7rem;color:var(--ink-light);">—</span>';
    } else if (roiPct >= 0) {
      roiCell = '<span style="color:var(--green);font-size:.82rem;font-weight:700;font-family:monospace;">+'+roiPct.toFixed(1)+'%</span>';
    } else {
      roiCell = '<span style="color:var(--red);font-size:.82rem;font-weight:700;font-family:monospace;">'+roiPct.toFixed(1)+'%</span>';
    }
    // Info rows: show infoLabel or 'incl. in egress' instead of $0 (free)
    var infoCostCell = (c.isInfo && c.infoLabel)
      ? '<span style="font-size:.78rem;color:var(--ink-light);font-style:italic;">'+c.infoLabel+'</span>'
      : null;
    return '<tr style="border-bottom:1px solid var(--rule);">'
      +'<td style="padding:8px 12px;font-weight:600;">'+c.label+detailHtml+'</td>'
      +'<td style="padding:8px 12px;text-align:center;"><span class="warning-badge '+c.badge+'">'+c.cat+'</span></td>'
      +'<td style="padding:8px 12px;text-align:right;">'+costCell+'</td>'
      +'<td style="padding:8px 12px;text-align:right;">'+pct+'</td>'
      +'<td style="padding:8px 12px;text-align:right;">'+roiCell+'</td></tr>';
  }).join('');
  rowsHtml += '<tr style="background:var(--blue-deep);color:#fff;font-weight:700;">'
    +'<td style="padding:9px 12px;" colspan="2">Total Cost of Ownership (TCO)</td>'
    +'<td style="padding:9px 12px;text-align:right;font-family:monospace;">$'+authTco.toFixed(2)+'/mo</td>'
    +'<td style="padding:9px 12px;text-align:right;">100.0%</td>'
    +'<td style="padding:9px 12px;text-align:right;font-size:.74rem;">ROI Impact %</td></tr>';
  container.innerHTML =
    '<div style="font-family:\'DM Serif Display\',serif;font-size:1rem;color:var(--blue-deep);margin-bottom:10px;display:flex;align-items:center;gap:8px;">'
    +'<i class="fas fa-table"></i> Total Cost of Ownership (TCO) Breakdown</div>'
    +'<table style="width:100%;border-collapse:collapse;font-size:.84rem;margin-bottom:4px;">'
    +'<thead><tr style="background:var(--paper);">'
    +'<th style="padding:7px 12px;text-align:left;border-bottom:1px solid var(--rule);">Cost Component</th>'
    +'<th style="padding:7px 12px;text-align:center;border-bottom:1px solid var(--rule);">Category</th>'
    +'<th style="padding:7px 12px;text-align:right;border-bottom:1px solid var(--rule);">Monthly Cost</th>'
    +'<th style="padding:7px 12px;text-align:right;border-bottom:1px solid var(--rule);">% of TCO</th>'
    +'<th style="padding:7px 12px;text-align:center;border-bottom:1px solid var(--rule);">ROI Impact</th>'
    +'</tr></thead><tbody>'+rowsHtml+'</tbody></table>'
    +'<p style="font-size:.73rem;color:var(--ink-light);margin-top:6px;">'
    +'&#9650; Promotes ROI = positive efficiency factor. &#9660; Inhibits ROI = margin cost (hover for tip). '
    +'Costs via AWS Pricing API. DR = Disaster Recovery | BC = Business Continuity.</p>';
}

/* =======================================================================
   _renderUnitEconomicsGrid — cost/user, cost/req, annual projection + ROI
   ======================================================================= */
function _renderUnitEconomicsGrid(gridEl, ueResp, tcoMonthly, egressCost, infraCost,
                                   numConsumers, consumerType, rps, monthlyReqs, revenue) {
  if (!gridEl) return;
  var smallFmt = function(v,p){ return (v>0&&v<Math.pow(10,-(p-1)))?v.toExponential(4):v.toFixed(p); };
  var consumerLabels = {SERVICES:'Microservices / APIs',USERS:'End Users',BOTH:'Mixed'};
  var cLabel = consumerLabels[consumerType]||'consumers';
  var bucLabel = window.selectedBUC
    ? (window.selectedBUC.indexOf('CUSTOM:')===0?window.selectedBUC.replace('CUSTOM:',''):window.selectedBUC)
    : '\u2014';

  var costPerUser    = (ueResp && ueResp.costPerUserPerMonthUsd) ? ueResp.costPerUserPerMonthUsd : tcoMonthly/numConsumers;
  var costPerReq     = (ueResp && ueResp.costPerRequestUsd)      ? ueResp.costPerRequestUsd      : (monthlyReqs > 0 ? tcoMonthly/monthlyReqs : 0);
  var costPerUserDay = (ueResp && ueResp.costPerUserPerDayUsd)   ? ueResp.costPerUserPerDayUsd   : costPerUser/30;

  gridEl.innerHTML =
    '<div class="unit-econ-item" style="grid-column:1/-1;background:linear-gradient(135deg,#eff6ff,#dbeafe);border:1px solid #93c5fd;">'
    +'<div class="unit-econ-label">Total Monthly TCO (Networking + Cloud Infra)</div>'
    +'<div class="unit-econ-value" style="color:var(--blue-deep);">$'+tcoMonthly.toFixed(2)+'</div>'
    +'<div class="unit-econ-sub">'
    +(infraCost>0?'$'+egressCost.toFixed(2)+' egress + $'+infraCost.toFixed(2)+' cloud infra':'$'+egressCost.toFixed(2)+' egress networking only')
    +'</div></div>'

    +'<div class="unit-econ-item" style="grid-column:1/-1;background:var(--paper);border:none;padding:10px 0 4px;">'
    +'<div style="font-family:\'DM Serif Display\',serif;font-size:.9rem;color:var(--blue-deep);border-bottom:1px solid var(--rule);padding-bottom:6px;">'
    +'<i class="fas fa-microchip" style="font-size:.85rem;margin-right:6px;"></i> Per-Microservice Cost Metrics</div></div>'
    +'<div class="unit-econ-item"><div class="unit-econ-label">TCO / Microservice / Month</div>'
    +'<div class="unit-econ-value">$'+tcoMonthly.toFixed(2)+'</div><div class="unit-econ-sub">This service \u00b7 full TCO</div></div>'
    +'<div class="unit-econ-item"><div class="unit-econ-label">Cost / Request</div>'
    +'<div class="unit-econ-value">$'+smallFmt(costPerReq,8)+'</div><div class="unit-econ-sub">'+monthlyReqs.toLocaleString()+' req/mo</div></div>'
    +'<div class="unit-econ-item"><div class="unit-econ-label">Annual Projection</div>'
    +'<div class="unit-econ-value">$'+(tcoMonthly*12).toFixed(2)+'</div><div class="unit-econ-sub">12\u00d7 monthly TCO</div></div>'

    +'<div class="unit-econ-item" style="grid-column:1/-1;background:var(--paper);border:none;padding:10px 0 4px;">'
    +'<div style="font-family:\'DM Serif Display\',serif;font-size:.9rem;color:var(--blue-deep);border-bottom:1px solid var(--rule);padding-bottom:6px;">'
    +'<i class="fas fa-users" style="font-size:.85rem;margin-right:6px;"></i> Per-End-User Cost Metrics'
    +' <span style="font-size:.72rem;font-weight:400;color:var(--ink-light);">('+numConsumers.toLocaleString()+' '+cLabel+')</span></div></div>'
    +'<div class="unit-econ-item"><div class="unit-econ-label">TCO / End User / Month</div>'
    +'<div class="unit-econ-value">$'+smallFmt(costPerUser,4)+'</div>'
    +'<div class="unit-econ-sub">$'+tcoMonthly.toFixed(2)+' \u00f7 '+numConsumers.toLocaleString()+' users</div></div>'
    +'<div class="unit-econ-item"><div class="unit-econ-label">TCO / End User / Day</div>'
    +'<div class="unit-econ-value">$'+smallFmt(costPerUserDay,6)+'</div><div class="unit-econ-sub">Monthly \u00f7 30</div></div>'
    +'<div class="unit-econ-item"><div class="unit-econ-label">Business Use Case</div>'
    +'<div class="unit-econ-value" style="font-size:.85rem;">'+bucLabel+'</div>'
    +'<div class="unit-econ-sub">Pattern cost attribution</div></div>';

  window._lastComputedTco = tcoMonthly;

  /* ── Financial Return Matrix (ROI & ARPU Profiling) ── */
  var revenueInputModelElement = document.getElementById('revenuePerTransaction');
  var expectedRevenuePerUserElement = (revenueInputModelElement && revenueInputModelElement.value) ? (parseFloat(revenueInputModelElement.value) || 0)
    : (parseFloat(sessionStorage.getItem('svc_revenue_per_tx')) || 0);

  var roiSectionContainerElement = document.getElementById('ue-roi-section');
  if (!roiSectionContainerElement) {
    roiSectionContainerElement = document.createElement('div');
    roiSectionContainerElement.id = 'ue-roi-section';
    roiSectionContainerElement.style.cssText = 'margin-top:16px;padding:16px 20px;border-radius:var(--r);'
      + 'background:linear-gradient(135deg,#fffbeb,#fef3c7);border:1px solid var(--amber-border);';
    if (gridEl && gridEl.parentElement) {
      gridEl.parentElement.appendChild(roiSectionContainerElement);
    }
  }

  var analyticalGuideMessageHtml = '<div style="font-size:.79rem;color:var(--ink-medium);margin-bottom:12px;line-height:1.55;'
    + 'padding:10px 12px;background:rgba(255,255,255,.6);border-radius:var(--r);border:1px solid rgba(212,160,23,.2);">'
    + '<strong>Revenue model:</strong> The value entered in Phase\u00a01 is the '
    + '<em>expected monthly revenue per end user</em> for this service. '
    + 'Total monthly revenue\u00a0= that value\u00a0\u00d7\u00a0number of end users. '
    + '<strong>ARPU</strong>\u00a0= revenue per user per month. '
    + '<strong>ROI</strong>\u00a0= (total revenue\u00a0\u2212\u00a0TCO) \u00f7 TCO. '
    + '<strong>Break-even users</strong>\u00a0= minimum end users needed to cover full TCO at current ARPU. '
    + 'Net Margin per user = ARPU\u00a0\u2212\u00a0TCO per user. '
    + 'Present to Finance and Leadership to frame architecture cost as a revenue-generating investment.'
    + '</div>';

  if (expectedRevenuePerUserElement > 0) {
    var generatedMonthlyRevenue = expectedRevenuePerUserElement * numConsumers;
    var arpuValue = expectedRevenuePerUserElement;
    var monthlyRoiValue = tcoMonthly > 0 ? ((generatedMonthlyRevenue - tcoMonthly) / tcoMonthly * 100).toFixed(1) : '--';
    var annualRoiValue = tcoMonthly > 0 ? ((generatedMonthlyRevenue * 12 - tcoMonthly * 12) / (tcoMonthly * 12) * 100).toFixed(1) : '--';

    var breakEvenUsersVolume = (expectedRevenuePerUserElement > 0 && tcoMonthly > 0) ? Math.ceil(tcoMonthly / expectedRevenuePerUserElement).toLocaleString() : '--';
    var infrastructureEfficiencyRatio = (tcoMonthly > 0) ? (generatedMonthlyRevenue / tcoMonthly).toFixed(2) : '--';
    var netMarginPerUserMonthly = expectedRevenuePerUserElement - costPerUser;

    var roiColorTheme = (parseFloat(monthlyRoiValue) >= 0) ? 'var(--green)' : 'var(--red)';
    var marginColorTheme = netMarginPerUserMonthly >= 0 ? 'var(--green)' : 'var(--red)';

    roiSectionContainerElement.innerHTML =
      '<div style="font-family:\'DM Serif Display\',serif;font-size:1rem;color:var(--amber);'
      + 'margin-bottom:10px;display:flex;align-items:center;gap:8px;">'
      + '<i class="fas fa-chart-line"></i> Revenue, ARPU &amp; ROI Analysis</div>'
      + analyticalGuideMessageHtml
      + '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;">'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Total Monthly Revenue</div>'
      + '<div class="unit-econ-value">$' + generatedMonthlyRevenue.toLocaleString(undefined, { maximumFractionDigits: 2 }) + '</div>'
      + '<div class="unit-econ-sub">$' + expectedRevenuePerUserElement.toFixed(2) + '/user \u00d7 ' + numConsumers.toLocaleString() + ' users</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">ARPU / Month</div>'
      + '<div class="unit-econ-value">$' + arpuValue.toLocaleString(undefined, { maximumFractionDigits: 2 }) + '</div>'
      + '<div class="unit-econ-sub">Revenue per end user / month</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Net Margin / User / mo</div>'
      + '<div class="unit-econ-value" style="color:' + marginColorTheme + ';">$' + netMarginPerUserMonthly.toFixed(4) + '</div>'
      + '<div class="unit-econ-sub">ARPU \u2212 TCO/user</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Monthly ROI</div>'
      + '<div class="unit-econ-value" style="color:' + roiColorTheme + ';">' + monthlyRoiValue + '%</div>'
      + '<div class="unit-econ-sub">(Revenue \u2212 TCO) \u00f7 TCO</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Annual ROI</div>'
      + '<div class="unit-econ-value" style="color:' + roiColorTheme + ';">' + annualRoiValue + '%</div>'
      + '<div class="unit-econ-sub">Annualized (Monthly Run-Rate)</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Break-even Users</div>'
      + '<div class="unit-econ-value">' + breakEvenUsersVolume + '</div>'
      + '<div class="unit-econ-sub">users to cover full TCO</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Revenue / $1 Infra</div>'
      + '<div class="unit-econ-value">$' + infrastructureEfficiencyRatio + '</div>'
      + '<div class="unit-econ-sub">Revenue efficiency</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Annual Revenue</div>'
      + '<div class="unit-econ-value">$' + (generatedMonthlyRevenue * 12).toLocaleString(undefined, { maximumFractionDigits: 2 }) + '</div>'
      + '<div class="unit-econ-sub">' + 12 + '\u00d7 monthly revenue</div></div>'
      + '</div>';
  } else {
    roiSectionContainerElement.innerHTML =
      '<div style="font-family:\'DM Serif Display\',serif;font-size:.95rem;color:var(--amber);'
      + 'margin-bottom:8px;"><i class="fas fa-chart-line" style="margin-right:6px;"></i>'
      + 'Revenue, ARPU &amp; ROI Analysis</div>'
      + analyticalGuideMessageHtml
      + '<div style="font-size:.79rem;color:var(--ink-light);">'
      + '<i class="fas fa-circle-info" style="margin-right:5px;color:var(--gold);"></i>'
      + 'Enter <strong>Expected Revenue per Transaction</strong> in Phase\u00a01 to unlock '
      + 'ROI, ARPU, break-even and revenue efficiency analysis.</div>';
  }
}