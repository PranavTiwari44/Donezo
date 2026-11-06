package com.Springboot.ToDoWebApp.todo;

import com.Springboot.ToDoWebApp.security.SpringSecurityConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Todo 5 belongs to Pranav. Pran must not be able to read or change it.
 */
@WebMvcTest(ToDoControllerJpa.class)
@Import(SpringSecurityConfiguration.class)
class ToDoOwnershipTest {

    private static final int PRANAVS_TODO_ID = 5;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoRepository todoRepository;

    @BeforeEach
    void pranavOwnsTodoFive() {
        Todo pranavsTodo = new Todo(PRANAVS_TODO_ID, "Pranav", "Pranav's private todo",
                LocalDate.of(2030, 1, 1), false);

        when(todoRepository.findById(PRANAVS_TODO_ID)).thenReturn(Optional.of(pranavsTodo));
        when(todoRepository.findByIdAndUsername(PRANAVS_TODO_ID, "Pranav")).thenReturn(Optional.of(pranavsTodo));
    }

    // Attacks: Pran goes after Pranav's todo

    @Test
    @WithMockUser("Pran")
    void cannotDeleteAnotherUsersTodo() throws Exception {
        mockMvc.perform(post("/delete-todo").with(csrf()).param("id", "5"))
                .andExpect(status().isNotFound());

        verify(todoRepository, never()).delete(any());
        verify(todoRepository, never()).deleteById(anyInt());
    }

    @Test
    @WithMockUser("Pran")
    void cannotViewAnotherUsersTodo() throws Exception {
        mockMvc.perform(get("/update-todo").param("id", "5"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("Pran")
    void cannotOverwriteAnotherUsersTodoThroughUpdate() throws Exception {
        mockMvc.perform(post("/update-todo").with(csrf())
                        .param("id", "5")
                        .param("description", "Overwritten by Pran")
                        .param("targetDate", "2030-01-01"))
                .andExpect(status().isNotFound());

        verify(todoRepository, never()).save(any());
    }

    @Test
    @WithMockUser("Pran")
    void addFormIgnoresForgedIdAndUsername() throws Exception {
        mockMvc.perform(post("/add-todos").with(csrf())
                        .param("id", "5")
                        .param("username", "Pranav")
                        .param("description", "Overwritten by Pran")
                        .param("targetDate", "2030-01-01"))
                .andExpect(status().is3xxRedirection());

        Todo saved = capturedSave();
        assertEquals(0, saved.getId());
        assertEquals("Pran", saved.getUsername());
    }

    // Normal use: the owner can still manage their own todo

    @Test
    @WithMockUser("Pranav")
    void ownerCanDeleteTheirTodo() throws Exception {
        mockMvc.perform(post("/delete-todo").with(csrf()).param("id", "5"))
                .andExpect(status().is3xxRedirection());

        verify(todoRepository).delete(any(Todo.class));
    }

    @Test
    @WithMockUser("Pranav")
    void ownerCanViewTheirTodo() throws Exception {
        mockMvc.perform(get("/update-todo").param("id", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser("Pranav")
    void ownerCanUpdateTheirTodo() throws Exception {
        mockMvc.perform(post("/update-todo").with(csrf())
                        .param("id", "5")
                        .param("description", "Updated by Pranav")
                        .param("targetDate", "2030-01-01"))
                .andExpect(status().is3xxRedirection());

        Todo saved = capturedSave();
        assertEquals(PRANAVS_TODO_ID, saved.getId());
        assertEquals("Pranav", saved.getUsername());
        assertEquals("Updated by Pranav", saved.getDescription());
    }

    // Forged requests: another website uses the owner's login to send these

    @Test
    @WithMockUser("Pranav")
    void deleteRejectsGetRequests() throws Exception {
        mockMvc.perform(get("/delete-todo").param("id", "5"))
                .andExpect(status().isMethodNotAllowed());

        verify(todoRepository, never()).delete(any());
    }

    @Test
    @WithMockUser("Pranav")
    void deleteRejectsPostWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/delete-todo").param("id", "5"))
                .andExpect(status().isForbidden());

        verify(todoRepository, never()).delete(any());
    }

    @Test
    @WithMockUser("Pranav")
    void updateRejectsPostWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/update-todo")
                        .param("id", "5")
                        .param("description", "Forged by another website")
                        .param("targetDate", "2030-01-01"))
                .andExpect(status().isForbidden());

        verify(todoRepository, never()).save(any());
    }

    @Test
    @WithMockUser("Pranav")
    void addRejectsPostWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/add-todos")
                        .param("description", "Forged by another website")
                        .param("targetDate", "2030-01-01"))
                .andExpect(status().isForbidden());

        verify(todoRepository, never()).save(any());
    }

    private Todo capturedSave() {
        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).save(captor.capture());
        return captor.getValue();
    }
}
