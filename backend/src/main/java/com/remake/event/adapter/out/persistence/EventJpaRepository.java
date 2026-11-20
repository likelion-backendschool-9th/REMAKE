package com.remake.event.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface EventJpaRepository extends JpaRepository<EventJpaEntity, Long> {
}
