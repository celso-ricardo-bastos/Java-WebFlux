const form = document.querySelector("#request-form");
const cpfInput = document.querySelector("#cpf");
const endpointInput = document.querySelector("#endpoint");
const submitButton = document.querySelector("#submit-button");
const clearButton = document.querySelector("#clear-cpf");
const progressSection = document.querySelector("#progress-section");
const progressLabel = document.querySelector("#progress-label");
const elapsedLabel = document.querySelector("#elapsed");
const progressFill = document.querySelector("#progress-fill");
const progressTrack = document.querySelector(".progress-track");
const responseState = document.querySelector("#response-state");
const responseEmpty = document.querySelector("#response-empty");
const responseResult = document.querySelector("#response-result");
const statusOutput = document.querySelector("#http-status");
const durationOutput = document.querySelector("#response-duration");
const payloadOutput = document.querySelector("#payload");
const payloadFormat = document.querySelector("#payload-format");
const toast = document.querySelector("#toast");

let animationFrame = 0;
let toastTimer = 0;

cpfInput.addEventListener("input", () => {
  const digits = cpfInput.value.replace(/\D/g, "").slice(0, 11);
  cpfInput.value = formatCpf(digits);
});

clearButton.addEventListener("click", () => {
  cpfInput.value = "";
  cpfInput.focus();
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const cpf = cpfInput.value.replace(/\D/g, "");
  const endpoint = endpointInput.value.trim();

  if (cpf.length !== 11) {
    showToast("Informe um CPF com 11 números.");
    cpfInput.focus();
    return;
  }

  if (!isValidEndpoint(endpoint)) {
    showToast("Informe uma URL válida para o endpoint.");
    endpointInput.focus();
    return;
  }

  await sendRequest(endpoint, cpf);
});

function formatCpf(digits) {
  if (digits.length <= 3) return digits;
  if (digits.length <= 6) return `${digits.slice(0, 3)}.${digits.slice(3)}`;
  if (digits.length <= 9) return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6)}`;
  return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`;
}

function isValidEndpoint(value) {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

async function sendRequest(endpoint, cpf) {
  const startedAt = performance.now();
  const controller = new AbortController();
  const browserTimeout = window.setTimeout(() => controller.abort(), 5000);
  submitButton.disabled = true;
  submitButton.querySelector(".button-label").textContent = "Aguardando resposta";
  responseEmpty.hidden = true;
  responseResult.hidden = true;
  progressSection.hidden = false;
  responseState.textContent = "EM ANDAMENTO";
  responseState.className = "response-state is-loading";
  progressLabel.textContent = "Validando fluxo reativo";
  progressFill.style.width = "0%";
  progressTrack.setAttribute("aria-valuenow", "0");

  animateElapsed(startedAt);

  try {
    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "*/*"
      },
      body: JSON.stringify({ cpf }),
      signal: controller.signal
    });

    const elapsedMs = performance.now() - startedAt;
    const rawBody = await response.text();
    const parsedBody = parseResponse(rawBody);
    renderResponse(response.status, elapsedMs, parsedBody);
  } catch (error) {
    const elapsedMs = performance.now() - startedAt;
    const message = error.name === "AbortError"
      ? "A requisição excedeu o limite local de espera do navegador."
      : "Não foi possível conectar à API. Confirme se o backend está ativo e se o CORS permite esta origem.";
    renderResponse("NET", elapsedMs, { erro: message }, true);
    showToast(message);
  } finally {
    window.clearTimeout(browserTimeout);
    cancelAnimationFrame(animationFrame);
    submitButton.disabled = false;
    submitButton.querySelector(".button-label").textContent = "Enviar requisição";
  }
}

function animateElapsed(startedAt) {
  const update = () => {
    const elapsedMs = performance.now() - startedAt;
    const budgetPercent = Math.min((elapsedMs / 1000) * 100, 100);
    elapsedLabel.textContent = formatDuration(elapsedMs);
    progressFill.style.width = `${budgetPercent}%`;
    progressTrack.setAttribute("aria-valuenow", String(Math.round(budgetPercent)));
    if (!responseResult.hidden) return;
    animationFrame = requestAnimationFrame(update);
  };
  animationFrame = requestAnimationFrame(update);
}

function parseResponse(rawBody) {
  if (!rawBody) return "(resposta sem corpo)";
  try {
    return JSON.parse(rawBody);
  } catch {
    return rawBody;
  }
}

function renderResponse(status, elapsedMs, body, isNetworkError = false) {
  responseResult.hidden = false;
  progressSection.hidden = true;
  statusOutput.textContent = typeof status === "number" ? `${status} ${statusLabel(status)}` : status;
  statusOutput.classList.toggle("status-error", isNetworkError || Number(status) >= 400);
  durationOutput.textContent = formatDuration(elapsedMs);
  const isJson = body !== null && typeof body === "object";
  payloadFormat.textContent = isJson ? "JSON" : "TEXT";
  payloadOutput.textContent = isJson ? JSON.stringify(body, null, 2) : String(body);
  payloadOutput.classList.toggle("is-error", isNetworkError || Number(status) >= 400);

  const successful = !isNetworkError && Number(status) < 400;
  responseState.textContent = isNetworkError ? "SEM CONEXÃO" : successful ? "CONCLUÍDO" : "REGRA / TIMEOUT";
  responseState.className = `response-state ${successful ? "is-success" : "is-error"}`;
}

function statusLabel(status) {
  if (status === 200) return "OK";
  if (status === 400) return "BAD REQUEST";
  if (status === 422) return "UNPROCESSABLE";
  return "HTTP";
}

function formatDuration(milliseconds) {
  if (milliseconds < 1000) return `${Math.round(milliseconds)} ms`;
  return `${(milliseconds / 1000).toFixed(2)} s`;
}

function showToast(message) {
  window.clearTimeout(toastTimer);
  toast.textContent = message;
  toast.classList.add("is-visible");
  toastTimer = window.setTimeout(() => toast.classList.remove("is-visible"), 3400);
}
