/* =====================================================================
       STATE  — all at global scope so HTML onclick handlers can reach them
    ===================================================================== */
var currentPhase = 1;
var selectedBUC = null;
var baseRpsForComparison = 0;
var _saveTimer = null;
var _compDebounce = null;
var _awsLoaded = {};
var _albData = null, _dbData = null, _secData = null, _coData = null, _cacheData = null;

/* AWS egress tiers — parallel arrays, safe for Thymeleaf templates */
var TIER_GB = [10240, 40960, 102400];
var TIER_RATES = [0.09, 0.085, 0.07];
var TIER_TAIL = 0.05;
var PLACEHOLDER_REQ_BYTES = 200;
var PLACEHOLDER_RESP_BYTES = 1200;

/* =====================================================================
   REPLICA SIZING
===================================================================== */
function recalculateReplicas() {
  var _cReqEl = document.getElementById('input-max-req-per-replica');
  var _bRpsV = parseInt((document.getElementById('requestsPerSecond') || { value: '0' }).value) || 0;
  if (_cReqEl && _bRpsV > 0) {
    if (!_cReqEl.value) _cReqEl.placeholder = 'from RPS: ' + _bRpsV;
    /* Also update the default value once per RPS change if field hasn't been manually set */
    if (!_cReqEl.dataset.userSet && !_cReqEl.value) _cReqEl.value = _bRpsV;
  }
  var ri = document.getElementById('replica-result');
  var gi = document.getElementById('replica-grid');
  var ci = document.getElementById('replica-count-display');
  var bi = document.getElementById('replica-bottleneck');
  var sel = document.getElementById('input-ec2-instance');
  var ov = parseInt((document.getElementById('input-max-req-per-replica') || { value: '' }).value) || 0;
  if (!sel || !sel.value) { if (ri) ri.style.display = 'none'; return; }
  var p = sel.value.split('|');
  var cReq = ov > 0 ? ov : parseInt(p[1]);
  var cIn = parseInt(p[2]);
  var cOut = parseInt(p[3]);
  var R = parseInt((document.getElementById('requestsPerSecond') || { value: '' }).value) || 0;
  var sReq = window._lastProtoReqBytes || PLACEHOLDER_REQ_BYTES;
  var sRes = window._lastProtoRespBytes || PLACEHOLDER_RESP_BYTES;
  if (!R || !cReq) { if (ri) ri.style.display = 'none'; return; }
  var nReq = Math.ceil(R / cReq);
  var nIn = Math.ceil((R * sReq) / cIn);
  var nOut = Math.ceil((R * sRes) / cOut);
  var N = Math.max(nReq, nIn, nOut);
  var btl = (nReq >= nIn && nReq >= nOut) ? 'Request throughput (C_req)' :
    (nIn >= nOut) ? 'Network ingress (C_in)' : 'Network egress (C_out)';
  var dims = [
    { l: 'N from throughput', v: nReq, f: '\u2308R\u00f7C_req\u2309 = ' + nReq },
    { l: 'N from net ingress', v: nIn, f: '\u2308(R\u00d7S_req)\u00f7C_in\u2309 = ' + nIn },
    { l: 'N from net egress', v: nOut, f: '\u2308(R\u00d7S_res)\u00f7C_out\u2309 = ' + nOut }
  ];
  if (gi) gi.innerHTML = dims.map(function (d) {
    return '<div style="background:rgba(255,255,255,.6);border:1px solid rgba(212,160,23,.25);border-radius:var(--r);padding:10px;">'
      + '<div style="font-size:.68rem;font-weight:700;text-transform:uppercase;color:var(--ink-light);margin-bottom:4px;">' + d.l + '</div>'
      + '<div style="font-family:monospace;font-size:1rem;font-weight:700;color:' + (d.v === N ? 'var(--red)' : 'var(--blue-deep)') + ';">' + d.v + '</div>'
      + '<div style="font-size:.7rem;color:var(--ink-light);margin-top:2px;">' + d.f + '</div>'
      + '</div>';
  }).join('');
  if (ci) ci.textContent = N + ' replica' + (N !== 1 ? 's' : '');
  if (bi) bi.textContent = 'Bottleneck: ' + btl;
  if (ri) ri.style.display = 'block';
}

/* =====================================================================
   COST MATH
===================================================================== */

function calcMonthlyCost(rps, respBytes) {
  var gbPerMonth = (rps * 60 * 60 * 24 * 30 * respBytes) / (1024 * 1024 * 1024);
  var cost = 0, rem = gbPerMonth;
  for (var i = 0; i < TIER_GB.length; i++) {
    if (rem <= 0) break;
    var used = Math.min(rem, TIER_GB[i]);
    cost += used * TIER_RATES[i];
    rem -= used;
  }
  if (rem > 0) cost += rem * TIER_TAIL;
  return { gbPerMonth: gbPerMonth, cost: cost };
}

/* =====================================================================
   RPS CALCULATION + LIVE COMPARISON
===================================================================== */
function recalculateRps() {
  var baseRps = parseInt((document.getElementById('requestsPerSecond') || { value: '' }).value) || 0;
  var sumEl   = document.getElementById('rps-impact-summary');
  var sagaPrv = document.getElementById('saga-live-preview');
  var oaPrv   = document.getElementById('oauth-live-preview');
  var cmpBlk  = document.getElementById('tactics-comparison-block');

  if (!baseRps) {
    if (sumEl)   sumEl.classList.remove('visible');
    if (sagaPrv) sagaPrv.classList.remove('visible');
    if (oaPrv)   oaPrv.classList.remove('visible');
    if (cmpBlk)  cmpBlk.classList.remove('visible');
    return;
  }

  baseRpsForComparison = baseRps;

  var payload = {
    baseRps:              baseRps,
    sagaEnabled:          !!(document.getElementById('tactic-saga') && document.getElementById('tactic-saga').checked),
    sagaExternalVpc:      !!(document.getElementById('tactic-saga-external-vpc') && document.getElementById('tactic-saga-external-vpc').checked),
    sagaCompensatable:    parseInt((document.getElementById('input-saga-compensatable') || { value: '0' }).value) || 0,
    sagaRetriable:        parseInt((document.getElementById('input-saga-retriable')     || { value: '0' }).value) || 0,
    sagaPivot:            parseInt((document.getElementById('input-saga-pivot')         || { value: '0' }).value) || 0,
    retryEnabled:         !!(document.getElementById('tactic-retry') && document.getElementById('tactic-retry').checked),
    retryErrorPct:        parseFloat((document.getElementById('input-retry-error-pct')  || { value: '5' }).value) || 5,
    tlsEnabled:           !!(document.getElementById('tactic-tls')  && document.getElementById('tactic-tls').checked),
    mtlsEnabled:          !!(document.getElementById('tactic-mtls') && document.getElementById('tactic-mtls').checked),
    tlsReconnectsPerHour: parseInt((document.getElementById('input-tls-reconnects')     || { value: '0' }).value) || 0,
    oauthEnabled:         !!(document.getElementById('tactic-oauth') && document.getElementById('tactic-oauth').checked),
    tokenValidationMode:  (document.getElementById('input-token-validation') || { value: 'LOCAL' }).value || 'LOCAL',
    tokenTtlSeconds:      parseInt((document.getElementById('input-token-ttl')          || { value: '3600' }).value) || 3600,
    concurrentClients:    parseInt((document.getElementById('input-concurrent-clients') || { value: '1' }).value) || 1
  };

  fetch('/api/tco/effective-rps', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  .then(function(r) { return r.ok ? r.json() : null; })
  .then(function(d) {
    if (!d) return;

    window._lastEffectiveRps = d.effectiveRps;

    // SAGA preview
    if (d.sagaPreview && payload.sagaEnabled) {
      if (sagaPrv) {
        sagaPrv.innerHTML = '<i class="fas fa-calculator" style="margin-right:6px;"></i><strong>' + d.sagaPreview + '</strong>';
        sagaPrv.classList.add('visible');
      }
    } else { if (sagaPrv) sagaPrv.classList.remove('visible'); }

    // Retry preview
    if (d.retryExtra > 0) {
      var prev = document.getElementById('retry-extra-rps-preview');
      if (prev) prev.textContent = '+' + d.retryExtra.toLocaleString()
        + ' req/s = ' + payload.retryErrorPct + '% of ' + baseRps.toLocaleString() + ' base RPS';
    }

    // OAuth preview
    if (d.oauthPreview && payload.oauthEnabled) {
      if (oaPrv) {
        oaPrv.innerHTML = '<i class="fas fa-calculator" style="margin-right:6px;"></i><strong>' + d.oauthPreview + '</strong>';
        oaPrv.classList.add('visible');
      }
    } else { if (oaPrv) oaPrv.classList.remove('visible'); }

    // RPS summary bar
    if (d.rpsWasAdjusted) {
      var dispEl = document.getElementById('rps-adjusted-display');
      var brkEl  = document.getElementById('rps-breakdown-text');
      if (dispEl) dispEl.textContent = d.effectiveRps.toLocaleString() + ' req/s';
      if (brkEl)  brkEl.textContent  = 'Base: ' + baseRps.toLocaleString() + '  |  ' + d.breakdown.join('  |  ');
      if (sumEl)  sumEl.classList.add('visible');
    } else { if (sumEl) sumEl.classList.remove('visible'); }

    updateLiveComparison(baseRps, d.effectiveRps);
  })
  .catch(function(e) {
    console.warn('recalculateRps backend call failed:', e.message);
    updateLiveComparison(baseRps, baseRps);
  });
}


function hasAnyImpactingTactic() {
  var allIds = ['tactic-retry', 'tactic-tls', 'tactic-mtls', 'tactic-oauth',
    'tactic-alb', 'tactic-cache', 'tactic-s3-backup', 'tactic-aurora-replica', 'tactic-apigw',
    'cef-clusters', 'cef-cluster-lb', 'cef-host-storage', 'cef-workload-license'];
  for (var i = 0; i < allIds.length; i++) { var el = document.getElementById(allIds[i]); if (el && el.checked) return true; }
  var sagaCb = document.getElementById('tactic-saga');
  if (sagaCb && sagaCb.checked) {
    var steps = (parseInt((document.getElementById('input-saga-compensatable') || { value: '0' }).value) || 0)
      + (parseInt((document.getElementById('input-saga-retriable') || { value: '0' }).value) || 0)
      + (parseInt((document.getElementById('input-saga-pivot') || { value: '0' }).value) || 0);
    if (steps > 0) return true;
  }
  var ssK = ['tco_alb_cost', 'tco_cache_cost', 'tco_db_cost', 'tco_sec_cost', 'tco_container_cost', 'tco_apigw_cost'];
  for (var k = 0; k < ssK.length; k++) { if (parseFloat(sessionStorage.getItem(ssK[k]) || '0') > 0) return true; }
  return false;
}

function updateLiveComparison(baseRps, effectiveRps) {
  var comparisonBlock = document.getElementById('tactics-comparison-block');
  if (!comparisonBlock) return;

  if (!baseRps || (!window._lastProtoFile && !hasAnyImpactingTactic() && (window._lastCloudInfraCost || 0) === 0)) {
    comparisonBlock.classList.remove('visible');
    return;
  }

  comparisonBlock.classList.add('visible');

  if (window._lastProtoFile) {
    setComparisonLoading(true);
    clearTimeout(_compDebounce);
    _compDebounce = setTimeout(function() {
      saveTacticsToSession()
        .then(function() {
          return Promise.all([
            fetchBackendCost(window._lastProtoFile, true),
            fetchBackendCost(window._lastProtoFile, false)
          ]);
        })
        .then(function(results) {
          var baseResult    = results[0];
          var tacticsResult = results[1];
          if (!baseResult || !tacticsResult) return;
          // collectTacticContributions now returns a Promise
          return collectTacticContributions(baseRps, baseResult)
            .then(function(contributions) {
              renderComparisonFromBackend(
                baseResult, tacticsResult, baseRps, effectiveRps, false, contributions
              );
            });
        })
        .catch(function(err) {
          console.warn('Backend comparison failed:', err.message);
          renderComparisonEstimate(baseRps, effectiveRps);
        })
        .then(function() {
          setComparisonLoading(false);
        });
    }, 600);
  } else {
    renderComparisonEstimateWithBytes(baseRps, effectiveRps);
  }
}


function setComparisonLoading(on) {
  var block = document.getElementById('tactics-comparison-block');
  if (!block) return;
  var sp = block.querySelector('.cmp-spinner');
  if (sp) sp.style.display = on ? 'inline-block' : 'none';
}

/* =====================================================================
   BACKEND COST FETCH + PARSE
===================================================================== */
function fetchBackendCost(file, useBase) {
  var baseRps = parseInt((document.getElementById('requestsPerSecond') || { value: '0' }).value) || 0;
  var p1 = useBase ? fetch('/api/session/tactics', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(buildBaseTacticsDTO(baseRps))
  }) : Promise.resolve(null);
  return p1.then(function () {
    var fd = new FormData();
    fd.append('protoFile', file, file.name || 'upload.proto');
    return fetch('/calculateTCO', { method: 'POST', body: fd });
  }).then(function (resp) {
    if (!resp.ok) return null;
    return resp.text();
  }).then(function (html) {
    if (!html) return null;
    if (useBase) saveTacticsToSession();
    return parseBackendCostFromHtml(html);
  });
}

