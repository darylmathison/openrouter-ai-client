package com.chatgpt.client.service;

import com.chatgpt.client.model.Chat;
import com.chatgpt.client.model.Message;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MarkdownService {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd HH:mm:ss");
  private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(\\w+)?\\n([\\s\\S]*?)```");
  private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");

  public String formatAsMarkdown(String content) {
    if (content == null || content.trim().isEmpty()) {
      return content;
    }

    // Already properly formatted markdown
    return content;
  }

  public String convertChatToMarkdown(Chat chat) {
    StringBuilder markdown = new StringBuilder();

    // Chat header
    markdown.append("# ").append(chat.getTitle()).append("\n\n");
    markdown.append("**Created:** ").append(chat.getCreatedAt().format(FORMATTER)).append("\n");
    markdown.append("**Updated:** ").append(chat.getUpdatedAt().format(FORMATTER)).append("\n");

    if (chat.getTotalTokens() != null) {
      markdown.append("**Total Tokens:** ").append(chat.getTotalTokens()).append("\n");
    }

    if (chat.getEstimatedCost() != null) {
      markdown.append("**Estimated Cost:** $")
          .append(String.format("%.4f", chat.getEstimatedCost())).append("\n");
    }

    if (chat.getModelUsed() != null) {
      markdown.append("**Model:** ").append(chat.getModelUsed()).append("\n");
    }

    markdown.append("\n---\n\n");

    // Messages
    for (Message message : chat.getMessages()) {
      if (message.getRole() == Message.MessageRole.SYSTEM) {
        continue; // Skip system messages in export
      }

      String roleLabel =
          message.getRole() == Message.MessageRole.USER ? "**You:**" : "**Assistant:**";
      markdown.append(roleLabel).append("\n\n");

      // Process content based on whether it contains code
      String content = processContentForExport(message.getContent());
      markdown.append(content).append("\n\n");

      // Add timestamp
      markdown.append("*").append(message.getCreatedAt().format(FORMATTER)).append("*\n\n");
      markdown.append("---\n\n");
    }

    return markdown.toString();
  }

  public String generatePromptFromChat(Chat chat) {
    if (chat.getMessages().isEmpty()) {
      return "";
    }

    StringBuilder prompt = new StringBuilder();
    prompt.append("Create a conversation that would result in the following chat:\n\n");

    for (Message message : chat.getMessages()) {
      if (message.getRole() == Message.MessageRole.SYSTEM) {
        continue;
      }

      String role = message.getRole() == Message.MessageRole.USER ? "User" : "Assistant";
      prompt.append("**").append(role).append(":** ");
      prompt.append(message.getContent()).append("\n\n");
    }

    prompt.append(
        "Generate a prompt that would create a similar conversation with the same depth and style.");

    return prompt.toString();
  }

  private String processContentForExport(String content) {
    // Extract and handle code blocks separately
    return CODE_BLOCK_PATTERN.matcher(content).replaceAll(match -> {
      String language = match.group(1) != null ? match.group(1) : "";
      String code = match.group(2);

      // Check if this looks like a complete file
      if (isCompleteFile(code, language)) {
        return "```" + language + "\n" + code + "```";
      } else {
        return "```" + language + "\n" + code + "```";
      }
    });
  }

  private boolean isCompleteFile(String code, String language) {
    // Simple heuristics to determine if code represents a complete file
    if (language.equalsIgnoreCase("java")) {
      return code.contains("public class") || code.contains("package ");
    }
    if (language.equalsIgnoreCase("python")) {
      return code.contains("def ") && code.contains("if __name__");
    }
    if (language.equalsIgnoreCase("javascript") || language.equalsIgnoreCase("js")) {
      return code.contains("function ") || code.contains("const ") || code.contains("var ");
    }
    if (language.equalsIgnoreCase("json")) {
      return code.trim().startsWith("{") && code.trim().endsWith("}");
    }
    if (language.equalsIgnoreCase("csv")) {
      return code.contains(",") && code.split("\n").length > 1;
    }

    return false;
  }
}
