const PORTFOLIO_STORAGE_KEY = 'grpc_tco_portfolio_v1';

function getPortfolio() {
  try {
    const serializedPortfolio = localStorage.getItem(PORTFOLIO_STORAGE_KEY);
    return serializedPortfolio ? JSON.parse(serializedPortfolio) : [];
  } catch (error) {
    console.error('Failed to parse portfolio from localStorage:', error);
    return [];
  }
}

function savePortfolio(portfolioList) {
  try {
    localStorage.setItem(PORTFOLIO_STORAGE_KEY, JSON.stringify(portfolioList));
  } catch (error) {
    console.error('Failed to save portfolio to localStorage:', error);
  }
}

function saveServiceToPortfolio() {
  const serviceName = getInputValueOrSessionBackup('serviceName', 'svc_name', 'Unnamed Service');
  const servicePurpose = getInputValueOrSessionBackup('servicePurpose', 'svc_purpose', '');
  const businessUseCase = sessionStorage.getItem('svc_buc') || '—';

  const requestsPerSecond = parseInt(sessionStorage.getItem('svc_rps') || '0', 10) || 0;
  const activeConsumers = parseInt(sessionStorage.getItem('svc_consumers') || '0', 10) || 0;

  const totalCostOfOwnership = extractServiceTotalCostOfOwnership();
  const selectedTactics = compileActiveArchitecturalTactics();

  // Handle revenue unit economics backups
  const revenueInputField = document.getElementById('revenuePerTransaction');
  const revenuePerUserMonth = (revenueInputField && revenueInputField.value)
    ? parseFloat(revenueInputField.value) || 0
    : parseFloat(sessionStorage.getItem('svc_revenue_per_tx') || '0') || 0;

  // Build and save updated state array, evicting any stale duplicates of the same service name
  let portfolio = getPortfolio();
  portfolio = portfolio.filter(service => service.name !== serviceName);

  portfolio.push({
    id: Date.now(),
    name: serviceName,
    purpose: servicePurpose.substring(0, 150),
    buc: businessUseCase,
    rps: requestsPerSecond,
    consumers: activeConsumers,
    tactics: selectedTactics,
    tco: totalCostOfOwnership,
    revenuePerUserMonth: revenuePerUserMonth,
    timestamp: new Date().toLocaleDateString()
  });

  savePortfolio(portfolio);

  triggerSaveVisualConfirmationButton(serviceName, totalCostOfOwnership);
  renderPhase5Portfolio();
  showPortfolioNextDialog(serviceName, totalCostOfOwnership);
}

function getInputValueOrSessionBackup(elementId, sessionKey, defaultValue) {
  const domElement = document.getElementById(elementId);
  if (domElement && domElement.value.trim()) {
    return domElement.value.trim();
  }
  return (sessionStorage.getItem(sessionKey) || defaultValue).trim();
}

function extractServiceTotalCostOfOwnership() {
  const primaryTcoSource = window._lastComputedTco;
  if (typeof primaryTcoSource === 'number' && primaryTcoSource > 0) {
    return Math.round(primaryTcoSource * 100) / 100;
  }
  return scrapeTotalCostOfOwnershipFromUiTable();
}

function scrapeTotalCostOfOwnershipFromUiTable() {
  const breakdownTable = document.querySelector('#cloudTcoBreakdown table');
  if (!breakdownTable) return 0;

  const tableRows = breakdownTable.querySelectorAll('tbody tr');
  if (!tableRows.length) return 0;

  const finalRowCells = tableRows[tableRows.length - 1].querySelectorAll('td');
  let detectedTco = 0;

  finalRowCells.forEach(cell => {
    const textContent = cell.textContent || '';
    const containsCurrencySymbols = textContent.includes('$') && textContent.includes('/mo');
    const isPercentageColumn = textContent.includes('%');

    if (containsCurrencySymbols && !isPercentageColumn) {
      const numericValue = parseFloat(textContent.replace(/[^0-9.]/g, ''));
      if (numericValue > 1) { // Prevents capturing tiny rounding adjustments
        detectedTco = numericValue;
      }
    }
  });

  return detectedTco;
}

