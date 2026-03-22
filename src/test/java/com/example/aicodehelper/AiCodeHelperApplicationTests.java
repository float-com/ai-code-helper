package com.example.aicodehelper;

import com.example.aicodehelper.ai.AiCodeHelper;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiCodeHelperApplicationTests {

    @Resource
    private AiCodeHelper aiCodeHelper;

    @Test
    void chat() {
        aiCodeHelper.chat("你好，我是程序员float_com");
    }

    @Test
    void chatWithMessage() {
        UserMessage userMessage = UserMessage.from(
                TextContent.from("请为我描述这张图片"),
                // 注意当前模型可能不支持直接描述图片，这里仅作示例
                ImageContent.from("https://ts3.tc.mm.bing.net/th/id/OIP-C.xz4wolkJUmpqapIRIuYExQHaKY?rs=1&pid=ImgDetMain&o=7&rm=3"));
        aiCodeHelper.chatWithMessage(userMessage);
    }
}
