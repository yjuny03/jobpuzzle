-- 운영 DB에서 무관 답변 시도를 보존하기 위한 메시지 유형 확장.
ALTER TABLE interview_message
    MODIFY COLUMN message_type
    ENUM(
        'ORIGINAL_QUESTION',
        'ORIGINAL_ANSWER',
        'FOLLOW_UP_QUESTION',
        'FOLLOW_UP_ANSWER',
        'REJECTED_ANSWER',
        'EVALUATION_FAILED_ANSWER'
    ) NOT NULL;
