package ru.petrsu.killteam.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.petrsu.killteam.service.DataSeedService;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DataSeedService seedService;

    @PostMapping("/reset-data")
    public Map<String, Object> resetData() {
        seedService.resetAndSeed();
        return Map.of(
                "status", "ok",
                "message", "База очищена и заполнена тестовыми данными"
        );
    }
}