function compileActiveArchitecturalTactics() {
  const TACTIC_LABELS_MAPPING = {
    'tactic-client-lb': 'LB-Client',
    'tactic-server-lb': 'ALB/LB-Server',
    'tactic-timeout': 'Timeout',
    'tactic-retry': 'Retry',
    'tactic-cb': 'Circuit Breaker',
    'tactic-saga': 'SAGA',
    'tactic-apigw': 'API GW',
    'tactic-tls': 'TLS',
    'tactic-mtls': 'mTLS',
    'tactic-oauth': 'OAuth2+JWT',
    'tactic-basic-auth': 'Basic Auth',
    'tactic-alb': 'ALB',
    'tactic-cache': 'ElastiCache',
    'tactic-s3-backup': 'S3 Backup',
    'tactic-aurora-replica': 'Aurora',
    'sec-guardduty': 'GuardDuty',
    'sec-inspector': 'Inspector',
    'sec-waf': 'WAF',
    'sec-macie': 'Macie',
    'sec-cloudwatch': 'CloudWatch',
    'sec-audit': 'Audit Mgr',
    'sec-kms': 'KMS',
    'cef-clusters': 'EKS Cluster',
    'cef-host-storage': 'Host Storage',
    'cef-cluster-lb': 'Cluster LB',
    'cef-host-license': 'OS License',
    'cef-workload-license': 'Workload License',
    'cef-cronjobs': 'CronJobs',
    'cef-statefulsets': 'StatefulSets',
    'opt-ri-standard': 'RI 1yr',
    'opt-ri-3yr': 'RI 3yr',
    'opt-savings-plan-1yr': 'SP 1yr',
    'opt-savings-plan-3yr': 'SP 3yr'
  };

  const activeTactics = Object.keys(TACTIC_LABELS_MAPPING).filter(elementId => {
    const checkboxElement = document.getElementById(elementId);
    return checkboxElement && checkboxElement.checked;
  }).map(elementId => TACTIC_LABELS_MAPPING[elementId]);

  const finopsSavings = parseFloat(sessionStorage.getItem('tco_finops_saving') || '0');
  if (finopsSavings > 0) {
    activeTactics.push(`FinOps -$${finopsSavings.toFixed(0)}/mo`);
  }

  return activeTactics;
}

function triggerSaveVisualConfirmationButton(serviceName, totalCostOfOwnership) {
  const saveButton = document.querySelector('.phase4-actions [onclick="saveServiceToPortfolio()"]');
  if (!saveButton) {
    showToast(`✓ Service ${serviceName} saved to portfolio!`);
    return;
  }

  const originalHtml = saveButton.innerHTML;
  const originalBackgroundStyle = saveButton.style.background;

  saveButton.innerHTML = '<i class="fas fa-check"></i> Saved to Portfolio!';
  saveButton.style.background = 'var(--green)';
  saveButton.disabled = true;

  setTimeout(() => {
    saveButton.innerHTML = originalHtml;
    saveButton.style.background = originalBackgroundStyle;
    saveButton.disabled = false;
  }, 2500);
}

function renderPhase5Portfolio() {
  const portfolio = getPortfolio();
  const listContainer = document.getElementById('p5-portfolio-list');
  const totalsWidgetContainer = document.getElementById('p5-totals');

  if (!listContainer) return;

  if (!portfolio.length) {
    listContainer.innerHTML = '<div style="text-align:center;color:var(--ink-light);padding:32px;">No services yet.</div>';
    if (totalsWidgetContainer) totalsWidgetContainer.style.display = 'none';
    return;
  }

  const totalPortfolioTco = portfolio.reduce((sum, service) => sum + service.tco, 0);

  // Generate interactive element views
  listContainer.innerHTML = portfolio.map((service, index) => {
    return generateServiceCardMarkup(service, index, totalPortfolioTco);
  }).join('');

  buildStaticPrintMirrorTable(portfolio, totalPortfolioTco);
  dispatchUnitEconomicsCalculationRequest(portfolio);
  updateTotalsGridDisplayLayout(portfolio, totalPortfolioTco, totalsWidgetContainer);
}

