package com.example.jobpuzzle.interview.repository;

import com.example.jobpuzzle.interview.entity.InterviewMessage;
import com.example.jobpuzzle.interview.entity.InterviewMessageSender;
import com.example.jobpuzzle.interview.entity.InterviewMessageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InterviewMessageRepository
        extends JpaRepository<InterviewMessage, Long> {

    // 세션 질문의 전체 대화 기록을 발생 순서대로 조회
    List<InterviewMessage>
    findBySessionQuestion_SessionQuestionIdOrderByMessageIdAsc(
            Long sessionQuestionId
    );

    // 사용자 소유권까지 검증하여 메시지 조회
    Optional<InterviewMessage>
    findByMessageIdAndSessionQuestion_Session_User_UserId(
            Long messageId,
            Long userId
    );

    // 세션에서 답변 메시지가 하나라도 존재하는지 확인
    boolean existsBySessionQuestion_Session_SessionIdAndMessageTypeIn(
            Long sessionId,
            Collection<InterviewMessageType> messageTypes
    );

    // 현재 질문을 명시적으로 마칠 수 있는지 확인한다. 평가 실패 답변도 제출 이력으로 본다.
    boolean existsBySessionQuestion_SessionQuestionIdAndMessageTypeIn(
            Long sessionQuestionId,
            Collection<InterviewMessageType> messageTypes
    );

    // 특정 세션 질문에서 생성된 꼬리질문 개수 확인
    long countBySessionQuestion_SessionQuestionIdAndMessageType(
            Long sessionQuestionId,
            InterviewMessageType messageType
    );

    // 동일 질문 메시지에 이미 사용자 답변이 제출됐는지 확인
    boolean existsByParentMessage_MessageIdAndSenderAndMessageTypeIn(
            Long parentMessageId,
            InterviewMessageSender sender,
            Collection<InterviewMessageType> messageTypes
    );

    long countByParentMessage_MessageIdAndSenderAndMessageTypeIn(
            Long parentMessageId,
            InterviewMessageSender sender,
            Collection<InterviewMessageType> messageTypes
    );

    // 최종 리포트에서 원 답변 메시지를 조회할 때 사용하는 실제 JPA 경로
    Optional<InterviewMessage>
    findBySessionQuestion_SessionQuestionIdAndMessageType(
            Long sessionQuestionId,
            InterviewMessageType messageType
    );

    // develop 최종 리포트 코드의 기존 호출 이름을 유지하기 위한 호환 메서드
    default Optional<InterviewMessage>
    findBySessionQuestionIdAndMessageType(
            Long sessionQuestionId,
            InterviewMessageType messageType
    ) {
        return findBySessionQuestion_SessionQuestionIdAndMessageType(
                sessionQuestionId,
                messageType
        );
    }
}
