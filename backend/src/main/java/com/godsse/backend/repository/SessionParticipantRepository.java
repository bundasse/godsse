package com.godsse.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.godsse.backend.domain.SessionParticipant;

/**
 * 세션 일정 참가자(함께한 사람) 조회/저장.
 *
 * <p>
 * 화면에 이름을 보여 주려면 사용자 정보가 필요하므로, 목록 조회는 항상 {@code join fetch} 로 사용자까지 함께 읽는다.
 */
public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, Long> {

	/** 한 일정의 참가자(기록한 순서). */
	@Query("select sp from SessionParticipant sp join fetch sp.user where sp.plan.id = :planId order by sp.id asc")
	List<SessionParticipant> findByPlanIdWithUser(@Param("planId") Long planId);

	/**
	 * 여러 일정의 참가자를 <b>한 번에</b> 가져온다(달력 한 달치).
	 *
	 * <p>
	 * 일정마다 조회하면 일정 수만큼 쿼리가 나가므로(N+1), id 목록으로 한 번에 읽고 서비스에서 일정별로 묶는다.
	 */
	@Query("select sp from SessionParticipant sp join fetch sp.user where sp.plan.id in :planIds order by sp.id asc")
	List<SessionParticipant> findByPlanIdsWithUser(@Param("planIds") List<Long> planIds);

	boolean existsByPlanIdAndUserId(Long planId, Long userId);

	Optional<SessionParticipant> findByPlanIdAndUserId(Long planId, Long userId);

	/** 일정을 지울 때 참가자 행을 먼저 지운다(자식 → 부모 순서). */
	void deleteByPlanId(Long planId);

}
