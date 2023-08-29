package com.example.server.repository;

import org.example.entity.Chat;
import org.example.entity.Pv;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends ListCrudRepository<Chat, Long> {

}
