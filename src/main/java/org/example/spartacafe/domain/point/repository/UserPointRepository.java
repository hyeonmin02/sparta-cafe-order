package org.example.spartacafe.domain.point.repository;

import jakarta.persistence.LockModeType;
import org.example.spartacafe.domain.point.entity.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {

    // userId가 FK겸 PK라서 findById로 바로 조회 가능
    // 동일 사용자가 모바일/키오스크로 동시에 같은 계정으로 주문시의 동시성 제어
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select up from UserPoint up where up.userId = :userId")
    Optional<UserPoint> findByUserIdWithLock(Long userId);
}
