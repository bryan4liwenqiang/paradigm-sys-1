const ids = {
  businessRequest: document.getElementById("businessRequest"),
  domain: document.getElementById("domain"),
  status: document.getElementById("status"),
  sessionMeta: document.getElementById("sessionMeta"),
  stageStepsTitle: document.getElementById("stageStepsTitle"),
  stageCatalog: document.getElementById("stageCatalog"),
  reqSteps: document.getElementById("reqSteps"),
  chatBox: document.getElementById("chatBox"),
  chatForm: document.getElementById("chatForm"),
  chatInput: document.getElementById("chatInput"),
  sendBtn: document.getElementById("sendBtn"),
  startBtn: document.getElementById("startBtn"),
  recommendBtn: document.getElementById("recommendBtn"),
  followCurrentBtn: document.getElementById("followCurrentBtn"),
  viewingStageMeta: document.getElementById("viewingStageMeta"),
  paradigms: document.getElementById("paradigms"),
  metas: document.getElementById("metas"),
  methodologies: document.getElementById("methodologies"),
  methods: document.getElementById("methods"),
  appliedRules: document.getElementById("appliedRules"),
  droppedRules: document.getElementById("droppedRules")
};

const defaults = {
  businessRequest: "构建一个合同审批系统，支持多级审批、移动端与审计追踪。",
  domain: "finance"
};

let currentSession = null;
let lastRecommendedStage = null;
let selectedViewStage = null;
let lastCurrentStage = null;
const STORAGE_KEY = "sdlc_ui_state_v1";

ids.startBtn.addEventListener("click", async () => {
  await startSession();
});

ids.chatForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  await sendMessage();
});

ids.recommendBtn.addEventListener("click", async () => {
  await runRecommend(selectedViewStage || currentSession?.currentStage || "requirements");
});
ids.followCurrentBtn.addEventListener("click", async () => {
  selectedViewStage = null;
  renderStages(currentSession?.stages || []);
  await runRecommend(currentSession?.currentStage || "requirements");
});
bindDraftPersistence();

function loadDefaults() {
  ids.businessRequest.value = defaults.businessRequest;
  ids.domain.value = defaults.domain;
}

async function startSession() {
  const payload = {
    businessRequest: ids.businessRequest.value.trim(),
    domain: ids.domain.value
  };
  if (!payload.businessRequest) {
    setStatus("请先输入业务诉求", false, true);
    return;
  }
  ids.startBtn.disabled = true;
  setStatus("正在创建会话并启动需求澄清...", false, false);
  try {
    const res = await fetch("/api/v1/sessions/start", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || "启动失败");
    currentSession = data;
    renderSession(data);
    saveState();
    setStatus("会话已启动，开始需求阶段互动。", true, false);
  } catch (err) {
    setStatus("启动失败: " + err.message, false, true);
  } finally {
    ids.startBtn.disabled = false;
  }
}

async function sendMessage() {
  if (!currentSession?.sessionId) {
    setStatus("请先启动会话", false, true);
    return;
  }
  // 交互时始终跟随真实当前阶段，避免停留在手动查看阶段
  selectedViewStage = null;
  const msg = ids.chatInput.value.trim();
  if (!msg) return;
  ids.sendBtn.disabled = true;
  ids.chatInput.value = "";
  appendMessage("user", msg);
  const pendingNode = appendMessage("assistant", "正在分析中（方法论匹配 + AI追问）...", true);
  setStatus("正在生成反馈...", false, false);
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 8000);
  try {
    const res = await fetch(`/api/v1/sessions/${currentSession.sessionId}/message`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message: msg }),
      signal: controller.signal
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || "发送失败");
    currentSession = data;
    renderSession(data);
    saveState();
    setStatus("已生成方法论反馈与下一步问题。", true, false);
  } catch (err) {
    if (pendingNode && pendingNode.parentNode) {
      pendingNode.parentNode.removeChild(pendingNode);
    }
    setStatus("发送失败或超时，请重试。", false, true);
  } finally {
    clearTimeout(timer);
    ids.sendBtn.disabled = false;
  }
}

