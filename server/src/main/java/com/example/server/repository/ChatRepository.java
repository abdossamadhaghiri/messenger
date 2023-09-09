package com.example.server.repository;

import org.example.entity.Chat;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends ListCrudRepository<Chat, Long> {

}
