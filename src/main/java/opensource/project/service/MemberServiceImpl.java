package opensource.project.service;

import opensource.project.domain.Member;
import opensource.project.dto.MemberRequestDto;
import opensource.project.dto.MemberResponseDto;
import opensource.project.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    // 회원 등록 (수동 ID 지정)
    @Override
    @Transactional
    public MemberResponseDto createMember(MemberRequestDto requestDto) {
        // ID 중복 체크
        if(memberRepository.existsById(requestDto.getId())){
            throw new IllegalArgumentException(
                    "Member with id " + requestDto.getId() + " already exists");
        }

        //Member 객체 생성
        Member member =
                Member.builder().id(requestDto.getId()).name(requestDto.getName()).build();

        Member savedMember = memberRepository.save(member);

        return MemberResponseDto.from(savedMember);
    }

    //전체 회원 조회
    @Override
    public List<MemberResponseDto> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(MemberResponseDto::from)
                .collect(Collectors.toList());
    }

    //특정 회원 조회
    @Override
    public MemberResponseDto getMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        return MemberResponseDto.from(member);
    }

    //특정 회원정보 수정
    @Override
    @Transactional
    public MemberResponseDto updateMember(Long id, MemberRequestDto requestDto) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + id));
        member.setName(requestDto.getName());
        return MemberResponseDto.from(member);
    }

    //특정 회원 삭제
    @Override
    @Transactional
    public void deleteMember(Long id) {
        Member member =
                memberRepository.findById(id).orElseThrow(()
                        -> new RuntimeException("Member not found with id: " + id));
        memberRepository.delete(member);
    }

}