package com.maison.backend.repository;

import com.maison.backend.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    List<RestaurantTable> findByCapacityGreaterThanEqualAndStatusNotOrderByCapacityAsc(int party, String status);
    List<RestaurantTable> findAllByOrderByCapacityAsc();
}
