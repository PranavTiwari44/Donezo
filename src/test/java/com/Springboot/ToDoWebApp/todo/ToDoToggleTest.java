package com.Springboot.ToDoWebApp.todo;

import com.Springboot.ToDoWebApp.security.SpringSecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the Done / Undo button. Todo 5 belongs to Pranav.
 */
@WebMvcTest(ToDoControllerJpa.class)
@Import(SpringSecurityConfiguration.class)
class ToDoToggleTest {

    private static final int PRANAVS_TODO_ID = 5;
    private static final String DESCRIPTION = "Pranav's private todo";
    private static final LocalDate TARGET_DATE = LocalDate.of(2030, 1, 1);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TodoRepository todoRepository;

    // Normal use

    @Test
    @WithMockUser("Pranav")
    void ownerCanMarkTodoAsDone() throws Exception {
        pranavOwnsTodoWithDoneSetTo(false);

        toggleWithValidToken().andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("list-todos"));

        assertTrue(capturedSave().isDone());
    }

    @Test
    @WithMockUser("Pranav")
    void ownerCanUndoADoneTodo() throws Exception {
        pranavOwnsTodoWithDoneSetTo(true);

        toggleWithValidToken().andExpect(status().is3xxRedirection());

        assertFalse(capturedSave().isDone());
    }

    @Test
    @WithMockUser("Pranav")
    void togglingOnlyChangesTheDoneField() throws Exception {
        pranavOwnsTodoWithDoneSetTo(false);

        toggleWithValidToken().andExpect(status().is3xxRedirection());

        Todo saved = capturedSave();
        assertEquals(PRANAVS_TODO_ID, saved.getId());
        assertEquals("Pranav", saved.getUsername());
        assertEquals(DESCRIPTION, saved.getDescription());
        assertEquals(TARGET_DATE, saved.getTargetDate());
    }

    // Attacks and bad requests

    @Test
    @WithMockUser("Pran")
    void cannotToggleAnotherUsersTodo() throws Exception {
        pranavOwnsTodoWithDoneSetTo(false);

        toggleWithValidToken().andExpect(status().isNotFound());

        verify(todoRepository, never()).save(any());
    }

    @Test
    @WithMockUser("Pranav")
    void togglingATodoThatDoesNotExistReturnsNotFound() throws Exception {
        mockMvc.perform(post("/toggle-todo").with(csrf()).param("id", "999"))
                .andExpect(status().isNotFound());

        verify(todoRepository, never()).save(any());
    }

    @Test
    @WithMockUser("Pranav")
    void toggleRejectsGetRequests() throws Exception {
        pranavOwnsTodoWithDoneSetTo(false);

        mockMvc.perform(get("/toggle-todo").param("id", "5"))
                .andExpect(status().isMethodNotAllowed());

        verify(todoRepository, never()).save(any());
    }

    @Test
    @WithMockUser("Pranav")
    void toggleRejectsPostWithoutCsrfToken() throws Exception {
        pranavOwnsTodoWithDoneSetTo(false);

        mockMvc.perform(post("/toggle-todo").param("id", "5"))
                .andExpect(status().isForbidden());

        verify(todoRepository, never()).save(any());
    }

    @Test
    void toggleRequiresLogin() throws Exception {
        pranavOwnsTodoWithDoneSetTo(false);

        toggleWithValidToken().andExpect(status().is3xxRedirection());

        verify(todoRepository, never()).save(any());
    }

    private void pranavOwnsTodoWithDoneSetTo(boolean done) {
        Todo todo = new Todo(PRANAVS_TODO_ID, "Pranav", DESCRIPTION, TARGET_DATE, done);

        when(todoRepository.findById(PRANAVS_TODO_ID)).thenReturn(Optional.of(todo));
        when(todoRepository.findByIdAndUsername(PRANAVS_TODO_ID, "Pranav")).thenReturn(Optional.of(todo));
    }

    private ResultActions toggleWithValidToken() throws Exception {
        return mockMvc.perform(post("/toggle-todo").with(csrf()).param("id", String.valueOf(PRANAVS_TODO_ID)));
    }

    private Todo capturedSave() {
        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).save(captor.capture());
        return captor.getValue();
    }
}
