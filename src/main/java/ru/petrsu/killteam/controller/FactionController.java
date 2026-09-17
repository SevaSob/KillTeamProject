package ru.petrsu.killteam.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.petrsu.killteam.dto.FactionDto;
import ru.petrsu.killteam.service.FactionService;

import java.util.List;

@RestController
@RequestMapping("/api/factions")
@RequiredArgsConstructor
public class FactionController {

    private final FactionService service;

    @GetMapping
    public List<FactionDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public FactionDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FactionDto create(@RequestBody FactionDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public FactionDto update(@PathVariable Long id, @RequestBody FactionDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}