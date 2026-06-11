var tacticMissionCriticalCheckbox = document.getElementById('tactic-mission-critical');
var missionCriticalNoteElement = document.getElementById('p4-mission-critical-note');
var finopsNotesElement = document.getElementById('p4-finops-notes');
var runningTotalCost = 0;
var hoursInMonth = 730;
var billingMonthsInYear = 12;

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

  // Compute RPS-impact details for the summary card in Phase 4
  var rpsImpacts = [];
  var numericRps = parseInt(requestsPerSecond) || 0;

  if (document.getElementById('tactic-retry') && document.getElementById('tactic-retry').checked) {
    var retryErrorPercentage = parseInt((document.getElementById('input-retry-error-pct') || { value: '5' }).value) || 5;

    var extraRetryRequests = Math.round(numericRps * retryErrorPercentage / 100);

    rpsImpacts.push('Retry: +' + extraRetryRequests.toLocaleString() + ' req/s (' + retryErrorPercentage + '% of ' + effectiveBaseRpsForRetry.toLocaleString() + ' eff.RPS)');
  }

  if (document.getElementById('tactic-tls') && document.getElementById('tactic-tls').checked) {
    rpsImpacts.push('TLS: +29 B/frame on responses (RFC 8446 AES-GCM) \u2192 increases egress GB cost');
  }
  if (document.getElementById('tactic-mtls') && document.getElementById('tactic-mtls').checked) {
    rpsImpacts.push('mTLS: +29 B/frame on responses (RFC 8446) + 5 handshake msgs/reconnect');
  }

  if (document.getElementById('tactic-oauth') && document.getElementById('tactic-oauth').checked) {
    var tokenTtlSeconds = parseInt((document.getElementById('input-token-ttl') || { value: '3600' }).value) || 3600;
    var concurrentClients = parseInt((document.getElementById('input-concurrent-clients') || { value: '1' }).value) || 1;
    var tokenValidationMode = (document.getElementById('input-token-validation') || { value: 'LOCAL' }).value || 'LOCAL';

    var tokenAcquisitionsPerSecond = Math.round(numericRps / (tokenTtlSeconds * concurrentClients));
    var remoteIntrospectionRequestsPerSecond = tokenValidationMode === 'REMOTE_INTROSPECTION' ? numericRps : 0;

    var oauthImpactDetails = '+650 B/req JWT header (RFC 7519, request-side \u2014 AWS inbound free)';
    if (tokenAcquisitionsPerSecond > 0) oauthImpactDetails += ', +' + tokenAcquisitionsPerSecond + ' token acq/s';
    if (remoteIntrospectionRequestsPerSecond > 0) oauthImpactDetails += ', +' + remoteIntrospectionRequestsPerSecond.toLocaleString() + ' remote introspection/s';

    rpsImpacts.push('OAuth2+JWT: ' + oauthImpactDetails);
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
}

