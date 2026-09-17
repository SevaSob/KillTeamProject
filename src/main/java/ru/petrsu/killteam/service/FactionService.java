package ru.petrsu.killteam.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.petrsu.killteam.dto.FactionDto;
import ru.petrsu.killteam.entity.Faction;
import ru.petrsu.killteam.repository.FactionRepository;
import ru.petrsu.killteam.repository.KillTeamRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FactionService {

    private final FactionRepository repo;
    private final KillTeamRepository killTeamRepo;
    private final KillTeamMapper mapper;

    @Transactional(readOnly = true)
    public List<FactionDto> getAll() {
        return repo.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FactionDto getById(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional
    public FactionDto create(FactionDto dto) {
        Faction parent = dto.parentId() != null ? find(dto.parentId()) : null;
        Faction entity = mapper.toEntity(dto, parent);
        return mapper.toDto(repo.save(entity));
    }

    @Transactional
    public FactionDto update(Long id, FactionDto dto) {
        Faction entity = find(id);
        if (dto.parentId() != null && dto.parentId().equals(id)) {
            throw new RuntimeException("Фракция не может быть родителем самой себе");
        }
        Faction parent = dto.parentId() != null ? find(dto.parentId()) : null;
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setParent(parent);
        return mapper.toDto(repo.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        long teams = killTeamRepo.findAll().stream()
                .filter(kt -> kt.getFaction() != null && kt.getFaction().getId().equals(id))
                .count();
        if (teams > 0) {
            throw new RuntimeException("Нельзя удалить фракцию: в ней " + teams + " команд");
        }
        long children = repo.findAll().stream()
                .filter(f -> f.getParent() != null && f.getParent().getId().equals(id))
                .count();
        if (children > 0) {
            throw new RuntimeException("Нельзя удалить фракцию: у неё " + children + " подфракций");
        }
        repo.deleteById(id);
    }

    private Faction find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Фракция не найдена: " + id));
    }
}