function generateServiceCardMarkup(service, index, totalPortfolioTco) {
  const businessUseCaseLabel = service.buc && service.buc.startsWith('BUC') ? service.buc : (service.buc || '—');
  const portfolioPercentageContribution = totalPortfolioTco > 0 ? ((service.tco / totalPortfolioTco) * 100).toFixed(1) : '0.0';
  const visualBarWidthPercentage = totalPortfolioTco > 0 ? Math.max(4, Math.round((service.tco / totalPortfolioTco) * 100)) : 0;

  const activeTacticBadgesMarkup = (service.tactics && service.tactics.length)
    ? service.tactics.map(tacticName => `<span style="background:rgba(22,101,52,.08);color:var(--green);padding:2px 7px;border-radius:999px;font-size:.68rem;">${tacticName}</span>`).join('')
    : '';

  return `
    <div class="phase5-service-card" style="position:relative;">
      <span class="phase5-service-number">#${index + 1}&nbsp;&nbsp;${service.timestamp}</span>
      <div style="display:flex;align-items:flex-start;gap:14px;flex-wrap:wrap;">
        <div style="flex:1;min-width:180px;">
          <div style="font-weight:700;font-size:1.05rem;color:var(--blue-deep);margin-bottom:3px;">${service.name}</div>
          <div style="font-size:.78rem;color:var(--ink-light);margin-bottom:8px;line-height:1.5;">${service.purpose}</div>
          <div style="display:flex;gap:6px;flex-wrap:wrap;">
            <span style="background:rgba(37,99,235,.09);color:var(--blue-mid);padding:2px 9px;border-radius:999px;font-size:.7rem;font-weight:700;">${businessUseCaseLabel}</span>
            <span style="background:var(--paper);border:1px solid var(--rule);padding:2px 9px;border-radius:999px;font-size:.7rem;">${service.rps.toLocaleString()}&nbsp;RPS</span>
            <span style="background:var(--paper);border:1px solid var(--rule);padding:2px 9px;border-radius:999px;font-size:.7rem;">${service.consumers.toLocaleString()}&nbsp;users</span>
            ${activeTacticBadgesMarkup}
          </div>
        </div>
        <div style="text-align:right;min-width:130px;">
          <div style="font-family:monospace;font-size:1.2rem;font-weight:700;color:var(--blue-deep);">$${service.tco.toFixed(2)}/mo</div>
          <div style="font-size:.72rem;color:var(--ink-light);">$${(service.tco * 12).toFixed(2)}/yr</div>
          <div style="font-size:.72rem;color:var(--ink-light);margin-bottom:6px;">${portfolioPercentageContribution}% of portfolio</div>
          <div style="height:6px;background:var(--rule);border-radius:999px;overflow:hidden;">
            <div style="height:100%;width:${visualBarWidthPercentage}%;background:linear-gradient(to right,var(--blue-deep),var(--blue-mid));border-radius:999px;"></div>
          </div>
        </div>
      </div>
      <button onclick="removeFromPortfolio(${service.id})" title="Remove" style="position:absolute;top:10px;right:76px;background:none;border:none;cursor:pointer;color:var(--ink-light);font-size:.82rem;padding:4px 6px;">
        <i class="fas fa-times"></i>
      </button>
    </div>`;
}

