package com.example.aicodehelper.ai;

import dev.langchain4j.service.SystemMessage;

import java.util.List;

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

    /**
     * 基于系统提示词模板的报告生成功能
     *
     * 使用@SystemMessage注解指定系统级提示词模板，该模板定义了AI模型在生成响应时应遵循的行为准则和输出格式。
     * 系统提示词从resources目录下的system-prompt.txt文件中读取，确保所有生成的内容都符合预设的指导原则，
     * 即专注于编程学习路线规划、项目学习建议、求职全流程指南和面试技巧分享四大核心方向。
     * 此方法接收用户输入的消息作为上下文，结合系统提示词生成结构化的报告响应。
     *
     * @param userMessage 用户输入的原始消息内容，将作为生成报告的输入依据和上下文信息
     * @return 返回包含分析结果的Report对象，其中name字段表示报告主题，suggestionList包含具体建议列表
     * @see SystemMessage 注解用于指定系统提示词的来源文件路径和配置参数
     */
    @SystemMessage(fromResource = "system-prompt.txt")
    Report chatForReport(String userMessage);

    /**
     * 报告记录类，用于封装AI生成的建议报告数据
     *
     * 使用Java 14+的record语法创建不可变的数据载体类，自动提供构造函数、
     * getter方法、equals()、hashCode()和toString()等标准方法实现。
     * 该记录类包含两个核心字段：
     * - name: 报告的主题名称，标识建议的核心内容方向
     * - suggestionList: 字符串列表，包含具体的、可操作的建议项
     * 这种设计确保了报告数据的完整性和不可变性，适合用作方法返回值。
     *
     * @param name 报告的主题名称，不能为空
     * @param suggestionList 包含具体建议的字符串列表，每个元素代表一条独立建议
     */
    record Report(String name, List<String> suggestionList) {};


}

