package jku.pr_dke.mymakrochef.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    private final ChatClient chatClient;
    private final JdbcTemplate jdbc;

    public TestController(ChatClient.Builder chatClientBuilder, JdbcTemplate jdbc) {
        this.chatClient = chatClientBuilder.build();
        this.jdbc = jdbc;
    }

    // 1) DB only: http://localhost:8080/test/db
    @GetMapping("/db")
    public String testDb() {
        return jdbc.queryForObject("SELECT version()", String.class);
    }

    // 2) AI only: http://localhost:8080/test/ai?prompt=Tell me a joke about broccoli
    @GetMapping("/ai")
    public String testAi(@RequestParam(defaultValue = "Say hello in one sentence.") String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    // 3) Combined: asks the AI, stores prompt + answer in the DB, returns all stored rows
    //    http://localhost:8080/test/ai-db?prompt=How much protein is in 100g of chicken breast?
    @GetMapping("/ai-db")
    public String testAiAndDb(
            @RequestParam(defaultValue = "Find a recipe with beef and eggs and atleast 10 g protein!") String prompt) {


        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        System.out.println(answer);
        return answer;

    }
}