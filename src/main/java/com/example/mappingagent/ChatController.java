package com.example.mappingagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
        System.out.println(this.chatClient);
    }

    @GetMapping("")
    public String home() {
        return chatClient.prompt()
                .user("Tell me a dad joke about Technology")
                .call()
                .content();
    }

}