function buildBaseTacticsDTO(rps) {
  return {
    requestsPerSecond: rps,
    reliabilityTactics: { reliabilityClientSideLoadBalancerTactic: false, reliabilityServerSideLoadBalancerTactic: false },
    timeoutTactic: { resiliencyTimeoutTactic: false, tacticTimeoutMilliseconds: 0 },
    retryTactic: { resiliencyRetryTactic: false, tacticRetryTimes: 0 },
    circuitBreakerTactic: { resiliencyCircuitBreakerPattern: false, circuitBreakerPatternMinimumCalls: 0, circuitBreakerHalfOpen: 0, circuitBreakerWaitMilliseconds: 0, circuitBreakerFailureRate: 0 },
    sagaPattern: { microservicesSAGAPattern: false, sagaCompensatableTransactions: 0, sagaRetriableTransactions: 0, sagaPivotTransactions: 0 },
    securityTactics: {
      tlsTactic: { tlsEnabled: false, mtlsEnabled: false, tlsReconnectsPerHour: 0 },
      jwtTactic: { oauthJwtEnabled: false, tokenValidationMode: 'LOCAL', tokenTtlSeconds: 3600, concurrentClients: 1, interceptorType: 'UNARY' },
      basicAuthenticationPattern: { basicAuthEnabled: false }
    }
  };
}

function parseBackendCostFromHtml(html) {
  var parser = new DOMParser();
  var doc = parser.parseFromString(html, 'text/html');
  var rows = doc.querySelectorAll('.results-table tbody tr');
  var cost = null, respGb = null, reqSizeEff = null, respSizeEff = null, tlsOH = 0, jwtOH = 0;
  rows.forEach(function (row) {
    var cells = row.querySelectorAll('td');
    if (cells.length < 6) return;
    var type = cells[0].textContent.trim();
    var effB = parseInt((cells[2].textContent || '').replace(/[^0-9]/g, '')) || null;
    if (type === 'Request') reqSizeEff = effB;
    if (type === 'Response') {
      respSizeEff = effB;
      respGb = parseFloat((cells[4].textContent || '').replace(/[^0-9.,]/g, '').replace(',', '')) || null;
      cost = parseFloat((cells[5].textContent || '').replace(/[^0-9.]/g, '')) || null;
    }
  });
  doc.querySelectorAll('.overhead-item').forEach(function (item) {
    var lbl = ((item.querySelector('.overhead-item-label') || { textContent: '' }).textContent || '').toLowerCase();
    var val = parseInt(((item.querySelector('.overhead-item-value') || { textContent: '' }).textContent || '').replace(/[^0-9]/g, '')) || 0;
    if (lbl.indexOf('tls') >= 0) tlsOH = val;
    if (lbl.indexOf('jwt') >= 0) jwtOH = val;
  });
  var rpsNums = doc.querySelectorAll('.rps-val-number');
  var baseRpsP = null, effRpsP = null;
  if (rpsNums.length >= 2) {
    baseRpsP = parseInt((rpsNums[0].textContent || '').replace(/[^0-9]/g, '')) || null;
    effRpsP = parseInt((rpsNums[1].textContent || '').replace(/[^0-9]/g, '')) || null;
  }
  if (cost === null) return null;
  if (reqSizeEff != null) window._lastProtoReqBytes = reqSizeEff;
  if (respSizeEff != null) window._lastProtoRespBytes = respSizeEff;
  if (tlsOH > 0) sessionStorage.setItem('tco_tls_overhead', tlsOH);
  if (jwtOH > 0) sessionStorage.setItem('tco_jwt_overhead', jwtOH);
  return {
    cost: cost, respGb: respGb, baseRps: baseRpsP, effectiveRps: effRpsP,
    requestSizeEff: reqSizeEff, responseSizeEff: respSizeEff,
    tlsOverheadBytes: tlsOH, jwtOverheadBytes: jwtOH
  };
}

