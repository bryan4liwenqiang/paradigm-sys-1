const ids = {
  businessRequest: document.getElementById("businessRequest"),
  domain: document.getElementById("domain"),
  domainSelect: document.getElementById("domainSelect"),
  domainMode: document.getElementById("domainMode"),
  complianceLevel: document.getElementById("complianceLevel"),
  complianceLevelSelect: document.getElementById("complianceLevelSelect"),
  complianceLevelMode: document.getElementById("complianceLevelMode"),
  timelineLevel: document.getElementById("timelineLevel"),
  timelineLevelSelect: document.getElementById("timelineLevelSelect"),
  timelineLevelMode: document.getElementById("timelineLevelMode"),
  businessGoal: document.getElementById("businessGoal"),
  businessGoalSelect: document.getElementById("businessGoalSelect"),
  businessGoalMode: document.getElementById("businessGoalMode"),
  successKpi: document.getElementById("successKpi"),
  successKpiSelect: document.getElementById("successKpiSelect"),
  successKpiMode: document.getElementById("successKpiMode"),
  stakeholders: document.getElementById("stakeholders"),
  stakeholdersSelect: document.getElementById("stakeholdersSelect"),
  stakeholdersMode: document.getElementById("stakeholdersMode"),
  constraints: document.getElementById("constraints"),
  constraintsSelect: document.getElementById("constraintsSelect"),
  constraintsMode: document.getElementById("constraintsMode"),
  status: document.getElementById("status"),
  sessionMeta: document.getElementById("sessionMeta"),
  stageStepsTitle: document.getElementById("stageStepsTitle"),
  chatTitle: document.getElementById("chatTitle"),
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
  ,
  stageExplainability: document.getElementById("stageExplainability")
};

const defaults = {
  businessRequest: "构建一个合同审批系统，支持多级审批、移动端与审计追踪。",
  domain: "finance",
  complianceLevel: "L2",
  timelineLevel: "normal",
  businessGoal: "",
  successKpi: "",
  stakeholders: "",
  constraints: ""
};

const comboFieldConfigs = [
  {
    key: "domain",
    selectId: "domainSelect",
    inputId: "domain",
    modeId: "domainMode",
    options: ["finance", "retail", "manufacturing", "healthcare", "education"]
  },
  {
    key: "complianceLevel",
    selectId: "complianceLevelSelect",
    inputId: "complianceLevel",
    modeId: "complianceLevelMode",
    options: ["L1", "L2", "L3"]
  },
  {
    key: "timelineLevel",
    selectId: "timelineLevelSelect",
    inputId: "timelineLevel",
    modeId: "timelineLevelMode",
    options: ["tight", "normal", "relaxed"]
  },
  {
    key: "businessGoal",
    selectId: "businessGoalSelect",
    inputId: "businessGoal",
    modeId: "businessGoalMode",
    options: ["审批效率提升", "库存准确率提升", "人力成本下降", "合规审计通过率提升"]
  },
  {
    key: "successKpi",
    selectId: "successKpiSelect",
    inputId: "successKpi",
    modeId: "successKpiMode",
    options: ["平均处理时长 < 24h", "错误率 < 1%", "自动化率 > 70%", "一次通过率 > 95%"]
  },
  {
    key: "stakeholders",
    selectId: "stakeholdersSelect",
    inputId: "stakeholders",
    modeId: "stakeholdersMode",
    options: ["申请人,审批人,财务", "仓管,运营,客服", "项目经理,开发,测试", "法务,合规,审计"]
  },
  {
    key: "constraints",
    selectId: "constraintsSelect",
    inputId: "constraints",
    modeId: "constraintsMode",
    options: ["预算固定", "上线窗口受限", "必须满足审计要求", "只能私有化部署"]
  }
];

let currentSession = null;
let lastRecommendedStage = null;
let selectedViewStage = null;
let lastCurrentStage = null;
const STORAGE_KEY = "sdlc_ui_state_v1";
const MESSAGE_REQUEST_TIMEOUT_MS = 30000;

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
  ids.complianceLevel.value = defaults.complianceLevel;
  ids.timelineLevel.value = defaults.timelineLevel;
  ids.businessGoal.value = defaults.businessGoal;
  ids.successKpi.value = defaults.successKpi;
  ids.stakeholders.value = defaults.stakeholders;
  ids.constraints.value = defaults.constraints;
  initComboFields();
  for (const cfg of comboFieldConfigs) {
    const modeEl = ids[cfg.modeId];
    if (modeEl) {
      modeEl.value = "select";
    }
    syncComboFieldMode(cfg);
  }
}

