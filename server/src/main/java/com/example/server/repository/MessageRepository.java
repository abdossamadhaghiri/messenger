package com.example.server.repository;

import org.example.entity.Message;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends ListCrudRepository<Message, Long> {
}