/* =====================================================================
   COMPARISON RENDER
===================================================================== */
function renderComparisonFromBackend(base, tactics, baseRps, effectiveRps, isEstimate, contributions) {
  var cachedCloudInfraCost  = window._lastCloudInfraCost || 0;
  var networkingCostDelta   = tactics.cost - base.cost;
  var tacticsTotalCost      = tactics.cost + cachedCloudInfraCost;
  var totalMonthlyCostDelta = tacticsTotalCost - base.cost;
  var el;

  el = document.getElementById('cmp-base-cost');
  if (el) el.textContent = '$' + base.cost.toFixed(2) + ' / mo';

  el = document.getElementById('cmp-base-rps');
  if (el) el.textContent = baseRps.toLocaleString() + ' RPS (networking only)';

  el = document.getElementById('cmp-tactics-cost');
  if (el) el.textContent = '$' + tacticsTotalCost.toFixed(2) + ' / mo';

  el = document.getElementById('cmp-tactics-rps');
  if (el) el.textContent = effectiveRps.toLocaleString() + ' eff. RPS'
    + (networkingCostDelta  > 0 ? ' (+$' + networkingCostDelta.toFixed(2)   + ' networking)'  : '')
    + (cachedCloudInfraCost > 0 ? ' · +$' + cachedCloudInfraCost.toFixed(2) + ' cloud infra' : '');

  var breakdownTableEl = document.getElementById('cmp-tactic-breakdown');
  if (breakdownTableEl) {
    // Networking tactic rows — cost impacts from TacticContributionController response
    var tacticRowsHtml = '';
    if (contributions && contributions.length) {
      contributions.forEach(function(contribution) {
        var costImpactHtml;
        if (contribution.kind === 'info') {
          costImpactHtml = '—';
        } else if (contribution.jwtOnRequestOnly && contribution.estimatedMonthlyCostUsd === 0) {
          costImpactHtml = '<span style="color:var(--ink-light);font-size:.74rem;">' + contribution.costDisplayLabel + '</span>';
        } else {
          costImpactHtml = contribution.costDisplayLabel || '—';
        }

        var kindBadge = '';
        if (contribution.kind === 'info')  kindBadge = '<span class="tbd-badge tbd-info">informational</span>';
        if (contribution.kind === 'rps')   kindBadge = '<span class="tbd-badge tbd-rps">+ RPS</span>';
        if (contribution.kind === 'bytes') kindBadge = '<span class="tbd-badge tbd-bytes">+ bytes</span>';
        if (contribution.kind === 'both')  kindBadge = '<span class="tbd-badge tbd-rps">+ RPS</span><span class="tbd-badge tbd-bytes">+ bytes</span>';

        var hasCostImpact = contribution.kind !== 'info' && costImpactHtml !== '—';
        tacticRowsHtml += '<tr>'
          + '<td class="tbd-name">' + contribution.label
          + (contribution.value  ? ' <span style="font-size:.73rem;color:var(--ink-light);font-weight:400;">' + contribution.value  + '</span>' : '')
          + (contribution.note   ? '<div class="tbd-note">'   + contribution.note   + '</div>' : '')
          + (contribution.detail ? '<div class="tbd-detail">' + contribution.detail + '</div>' : '')
          + '</td>'
          + '<td class="tbd-badges">' + kindBadge + '</td>'
          + '<td class="tbd-cost' + (hasCostImpact ? ' tbd-cost-impact' : '') + '">' + costImpactHtml + '</td>'
          + '</tr>';
      });
    }

    // Cloud infrastructure rows — read from sessionStorage (populated by /api/cost/* calls)
    var cloudServiceRowsHtml = buildCloudServiceBreakdownRows();

    if (cloudServiceRowsHtml) {
      cloudServiceRowsHtml += '<tr style="background:var(--paper);font-weight:700;border-top:2px solid var(--rule);">'
        + '<td class="tbd-name" colspan="2">Total Monthly Cost Increase (networking + cloud infra)</td>'
        + '<td class="tbd-cost tbd-cost-impact">+$' + totalMonthlyCostDelta.toFixed(2) + '/mo</td></tr>';
    }

    breakdownTableEl.innerHTML = '<table class="tbd-table">'
      + '<thead><tr><th>Tactic</th><th>Type</th><th>Est. Cost Impact</th></tr></thead>'
      + '<tbody>' + tacticRowsHtml + cloudServiceRowsHtml + '</tbody></table>';
    breakdownTableEl.style.display = 'block';
  }

  var sourceNoteEl = document.getElementById('cmp-source-note');
  if (sourceNoteEl) sourceNoteEl.textContent = isEstimate
    ? '* Estimated using placeholder sizes (200 B req / 1,200 B resp). Upload your .proto for exact figures.'
    : '* Calculated using your .proto file. ℹ SAGA multiplies inter-service calls. AWS charges $0.00/GB same-AZ, $0.01/GB cross-AZ, $0.09+/GB to internet.';

  var deltaRowEl   = document.getElementById('cmp-delta-row');
  var deltaValueEl = document.getElementById('cmp-delta-val');
  var deltaBreakEl = document.getElementById('cmp-delta-breakdown');

  if (Math.abs(totalMonthlyCostDelta) < 0.01) {
    if (deltaRowEl)   deltaRowEl.className     = 'comparison-delta neutral';
    if (deltaValueEl) deltaValueEl.textContent = 'No change';
    if (deltaBreakEl) deltaBreakEl.textContent = '';
  } else {
    if (deltaRowEl)   deltaRowEl.className     = totalMonthlyCostDelta > 0 ? 'comparison-delta' : 'comparison-delta neutral';
    if (deltaValueEl) deltaValueEl.textContent = (totalMonthlyCostDelta > 0 ? '+' : '') + ' $' + totalMonthlyCostDelta.toFixed(2) + ' / mo';
    if (deltaBreakEl) {
      var parts = [];
      if (Math.abs(networkingCostDelta)  >= 0.01) parts.push('networking: '  + (networkingCostDelta  > 0 ? '+' : '') + '$' + networkingCostDelta.toFixed(2));
      if (cachedCloudInfraCost >= 0.01)            parts.push('cloud infra: +$' + cachedCloudInfraCost.toFixed(2));
      deltaBreakEl.textContent = parts.join(' · ');
    }
  }
}
function buildCloudServiceBreakdownRows() {
  var rows = '';

  // ALB — reads sessionStorage written by recalculateAlb() → /api/cost/alb
  if (document.getElementById('tactic-alb') && document.getElementById('tactic-alb').checked) {
    var albCost  = parseFloat(sessionStorage.getItem('tco_alb_cost')  || '0');
    var albLabel = sessionStorage.getItem('tco_alb_label')             || 'ALB';
    if (albCost > 0) rows += buildCloudRow('ALB — ' + albLabel, '$' + albCost.toFixed(2) + '/mo', albCost);
  }

  // ElastiCache — reads sessionStorage written by recalculateCaching() → /api/cost/caching
  if (document.getElementById('tactic-cache') && document.getElementById('tactic-cache').checked) {
    var cacheCost  = parseFloat(sessionStorage.getItem('tco_cache_cost')  || '0');
    var cacheLabel = sessionStorage.getItem('tco_cache_label')              || 'ElastiCache';
    if (cacheCost > 0) rows += buildCloudRow('ElastiCache — ' + cacheLabel, '$' + cacheCost.toFixed(2) + '/mo', cacheCost);
  }

  // Database / backup — reads sessionStorage written by recalculateDbCost() → /api/cost/database-backup
  var dbCost = parseFloat(sessionStorage.getItem('tco_db_cost') || '0');
  if (dbCost > 0) rows += buildCloudRow('Database Backup / DR', '$' + dbCost.toFixed(2) + '/mo', dbCost);

  // Cloud security — reads sessionStorage written by recalculateSecCost() → /api/cost/security-services
  var secCost = parseFloat(sessionStorage.getItem('tco_sec_cost') || '0');
  if (secCost > 0) rows += buildCloudRow('Cloud Security Services', 'GuardDuty / WAF / Inspector / KMS / Macie / CloudWatch', secCost);

  // API Gateway — reads sessionStorage written by recalculateApiGw() → /api/cost/api-gateway
  var apiGatewayCost = parseFloat(sessionStorage.getItem('tco_apigw_cost') || '0');
  var isApiGwEnabled = !!(document.getElementById('tactic-apigw') && document.getElementById('tactic-apigw').checked);
  if (isApiGwEnabled || apiGatewayCost > 0) {
    var apiGwType = (document.getElementById('input-apigw-type') || { value: 'REST' }).value.toUpperCase();
    if (apiGatewayCost > 0) {
      rows += buildCloudRow('API Gateway (' + apiGwType + ')', '$' + apiGatewayCost.toFixed(2) + '/mo', apiGatewayCost);
    } else {
      rows += '<tr>'
        + '<td class="tbd-name">API Gateway (' + apiGwType + ')'
        + '<div class="tbd-detail" style="color:var(--amber);">Enter call volume to calculate cost</div></td>'
        + '<td class="tbd-badges"><span class="tbd-badge tbd-info">cloud infra</span></td>'
        + '<td class="tbd-cost" style="color:var(--ink-light);font-size:.78rem;">enter volume ↓</td></tr>';
    }
  }

  // Container — reads sessionStorage written by recalculateContainerCost() → /api/cost/container
  var containerCost     = parseFloat(sessionStorage.getItem('tco_container_cost') || '0');
  var orchestrationType = (document.getElementById('input-orchestration') || { value: 'EKS' }).value || 'EKS';
  if (containerCost > 0) rows += buildCloudRow('Containerized Cluster (' + orchestrationType.toUpperCase() + ')', '$' + containerCost.toFixed(2) + '/mo · cluster + storage + licenses', containerCost);

  // FinOps saving (negative row)
  var finopsSaving = parseFloat(sessionStorage.getItem('tco_finops_saving') || '0');
  if (finopsSaving > 0) {
    rows += '<tr style="background:var(--green-bg);">'
      + '<td class="tbd-name" style="color:var(--green);">FinOps RI/SP Saving'
      + '<div class="tbd-detail">Reserved Instance / Savings Plan discount applied via /api/cost/finops-discount</div></td>'
      + '<td class="tbd-badges"><span class="tbd-badge" style="background:rgba(22,101,52,.1);color:var(--green);">saving</span></td>'
      + '<td class="tbd-cost" style="color:var(--green);font-weight:700;">-$' + finopsSaving.toFixed(2) + '/mo</td></tr>';
  }

  return rows;
}

function buildCloudRow(serviceName, detailText, monthlyCostUsd) {
  return '<tr>'
    + '<td class="tbd-name">' + serviceName + '<div class="tbd-detail">' + detailText + '</div></td>'
    + '<td class="tbd-badges"><span class="tbd-badge tbd-info">cloud infra</span></td>'
    + '<td class="tbd-cost tbd-cost-impact">+$' + monthlyCostUsd.toFixed(2) + '/mo</td>'
    + '</tr>';
}



function renderComparisonEstimate(baseRps, effectiveRps) {
  var baseCost = calcMonthlyCost(baseRps, PLACEHOLDER_RESP_BYTES);
  var tacticsCost = calcMonthlyCost(effectiveRps, PLACEHOLDER_RESP_BYTES);
  var base = { cost: baseCost.cost, respGb: baseCost.gbPerMonth, responseSizeEff: null };
  var tactics = { cost: tacticsCost.cost, respGb: tacticsCost.gbPerMonth, responseSizeEff: null };
  renderComparisonFromBackend(base, tactics, baseRps, effectiveRps, true, collectTacticContributions(baseRps, null));
}

// Patched version with byte estimation called from recalculate
function renderComparisonEstimateWithBytes(baseRps, effectiveRps) {
  var estTlsB = 0, estJwtB = 0;
  if ((document.getElementById('tactic-tls') && document.getElementById('tactic-tls').checked) ||
    (document.getElementById('tactic-mtls') && document.getElementById('tactic-mtls').checked)) estTlsB = 29;
  if (document.getElementById('tactic-oauth') && document.getElementById('tactic-oauth').checked) estJwtB = 650;
  var estRespBytes = PLACEHOLDER_RESP_BYTES + estTlsB;
  var fakeBackend = { responseSizeEff: PLACEHOLDER_RESP_BYTES, tlsOverheadBytes: estTlsB, jwtOverheadBytes: estJwtB };
  var base = {
    cost: calcMonthlyCost(baseRps, PLACEHOLDER_RESP_BYTES).cost,
    respGb: calcMonthlyCost(baseRps, PLACEHOLDER_RESP_BYTES).gbPerMonth,
    responseSizeEff: PLACEHOLDER_RESP_BYTES, tlsOverheadBytes: estTlsB, jwtOverheadBytes: estJwtB
  };
  var tactics2 = {
    cost: calcMonthlyCost(effectiveRps, estRespBytes).cost,
    respGb: calcMonthlyCost(effectiveRps, estRespBytes).gbPerMonth,
    responseSizeEff: estRespBytes
  };
  renderComparisonFromBackend(base, tactics2, baseRps, effectiveRps, true, collectTacticContributions(baseRps, fakeBackend));
}

function recalculateAlb() {
  var checked = !!(document.getElementById('tactic-alb') && document.getElementById('tactic-alb').checked);
  var params = document.getElementById('alb-params');
  if (params) params.style.display = checked ? 'block' : 'none';
  if (!checked || !_albData) return;
  var count = parseInt((document.getElementById('input-alb-count') || { value: '1' }).value) || 1;
  var lcu = parseFloat((document.getElementById('input-alb-lcu') || { value: '0' }).value) || 0;
  var fixedM = _albData.fixedPerMonthUsd * count;
  var lcuM = _albData.lcuPerHourUsd * lcu * 730 * count;
  var res = document.getElementById('alb-result');
  if (res) res.innerHTML = '<i class="fas fa-calculator" style="margin-right:6px;"></i><strong>' + count + ' ALB(s): $' + fixedM.toFixed(2) + '/mo fixed' + (lcu > 0 ? ' + $' + lcuM.toFixed(2) + '/mo LCUs = <span style="color:var(--red);">$' + (fixedM + lcuM).toFixed(2) + '/mo</span>' : '') + '</strong>';
  sessionStorage.setItem('tco_alb_cost', (fixedM + lcuM).toFixed(4));
  sessionStorage.setItem('tco_alb_label', count + ' ALB' + (count > 1 ? 's' : ''));

  /* Refresh live comparison when cloud tactic cost changes */
  var _bRpsR = parseInt((document.getElementById('requestsPerSecond') || { value: '0' }).value) || 0;
  if (_bRpsR) {
    var _effR = _bRpsR;
    var _sagaCbR = document.getElementById('tactic-saga');
    if (_sagaCbR && _sagaCbR.checked) {
      var _ssR2 = (parseInt((document.getElementById('input-saga-compensatable') || { value: '0' }).value) || 0)
        + (parseInt((document.getElementById('input-saga-retriable') || { value: '0' }).value) || 0)
        + (parseInt((document.getElementById('input-saga-pivot') || { value: '0' }).value) || 0);
      var _extR = !!(document.getElementById('tactic-saga-external-vpc') && document.getElementById('tactic-saga-external-vpc').checked);
      if (_ssR2 > 0 && _extR) _effR = _bRpsR * _ssR2;
    }
    var _retryCbR = document.getElementById('tactic-retry');
    if (_retryCbR && _retryCbR.checked) {
      var _errPR = parseFloat((document.getElementById('input-retry-error-pct') || { value: '5' }).value) || 5;
      _effR += Math.round(_effR * _errPR / 100);
    }
    updateLiveComparison(_bRpsR, _effR);
  }
}

