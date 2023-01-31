package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;    //컴파일 시점에서 체크하기 위해 final 사용 권장

    /*
    @Autowired
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
    setter Injection 사용시 테스트 작성시 더미 repository 작성가능
    실행중 다른사람이 repository 변경가능하나 실행중 바꿀일이 거의 없으므로
    생성자 Injection 권장
    */

    /**
     * 회원가입
     */
    @Transactional
    public Long join(Member member){
        validateDuplicateMember(member);    //중복 회원 검증
        memberRepository.save(member);
        return member.getId();
    }

    private void validateDuplicateMember(Member member) {
        List<Member> findMembers = memberRepository.findByName(member.getName());   //동시성 문제로 유니크 제약조건 걸어주는 것을 권장
        if (!findMembers.isEmpty()){
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        }
    }

    //회원 전체 조회
    public List<Member> findMembers(){
        return memberRepository.findAll();
    }

    public Member findOne(Member member){
        return memberRepository.findOne(member.getId());
    }

}