function computeCloudInfraCost() {
  runningTotalCost = 0;   /* ← MUST reset each call; module-level var accumulates otherwise */
  if (document.getElementById('tactic-alb') && document.getElementById('tactic-alb').checked) {
    if (window._albData) {
      var albCount = parseInt((document.getElementById('input-alb-count') || { value: '1' }).value) || 1;
      var albLcuCount = parseFloat((document.getElementById('input-alb-lcu') || { value: '0' }).value) || 0;
      runningTotalCost += (window._albData.fixedPerMonthUsd * albCount) + (window._albData.lcuPerHourUsd * albLcuCount * hoursInMonth * albCount);
    } else {
      runningTotalCost += parseFloat(sessionStorage.getItem('tco_alb_cost') || '0');
    }
  }

  /* ── ElastiCache Distributed Caching Cost ── */
  if (document.getElementById('tactic-cache') && document.getElementById('tactic-cache').checked) {
    if (window._cacheData) {
      var cacheEngine = (document.getElementById('input-cache-engine') || { value: 'redis' }).value || 'redis';
      var cacheNodeInstanceType = (document.getElementById('input-cache-node') || { value: 'r6g.large' }).value || 'r6g.large';
      var cacheNodesCount = parseInt((document.getElementById('input-cache-nodes') || { value: '1' }).value) || 1;

      var cachePriceMap = {
        'redisr6glarge': window._cacheData.redisR6gLargePerHour,
        'redisr6gxlarge': window._cacheData.redisR6gXlargePerHour,
        'redisr6g2xlarge': window._cacheData.redisR6g2xlargePerHour,
        'memcachedr6glarge': window._cacheData.memcachedR6gLargePerHour,
        'memcachedr6gxlarge': window._cacheData.memcachedR6gXlargePerHour
      };

      var normalizedNodeKey = cacheEngine + cacheNodeInstanceType.replace(/\./g, '').replace('cache', '');
      var cachePricePerHour = cachePriceMap[normalizedNodeKey] || cachePriceMap[cacheEngine + 'r6glarge'] || 0.166;

      runningTotalCost += cachePricePerHour * hoursInMonth * cacheNodesCount;
    } else {
      runningTotalCost += parseFloat(sessionStorage.getItem('tco_cache_cost') || '0');
    }
  }

  /* ── Database Backup Storage Cost (S3 Standard) ── */
  if (document.getElementById('tactic-s3-backup') && document.getElementById('tactic-s3-backup').checked) {
    if (window._dbData) {
      var databaseGigabytes = parseFloat((document.getElementById('db-gb') || { value: '10' }).value) || 10;
      runningTotalCost += databaseGigabytes * (window._dbData.s3StandardPerGbMonth || 0.023);
    } else {
      runningTotalCost += parseFloat(sessionStorage.getItem('tco_db_cost') || '0') * 0.5;
    }
  }

  /* ── Aurora Database Read Replica Cost ── */
  if (document.getElementById('tactic-aurora-replica') && document.getElementById('tactic-aurora-replica').checked) {
    if (window._dbData) {
      var auroraReplicaCount = parseInt((document.getElementById('aurora-replica-count') || { value: '1' }).value) || 1;
      runningTotalCost += (window._dbData.auroraReplicaPerHour || 0.26) * hoursInMonth * auroraReplicaCount;
    } else {
      runningTotalCost += parseFloat(sessionStorage.getItem('tco_db_cost') || '0');
    }
  }

  /* ── AWS Security Services Cost Model ── */
  if (!window._secData) {
    runningTotalCost += parseFloat(sessionStorage.getItem('tco_sec_cost') || '0');
  } else {
    var baseRequestsPerSecond = parseInt((document.getElementById('requestsPerSecond') || { value: '0' }).value) || 0;
    var totalRequestsPerMonth = baseRequestsPerSecond * 2592000;
    var getElementReference = function (id) { return document.getElementById(id) || {}; };
    var securityServicesCost = 0;

    if (getElementReference('sec-guardduty').checked) {
      var guardDutyLogsGb = parseFloat(getElementReference('input-guardduty-gb').value) || 0;
      securityServicesCost += Math.max(0, guardDutyLogsGb - 500) * window._secData.guardDutyPerGbLogs;
    }
    if (getElementReference('sec-inspector').checked) {
      var inspectorInstances = parseInt(getElementReference('input-inspector-instances').value) || 1;
      securityServicesCost += inspectorInstances * window._secData.inspectorPerInstanceMonth;
    }
    if (getElementReference('sec-waf').checked) {
      var wafRulesCount = parseInt(getElementReference('input-waf-rules').value) || 5;
      securityServicesCost += window._secData.wafWebAclPerMonth + (wafRulesCount * window._secData.wafRulePerMonth) + ((totalRequestsPerMonth / 1000000) * window._secData.wafPer1MRequests);
    }
    if (getElementReference('sec-macie').checked) {
      var macieClassifiedGb = parseFloat(getElementReference('input-macie-gb').value) || 0;
      securityServicesCost += Math.max(0, macieClassifiedGb - 1) * window._secData.maciePerGbClassified;
    }
    if (getElementReference('sec-cloudwatch').checked) {
      var cloudwatchLogsGb = parseFloat(getElementReference('input-cw-gb').value) || 1;
      securityServicesCost += (cloudwatchLogsGb * window._secData.cloudwatchLogsIngestionPerGb) + (cloudwatchLogsGb * window._secData.cloudwatchLogsStoragePerGbMonth);
    }
    if (getElementReference('sec-audit').checked) {
      var activeAssessments = parseInt(getElementReference('input-audit-assessments').value) || 1;
      securityServicesCost += activeAssessments * window._secData.auditManagerPerAssessmentMonth;
    }
    if (getElementReference('sec-kms').checked) {
      var kmsKeysCount = parseInt(getElementReference('input-kms-keys').value) || 1;
      securityServicesCost += (kmsKeysCount * window._secData.kmsCmkPerMonth) + ((totalRequestsPerMonth / 10000) * window._secData.kmsApiCallsPer10k);
    }

    runningTotalCost += securityServicesCost;
  }

  /* ── Base Container & Shared Infrastructure Additions ── */
  runningTotalCost += parseFloat(sessionStorage.getItem('tco_container_cost') || '0');
  runningTotalCost += parseFloat(sessionStorage.getItem('tco_apigw_cost') || '0');

  /* ── FinOps Rate Optimization Savings Deduced ── */
  var finopsSavingAmount = parseFloat(sessionStorage.getItem('tco_finops_saving') || '0');
  runningTotalCost = Math.max(0, runningTotalCost - finopsSavingAmount);

  /* ── Target EC2 Compute Node Allocation (Scalability Strategy) ── */
  var ec2InstanceSelectElement = document.getElementById('input-ec2-instance');
  var replicaDisplayElement = document.getElementById('replica-count-display');
  var ec2ReplicaCost = 0;

  if (ec2InstanceSelectElement && ec2InstanceSelectElement.value) {
    var rawInstanceValueParts = ec2InstanceSelectElement.value.split('|');
    var targetInstanceName = rawInstanceValueParts[0];
    var currentReplicaCount = replicaDisplayElement ? parseInt(replicaDisplayElement.textContent) || 0 : 0;

    if (currentReplicaCount > 0) {
      var ec2PriceLookupMap = window._ec2PriceMap || ((window._containerPriceData && window._containerPriceData.ec2OnDemandPrices) || {});
      var resolvedInstancePrice = ec2PriceLookupMap[targetInstanceName] || (Object.keys(ec2PriceLookupMap).filter(function (key) { return key.indexOf(targetInstanceName) >= 0; })[0] && ec2PriceLookupMap[Object.keys(ec2PriceLookupMap).filter(function (key) { return key.indexOf(targetInstanceName) >= 0; })[0]]) || 0.096;
      ec2ReplicaCost = resolvedInstancePrice * hoursInMonth * currentReplicaCount;
    }
  }

  runningTotalCost += ec2ReplicaCost;
  return Math.round(runningTotalCost * 100) / 100;
}

