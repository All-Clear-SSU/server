package opensource.project.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import opensource.project.dto.MemberRequestDto;
import opensource.project.dto.MemberResponseDto;
import opensource.project.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 프론트엔드 연결 테스트용으로 모든 origin 허용
public class MemberController {

    private final MemberService memberService;

    // 회원 생성(ID 자동 생성 또는 수동 지정)
    @PostMapping
    public ResponseEntity<MemberResponseDto> createMember(@Valid @RequestBody MemberRequestDto requestDto) {
        MemberResponseDto response = memberService.createMember(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 전체 회원 조회
    @GetMapping
    public ResponseEntity<List<MemberResponseDto>> getAllMembers() {
        List<MemberResponseDto> members = memberService.getAllMembers();
        return ResponseEntity.ok(members);
    }

    // 특정 회원 조회
    @GetMapping("/{id}")
    public ResponseEntity<MemberResponseDto> getMember(@PathVariable Long id) {
        MemberResponseDto member = memberService.getMember(id);
        return ResponseEntity.ok(member);
    }

    // 특정 회원정보 수정
    @PutMapping("/{id}")
    public ResponseEntity<MemberResponseDto> updateMember(
            @PathVariable Long id,
            @RequestBody MemberRequestDto requestDto) {
        MemberResponseDto response = memberService.updateMember(id, requestDto);
        return ResponseEntity.ok(response);
    }

    // 회원 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok("Member deleted successfully");
    }
}