function renderSession(data) {
  const prevStage = lastCurrentStage;
  lastCurrentStage = data.currentStage;
  // 阶段推进时自动回到跟随模式，展示新阶段步骤
  if (prevStage && prevStage !== data.currentStage) {
    selectedViewStage = null;
  }
  ids.sessionMeta.textContent = `会话ID: ${data.sessionId} | 当前阶段: ${data.currentStageName || data.currentStage} | 互动方式: ${data.currentStageInteraction || "-"}`;
  const viewStage = selectedViewStage || data.currentStage;
  const stageMeta = (data.stages || []).find(s => s.code === viewStage) || {};
  ids.stageStepsTitle.textContent = `${stageMeta.name || data.currentStageName || data.currentStage}步骤（${stageMeta.interactionStyle || data.currentStageInteraction || "通用互动"}）`;
  renderStages(data.stages || []);
  const wf = data.stageDetails?.[viewStage]?.workflow || data.currentStageWorkflow;
  renderReqSteps(wf?.steps || []);
  renderMessages(data.messages || []);
  saveState();
  if (prevStage && prevStage !== data.currentStage) {
    setStatus(`已进入下一阶段：${data.currentStageName || data.currentStage}，请继续按该阶段步骤互动。`, true, false);
  }
  if (!selectedViewStage && data.currentStage && data.currentStage !== lastRecommendedStage) {
    runRecommend(data.currentStage);
  } else if (selectedViewStage) {
    updateViewingStageMeta(selectedViewStage, data.currentStage);
  }
}

function renderStages(stages) {
  ids.stageCatalog.innerHTML = "";
  const currentStageCode = currentSession?.currentStage;
  const viewingStage = selectedViewStage || currentStageCode || "requirements";
  for (const s of stages) {
    const div = document.createElement("div");
    div.className = `catalog-item ${s.status.toLowerCase()}${viewingStage === s.code ? " viewing" : ""}`;
    const currentTag = s.code === currentStageCode ? "（当前）" : "";
    const viewTag = s.code === viewingStage ? "（查看中）" : "";
    div.innerHTML = `
      <div class="name">${s.name}${currentTag}${viewTag}</div>
      <div class="style">${s.interactionStyle || "-"}</div>
      <div class="desc">${s.description || ""}</div>
      <div class="desc">状态：${s.status}</div>
    `;
    div.addEventListener("click", async () => {
      selectedViewStage = s.code;
      renderStages(stages);
      const wf = currentSession?.stageDetails?.[s.code]?.workflow;
      renderReqSteps(wf?.steps || []);
      ids.stageStepsTitle.textContent = `${s.name}步骤（${s.interactionStyle || "通用互动"}）`;
      await runRecommend(s.code);
    });
    ids.stageCatalog.appendChild(div);
  }
  updateViewingStageMeta(viewingStage, currentStageCode);
}

function renderReqSteps(steps) {
  ids.reqSteps.innerHTML = "";
  for (const s of steps) {
    const div = document.createElement("div");
    div.className = `step-item ${s.status.toLowerCase()}`;
    const answer = s.answer ? `<div class="step-answer">已记录: ${escapeHtml(s.answer)}</div>` : "";
    div.innerHTML = `<div class="step-title">${s.name} (${s.status})</div><div>${escapeHtml(s.question)}</div>${answer}`;
    ids.reqSteps.appendChild(div);
  }
}

function renderMessages(messages) {
  ids.chatBox.innerHTML = "";
  for (const m of messages) {
    const div = document.createElement("div");
    div.className = `msg ${m.role}`;
    div.textContent = m.content;
    ids.chatBox.appendChild(div);
  }
  ids.chatBox.scrollTop = ids.chatBox.scrollHeight;
}

function appendMessage(role, content, pending = false) {
  const div = document.createElement("div");
  div.className = `msg ${role}${pending ? " pending" : ""}`;
  div.textContent = content;
  ids.chatBox.appendChild(div);
  ids.chatBox.scrollTop = ids.chatBox.scrollHeight;
  return div;
}

