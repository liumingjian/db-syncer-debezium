package com.dbsyncer.metadata.controller;

import com.dbsyncer.metadata.dto.TaskCreateRequest;
import com.dbsyncer.metadata.entity.DatabaseType;
import com.dbsyncer.metadata.repository.MigrationTaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MigrationTaskRepository taskRepository;

    private TaskCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();

        createRequest = new TaskCreateRequest();
        createRequest.setTaskName("integration-test-task");
        createRequest.setDescription("Integration test task");
        createRequest.setSourceType(DatabaseType.MYSQL);
        createRequest.setSourceHost("localhost");
        createRequest.setSourcePort(3306);
        createRequest.setSourceDatabase("sourcedb");
        createRequest.setSourceUsername("root");
        createRequest.setSourcePassword("password");
        createRequest.setTargetType(DatabaseType.POSTGRESQL);
        createRequest.setTargetHost("localhost");
        createRequest.setTargetPort(5432);
        createRequest.setTargetDatabase("targetdb");
        createRequest.setTargetUsername("postgres");
        createRequest.setTargetPassword("password");
    }

    @Test
    @DisplayName("Should create task via REST API")
    void shouldCreateTaskViaRestApi() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskName").value("integration-test-task"))
                .andExpect(jsonPath("$.sourceType").value("MYSQL"))
                .andExpect(jsonPath("$.targetType").value("POSTGRESQL"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("Should get task by ID")
    void shouldGetTaskById() throws Exception {
        // Create task first
        String response = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String taskId = objectMapper.readTree(response).get("id").asText();

        // Get task
        mockMvc.perform(get("/api/v1/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.taskName").value("integration-test-task"));
    }

    @Test
    @DisplayName("Should get task by name")
    void shouldGetTaskByName() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tasks/name/{taskName}", "integration-test-task"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskName").value("integration-test-task"));
    }

    @Test
    @DisplayName("Should return 404 when task not found")
    void shouldReturn404WhenTaskNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/{taskId}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").containsString("not found"));
    }

    @Test
    @DisplayName("Should return 409 when task already exists")
    void shouldReturn409WhenTaskAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").containsString("already exists"));
    }

    @Test
    @DisplayName("Should return validation errors for invalid request")
    void shouldReturnValidationErrorsForInvalidRequest() throws Exception {
        TaskCreateRequest invalidRequest = new TaskCreateRequest();
        // Missing required fields

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    @DisplayName("Should get all tasks with pagination")
    void shouldGetAllTasksWithPagination() throws Exception {
        // Create multiple tasks
        for (int i = 0; i < 3; i++) {
            createRequest.setTaskName("task-" + i);
            mockMvc.perform(post("/api/v1/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/tasks")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("Should delete task")
    void shouldDeleteTask() throws Exception {
        String response = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String taskId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(delete("/api/v1/tasks/{taskId}", taskId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tasks/{taskId}", taskId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should start task")
    void shouldStartTask() throws Exception {
        String response = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String taskId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(post("/api/v1/tasks/{taskId}/start", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STARTING"));
    }

    @Test
    @DisplayName("Should search tasks by name")
    void shouldSearchTasksByName() throws Exception {
        createRequest.setTaskName("mysql-to-postgres");
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tasks/search")
                        .param("query", "mysql"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].taskName").value("mysql-to-postgres"));
    }
}