function recalculateAvailability() {
  var sla = parseFloat((document.getElementById('input-sla') || { value: '0' }).value) || 0;
  var res = document.getElementById('availability-result');
  var mc = !!(document.getElementById('tactic-mission-critical') && document.getElementById('tactic-mission-critical').checked);
  var mcNote = document.getElementById('finops-mission-critical-note');
  if (mcNote) mcNote.style.display = mc ? 'flex' : 'none';
  var mcNoteP4 = document.getElementById('p4-mission-critical-note');
  if (mcNoteP4) mcNoteP4.style.display = mc ? 'flex' : 'none';
  var fn4r = document.getElementById('p4-finops-notes');
  if (fn4r) fn4r.style.display = 'block';
  if (!res) return;
  if (!sla) { res.style.display = 'none'; return; }
  var dtMo = ((1 - sla / 100) * 30 * 24 * 60).toFixed(1);
  var dtYr = ((1 - sla / 100) * 365 * 24).toFixed(1);
  var mult = sla >= 99.99 ? 2.5 : sla >= 99.9 ? 1.8 : 1.2;
  var badge, arch;
  if (sla <= 99) {
    badge = '<span class="tbd-badge badge-dr" style="margin-left:5px;">DR</span>';
    arch =
      '<div class="notice notice-gold" style="margin-top:10px;"><i class="fas fa-server"></i><div>'
      + '<strong>Recommended Architecture \u2014 99% SLA (Disaster Recovery)</strong>'
      + '<ul style="margin:6px 0 0 14px;font-size:.79rem;line-height:1.7;">'
      + '<li>Single EC2 instance with self-hosted SQL database</li>'
      + '<li>Automated DB backup to S3; S3 Lifecycle Policies to move older backups to <strong>S3 Glacier</strong></li>'
      + '<li><strong>Route 53 Health Check</strong> to monitor service health and trigger DNS failover</li>'
      + '</ul>'
      + '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:10px;">'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Downtime / year</div><div class="unit-econ-value" style="font-size:.9rem;">' + dtYr + ' hr</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">RTO</div><div class="unit-econ-value" style="font-size:.9rem;">~70 min</div><div class="unit-econ-sub">30 min analysis + 40 min recovery</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">RPO</div><div class="unit-econ-value" style="font-size:.9rem;">~4.8 hrs</div><div class="unit-econ-sub">1 failure/quarter assumption</div></div>'
      + '</div>'
      + '<div style="font-size:.74rem;color:var(--amber);margin-top:8px;padding:6px 10px;background:rgba(212,160,23,.12);border-radius:var(--r);">'
      + '\u2139 Assumptions: 1 failure/quarter. 30 min to analyze, 40 min to restore. RPO of 4.8 hrs based on backup frequency.</div>'
      + '</div></div>';
  } else if (sla <= 99.9) {
    badge = '<span class="tbd-badge badge-bc" style="margin-left:5px;">BC</span>';
    arch =
      '<div class="notice notice-blue" style="margin-top:10px;"><i class="fas fa-layer-group"></i><div>'
      + '<strong>Recommended Architecture \u2014 99.9% SLA (Business Continuity)</strong>'
      + '<ul style="margin:6px 0 0 14px;font-size:.79rem;line-height:1.7;">'
      + '<li><strong>ALB</strong> proxying traffic with health checks across multiple AZs</li>'
      + '<li><strong>Auto Scaling Group</strong> with minimum <strong>6 running instances</strong> across \u22652 Availability Zones</li>'
      + '<li><strong>Multi-AZ RDS</strong> (standby in separate AZ) \u2014 RPO reduced to 5 min</li>'
      + '</ul>'
      + '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:10px;">'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Downtime / year</div><div class="unit-econ-value" style="font-size:.9rem;">' + dtYr + ' hr</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Cost multiplier</div><div class="unit-econ-value" style="font-size:.9rem;">' + mult + '\u00d7</div><div class="unit-econ-sub">vs single-AZ base</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">RPO target</div><div class="unit-econ-value" style="font-size:.9rem;">~5 min</div><div class="unit-econ-sub">Multi-AZ RDS failover</div></div>'
      + '</div></div></div>';
  } else {
    badge = '<span class="tbd-badge badge-dr" style="margin-left:4px;">DR</span><span class="tbd-badge badge-bc" style="margin-left:4px;">BC</span>';
    arch =
      '<div class="notice notice-green" style="margin-top:10px;"><i class="fas fa-earth-americas"></i><div>'
      + '<strong>Recommended Architecture \u2014 99.99% SLA (BC + DR)</strong>'
      + '<ul style="margin:6px 0 0 14px;font-size:.79rem;line-height:1.7;">'
      + '<li><strong>Two simultaneous instances of the service in two separate AWS Regions</strong></li>'
      + '<li><strong>Active/Passive</strong> regional setup \u2014 Route 53 Failover routing for region-level DNS failover</li>'
      + '<li>Multi-AZ RDS per region (primary + secondary in the same region per deployment)</li>'
      + '<li>Cross-region Read Replicas or <strong>Aurora Global Database</strong> \u2014 RPO ~10 min</li>'
      + '</ul>'
      + '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:10px;">'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Downtime / year</div><div class="unit-econ-value" style="font-size:.9rem;">' + dtYr + ' hr</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Cost multiplier</div><div class="unit-econ-value" style="font-size:.9rem;">' + mult + '\u00d7</div><div class="unit-econ-sub">multi-region overhead</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">RPO target</div><div class="unit-econ-value" style="font-size:.9rem;">~10 min</div><div class="unit-econ-sub">Aurora Global / cross-region RR</div></div>'
      + '</div></div></div>';
  }
  res.innerHTML =
    '<div style="padding:14px;border-radius:var(--r);background:linear-gradient(135deg,#eff6ff,#dbeafe);border:1px solid #93c5fd;">'
    + '<div style="font-family:\'DM Serif Display\',serif;font-size:1rem;color:var(--blue-deep);margin-bottom:4px;">'
    + '<i class="fas fa-shield-halved" style="margin-right:6px;"></i>SLA ' + sla + '% Analysis ' + badge + '</div>'
    + '<div style="font-size:.78rem;color:var(--ink-light);margin-bottom:8px;">Max downtime: <strong>' + dtMo + ' min/month</strong> \u00b7 ' + dtYr + ' hr/year \u00b7 Cost multiplier <strong>' + mult + '\u00d7</strong></div>'
    + arch
    + '</div>';
  res.style.display = 'block';
}

function renderDbOptions() {
  var engine = (document.getElementById('input-db-engine') || { value: '' }).value || '';
  var cont = document.getElementById('db-options');
  var res = document.getElementById('dbbackup-result');
  if (!engine || engine === 'none' || !_dbData || !cont) { if (cont) cont.innerHTML = ''; if (res) res.style.display = 'none'; return; }
  var html = '';
  function dbCb(id, label, desc, numId, numLabel) {
    return '<div class="tactic-row"><input type="checkbox" class="tactic-check" id="' + id + '">'
      + '<div class="tactic-label-group"><label class="tactic-label" for="' + id + '">' + label + '</label><span class="tactic-description">' + desc + '</span></div>'
      + (numId ? '<div class="tactic-input-group"><input type="number" id="' + numId + '" min="1" placeholder="10"><span class="tactic-unit">' + numLabel + '</span></div>' : '')
      + '</div>';
  }
  if (engine === 'rds-mysql') {
    html = dbCb('tactic-s3-backup', 'S3 Backup', '$' + _dbData.s3StandardPerGbMonth + '/GB-month.', 'db-gb', 'DB size (GB)')
      + dbCb('tactic-rds-snapshot', 'RDS Snapshot', '$' + _dbData.rdsSnapshotPerGbMonth + '/GB-month beyond free tier.', 'snapshot-retention', 'Retention days')
      + dbCb('tactic-rds-multiaz', 'RDS Multi-AZ', _dbData.rdsMultiAzSurchargeNote, null, null);
  } else if (engine === 'aurora') {
    html = dbCb('tactic-aurora-replica', 'Aurora Read Replica', '~$' + _dbData.auroraReplicaPerHour + '/hr ($' + (_dbData.auroraReplicaPerHour * 730).toFixed(2) + '/mo) db.r6g.large.', 'aurora-replica-count', 'Replicas')
      + dbCb('tactic-s3-backup', 'S3 Snapshot Export', '$' + _dbData.s3StandardPerGbMonth + '/GB-month.', 'db-gb', 'DB size (GB)');
  } else if (engine === 'dynamodb') {
    html = dbCb('tactic-dynamo-pitr', 'DynamoDB PITR', 'Free — pay only for backup storage (~$0.20/GB-month).', null, null)
      + dbCb('tactic-dynamo-global', 'DynamoDB Global Tables', _dbData.dynamoGlobalTableNote, 'dynamo-extra-regions', 'Extra regions');
  }
  cont.innerHTML = html;
  cont.querySelectorAll('input[type="checkbox"]').forEach(function (cb) { cb.addEventListener('change', recalculateDbCost); });
  cont.querySelectorAll('input[type="number"]').forEach(function (inp) { inp.addEventListener('input', recalculateDbCost); });
}

function recalculateDbCost() {
  if (!_dbData) return;
  var total = 0; var lines = [];
  var dbGb = parseFloat((document.getElementById('db-gb') || { value: '10' }).value) || 10;
  var g = function (id) { return document.getElementById(id) || {}; };
  if (g('tactic-s3-backup').checked) { var c = dbGb * _dbData.s3StandardPerGbMonth; total += c; lines.push('S3 backup: $' + c.toFixed(2) + '/mo (' + dbGb + ' GB)'); }
  if (g('tactic-rds-snapshot').checked) lines.push('RDS Snapshot: first ' + dbGb + ' GB free, then $' + _dbData.rdsSnapshotPerGbMonth + '/GB-mo');
  if (g('tactic-rds-multiaz').checked) lines.push('Multi-AZ: ~2\u00d7 your RDS instance cost');
  if (g('tactic-aurora-replica').checked) { var cnt = parseInt(g('aurora-replica-count').value) || 1; var c2 = _dbData.auroraReplicaPerHour * 730 * cnt; total += c2; lines.push('Aurora replicas: $' + c2.toFixed(2) + '/mo (' + cnt + ' nodes)'); }
  if (g('tactic-dynamo-global').checked) { var reg = parseInt(g('dynamo-extra-regions').value) || 1; lines.push('DynamoDB Global: $' + _dbData.dynamoGlobalTablePerWruUsd + '/WRU \u00d7 ' + reg + ' region(s)'); }
  var res = document.getElementById('dbbackup-result');
  if (!res) return;
  if (!lines.length) { res.style.display = 'none'; return; }
  res.style.display = 'block';
  res.innerHTML = '<i class="fas fa-database" style="margin-right:6px;"></i><strong>DB backup/DR estimate:</strong><br>'
    + lines.map(function (l) { return '<div style="font-size:.8rem;margin-top:4px;">\u2022 ' + l + '</div>'; }).join('')
    + (total > 0 ? '<div style="margin-top:8px;font-weight:700;color:var(--red);">Quantifiable total: $' + total.toFixed(2) + '/mo</div>' : '');
  if (total > 0) sessionStorage.setItem('tco_db_cost', total.toFixed(4));
  else sessionStorage.removeItem('tco_db_cost');

  /* Refresh live comparison when cloud tactic cost changes */
  var _bRpsR = parseInt((document.getElementById('requestsPerSecond') || { value: '0' }).value) || 0;
  if (_bRpsR) {
    var _effR = _bRpsR;
    var _sagaCbR = document.getElementById('tactic-saga');
    if (_sagaCbR && _sagaCbR.checked) {
      var _ssR2 = (parseInt((document.getElementById('input-saga-compensatable') || { value: '0' }).value) || 0)
        + (parseInt((document.getElementById('input-saga-retriable') || { value: '0' }).value) || 0)
        + (parseInt((document.getElementById('input-saga-pivot') || { value: '0' }).value) || 0);
      var _extR = !!(document.getElementById('tactic-saga-external-vpc') && document.getElementById('tactic-saga-external-vpc').checked);
      if (_ssR2 > 0 && _extR) _effR = _bRpsR * _ssR2;
    }
    var _retryCbR = document.getElementById('tactic-retry');
    if (_retryCbR && _retryCbR.checked) {
      var _errPR = parseFloat((document.getElementById('input-retry-error-pct') || { value: '5' }).value) || 5;
      _effR += Math.round(_effR * _errPR / 100);
    }
    updateLiveComparison(_bRpsR, _effR);
  }
}

