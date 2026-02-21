package com.kestrel.booking.repo;

import com.kestrel.booking.domain.Carrier;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarrierRepository extends JpaRepository<Carrier, Long> {

    Optional<Carrier> findByCodeIgnoreCase(String code);

    List<Carrier> findAllByOrderByCodeAsc();
}
