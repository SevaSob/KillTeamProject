package ru.petrsu.killteam.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.petrsu.killteam.dto.MatchOperativeDto;
import ru.petrsu.killteam.entity.*;
import ru.petrsu.killteam.repository.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchOperativeService {

    private final MatchOperativeRepository repo;
    private final MatchRepository matchRepo;
    private final MatchPlayerRepository playerRepo;
    private final OperativeRepository operativeRepo;
    private final TurningPointRepository turningPointRepo;
    private final KillTeamMapper mapper;

    @Transactional(readOnly = true)
    public List<MatchOperativeDto> getByMatch(Long matchId) {
        return repo.findByMatchId(matchId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public MatchOperativeDto deploy(Long matchId, Long playerId, Long operativeId, double x, double y) {
        Match match = matchRepo.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!match.isDeploymentPhase()) {
            throw new RuntimeException("Выставление бойцов доступно только в фазе расстановки");
        }

        MatchPlayer player = playerRepo.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        Operative operative = operativeRepo.findById(operativeId)
                .orElseThrow(() -> new RuntimeException("Operative not found"));

        boolean exists = repo.findByMatchId(matchId).stream()
                .anyMatch(mo -> mo.getOperative().getId().equals(operativeId)
                        && mo.getPlayer().getId().equals(playerId));
        if (exists) throw new RuntimeException("Боец уже на поле");

        MatchOperative mo = new MatchOperative(match, player, operative, x, y);
        return mapper.toDto(repo.save(mo));
    }

    @Transactional
    public MatchOperativeDto reposition(Long id, double x, double y) {
        MatchOperative mo = find(id);
        Match match = mo.getMatch();

        if (mo.getWoundsCurrent() <= 0) {
            throw new RuntimeException("Боец выведен из строя и не может двигаться");
        }

        double[] dims = parseKillzone(match.getKillzone());
        if (x < 0 || y < 0 || x > dims[0] || y > dims[1]) {
            throw new RuntimeException(String.format(
                    "Нельзя выйти за пределы поля (%.0f×%.0f дюймов)", dims[0], dims[1]));
        }

        // Фаза расстановки — свободное перемещение
        if (match.isDeploymentPhase()) {
            mo.setPosX(x);
            mo.setPosY(y);
            return mapper.toDto(repo.save(mo));
        }

        // Фаза боя — только активный игрок
        TurningPoint current = turningPointRepo.findByMatchIdOrderByNumberAsc(match.getId())
                .stream().filter(tp -> !"FINISHED".equals(tp.getPhase())).findFirst().orElse(null);
        if (current == null || !"FIREFIGHT".equals(current.getPhase())) {
            throw new RuntimeException("Движение возможно только в фазе боя");
        }

        if (match.getActivePlayerId() != null
                && !match.getActivePlayerId().equals(mo.getPlayer().getId())) {
            throw new RuntimeException("Сейчас не ваш ход");
        }

        if ("EXPENDED".equals(mo.getState())) {
            throw new RuntimeException("Боец уже потрачен в этом раунде");
        }
        if (mo.getCurrentApl() < 1) {
            throw new RuntimeException("Недостаточно ОД для перемещения");
        }

        double dist = Math.hypot(x - mo.getPosX(), y - mo.getPosY());
        double maxDist = mo.getOperative().getMove();
        if (dist > maxDist + 0.01) {
            throw new RuntimeException(String.format(
                    "Слишком далеко: %.1f\" (максимум %.0f\")", dist, maxDist));
        }

        mo.setPosX(x);
        mo.setPosY(y);
        mo.setCurrentApl(mo.getCurrentApl() - 1);
        if (mo.getCurrentApl() == 0) mo.setState("EXPENDED");

        return mapper.toDto(repo.save(mo));
    }

    @Transactional
    public MatchOperativeDto move(Long id, double x, double y) {
        MatchOperative mo = find(id);
        mo.setPosX(x);
        mo.setPosY(y);
        return mapper.toDto(repo.save(mo));
    }

    @Transactional
    public MatchOperativeDto setOrder(Long id, String orderType) {
        MatchOperative mo = find(id);
        Match match = mo.getMatch();
        if (match.isDeploymentPhase()) {
            // В расстановке тоже можно менять приказ (подготовка)
            mo.setOrderType(orderType);
            return mapper.toDto(repo.save(mo));
        }
        TurningPoint current = turningPointRepo.findByMatchIdOrderByNumberAsc(match.getId())
                .stream().filter(tp -> !"FINISHED".equals(tp.getPhase())).findFirst().orElse(null);
        if (current == null || !"FIREFIGHT".equals(current.getPhase())) {
            throw new RuntimeException("Смена приказа возможна только в фазе боя");
        }
        if (match.getActivePlayerId() != null
                && !match.getActivePlayerId().equals(mo.getPlayer().getId())) {
            throw new RuntimeException("Сейчас не ваш ход");
        }
        mo.setOrderType(orderType);
        return mapper.toDto(repo.save(mo));
    }

    @Transactional
    public MatchOperativeDto setState(Long id, String state) {
        MatchOperative mo = find(id);
        mo.setState(state);
        return mapper.toDto(repo.save(mo));
    }

    @Transactional
    public MatchOperativeDto setWounds(Long id, int wounds) {
        MatchOperative mo = find(id);
        mo.setWoundsCurrent(Math.max(0, wounds));
        return mapper.toDto(repo.save(mo));
    }

    @Transactional
    public MatchOperativeDto setApl(Long id, int apl) {
        MatchOperative mo = find(id);
        mo.setCurrentApl(Math.max(0, apl));
        if (mo.getCurrentApl() == 0) mo.setState("EXPENDED");
        return mapper.toDto(repo.save(mo));
    }

    @Transactional
    public MatchOperativeDto resetApl(Long id) {
        MatchOperative mo = find(id);
        mo.setCurrentApl(mo.getOperative().getApl());
        mo.setState("READY");
        return mapper.toDto(repo.save(mo));
    }

    @Transactional
    public List<MatchOperativeDto> resetAllApl(Long matchId) {
        var list = repo.findByMatchId(matchId);
        for (MatchOperative mo : list) {
            mo.setCurrentApl(mo.getOperative().getApl());
            mo.setState("READY");
        }
        repo.saveAll(list);
        return list.stream().map(mapper::toDto).toList();
    }

    @Transactional
    public void remove(Long id) {
        MatchOperative mo = find(id);
        if (!mo.getMatch().isDeploymentPhase()) {
            throw new RuntimeException("Убирать бойцов с поля можно только в фазе расстановки");
        }
        repo.deleteById(id);
    }

    @Transactional
    public void clearField(Long matchId) {
        Match match = matchRepo.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        if (!match.isDeploymentPhase()) {
            throw new RuntimeException("Очистить поле можно только в фазе расстановки");
        }
        repo.deleteByMatchId(matchId);
    }

    private MatchOperative find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Боец на поле не найден: " + id));
    }

    private double[] parseKillzone(String kz) {
        if (kz != null && kz.matches("\\s*\\d+(\\.\\d+)?\\s*[xX×]\\s*\\d+(\\.\\d+)?\\s*")) {
            String[] parts = kz.toLowerCase().split("[xX×]");
            try {
                double w = Double.parseDouble(parts[0].trim());
                double h = Double.parseDouble(parts[1].trim());
                if (w > 0 && h > 0 && w <= 500 && h <= 500) return new double[]{w, h};
            } catch (Exception ignored) {}
        }
        return new double[]{30, 22};
    }
}