package com.Springboot.ToDoWebApp.todo;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Controller
@SessionAttributes("name")
public class ToDoControllerJpa {

    private TodoRepository todoRepository;

    public ToDoControllerJpa( TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    private Todo findOwnedTodoById(int id) {
        return todoRepository.findByIdAndUsername(id, getLoggedInUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @RequestMapping("list-todos")
    public String listAllToDos(ModelMap model) {
        String username = getLoggedInUsername();
        List<Todo> todos = todoRepository.findByUsername(username);
        model.addAttribute("todos", todos);
        return "listTodos";
    }

    @RequestMapping(value = "add-todos", method = RequestMethod.GET)
    public String showNewToDoPage(ModelMap model) {
        Todo todo = new Todo(0, getLoggedInUsername(), "", LocalDate.now().plusMonths(1), false);
        model.put("todo", todo);
        return "todo";
    }

    @RequestMapping(value = "add-todos", method = RequestMethod.POST)
    public String addNewToDo(ModelMap model, @Valid Todo todo, BindingResult result) {
        if(result.hasErrors()){
            return "todo";
        }
        todo.setId(0);
        todo.setUsername(getLoggedInUsername());
        todoRepository.save(todo);
        return "redirect:list-todos";
    }

    @RequestMapping(value = "delete-todo", method = RequestMethod.POST)
    public String deleteToDo(@RequestParam int id) {
       todoRepository.delete(findOwnedTodoById(id));
        return "redirect:list-todos";
    }

    @RequestMapping(value = "update-todo", method = RequestMethod.GET)
    public String showUpdateToDoPage(@RequestParam int id, ModelMap model) {
        Todo todo = findOwnedTodoById(id);
        model.addAttribute("todo", todo);
        return "todo";
    }

    @RequestMapping(value = "update-todo", method = RequestMethod.POST)
    public String updateToDo(ModelMap model, @Valid Todo todo, BindingResult result) {
        if(result.hasErrors()){
            return "todo";
        }
        findOwnedTodoById(todo.getId());
        todo.setUsername(getLoggedInUsername());
        todoRepository.save(todo);
        return "redirect:list-todos";
    }

    private String getLoggedInUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
