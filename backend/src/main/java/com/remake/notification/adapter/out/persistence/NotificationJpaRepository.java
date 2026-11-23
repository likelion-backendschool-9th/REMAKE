package com.remake.notification.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.remake.notification.domain.NotificationType;

interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {

	Page<NotificationJpaEntity> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

	Optional<NotificationJpaEntity> findFirstByUserIdOrderByCreatedAtDescIdDesc(Long userId);

	long countByUserIdAndReadAtIsNull(Long userId);

	@Query("""
			select count(n) > 0 from NotificationJpaEntity n
			where n.userId = :userId
			  and n.type = :type
			  and n.ingredientId = :ingredientId
			  and n.createdAt >= :dayStart
			  and n.createdAt < :dayEnd
			""")
	boolean existsExpirySoonOnDay(
			@Param("userId") Long userId,
			@Param("type") NotificationType type,
			@Param("ingredientId") Long ingredientId,
			@Param("dayStart") LocalDateTime dayStart,
			@Param("dayEnd") LocalDateTime dayEnd);

	@Query("""
			select count(n) > 0 from NotificationJpaEntity n
			where n.userId = :userId
			  and n.type = :type
			  and n.createdAt >= :dayStart
			  and n.createdAt < :dayEnd
			""")
	boolean existsTypeOnDay(
			@Param("userId") Long userId,
			@Param("type") NotificationType type,
			@Param("dayStart") LocalDateTime dayStart,
			@Param("dayEnd") LocalDateTime dayEnd);

	@Modifying(clearAutomatically = true)
	@Query("""
			update NotificationJpaEntity n
			set n.readAt = :readAt
			where n.userId = :userId and n.readAt is null
			""")
	int markAllUnreadAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

	@Modifying(clearAutomatically = true)
	@Query("delete from NotificationJpaEntity n where n.createdAt < :before")
	int deleteByCreatedAtBefore(@Param("before") LocalDateTime before);
}
