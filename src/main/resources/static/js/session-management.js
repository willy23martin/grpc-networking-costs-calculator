let sessionSaveDebounceTimer = null;

function scheduleSessionSave() {
  clearTimeout(sessionSaveDebounceTimer);

  const DEBOUNCE_DELAY_MILLISECONDS = 400;
  sessionSaveDebounceTimer = setTimeout(saveTacticsToSession, DEBOUNCE_DELAY_MILLISECONDS);
}

function schedulePhase1Save() {
  scheduleSessionSave();
  updateStepIndicators();

  const serviceNameInputField = document.getElementById('serviceName');
  const servicePurposeInputField = document.getElementById('servicePurpose');
  const requestsPerSecondInputField = document.getElementById('requestsPerSecond');
  const revenuePerTransactionInputField = document.getElementById('revenuePerTransaction');

  if (serviceNameInputField) {
    sessionStorage.setItem('svc_name', serviceNameInputField.value);
  }
  if (servicePurposeInputField) {
    sessionStorage.setItem('svc_purpose', servicePurposeInputField.value);
  }

  if (requestsPerSecondInputField) {
    const rawRequestsPerSecond = parseFloat(requestsPerSecondInputField.value) || 0;
    const sanitizedIntegerRequestsPerSecond = Math.max(0, Math.floor(rawRequestsPerSecond));

    // UI Correction enforcement loop preventing fraction inputs for execution frequency
    if (rawRequestsPerSecond !== sanitizedIntegerRequestsPerSecond) {
      requestsPerSecondInputField.value = String(sanitizedIntegerRequestsPerSecond);
    }
    sessionStorage.setItem('svc_rps', String(sanitizedIntegerRequestsPerSecond));
  }

  if (revenuePerTransactionInputField && revenuePerTransactionInputField.value) {
    sessionStorage.setItem('svc_revenue_per_tx', revenuePerTransactionInputField.value);
  } else {
    sessionStorage.removeItem('svc_revenue_per_tx');
  }
}

function schedulePhase2Save() {
  scheduleSessionSave();
  updateStepIndicators();

  const consumerCountInputField = document.getElementById('numConsumers');
  const consumerTypeSelectField = document.getElementById('consumerType');

  if (consumerCountInputField && consumerCountInputField.value) {
    sessionStorage.setItem('svc_consumers', consumerCountInputField.value);
  }
  if (consumerTypeSelectField && consumerTypeSelectField.value) {
    sessionStorage.setItem('svc_consumer_type', consumerTypeSelectField.value);
  }
}

function saveTacticsToSession() {
  const structuredDataTransferObject = collectTacticsDTO();

  return fetch('/api/session/tactics', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(structuredDataTransferObject)
  })
  .catch((error) => {
    console.warn('Session save skipped due to networking/infrastructure boundary failure:', error);
  });
}

function collectTacticsDTO() {
  // Pure state selector checking fallback element objects safely
  const getUiElementOrEmptyObject = (elementId) => document.getElementById(elementId) || {};
  const isChecked = (elementId) => !!(getUiElementOrEmptyObject(elementId).checked);
  const getParsedInteger = (elementId, numericalBaseline = 0) => {
    return parseInt(getUiElementOrEmptyObject(elementId).value, 10) || numericalBaseline;
  };

  const requestsPerSecond = getParsedInteger('requestsPerSecond');

  return {
    requestsPerSecond: requestsPerSecond,

    reliabilityTactics: {
      reliabilityClientSideLoadBalancerTactic: isChecked('tactic-client-lb'),
      reliabilityServerSideLoadBalancerTactic: isChecked('tactic-server-lb')
    },

    timeoutTactic: {
      resiliencyTimeoutTactic: isChecked('tactic-timeout'),
      tacticTimeoutMilliseconds: getParsedInteger('input-timeout')
    },

    retryTactic: calculateResiliencyRetryParameters(requestsPerSecond),

    circuitBreakerTactic: {
      resiliencyCircuitBreakerPattern: isChecked('tactic-cb'),
      circuitBreakerPatternMinimumCalls: getParsedInteger('input-cb-min-calls'),
      circuitBreakerHalfOpen: getParsedInteger('input-cb-half-open'),
      circuitBreakerWaitMilliseconds: getParsedInteger('input-cb-wait'),
      circuitBreakerFailureRate: getParsedInteger('input-cb-failure-rate')
    },

    sagaPattern: {
      microservicesSAGAPattern: isChecked('tactic-saga'),
      sagaExternalVpc: isChecked('tactic-saga-external-vpc'),
      sagaCompensatableTransactions: getParsedInteger('input-saga-compensatable'),
      sagaRetriableTransactions: getParsedInteger('input-saga-retriable'),
      sagaPivotTransactions: getParsedInteger('input-saga-pivot')
    },

    securityTactics: {
      tlsTactic: {
        tlsEnabled: isChecked('tactic-tls'),
        mtlsEnabled: isChecked('tactic-mtls'),
        tlsReconnectsPerHour: getParsedInteger('input-tls-reconnects')
      },
      jwtTactic: {
        oauthJwtEnabled: isChecked('tactic-oauth'),
        tokenValidationMode: getUiElementOrEmptyObject('input-token-validation').value || 'LOCAL',
        tokenTtlSeconds: getParsedInteger('input-token-ttl', 3600),
        concurrentClients: getParsedInteger('input-concurrent-clients', 1),
        interceptorType: getUiElementOrEmptyObject('input-interceptor-type').value || 'UNARY'
      },
      basicAuthenticationPattern: {
        basicAuthEnabled: isChecked('tactic-basic-auth')
      }
    }
  };
}

function calculateResiliencyRetryParameters(baselineRequestsPerSecond) {
  const getUiElementOrEmptyObject = (elementId) => document.getElementById(elementId) || {};
  const isChecked = (elementId) => !!(getUiElementOrEmptyObject(elementId).checked);
  const getParsedInteger = (elementId) => parseInt(getUiElementOrEmptyObject(elementId).value, 10) || 0;

  const failureRatePercentageThreshold = parseFloat(getUiElementOrEmptyObject('input-retry-error-pct').value) || 5;

  const isSagaDistributedTransactionActive = isChecked('tactic-saga') && isChecked('tactic-saga-external-vpc');
  let compositeTransactionStepsMultiplier = 0;

  if (isSagaDistributedTransactionActive) {
    compositeTransactionStepsMultiplier = getParsedInteger('input-saga-compensatable') +
                                           getParsedInteger('input-saga-retriable') +
                                           getParsedInteger('input-saga-pivot');
  }

  // Evaluate the effective workload load adjustments introduced by nested choreography steps
  const effectiveBaseTrafficLoad = compositeTransactionStepsMultiplier > 0
    ? baselineRequestsPerSecond * compositeTransactionStepsMultiplier
    : baselineRequestsPerSecond;

  const totalCalculatedRetryInvocations = Math.round((effectiveBaseTrafficLoad * failureRatePercentageThreshold) / 100);

  return {
    resiliencyRetryTactic: isChecked('tactic-retry'),
    tacticRetryErrorPct: failureRatePercentageThreshold,
    tacticRetryTimes: totalCalculatedRetryInvocations
  };
}