function recalculateSecCost() {
  if (!_secData) return;
  var total = 0; var lines = [];
  var rps = parseInt((document.getElementById('requestsPerSecond') || { value: '0' }).value) || 0;
  var rpm = rps * 2592000;
  var g = function (id) { return document.getElementById(id) || {}; };
  if (g('sec-guardduty').checked) { var gb = parseFloat(g('input-guardduty-gb').value) || 0; var c = Math.max(0, gb - 500) * _secData.guardDutyPerGbLogs; total += c; lines.push('GuardDuty: $' + c.toFixed(2) + '/mo'); }
  if (g('sec-inspector').checked) { var i = parseInt(g('input-inspector-instances').value) || 1; var c2 = i * _secData.inspectorPerInstanceMonth; total += c2; lines.push('Inspector: $' + c2.toFixed(2) + '/mo (' + i + ' instances)'); }
  if (g('sec-waf').checked) { var r = parseInt(g('input-waf-rules').value) || 5; var c3 = _secData.wafWebAclPerMonth + r * _secData.wafRulePerMonth + (rpm / 1000000) * _secData.wafPer1MRequests; total += c3; lines.push('WAF: $' + c3.toFixed(2) + '/mo'); }
  if (g('sec-macie').checked) { var gbM = parseFloat(g('input-macie-gb').value) || 0; var c4 = Math.max(0, gbM - 1) * _secData.maciePerGbClassified; total += c4; lines.push('Macie: $' + c4.toFixed(2) + '/mo'); }
  if (g('sec-cloudwatch').checked) { var gbC = parseFloat(g('input-cw-gb').value) || 1; var c5 = gbC * _secData.cloudwatchLogsIngestionPerGb + gbC * _secData.cloudwatchLogsStoragePerGbMonth; total += c5; lines.push('CloudWatch Logs: $' + c5.toFixed(2) + '/mo'); }
  if (g('sec-audit').checked) { var a = parseInt(g('input-audit-assessments').value) || 1; var c6 = a * _secData.auditManagerPerAssessmentMonth; total += c6; lines.push('Audit Manager: $' + c6.toFixed(2) + '/mo'); }
  if (g('sec-kms').checked) { var k = parseInt(g('input-kms-keys').value) || 1; var c7 = k * _secData.kmsCmkPerMonth + (rpm / 10000) * _secData.kmsApiCallsPer10k; total += c7; lines.push('KMS: $' + c7.toFixed(2) + '/mo'); }
  var tot = document.getElementById('cloudsec-total');
  if (!tot) return;
  if (!lines.length) { tot.style.display = 'none'; return; }
  tot.style.display = 'block';
  tot.innerHTML = '<i class="fas fa-lock" style="margin-right:6px;"></i><strong>Cloud security: <span style="color:var(--red);">$' + total.toFixed(2) + '/mo</span></strong>'
    + lines.map(function (l) { return '<div style="font-size:.78rem;margin-top:4px;color:var(--ink-medium);">\u2022 ' + l + '</div>'; }).join('');
  sessionStorage.setItem('tco_sec_cost', total.toFixed(4));

  /* Refresh live comparison when cloud tactic cost changes */
  var _bRpsR = parseInt((document.getElementById('requestsPerSecond') || { value: '0' }).value) || 0;
  if (_bRpsR) {
    var _effR = _bRpsR;
    var _sagaCbR = document.getElementById('tactic-saga');
    if (_sagaCbR && _sagaCbR.checked) {
      var _ssR2 = (parseInt((document.getElementById('input-saga-compensatable') || { value: '0' }).value) || 0)
        + (parseInt((document.getElementById('input-saga-retriable') || { value: '0' }).value) || 0)
        + (parseInt((document.getElementById('input-saga-pivot') || { value: '0' }).value) || 0);
      var _extR = !!(document.getElementById('tactic-saga-external-vpc') && document.getElementById('tactic-saga-external-vpc').checked);
      if (_ssR2 > 0 && _extR) _effR = _bRpsR * _ssR2;
    }
    var _retryCbR = document.getElementById('tactic-retry');
    if (_retryCbR && _retryCbR.checked) {
      var _errPR = parseFloat((document.getElementById('input-retry-error-pct') || { value: '5' }).value) || 5;
      _effR += Math.round(_effR * _errPR / 100);
    }
    updateLiveComparison(_bRpsR, _effR);
  }
}

function recalculateCostOpt() {
  if (!_coData) return;
  var spend = parseFloat((document.getElementById('input-ec2-monthly-spend') || { value: '0' }).value) || 0;
  var res = document.getElementById('costopt-result');
  if (!res) return;
  if (!spend) { res.style.display = 'none'; return; }
  var lines = []; var best = 0;
  var g = function (id) { return document.getElementById(id) || {}; };
  function addOpt(checked, pct, label) { if (!checked) return; var s = spend * pct / 100; if (s > best) best = s; lines.push(label + ': save $' + s.toFixed(2) + '/mo (~' + pct + '%)'); }
  addOpt(g('opt-ri-standard').checked, _coData.reservedInstance1yrSavingsPct, 'Standard RI 1-yr');
  addOpt(g('opt-ri-3yr').checked, _coData.reservedInstance3yrSavingsPct, 'Standard RI 3-yr');
  addOpt(g('opt-ri-convertible').checked, _coData.convertibleRi1yrSavingsPct, 'Convertible RI 1-yr');
  addOpt(g('opt-savings-plan-1yr').checked, _coData.savingsPlan1yrSavingsPct, 'Savings Plan 1-yr');
  addOpt(g('opt-savings-plan-3yr').checked, _coData.savingsPlan3yrSavingsPct, 'Savings Plan 3-yr');
  if (g('opt-trusted-advisor').checked) { var tc = Math.max(_coData.businessSupportMinMonthUsd, spend * _coData.businessSupportPctMonthlyUsage / 100); lines.push('Trusted Advisor: +$' + tc.toFixed(2) + '/mo'); }
  res.style.display = 'block';
  var _fpRows = [];
  if (g('opt-ri-standard').checked) _fpRows.push({ s: 'EC2 On-Demand (Standard RI 1-yr)', p: _coData.reservedInstance1yrSavingsPct });
  if (g('opt-ri-3yr').checked) _fpRows.push({ s: 'EC2 On-Demand (Standard RI 3-yr)', p: _coData.reservedInstance3yrSavingsPct });
  if (g('opt-ri-convertible').checked) _fpRows.push({ s: 'EC2 On-Demand (Convertible RI 1-yr)', p: _coData.convertibleRi1yrSavingsPct });
  if (g('opt-savings-plan-1yr').checked) _fpRows.push({ s: 'EC2+Lambda+Fargate (SP 1-yr)', p: _coData.savingsPlan1yrSavingsPct });
  if (g('opt-savings-plan-3yr').checked) _fpRows.push({ s: 'EC2+Lambda+Fargate (SP 3-yr)', p: _coData.savingsPlan3yrSavingsPct });
  var _fpHtml = _fpRows.length > 0
    ? '<div style="margin-top:14px;padding:12px 14px;border-radius:var(--r);'
    + 'background:linear-gradient(135deg,#f0fdf4,#e8f5e9);border:1px solid var(--green-border);">'
    + '<div style="font-family:\'DM Serif Display\',serif;font-size:.9rem;color:var(--green);margin-bottom:9px;">'
    + '<i class="fas fa-tags" style="margin-right:6px;"></i>Configuration of Cloud Services based on FinOps Practices</div>'
    + '<table style="width:100%;border-collapse:collapse;font-size:.8rem;">'
    + '<thead><tr style="background:rgba(22,101,52,.1);">'
    + '<th style="padding:6px 10px;text-align:left;">AWS Service</th>'
    + '<th style="padding:6px 10px;text-align:center;">Saving %</th>'
    + '<th style="padding:6px 10px;text-align:right;">On-Demand</th>'
    + '<th style="padding:6px 10px;text-align:right;">Adjusted Cost</th>'
    + '</tr></thead><tbody>'
    + _fpRows.map(function (r) {
      var sv = spend * r.p / 100;
      return '<tr style="border-bottom:1px solid rgba(22,101,52,.12);">'
        + '<td style="padding:6px 10px;font-weight:600;">' + r.s + '</td>'
        + '<td style="padding:6px 10px;text-align:center;color:var(--green);font-weight:700;">' + r.p + '%</td>'
        + '<td style="padding:6px 10px;text-align:right;color:var(--ink-light);text-decoration:line-through;">$' + spend.toFixed(2) + '/mo</td>'
        + '<td style="padding:6px 10px;text-align:right;font-weight:700;color:var(--green);">$' + (spend - sv).toFixed(2) + '/mo</td>'
        + '</tr>';
    }).join('')
    + '</tbody></table>'
    + '<div style="font-size:.73rem;color:var(--ink-light);margin-top:7px;">'
    + '&#9432; Applies to EC2 compute costs only. '
    + 'Data transfer (egress) costs cannot be discounted via Reserved Instances or Savings Plans.</div></div>'
    : '';
  /* Save FinOps saving to sessionStorage so TCO table can reflect it */
  /* Convention: tco_finops_saving = positive number = monthly saving to subtract */
  if (best > 0) {
    sessionStorage.setItem('tco_finops_saving', best.toFixed(4));
    sessionStorage.setItem('tco_finops_spend', spend.toFixed(4));
  } else {
    sessionStorage.removeItem('tco_finops_saving');
    sessionStorage.removeItem('tco_finops_spend');
  }
  res.innerHTML = '<div style="padding:14px;border-radius:var(--r);background:linear-gradient(135deg,var(--gold-light),#fffdf5);border:1px solid var(--gold-border);">'
    + '<div style="font-family:\'DM Serif Display\',serif;font-size:.95rem;color:var(--gold);margin-bottom:10px;"><i class="fas fa-coins" style="margin-right:6px;"></i>Cost Optimisation Summary</div>'
    + lines.map(function (l) { return '<div style="font-size:.82rem;margin-top:6px;color:var(--ink-medium);">\u2022 ' + l + '</div>'; }).join('')
    + (best > 0 ? '<div style="margin-top:10px;font-weight:700;color:var(--green);">Best monthly saving: $' + best.toFixed(2) + '/mo \u2192 $' + ((spend - best) * 12).toFixed(2) + '/yr</div>' : '')
    + '</div>'
    + _fpHtml;
  /* Refresh live comparison delta */
  var _bRco = parseInt((document.getElementById('requestsPerSecond') || { value: '0' }).value) || 0;
  if (_bRco) updateLiveComparison(_bRco, _bRco);
  /* Fetch service-specific RI prices when EC2 instance is configured */
  var _instSel = document.getElementById('input-ec2-instance');
  var _instTyp = _instSel && _instSel.value ? _instSel.value.split('|')[0] : null;
  if (_instTyp && _fpRows.length > 0) {
    fetch('/api/finops/ri-prices/' + encodeURIComponent(_instTyp))
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (riData) {
        if (!riData) return;
        /* Update rows with live actual % */
        _fpRows.forEach(function (row) {
          if (row.s.indexOf('1-yr') >= 0 && riData.ri1yrActualDiscountPct > 0) row.p = riData.ri1yrActualDiscountPct;
          if (row.s.indexOf('3-yr') >= 0 && riData.ri3yrActualDiscountPct > 0) row.p = riData.ri3yrActualDiscountPct;
        });
        var newBest2 = 0;
        _fpRows.forEach(function (r2) { var sv = spend * r2.p / 100; if (sv > newBest2) newBest2 = sv; });
        if (newBest2 > 0) {
          sessionStorage.setItem('tco_finops_saving', newBest2.toFixed(4));
          sessionStorage.setItem('tco_finops_spend', spend.toFixed(4));
          if (_bRco) updateLiveComparison(_bRco, _bRco);
        }
        /* Append live price note */
        if (res) {
          var _r1n = riData.ri1yrActualDiscountPct > 0 ? ' · 1-yr RI: ' + riData.ri1yrActualDiscountPct + '% off' : '';
          var _r3n = riData.ri3yrActualDiscountPct > 0 ? ' · 3-yr RI: ' + riData.ri3yrActualDiscountPct + '% off' : '';
          var liveNote = '<div style="font-size:.72rem;color:var(--blue-mid);margin-top:6px;">'
            + '<i class="fas fa-cloud" style="margin-right:4px;"></i>'
            + 'Live: ' + _instTyp + ' On-Demand $' + riData.onDemandPerHour.toFixed(4) + '/hr' + _r1n + _r3n + '</div>';
          res.insertAdjacentHTML('beforeend', liveNote);
        }
      }).catch(function () { });
  }
}

