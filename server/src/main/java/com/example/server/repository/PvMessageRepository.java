package com.example.server.repository;

import com.example.server.entity.PvMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PvMessageRepository extends JpaRepository<PvMessage, Long> {

}