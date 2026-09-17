package ru.petrsu.killteam.service;

import org.springframework.stereotype.Component;
import ru.petrsu.killteam.dto.*;
import ru.petrsu.killteam.entity.*;

import java.util.List;

@Component
public class KillTeamMapper {

    public FactionDto toDto(Faction e) {
        if (e == null) return null;
        return new FactionDto(
                e.getId(), e.getName(), e.getDescription(),
                e.getParent() != null ? e.getParent().getId() : null,
                e.getParent() != null ? e.getParent().getName() : null
        );
    }

    public WeaponDto toDto(Weapon e) {
        if (e == null) return null;
        return new WeaponDto(e.getId(), e.getName(), e.getAtk(),
                e.getHit(), e.getDmg(), e.getSpecial());
    }

    public OperativeDto toDto(Operative e) {
        if (e == null) return null;
        List<WeaponDto> ws = e.getWeapons().stream().map(this::toDto).toList();
        return new OperativeDto(e.getId(), e.getName(), e.getApl(), e.getMove(),
                e.getSave(), e.getWounds(), e.getAbility(), ws);
    }

    public KillTeamDto toDto(KillTeam e) {
        if (e == null) return null;
        List<OperativeDto> ops = e.getOperatives().stream().map(this::toDto).toList();
        return new KillTeamDto(
                e.getId(),
                e.getFaction() != null ? e.getFaction().getId() : null,
                e.getFaction() != null ? e.getFaction().getName() : null,
                e.getName(), e.getVersion(), e.getRulesText(), ops
        );
    }

    public MatchPlayerDto toDto(MatchPlayer e) {
        if (e == null) return null;
        return new MatchPlayerDto(
                e.getId(), e.getName(),
                e.getKillTeam() != null ? e.getKillTeam().getId() : null,
                e.getKillTeam() != null ? e.getKillTeam().getName() : null,
                e.getCp(), e.getScore(), e.isInitiative()
        );
    }

    public TurningPointDto toDto(TurningPoint e) {
        if (e == null) return null;
        return new TurningPointDto(e.getId(), e.getNumber(), e.getPhase());
    }

    public GameEventDto toDto(GameEvent e) {
        if (e == null) return null;
        return new GameEventDto(
                e.getId(),
                e.getMatch() != null ? e.getMatch().getId() : null,
                e.getTurningPoint() != null ? e.getTurningPoint().getId() : null,
                e.getPlayer() != null ? e.getPlayer().getId() : null,
                e.getOperative() != null ? e.getOperative().getId() : null,
                e.getEventType(), e.getDescription(), e.getCreatedAt()
        );
    }

    public MatchDto toDto(Match entity) {
        if (entity == null) return null;
        var players = entity.getPlayers().stream()
                .map(this::toDto)
                .sorted(java.util.Comparator.comparing(MatchPlayerDto::id))
                .toList();
        var tps = entity.getTurningPoints().stream()
                .map(this::toDto)
                .sorted(java.util.Comparator.comparing(TurningPointDto::number))
                .toList();
        var events = entity.getEvents().stream()
                .map(this::toDto)
                .sorted(java.util.Comparator.comparing(GameEventDto::createdAt))
                .toList();
        return new MatchDto(
                entity.getId(), entity.getName(), entity.getMission(), entity.getKillzone(),
                entity.getStatus(),
                entity.isDeploymentPhase(), entity.getActivePlayerId(),
                entity.getCreatedAt(),
                players, tps, events
        );
    }

    public MatchOperativeDto toDto(MatchOperative e) {
        if (e == null) return null;
        Operative op = e.getOperative();
        return new MatchOperativeDto(
                e.getId(),
                e.getMatch() != null ? e.getMatch().getId() : null,
                e.getPlayer() != null ? e.getPlayer().getId() : null,
                e.getPlayer() != null ? e.getPlayer().getName() : null,
                op != null ? op.getId() : null,
                op != null ? op.getName() : null,
                e.getPosX(), e.getPosY(),
                e.getOrderType(), e.getState(),
                e.getWoundsCurrent(), op != null ? op.getWounds() : 0,
                e.getCurrentApl(), op != null ? op.getApl() : 0,
                op != null ? op.getMove() : 0
        );
    }

    // === Entity из DTO ===

    public Faction toEntity(FactionDto dto, Faction parent) {
        Faction f = new Faction();
        f.setName(dto.name());
        f.setDescription(dto.description());
        f.setParent(parent);
        return f;
    }

    public KillTeam toEntity(KillTeamDto dto, Faction faction) {
        KillTeam kt = new KillTeam();
        kt.setName(dto.name());
        kt.setVersion(dto.version());
        kt.setRulesText(dto.rulesText());
        kt.setFaction(faction);
        return kt;
    }

    public Operative toEntity(OperativeDto dto, KillTeam killTeam) {
        Operative op = new Operative();
        op.setName(dto.name());
        op.setApl(dto.apl());
        op.setMove(dto.move());
        op.setSave(dto.save());
        op.setWounds(dto.wounds());
        op.setAbility(dto.ability());
        op.setKillTeam(killTeam);
        if (dto.weapons() != null) {
            for (WeaponDto w : dto.weapons()) {
                Weapon weapon = new Weapon(
                        w.name(), w.atk(), w.hit(), w.dmg(), w.special()
                );
                op.addWeapon(weapon);
            }
        }
        return op;
    }
}