function recalculateCaching() {
  var checked = !!(document.getElementById('tactic-cache') && document.getElementById('tactic-cache').checked);
  var params = document.getElementById('cache-params');
  if (params) params.style.display = checked ? 'block' : 'none';
  if (!checked || !_cacheData) return;
  var engine = (document.getElementById('input-cache-engine') || { value: 'redis' }).value || 'redis';
  var node = (document.getElementById('input-cache-node') || { value: 'r6g.large' }).value || 'r6g.large';
  var nodes = parseInt((document.getElementById('input-cache-nodes') || { value: '1' }).value) || 1;
  var pMap = {
    'redisr6glarge': _cacheData.redisR6gLargePerHour,
    'redisr6gxlarge': _cacheData.redisR6gXlargePerHour,
    'redisr6g2xlarge': _cacheData.redisR6g2xlargePerHour,
    'memcachedr6glarge': _cacheData.memcachedR6gLargePerHour,
    'memcachedr6gxlarge': _cacheData.memcachedR6gXlargePerHour
  };
  var prHr = pMap[engine + node.replace('.', 'g')] || 0.166;
  var prMo = prHr * 730 * nodes;
  var inner = document.getElementById('cache-result-inner');
  var outer = document.getElementById('caching-result');
  if (inner) { inner.style.display = 'block'; inner.innerHTML = '<i class="fas fa-calculator" style="margin-right:6px;"></i><strong>' + nodes + ' x cache.' + node + ' (' + engine + '): $' + prHr.toFixed(4) + '/hr x ' + nodes + ' x 730 hr = <span style="color:var(--red);">$' + prMo.toFixed(2) + '/mo</span></strong>'; }
  if (outer) { outer.style.display = 'block'; outer.innerHTML = '<i class="fas fa-bolt" style="margin-right:6px;"></i><strong>ElastiCache ' + engine + ': $' + prMo.toFixed(2) + '/mo</strong><br><span style="font-size:.76rem;color:var(--ink-light);">Reserved 1-yr: ~$' + (prMo * 0.45).toFixed(2) + '/mo (55% saving) | 3-yr: ~$' + (prMo * 0.30).toFixed(2) + '/mo (70% saving)</span>'; }
  sessionStorage.setItem('tco_cache_cost', prMo.toFixed(4));
  sessionStorage.setItem('tco_cache_label', nodes + 'x cache.' + node + ' (' + engine + ')');

  /* Refresh live comparison when cloud tactic cost changes */
  var _bRpsR = parseInt((document.getElementById('requestsPerSecond') || { value: '0' }).value) || 0;
  if (_bRpsR) {
    var _effR = _bRpsR;
    var _sagaCbR = document.getElementById('tactic-saga');
    if (_sagaCbR && _sagaCbR.checked) {
      var _ssR2 = (parseInt((document.getElementById('input-saga-compensatable') || { value: '0' }).value) || 0)
        + (parseInt((document.getElementById('input-saga-retriable') || { value: '0' }).value) || 0)
        + (parseInt((document.getElementById('input-saga-pivot') || { value: '0' }).value) || 0);
      var _extR = !!(document.getElementById('tactic-saga-external-vpc') && document.getElementById('tactic-saga-external-vpc').checked);
      if (_ssR2 > 0 && _extR) _effR = _bRpsR * _ssR2;
    }
    var _retryCbR = document.getElementById('tactic-retry');
    if (_retryCbR && _retryCbR.checked) {
      var _errPR = parseFloat((document.getElementById('input-retry-error-pct') || { value: '5' }).value) || 5;
      _effR += Math.round(_effR * _errPR / 100);
    }
    updateLiveComparison(_bRpsR, _effR);
  }
}

