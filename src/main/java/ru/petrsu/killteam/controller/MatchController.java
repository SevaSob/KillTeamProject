package ru.petrsu.killteam.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.petrsu.killteam.dto.GameEventDto;
import ru.petrsu.killteam.dto.MatchDto;
import ru.petrsu.killteam.service.MatchService;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService service;

    @GetMapping
    public List<MatchDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public MatchDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchDto create(@RequestBody CreateMatchRequest req) {
        return service.create(
                req.name(), req.mission(), req.killzone(),
                req.player1Name(), req.killTeam1Id(),
                req.player2Name(), req.killTeam2Id()
        );
    }

    @PostMapping("/{id}/advance")
    public MatchDto advancePhase(@PathVariable Long id) {
        return service.nextPhase(id);
    }

    @PostMapping("/{id}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public GameEventDto addEvent(@PathVariable Long id, @RequestBody CreateEventRequest req) {
        return service.addEvent(
                id, req.turningPointId(), req.playerId(),
                req.operativeId(), req.eventType(), req.description()
        );
    }

    @GetMapping("/{id}/events")
    public List<GameEventDto> getEvents(@PathVariable Long id) {
        return service.getEvents(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    public record CreateMatchRequest(
            String name,
            String mission,
            String killzone,
            String player1Name,
            Long killTeam1Id,
            String player2Name,
            Long killTeam2Id
    ) {}

    public record CreateEventRequest(
            Long turningPointId,
            Long playerId,
            Long operativeId,
            String eventType,
            String description
    ) {}
    @PostMapping("/{matchId}/players/{playerId}/cp")
    public MatchDto changeCp(@PathVariable Long matchId,
                             @PathVariable Long playerId,
                             @RequestParam int delta) {
        return service.changeCp(matchId, playerId, delta);
    }

    @PostMapping("/{matchId}/players/{playerId}/score")
    public MatchDto changeScore(@PathVariable Long matchId,
                                @PathVariable Long playerId,
                                @RequestParam int delta) {
        return service.changeScore(matchId, playerId, delta);
    }
    @PostMapping("/{id}/start-battle")
    public MatchDto startBattle(@PathVariable Long id) {
        return service.startBattle(id);
    }

    @PostMapping("/{id}/restart-deployment")
    public MatchDto restartDeployment(@PathVariable Long id) {
        return service.restartDeployment(id);
    }

    @PostMapping("/{id}/next-turn")
    public MatchDto nextTurn(@PathVariable Long id) {
        return service.nextTurn(id);
    }
    @PostMapping("/{id}/next-phase")
    public MatchDto nextPhase(@PathVariable Long id) {
        return service.nextPhase(id);
    }
}