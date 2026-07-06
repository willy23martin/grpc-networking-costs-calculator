const MANDATORY_TAGS_SCHEMA = [
  { key: 'Environment',        example: 'production | staging | dev',            purpose: 'Separate cost by environment',             dimension: 'Allocation' },
  { key: 'Team',               example: 'platform-engineering | backend',        purpose: 'Charge back costs to the owning team',     dimension: 'Chargeback' },
  { key: 'CostCentre',         example: 'CC-1042',                               purpose: 'Finance cost centre mapping',              dimension: 'Chargeback' },
  { key: 'Project',            example: 'mach-ecommerce-replatform',             purpose: 'Group costs by business initiative',       dimension: 'Allocation' },
  { key: 'Application',        example: 'order-management-service',              purpose: 'Identify the owning application',          dimension: 'Showback' },
  { key: 'ManagedBy',          example: 'terraform | cloudformation | cdk',      purpose: 'Track IaC ownership and drift',            dimension: 'Governance' },
  { key: 'DataClassification', example: 'confidential | internal | public',      purpose: 'Support security and compliance policy',   dimension: 'Governance' },
];

function renderFinOpsAllocationAndTaggingStrategy() {
  const mandatoryTagsTableBody = document.getElementById('tagging-mandatory-rows');
  const serviceTagsTableBody = document.getElementById('tagging-service-rows');
  const cloudChecklistContainer = document.getElementById('tagging-cloud-checklist');

  if (!mandatoryTagsTableBody) return;

  const currentServiceName = extractServiceName();

  renderMandatoryTagsTable(mandatoryTagsTableBody, currentServiceName);
  renderServiceSpecificTagsTable(serviceTagsTableBody, currentServiceName);
  renderCloudServicesChecklist(cloudChecklistContainer);
}

function extractServiceName() {
  const serviceNameInputField = document.getElementById('serviceName');
  if (serviceNameInputField && serviceNameInputField.value.trim()) {
    return serviceNameInputField.value.trim();
  }
  return sessionStorage.getItem('svc_name') || 'your-service-name';
}

function renderMandatoryTagsTable(tableBodyContainer, serviceName) {
  tableBodyContainer.innerHTML = MANDATORY_TAGS_SCHEMA.map(tag => `
    <tr style="border-bottom:1px solid rgba(134,239,172,.3); transition:background .15s">
      <td style="padding:7px 12px; font-family:monospace; font-weight:600; color:#166534">${tag.key}</td>
      <td style="padding:7px 12px; color:#6B7280; font-family:monospace; font-size:.78rem">${tag.example}</td>
      <td style="padding:7px 12px">${tag.purpose}</td>
      <td style="padding:7px 12px">
        <span style="display:inline-block; padding:1px 7px; border-radius:999px; font-size:.67rem; font-weight:700; background:#DCFCE7; border:1px solid #86EFAC; color:#166534">
          ${tag.dimension}
        </span>
      </td>
    </tr>`
  ).join('');
}

function renderServiceSpecificTagsTable(tableBodyContainer, serviceName) {
  if (!tableBodyContainer) return;

  const activeBusinessUseCase = sessionStorage.getItem('svc_buc') || window.selectedBUC || 'BUC1';
  const baselineRequestsPerSecond = sessionStorage.getItem('svc_rps') || '0';

  const GRPC_STREAMING_PATTERN_MAPPING = {
    BUC1: 'unary-rpc',
    BUC2: 'server-streaming',
    BUC3: 'client-streaming',
    BUC4: 'bidirectional-streaming'
  };
  const identifiedPattern = GRPC_STREAMING_PATTERN_MAPPING[activeBusinessUseCase] || 'grpc';
  const sanitizedServiceIdentifier = serviceName.toLowerCase().replace(/[^a-z0-9-]/g, '-');

  const serviceSpecificTagsSchema = [
    { key: 'ServiceName',       value: sanitizedServiceIdentifier,                       purpose: 'Exact microservice identifier for cost drill-down' },
    { key: 'gRPCPattern',       value: identifiedPattern,                                 purpose: 'The gRPC streaming pattern used by this service' },
    { key: 'BaseRPS',           value: baselineRequestsPerSecond,                        purpose: 'Baseline load — supports capacity cost attribution' },
    { key: 'BusinessUseCase',   value: activeBusinessUseCase,                            purpose: 'Ties cost to the business capability it supports' },
    { key: 'ProtocolBuffer',    value: 'proto3',                                          purpose: 'Serialisation format — relevant to egress cost model' },
    { key: 'FinOpsReviewed',    value: new Date().toISOString().substring(0, 10),         purpose: 'Date last reviewed by FinOps/architect for cost fitness' },
  ];

  tableBodyContainer.innerHTML = serviceSpecificTagsSchema.map(tag => `
    <tr style="border-bottom:1px solid rgba(134,239,172,.3)">
      <td style="padding:7px 12px; font-family:monospace; font-weight:600; color:#166534">${tag.key}</td>
      <td style="padding:7px 12px; font-family:monospace; color:#2563EB; font-size:.78rem">${tag.value}</td>
      <td style="padding:7px 12px">${tag.purpose}</td>
    </tr>`
  ).join('');
}