function populateUnitEconomics(transferCostUsd, effectiveRps, requestsPerMonthRaw) {
  var unitEconGridElement = document.getElementById('unitEconGrid');
  if (!unitEconGridElement || !transferCostUsd) return;

  var egressDataTransferCost = parseFloat(transferCostUsd);
  var calculatedCloudInfraCost = computeCloudInfraCost();
  var rawCalculatedTotalCost = Math.round((egressDataTransferCost + calculatedCloudInfraCost) * 100) / 100;

  var numConsumersElement = document.getElementById('numConsumers');
  var fallbackConsumerCount = (numConsumersElement && numConsumersElement.value) ? numConsumersElement.value : (sessionStorage.getItem('svc_consumers') || '1');
  var totalConsumersCount = parseInt(fallbackConsumerCount) || 1;

  var consumerTypeSelectElement = document.getElementById('consumerType');
  var calculatedConsumerTypeValue = (consumerTypeSelectElement && consumerTypeSelectElement.value) ? consumerTypeSelectElement.value : (sessionStorage.getItem('svc_consumer_type') || 'SERVICES');

  if (consumerTypeSelectElement && !consumerTypeSelectElement.value && calculatedConsumerTypeValue) {
    consumerTypeSelectElement.value = calculatedConsumerTypeValue;
  }

  var baselineRequestsPerSecond = parseInt((document.getElementById('requestsPerSecond') || { value: '1' }).value) || 1;
  var resolvedRps = effectiveRps || baselineRequestsPerSecond;
  var monthlyRequestsVolume = requestsPerMonthRaw || Math.round(resolvedRps * 60 * 60 * 24 * 30);

  var businessUseCaseLabel = selectedBUC ? (selectedBUC.indexOf('CUSTOM:') === 0 ? selectedBUC.replace('CUSTOM:', '') : selectedBUC) : '\u2014';

  var consumerTypeLabelMap = {
    SERVICES: 'Microservices / APIs',
    USERS: 'End Users',
    BOTH: 'Mixed (Services + Users)'
  };
  var displayConsumerTypeLabel = consumerTypeLabelMap[calculatedConsumerTypeValue] || 'consumers';

  var smallValueFormatter = function (value, precisionDigits) {
    return (value > 0 && value < Math.pow(10, -(precisionDigits - 1))) ? value.toExponential(4) : value.toFixed(precisionDigits);
  };

  /* ── Parse Component Grid Allocation ── */
  var cloudTcoBreakdownContainer = document.getElementById('cloudTcoBreakdown');
  if (cloudTcoBreakdownContainer) {
    var costComponentsCollection = [];

    costComponentsCollection.push({
      label: 'AWS Egress (Response Transfer)',
      cat: 'Networking',
      badge: 'badge-bytes',
      cost: egressDataTransferCost
    });

    var isTlsActive = !!(document.getElementById('tactic-tls') && document.getElementById('tactic-tls').checked);
    var isMtlsActive = !!(document.getElementById('tactic-mtls') && document.getElementById('tactic-mtls').checked);
    var isOauthActive = !!(document.getElementById('tactic-oauth') && document.getElementById('tactic-oauth').checked);

    var computedTlsByteOverhead = window._lastTlsBytes || ((isTlsActive || isMtlsActive) ? 29 : 0);
    var computedJwtByteOverhead = window._lastJwtBytes || (isOauthActive ? 650 : 0);

    if ((isTlsActive || isMtlsActive) && computedTlsByteOverhead > 0) {
      costComponentsCollection.push({
        label: (isMtlsActive ? 'mTLS' : 'TLS') + ' frame overhead (+' + computedTlsByteOverhead + ' B/frame, RFC 8446 — included in egress above)',
        cat: 'Security',
        badge: 'badge-dr',
        cost: 0,
        zeroNote: 'TLS overhead is counted within AWS Egress cost above. No additional line charge.'
      });
    }
    if (isOauthActive && computedJwtByteOverhead > 0) {
      costComponentsCollection.push({
        label: 'JWT header overhead (+' + computedJwtByteOverhead + ' B/req, RFC 7519 — request-side, AWS inbound)',
        cat: 'Security',
        badge: 'badge-dr',
        cost: 0,
        zeroNote: 'JWT tokens travel request-side (inbound). AWS does not charge for inbound data transfer.'
      });
    }

    var cloudInfraSubComponents = [];

    /* Segment: ALB */
    var structuralAlbCost = 0;
    if (window._albData && document.getElementById('tactic-alb') && document.getElementById('tactic-alb').checked) {
      var localAlbCount = parseInt((document.getElementById('input-alb-count') || { value: '1' }).value) || 1;
      var localAlbLcu = parseFloat((document.getElementById('input-alb-lcu') || { value: '0' }).value) || 0;
      structuralAlbCost = (window._albData.fixedPerMonthUsd * localAlbCount) + (window._albData.lcuPerHourUsd * localAlbLcu * 730 * localAlbCount);
    } else {
      structuralAlbCost = parseFloat(sessionStorage.getItem('tco_alb_cost') || '0');
    }
    if (structuralAlbCost > 0) cloudInfraSubComponents.push({ n: 'ALB(s)', v: structuralAlbCost });

    /* Segment: ElastiCache */
    var structuralCacheCost = 0;
    if (window._cacheData && document.getElementById('tactic-cache') && document.getElementById('tactic-cache').checked) {
      var localCacheEngine = (document.getElementById('input-cache-engine') || { value: 'redis' }).value || 'redis';
      var localCacheNode = (document.getElementById('input-cache-node') || { value: 'r6g.large' }).value || 'r6g.large';
      var localCacheNodesCount = parseInt((document.getElementById('input-cache-nodes') || { value: '1' }).value) || 1;
      var localCachePriceMap = {
        'redisr6glarge': window._cacheData.redisR6gLargePerHour,
        'redisr6gxlarge': window._cacheData.redisR6gXlargePerHour,
        'redisr6g2xlarge': window._cacheData.redisR6g2xlargePerHour,
        'memcachedr6glarge': window._cacheData.memcachedR6gLargePerHour,
        'memcachedr6gxlarge': window._cacheData.memcachedR6gXlargePerHour
      };
      var targetNormalizedCacheKey = localCacheEngine + localCacheNode.replace(/\./g, '').replace('cache', '');
      structuralCacheCost = (localCachePriceMap[targetNormalizedCacheKey] || localCachePriceMap[localCacheEngine + 'r6glarge'] || 0.166) * 730 * localCacheNodesCount;
    } else {
      structuralCacheCost = parseFloat(sessionStorage.getItem('tco_cache_cost') || '0');
    }
    if (structuralCacheCost > 0) cloudInfraSubComponents.push({ n: 'ElastiCache', v: structuralCacheCost });

    /* Segment: Storage / Engine Relational Clusters */
    var structuralDatabaseCost = 0;
    if (window._dbData) {
      var hasS3BackupActive = document.getElementById('tactic-s3-backup') && document.getElementById('tactic-s3-backup').checked;
      var hasAuroraReplicaActive = document.getElementById('tactic-aurora-replica') && document.getElementById('tactic-aurora-replica').checked;
      if (hasS3BackupActive) {
        var localDbGb = parseFloat((document.getElementById('db-gb') || { value: '10' }).value) || 10;
        structuralDatabaseCost += localDbGb * (window._dbData.s3StandardPerGbMonth || 0.023);
      }
      if (hasAuroraReplicaActive) {
        var localReplicaCount = parseInt((document.getElementById('aurora-replica-count') || { value: '1' }).value) || 1;
        structuralDatabaseCost += (window._dbData.auroraReplicaPerHour || 0.26) * 730 * localReplicaCount;
      }
    } else {
      structuralDatabaseCost = parseFloat(sessionStorage.getItem('tco_db_cost') || '0');
    }
    if (structuralDatabaseCost > 0) cloudInfraSubComponents.push({ n: 'Database/Backup', v: structuralDatabaseCost });

    /* Segment: Enterprise Security Services */
    var structuralSecurityCost = 0;
    if (window._secData) {
      var getLocalReference = function (id) { return document.getElementById(id) || {}; };
      var baseRpsLocal = parseInt((document.getElementById('requestsPerSecond') || { value: '0' }).value) || 0;
      var monthlyRequestsLocal = baseRpsLocal * 2592000;
      if (getLocalReference('sec-guardduty').checked) structuralSecurityCost += Math.max(0, (parseFloat(getLocalReference('input-guardduty-gb').value) || 0) - 500) * window._secData.guardDutyPerGbLogs;
      if (getLocalReference('sec-inspector').checked) structuralSecurityCost += (parseInt(getLocalReference('input-inspector-instances').value) || 1) * window._secData.inspectorPerInstanceMonth;
      if (getLocalReference('sec-waf').checked) structuralSecurityCost += window._secData.wafWebAclPerMonth + (parseInt(getLocalReference('input-waf-rules').value) || 5) * window._secData.wafRulePerMonth + (monthlyRequestsLocal / 1000000) * window._secData.wafPer1MRequests;
      if (getLocalReference('sec-macie').checked) structuralSecurityCost += Math.max(0, (parseFloat(getLocalReference('input-macie-gb').value) || 0) - 1) * window._secData.maciePerGbClassified;
      if (getLocalReference('sec-cloudwatch').checked) { var localCwGb = parseFloat(getLocalReference('input-cw-gb').value) || 1; structuralSecurityCost += localCwGb * window._secData.cloudwatchLogsIngestionPerGb + localCwGb * window._secData.cloudwatchLogsStoragePerGbMonth; }
      if (getLocalReference('sec-audit').checked) structuralSecurityCost += (parseInt(getLocalReference('input-audit-assessments').value) || 1) * window._secData.auditManagerPerAssessmentMonth;
      if (getLocalReference('sec-kms').checked) { var localKmsKeys = parseInt(getLocalReference('input-kms-keys').value) || 1; structuralSecurityCost += localKmsKeys * window._secData.kmsCmkPerMonth + (monthlyRequestsLocal / 10000) * window._secData.kmsApiCallsPer10k; }
    } else {
      structuralSecurityCost = parseFloat(sessionStorage.getItem('tco_sec_cost') || '0');
    }
    if (structuralSecurityCost > 0) cloudInfraSubComponents.push({ n: 'Security Services', v: structuralSecurityCost });

    /* Segment: Compute Node Orchestration */
    var structuralContainerCost = parseFloat(sessionStorage.getItem('tco_container_cost') || '0');
    if (structuralContainerCost > 0) cloudInfraSubComponents.push({ n: 'Containerized Cluster', v: structuralContainerCost });

    /* Segment: Core API Gateway Egress Point */
    var structuralApiGatewayCost = parseFloat(sessionStorage.getItem('tco_apigw_cost') || '0');
    if (structuralApiGatewayCost > 0) cloudInfraSubComponents.push({ n: 'API Gateway', v: structuralApiGatewayCost });

    /* Consolidation step for overall Infrastructure allocation row */
    var totalCloudInfrastructureSum = cloudInfraSubComponents.reduce(function (accumulator, component) { return accumulator + component.v; }, 0);
    if (totalCloudInfrastructureSum > 0) {
      var infrastructureBreakdownDetails = cloudInfraSubComponents.map(function (component) {
        return component.n + ' $' + component.v.toFixed(2);
      }).join(' | ');

      costComponentsCollection.push({
        label: 'Cloud Infrastructure',
        cat: 'Cloud + Container',
        badge: 'badge-warn',
        cost: totalCloudInfrastructureSum,
        detail: infrastructureBreakdownDetails
      });
    }

    /* Rate Optimization Strategy Deduction Line */
    var structuralFinopsSavings = parseFloat(sessionStorage.getItem('tco_finops_saving') || '0');
    if (structuralFinopsSavings > 0) {
      costComponentsCollection.push({
        label: 'FinOps Optimisation (RI/SP saving)',
        cat: 'Cost Reduction',
        badge: 'badge-bc',
        cost: -structuralFinopsSavings,
        isSaving: true
      });
    }

    /* Resolve precise non-negative TCO value across categories */
    var accurateTotalCostOfOwnership = Math.round(costComponentsCollection.reduce(function (accumulator, component) {
      return accumulator + (component.zeroNote ? 0 : component.cost);
    }, 0) * 100) / 100;

    accurateTotalCostOfOwnership = Math.max(0, accurateTotalCostOfOwnership);
    window._lastComputedTco = accurateTotalCostOfOwnership;

    var sumOfPositiveAllocations = costComponentsCollection.filter(function (component) {
      return !component.isSaving && !component.zeroNote && component.cost > 0;
    }).reduce(function (accumulator, component) {
      return accumulator + component.cost;
    }, 0);

    var generatedTcoRowsHtml = costComponentsCollection.map(function (component) {
      var percentageOfTotal = (sumOfPositiveAllocations > 0 && component.cost > 0 && !component.isSaving ? (component.cost / sumOfPositiveAllocations * 100).toFixed(1) : '0.0');
      var costValueCellHtml;

      if (component.isSaving) {
        costValueCellHtml = '<span style="font-family:monospace;color:var(--green);font-weight:700;">-$' + Math.abs(component.cost).toFixed(2) + ' saved</span>';
      } else if (component.zeroNote) {
        costValueCellHtml = '<span style="font-size:.73rem;color:var(--green);font-style:italic;">$0 egress</span>';
      } else {
        costValueCellHtml = '<span style="font-family:monospace;color:var(--blue-deep);">$' + component.cost.toFixed(2) + '</span>';
      }

      var percentageDisplayHtml = component.zeroNote ? '<span style="font-size:.73rem;color:var(--green);">free</span>' : (component.isSaving ? '<span style="color:var(--green);">saving</span>' : percentageOfTotal + '%');
      var informationalRowHtml = component.zeroNote ? '<tr style="background:var(--green-bg);"><td colspan="4" style="padding:3px 12px 8px;font-size:.73rem;color:var(--green);font-style:italic;">\u2139 ' + component.zeroNote + '</td></tr>' : '';
      var subTierDetailHtml = component.detail ? '<div style="font-size:.72rem;color:var(--ink-light);margin-top:2px;">' + component.detail + '</div>' : '';

      return '<tr style="border-bottom:' + (component.zeroNote ? 'none' : '1px solid var(--rule)') + ';"><td style="padding:8px 12px;font-weight:' + (component.isSaving ? '700' : '600') + ';">' + component.label + subTierDetailHtml + '</td>'
        + '<td style="padding:8px 12px;text-align:center;"><span class="warning-badge ' + component.badge + '">' + component.cat + '</span></td>'
        + '<td style="padding:8px 12px;text-align:right;">' + costValueCellHtml + '</td>'
        + '<td style="padding:8px 12px;text-align:right;">' + percentageDisplayHtml + '</td></tr>' + informationalRowHtml;
    }).join('');

    generatedTcoRowsHtml += '<tr style="background:var(--blue-deep);color:#fff;font-weight:700;">'
      + '<td style="padding:9px 12px;" colspan="2">Total Cost of Ownership (TCO)</td>'
      + '<td style="padding:9px 12px;text-align:right;font-family:monospace;">$' + accurateTotalCostOfOwnership.toFixed(2) + '/mo</td>'
      + '<td style="padding:9px 12px;text-align:right;">100.0%</td></tr>';

    cloudTcoBreakdownContainer.innerHTML = '<div style="font-family:\'DM Serif Display\',serif;font-size:1rem;'
      + 'color:var(--blue-deep);margin-bottom:10px;display:flex;align-items:center;gap:8px;">'
      + '<i class="fas fa-table"></i> Total Cost of Ownership (TCO) Breakdown</div>'
      + '<table style="width:100%;border-collapse:collapse;font-size:.84rem;margin-bottom:4px;">'
      + '<thead><tr style="background:var(--paper);">'
      + '<th style="padding:7px 12px;text-align:left;border-bottom:1px solid var(--rule);">Cost Component</th>'
      + '<th style="padding:7px 12px;text-align:center;border-bottom:1px solid var(--rule);">DR / BC</th>'
      + '<th style="padding:7px 12px;text-align:right;border-bottom:1px solid var(--rule);">Monthly Cost</th>'
      + '<th style="padding:7px 12px;text-align:right;border-bottom:1px solid var(--rule);">% of TCO</th>'
      + '</tr></thead><tbody>' + generatedTcoRowsHtml + '</tbody></table>'
      + '<p style="font-size:.73rem;color:var(--ink-light);margin-top:6px;">'
      + '* % of TCO is calculated against the sum of ALL cost rows shown above (must total 100%). '
      + 'DR = Disaster Recovery &nbsp;|&nbsp; BC = Business Continuity.</p>';
  }

  /* ── Master Cost Vector Assignment ── */
  var authoritativeTcoMonthly = (typeof accurateTotalCostOfOwnership !== 'undefined' && accurateTotalCostOfOwnership > 0) ? accurateTotalCostOfOwnership : rawCalculatedTotalCost;
  window._lastComputedTco = authoritativeTcoMonthly;

  var costPerUserMonthly = authoritativeTcoMonthly / totalConsumersCount;
  var costPerRequestVolume = authoritativeTcoMonthly / monthlyRequestsVolume;
  var costPerUserDaily = costPerUserMonthly / 30;

  unitEconGridElement.innerHTML =
    '<div class="unit-econ-item" style="grid-column:1/-1;background:linear-gradient(135deg,#eff6ff,#dbeafe);border:1px solid #93c5fd;">'
    + '<div class="unit-econ-label">Total Monthly TCO (Networking + Cloud Infra)</div>'
    + '<div class="unit-econ-value" style="color:var(--blue-deep);">$' + authoritativeTcoMonthly.toFixed(2) + '</div>'
    + '<div class="unit-econ-sub">'
    + (calculatedCloudInfraCost > 0 ? '$' + egressDataTransferCost.toFixed(2) + ' egress + $' + calculatedCloudInfraCost.toFixed(2) + ' cloud infra' : '$' + egressDataTransferCost.toFixed(2) + ' egress networking only')
    + '</div></div>'

    + '<div class="unit-econ-item" style="grid-column:1/-1;background:var(--paper);border:none;padding:10px 0 4px;">'
    + '<div style="font-family:\'DM Serif Display\',serif;font-size:.9rem;color:var(--blue-deep);'
    + 'border-bottom:1px solid var(--rule);padding-bottom:6px;display:flex;align-items:center;gap:8px;">'
    + '<i class="fas fa-microchip" style="font-size:.85rem;"></i> Per-Microservice Cost Metrics</div></div>'

    + '<div class="unit-econ-item"><div class="unit-econ-label">TCO / Microservice / Month</div>'
    + '<div class="unit-econ-value">$' + authoritativeTcoMonthly.toFixed(2) + '</div>'
    + '<div class="unit-econ-sub">This service · full TCO</div></div>'

    + '<div class="unit-econ-item"><div class="unit-econ-label">Cost / Request</div>'
    + '<div class="unit-econ-value">$' + smallValueFormatter(costPerRequestVolume, 8) + '</div>'
    + '<div class="unit-econ-sub">' + monthlyRequestsVolume.toLocaleString() + ' req/mo</div></div>'

    + '<div class="unit-econ-item"><div class="unit-econ-label">Annual Projection</div>'
    + '<div class="unit-econ-value">$' + (authoritativeTcoMonthly * billingMonthsInYear).toFixed(2) + '</div>'
    + '<div class="unit-econ-sub">' + billingMonthsInYear + '× monthly TCO</div></div>'

    + '<div class="unit-econ-item" style="grid-column:1/-1;background:var(--paper);border:none;padding:10px 0 4px;">'
    + '<div style="font-family:\'DM Serif Display\',serif;font-size:.9rem;color:var(--blue-deep);'
    + 'border-bottom:1px solid var(--rule);padding-bottom:6px;display:flex;align-items:center;gap:8px;">'
    + '<i class="fas fa-users" style="font-size:.85rem;"></i> Per-End-User Cost Metrics'
    + ' <span style="font-size:.72rem;font-weight:400;color:var(--ink-light);">(' + totalConsumersCount.toLocaleString() + ' end users)</span></div></div>'

    + '<div class="unit-econ-item"><div class="unit-econ-label">TCO / End User / Month</div>'
    + '<div class="unit-econ-value">$' + smallValueFormatter(costPerUserMonthly, 4) + '</div>'
    + '<div class="unit-econ-sub">$' + authoritativeTcoMonthly.toFixed(2) + ' ÷ ' + totalConsumersCount.toLocaleString() + ' users</div></div>'

    + '<div class="unit-econ-item"><div class="unit-econ-label">TCO / End User / Day</div>'
    + '<div class="unit-econ-value">$' + smallValueFormatter(costPerUserDaily, 6) + '</div>'
    + '<div class="unit-econ-sub">Monthly ÷ 30</div></div>'

    + '<div class="unit-econ-item"><div class="unit-econ-label">Business Use Case</div>'
    + '<div class="unit-econ-value" style="font-size:.85rem;">' + businessUseCaseLabel + '</div>'
    + '<div class="unit-econ-sub">Pattern cost attribution</div></div>';

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
    if (unitEconGridElement && unitEconGridElement.parentElement) {
      unitEconGridElement.parentElement.appendChild(roiSectionContainerElement);
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
    var generatedMonthlyRevenue = expectedRevenuePerUserElement * totalConsumersCount;
    var arpuValue = expectedRevenuePerUserElement;
    var monthlyRoiValue = rawCalculatedTotalCost > 0 ? ((generatedMonthlyRevenue - rawCalculatedTotalCost) / rawCalculatedTotalCost * 100).toFixed(1) : '--';
    var annualRoiValue = rawCalculatedTotalCost > 0 ? ((generatedMonthlyRevenue * billingMonthsInYear - rawCalculatedTotalCost * billingMonthsInYear) / (rawCalculatedTotalCost * billingMonthsInYear) * 100).toFixed(1) : '--';

    var breakEvenUsersVolume = (expectedRevenuePerUserElement > 0 && rawCalculatedTotalCost > 0) ? Math.ceil(rawCalculatedTotalCost / expectedRevenuePerUserElement).toLocaleString() : '--';
    var infrastructureEfficiencyRatio = (rawCalculatedTotalCost > 0) ? (generatedMonthlyRevenue / rawCalculatedTotalCost).toFixed(2) : '--';
    var netMarginPerUserMonthly = expectedRevenuePerUserElement - costPerUserMonthly;

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
      + '<div class="unit-econ-sub">$' + expectedRevenuePerUserElement.toFixed(2) + '/user \u00d7 ' + totalConsumersCount.toLocaleString() + ' users</div></div>'
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
      + '<div class="unit-econ-sub">12\u00d7 monthly projection</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Break-even Users</div>'
      + '<div class="unit-econ-value">' + breakEvenUsersVolume + '</div>'
      + '<div class="unit-econ-sub">users to cover full TCO</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Revenue / $1 Infra</div>'
      + '<div class="unit-econ-value">$' + infrastructureEfficiencyRatio + '</div>'
      + '<div class="unit-econ-sub">Revenue efficiency</div></div>'
      + '<div class="unit-econ-item"><div class="unit-econ-label">Annual Revenue</div>'
      + '<div class="unit-econ-value">$' + (generatedMonthlyRevenue * billingMonthsInYear).toLocaleString(undefined, { maximumFractionDigits: 2 }) + '</div>'
      + '<div class="unit-econ-sub">' + billingMonthsInYear + '\u00d7 monthly revenue</div></div>'
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