function recalculateContainerCost() {
  var cronOn = !!(document.getElementById('cef-cronjobs') && document.getElementById('cef-cronjobs').checked);
  var dR = document.getElementById('row-cronjob-duration'); if (dR) dR.style.display = cronOn ? 'flex' : 'none';
  var fR = document.getElementById('row-cronjob-freq'); if (fR) fR.style.display = cronOn ? 'flex' : 'none';
  var g = function (id) { return document.getElementById(id) || {}; };
  var chk = function (id) { return !!(g(id).checked); };
  var num = function (id, def) { return parseFloat(g(id).value) || (def || 0); };
  /* Use live AWS prices from /api/aws/container-pricing if already loaded */
  var _cp = window._containerPriceData || {};
  var _eksPerCluster = (_cp.eksControlPlanePerMonth != null) ? _cp.eksControlPlanePerMonth : 73.0;
  var _albPerLb = (_cp.albFixedPerMonth != null) ? _cp.albFixedPerMonth : 16.43;
  var _ebsPerGb = (_cp.ebsGp3PerGbMonth != null) ? _cp.ebsGp3PerGbMonth : 0.08;
  var _s3PerGb = 0.023; /* S3 Standard — stable, no API needed */
  /* If live prices not yet loaded, kick off the fetch */
  if (!window._containerPriceData) {
    fetch('/api/aws/container-pricing').then(function (r) { return r.ok ? r.json() : null; })
      .then(function (d) { if (d) { window._containerPriceData = d; recalculateContainerCost(); } }).catch(function () { });
  }
  var aLines = [], aTotal = 0;
  if (chk('cef-clusters')) {
    var nCl = num('input-clusters', 1);
    var orch = g('input-orchestration').value || '';
    if (orch === '' || orch === 'eks') { var c0 = nCl * _eksPerCluster; aTotal += c0; aLines.push('EKS Control Plane (' + nCl + ' x $' + _eksPerCluster.toFixed(2) + '/mo): $' + c0.toFixed(2) + '/mo'); }
    else aLines.push('ECS clusters: no control plane charge');
  }
  if (chk('cef-cluster-lb')) { var nLB = num('input-cluster-lbs', 1); var cLB = nLB * _albPerLb; aTotal += cLB; aLines.push('Cluster ALBs (' + nLB + 'x $' + _albPerLb.toFixed(2) + '/mo): $' + cLB.toFixed(2) + '/mo'); }
  if (chk('cef-host-storage')) { var gbN = num('input-host-storage-gb', 30); var nPo = num('input-pods', 10); var estN = Math.max(1, Math.ceil(nPo / 10)); var cHS = gbN * estN * _ebsPerGb; aTotal += cHS; aLines.push('Host OS Storage (~' + estN + ' nodes x ' + gbN + ' GB x $' + _ebsPerGb.toFixed(3) + '): $' + cHS.toFixed(2) + '/mo'); }
  if (chk('cef-cluster-backup')) { var bkGb = num('input-cluster-backup-gb', 50); var cBk = bkGb * _s3PerGb; aTotal += cBk; aLines.push('Cluster Backup (' + bkGb + ' GB x $' + _s3PerGb + '): $' + cBk.toFixed(2) + '/mo'); }
  if (chk('cef-host-license')) { var licR = parseFloat(g('input-host-os').value) || 0; if (licR > 0) { var nPo2 = num('input-pods', 10); var estN2 = Math.max(1, Math.ceil(nPo2 / 10)); var cHL = licR * 730 * estN2; aTotal += cHL; aLines.push('Host OS License: $' + cHL.toFixed(2) + '/mo'); } }
  if (chk('cef-workload-license')) { var wl = num('input-workload-license-cost', 0); if (wl > 0) { aTotal += wl; aLines.push('Workload License: $' + wl.toFixed(2) + '/mo'); } }
  /* Spot discount is still valid here — it's about instance type selection, not RI/SP purchasing */
  var spotDisc = (parseInt(g('input-spot-pct').value) || 0) / 100;
  if (spotDisc > 0 && aTotal > 0) { var sv = aTotal * spotDisc; aTotal -= sv; aLines.push('Spot discount (' + Math.round(spotDisc * 100) + '%): -$' + sv.toFixed(2) + '/mo'); }
  var appLines = [], appTotal = 0;
  var isFg = (g('input-underlying-resource').value || '').indexOf('fargate') >= 0;
  var _fgVcpuRate = (_cp.fargateVcpuPerHour != null) ? _cp.fargateVcpuPerHour : 0.04048;
  var _fgGbRate = (_cp.fargateGbPerHour != null) ? _cp.fargateGbPerHour : 0.004445;
  if (isFg && (g('input-underlying-resource').value || '').indexOf('spot') >= 0) {
    _fgVcpuRate = (_cp.fargateSpotVcpuPerHour != null) ? _cp.fargateSpotVcpuPerHour : 0.01254688;
    _fgGbRate = (_cp.fargateSpotGbPerHour != null) ? _cp.fargateSpotGbPerHour : 0.00137248;
  }
  if (chk('cef-cronjobs') && isFg) { var nCJ = num('input-cronjobs', 1); var durM = num('input-cronjob-duration-min', 15); var rD = num('input-cronjob-freq', 4); var cjC = durM * 60 * rD * 30 * nCJ * (0.25 * _fgVcpuRate / 3600 + 0.5 * _fgGbRate / 3600); appTotal += cjC; appLines.push('CronJobs on Fargate: $' + cjC.toFixed(2) + '/mo'); }
  if (chk('cef-statefulsets')) { var stR = num('input-statefulsets', 1); var stV = num('input-vol-limit', 20); var stC = stR * stV * _ebsPerGb; appTotal += stC; appLines.push('StatefulSet PVCs (' + stR + 'x ' + stV + ' GB x $' + _ebsPerGb.toFixed(3) + '): $' + stC.toFixed(2) + '/mo'); }
  var oT = Math.round((aTotal + appTotal) * 100) / 100;
  function rRes(el, lines, tot, icon, title, col) {
    if (!el) return;
    if (!lines.length) { el.style.display = 'none'; return; }
    el.style.display = 'block';
    el.innerHTML = '<div style="padding:11px 13px;border-radius:var(--r);background:rgba(255,255,255,.65);border:1px solid ' + col + '">'
      + '<div style="font-size:.78rem;font-weight:700;margin-bottom:6px;"><i class="fas ' + icon + '" style="margin-right:5px;"></i>' + title + '</div>'
      + lines.map(function (l) { return '<div style="font-size:.78rem;margin-top:3px;color:var(--ink-medium);">\u2022 ' + l + '</div>'; }).join('')
      + (tot > 0 ? '<div style="margin-top:8px;font-weight:700;font-family:monospace;color:var(--red);">Subtotal: $' + tot.toFixed(2) + '/mo</div>' : '')
      + '</div>';
  }
  rRes(document.getElementById('container-assets-result'), aLines, aTotal, 'fa-coins', 'Cluster Asset Costs', 'var(--gold-border)');
  rRes(document.getElementById('container-app-result'), appLines, appTotal, 'fa-code-branch', 'App Design Costs', 'var(--green-border)');
  var ovEl = document.getElementById('container-overall-result');
  if (ovEl) {
    if (oT > 0) {
      ovEl.style.display = 'block';
      ovEl.innerHTML = '<i class="fas fa-layer-group" style="margin-right:6px;"></i><strong>Containerized Environment Factors \u2014 Est. Monthly Cost: <span style="color:var(--red);">$' + oT.toFixed(2) + '/mo</span></strong><div style="font-size:.74rem;color:var(--ink-light);margin-top:4px;">Cluster assets + app design. Qualitative selections inform architecture decisions.</div>';
      sessionStorage.setItem('tco_container_cost', oT.toFixed(4));
    } else {
      ovEl.style.display = 'none'; sessionStorage.removeItem('tco_container_cost');
    }
  }
  var _bR = parseInt((document.getElementById('requestsPerSecond') || { value: '0' }).value) || 0;
  if (_bR) {
    var _eR = _bR;
    var _sOn = !!(document.getElementById('tactic-saga') && document.getElementById('tactic-saga').checked && document.getElementById('tactic-saga-external-vpc') && document.getElementById('tactic-saga-external-vpc').checked);
    if (_sOn) { var _ss = (parseInt((document.getElementById('input-saga-compensatable') || { value: '0' }).value) || 0) + (parseInt((document.getElementById('input-saga-retriable') || { value: '0' }).value) || 0) + (parseInt((document.getElementById('input-saga-pivot') || { value: '0' }).value) || 0); if (_ss > 0) _eR = _bR * _ss; }
    var _rOn = !!(document.getElementById('tactic-retry') && document.getElementById('tactic-retry').checked);
    if (_rOn) { var _ep = parseFloat((document.getElementById('input-retry-error-pct') || { value: '5' }).value) || 5; _eR += Math.round(_eR * _ep / 100); }
    updateLiveComparison(_bR, _eR);
  }
}

function toggleApiGwParams(show) {
  var p = document.getElementById('apigw-params'); if (p) p.style.display = show ? 'block' : 'none';
  if (show && !_apigwData) loadApiGwSection(); else if (show) recalculateApiGw();
  recalculateRps(); scheduleSessionSave(); updateCloudTacticsBadge();
}

