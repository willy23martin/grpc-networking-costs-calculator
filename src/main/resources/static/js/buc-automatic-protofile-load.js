const BUSINESS_USE_CASE_PROTO_MAP = {
  'BUC1': 'unaryRPCPattern.proto',
  'BUC2': 'serverStreamingRPCPattern.proto',
  'BUC3': 'clientStreamingRPCPattern.proto',
  'BUC4': 'biDirectionalStreamingRPCPattern.proto'
};

const PROTO_BASE_URL = '/protos/basicBusinessUseCases/';

function selectBusinessUseCase(useCase, cardElement) {
  document.querySelectorAll('.buc-card').forEach(card => card.classList.remove('selected'));

  let finalUseCaseValue = null;

  if (useCase !== 'CUSTOM') {
    const customNameInput = document.getElementById('customBucName');
    if (customNameInput) {
      customNameInput.value = '';
    }

    finalUseCaseValue = useCase;
    cardElement?.classList.add('selected');
    autoLoadProtoForUseCase(useCase);
  } else {
    const customInput = document.getElementById('customBucName');
    const customValue = (customInput?.value ?? '').trim();
    finalUseCaseValue = customValue ? `CUSTOM:${customValue}` : null;
  }

  window.selectedBUC = finalUseCaseValue;

  const hasSelectedUseCase = Boolean(finalUseCaseValue);
  toggleDependentSections(hasSelectedUseCase);

  updatePhase2NextButton();
  updateStepIndicators();
  scheduleSessionSave();

  if (finalUseCaseValue) {
    sessionStorage.setItem('svc_buc', finalUseCaseValue);
  } else {
    sessionStorage.removeItem('svc_buc');
  }
}

function toggleDependentSections(isVisible) {
  const displayStyle = isVisible ? 'block' : 'none';
  const protoSection = document.getElementById('phase2-proto-section');
  const consumersSection = document.getElementById('phase2-consumers-section');

  if (protoSection) protoSection.style.display = displayStyle;
  if (consumersSection) consumersSection.style.display = displayStyle;
}

function autoLoadProtoForUseCase(useCase) {
  const fileName = BUSINESS_USE_CASE_PROTO_MAP[useCase];
  if (!fileName) return;

  const url = `${PROTO_BASE_URL}${fileName}`;
  const indicator = document.getElementById('proto-autoload-indicator');
  const fileNameDisplay = document.getElementById('fileNameDisplay2');

  if (indicator) {
    indicator.textContent = `⏳ Loading ${fileName}…`;
    indicator.style.color = 'var(--ink-light)';
  }

  fetch(url)
    .then(response => {
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      return response.blob();
    })
    .then(blob => {
      const file = new File([blob], fileName, { type: 'text/plain' });
      window._lastProtoFile = file;

      syncFileToInputDropzone(file);

      if (fileNameDisplay) {
        fileNameDisplay.textContent = `✓  ${fileName}  (pre-loaded for ${useCase})`;
      }
      if (indicator) {
        indicator.textContent = `✓ ${fileName} pre-loaded`;
        indicator.style.color = 'var(--green)';
      }
      updatePhase2NextButton();
    })
    .catch(error => {
      console.warn(
        `Proto auto-load failed for ${useCase}:`, error.message,
        '— Upload the file manually or ensure Spring Boot serves /protos/basicBusinessUseCases/'
      );
      if (indicator) {
        indicator.textContent = `⚠️ Could not auto-load ${fileName} — please upload manually.`;
        indicator.style.color = 'var(--amber)';
      }
    });
}

function updatePhase2NextButton() {
  const nextButton = document.getElementById('phase2NextBtn');
  if (!nextButton) return;

  const consumersInput = document.getElementById('numConsumers');
  const totalConsumers = parseInt(consumersInput?.value, 10) || 0;
  
  const isFormValid = Boolean(window.selectedBUC) && Boolean(window._lastProtoFile) && totalConsumers > 0;

  nextButton.disabled = !isFormValid;
  nextButton.style.opacity = isFormValid ? '1' : '0.45';
  nextButton.style.cursor = isFormValid ? 'pointer' : 'not-allowed';
}

function handlePhase2ProtoSelect(input) {
  const fileNameDisplay = document.getElementById('fileNameDisplay2');

  if (input.files?.length) {
    const selectedFile = input.files[0];
    window._lastProtoFile = selectedFile;
    
    if (fileNameDisplay) {
      fileNameDisplay.textContent = `✓  ${selectedFile.name}`;
    }
    syncFileToInputDropzone(selectedFile);
  } else {
    window._lastProtoFile = null;
    if (fileNameDisplay) {
      fileNameDisplay.textContent = '';
    }
  }

  updatePhase2NextButton();
  updateStepIndicators();
}

function syncFileToInputDropzone(file) {
  try {
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    const fileInput = document.getElementById('protoFile');
    if (fileInput) {
      fileInput.files = dataTransfer.files;
    }
  } catch (error) {
    console.error("DataTransfer API not supported or failed:", error);
  }
}

function submitProtoAndCalculate() {
  if (!window._lastProtoFile) return;

  saveTacticsToSession().then(() => {
    const loadingBar = document.getElementById('loadingBar');
    if (loadingBar) loadingBar.classList.add('visible');

    const calculateButton = document.getElementById('phase3CalcBtn');
    if (calculateButton) {
      calculateButton.disabled = true;
      calculateButton.innerHTML = '<span class="spinner"></span> Calculating…';
    }

    const uploadForm = document.getElementById('uploadForm');
    uploadForm?.submit();
  });
}

function enforceAuthMutualExclusivityBetweenOAuthAndBasicCheckboxes(selectedType) {
  const oauthCheckbox = document.getElementById('tactic-oauth');
  const basicAuthCheckbox = document.getElementById('tactic-basic-auth');

  if (selectedType === 'oauth' && oauthCheckbox?.checked) {
    if (basicAuthCheckbox) basicAuthCheckbox.checked = false;
  } else if (selectedType === 'basic' && basicAuthCheckbox?.checked) {
    if (oauthCheckbox) oauthCheckbox.checked = false;
    toggleOAuthParams(false);
  }
  
  scheduleSessionSave();
}