function escapeHtml(str) {
  return str
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

async function runRecommend(stageCode) {
  const targetStage = stageCode || currentSession?.currentStage || "requirements";
  const payload = {
    projectId: 1001,
    stageCode: targetStage,
    context: {
      domain: ids.domain.value
    }
  };
  setStatus(`正在生成 ${targetStage} 阶段方法推荐...`, false, false);
  ids.recommendBtn.disabled = true;
  try {
    const res = await fetch("/api/v1/mapping/recommend", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || "请求失败");
    renderList(ids.paradigms, data.recommendedParadigms);
    renderList(ids.metas, data.recommendedMetaMethodologies);
    renderList(ids.methodologies, data.recommendedMethodologies);
    renderList(ids.methods, data.recommendedMethods);
    renderStringList(ids.appliedRules, data.trace?.appliedRules || []);
    renderStringList(ids.droppedRules, data.trace?.droppedByCondition || []);
    lastRecommendedStage = targetStage;
    saveState();
    updateViewingStageMeta(targetStage, currentSession?.currentStage);
    setStatus("方法推荐已生成。", true, false);
  } catch (err) {
    clearResults();
    setStatus("方法推荐失败: " + err.message, false, true);
  } finally {
    ids.recommendBtn.disabled = false;
  }
}

function renderList(target, list) {
  target.innerHTML = "";
  if (!list || list.length === 0) {
    target.innerHTML = "<li>无数据</li>";
    return;
  }
  for (const item of list) {
    const li = document.createElement("li");
    li.textContent = `${item.name} (${item.code})`;
    target.appendChild(li);
  }
}

function renderStringList(target, list) {
  target.innerHTML = "";
  if (!list || list.length === 0) {
    target.innerHTML = "<li>无</li>";
    return;
  }
  for (const item of list) {
    const li = document.createElement("li");
    li.textContent = item;
    target.appendChild(li);
  }
}

function clearResults() {
  renderList(ids.paradigms, []);
  renderList(ids.metas, []);
  renderList(ids.methodologies, []);
  renderList(ids.methods, []);
  renderStringList(ids.appliedRules, []);
  renderStringList(ids.droppedRules, []);
}

function snapshotState() {
  return {
    form: {
      businessRequest: ids.businessRequest.value,
      domain: ids.domain.value,
      chatInput: ids.chatInput.value
    },
    sessionId: currentSession?.sessionId || null,
    lastRecommendedStage,
    selectedViewStage,
    recommendations: {
      paradigms: ids.paradigms.innerHTML,
      metas: ids.metas.innerHTML,
      methodologies: ids.methodologies.innerHTML,
      methods: ids.methods.innerHTML,
      appliedRules: ids.appliedRules.innerHTML,
      droppedRules: ids.droppedRules.innerHTML
    }
  };
}

function saveState() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshotState()));
  } catch (_) {
    // ignore storage failures
  }
}

function restoreState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw);
  } catch (_) {
    return null;
  }
}

function applyFormState(form) {
  if (!form) return;
  ids.businessRequest.value = form.businessRequest ?? ids.businessRequest.value;
  ids.domain.value = form.domain ?? ids.domain.value;
  ids.chatInput.value = form.chatInput ?? "";
}

function applyRecommendationState(rec) {
  if (!rec) return;
  ids.paradigms.innerHTML = rec.paradigms || "<li>无数据</li>";
  ids.metas.innerHTML = rec.metas || "<li>无数据</li>";
  ids.methodologies.innerHTML = rec.methodologies || "<li>无数据</li>";
  ids.methods.innerHTML = rec.methods || "<li>无数据</li>";
  ids.appliedRules.innerHTML = rec.appliedRules || "<li>无</li>";
  ids.droppedRules.innerHTML = rec.droppedRules || "<li>无</li>";
}

function updateViewingStageMeta(viewingStage, currentStage) {
  const view = viewingStage || "-";
  const current = currentStage || "-";
  const mode = selectedViewStage ? "手动查看" : "自动跟随";
  ids.viewingStageMeta.textContent = `查看阶段：${view} ｜ 当前阶段：${current} ｜ 模式：${mode}`;
}

function bindDraftPersistence() {
  const fields = [
    ids.businessRequest, ids.domain, ids.chatInput
  ];
  for (const el of fields) {
    el.addEventListener("input", saveState);
    el.addEventListener("change", saveState);
  }
}

async function resumeSession(sessionId) {
  if (!sessionId) return false;
  try {
    const res = await fetch(`/api/v1/sessions/${sessionId}`);
    if (!res.ok) return false;
    const data = await res.json();
    currentSession = data;
    renderSession(data);
    setStatus("已恢复上次会话。", true, false);
    return true;
  } catch (_) {
    return false;
  }
}

function setStatus(text, ok, err) {
  ids.status.textContent = text;
  ids.status.classList.toggle("ok", !!ok);
  ids.status.classList.toggle("err", !!err);
}

async function bootstrap() {
  loadDefaults();
  clearResults();
  const saved = restoreState();
  if (!saved) return;
  applyFormState(saved.form);
  lastRecommendedStage = saved.lastRecommendedStage || null;
  selectedViewStage = saved.selectedViewStage || null;
  applyRecommendationState(saved.recommendations);
  const resumed = await resumeSession(saved.sessionId);
  if (!resumed && saved.sessionId) {
    setStatus("未找到历史会话，已恢复本地输入。", false, true);
  }
  if (!resumed) {
    try {
      const res = await fetch("/api/v1/stages");
      if (res.ok) {
        const stages = await res.json();
        renderStages(stages);
      }
    } catch (_) {
      // ignore catalog preload failure
    }
  }
}

bootstrap();
