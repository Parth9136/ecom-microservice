
package com.ecommerce.order.repositories;

import com.ecommerce.order.models.ReturnRequest;
import com.ecommerce.order.models.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ReturnRequestRepository
        extends JpaRepository<ReturnRequest, Long> {

    boolean existsByOrderItem_IdAndStatusIn(
            Long orderItemId,
            Collection<ReturnStatus> statuses
    );
}
