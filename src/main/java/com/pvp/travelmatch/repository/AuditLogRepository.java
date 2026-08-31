package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        select a from AuditLog a
        join a.admin admin
        where (:search is null or :search = ''
          or lower(a.action) like lower(concat('%', :search, '%'))
          or lower(a.targetType) like lower(concat('%', :search, '%'))
          or lower(a.description) like lower(concat('%', :search, '%'))
          or lower(admin.name) like lower(concat('%', :search, '%')))
        order by a.createdAt desc
    """)
    Page<AuditLog> search(@Param("search") String search, Pageable pageable);
}
