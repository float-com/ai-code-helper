package com.example.aicodehelper.ai;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI代码助手服务工厂配置类
 *
 * 该配置类负责创建和管理AI代码助手相关服务的Bean实例。
 * 使用Spring框架的@Configuration注解标记为配置类，允许定义Bean的创建逻辑。
 * 通过依赖注入获取ChatModel实例，并使用AiServices工具类创建具体的AI服务代理。
 *
 * 主要职责：
 * 1. 管理AiCodeHelperService的生命周期
 * 2. 将qwenChatModel与具体的服务接口进行绑定
 * 3. 提供服务实例的集中化创建和配置
 */
@Configuration
public class AiCodeHelperServiceFactory {
    /**
     * 注入Qwen聊天模型实例
     *
     * 该ChatModel实例由Spring容器管理，包含了与通义千问大模型交互所需的所有配置，
     * 如API密钥、端点地址、超时设置等。它作为AI服务的基础组件，被用于创建具体的
     * 服务代理实例，实现自然语言处理和代码辅助功能。
     */
    @Resource
    private ChatModel qwenChatModel;


    /**
     * 创建AI代码助手服务实例的Bean工厂方法
     *
     * 使用@Bean注解声明该方法返回的对象将被注册为Spring容器中的Bean。
     * 通过AiServices.builder()构建器模式创建AiCodeHelperService接口的动态代理实例，
     * 将服务接口与底层的qwenChatModel进行绑定，实现声明式的服务调用。
     * 这种模式简化了AI服务的使用，开发者只需定义接口即可获得完整的实现。
     * 同时配置了会话记忆功能，限制最多保存10条消息记录，防止内存无限增长。
     *
     * @return 返回配置好的AiCodeHelperService实例，该实例具有自然语言处理和代码辅助能力
     * @see AiServices#create(Class, ChatModel) 工具方法用于创建AI服务代理
     */
    @Bean
    public AiCodeHelperService aiCodeHelperService() {
        // 创建消息窗口聊天记忆实例，限制最大消息数为10条，用于维护对话上下文
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        // 使用AiServices构建器创建AiCodeHelperService代理实例
        AiCodeHelperService aiCodeHelperService = AiServices.builder(AiCodeHelperService.class)
                // 设置底层使用的聊天模型，即通义千问模型实例
                .chatModel(qwenChatModel)
                // 设置聊天记忆组件，用于保持对话的连贯性
                .chatMemory(chatMemory)
                // 构建并返回最终的服务代理实例
                .build();
        return aiCodeHelperService;
    }

}