function renderCloudServicesChecklist(checklistContainer) {
  if (!checklistContainer) return;

  const activatedCloudResources = compileActiveCloudInfrastructurePayload();

  if (!activatedCloudResources.length) {
    checklistContainer.innerHTML = `
      <div style="font-size:.82rem; color:#6B7280; font-style:italic">
        No cloud services selected yet — tags will appear here once resources are configured in Phase 3.
      </div>`;
    return;
  }

  checklistContainer.innerHTML = activatedCloudResources.map(resource => {
    const badgeTagsMarkup = resource.tags.map(tagName => `
      <span style="font-family:monospace; font-size:.68rem; font-weight:700; background:#DCFCE7; border:1px solid #86EFAC; color:#166534; padding:1px 7px; border-radius:999px">
        ${tagName}
      </span>`
    ).join('');

    return `
      <div style="padding:12px 14px; border-radius:var(--r); background:rgba(255,255,255,.7); border:1px solid rgba(134,239,172,.5)">
        <div style="font-weight:600; font-size:.85rem; color:#166534; margin-bottom:5px">
          <i class="fas fa-cloud" style="margin-right:6px; font-size:.78rem"></i>${resource.serviceName}
        </div>
        <div style="display:flex; flex-wrap:wrap; gap:5px; margin-bottom:6px">
          ${badgeTagsMarkup}
        </div>
        <div style="font-size:.75rem; color:#6B7280; line-height:1.5">
          <i class="fas fa-circle-info" style="margin-right:4px; color:#166534"></i>${resource.allocationNote}
        </div>
      </div>`;
  }).join('');
}

