package ru.petrsu.killteam.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.petrsu.killteam.dto.KillTeamDto;
import ru.petrsu.killteam.entity.Faction;
import ru.petrsu.killteam.entity.KillTeam;
import ru.petrsu.killteam.repository.FactionRepository;
import ru.petrsu.killteam.repository.KillTeamRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KillTeamService {

    private final KillTeamRepository repository;
    private final FactionRepository factionRepository;
    private final KillTeamMapper mapper;

    @Transactional(readOnly = true)
    public List<KillTeamDto> getAll(Long factionId) {
        List<KillTeam> teams = (factionId != null)
                ? repository.findByFactionId(factionId)
                : repository.findAll();
        return teams.stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public KillTeamDto getById(Long id) {
        return mapper.toDto(
                repository.findById(id).orElseThrow(
                        () -> new RuntimeException("KillTeam not found: " + id)
                )
        );
    }

    @Transactional
    public KillTeamDto create(KillTeamDto dto) {
        Faction faction = factionRepository.findById(dto.factionId())
                .orElseThrow(() -> new RuntimeException("Faction not found: " + dto.factionId()));
        KillTeam entity = mapper.toEntity(dto, faction);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public KillTeamDto update(Long id, KillTeamDto dto) {
        KillTeam entity = repository.findById(id).orElseThrow(
                () -> new RuntimeException("KillTeam not found: " + id)
        );
        Faction faction = factionRepository.findById(dto.factionId())
                .orElseThrow(() -> new RuntimeException("Faction not found: " + dto.factionId()));
        entity.setName(dto.name());
        entity.setVersion(dto.version());
        entity.setRulesText(dto.rulesText());
        entity.setFaction(faction);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}