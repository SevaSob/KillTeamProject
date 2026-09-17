package ru.petrsu.killteam.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.petrsu.killteam.dto.CombatResultDto;
import ru.petrsu.killteam.entity.*;
import ru.petrsu.killteam.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CombatService {

    private final MatchOperativeRepository moRepo;
    private final TurningPointRepository turningPointRepo;
    private final Random random = new Random();

    // ================== СТРЕЛЬБА ==================
    @Transactional
    public CombatResultDto shoot(Long attackerId, Long targetId) {
        MatchOperative attacker = find(attackerId);
        MatchOperative target = find(targetId);

        validatePhase(attacker);
        validateAlive(attacker);
        validateAlive(target);
        validateAction(attacker);

        if (attacker.getPlayer().getId().equals(target.getPlayer().getId())) {
            throw new RuntimeException("Нельзя стрелять по своему бойцу");
        }

        Weapon weapon = getRangedWeapon(attacker.getOperative());
        if (weapon == null) {
            throw new RuntimeException("У бойца нет дальнобойного оружия");
        }

        double dist = Math.hypot(attacker.getPosX() - target.getPosX(),
                attacker.getPosY() - target.getPosY());
        Double range = parseRange(weapon.getSpecial());
        if (range != null && dist > range) {
            throw new RuntimeException(String.format(
                    "Цель слишком далеко: %.1f\" (дальность оружия %.0f\")", dist, range));
        }

        int hitThreshold = parseThreshold(weapon.getHit());
        int[] dmg = parseDmg(weapon.getDmg());

        List<Integer> attackRolls = rollDice(weapon.getAtk());
        List<Integer> defenseRolls = rollDice(3);

        int attackHits = countSuccesses(attackRolls, hitThreshold);
        int attackCrits = countCrits(attackRolls);
        int defenseHits = countSuccesses(defenseRolls, 5);
        int defenseCrits = countCrits(defenseRolls);

        int netCrits = Math.max(0, attackCrits - defenseCrits);
        int netHits = Math.max(0, attackHits - defenseHits);

        int totalDamage = netHits * dmg[0] + netCrits * dmg[1];

        int before = target.getWoundsCurrent();
        int after = Math.max(0, before - totalDamage);
        target.setWoundsCurrent(after);

        String injury = null;
        if (after == 0) {
            int roll = roll();
            if (roll >= 4) {
                injury = "Боец выведен из строя (бросок " + roll + ")!";
                target.setState("EXPENDED");
            } else {
                injury = "Лёгкое ранение (бросок " + roll + "). Боец держится на 1 ране.";
                target.setWoundsCurrent(1);
            }
        }

        spendApl(attacker, 1);
        moRepo.save(attacker);
        moRepo.save(target);

        String log = buildShootLog(attacker, target, weapon, attackRolls, defenseRolls,
                attackHits, attackCrits, defenseHits, defenseCrits,
                netHits, netCrits, totalDamage, before, after, injury);

        return new CombatResultDto(
                "SHOOT",
                attacker.getOperative().getName(),
                target.getOperative().getName(),
                weapon.getName(),
                attackRolls, defenseRolls,
                attackHits, attackCrits,
                defenseHits, defenseCrits,
                netHits, netCrits,
                totalDamage, before, after,
                injury, log
        );
    }

    // ================== БЛИЖНИЙ БОЙ ==================
    @Transactional
    public CombatResultDto fight(Long attackerId, Long targetId) {
        MatchOperative attacker = find(attackerId);
        MatchOperative target = find(targetId);

        validatePhase(attacker);
        validateAlive(attacker);
        validateAlive(target);
        validateAction(attacker);

        if (attacker.getPlayer().getId().equals(target.getPlayer().getId())) {
            throw new RuntimeException("Нельзя атаковать своего бойца");
        }

        double dist = Math.hypot(attacker.getPosX() - target.getPosX(),
                attacker.getPosY() - target.getPosY());
        if (dist > 2.0) {
            throw new RuntimeException(String.format(
                    "Цель слишком далеко для ближнего боя: %.1f\" (нужно ≤ 2\")", dist));
        }

        Weapon weaponA = getMeleeWeapon(attacker.getOperative());
        Weapon weaponB = getMeleeWeapon(target.getOperative());
        if (weaponA == null) throw new RuntimeException("У атакующего нет оружия ближнего боя");
        if (weaponB == null) throw new RuntimeException("У защитника нет оружия ближнего боя");

        int hitA = parseThreshold(weaponA.getHit());
        int hitB = parseThreshold(weaponB.getHit());
        int[] dmgA = parseDmg(weaponA.getDmg());
        int[] dmgB = parseDmg(weaponB.getDmg());

        List<Integer> rollsA = rollDice(weaponA.getAtk());
        List<Integer> rollsB = rollDice(weaponB.getAtk());

        int hitsA = countSuccesses(rollsA, hitA);
        int critsA = countCrits(rollsA);
        int hitsB = countSuccesses(rollsB, hitB);
        int critsB = countCrits(rollsB);

        int netCritsA = Math.max(0, critsA - critsB);
        int netHitsA = Math.max(0, hitsA - hitsB);
        int netCritsB = Math.max(0, critsB - critsA);
        int netHitsB = Math.max(0, hitsB - hitsA);

        int dmgToTarget = netHitsA * dmgA[0] + netCritsA * dmgA[1];
        int dmgToAttacker = netHitsB * dmgB[0] + netCritsB * dmgB[1];

        int beforeTarget = target.getWoundsCurrent();
        int beforeAttacker = attacker.getWoundsCurrent();
        int afterTarget = Math.max(0, beforeTarget - dmgToTarget);
        int afterAttacker = Math.max(0, beforeAttacker - dmgToAttacker);

        target.setWoundsCurrent(afterTarget);
        attacker.setWoundsCurrent(afterAttacker);

        String injuryTarget = null, injuryAttacker = null;
        if (afterTarget == 0) {
            int r = roll();
            injuryTarget = r >= 4 ? "Защитник выведен из строя (бросок " + r + ")"
                    : "Защитник легко ранен (бросок " + r + ")";
            if (r >= 4) target.setState("EXPENDED"); else target.setWoundsCurrent(1);
        }
        if (afterAttacker == 0) {
            int r = roll();
            injuryAttacker = r >= 4 ? "Атакующий выведен из строя (бросок " + r + ")"
                    : "Атакующий легко ранен (бросок " + r + ")";
            if (r >= 4) attacker.setState("EXPENDED"); else attacker.setWoundsCurrent(1);
        }

        spendApl(attacker, 1);
        moRepo.save(attacker);
        moRepo.save(target);

        String log = buildFightLog(attacker, target, weaponA, weaponB,
                rollsA, rollsB, netHitsA, netCritsA, netHitsB, netCritsB,
                dmgToTarget, dmgToAttacker, injuryTarget, injuryAttacker);

        return new CombatResultDto(
                "FIGHT",
                attacker.getOperative().getName(),
                target.getOperative().getName(),
                weaponA.getName(),
                rollsA, rollsB,
                hitsA, critsA, hitsB, critsB,
                netHitsA, netCritsA,
                dmgToTarget,
                beforeTarget, afterTarget,
                injuryTarget != null ? injuryTarget : injuryAttacker,
                log
        );
    }

    // ================== УТИЛИТЫ ==================
    private MatchOperative find(Long id) {
        return moRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Боец на поле не найден: " + id));
    }

    private void validatePhase(MatchOperative attacker) {
        Match match = attacker.getMatch();
        if (match.isDeploymentPhase()) {
            throw new RuntimeException("Сначала начните бой");
        }

        // Берём актуальный раунд из репозитория — не из ленивой коллекции
        TurningPoint current = turningPointRepo
                .findByMatchIdOrderByNumberAsc(match.getId()).stream()
                .filter(tp -> !"FINISHED".equals(tp.getPhase()))
                .findFirst()
                .orElse(null);

        if (current == null) {
            throw new RuntimeException("Все раунды завершены");
        }
        if (!"FIREFIGHT".equals(current.getPhase())) {
            throw new RuntimeException("Атака возможна только в фазе боя");
        }
        if (match.getActivePlayerId() != null
                && !match.getActivePlayerId().equals(attacker.getPlayer().getId())) {
            throw new RuntimeException("Сейчас не ваш ход");
        }
    }

    private void validateAlive(MatchOperative mo) {
        if (mo.getWoundsCurrent() <= 0) {
            throw new RuntimeException("Боец выведен из строя и не может действовать");
        }
    }

    private void validateAction(MatchOperative mo) {
        if ("EXPENDED".equals(mo.getState())) {
            throw new RuntimeException("Боец уже потрачен в этом раунде");
        }
        if (mo.getCurrentApl() < 1) {
            throw new RuntimeException("Недостаточно ОД");
        }
        if (!"ENGAGE".equals(mo.getOrderType())) {
            throw new RuntimeException("Боец с приказом Conceal не может атаковать");
        }
    }

    private void spendApl(MatchOperative mo, int cost) {
        mo.setCurrentApl(Math.max(0, mo.getCurrentApl() - cost));
        if (mo.getCurrentApl() == 0) mo.setState("EXPENDED");
    }

    private Weapon getRangedWeapon(Operative op) {
        return op.getWeapons().stream()
                .filter(w -> parseRange(w.getSpecial()) != null)
                .findFirst()
                .orElse(null);
    }

    private Weapon getMeleeWeapon(Operative op) {
        return op.getWeapons().stream()
                .filter(w -> parseRange(w.getSpecial()) == null)
                .findFirst()
                .orElseGet(() -> op.getWeapons().isEmpty() ? null : op.getWeapons().get(0));
    }

    private Double parseRange(String special) {
        if (special == null) return null;
        Matcher m = Pattern.compile("(?:Range|Дальность)\\s+(\\d+)").matcher(special);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private int parseThreshold(String hit) {
        if (hit == null) return 3;
        try { return Integer.parseInt(hit.replace("+", "").trim()); }
        catch (NumberFormatException e) { return 3; }
    }

    private int[] parseDmg(String dmg) {
        if (dmg == null) return new int[]{1, 1};
        try {
            String[] parts = dmg.split("/");
            int normal = Integer.parseInt(parts[0].trim());
            int crit = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : normal;
            return new int[]{normal, crit};
        } catch (Exception e) {
            return new int[]{1, 1};
        }
    }

    private List<Integer> rollDice(int count) {
        List<Integer> rolls = new ArrayList<>();
        for (int i = 0; i < count; i++) rolls.add(roll());
        return rolls;
    }

    private int roll() { return random.nextInt(6) + 1; }

    private int countSuccesses(List<Integer> rolls, int threshold) {
        return (int) rolls.stream().filter(r -> r >= threshold).count();
    }

    private int countCrits(List<Integer> rolls) {
        return (int) rolls.stream().filter(r -> r == 6).count();
    }

    private String buildShootLog(MatchOperative atk, MatchOperative tgt, Weapon w,
                                 List<Integer> aRolls, List<Integer> dRolls,
                                 int aH, int aC, int dH, int dC,
                                 int nH, int nC, int dmg, int before, int after,
                                 String injury) {
        StringBuilder sb = new StringBuilder();
        sb.append("СТРЕЛЬБА\n");
        sb.append(atk.getOperative().getName()).append(" → ")
                .append(tgt.getOperative().getName()).append("\n");
        sb.append("Оружие: ").append(w.getName()).append("\n\n");
        sb.append("Броски атакующего: ").append(aRolls)
                .append("  (успехов: ").append(aH).append(", критов: ").append(aC).append(")\n");
        sb.append("Броски защитника: ").append(dRolls)
                .append("  (успехов: ").append(dH).append(", критов: ").append(dC).append(")\n\n");
        sb.append("Неотменённых попаданий: ").append(nH).append("\n");
        sb.append("Неотменённых критов: ").append(nC).append("\n");
        sb.append("Всего урона: ").append(dmg).append("\n");
        sb.append("Раны цели: ").append(before).append(" → ").append(after).append("\n");
        if (injury != null) sb.append("\n").append(injury);
        return sb.toString();
    }

    private String buildFightLog(MatchOperative atk, MatchOperative tgt,
                                 Weapon wA, Weapon wB,
                                 List<Integer> rA, List<Integer> rB,
                                 int nHA, int nCA, int nHB, int nCB,
                                 int dmgA, int dmgB,
                                 String injuryT, String injuryA) {
        StringBuilder sb = new StringBuilder();
        sb.append("БЛИЖНИЙ БОЙ\n");
        sb.append(atk.getOperative().getName()).append(" (").append(wA.getName()).append(") → ")
                .append(tgt.getOperative().getName()).append(" (").append(wB.getName()).append(")\n\n");
        sb.append("Броски атакующего: ").append(rA)
                .append("  (нетто попаданий: ").append(nHA).append(", критов: ").append(nCA).append(")\n");
        sb.append("Броски защитника: ").append(rB)
                .append("  (нетто попаданий: ").append(nHB).append(", критов: ").append(nCB).append(")\n\n");
        sb.append("Урон по цели: ").append(dmgA).append("\n");
        sb.append("Урон по атакующему: ").append(dmgB).append("\n");
        if (injuryT != null) sb.append("\n").append(injuryT);
        if (injuryA != null) sb.append("\n").append(injuryA);
        return sb.toString();
    }
}