package opensource.project.service;

import opensource.project.dto.MemberRequestDto;
import opensource.project.dto.MemberResponseDto;

import java.util.List;

/**
 * 회원 관리 서비스 인터페이스
 */
public interface MemberService {

    // 회원 등록 (수동 ID 지정)
    MemberResponseDto createMember(MemberRequestDto requestDto);

    // 전체 회원 조회
    List<MemberResponseDto> getAllMembers();

    // 특정 회원 조회
    MemberResponseDto getMember(Long id);

    // 특정 회원정보 수정
    MemberResponseDto updateMember(Long id, MemberRequestDto requestDto);

    // 특정 회원 삭제
    void deleteMember(Long id);
}