function buildStaticPrintMirrorTable(portfolio, totalPortfolioTco) {
  const printTableContainer = document.getElementById('p5-print-table');
  if (!printTableContainer) return;

  const tableRowsHtml = portfolio.map((service, index) => {
    const zebraStripedBackground = (index % 2 === 0) ? '#fff' : '#f8f7f4';
    const truncatedPurpose = (service.purpose || '').substring(0, 70) + ((service.purpose || '').length > 70 ? '…' : '');

    return `
      <tr style="background:${zebraStripedBackground};border-bottom:1px solid #E5E3DF;">
        <td style="padding:9px 14px;font-weight:600;">${service.name}</td>
        <td style="padding:9px 14px;">${service.buc}</td>
        <td style="padding:9px 14px;font-size:.78rem;color:#6B7280;">${truncatedPurpose}</td>
        <td style="padding:9px 14px;text-align:right;font-family:monospace;font-weight:700;color:#991B1B;">$${service.tco.toFixed(2)}/mo</td>
        <td style="padding:9px 14px;text-align:right;font-family:monospace;">$${(service.tco * 12).toFixed(2)}/yr</td>
      </tr>`;
  }).join('');

  printTableContainer.innerHTML = `
    <h2 style="font-family:'DM Serif Display',serif;font-size:1.4rem;color:#1A3A5C;margin-bottom:4px;">MACH Architecture — TCO Portfolio Summary</h2>
    <p style="font-size:.8rem;color:#6B7280;margin-bottom:16px;">Generated: ${new Date().toLocaleString()}</p>
    <table style="width:100%;border-collapse:collapse;font-size:.86rem;">
      <thead>
        <tr style="background:#1A3A5C;color:#fff;">
          <th style="padding:10px 14px;text-align:left;">Service</th>
          <th style="padding:10px 14px;text-align:left;">BUC</th>
          <th style="padding:10px 14px;text-align:left;">Purpose</th>
          <th style="padding:10px 14px;text-align:right;">Monthly TCO</th>
          <th style="padding:10px 14px;text-align:right;">Annual TCO</th>
        </tr>
      </thead>
      <tbody>
        ${tableRowsHtml}
        <tr style="background:#1A3A5C;color:#fff;font-weight:700;">
          <td colspan="3" style="padding:10px 14px;">TOTAL (${portfolio.length} services)</td>
          <td style="padding:10px 14px;text-align:right;font-family:monospace;">$${totalPortfolioTco.toFixed(2)}/mo</td>
          <td style="padding:10px 14px;text-align:right;font-family:monospace;">$${(totalPortfolioTco * 12).toFixed(2)}/yr</td>
        </tr>
      </tbody>
    </table>
    <p style="font-size:.72rem;color:#6B7280;border-top:1px solid #E5E3DF;padding-top:10px;margin-top:14px;">
      Architectural TCO estimate. Validate with Engineering, FinOps, Finance and Procurement before acting on these figures.
    </p>`;
}

function dispatchUnitEconomicsCalculationRequest(portfolio) {
  const unitEconomicsContainer = document.getElementById('p5-unit-economics');
  if (!unitEconomicsContainer) return;

  unitEconomicsContainer.style.display = 'block';
  unitEconomicsContainer.innerHTML = '<div style="text-align:center;padding:16px;color:var(--ink-light);font-size:.82rem;"><span class="spinner"></span> Computing…</div>';

  const outboundPayload = {
    services: portfolio.map(service => ({
      name: service.name,
      buc: service.buc,
      tco: service.tco || 0,
      revenuePerUserMonth: service.revenuePerUserMonth || 0,
      consumers: service.consumers || 0,
      rps: service.rps || 0
    })),
    finopsMonthlySaving: parseFloat(sessionStorage.getItem('tco_finops_saving') || '0'),
    ec2BaselineSpend: parseFloat(sessionStorage.getItem('tco_finops_spend') || '0')
  };

  fetch('/api/portfolio/roi', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(outboundPayload)
  })
    .then(response => response.ok ? response.json() : null)
    .then(roiData => {
      if (roiData) {
        renderP5UnitEconomics(roiData, unitEconomicsContainer);
      } else {
        renderP5UnitEconomicsFallback(unitEconomicsContainer, portfolio);
      }
    })
    .catch(() => {
      renderP5UnitEconomicsFallback(unitEconomicsContainer, portfolio);
    });
}

