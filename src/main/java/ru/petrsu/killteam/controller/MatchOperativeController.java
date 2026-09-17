package ru.petrsu.killteam.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.petrsu.killteam.dto.MatchOperativeDto;
import ru.petrsu.killteam.service.MatchOperativeService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MatchOperativeController {

    private final MatchOperativeService service;

    @GetMapping("/matches/{matchId}/field")
    public List<MatchOperativeDto> getField(@PathVariable Long matchId) {
        return service.getByMatch(matchId);
    }

    @PostMapping("/matches/{matchId}/field/deploy")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchOperativeDto deploy(@PathVariable Long matchId,
                                    @RequestParam Long playerId,
                                    @RequestParam Long operativeId,
                                    @RequestParam double x,
                                    @RequestParam double y) {
        return service.deploy(matchId, playerId, operativeId, x, y);
    }

    @PostMapping("/match-operatives/{id}/reposition")
    public MatchOperativeDto reposition(@PathVariable Long id,
                                        @RequestParam double x,
                                        @RequestParam double y) {
        return service.reposition(id, x, y);
    }

    @PatchMapping("/match-operatives/{id}/move")
    public MatchOperativeDto move(@PathVariable Long id,
                                  @RequestParam double x,
                                  @RequestParam double y) {
        return service.move(id, x, y);
    }

    @PatchMapping("/match-operatives/{id}/order")
    public MatchOperativeDto setOrder(@PathVariable Long id,
                                      @RequestParam String orderType) {
        return service.setOrder(id, orderType);
    }

    @PatchMapping("/match-operatives/{id}/state")
    public MatchOperativeDto setState(@PathVariable Long id,
                                      @RequestParam String state) {
        return service.setState(id, state);
    }

    @PatchMapping("/match-operatives/{id}/wounds")
    public MatchOperativeDto setWounds(@PathVariable Long id,
                                       @RequestParam int wounds) {
        return service.setWounds(id, wounds);
    }

    @PatchMapping("/match-operatives/{id}/apl")
    public MatchOperativeDto setApl(@PathVariable Long id, @RequestParam int apl) {
        return service.setApl(id, apl);
    }

    @PostMapping("/match-operatives/{id}/reset-apl")
    public MatchOperativeDto resetApl(@PathVariable Long id) {
        return service.resetApl(id);
    }

    @PostMapping("/matches/{matchId}/reset-all-apl")
    public List<MatchOperativeDto> resetAllApl(@PathVariable Long matchId) {
        return service.resetAllApl(matchId);
    }

    @DeleteMapping("/match-operatives/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) {
        service.remove(id);
    }

    @DeleteMapping("/matches/{matchId}/field")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearField(@PathVariable Long matchId) {
        service.clearField(matchId);
    }
}