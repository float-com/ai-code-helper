package com.example.aicodehelper.ai;

import dev.langchain4j.service.SystemMessage;

public interface AiCodeHelperService {

    /**
     * 定义聊天接口方法，用于处理用户消息并生成AI响应
     *
     * 该方法使用@SystemMessage注解指定系统提示词模板，从resources目录下的
     * system-prompt.txt文件中读取系统级指令和行为准则，确保AI响应符合预设的
     * 人格特征、安全策略和业务规则。系统提示词在每次对话时都会作为上下文的一部分
     * 发送给AI模型，以引导其生成符合要求的回复。
     *
     * @param userMessage 用户输入的消息内容，不能为空
     * @return 返回AI模型生成的响应文本，包含对用户请求的理解和回答
     * @see SystemMessage 注解用于指定系统提示词的来源和配置
     */
    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(String userMessage);
}

