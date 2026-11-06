package com.Springboot.ToDoWebApp.todo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Integer> {

    public List<Todo> findByUsername(String username);
    Optional<Todo> findByIdAndUsername(int id, String username);

}
