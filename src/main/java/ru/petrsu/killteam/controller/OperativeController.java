package ru.petrsu.killteam.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.petrsu.killteam.dto.OperativeDto;
import ru.petrsu.killteam.service.OperativeService;

import java.util.List;

@RestController
@RequestMapping("/api/operatives")
@RequiredArgsConstructor
public class OperativeController {

    private final OperativeService service;

    @GetMapping
    public List<OperativeDto> getByKillTeam(@RequestParam Long killTeamId) {
        return service.getByKillTeam(killTeamId);
    }

    @GetMapping("/{id}")
    public OperativeDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OperativeDto create(@RequestParam Long killTeamId, @RequestBody OperativeDto dto) {
        return service.create(killTeamId, dto);
    }

    @PutMapping("/{id}")
    public OperativeDto update(@PathVariable Long id, @RequestBody OperativeDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}