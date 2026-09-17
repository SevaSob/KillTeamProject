package ru.petrsu.killteam;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.petrsu.killteam.repository.FactionRepository;
import ru.petrsu.killteam.service.DataSeedService;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DataSeedService seedService;
    private final FactionRepository factionRepo;

    @Value("${app.data.reset:false}")
    private boolean resetOnStartup;

    @Override
    public void run(String... args) {
        if (resetOnStartup) {
            System.out.println(">>> app.data.reset=true — перезаливка на старте");
            seedService.resetAndSeed();
            return;
        }
        if (factionRepo.count() == 0) {
            System.out.println(">>> База пуста — заливаю демо-данные");
            seedService.resetAndSeed();
        } else {
            System.out.println(">>> База уже содержит данные. Пропускаю.");
        }
    }
}