function updateTotalsGridDisplayLayout(portfolio, totalPortfolioTco, totalsWidgetContainer) {
  if (!totalsWidgetContainer) return;
  totalsWidgetContainer.style.display = 'block';

  const metricsGridElement = document.getElementById('p5-totals-grid');
  if (!metricsGridElement) {
    // In-line fallback rendering structure if targeted grid element is missing
    totalsWidgetContainer.innerHTML = `
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <div>
          <div style="font-size:.7rem;color:rgba(255,255,255,.6);margin-bottom:4px;">Total Portfolio TCO</div>
          <div style="font-family:monospace;font-size:1.6rem;font-weight:700;">$${totalPortfolioTco.toFixed(2)}/mo</div>
          <div style="font-size:.78rem;color:rgba(255,255,255,.75);margin-top:3px;">
            $${(totalPortfolioTco * 12).toFixed(2)}/yr &middot; ${portfolio.length} service${portfolio.length !== 1 ? 's' : ''} modelled
          </div>
        </div>
        <div style="text-align:right;">
          <div style="font-size:.7rem;color:rgba(255,255,255,.6);margin-bottom:4px;">Avg / service</div>
          <div style="font-family:monospace;font-size:1.1rem;font-weight:700;">$${(totalPortfolioTco / portfolio.length).toFixed(2)}/mo</div>
        </div>
      </div>`;
    return;
  }

  const summarizedMetricsMetadata = [
    { label: 'Services Modelled', value: portfolio.length, suffix: 'in this portfolio' },
    { label: 'Total Monthly TCO', value: `$${totalPortfolioTco.toFixed(2)}/mo`, suffix: 'sum of all services' },
    { label: 'Total Annual TCO', value: `$${(totalPortfolioTco * 12).toFixed(2)}/yr`, suffix: '12 × monthly total' },
    { label: 'Avg TCO / Service', value: `$${(totalPortfolioTco / portfolio.length).toFixed(2)}/mo`, suffix: 'mean per service' }
  ];

  metricsGridElement.style.gridTemplateColumns = 'repeat(4, 1fr)';
  metricsGridElement.innerHTML = summarizedMetricsMetadata.map(metric => `
    <div style="background:rgba(255,255,255,.1);border-radius:var(--r);padding:12px 14px;">
      <div style="font-size:.68rem;font-weight:700;text-transform:uppercase;letter-spacing:.08em;color:rgba(255,255,255,.55);margin-bottom:4px;">${metric.label}</div>
      <div style="font-family:monospace;font-size:1.1rem;font-weight:700;color:#F5ECD7;">${metric.value}</div>
      <div style="font-size:.7rem;color:rgba(255,255,255,.45);margin-top:2px;">${metric.suffix}</div>
    </div>`).join('');
}

function removeFromPortfolio(serviceId) {
  const adjustedPortfolio = getPortfolio().filter(service => service.id !== serviceId);
  savePortfolio(adjustedPortfolio);
  renderPhase5Portfolio();
}

function clearPortfolio() {
  const portfolioLength = getPortfolio().length;
  const isUserConfirmed = confirm(`Clear all ${portfolioLength} service(s)? This action cannot be undone.`);
  if (!isUserConfirmed) return;

  savePortfolio([]);
  renderPhase5Portfolio();
}

function standardiseWindowPrintPipeline(documentExportTitle) {
  const originalTitle = document.title;
  document.title = documentExportTitle;
  window.print();
  document.title = originalTitle;
}

function downloadPortfolioPdf() {
  const executionDateIsoString = new Date().toISOString().substring(0, 10);
  assertWindowPrintExecution(`MACH-Portfolio-TCO-${executionDateIsoString}`);
}

function assertWindowPrintExecution(exportTitleToken) {
  standardiseWindowPrintPipeline(exportTitleToken);
}