function escHtml(s) {
  return String(s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function startNewService(skipConfirm) {
  if (!skipConfirm && !confirm('Start modelling a new service? Current phase inputs will be cleared.')) return;
  ['serviceName', 'servicePurpose', 'requestsPerSecond', 'revenuePerTransaction'].forEach(function (id) {
    var el = document.getElementById(id); if (el) el.value = '';
  });
  document.querySelectorAll('.buc-card').forEach(function (c) { c.classList.remove('selected'); });
  selectedBUC = null; window._lastProtoFile = null;
  var fd = document.getElementById('fileNameDisplay2'); if (fd) fd.textContent = '';
  ['svc_name', 'svc_purpose', 'svc_rps', 'svc_buc', 'svc_consumers', 'svc_consumer_type',
    'svc_revenue_per_tx', 'tco_alb_cost', 'tco_alb_label', 'tco_cache_cost', 'tco_cache_label',
    'tco_db_cost', 'tco_sec_cost', 'tco_tls_overhead', 'tco_jwt_overhead',
    'tco_container_cost', 'tco_apigw_cost', 'tco_finops_saving', 'tco_finops_spend'].forEach(function (k) { sessionStorage.removeItem(k); });
  goToPhase(1);
}

function downloadTcoPdf() {
  var name = (document.getElementById('serviceName') || { value: '' }).value.trim()
    || sessionStorage.getItem('svc_name') || 'TCO-Report';
  var orig = document.title;
  document.title = 'TCO-' + name.replace(/[^a-zA-Z0-9-]/g, '-') + '-' + new Date().toISOString().substring(0, 10);
  window.print();
  document.title = orig;
}

function computeEc2BaselineSpend() {
  var s = 0, iSel = document.getElementById('input-instance-type'), nR = parseInt((document.getElementById('input-replica-count') || { value: '0' }).value) || 0;
  if (iSel && nR > 0 && window._ec2PriceMap) { var p = window._ec2PriceMap[iSel.value]; if (p > 0) s += p * 730 * nR; }
  var _cp = window._containerPriceData || {}, pods = parseInt((document.getElementById('input-pods') || { value: '0' }).value) || 0, ur = (document.getElementById('input-underlying-resource') || { value: '' }).value || '';
  if (pods > 0 && ur.indexOf('fargate') < 0) { var eN = Math.max(1, Math.ceil(pods / 10)), nT = (document.getElementById('input-ec2-instance') || { value: 'm6i.large' }).value || 'm6i.large', nP = (_cp.ec2OnDemandPrices && _cp.ec2OnDemandPrices[nT]) || 0.096; s += nP * 730 * eN; }
  return s > 0 ? Math.round(s * 100) / 100 : null;
}

function autoEnableAlb() {
  var cb = document.getElementById('tactic-alb');
  if (!cb || cb.checked) return;
  cb.checked = true;
  var ctBody = document.getElementById('body-cloud-tactics');
  if (ctBody && ctBody.style.display === 'none') toggleCategory('cloud-tactics');
  if (!window._albData) loadAlbSection();
  showToast('✓ ALB auto-enabled in Cloud Tactics — required for load balancing.');
}

function wireAllHandlers() {
  function on(id, ev, fn) { var el = document.getElementById(id); if (el) el.addEventListener(ev, fn); }

  on('serviceName', 'input', function () { schedulePhase1Save(); });
  on('servicePurpose', 'input', function () { schedulePhase1Save(); });
  on('requestsPerSecond', 'input', function () {
    var el = document.getElementById('requestsPerSecond');
    if (el && el.value.indexOf('.') >= 0) el.value = String(Math.floor(parseFloat(el.value) || 0));
    recalculateRps(); schedulePhase1Save();
    /* Sync C_req default with new base RPS */
    var cqEl = document.getElementById('input-max-req-per-replica');
    if (cqEl && !cqEl.dataset.userSet) {
      cqEl.value = el ? el.value : '';
      cqEl.placeholder = el && el.value ? 'from RPS: ' + el.value : '';
      recalculateReplicas();
    }
  });
  on('revenuePerTransaction', 'input', function () {
    if (this.value) sessionStorage.setItem('svc_revenue_per_tx', this.value);
    else sessionStorage.removeItem('svc_revenue_per_tx');
  });

  on('customBucName', 'input', function () { selectBusinessUseCase('CUSTOM', null); });
  on('protoFilePhase2', 'change', function () { handlePhase2ProtoSelect(this); });
  on('numConsumers', 'input', function () { schedulePhase2Save(); updatePhase2NextButton(); });
  on('consumerType', 'change', function () { schedulePhase2Save(); });

  // BUC cards — use explicit values to avoid closure-over-loop-variable bug
  on('buc-card-1', 'click', function () { selectBusinessUseCase('BUC1', this); });
  on('buc-card-2', 'click', function () { selectBusinessUseCase('BUC2', this); });
  on('buc-card-3', 'click', function () { selectBusinessUseCase('BUC3', this); });
  on('buc-card-4', 'click', function () { selectBusinessUseCase('BUC4', this); });

  on('tactic-timeout', 'change', function () { var el = document.getElementById('input-timeout'); if (el) el.disabled = !this.checked; scheduleSessionSave(); });
  on('input-timeout', 'input', function () { scheduleSessionSave(); });
  on('tactic-retry', 'change', function () { toggleRetry(this.checked); });
  on('input-retry-error-pct', 'input', function () { recalculateRps(); scheduleSessionSave(); });
  on('tactic-cb', 'change', function () { toggleCircuitBreakerParams(this.checked); scheduleSessionSave(); });
  ['input-cb-min-calls', 'input-cb-half-open', 'input-cb-wait', 'input-cb-failure-rate'].forEach(function (id) {
    on(id, 'input', function () { scheduleSessionSave(); });
  });

  on('tactic-saga', 'change', function () { toggleSagaParams(this.checked); });
  on('tactic-saga-external-vpc', 'change', function () { recalculateRps(); scheduleSessionSave(); });
  ['input-saga-compensatable', 'input-saga-retriable', 'input-saga-pivot'].forEach(function (id) {
    on(id, 'input', function () { recalculateRps(); scheduleSessionSave(); });
  });

  on('tactic-tls', 'change', function () { toggleTlsOptions(); });
  on('tactic-mtls', 'change', function () { toggleTlsOptions(); });
  on('input-tls-reconnects', 'input', function () { recalculateRps(); scheduleSessionSave(); });

  on('tactic-oauth', 'change', function () { toggleOAuthParams(this.checked); enforceAuthMutualExclusivityBetweenOAuthAndBasicCheckboxes('oauth'); });
  on('tactic-basic-auth', 'change', function () { enforceAuthMutualExclusivityBetweenOAuthAndBasicCheckboxes('basic'); scheduleSessionSave(); });
  on('input-token-validation', 'change', function () { recalculateRps(); scheduleSessionSave(); });
  on('input-token-ttl', 'input', function () { recalculateRps(); scheduleSessionSave(); });
  on('input-concurrent-clients', 'input', function () { recalculateRps(); scheduleSessionSave(); });
  on('input-interceptor-type', 'change', function () { scheduleSessionSave(); });

  // Navigation buttons
  on('btn-phase1-continue', 'click', function () { goToPhase(2); });
  on('btn-phase2-back', 'click', function () { goToPhase(1); });
  on('btn-phase3-back', 'click', function () { goToPhase(2); });
  on('phase2NextBtn', 'click', function () { goToPhase(3); });
  on('phase3CalcBtn', 'click', function () { submitProtoAndCalculate(); });

  on('tactic-client-lb', 'change', function () {
    if (document.getElementById('tactic-client-lb').checked) autoEnableAlb();
    recalculateRps(); scheduleSessionSave(); updateCloudTacticsBadge();
  });
  on('tactic-server-lb', 'change', function () {
    if (document.getElementById('tactic-server-lb').checked) autoEnableAlb();
    recalculateRps(); scheduleSessionSave(); updateCloudTacticsBadge();
  });

  on('tactic-alb', 'change', function () { recalculateAlb(); updateCloudTacticsBadge(); recalculateRps(); });
  on('input-alb-count', 'input', function () { recalculateAlb(); });
  on('input-alb-lcu', 'input', function () { recalculateAlb(); });

  on('input-sla', 'change', function () { recalculateAvailability(); });
  on('input-db-engine', 'change', function () { renderDbOptions(); });

  on('input-ec2-instance', 'change', function () { recalculateReplicas(); });
  on('input-max-req-per-replica', 'input', function () {
    var el = document.getElementById('input-max-req-per-replica');
    if (el) el.dataset.userSet = '1';
    recalculateReplicas();
  });
}

/* =====================================================================
   DOM READY — single registration
===================================================================== */
document.addEventListener('DOMContentLoaded', function () {

  wireAllHandlers();
  renderPhase5Portfolio();

  var zone2 = document.getElementById('fileUploadZone2');
  if (zone2) {
    ['dragover', 'dragenter'].forEach(function (ev) {
      zone2.addEventListener(ev, function (e) { e.preventDefault(); zone2.classList.add('dragover'); });
    });
    ['dragleave', 'drop'].forEach(function (ev) {
      zone2.addEventListener(ev, function (e) { e.preventDefault(); zone2.classList.remove('dragover'); });
    });
  }

  var form = document.getElementById('uploadForm');
  if (form) {
    form.addEventListener('submit', function () {
      saveTacticsToSession();
      var lb = document.getElementById('loadingBar');
      if (lb) lb.classList.add('visible');
    });
  }

  var serverMsg = document.getElementById('serverUploadMessage');
  var serverResults = document.getElementById('serverResultsSection');
  if (serverResults) {
    serverResults.classList.remove('hidden');
    serverResults.classList.add('visible');
    document.querySelectorAll('.phase-card').forEach(function (c) { c.classList.remove('visible'); });
    currentPhase = 4;
    updateStepIndicators();

    var rows = serverResults.querySelectorAll('.results-table tbody tr');
    var rawCost = null;
    rows.forEach(function (row) {
      var cells = row.querySelectorAll('td');
      if (cells.length < 3) return;
      var t = cells[0].textContent.trim();
      var effB = parseInt((cells[2].textContent || '').replace(/[^0-9]/g, '')) || 0;
      if (t === 'Request' && effB > 0) window._lastProtoReqBytes = effB;
      if (t === 'Response' && effB > 0) window._lastProtoRespBytes = effB;
      if (t === 'Response') rawCost = (cells[cells.length - 1].textContent || '').trim().replace(/[^0-9.]/g, '');
    });

    var rpsSpans = serverResults.querySelectorAll('.rps-val-number');
    var rendEffRps = null;
    if (rpsSpans.length >= 2) rendEffRps = parseInt((rpsSpans[1].textContent || '').replace(/[^0-9]/g, '')) || null;
    else if (rpsSpans.length === 1) rendEffRps = parseInt((rpsSpans[0].textContent || '').replace(/[^0-9]/g, '')) || null;
    if (!rendEffRps) rendEffRps = parseInt(sessionStorage.getItem('svc_rps')) || null;

    if (rawCost && parseFloat(rawCost) > 0) populateUnitEconomics(rawCost, rendEffRps, null);
    if (serverMsg) serverMsg.style.display = 'flex';
  }

  (function restoreSession() {
    fetch('/api/session/tactics').then(function (res) {
      if (!res.ok) throw new Error('not ok');
      return res.json();
    }).then(function (dto) {
      if (!dto || dto.requestsPerSecond === 0) return;
      var s = function (id, v) { var el = document.getElementById(id); if (el) el.checked = !!v; };
      var v = function (id, val) { var el = document.getElementById(id); if (el) el.value = val || ''; };

      v('requestsPerSecond', dto.requestsPerSecond);
      sessionStorage.setItem('svc_rps', dto.requestsPerSecond);
      // Restore phase 2 identity from sessionStorage if not in session DTO
      var _savedBuc = sessionStorage.getItem('svc_buc');
      if (_savedBuc && !selectedBUC) {
        selectedBUC = _savedBuc;
        // Re-select the matching BUC card visually
        document.querySelectorAll('.buc-card').forEach(function (c) { c.classList.remove('selected'); });
        var _bucMap = { 'BUC1': 'buc-card-1', 'BUC2': 'buc-card-2', 'BUC3': 'buc-card-3', 'BUC4': 'buc-card-4' };
        var _card = document.getElementById(_bucMap[_savedBuc] || '');
        if (_card) _card.classList.add('selected');
        var ps = document.getElementById('phase2-proto-section');
        var cs = document.getElementById('phase2-consumers-section');
        if (ps) ps.style.display = 'block';
        if (cs) cs.style.display = 'block';
      }
      var _savedCons = sessionStorage.getItem('svc_consumers');
      if (_savedCons) {
        v('numConsumers', _savedCons);
        sessionStorage.setItem('svc_consumers', _savedCons);
      }
      var _savedCtype = sessionStorage.getItem('svc_consumer_type');
      if (_savedCtype) {
        var _ctEl2 = document.getElementById('consumerType');
        if (_ctEl2) _ctEl2.value = _savedCtype;
      }

      s('tactic-client-lb', dto.reliabilityTactics.reliabilityClientSideLoadBalancerTactic);
      s('tactic-server-lb', dto.reliabilityTactics.reliabilityServerSideLoadBalancerTactic);

      s('tactic-timeout', dto.timeoutTactic.resiliencyTimeoutTactic);
      v('input-timeout', dto.timeoutTactic.tacticTimeoutMilliseconds);
      var tout = document.getElementById('input-timeout'); if (tout) tout.disabled = !dto.timeoutTactic.resiliencyTimeoutTactic;

      s('tactic-retry', dto.retryTactic.resiliencyRetryTactic);
      if (dto.retryTactic.tacticRetryErrorPct) v('input-retry-error-pct', dto.retryTactic.tacticRetryErrorPct);
      var ri = document.getElementById('input-retry-error-pct'); if (ri) ri.disabled = !dto.retryTactic.resiliencyRetryTactic;

      s('tactic-cb', dto.circuitBreakerTactic.resiliencyCircuitBreakerPattern);
      if (dto.circuitBreakerTactic.resiliencyCircuitBreakerPattern) {
        toggleCbParams(true);
        v('input-cb-min-calls', dto.circuitBreakerTactic.circuitBreakerPatternMinimumCalls);
        v('input-cb-half-open', dto.circuitBreakerTactic.circuitBreakerHalfOpen);
        v('input-cb-wait', dto.circuitBreakerTactic.circuitBreakerWaitMilliseconds);
        v('input-cb-failure-rate', dto.circuitBreakerTactic.circuitBreakerFailureRate);
      }

      s('tactic-saga', dto.sagaPattern.microservicesSAGAPattern);
      if (dto.sagaPattern.microservicesSAGAPattern) {
        toggleSagaParams(true);
        v('input-saga-compensatable', dto.sagaPattern.sagaCompensatableTransactions);
        v('input-saga-retriable', dto.sagaPattern.sagaRetriableTransactions);
        v('input-saga-pivot', dto.sagaPattern.sagaPivotTransactions);
      }

      if (dto.securityTactics) {
        var sec = dto.securityTactics;
        if (sec.tlsTactic) {
          s('tactic-tls', sec.tlsTactic.tlsEnabled);
          s('tactic-mtls', sec.tlsTactic.mtlsEnabled);
          if (sec.tlsTactic.tlsEnabled || sec.tlsTactic.mtlsEnabled) {
            toggleTlsOptions();
            v('input-tls-reconnects', sec.tlsTactic.tlsReconnectsPerHour);
          }
        }
        if (sec.jwtTactic && sec.jwtTactic.oauthJwtEnabled) {
          s('tactic-oauth', true);
          toggleOAuthParams(true);
          var tvEl = document.getElementById('input-token-validation'); if (tvEl) tvEl.value = sec.jwtTactic.tokenValidationMode || 'LOCAL';
          v('input-token-ttl', sec.jwtTactic.tokenTtlSeconds);
          v('input-concurrent-clients', sec.jwtTactic.concurrentClients);
          var itEl = document.getElementById('input-interceptor-type'); if (itEl) itEl.value = sec.jwtTactic.interceptorType || 'UNARY';
        }
        if (sec.basicAuthenticationPattern) s('tactic-basic-auth', sec.basicAuthenticationPattern.basicAuthEnabled);
      }

      recalculateRps();
      if (currentPhase === 4) populateReportSummary();
      var _sr = sessionStorage.getItem('svc_revenue_per_tx');
      if (_sr) {
        var _rfe = document.getElementById('revenuePerTransaction');
        if (_rfe && !_rfe.value) _rfe.value = _sr;
      }
      renderFinOpsAllocationAndTaggingStrategy();
    }).catch(function () { });
  })();

});