package com.godsse.backend.api.dto;

import java.time.Instant;

import com.godsse.backend.domain.ReviewReply;

/**
 * 답장 한 건의 응답.
 *
 * @param id        답장 번호
 * @param author    쓴 사람
 * @param body      내용
 * @param createdAt 작성 시각
 * @param editable  조회자가 이 답장을 삭제할 수 있는지(작성자 본인 여부)
 */
public record ReviewReplyResponse(Long id, UserSummary author, String body, Instant createdAt, boolean editable) {

	public static ReviewReplyResponse of(ReviewReply reply, boolean editable) {
		return new ReviewReplyResponse(reply.getId(), UserSummary.of(reply.getAuthor()), reply.getBody(),
				reply.getCreatedAt(), editable);
	}

}
