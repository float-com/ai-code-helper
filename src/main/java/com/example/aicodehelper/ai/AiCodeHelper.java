package com.example.aicodehelper.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AiCodeHelper {

    @Resource
    private ChatModel qwenChatModel;

    // 定义一个常量SYSTEM_MESSAGE，用于设置AI模型的系统消息
    private static final String SYSTEM_MESSAGE = """
            你是编程领域的小助手，帮助用户解答编程学习和求职面试相关的问题，并给出建议。重点关注 4 个方向：
            1. 规划清晰的编程学习路线
            2. 提供项目学习建议
            3. 给出程序员求职全流程指南（比如简历优化、投递技巧）
            4. 分享高频面试题和面试技巧
            请用简洁易懂的语言回答，助力用户高效学习与求职。
            """;
    /*
    * 简单对话 - 字符串类型消息
    * */
    // 定义一个公共方法chat，接收字符串类型的消息参数，返回处理后的字符串结果
    public String chat(String message){
        // 创建一个SystemMessage对象，用于设置AI模型的系统消息
        SystemMessage systemMessage = SystemMessage.from(SYSTEM_MESSAGE);
        // 将用户传入的消息字符串转换为UserMessage对象，这是与AI模型交互的标准消息格式
        UserMessage userMessage = UserMessage.from(message);
        // 调用注入的qwenChatModel实例的chat方法，传入用户消息并获取完整的聊天响应对象
        ChatResponse chatResponse = qwenChatModel.chat(systemMessage , userMessage);
        // 从返回的聊天响应中提取AI生成的消息内容，转换为AiMessage对象以便后续处理
        AiMessage aiMessage = chatResponse.aiMessage();
        // 使用日志记录器输出AI生成的完整消息内容到控制台，便于调试和监控
        log.info("AI 输出：" + aiMessage.toString());
        // 从AI消息对象中提取纯文本内容并作为方法的最终返回值
        return aiMessage.text();
    }

    /*
    * 简单对话 - 用户自定义消息
    * */
    // 定义一个公共方法chat，接收字符串类型的消息参数，返回处理后的字符串结果
    public String chatWithMessage(UserMessage userMessage){
        // 调用注入的qwenChatModel实例的chat方法，传入用户消息并获取完整的聊天响应对象
        ChatResponse chatResponse = qwenChatModel.chat(userMessage);
        // 从返回的聊天响应中提取AI生成的消息内容，转换为AiMessage对象以便后续处理
        AiMessage aiMessage = chatResponse.aiMessage();
        // 使用日志记录器输出AI生成的完整消息内容到控制台，便于调试和监控
        log.info("AI 输出："+aiMessage.toString());
        // 从AI消息对象中提取纯文本内容并作为方法的最终返回值
        return aiMessage.text();
    }
}
