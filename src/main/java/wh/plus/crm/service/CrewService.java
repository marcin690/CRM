package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wh.plus.crm.dto.crew.CrewDTO;
import wh.plus.crm.dto.crew.CrewMemberDTO;
import wh.plus.crm.mapper.CrewMapper;
import wh.plus.crm.model.crew.Crew;
import wh.plus.crm.model.crew.CrewMember;
import wh.plus.crm.repository.CrewMemberRepository;
import wh.plus.crm.repository.CrewRepository;
import wh.plus.crm.repository.MontageRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CrewService {

    private final CrewRepository crewRepository;
    private final CrewMemberRepository memberRepository;
    private final MontageRepository montageRepository;
    private final CrewMapper mapper;

    @Transactional(readOnly = true)
    public List<CrewDTO> list() {
        return crewRepository.findAllByOrderByNameAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public CrewDTO create(CrewDTO dto) {
        Crew crew = new Crew();
        mapper.update(dto, crew);
        return mapper.toDto(crewRepository.save(crew));
    }

    @Transactional
    public CrewDTO update(Long id, CrewDTO dto) {
        Crew crew = crewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Crew not found: " + id));
        mapper.update(dto, crew);
        return mapper.toDto(crewRepository.save(crew));
    }

    @Transactional
    public void delete(Long id) {
        montageRepository.clearCrew(id); // odpięcie montaży od usuwanej ekipy
        crewRepository.deleteById(id);
    }

    @Transactional
    public CrewMemberDTO addMember(Long crewId, CrewMemberDTO dto) {
        Crew crew = crewRepository.findById(crewId)
                .orElseThrow(() -> new IllegalArgumentException("Crew not found: " + crewId));
        CrewMember member = new CrewMember();
        member.setCrew(crew);
        mapper.updateMember(dto, member);
        return mapper.toMemberDto(memberRepository.save(member));
    }

    @Transactional
    public void deleteMember(Long memberId) {
        memberRepository.deleteById(memberId);
    }
}
