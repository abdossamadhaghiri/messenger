package com.example.server.Repository;

import org.example.Entity.Message;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
@Repository
public interface MessageRepository extends CrudRepository<Message, Integer> {
    List<Message> findMessagesByReceiverInAndSenderIn(Collection<String> receiver, Collection<String> sender);

}
