package com.kestrel.booking.repo;

import com.kestrel.booking.domain.Shipment;
import com.kestrel.booking.domain.ShipmentStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByReference(String reference);

    List<Shipment> findByCarrierIdAndStatus(Long carrierId, ShipmentStatus status);

    long countByCarrierIdAndStatusIn(Long carrierId, Collection<ShipmentStatus> statuses);

    @Query("select s.reference from Shipment s where s.id = :id")
    Optional<String> findReferenceById(@Param("id") Long id);

    @Query("""
            select new com.kestrel.booking.repo.CarrierLoad(s.carrierId, count(s))
            from Shipment s
            where s.carrierId is not null and s.status in :statuses
            group by s.carrierId
            """)
    List<CarrierLoad> loadByCarrier(@Param("statuses") Collection<ShipmentStatus> statuses);
}
