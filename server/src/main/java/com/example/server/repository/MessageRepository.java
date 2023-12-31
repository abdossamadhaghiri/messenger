package com.example.server.repository;

import lombok.NonNull;
import com.example.server.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findMessagesByChatId(@NonNull Long chatId);

}