async function startSession() {
  const payload = buildSessionPayload();
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

function initComboFields() {
  for (const cfg of comboFieldConfigs) {
    const selectEl = ids[cfg.selectId];
    const inputEl = ids[cfg.inputId];
    const modeEl = ids[cfg.modeId];
    if (!selectEl || !inputEl || !modeEl) {
      continue;
    }
    if (!selectEl.dataset.initialized) {
      renderSelectOptions(selectEl, cfg.options);
      selectEl.dataset.initialized = "1";
    }
    if (inputEl.value && !cfg.options.includes(inputEl.value)) {
      modeEl.value = "manual";
    }
    if (!inputEl.value && cfg.options.length > 0) {
      inputEl.value = cfg.options[0];
    }
    const normalized = normalizeComboValue(modeEl.value, inputEl.value, cfg.options);
    inputEl.value = normalized.value;
    modeEl.value = normalized.mode;
    if (normalized.mode === "select") {
      selectEl.value = normalized.value;
    }
    modeEl.addEventListener("change", () => {
      syncComboFieldMode(cfg);
      saveState();
    });
    selectEl.addEventListener("change", () => {
      if (modeEl.value === "select") {
        inputEl.value = selectEl.value;
      }
      saveState();
    });
  }
}

function renderSelectOptions(selectEl, options) {
  selectEl.innerHTML = "";
  for (const item of options) {
    const option = document.createElement("option");
    option.value = item;
    option.textContent = item;
    selectEl.appendChild(option);
  }
}

function normalizeComboValue(mode, value, options) {
  const trimmed = (value || "").trim();
  if (mode === "manual") {
    return { mode: "manual", value: trimmed };
  }
  if (trimmed && options.includes(trimmed)) {
    return { mode: "select", value: trimmed };
  }
  if (!trimmed && options.length > 0) {
    return { mode: "select", value: options[0] };
  }
  return { mode: "manual", value: trimmed };
}

function syncComboFieldMode(cfg) {
  const selectEl = ids[cfg.selectId];
  const inputEl = ids[cfg.inputId];
  const modeEl = ids[cfg.modeId];
  if (!selectEl || !inputEl || !modeEl) {
    return;
  }
  const isSelect = modeEl.value === "select";
  selectEl.disabled = !isSelect;
  inputEl.readOnly = isSelect;
  if (isSelect) {
    if (!selectEl.value && cfg.options.length > 0) {
      selectEl.value = cfg.options[0];
    }
    inputEl.value = selectEl.value;
  }
}

function buildSessionPayload() {
  const payload = {
    businessRequest: ids.businessRequest.value.trim()
  };
  for (const cfg of comboFieldConfigs) {
    const inputEl = ids[cfg.inputId];
    const modeEl = ids[cfg.modeId];
    const selectEl = ids[cfg.selectId];
    if (!inputEl || !modeEl || !selectEl) {
      continue;
    }
    syncComboFieldMode(cfg);
    const value = inputEl.value.trim();
    if (value) {
      payload[cfg.key] = value;
      payload[`${cfg.key}InputMode`] = modeEl.value;
    }
  }
  return payload;
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
  const timer = setTimeout(() => controller.abort(), MESSAGE_REQUEST_TIMEOUT_MS);
  const prevStage = currentSession?.currentStage || null;
  const prevIdx = currentSession?.currentStageWorkflow?.currentStepIdx ?? -1;
  try {
    const res = await fetch(`/api/v1/sessions/${currentSession.sessionId}/message`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message: msg }),
      signal: controller.signal
    });
    const data = await safeJson(res);
    if (!res.ok) throw new Error(data.error || "发送失败");
    currentSession = data;
    renderSession(data);
    saveState();
    const unchangedStep = prevStage && prevStage === data.currentStage
      && prevIdx === (data.currentStageWorkflow?.currentStepIdx ?? -1);
    if (unchangedStep) {
      setStatus("当前信息仍不足，系统已给出补充问题，请继续完善。", false, false);
    } else {
      setStatus("已生成方法论反馈与下一步问题。", true, false);
    }
  } catch (err) {
    if (pendingNode && pendingNode.parentNode) {
      pendingNode.parentNode.removeChild(pendingNode);
    }
    if (err?.name === "AbortError") {
      const resumed = await resumeSession(currentSession.sessionId);
      if (resumed) {
        setStatus("响应较慢，已自动同步最新结果。", true, false);
      } else {
        setStatus("响应超时，请稍后重试。", false, true);
      }
    } else {
      setStatus("发送失败: " + (err.message || "未知错误"), false, true);
    }
  } finally {
    clearTimeout(timer);
    ids.sendBtn.disabled = false;
  }
}

