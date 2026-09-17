package ru.petrsu.killteam.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.petrsu.killteam.dto.OperativeDto;
import ru.petrsu.killteam.entity.KillTeam;
import ru.petrsu.killteam.entity.Operative;
import ru.petrsu.killteam.entity.Weapon;
import ru.petrsu.killteam.repository.KillTeamRepository;
import ru.petrsu.killteam.repository.OperativeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperativeService {

    private final OperativeRepository repo;
    private final KillTeamRepository killTeamRepo;
    private final KillTeamMapper mapper;

    @Transactional(readOnly = true)
    public List<OperativeDto> getByKillTeam(Long killTeamId) {
        return repo.findByKillTeamId(killTeamId).stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OperativeDto getById(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional
    public OperativeDto create(Long killTeamId, OperativeDto dto) {
        KillTeam kt = killTeamRepo.findById(killTeamId)
                .orElseThrow(() -> new RuntimeException("KillTeam не найден: " + killTeamId));
        Operative entity = mapper.toEntity(dto, kt);
        return mapper.toDto(repo.save(entity));
    }

    @Transactional
    public OperativeDto update(Long id, OperativeDto dto) {
        Operative entity = find(id);
        entity.setName(dto.name());
        entity.setApl(dto.apl());
        entity.setMove(dto.move());
        entity.setSave(dto.save());
        entity.setWounds(dto.wounds());
        entity.setAbility(dto.ability());

        // Перезаписываем оружие
        entity.getWeapons().clear();
        if (dto.weapons() != null) {
            for (var w : dto.weapons()) {
                Weapon weapon = new Weapon(w.name(), w.atk(), w.hit(), w.dmg(), w.special());
                entity.addWeapon(weapon);
            }
        }
        return mapper.toDto(repo.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    private Operative find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Оперативник не найден: " + id));
    }
}