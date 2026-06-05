function goToPhase(phaseNumber) {
  const maxUnlockedPhase = getMaxUnlockedPhase();

  if (phaseNumber > maxUnlockedPhase) {
    highlightRequiredFields(phaseNumber);
    return;
  }

  document.querySelectorAll('.phase-card').forEach(card => card.classList.remove('visible'));

  const targetPhaseCard = document.getElementById(`phase${phaseNumber}`);
  if (targetPhaseCard) {
    if (phaseNumber === 5) {
      targetPhaseCard.style.display = 'block';
    }
    targetPhaseCard.classList.add('visible');
    targetPhaseCard.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  currentPhase = phaseNumber;
  updateStepIndicators();

  if (phaseNumber === 4) populateReportSummary();
  if (phaseNumber === 5) renderPhase5Portfolio();
}

function getMaxUnlockedPhase() {
  // Phase 5 is always accessible; phase 4 is unlocked via server-side rendering state.
  return 5;
}

function highlightRequiredFields(targetPhaseNumber) {
  const serviceNameInput = document.getElementById('serviceName');
  const rpsInput = document.getElementById('requestsPerSecond');
  const serviceName = (serviceNameInput?.value ?? '').trim();
  const requestsPerSecond = parseInt(rpsInput?.value, 10) || 0;

  // Phase 1 Validations
  if (!serviceName || !requestsPerSecond) {
    goToPhase(1);
    const missingField = !serviceName ? serviceNameInput : rpsInput;
    const missingMessage = !serviceName
      ? 'Please enter a Microservice Name to continue.'
      : 'Please enter the Base RPS to continue.';

    if (missingField) {
      missingField.focus();
      flashRequiredField(missingField);
    }
    showNavigationHint(missingMessage);
    return;
  }

  // Phase 2+ Validations (Requires Use Case Selection)
  if (targetPhaseNumber >= 3 && !window.selectedBUC) {
    goToPhase(2);
    showNavigationHint('Please select a Business Use Case to continue.');
    return;
  }

  // Phase 2+ Validations (Requires Proto Upload)
  if (targetPhaseNumber >= 3 && !window._lastProtoFile) {
    goToPhase(2);
    document.getElementById('fileUploadZone2')?.scrollIntoView({ behavior: 'smooth' });
    showNavigationHint('Please upload your .proto file to continue.');
    return;
  }

  // Phase 2+ Validations (Requires Consumers Value)
  if (targetPhaseNumber >= 3) {
    const consumersInput = document.getElementById('numConsumers');
    const totalConsumers = parseInt(consumersInput?.value, 10) || 0;

    if (!totalConsumers) {
      goToPhase(2);
      if (consumersInput) {
        consumersInput.focus();
        flashRequiredField(consumersInput);
      }
      showNavigationHint('Please enter the number of consumers to continue.');
    }
  }
}

function flashRequiredField(element) {
  if (!element) return;

  element.classList.add('is-invalid');

  setTimeout(() => {
    element.classList.remove('is-invalid');
  }, 2500);
}

function showNavigationHint(message) {
  document.getElementById('_nav-hint')?.remove();

  const hintContainer = document.createElement('div');
  hintContainer.id = '_nav-hint';
  hintContainer.textContent = message;

  cleanApplicationOfStyles();

  document.body.appendChild(hintContainer);
  setTimeout(() => hintContainer.remove(), 3500);
}

function updateStepIndicators() {
  const maxUnlockedPhase = getMaxUnlockedPhase();
  const TOTAL_PHASES = 5;

  for (let phaseIdx = 1; phaseIdx <= TOTAL_PHASES; phaseIdx++) {
    const progressDot = document.getElementById(`dot-${phaseIdx}`);
    const progressLabel = document.getElementById(`label-${phaseIdx}`);

    if (!progressDot || !progressLabel) continue;

    progressDot.className = 'step-dot';
    progressLabel.className = 'step-label';

    if (phaseIdx < currentPhase && phaseIdx <= maxUnlockedPhase) {
      progressDot.classList.add('done');
      progressLabel.classList.add('done');
    } else if (phaseIdx === currentPhase) {
      progressDot.classList.add('active');
      progressLabel.classList.add('active');
    }
  }
}

function cleanApplicationOfStyles(){
Object.assign(hintContainer.style, {
    position: 'fixed',
    bottom: '24px',
    left: '50%',
    transform: 'translateX(-50%)',
    background: 'var(--red)',
    color: '#fff',
    padding: '12px 24px',
    borderRadius: 'var(--r)',
    fontSize: '.88rem',
    fontWeight: '600',
    zIndex: '9999',
    boxShadow: 'var(--shadow-md)',
    animation: 'slideUp .2s ease',
    maxWidth: '90vw',
    textAlign: 'center'
  });
}