package ru.petrsu.killteam.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.petrsu.killteam.dto.WeaponDto;
import ru.petrsu.killteam.entity.Operative;
import ru.petrsu.killteam.entity.Weapon;
import ru.petrsu.killteam.repository.OperativeRepository;
import ru.petrsu.killteam.repository.WeaponRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeaponService {

    private final WeaponRepository repo;
    private final OperativeRepository operativeRepo;
    private final KillTeamMapper mapper;

    @Transactional(readOnly = true)
    public List<WeaponDto> getByOperative(Long operativeId) {
        return repo.findAll().stream()
                .filter(w -> w.getOperative() != null
                        && w.getOperative().getId().equals(operativeId))
                .map(mapper::toDto).toList();
    }

    @Transactional
    public WeaponDto create(Long operativeId, WeaponDto dto) {
        Operative op = operativeRepo.findById(operativeId)
                .orElseThrow(() -> new RuntimeException("Оперативник не найден"));
        Weapon w = new Weapon(dto.name(), dto.atk(), dto.hit(), dto.dmg(), dto.special());
        w.setOperative(op);
        return mapper.toDto(repo.save(w));
    }

    @Transactional
    public WeaponDto update(Long id, WeaponDto dto) {
        Weapon w = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Оружие не найдено"));
        w.setName(dto.name());
        w.setAtk(dto.atk());
        w.setHit(dto.hit());
        w.setDmg(dto.dmg());
        w.setSpecial(dto.special());
        return mapper.toDto(repo.save(w));
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }
}