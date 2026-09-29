
package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.SmsDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsDeliveryLogRepository
        extends JpaRepository<SmsDeliveryLog, Long> {

}