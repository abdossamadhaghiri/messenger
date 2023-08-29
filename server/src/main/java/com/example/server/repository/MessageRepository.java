package com.example.server.repository;

import lombok.NonNull;
import org.example.entity.Message;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface MessageRepository extends ListCrudRepository<Message, Integer> {
    List<Message> findMessagesByChatId(@NonNull Long chatId);

}
