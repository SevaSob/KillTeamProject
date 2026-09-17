package ru.petrsu.killteam.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.petrsu.killteam.dto.WeaponDto;
import ru.petrsu.killteam.service.WeaponService;

import java.util.List;

@RestController
@RequestMapping("/api/weapons")
@RequiredArgsConstructor
public class WeaponController {

    private final WeaponService service;

    @GetMapping
    public List<WeaponDto> getByOperative(@RequestParam Long operativeId) {
        return service.getByOperative(operativeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WeaponDto create(@RequestParam Long operativeId, @RequestBody WeaponDto dto) {
        return service.create(operativeId, dto);
    }

    @PutMapping("/{id}")
    public WeaponDto update(@PathVariable Long id, @RequestBody WeaponDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}