function showPortfolioNextDialog(serviceName, totalCostOfOwnership) {
  const staleDialog = document.getElementById('portfolio-next-dialog');
  if (staleDialog) staleDialog.remove();

  const currentPortfolio = getPortfolio();
  const calculatedTotalPortfolioSum = currentPortfolio.reduce((sum, service) => sum + service.tco, 0);

  const dialogWrapperOverlay = document.createElement('div');
  dialogWrapperOverlay.id = 'portfolio-next-dialog';
  dialogWrapperOverlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(10,20,40,.55);z-index:9999;display:flex;align-items:center;justify-content:center;';

  const serviceNameLabel = serviceName ? `“${serviceName}”` : 'this service';
  const totalCostLabel = totalCostOfOwnership ? ` &mdash; <strong style="color:var(--red);">$${(Math.round(totalCostOfOwnership * 100) / 100).toFixed(2)}/mo</strong>` : '';
  const serviceCountPluralizationText = currentPortfolio.length !== 1 ? 'services' : 'service';

  dialogWrapperOverlay.innerHTML = `
    <div style="background:var(--card);border-radius:var(--r);box-shadow:var(--shadow-lg);padding:28px 32px;max-width:480px;width:90%;">
      <div style="font-family:'DM Serif Display',serif;font-size:1.1rem;color:var(--blue-deep);margin-bottom:6px;">
        <i class="fas fa-folder-plus" style="margin-right:8px;color:var(--gold);"></i>Service Added to Portfolio
      </div>
      <div style="font-size:.88rem;color:var(--ink-medium);margin-bottom:4px;">${serviceNameLabel}${totalCostLabel} saved.</div>
      <div style="font-size:.82rem;color:var(--ink-light);margin-bottom:20px;">
        Portfolio: <strong>${currentPortfolio.length} ${serviceCountPluralizationText}</strong> &middot;
        Total: <strong style="color:var(--blue-deep);">$${calculatedTotalPortfolioSum.toFixed(2)}/mo</strong>
      </div>
      <div style="display:flex;gap:12px;flex-wrap:wrap;">
        <button id="pnd-c" class="btn btn-gold" style="flex:1;"><i class="fas fa-plus-circle"></i> Model Another Service</button>
        <button id="pnd-f" class="btn btn-primary" style="flex:1;"><i class="fas fa-chart-pie"></i> View Portfolio</button>
      </div>
      <button id="pnd-s" class="btn btn-ghost" style="width:100%;margin-top:8px;"><i class="fas fa-eye"></i> Stay &mdash; review report</button>
    </div>`;

  document.body.appendChild(dialogWrapperOverlay);

  document.getElementById('pnd-c').onclick = () => { dialogWrapperOverlay.remove(); startNewService(true); };
  document.getElementById('pnd-f').onclick = () => { dialogWrapperOverlay.remove(); goToPhase(5); };
  document.getElementById('pnd-s').onclick = () => { dialogWrapperOverlay.remove(); };

  dialogWrapperOverlay.addEventListener('click', (event) => {
    if (event.target === dialogWrapperOverlay) dialogWrapperOverlay.remove();
  });

  const phase4ResetButton = document.querySelector('.phase4-actions [onclick="startNewService()"]');
  if (phase4ResetButton) {
    phase4ResetButton.setAttribute('onclick', 'startNewService(true)');
  }
}

function renderP5UnitEconomicsFallback(container, portfolio) {
  // Build the same request payload the primary call uses, but from local data.
  // This guarantees the fallback path uses the same backend formula — no inline math.
  const fallbackPayload = {
    services: portfolio.map(service => ({
      name:                 service.name,
      buc:                  service.buc,
      tco:                  service.tco || 0,
      revenuePerUserMonth:  service.revenuePerUserMonth || 0,
      consumers:            service.consumers || 0,
      rps:                  service.rps || 0
    })),
    finopsMonthlySaving: parseFloat(sessionStorage.getItem('tco_finops_saving') || '0'),
    ec2BaselineSpend:    parseFloat(sessionStorage.getItem('tco_finops_spend')   || '0')
  };

  fetch('/api/portfolio/roi', {
    method:  'POST',
    headers: { 'Content-Type': 'application/json' },
    body:    JSON.stringify(fallbackPayload)
  })
    .then(response => response.ok ? response.json() : null)
    .then(roiData => {
      if (roiData) {
        renderP5UnitEconomics(roiData, container);
      } else {
        container.innerHTML =
          '<div style="font-size:.82rem;color:var(--ink-light);padding:12px;">'
          + '<i class="fas fa-triangle-exclamation" style="color:var(--amber);margin-right:6px;"></i>'
          + 'Portfolio unit economics unavailable — please ensure Spring Boot is running.'
          + '</div>';
      }
    })
    .catch(() => {
      container.innerHTML =
        '<div style="font-size:.82rem;color:var(--ink-light);padding:12px;">'
        + '<i class="fas fa-triangle-exclamation" style="color:var(--amber);margin-right:6px;"></i>'
        + 'Could not connect to backend for portfolio ROI calculation.'
        + '</div>';
    });
}


