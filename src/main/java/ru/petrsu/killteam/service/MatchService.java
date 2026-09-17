package ru.petrsu.killteam.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.petrsu.killteam.dto.*;
import ru.petrsu.killteam.entity.*;
import ru.petrsu.killteam.repository.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepo;
    private final MatchPlayerRepository playerRepo;
    private final TurningPointRepository turningPointRepo;
    private final GameEventRepository eventRepo;
    private final MatchOperativeRepository matchOperativeRepo;
    private final KillTeamRepository killTeamRepo;
    private final OperativeRepository operativeRepo;
    private final KillTeamMapper mapper;

    @Transactional(readOnly = true)
    public List<MatchDto> getAll() {
        return matchRepo.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public MatchDto getById(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional
    public MatchDto create(String name, String mission, String killzone,
                           String player1Name, Long killTeam1Id,
                           String player2Name, Long killTeam2Id) {
        KillTeam kt1 = killTeamRepo.findById(killTeam1Id)
                .orElseThrow(() -> new RuntimeException("KillTeam not found: " + killTeam1Id));
        KillTeam kt2 = killTeamRepo.findById(killTeam2Id)
                .orElseThrow(() -> new RuntimeException("KillTeam not found: " + killTeam2Id));

        Match match = new Match(name, mission, killzone);
        MatchPlayer p1 = new MatchPlayer(player1Name, kt1);
        MatchPlayer p2 = new MatchPlayer(player2Name, kt2);
        p1.setInitiative(true);
        match.addPlayer(p1);
        match.addPlayer(p2);

        for (int i = 1; i <= 4; i++) {
            match.addTurningPoint(new TurningPoint(i));
        }

        return mapper.toDto(matchRepo.save(match));
    }

    @Transactional
    public MatchDto startBattle(Long matchId) {
        Match match = find(matchId);
        if (!match.isDeploymentPhase()) {
            throw new RuntimeException("Бой уже идёт");
        }

        var field = matchOperativeRepo.findByMatchId(matchId);
        for (MatchPlayer player : match.getPlayers()) {
            boolean hasOperative = field.stream()
                    .anyMatch(mo -> mo.getPlayer().getId().equals(player.getId()));
            if (!hasOperative) {
                throw new RuntimeException("Игрок \"" + player.getName()
                        + "\" не выставил ни одного бойца");
            }
        }

        match.setDeploymentPhase(false);
        match.setStatus("IN_PROGRESS");
        match.setActivePlayerId(null);

        // Все раунды в фазу Стратегия
        for (TurningPoint tp : match.getTurningPoints()) {
            tp.setPhase("STRATEGY");
        }
        turningPointRepo.saveAll(match.getTurningPoints());

        return mapper.toDto(matchRepo.save(match));
    }

    /**
     * Переход по фазам:
     * STRATEGY → FIREFIGHT → FINISHED (раунд закрыт, следующий начнётся в STRATEGY)
     * После 4-го раунда — матч FINISHED.
     */
    @Transactional
    public MatchDto nextPhase(Long matchId) {
        Match match = find(matchId);
        if (match.isDeploymentPhase()) {
            throw new RuntimeException("Сначала начните бой");
        }

        List<TurningPoint> tps = turningPointRepo.findByMatchIdOrderByNumberAsc(matchId);
        TurningPoint current = tps.stream()
                .filter(tp -> !"FINISHED".equals(tp.getPhase()))
                .findFirst()
                .orElse(null);

        if (current == null) {
            throw new RuntimeException("Все раунды завершены");
        }

        if ("STRATEGY".equals(current.getPhase())) {
            // Стратегия → Бой
            current.setPhase("FIREFIGHT");
            turningPointRepo.save(current);

            resetAllAplAndStates(matchId);

            MatchPlayer active = match.getPlayers().stream()
                    .filter(MatchPlayer::isInitiative)
                    .findFirst()
                    .orElse(match.getPlayers().isEmpty() ? null : match.getPlayers().get(0));
            match.setActivePlayerId(active != null ? active.getId() : null);

        } else if ("FIREFIGHT".equals(current.getPhase())) {
            // Бой → Конец раунда
            current.setPhase("FINISHED");
            turningPointRepo.save(current);

            boolean allDone = tps.stream().allMatch(tp -> "FINISHED".equals(tp.getPhase()));
            if (allDone) {
                match.setStatus("FINISHED");
                match.setActivePlayerId(null);
            } else {
                match.setActivePlayerId(null);
            }
        }

        return mapper.toDto(matchRepo.save(match));
    }

    @Transactional
    public MatchDto nextTurn(Long matchId) {
        Match match = find(matchId);
        if (match.isDeploymentPhase()) throw new RuntimeException("Сначала начните бой");

        TurningPoint current = getCurrentTurningPoint(matchId);
        if (current == null || !"FIREFIGHT".equals(current.getPhase())) {
            throw new RuntimeException("Смена хода возможна только в фазе боя");
        }

        if (match.getPlayers().size() < 2) return mapper.toDto(match);

        Long currentActive = match.getActivePlayerId();
        MatchPlayer other = match.getPlayers().stream()
                .filter(p -> !p.getId().equals(currentActive))
                .findFirst()
                .orElse(match.getPlayers().get(0));
        match.setActivePlayerId(other.getId());

        return mapper.toDto(matchRepo.save(match));
    }

    @Transactional
    public MatchDto restartDeployment(Long matchId) {
        Match match = find(matchId);
        match.setDeploymentPhase(true);
        match.setStatus("PLANNED");
        match.setActivePlayerId(null);

        var field = matchOperativeRepo.findByMatchId(matchId);
        if (!field.isEmpty()) matchOperativeRepo.deleteAll(field);

        for (TurningPoint tp : match.getTurningPoints()) {
            tp.setPhase("STRATEGY");
        }
        turningPointRepo.saveAll(match.getTurningPoints());

        return mapper.toDto(matchRepo.save(match));
    }

    @Transactional
    public GameEventDto addEvent(Long matchId, Long turningPointId, Long playerId,
                                 Long operativeId, String eventType, String description) {
        Match match = find(matchId);
        GameEvent event = new GameEvent(eventType, description);
        event.setMatch(match);
        if (turningPointId != null) event.setTurningPoint(turningPointRepo.findById(turningPointId).orElse(null));
        if (playerId != null) event.setPlayer(playerRepo.findById(playerId).orElse(null));
        if (operativeId != null) event.setOperative(operativeRepo.findById(operativeId).orElse(null));
        return mapper.toDto(eventRepo.save(event));
    }

    @Transactional(readOnly = true)
    public List<GameEventDto> getEvents(Long matchId) {
        return eventRepo.findByMatchIdOrderByCreatedAtAsc(matchId)
                .stream().map(mapper::toDto).toList();
    }

    @Transactional
    public MatchDto changeCp(Long matchId, Long playerId, int delta) {
        Match match = find(matchId);
        MatchPlayer player = playerRepo.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found: " + playerId));
        player.setCp(Math.max(0, player.getCp() + delta));
        playerRepo.save(player);
        return mapper.toDto(match);
    }

    @Transactional
    public MatchDto changeScore(Long matchId, Long playerId, int delta) {
        Match match = find(matchId);
        MatchPlayer player = playerRepo.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found: " + playerId));
        player.setScore(Math.max(0, player.getScore() + delta));
        playerRepo.save(player);
        return mapper.toDto(match);
    }

    @Transactional
    public void delete(Long id) {
        var field = matchOperativeRepo.findByMatchId(id);
        if (!field.isEmpty()) matchOperativeRepo.deleteAll(field);
        matchRepo.deleteById(id);
    }

    public TurningPoint getCurrentTurningPoint(Long matchId) {
        return turningPointRepo.findByMatchIdOrderByNumberAsc(matchId).stream()
                .filter(tp -> !"FINISHED".equals(tp.getPhase()))
                .findFirst()
                .orElse(null);
    }

    private void resetAllAplAndStates(Long matchId) {
        var field = matchOperativeRepo.findByMatchId(matchId);
        for (MatchOperative mo : field) {
            mo.setCurrentApl(mo.getOperative().getApl());
            mo.setState("READY");
        }
        matchOperativeRepo.saveAll(field);
    }

    private Match find(Long id) {
        return matchRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found: " + id));
    }
}