async function safeJson(res) {
  try {
    return await res.json();
  } catch (_) {
    return {};
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
  updateChatTitle(data.currentStageName || data.currentStage);
  const viewStage = selectedViewStage || data.currentStage;
  const stageMeta = (data.stages || []).find(s => s.code === viewStage) || {};
  ids.stageStepsTitle.textContent = `${stageMeta.name || data.currentStageName || data.currentStage}步骤（${stageMeta.interactionStyle || data.currentStageInteraction || "通用互动"}）`;
  renderStages(data.stages || []);
  const wf = data.stageDetails?.[viewStage]?.workflow || data.currentStageWorkflow;
  renderReqSteps(wf?.steps || []);
  renderStageExplainability(data, viewStage);
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

function updateChatTitle(currentStageName) {
  const stageName = currentStageName || "当前阶段";
  ids.chatTitle.textContent = `${stageName}互动`;
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

function renderStageExplainability(data, stageCode) {
  const byStage = data.stageDetails?.[stageCode]?.explainability;
  const current = data.currentStageExplainability;
  const exp = byStage || (stageCode === data.currentStage ? current : null);
  if (!exp) {
    ids.stageExplainability.textContent = "暂无阶段解释信息";
    return;
  }
  const facts = (exp.confirmedFacts || []).slice(0, 3);
  const gaps = exp.keyGaps || [];
  const unresolved = exp.unresolvedItems || [];
  ids.stageExplainability.innerHTML = `
    <div><strong>阶段目标：</strong>${escapeHtml(exp.stageGoal || "-")}</div>
    <div><strong>已确认事实：</strong>${facts.length ? facts.map(escapeHtml).join("；") : "暂无"}</div>
    <div><strong>关键缺口：</strong>${gaps.length ? gaps.map(escapeHtml).join("、") : "暂无"}</div>
    <div><strong>下一步建议：</strong>${escapeHtml(exp.nextAction || "-")}</div>
    <div><strong>未决项：</strong>${unresolved.length ? unresolved.map(escapeHtml).join("、") : "暂无"}</div>
  `;
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
  const comboModes = {};
  for (const cfg of comboFieldConfigs) {
    const modeEl = ids[cfg.modeId];
    if (modeEl) {
      comboModes[cfg.key] = modeEl.value;
    }
  }
  return {
    form: {
      businessRequest: ids.businessRequest.value,
      domain: ids.domain.value,
      complianceLevel: ids.complianceLevel.value,
      timelineLevel: ids.timelineLevel.value,
      businessGoal: ids.businessGoal.value,
      successKpi: ids.successKpi.value,
      stakeholders: ids.stakeholders.value,
      constraints: ids.constraints.value,
      chatInput: ids.chatInput.value,
      comboModes
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
  ids.complianceLevel.value = form.complianceLevel ?? ids.complianceLevel.value;
  ids.timelineLevel.value = form.timelineLevel ?? ids.timelineLevel.value;
  ids.businessGoal.value = form.businessGoal ?? "";
  ids.successKpi.value = form.successKpi ?? "";
  ids.stakeholders.value = form.stakeholders ?? "";
  ids.constraints.value = form.constraints ?? "";
  ids.chatInput.value = form.chatInput ?? "";
  const comboModes = form.comboModes || {};
  for (const cfg of comboFieldConfigs) {
    const modeEl = ids[cfg.modeId];
    if (modeEl && comboModes[cfg.key]) {
      modeEl.value = comboModes[cfg.key];
    }
    syncComboFieldMode(cfg);
  }
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
    ids.businessRequest,
    ids.domain,
    ids.complianceLevel,
    ids.timelineLevel,
    ids.businessGoal,
    ids.successKpi,
    ids.stakeholders,
    ids.constraints,
    ids.chatInput
  ];
  for (const cfg of comboFieldConfigs) {
    if (ids[cfg.selectId]) {
      fields.push(ids[cfg.selectId]);
    }
    if (ids[cfg.modeId]) {
      fields.push(ids[cfg.modeId]);
    }
  }
  for (const el of fields) {
    if (!el) continue;
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
  updateChatTitle(null);
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