function compileActiveCloudInfrastructurePayload() {
  const activeServicesList = [];

  // Use tco_snapshot (written by writeTcoSnapshot in calculator.js after every
  // recalculate* call) instead of DOM checkbox state. The snapshot is always
  // current and avoids the duplicate-ID problem where getElementById finds the
  // static hidden checkbox in body-cloud instead of the dynamic one.
  let _snap = {};
  try { _snap = JSON.parse(sessionStorage.getItem('tco_snapshot') || '{}'); } catch(e) {}
  const _secSnap = _snap.sec || {};

  // A service is "active" if it has a cost > 0 in the snapshot, OR if the
  // sessionStorage key exists (written by recalculate* functions).
  const hasSnapCost = (key) => parseFloat(_snap[key] || 0) > 0;
  const hasSecCost  = (...ids) => ids.some(id => parseFloat(_secSnap[id] || 0) > 0);
  const hasDbKey    = () => parseFloat(sessionStorage.getItem('tco_db_cost') || 0) > 0;

  // Fallback: also check DOM for cases where snapshot not yet written
  const domChecked  = (id) => {
    const sc = document.getElementById('cloudsec-content');
    const el = (sc && sc.querySelector('#' + id)) || document.getElementById(id);
    return !!(el && el.checked);
  };
  const isTacticChecked = (id) => domChecked(id);

  if (hasSnapCost('alb') || isTacticChecked('tactic-alb')) {
    activeServicesList.push({
      serviceName: 'Application Load Balancer (ALB)',
      tags: ['Application', 'Environment', 'Team', 'CostCentre', 'ServiceName'],
      allocationNote: 'Tag each ALB individually — one ALB can serve multiple services, so tag at listener/target-group level too.'
    });
  }

  if (hasSnapCost('cache') || isTacticChecked('tactic-cache')) {
    activeServicesList.push({
      serviceName: 'Amazon ElastiCache',
      tags: ['Application', 'Environment', 'Team', 'ServiceName', 'DataClassification'],
      allocationNote: 'Tag the replication group. Snapshot storage inherits tags automatically.'
    });
  }

  const isRdsOrAuroraEnabled = hasDbKey() || isTacticChecked('tactic-s3-backup') || isTacticChecked('tactic-aurora-replica') || isTacticChecked('tactic-rds-snapshot') || isTacticChecked('tactic-rds-multiaz');
  if (isRdsOrAuroraEnabled) {
    activeServicesList.push({
      serviceName: 'Amazon RDS / Aurora',
      tags: ['Application', 'Environment', 'Team', 'CostCentre', 'DataClassification', 'ManagedBy'],
      allocationNote: 'Tag DB instance and snapshots separately. Aurora: tag cluster + each instance. Enable Cost Allocation Tags in RDS console.'
    });
  }

  const isSecuritySuiteEnabled = hasSecCost('sec-guardduty','sec-inspector','sec-waf','sec-kms','sec-macie','sec-cloudwatch','sec-audit') ||
                                isTacticChecked('sec-guardduty') || isTacticChecked('sec-inspector') ||
                                isTacticChecked('sec-waf') || isTacticChecked('sec-kms');
  if (isSecuritySuiteEnabled) {
    activeServicesList.push({
      serviceName: 'Security Services (GuardDuty / WAF / KMS / Macie / CloudWatch)',
      tags: ['Application', 'Environment', 'Team', 'CostCentre'],
      allocationNote: 'GuardDuty and Macie are account-level; use AWS Cost Categories for allocation. KMS CMKs: tag per owning service.'
    });
  }

  if (hasSnapCost('container') || isTacticChecked('cef-clusters') || isTacticChecked('cef-control-plane') || isTacticChecked('cef-cluster-lb') || isTacticChecked('cef-host-storage')) {
    activeServicesList.push({
      serviceName: 'Amazon EKS Cluster',
      tags: ['Application', 'Environment', 'Team', 'CostCentre', 'ManagedBy'],
      allocationNote: 'Add tags to the EKS cluster AND all managed node groups. Use AWS Cost Allocation Tags + Kubecost/OpenCost for pod-level attribution.'
    });
  }

  if (hasSnapCost('apigw') || isTacticChecked('tactic-apigw')) {
    activeServicesList.push({
      serviceName: 'Amazon API Gateway',
      tags: ['Application', 'Environment', 'Team', 'BusinessUseCase', 'gRPCPattern'],
      allocationNote: 'Tag at the API level. REST APIs support tagging; usage plans and stages inherit API tags.'
    });
  }

  const computeInstanceSelectionElement = document.getElementById('input-ec2-instance');
  if (computeInstanceSelectionElement && computeInstanceSelectionElement.value) {
    activeServicesList.push({
      serviceName: 'Amazon EC2 (Compute Replicas)',
      tags: ['Application', 'Environment', 'Team', 'CostCentre', 'ServiceName', 'BaseRPS'],
      allocationNote: 'Tag every instance AND its EBS volumes. Use Launch Templates to enforce tags at launch time. Spot instances: add tag propagation in the Spot Fleet config.'
    });
  }

  // FinOps: show when any saving plan or RI discount is active
  if (hasSnapCost('finops')) {
    activeServicesList.push({
      serviceName: 'FinOps — Reserved Instances / Savings Plans',
      tags: ['Environment', 'Team', 'CostCentre', 'FinOpsReviewed'],
      allocationNote: 'Tag all Reserved Instances and Savings Plans with CostCentre and Team. Use AWS Cost Explorer RI utilisation reports to verify tag coverage.'
    });
  }

  return activeServicesList;
}