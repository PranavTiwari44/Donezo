package com.Springboot.ToDoWebApp.todo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real queries against a real (in-memory) database.
 */
@DataJpaTest
@ActiveProfiles("test")
class TodoRepositoryTest {

    @Autowired
    private TodoRepository todoRepository;

    @Test
    void findByIdAndUsernameFindsTheOwnersTodo() {
        Todo saved = todoRepository.save(newTodo("Pranav"));

        assertEquals(saved.getId(),
                todoRepository.findByIdAndUsername(saved.getId(), "Pranav").orElseThrow().getId());
    }

    @Test
    void findByIdAndUsernameFindsNothingForAnotherUser() {
        Todo saved = todoRepository.save(newTodo("Pranav"));

        assertTrue(todoRepository.findByIdAndUsername(saved.getId(), "Pran").isEmpty());
    }

    @Test
    void savingTodoWithIdZeroCreatesANewRow() {
        Todo first = todoRepository.save(newTodo("Pranav"));
        Todo second = todoRepository.save(newTodo("Pranav"));

        assertNotEquals(0, first.getId());
        assertNotEquals(first.getId(), second.getId());
    }

    private Todo newTodo(String username) {
        return new Todo(0, username, "A todo for testing", LocalDate.of(2030, 1, 1), false);
    }
}
