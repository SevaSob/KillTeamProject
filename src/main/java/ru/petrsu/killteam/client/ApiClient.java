package ru.petrsu.killteam.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import ru.petrsu.killteam.dto.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ApiClient {

    private static final String BASE = "http://localhost:8080";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Faction
    public List<FactionDto> getFactions() throws Exception {
        return readList(get("/api/factions"), new TypeReference<>() {});
    }
    public FactionDto createFaction(FactionDto dto) throws Exception {
        return read(post("/api/factions", dto), FactionDto.class);
    }
    public FactionDto updateFaction(Long id, FactionDto dto) throws Exception {
        return read(put("/api/factions/" + id, dto), FactionDto.class);
    }
    public void deleteFaction(Long id) throws Exception {
        delete("/api/factions/" + id);
    }

    // KillTeam
    public List<KillTeamDto> getKillTeams() throws Exception {
        return readList(get("/api/kill-teams"), new TypeReference<>() {});
    }
    public KillTeamDto createKillTeam(KillTeamDto dto) throws Exception {
        return read(post("/api/kill-teams", dto), KillTeamDto.class);
    }
    public KillTeamDto updateKillTeam(Long id, KillTeamDto dto) throws Exception {
        return read(put("/api/kill-teams/" + id, dto), KillTeamDto.class);
    }
    public void deleteKillTeam(Long id) throws Exception {
        delete("/api/kill-teams/" + id);
    }

    // Operative
    public List<OperativeDto> getOperatives(Long killTeamId) throws Exception {
        return readList(get("/api/operatives?killTeamId=" + killTeamId), new TypeReference<>() {});
    }
    public OperativeDto getOperative(Long id) throws Exception {
        return read(get("/api/operatives/" + id), OperativeDto.class);
    }
    public OperativeDto createOperative(Long killTeamId, OperativeDto dto) throws Exception {
        return read(post("/api/operatives?killTeamId=" + killTeamId, dto), OperativeDto.class);
    }
    public OperativeDto updateOperative(Long id, OperativeDto dto) throws Exception {
        return read(put("/api/operatives/" + id, dto), OperativeDto.class);
    }
    public void deleteOperative(Long id) throws Exception {
        delete("/api/operatives/" + id);
    }

    // Weapon
    public WeaponDto createWeapon(Long operativeId, WeaponDto dto) throws Exception {
        return read(post("/api/weapons?operativeId=" + operativeId, dto), WeaponDto.class);
    }
    public WeaponDto updateWeapon(Long id, WeaponDto dto) throws Exception {
        return read(put("/api/weapons/" + id, dto), WeaponDto.class);
    }
    public void deleteWeapon(Long id) throws Exception {
        delete("/api/weapons/" + id);
    }

    // Match
    public List<MatchDto> getMatches() throws Exception {
        return readList(get("/api/matches"), new TypeReference<>() {});
    }
    public MatchDto getMatch(Long id) throws Exception {
        return read(get("/api/matches/" + id), MatchDto.class);
    }
    public MatchDto createMatch(Object req) throws Exception {
        return read(post("/api/matches", req), MatchDto.class);
    }
    public MatchDto advancePhase(Long matchId) throws Exception {
        return read(post("/api/matches/" + matchId + "/advance", null), MatchDto.class);
    }
    public List<GameEventDto> getEvents(Long matchId) throws Exception {
        return readList(get("/api/matches/" + matchId + "/events"), new TypeReference<>() {});
    }
    public GameEventDto addEvent(Long matchId, Object req) throws Exception {
        return read(post("/api/matches/" + matchId + "/events", req), GameEventDto.class);
    }
    public void deleteMatch(Long matchId) throws Exception {
        delete("/api/matches/" + matchId);
    }
    public MatchDto changeCp(Long matchId, Long playerId, int delta) throws Exception {
        return read(post("/api/matches/" + matchId + "/players/" + playerId
                + "/cp?delta=" + delta, null), MatchDto.class);
    }
    public MatchDto changeScore(Long matchId, Long playerId, int delta) throws Exception {
        return read(post("/api/matches/" + matchId + "/players/" + playerId
                + "/score?delta=" + delta, null), MatchDto.class);
    }

    // Field
    public List<MatchOperativeDto> getField(Long matchId) throws Exception {
        return readList(get("/api/matches/" + matchId + "/field"), new TypeReference<>() {});
    }
    public MatchOperativeDto deploy(Long matchId, Long playerId, Long operativeId,
                                    double x, double y) throws Exception {
        return read(post("/api/matches/" + matchId + "/field/deploy?playerId=" + playerId
                        + "&operativeId=" + operativeId + "&x=" + x + "&y=" + y, null),
                MatchOperativeDto.class);
    }
    public MatchOperativeDto reposition(Long id, double x, double y) throws Exception {
        return read(post("/api/match-operatives/" + id + "/reposition?x=" + x + "&y=" + y, null),
                MatchOperativeDto.class);
    }
    public MatchOperativeDto setOrder(Long id, String orderType) throws Exception {
        return read(patch("/api/match-operatives/" + id + "/order?orderType=" + orderType, null),
                MatchOperativeDto.class);
    }
    public MatchOperativeDto setState(Long id, String state) throws Exception {
        return read(patch("/api/match-operatives/" + id + "/state?state=" + state, null),
                MatchOperativeDto.class);
    }
    public MatchOperativeDto setWounds(Long id, int wounds) throws Exception {
        return read(patch("/api/match-operatives/" + id + "/wounds?wounds=" + wounds, null),
                MatchOperativeDto.class);
    }
    public MatchOperativeDto resetApl(Long id) throws Exception {
        return read(post("/api/match-operatives/" + id + "/reset-apl", null),
                MatchOperativeDto.class);
    }
    public List<MatchOperativeDto> resetAllApl(Long matchId) throws Exception {
        return readList(post("/api/matches/" + matchId + "/reset-all-apl", null),
                new TypeReference<>() {});
    }
    public void removeFromField(Long id) throws Exception {
        delete("/api/match-operatives/" + id);
    }
    public void clearField(Long matchId) throws Exception {
        delete("/api/matches/" + matchId + "/field");
    }
    public MatchDto startBattle(Long matchId) throws Exception {
        return read(post("/api/matches/" + matchId + "/start-battle", null), MatchDto.class);
    }
    public MatchDto restartDeployment(Long matchId) throws Exception {
        return read(post("/api/matches/" + matchId + "/restart-deployment", null), MatchDto.class);
    }
    public MatchDto nextTurn(Long matchId) throws Exception {
        return read(post("/api/matches/" + matchId + "/next-turn", null), MatchDto.class);
    }
    public MatchDto nextPhase(Long matchId) throws Exception {
        return read(post("/api/matches/" + matchId + "/next-phase", null), MatchDto.class);
    }
    // Combat
    public CombatResultDto shoot(Long attackerId, Long targetId) throws Exception {
        return read(post("/api/combat/shoot?attackerId=" + attackerId
                + "&targetId=" + targetId, null), CombatResultDto.class);
    }
    public CombatResultDto fight(Long attackerId, Long targetId) throws Exception {
        return read(post("/api/combat/fight?attackerId=" + attackerId
                + "&targetId=" + targetId, null), CombatResultDto.class);
    }

    // Низкоуровневые
    // Admin
    public void resetTestData() throws Exception {
        post("/api/admin/reset-data", null);
    }

    private String get(String path) throws Exception {
        return send(HttpRequest.newBuilder().uri(URI.create(BASE + path)).GET());
    }
    private String post(String path, Object body) throws Exception {
        var b = HttpRequest.newBuilder().uri(URI.create(BASE + path))
                .header("Content-Type", "application/json");
        if (body == null) b.POST(HttpRequest.BodyPublishers.noBody());
        else b.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        return send(b);
    }
    private String put(String path, Object body) throws Exception {
        var b = HttpRequest.newBuilder().uri(URI.create(BASE + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        return send(b);
    }
    private String patch(String path, Object body) throws Exception {
        var b = HttpRequest.newBuilder().uri(URI.create(BASE + path))
                .header("Content-Type", "application/json");
        if (body == null) b.method("PATCH", HttpRequest.BodyPublishers.noBody());
        else b.method("PATCH", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        return send(b);
    }
    private void delete(String path) throws Exception {
        send(HttpRequest.newBuilder().uri(URI.create(BASE + path)).DELETE());
    }
    private String send(HttpRequest.Builder b) throws Exception {
        var req = b.header("Accept", "application/json").build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }
    private <T> T read(String body, Class<T> type) throws Exception {
        return mapper.readValue(body, type);
    }
    private <T> List<T> readList(String body, TypeReference<List<T>> type) throws Exception {
        return mapper.readValue(body, type);
    }
}