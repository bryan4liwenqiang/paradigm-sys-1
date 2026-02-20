import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappingServer {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(Long.parseLong(System.getenv().getOrDefault("OPENROUTER_CONNECT_TIMEOUT_MS", "1500"))))
            .build();
    private static final Map<String, SessionState> SESSIONS = new ConcurrentHashMap<>();
    private static final String SESSION_STORE_FILE = System.getenv().getOrDefault("SESSION_STORE_FILE", "standalone/data/sessions.bin");
    private static final List<StageDef> STAGE_DEFS = List.of(
            new StageDef("requirements", "需求阶段", "澄清式访谈", "明确业务目标、角色场景、流程与验收标准"),
            new StageDef("design", "设计阶段", "方案评审式", "形成架构方案、领域模型与质量属性设计"),
            new StageDef("planning", "规划阶段", "里程碑拆解式", "明确里程碑、资源依赖与阶段闸门"),
            new StageDef("development", "开发阶段", "实现约束确认式", "确定实现范围、工程规范与技术风险"),
            new StageDef("testing", "测试阶段", "风险验证式", "定义测试策略、关键用例与退出标准"),
            new StageDef("deployment", "部署阶段", "发布就绪检查式", "明确发布策略、回滚预案与发布闸门"),
            new StageDef("implementation", "实施阶段", "业务落地辅导式", "推动试点上线、培训SOP与采纳反馈"),
            new StageDef("operations", "运维阶段", "持续优化复盘式", "建立监控告警、故障响应与持续优化")
    );
    private static final List<ReqStepDef> REQUIREMENT_STEPS = List.of(
            new ReqStepDef("goal", "业务目标与价值", "请描述这个系统要解决的核心业务问题，以及成功标准（例如效率提升、成本下降、合规达标）。"),
            new ReqStepDef("users", "用户角色与场景", "请列出主要用户角色（谁在用）和关键使用场景（在什么情况下使用）。"),
            new ReqStepDef("process", "业务流程", "请按先后顺序描述核心业务流程（输入 -> 处理 -> 输出）。"),
            new ReqStepDef("rules", "业务规则与合规", "请补充必须遵守的业务规则、审批规则或监管合规要求。"),
            new ReqStepDef("constraints", "非功能约束", "请说明性能、可用性、安全、部署环境、预算和时间约束。"),
            new ReqStepDef("acceptance", "验收标准", "请给出可验收的标准（功能范围、质量指标、上线时间）。")
    );
    private static final Map<String, List<ReqStepDef>> STAGE_STEP_DEFS = Map.of(
            "requirements", REQUIREMENT_STEPS,
            "design", List.of(
                    new ReqStepDef("architecture", "架构方案", "请确认系统架构风格（单体/微服务/事件驱动）与原因。"),
                    new ReqStepDef("domain_model", "领域模型", "请确认核心实体、关系与关键接口。"),
                    new ReqStepDef("nfr_design", "质量属性设计", "请说明性能、安全、可用性在设计上的实现方案。")
            ),
            "planning", List.of(
                    new ReqStepDef("milestone", "里程碑与迭代", "请给出里程碑、每期目标和交付范围。"),
                    new ReqStepDef("resource", "资源与依赖", "请确认人力安排、外部依赖和关键风险。"),
                    new ReqStepDef("gate", "阶段闸门", "请定义每个里程碑的验收标准与决策点。")
            ),
            "development", List.of(
                    new ReqStepDef("implementation_scope", "实现范围", "请确认当前迭代需要开发的模块与接口。"),
                    new ReqStepDef("engineering_rules", "工程规范", "请确认编码规范、分支策略和评审规则。"),
                    new ReqStepDef("dev_risk", "开发风险", "请说明技术难点和回退方案。")
            ),
            "testing", List.of(
                    new ReqStepDef("test_strategy", "测试策略", "请确认单测/集成/端到端测试范围。"),
                    new ReqStepDef("test_case", "关键用例", "请列出高风险关键路径测试用例。"),
                    new ReqStepDef("exit_criteria", "退出标准", "请定义测试通过的质量门槛。")
            ),
            "deployment", List.of(
                    new ReqStepDef("release_plan", "发布策略", "请确认蓝绿/灰度/金丝雀等发布方式。"),
                    new ReqStepDef("rollback", "回滚与应急", "请给出回滚触发条件和应急流程。"),
                    new ReqStepDef("release_gate", "发布闸门", "请确认发布前检查项与审批责任人。")
            ),
            "implementation", List.of(
                    new ReqStepDef("pilot", "试点范围", "请确认首批上线业务范围和试点团队。"),
                    new ReqStepDef("training", "培训与SOP", "请说明培训计划和操作规范落地方式。"),
                    new ReqStepDef("adoption", "采纳与反馈", "请定义采纳指标和反馈收集机制。")
            ),
            "operations", List.of(
                    new ReqStepDef("monitoring", "监控与告警", "请确认核心监控指标、告警阈值和响应机制。"),
                    new ReqStepDef("incident", "故障响应", "请明确故障分级、值班和复盘流程。"),
                    new ReqStepDef("optimization", "持续优化", "请说明性能、成本和容量优化计划。")
            )
    );

    public static void main(String[] args) throws Exception {
        loadSessionsFromDisk();
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", MappingServer::handleUi);
        server.createContext("/app.js", MappingServer::handleUi);
        server.createContext("/styles.css", MappingServer::handleUi);
        server.createContext("/health", MappingServer::handleHealth);
        server.createContext("/api/v1/stages", MappingServer::handleStages);
        server.createContext("/api/v1/sessions/start", MappingServer::handleStartSession);
        server.createContext("/api/v1/sessions", MappingServer::handleSessions);
        server.createContext("/api/v1/mapping/recommend", MappingServer::handleRecommend);
        server.setExecutor(null);
        server.start();
        System.out.println("MappingServer started on port " + port);
    }

    private static void handleUi(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        String reqPath = exchange.getRequestURI().getPath();
        if (reqPath == null || reqPath.isBlank() || "/".equals(reqPath) || "/index.html".equals(reqPath)) {
            writeStatic(exchange, "standalone/web/index.html", "text/html; charset=utf-8");
            return;
        }
        if ("/app.js".equals(reqPath)) {
            writeStatic(exchange, "standalone/web/app.js", "application/javascript; charset=utf-8");
            return;
        }
        if ("/styles.css".equals(reqPath)) {
            writeStatic(exchange, "standalone/web/styles.css", "text/css; charset=utf-8");
            return;
        }
        writeJson(exchange, 404, "{\"error\":\"not_found\"}");
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        writeJson(exchange, 200, "{\"status\":\"UP\"}");
    }

    private static void handleStages(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        writeJson(exchange, 200, stagesJson(null));
    }

    private static void handleStartSession(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        String body = readBody(exchange.getRequestBody());
        String businessRequest = extractString(body, "businessRequest");
        if (businessRequest == null || businessRequest.isBlank()) {
            writeJson(exchange, 400, "{\"error\":\"businessRequest is required\"}");
            return;
        }

        SessionState session = SessionState.create(businessRequest);
        Map<String, String> ctx = session.context;
        putIfPresent(ctx, "domain", extractString(body, "domain"));
        putIfPresent(ctx, "complianceLevel", extractString(body, "complianceLevel"));
        putIfPresent(ctx, "timelineLevel", extractString(body, "timelineLevel"));
        putIfPresent(ctx, "teamMaturity", extractString(body, "teamMaturity"));
        putIfPresent(ctx, "budgetLevel", extractString(body, "budgetLevel"));
        putIfPresent(ctx, "projectType", extractString(body, "projectType"));
        SESSIONS.put(session.id, session);
        persistSessionsToDisk();

        String firstQuestion = generateAssistantQuestion(session, STAGE_DEFS.get(0).code, null);
        session.messages.add(new ChatMessage("assistant", firstQuestion, Instant.now().toString()));
        persistSessionsToDisk();
        writeJson(exchange, 200, sessionJson(session));
    }

    private static void handleSessions(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path == null) {
            writeJson(exchange, 404, "{\"error\":\"not_found\"}");
            return;
        }
        String prefix = "/api/v1/sessions/";
        if (!path.startsWith(prefix)) {
            writeJson(exchange, 404, "{\"error\":\"not_found\"}");
            return;
        }
        String rest = path.substring(prefix.length());
        if (rest.isBlank()) {
            writeJson(exchange, 404, "{\"error\":\"not_found\"}");
            return;
        }
        String[] parts = rest.split("/");
        String sessionId = parts[0];
        SessionState session = SESSIONS.get(sessionId);
        if (session == null) {
            writeJson(exchange, 404, "{\"error\":\"session_not_found\"}");
            return;
        }

        if (parts.length == 1 && "GET".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 200, sessionJson(session));
            return;
        }
        if (parts.length == 2 && "message".equals(parts[1]) && "POST".equals(exchange.getRequestMethod())) {
            String body = readBody(exchange.getRequestBody());
            String userMsg = extractString(body, "message");
            if (userMsg == null || userMsg.isBlank()) {
                writeJson(exchange, 400, "{\"error\":\"message is required\"}");
                return;
            }
            session.messages.add(new ChatMessage("user", userMsg, Instant.now().toString()));
            progressCurrentStage(session, userMsg);
            persistSessionsToDisk();
            writeJson(exchange, 200, sessionJson(session));
            return;
        }
        if (parts.length == 4 && "stage".equals(parts[1]) && "refresh".equals(parts[3]) && "POST".equals(exchange.getRequestMethod())) {
            String stageCode = parts[2];
            refreshStageByModel(session, stageCode);
            persistSessionsToDisk();
            writeJson(exchange, 200, sessionJson(session));
            return;
        }
        writeJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
    }

    private static void handleRecommend(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }

        String body = readBody(exchange.getRequestBody());
        String stageCode = extractString(body, "stageCode");
        if (stageCode == null || stageCode.isBlank()) {
            writeJson(exchange, 400, "{\"error\":\"stageCode is required\"}");
            return;
        }
        Map<String, String> ctx = new HashMap<>();
        putIfPresent(ctx, "domain", extractString(body, "domain"));
        putIfPresent(ctx, "complianceLevel", extractString(body, "complianceLevel"));
        putIfPresent(ctx, "timelineLevel", extractString(body, "timelineLevel"));
        putIfPresent(ctx, "teamMaturity", extractString(body, "teamMaturity"));
        putIfPresent(ctx, "budgetLevel", extractString(body, "budgetLevel"));
        putIfPresent(ctx, "projectType", extractString(body, "projectType"));

        Recommendation recommendation = recommendByStage(stageCode, ctx);
        String json = toResponseJson(
                stageCode,
                recommendation.paradigms,
                recommendation.metas,
                recommendation.methodologies,
                recommendation.methods,
                recommendation.trace
        );
        writeJson(exchange, 200, json);
    }

    private static void progressCurrentStage(SessionState session, String userMsg) {
        StageDef currentStage = STAGE_DEFS.get(session.currentStageIdx);
        StageWorkflowState workflow = session.stageWorkflows.get(currentStage.code);
        if (workflow == null) {
            session.messages.add(new ChatMessage("assistant", "当前阶段流程未配置。", Instant.now().toString()));
            return;
        }
        int idx = workflow.currentStepIdx;
        if (idx >= workflow.steps.size()) {
            completeStageAndMoveToNext(session, currentStage.code);
            return;
        }
        ReqStepState step = workflow.steps.get(idx);
        step.status = "DONE";
        step.answer = userMsg;
        workflow.currentStepIdx++;

        if (workflow.currentStepIdx < workflow.steps.size()) {
            workflow.steps.get(workflow.currentStepIdx).status = "CURRENT";
            String next = generateAssistantQuestion(session, currentStage.code, userMsg);
            session.messages.add(new ChatMessage("assistant", next, Instant.now().toString()));
            return;
        }
        completeStageAndMoveToNext(session, currentStage.code);
    }

    private static void completeStageAndMoveToNext(SessionState session, String stageCode) {
        String summary = generateStageSummary(session, stageCode);
        session.stageSummaries.put(stageCode, summary);
        session.stageCompletedAt.put(stageCode, Instant.now().toString());
        if (session.currentStageIdx >= STAGE_DEFS.size() - 1) {
            session.messages.add(new ChatMessage("assistant", summary + "\n\n全部阶段已完成，可进入复盘与持续优化。", Instant.now().toString()));
            return;
        }
        session.currentStageIdx++;
        StageDef nextStage = STAGE_DEFS.get(session.currentStageIdx);
        String nextQuestion = generateAssistantQuestion(session, nextStage.code, null);
        String intro = stageCode + " 已完成，进入 " + nextStage.name + "（互动方式：" + nextStage.interactionStyle + "）。";
        session.messages.add(new ChatMessage("assistant", summary + "\n\n" + intro + "\n\n" + nextQuestion, Instant.now().toString()));
    }

    private static String generateAssistantQuestion(SessionState session, String stageCode, String latestUserMsg) {
        StageWorkflowState workflow = session.stageWorkflows.get(stageCode);
        if (workflow == null || workflow.steps.isEmpty()) {
            return "当前阶段暂无步骤配置，请补充阶段模板。";
        }
        int stepIdx = Math.min(workflow.currentStepIdx, workflow.steps.size() - 1);
        ReqStepState step = workflow.steps.get(stepIdx);
        StageDef stageDef = findStageDef(stageCode);
        Recommendation recommendation = recommendByStage(stageCode, session.context);
        String methodologyHint = methodologyHintText(recommendation);

        String system = "你是资深产品经理，请用中文与业务人员互动，帮助澄清软件需求。"
                + "当前必须聚焦给定阶段，不要跳阶段。"
                + "请先根据给定方法论给出简短反馈，再提出1-3个高质量追问，语气专业简洁。";
        StringBuilder user = new StringBuilder();
        user.append("业务诉求：").append(session.businessRequest).append("\n");
        user.append("当前阶段：").append(stageDef.name).append("\n");
        user.append("互动方式：").append(stageDef.interactionStyle).append("\n");
        user.append("当前步骤：").append(step.name).append("\n");
        user.append("步骤目标提问：").append(step.question).append("\n");
        user.append("方法论引导：").append(methodologyHint).append("\n");
        if (latestUserMsg != null) {
            user.append("用户刚回复：").append(latestUserMsg).append("\n");
        }
        user.append("输出格式：\n")
                .append("1) 方法论反馈：一句话说明当前信息完整度与缺口\n")
                .append("2) 进一步问题：列出1-3个问题\n");
        String modelResponse = callOpenRouter(system, user.toString());
        if (modelResponse != null && !modelResponse.isBlank()) {
            return "【" + stageDef.name + " 步骤 " + (stepIdx + 1) + "/" + workflow.steps.size() + " - " + step.name + "】\n"
                    + "方法论：" + methodologyHint + "\n"
                    + modelResponse;
        }
        return "【" + stageDef.name + " 步骤 " + (stepIdx + 1) + "/" + workflow.steps.size() + " - " + step.name + "】\n"
                + "方法论：" + methodologyHint + "\n"
                + "方法论反馈：当前信息仍不完整，需要补齐本步骤关键信息。\n"
                + "进一步问题：" + step.question + "\n请尽量具体描述，包含可量化目标。";
    }

    private static String generateStageSummary(SessionState session, String stageCode) {
        StageDef stageDef = findStageDef(stageCode);
        StageWorkflowState workflow = session.stageWorkflows.get(stageCode);
        Recommendation recommendation = recommendByStage(stageCode, session.context);
        String methodologyHint = methodologyHintText(recommendation);
        StringBuilder brief = new StringBuilder();
        brief.append("当前阶段：").append(stageDef.name).append("，互动方式：").append(stageDef.interactionStyle).append("\n");
        brief.append("当前阶段采用方法论：").append(methodologyHint).append("\n");
        brief.append("请基于以下阶段信息生成结构化总结：\n");
        if (workflow != null) for (ReqStepState step : workflow.steps) {
            brief.append("- ").append(step.name).append(": ").append(step.answer == null ? "未提供" : step.answer).append("\n");
        }
        String modelResponse = callOpenRouter(
                "你是资深需求分析师，输出简洁中文总结，条目化。",
                brief.toString()
        );
        if (modelResponse != null && !modelResponse.isBlank()) {
            return stageDef.name + "总结：\n" + modelResponse;
        }
        StringBuilder fallback = new StringBuilder(stageDef.name).append("总结：\n");
        if (workflow != null) for (ReqStepState step : workflow.steps) {
            fallback.append("- ").append(step.name).append("：").append(step.answer == null ? "未填写" : step.answer).append("\n");
        }
        return fallback.toString().trim();
    }

    private static void refreshStageByModel(SessionState session, String stageCode) {
        StageWorkflowState workflow = session.stageWorkflows.get(stageCode);
        StageDef stageDef = findStageDef(stageCode);
        if (workflow == null || workflow.steps.isEmpty()) {
            return;
        }
        int idx = Math.min(workflow.currentStepIdx, workflow.steps.size() - 1);
        ReqStepState step = workflow.steps.get(idx);
        Recommendation recommendation = recommendByStage(stageCode, session.context);
        String methodologyHint = methodologyHintText(recommendation);
        String system = "你是企业软件交付顾问。请基于给定阶段与方法论，生成一句更具体的步骤问题，用于引导业务方补充信息。只输出问题本身。";
        String user = "阶段：" + stageDef.name + "\n互动方式：" + stageDef.interactionStyle + "\n阶段目标：" + stageDef.description
                + "\n当前步骤：" + step.name + "\n现有问题：" + step.question + "\n方法论：" + methodologyHint;
        String refreshed = callOpenRouter(system, user);
        if (refreshed != null && !refreshed.isBlank()) {
            step.question = refreshed.trim();
        }
        session.messages.add(new ChatMessage(
                "assistant",
                "已按「" + stageDef.name + " / " + stageDef.interactionStyle + "」刷新步骤关注点：\n" + step.question,
                Instant.now().toString()
        ));
    }

    private static StageDef findStageDef(String stageCode) {
        for (StageDef def : STAGE_DEFS) {
            if (def.code.equals(stageCode)) {
                return def;
            }
        }
        return new StageDef(stageCode, stageCode, "通用互动", "通用阶段说明");
    }

    private static Recommendation recommendByStage(String stageCode, Map<String, String> ctx) {
        Seed seed = Seed.create();
        Trace trace = new Trace();
        int stageIdx = 1;
        for (int i = 0; i < STAGE_DEFS.size(); i++) {
            if (STAGE_DEFS.get(i).code.equals(stageCode)) {
                stageIdx = i + 1;
                break;
            }
        }
        List<MapRow> stageRows = new ArrayList<>();
        for (MapRow row : seed.stageToParadigm) {
            if (row.fromId == stageIdx) {
                stageRows.add(row);
            }
        }

        List<ScoredRef> paradigms = filterAndScore(stageRows, ctx, trace, 3, null, seed.paradigmById);
        Set<Long> paradigmIds = idsOf(paradigms);

        List<MapRow> pmRows = new ArrayList<>();
        for (MapRow row : seed.paradigmToMeta) {
            if (paradigmIds.contains(row.fromId)) {
                pmRows.add(row);
            }
        }
        List<ScoredRef> metas = filterAndScore(pmRows, ctx, trace, 5, paradigms, seed.metaById);
        Set<Long> metaIds = idsOf(metas);

        List<MapRow> mmRows = new ArrayList<>();
        for (MapRow row : seed.metaToMethodology) {
            if (metaIds.contains(row.fromId)) {
                mmRows.add(row);
            }
        }
        List<ScoredRef> methodologies = filterAndScore(mmRows, ctx, trace, 5, metas, seed.methodologyById);
        Set<Long> methodologyIds = idsOf(methodologies);

        List<MapRow> methodRows = new ArrayList<>();
        for (MapRow row : seed.methodologyToMethod) {
            if (methodologyIds.contains(row.fromId)) {
                methodRows.add(row);
            }
        }
        List<ScoredRef> methods = filterAndScore(methodRows, ctx, trace, 20, methodologies, seed.methodById);
        return new Recommendation(paradigms, metas, methodologies, methods, trace);
    }

    private static String methodologyHintText(Recommendation recommendation) {
        String methodology = recommendation.methodologies.isEmpty() ? "通用需求访谈方法论" : recommendation.methodologies.get(0).name;
        String method = recommendation.methods.isEmpty() ? "结构化提问" : recommendation.methods.get(0).name;
        String paradigm = recommendation.paradigms.isEmpty() ? "需求澄清范式" : recommendation.paradigms.get(0).name;
        return paradigm + " / " + methodology + " / " + method;
    }

    private static String callOpenRouter(String systemPrompt, String userPrompt) {
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        String model = System.getenv().getOrDefault("OPENROUTER_MODEL", "openai/gpt-4o-mini");
        int maxTokens = Integer.parseInt(System.getenv().getOrDefault("OPENROUTER_MAX_TOKENS", "220"));
        long timeoutMs = Long.parseLong(System.getenv().getOrDefault("OPENROUTER_TIMEOUT_MS", "3800"));
        String body = "{"
                + "\"model\":\"" + esc(model) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + esc(systemPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + esc(userPrompt) + "\"}"
                + "],"
                + "\"temperature\":0.2,"
                + "\"max_tokens\":" + maxTokens
                + "}";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "http://localhost:8080")
                    .header("X-Title", "paradigm-sys")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                return null;
            }
            return extractFirstContent(response.body());
        } catch (Exception ex) {
            return null;
        }
    }

    private static String extractFirstContent(String json) {
        int idx = json.indexOf("\"content\"");
        if (idx < 0) {
            return null;
        }
        int colon = json.indexOf(":", idx);
        if (colon < 0) {
            return null;
        }
        int start = json.indexOf("\"", colon + 1);
        if (start < 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (esc) {
                if (c == 'n') {
                    sb.append('\n');
                } else {
                    sb.append(c);
                }
                esc = false;
                continue;
            }
            if (c == '\\') {
                esc = true;
                continue;
            }
            if (c == '"') {
                return sb.toString();
            }
            sb.append(c);
        }
        return null;
    }

    private static List<ScoredRef> filterAndScore(
            List<MapRow> rows,
            Map<String, String> ctx,
            Trace trace,
            int limit,
            List<ScoredRef> parent,
            Map<Long, Ref> refById
    ) {
        Map<Long, ScoredRef> bestById = new HashMap<>();
        for (MapRow row : rows) {
            String ruleId = row.mapType + ":" + row.id;
            if (!eval(row.conditionExpr, ctx)) {
                trace.droppedByCondition.add(ruleId);
                continue;
            }
            trace.appliedRules.add(ruleId);
            double score = 1000.0 - row.priority;
            if ("L3".equals(ctx.get("complianceLevel"))) {
                score += 20.0;
            }
            if ("tight".equals(ctx.get("timelineLevel"))) {
                score -= 10.0;
            }
            if (parent != null) {
                score += parentScore(parent, row.fromId) * 0.2;
            }
            Ref ref = refById.get(row.toId);
            if (ref == null) {
                continue;
            }
            ScoredRef candidate = new ScoredRef(ref.id, ref.code, ref.name, score);
            ScoredRef existing = bestById.get(ref.id);
            if (existing == null || candidate.score > existing.score) {
                bestById.put(ref.id, candidate);
            }
        }
        List<ScoredRef> result = new ArrayList<>(bestById.values());
        result.sort((a, b) -> Double.compare(b.score, a.score));
        if (result.size() > limit) {
            return new ArrayList<>(result.subList(0, limit));
        }
        return result;
    }

    private static double parentScore(List<ScoredRef> parent, long id) {
        for (ScoredRef ref : parent) {
            if (ref.id == id) {
                return ref.score;
            }
        }
        return 0.0;
    }

    private static Set<Long> idsOf(List<ScoredRef> refs) {
        Set<Long> ids = new HashSet<>();
        for (ScoredRef ref : refs) {
            ids.add(ref.id);
        }
        return ids;
    }

    private static void putIfPresent(Map<String, String> map, String key, String val) {
        if (val != null && !val.isBlank()) {
            map.put(key, val);
        }
    }

    private static boolean eval(String expr, Map<String, String> ctx) {
        if (expr == null || expr.isBlank()) {
            return true;
        }
        String[] orParts = expr.split("\\|\\|");
        for (String orPart : orParts) {
            boolean andOk = true;
            String[] andParts = orPart.split("&&");
            for (String atom : andParts) {
                if (!evalAtom(atom.trim(), ctx)) {
                    andOk = false;
                    break;
                }
            }
            if (andOk) {
                return true;
            }
        }
        return false;
    }

    private static boolean evalAtom(String atom, Map<String, String> ctx) {
        if (atom.contains(" in ")) {
            Matcher m = Pattern.compile("^([a-zA-Z0-9_]+)\\s+in\\s+\\[(.*)]$").matcher(atom);
            if (!m.find()) {
                return false;
            }
            String key = m.group(1).trim();
            String rawList = m.group(2).trim();
            String val = ctx.get(key);
            if (val == null) {
                return false;
            }
            for (String part : rawList.split(",")) {
                String item = stripQuotes(part.trim());
                if (val.equals(item)) {
                    return true;
                }
            }
            return false;
        }
        if (atom.contains("==")) {
            String[] parts = atom.split("==");
            if (parts.length != 2) {
                return false;
            }
            String key = parts[0].trim();
            String expected = stripQuotes(parts[1].trim());
            return expected.equals(ctx.get(key));
        }
        return false;
    }

    private static String stripQuotes(String s) {
        String out = s;
        if (out.startsWith("\"") && out.endsWith("\"") && out.length() >= 2) {
            out = out.substring(1, out.length() - 1);
        }
        return out;
    }

    private static String extractString(String body, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(body);
        return m.find() ? m.group(1) : null;
    }

    private static String readBody(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void writeStatic(HttpExchange exchange, String relativePath, String contentType) throws IOException {
        Path p = Path.of(relativePath);
        if (!Files.exists(p)) {
            writeJson(exchange, 404, "{\"error\":\"asset_not_found\"}");
            return;
        }
        byte[] data = Files.readAllBytes(p);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private static void writeJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private static String toResponseJson(
            String stageCode,
            List<ScoredRef> paradigms,
            List<ScoredRef> metas,
            List<ScoredRef> methodologies,
            List<ScoredRef> methods,
            Trace trace
    ) {
        return "{"
                + "\"stage\":\"" + esc(stageCode) + "\","
                + "\"recommendedParadigms\":" + refsJson(paradigms) + ","
                + "\"recommendedMetaMethodologies\":" + refsJson(metas) + ","
                + "\"recommendedMethodologies\":" + refsJson(methodologies) + ","
                + "\"recommendedMethods\":" + refsJson(methods) + ","
                + "\"trace\":{"
                + "\"appliedRules\":" + stringsJson(trace.appliedRules) + ","
                + "\"droppedByCondition\":" + stringsJson(trace.droppedByCondition)
                + "}"
                + "}";
    }

    private static String refsJson(List<ScoredRef> refs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < refs.size(); i++) {
            ScoredRef r = refs.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{")
                    .append("\"id\":").append(r.id).append(",")
                    .append("\"code\":\"").append(esc(r.code)).append("\",")
                    .append("\"name\":\"").append(esc(r.name)).append("\"")
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String stringsJson(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(esc(list.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String sessionJson(SessionState session) {
        StageDef currentStage = STAGE_DEFS.get(session.currentStageIdx);
        StageWorkflowState currentWorkflow = session.stageWorkflows.get(currentStage.code);
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append("\"sessionId\":\"").append(esc(session.id)).append("\",")
                .append("\"businessRequest\":\"").append(esc(session.businessRequest)).append("\",")
                .append("\"currentStage\":\"").append(esc(currentStage.code)).append("\",")
                .append("\"currentStageName\":\"").append(esc(currentStage.name)).append("\",")
                .append("\"currentStageInteraction\":\"").append(esc(currentStage.interactionStyle)).append("\",")
                .append("\"stages\":").append(stagesJson(session)).append(",")
                .append("\"stageDetails\":").append(allStageDetailsJson(session)).append(",")
                .append("\"currentStageWorkflow\":").append(workflowJson(currentWorkflow)).append(",")
                .append("\"requirements\":").append(workflowJson(session.stageWorkflows.get("requirements"))).append(",")
                .append("\"messages\":").append(messagesJson(session.messages))
                .append("}");
        return sb.toString();
    }

    private static String stagesJson(SessionState session) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < STAGE_DEFS.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            StageDef s = STAGE_DEFS.get(i);
            String status = "PENDING";
            if (session != null) {
                if (i < session.currentStageIdx) {
                    status = "DONE";
                } else if (i == session.currentStageIdx) {
                    status = "CURRENT";
                }
            } else if (i == 0) {
                status = "CURRENT";
            }
            sb.append("{")
                    .append("\"code\":\"").append(esc(s.code)).append("\",")
                    .append("\"name\":\"").append(esc(s.name)).append("\",")
                    .append("\"interactionStyle\":\"").append(esc(s.interactionStyle)).append("\",")
                    .append("\"description\":\"").append(esc(s.description)).append("\",")
                    .append("\"status\":\"").append(status).append("\"")
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String workflowJson(StageWorkflowState workflow) {
        if (workflow == null) {
            return "{\"currentStepIdx\":0,\"totalSteps\":0,\"steps\":[]}";
        }
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"currentStepIdx\":").append(workflow.currentStepIdx).append(",");
        sb.append("\"totalSteps\":").append(workflow.steps.size()).append(",");
        sb.append("\"steps\":[");
        for (int i = 0; i < workflow.steps.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            ReqStepState step = workflow.steps.get(i);
            sb.append("{")
                    .append("\"key\":\"").append(esc(step.key)).append("\",")
                    .append("\"name\":\"").append(esc(step.name)).append("\",")
                    .append("\"question\":\"").append(esc(step.question)).append("\",")
                    .append("\"status\":\"").append(esc(step.status)).append("\",")
                    .append("\"answer\":\"").append(step.answer == null ? "" : esc(step.answer)).append("\"")
                    .append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String allStageDetailsJson(SessionState session) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < STAGE_DEFS.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            StageDef def = STAGE_DEFS.get(i);
            StageWorkflowState workflow = session.stageWorkflows.get(def.code);
            String summary = session.stageSummaries.getOrDefault(def.code, "");
            String completedAt = session.stageCompletedAt.getOrDefault(def.code, "");
            sb.append("\"").append(esc(def.code)).append("\":{")
                    .append("\"workflow\":").append(workflowJson(workflow)).append(",")
                    .append("\"summary\":\"").append(esc(summary)).append("\",")
                    .append("\"completedAt\":\"").append(esc(completedAt)).append("\"")
                    .append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    private static void persistSessionsToDisk() {
        try {
            Path p = Path.of(SESSION_STORE_FILE);
            Path parent = p.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(p.toFile()))) {
                oos.writeObject(new HashMap<>(SESSIONS));
            }
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadSessionsFromDisk() {
        try {
            Path p = Path.of(SESSION_STORE_FILE);
            if (!Files.exists(p)) {
                return;
            }
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(p.toFile()))) {
                Object obj = ois.readObject();
                if (obj instanceof Map<?, ?> raw) {
                    for (Map.Entry<?, ?> e : raw.entrySet()) {
                        if (e.getKey() instanceof String k && e.getValue() instanceof SessionState v) {
                            SESSIONS.put(k, v);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static String messagesJson(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            ChatMessage m = messages.get(i);
            sb.append("{")
                    .append("\"role\":\"").append(esc(m.role)).append("\",")
                    .append("\"content\":\"").append(esc(m.content)).append("\",")
                    .append("\"timestamp\":\"").append(esc(m.timestamp)).append("\"")
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static class Ref {
        long id;
        String code;
        String name;

        Ref(long id, String code, String name) {
            this.id = id;
            this.code = code;
            this.name = name;
        }
    }

    private static class ScoredRef extends Ref {
        double score;

        ScoredRef(long id, String code, String name, double score) {
            super(id, code, name);
            this.score = score;
        }
    }

    private static class MapRow {
        long id;
        String mapType;
        long fromId;
        long toId;
        int priority;
        String conditionExpr;

        MapRow(long id, String mapType, long fromId, long toId, int priority, String conditionExpr) {
            this.id = id;
            this.mapType = mapType;
            this.fromId = fromId;
            this.toId = toId;
            this.priority = priority;
            this.conditionExpr = conditionExpr;
        }
    }

    private static class Trace {
        List<String> appliedRules = new ArrayList<>();
        List<String> droppedByCondition = new ArrayList<>();
    }

    private static class Seed {
        Map<Long, Ref> paradigmById = new HashMap<>();
        Map<Long, Ref> metaById = new HashMap<>();
        Map<Long, Ref> methodologyById = new HashMap<>();
        Map<Long, Ref> methodById = new HashMap<>();
        List<MapRow> stageToParadigm = new ArrayList<>();
        List<MapRow> paradigmToMeta = new ArrayList<>();
        List<MapRow> metaToMethodology = new ArrayList<>();
        List<MapRow> methodologyToMethod = new ArrayList<>();

        static Seed create() {
            Seed s = new Seed();
            s.paradigmById.put(11L, new Ref(11L, "VALUE_DRIVEN", "价值驱动范式"));
            s.paradigmById.put(12L, new Ref(12L, "DOMAIN_DRIVEN", "领域驱动范式"));
            s.paradigmById.put(13L, new Ref(13L, "RULE_CONSTRAINT", "规则约束范式"));
            s.paradigmById.put(14L, new Ref(14L, "ARCH_DRIVEN", "架构驱动范式"));
            s.paradigmById.put(15L, new Ref(15L, "AGILE_PLANNING", "敏捷规划范式"));
            s.paradigmById.put(16L, new Ref(16L, "ENGINEERING_EXCELLENCE", "工程化范式"));
            s.paradigmById.put(17L, new Ref(17L, "QUALITY_ASSURANCE", "质量保障范式"));
            s.paradigmById.put(18L, new Ref(18L, "RELEASE_DELIVERY", "持续交付范式"));
            s.paradigmById.put(19L, new Ref(19L, "CHANGE_ADOPTION", "变更采纳范式"));
            s.paradigmById.put(20L, new Ref(20L, "SRE_OPERATIONS", "SRE运维范式"));

            s.metaById.put(21L, new Ref(21L, "VALUE_DISCOVERY_META", "价值发现元方法论"));
            s.metaById.put(22L, new Ref(22L, "SEMANTIC_MODEL_META", "语义建模元方法论"));
            s.metaById.put(23L, new Ref(23L, "COMPLIANCE_META", "合规建模元方法论"));
            s.metaById.put(24L, new Ref(24L, "ARCH_META", "架构权衡元方法论"));
            s.metaById.put(25L, new Ref(25L, "PLANNING_META", "迭代计划元方法论"));
            s.metaById.put(26L, new Ref(26L, "ENGINEERING_META", "工程治理元方法论"));
            s.metaById.put(27L, new Ref(27L, "TEST_META", "测试验证元方法论"));
            s.metaById.put(28L, new Ref(28L, "DEPLOY_META", "发布治理元方法论"));
            s.metaById.put(29L, new Ref(29L, "IMPLEMENT_META", "实施采纳元方法论"));
            s.metaById.put(30L, new Ref(30L, "OPS_META", "运维优化元方法论"));

            s.methodologyById.put(31L, new Ref(31L, "WSJF_METHODLOGY", "WSJF优先级方法论"));
            s.methodologyById.put(32L, new Ref(32L, "EVENT_STORMING_METHODLOGY", "事件风暴方法论"));
            s.methodologyById.put(33L, new Ref(33L, "RULE_TABLE_METHODLOGY", "规则表方法论"));
            s.methodologyById.put(34L, new Ref(34L, "ARCH_REVIEW_METHODOLOGY", "架构评审方法论"));
            s.methodologyById.put(35L, new Ref(35L, "ROADMAP_METHODOLOGY", "路线图规划方法论"));
            s.methodologyById.put(36L, new Ref(36L, "DEV_GOV_METHODOLOGY", "开发治理方法论"));
            s.methodologyById.put(37L, new Ref(37L, "TEST_STRATEGY_METHODOLOGY", "测试策略方法论"));
            s.methodologyById.put(38L, new Ref(38L, "RELEASE_METHODOLOGY", "发布策略方法论"));
            s.methodologyById.put(39L, new Ref(39L, "ADOPTION_METHODOLOGY", "实施推广方法论"));
            s.methodologyById.put(40L, new Ref(40L, "SRE_METHODOLOGY", "SRE方法论"));

            s.methodById.put(41L, new Ref(41L, "INTERVIEW_METHOD", "结构化访谈"));
            s.methodById.put(42L, new Ref(42L, "VALUE_SCORING_METHOD", "价值评分"));
            s.methodById.put(43L, new Ref(43L, "WORKSHOP_METHOD", "需求工作坊"));
            s.methodById.put(44L, new Ref(44L, "EVENT_MODELING_METHOD", "事件建模"));
            s.methodById.put(45L, new Ref(45L, "RULE_TABLE_METHOD", "决策表梳理"));
            s.methodById.put(46L, new Ref(46L, "COMPLIANCE_CHECK_METHOD", "合规约束检查"));
            s.methodById.put(47L, new Ref(47L, "C4_MODELING_METHOD", "C4建模"));
            s.methodById.put(48L, new Ref(48L, "ARCH_RISK_METHOD", "架构风险评估"));
            s.methodById.put(49L, new Ref(49L, "MILESTONE_METHOD", "里程碑拆解"));
            s.methodById.put(50L, new Ref(50L, "CAPACITY_PLAN_METHOD", "容量规划"));
            s.methodById.put(51L, new Ref(51L, "CODE_REVIEW_METHOD", "代码评审"));
            s.methodById.put(52L, new Ref(52L, "CI_POLICY_METHOD", "流水线门禁"));
            s.methodById.put(53L, new Ref(53L, "TEST_CASE_DESIGN_METHOD", "测试用例设计"));
            s.methodById.put(54L, new Ref(54L, "RISK_TEST_METHOD", "风险测试"));
            s.methodById.put(55L, new Ref(55L, "CANARY_METHOD", "金丝雀发布"));
            s.methodById.put(56L, new Ref(56L, "ROLLBACK_METHOD", "自动回滚"));
            s.methodById.put(57L, new Ref(57L, "PILOT_ROLLOUT_METHOD", "试点推广"));
            s.methodById.put(58L, new Ref(58L, "TRAINING_METHOD", "培训落地"));
            s.methodById.put(59L, new Ref(59L, "SLO_DESIGN_METHOD", "SLO设计"));
            s.methodById.put(60L, new Ref(60L, "POSTMORTEM_METHOD", "复盘改进"));

            s.stageToParadigm.add(new MapRow(1001L, "stage_paradigm_map", 1L, 11L, 100, "timelineLevel == \"tight\""));
            s.stageToParadigm.add(new MapRow(1002L, "stage_paradigm_map", 1L, 12L, 120, ""));
            s.stageToParadigm.add(new MapRow(1003L, "stage_paradigm_map", 1L, 13L, 80, "complianceLevel == \"L3\""));
            s.stageToParadigm.add(new MapRow(1004L, "stage_paradigm_map", 2L, 14L, 85, ""));
            s.stageToParadigm.add(new MapRow(1005L, "stage_paradigm_map", 2L, 12L, 95, ""));
            s.stageToParadigm.add(new MapRow(1006L, "stage_paradigm_map", 2L, 13L, 90, "complianceLevel == \"L3\""));
            s.stageToParadigm.add(new MapRow(1007L, "stage_paradigm_map", 3L, 15L, 80, ""));
            s.stageToParadigm.add(new MapRow(1008L, "stage_paradigm_map", 3L, 11L, 95, ""));
            s.stageToParadigm.add(new MapRow(1009L, "stage_paradigm_map", 3L, 13L, 90, "complianceLevel == \"L3\""));
            s.stageToParadigm.add(new MapRow(1010L, "stage_paradigm_map", 4L, 16L, 80, ""));
            s.stageToParadigm.add(new MapRow(1011L, "stage_paradigm_map", 4L, 12L, 95, ""));
            s.stageToParadigm.add(new MapRow(1012L, "stage_paradigm_map", 4L, 13L, 90, "complianceLevel == \"L3\""));
            s.stageToParadigm.add(new MapRow(1013L, "stage_paradigm_map", 5L, 17L, 80, ""));
            s.stageToParadigm.add(new MapRow(1014L, "stage_paradigm_map", 5L, 13L, 90, "complianceLevel == \"L3\""));
            s.stageToParadigm.add(new MapRow(1015L, "stage_paradigm_map", 5L, 11L, 100, ""));
            s.stageToParadigm.add(new MapRow(1016L, "stage_paradigm_map", 6L, 18L, 80, ""));
            s.stageToParadigm.add(new MapRow(1017L, "stage_paradigm_map", 6L, 13L, 88, "complianceLevel == \"L3\""));
            s.stageToParadigm.add(new MapRow(1018L, "stage_paradigm_map", 6L, 11L, 100, ""));
            s.stageToParadigm.add(new MapRow(1019L, "stage_paradigm_map", 7L, 19L, 80, ""));
            s.stageToParadigm.add(new MapRow(1020L, "stage_paradigm_map", 7L, 11L, 92, ""));
            s.stageToParadigm.add(new MapRow(1021L, "stage_paradigm_map", 7L, 13L, 90, "complianceLevel == \"L3\""));
            s.stageToParadigm.add(new MapRow(1022L, "stage_paradigm_map", 8L, 20L, 80, ""));
            s.stageToParadigm.add(new MapRow(1023L, "stage_paradigm_map", 8L, 13L, 88, "complianceLevel == \"L3\""));
            s.stageToParadigm.add(new MapRow(1024L, "stage_paradigm_map", 8L, 11L, 98, ""));

            s.paradigmToMeta.add(new MapRow(2001L, "paradigm_meta_map", 11L, 21L, 100, ""));
            s.paradigmToMeta.add(new MapRow(2002L, "paradigm_meta_map", 12L, 22L, 100, ""));
            s.paradigmToMeta.add(new MapRow(2003L, "paradigm_meta_map", 13L, 23L, 90, "complianceLevel == \"L3\""));
            s.paradigmToMeta.add(new MapRow(2004L, "paradigm_meta_map", 14L, 24L, 90, ""));
            s.paradigmToMeta.add(new MapRow(2005L, "paradigm_meta_map", 15L, 25L, 90, ""));
            s.paradigmToMeta.add(new MapRow(2006L, "paradigm_meta_map", 16L, 26L, 90, ""));
            s.paradigmToMeta.add(new MapRow(2007L, "paradigm_meta_map", 17L, 27L, 90, ""));
            s.paradigmToMeta.add(new MapRow(2008L, "paradigm_meta_map", 18L, 28L, 90, ""));
            s.paradigmToMeta.add(new MapRow(2009L, "paradigm_meta_map", 19L, 29L, 90, ""));
            s.paradigmToMeta.add(new MapRow(2010L, "paradigm_meta_map", 20L, 30L, 90, ""));

            s.metaToMethodology.add(new MapRow(3001L, "meta_methodology_map", 21L, 31L, 100, ""));
            s.metaToMethodology.add(new MapRow(3002L, "meta_methodology_map", 22L, 32L, 100, ""));
            s.metaToMethodology.add(new MapRow(3003L, "meta_methodology_map", 23L, 33L, 80, "domain == \"finance\""));
            s.metaToMethodology.add(new MapRow(3004L, "meta_methodology_map", 24L, 34L, 90, ""));
            s.metaToMethodology.add(new MapRow(3005L, "meta_methodology_map", 25L, 35L, 90, ""));
            s.metaToMethodology.add(new MapRow(3006L, "meta_methodology_map", 26L, 36L, 90, ""));
            s.metaToMethodology.add(new MapRow(3007L, "meta_methodology_map", 27L, 37L, 90, ""));
            s.metaToMethodology.add(new MapRow(3008L, "meta_methodology_map", 28L, 38L, 90, ""));
            s.metaToMethodology.add(new MapRow(3009L, "meta_methodology_map", 29L, 39L, 90, ""));
            s.metaToMethodology.add(new MapRow(3010L, "meta_methodology_map", 30L, 40L, 90, ""));

            s.methodologyToMethod.add(new MapRow(4001L, "methodology_method_map", 31L, 41L, 1, ""));
            s.methodologyToMethod.add(new MapRow(4002L, "methodology_method_map", 31L, 42L, 2, ""));
            s.methodologyToMethod.add(new MapRow(4003L, "methodology_method_map", 32L, 43L, 1, ""));
            s.methodologyToMethod.add(new MapRow(4004L, "methodology_method_map", 32L, 44L, 2, ""));
            s.methodologyToMethod.add(new MapRow(4005L, "methodology_method_map", 33L, 45L, 1, ""));
            s.methodologyToMethod.add(new MapRow(4006L, "methodology_method_map", 33L, 46L, 2, "complianceLevel == \"L3\""));
            s.methodologyToMethod.add(new MapRow(4007L, "methodology_method_map", 34L, 47L, 1, ""));
            s.methodologyToMethod.add(new MapRow(4008L, "methodology_method_map", 34L, 48L, 2, ""));
            s.methodologyToMethod.add(new MapRow(4009L, "methodology_method_map", 35L, 49L, 1, ""));
            s.methodologyToMethod.add(new MapRow(4010L, "methodology_method_map", 35L, 50L, 2, ""));
            s.methodologyToMethod.add(new MapRow(4011L, "methodology_method_map", 36L, 51L, 1, ""));
            s.methodologyToMethod.add(new MapRow(4012L, "methodology_method_map", 36L, 52L, 2, ""));
            s.methodologyToMethod.add(new MapRow(4013L, "methodology_method_map", 37L, 53L, 1, ""));
            s.methodologyToMethod.add(new MapRow(4014L, "methodology_method_map", 37L, 54L, 2, ""));
            s.methodologyToMethod.add(new MapRow(4015L, "methodology_method_map", 38L, 55L, 1, ""));
            s.methodologyToMethod.add(new MapRow(4016L, "methodology_method_map", 38L, 56L, 2, ""));
            s.methodologyToMethod.add(new MapRow(4017L, "methodology_method_map", 39L, 57L, 1, ""));
            s.methodologyToMethod.add(new MapRow(4018L, "methodology_method_map", 39L, 58L, 2, ""));
            s.methodologyToMethod.add(new MapRow(4019L, "methodology_method_map", 40L, 59L, 1, ""));
            s.methodologyToMethod.add(new MapRow(4020L, "methodology_method_map", 40L, 60L, 2, ""));
            return s;
        }
    }

    private static class SessionState implements Serializable {
        private static final long serialVersionUID = 1L;
        String id;
        String businessRequest;
        Map<String, String> context = new HashMap<>();
        Map<String, StageWorkflowState> stageWorkflows = new HashMap<>();
        Map<String, String> stageSummaries = new HashMap<>();
        Map<String, String> stageCompletedAt = new HashMap<>();
        List<ChatMessage> messages = new ArrayList<>();
        int currentStageIdx = 0;

        static SessionState create(String businessRequest) {
            SessionState s = new SessionState();
            s.id = "sess_" + UUID.randomUUID().toString().replace("-", "");
            s.businessRequest = businessRequest;
            for (StageDef stage : STAGE_DEFS) {
                List<ReqStepDef> defs = STAGE_STEP_DEFS.getOrDefault(stage.code, List.of());
                List<ReqStepState> steps = new ArrayList<>();
                for (int i = 0; i < defs.size(); i++) {
                    ReqStepDef def = defs.get(i);
                    String status = i == 0 ? "CURRENT" : "PENDING";
                    steps.add(new ReqStepState(def.key, def.name, def.question, status));
                }
                s.stageWorkflows.put(stage.code, new StageWorkflowState(0, steps));
            }
            return s;
        }
    }

    private static class StageWorkflowState implements Serializable {
        private static final long serialVersionUID = 1L;
        int currentStepIdx;
        List<ReqStepState> steps;

        StageWorkflowState(int currentStepIdx, List<ReqStepState> steps) {
            this.currentStepIdx = currentStepIdx;
            this.steps = steps;
        }
    }

    private static class ReqStepState implements Serializable {
        private static final long serialVersionUID = 1L;
        String key;
        String name;
        String question;
        String status;
        String answer;

        ReqStepState(String key, String name, String question, String status) {
            this.key = key;
            this.name = name;
            this.question = question;
            this.status = status;
        }
    }

    private record ReqStepDef(String key, String name, String question) implements Serializable {
    }

    private record StageDef(String code, String name, String interactionStyle, String description) {
    }

    private record ChatMessage(String role, String content, String timestamp) implements Serializable {
    }

    private record Recommendation(
            List<ScoredRef> paradigms,
            List<ScoredRef> metas,
            List<ScoredRef> methodologies,
            List<ScoredRef> methods,
            Trace trace
    ) {
    }
}
