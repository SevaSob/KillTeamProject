package ru.petrsu.killteam.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.petrsu.killteam.dto.CombatResultDto;
import ru.petrsu.killteam.service.CombatService;

@RestController
@RequestMapping("/api/combat")
@RequiredArgsConstructor
public class CombatController {

    private final CombatService service;

    @PostMapping("/shoot")
    public CombatResultDto shoot(@RequestParam Long attackerId,
                                 @RequestParam Long targetId) {
        return service.shoot(attackerId, targetId);
    }

    @PostMapping("/fight")
    public CombatResultDto fight(@RequestParam Long attackerId,
                                 @RequestParam Long targetId) {
        return service.fight(attackerId, targetId);
    }
}