function renderP5UnitEconomics(roi, container) {
  const roiStatusColorVariable = roi.monthlyRoi >= 0 ? 'var(--green)' : 'var(--red)';
  const netProfitStatusColorVariable = roi.netMonthlyProfit >= 0 ? 'var(--green)' : 'var(--red)';

  const coreUnitMetricsSchema = [
    { label: 'Total Architecture TCO', value: `$${roi.totalMonthlyTco.toFixed(2)}/mo`, summary: `sum of ${roi.serviceCount} services` },
    { label: 'Total Annual TCO', value: `$${roi.totalAnnualTco.toFixed(2)}/yr`, summary: '12 × monthly' },
    { label: 'Avg TCO / Microservice', value: `$${roi.avgTcoPerService.toFixed(2)}/mo`, summary: 'mean' },
    { label: 'Total RPS', value: roi.totalRps.toLocaleString(), summary: 'aggregate req/s' },
    { label: 'Cost / Request', value: roi.costPerRequestUsd > 0 ? `$${roi.costPerRequestUsd.toFixed(6)}` : '—', summary: 'avg' },
    { label: 'Cost / User / Month', value: roi.costPerUserPerMonth > 0 ? `$${roi.costPerUserPerMonth.toFixed(4)}` : '—', summary: roi.totalConsumers > 0 ? `${roi.totalConsumers.toLocaleString()} users` : 'set in Phase 2' }
  ];

  const buildMetricGridHtml = (metricsList, columnStylePattern = 'repeat(3,1fr)') => `
    <div style="display:grid;grid-template-columns:${columnStylePattern};gap:12px;margin-bottom:16px;">
      ${metricsList.map(metric => `
        <div style="background:var(--paper);border:1px solid var(--rule);border-radius:var(--r);padding:12px 14px;">
          <div style="font-size:.68rem;font-weight:700;text-transform:uppercase;color:var(--ink-light);margin-bottom:4px;">${metric.label}</div>
          <div style="font-family:monospace;font-size:1.05rem;font-weight:700;color:${metric.customColor || 'var(--blue-deep)'};">${metric.value}</div>
          <div style="font-size:.7rem;color:var(--ink-light);margin-top:2px;">${metric.summary}</div>
        </div>`).join('')}
    </div>`;

  let aggregatedHtmlOutput = `
    <div style="margin-top:20px;">
      <div style="font-family:'DM Serif Display',serif;font-size:1rem;color:var(--blue-deep);margin-bottom:12px;">
        <i class="fas fa-coins" style="margin-right:8px;"></i>Aggregated Unit Economics
      </div>
      ${buildMetricGridHtml(coreUnitMetricsSchema)}`

  if (roi.hasRevenueData) {
    const strategicFinancialMetricsSchema = [
      { label: 'Total Monthly Revenue', value: `$${roi.totalMonthlyRevenue.toFixed(2)}`, summary: 'across all services' },
      { label: 'Total Annual Revenue', value: `$${roi.totalAnnualRevenue.toFixed(2)}`, summary: '12 × monthly' },
      { label: 'Portfolio ARPU', value: roi.portfolioArpu > 0 ? `$${roi.portfolioArpu.toFixed(4)}/user/mo` : '—', summary: 'weighted avg' },
      { label: 'Monthly ROI', value: `${roi.monthlyRoi.toFixed(1)}%`, customColor: roiStatusColorVariable, summary: '(rev − TCO) ÷ TCO' },
      { label: 'Annual ROI', value: `${roi.annualRoi.toFixed(1)}%`, customColor: roiStatusColorVariable, summary: '12× monthly' },
      { label: 'Net Monthly Profit', value: `$${roi.netMonthlyProfit.toFixed(2)}`, customColor: netProfitStatusColorVariable, summary: 'rev − TCO' },
      { label: 'Net Annual Profit', value: `$${roi.netAnnualProfit.toFixed(2)}`, customColor: netProfitStatusColorVariable, summary: '12×' },
      { label: 'Break-even Users', value: roi.breakEvenUsers > 0 ? roi.breakEvenUsers.toLocaleString() : '—', summary: 'to cover portfolio TCO' },
      { label: 'Revenue / $1 Infra', value: roi.revenuePerDollarInfra > 0 ? `$${roi.revenuePerDollarInfra.toFixed(2)}` : '—', summary: 'efficiency' }
    ];

    aggregatedHtmlOutput += `
      <div style="font-family:'DM Serif Display',serif;font-size:1rem;color:var(--amber);margin-bottom:12px;">
        <i class="fas fa-chart-line" style="margin-right:8px;"></i>Portfolio Revenue, ARPU &amp; ROI Analysis
      </div>
      <div style="font-size:.77rem;color:var(--ink-medium);padding:8px 12px;background:rgba(255,255,255,.6);border-radius:var(--r);border:1px solid rgba(212,160,23,.2);margin-bottom:10px;">
        Aggregate ROI across all modelled microservices. Set Expected Revenue per Transaction in Phase 1 per service.
        <strong>Break-even</strong> = users to cover full portfolio TCO. Share with Finance and Leadership.
      </div>
      ${buildMetricGridHtml(strategicFinancialMetricsSchema)}`;

    if (roi.finopsMonthlySaving > 0) {
      aggregatedHtmlOutput += `
        <div style="padding:12px 14px;border-radius:var(--r);background:linear-gradient(135deg,#f0fdf4,#e8f5e9);border:1px solid var(--green-border);margin-top:4px;">
          <div style="font-size:.84rem;font-weight:600;color:var(--green);margin-bottom:6px;">
            <i class="fas fa-tags" style="margin-right:5px;"></i>FinOps Savings Impact on Portfolio ROI
          </div>
          <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:10px;font-size:.8rem;">
            <div>
              <div style="color:var(--ink-light);margin-bottom:2px;">Monthly Saving</div>
              <div style="font-family:monospace;font-weight:700;color:var(--green);">-$${roi.finopsMonthlySaving.toFixed(2)}/mo</div>
            </div>
            <div>
              <div style="color:var(--ink-light);margin-bottom:2px;">Adjusted TCO</div>
              <div style="font-family:monospace;font-weight:700;color:var(--blue-deep);">$${roi.finopsAdjustedTco.toFixed(2)}/mo</div>
            </div>
            <div>
              <div style="color:var(--ink-light);margin-bottom:2px;">Adj. Monthly ROI</div>
              <div style="font-family:monospace;font-weight:700;color:var(--green);">
                ${roi.finopsAdjustedRoi.toFixed(1)}%${roi.finopsRoiImprovementPct > 0 ? ` (+${roi.finopsRoiImprovementPct.toFixed(1)}%)` : ''}
              </div>
            </div>
          </div>
        </div>`;
    }
  } else {
    aggregatedHtmlOutput += `
      <div style="font-size:.77rem;color:var(--ink-light);padding:10px 12px;border-radius:var(--r);background:rgba(212,160,23,.05);border:1px solid rgba(212,160,23,.2);">
        <i class="fas fa-circle-info" style="margin-right:5px;color:var(--gold);"></i>
        Enter <strong>Expected Revenue per Transaction</strong> in Phase 1 to unlock Portfolio ROI &amp; ARPU analysis.
      </div>`;
  }

  aggregatedHtmlOutput += '</div>';
  container.innerHTML = aggregatedHtmlOutput;
}

function showToast(message) {
  const toastWrapperElement = document.createElement('div');
  toastWrapperElement.style.cssText = 'position:fixed;bottom:28px;left:50%;transform:translateX(-50%);background:var(--green);color:#fff;padding:12px 26px;border-radius:var(--r);font-size:.88rem;font-weight:600;z-index:9999;box-shadow:var(--shadow-md);max-width:90vw;text-align:center;';
  toastWrapperElement.textContent = message;

  document.body.appendChild(toastWrapperElement);
  setTimeout(() => toastWrapperElement.remove(), 2600);
}