# Custom ChatGPT Client

A comprehensive custom ChatGPT client built with Jakarta EE, Spring WebFlux, and modern web technologies. This application provides a ChatGPT-like interface with advanced features for content creation, interview preparation, and AI-powered workflow automation.

## ✨ Features

### Core Functionality
- **ChatGPT-like Interface**: Clean, responsive UI similar to the official ChatGPT interface
- **OpenAI API Integration**: Direct integration with OpenAI's API for cost-effective usage
- **Context Preservation**: Maintains conversation context throughout chat sessions
- **Multiple Model Support**: GPT-3.5, GPT-4, GPT-4 Turbo, and more

### Advanced Features
- **One-Click Saved Prompts**: Create and execute reusable prompts (agents) with a single click
- **External Tool Integration**: Connect CrewAI, custom APIs, and other external services
- **File Attachments**: Attach and process various file types in conversations
- **Cost Tracking**: Real-time cost estimation and monthly budget monitoring
- **Chat Export**: Export conversations to markdown format with proper formatting
- **Prompt Generation**: Generate prompts that recreate existing conversations
- **Searchable History**: Find past conversations by content or title
- **Ollama Support**: Integration with local Ollama models for privacy

### Technical Features
- **Reactive Architecture**: Built with Spring WebFlux for high performance
- **Object-Oriented Design**: Clean, maintainable code structure
- **Comprehensive Testing**: Full test coverage for all major components
- **Markdown Support**: All outputs formatted in markdown with syntax highlighting
- **Real-time Updates**: Responsive UI with immediate feedback

## 🛠️ Technology Stack

- **Backend**: Java 21, Spring Boot 3.x, Spring WebFlux
- **Framework**: Jakarta EE with Jakarta imports
- **Database**: H2 (development), PostgreSQL (production)
- **AI Integration**: OpenAI Java SDK, Ollama support
- **Frontend**: Vanilla JavaScript, Tailwind CSS, Marked.js, Highlight.js
- **Build Tool**: Gradle
- **Testing**: JUnit 5, Mockito, Reactor Test

## 🚀 Quick Start

### Prerequisites
- Java 21 or higher
- Valid OpenAI API key
- (Optional) Ollama for local AI models

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd custom-chatgpt-client
   ```

2. **Set environment variables**
   ```bash
   export OPENAI_API_KEY=your-openai-api-key-here
   ```

3. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

4. **Access the application**
   Open your browser and navigate to `http://localhost:8080`

### Configuration

The application can be configured via `application.yml`:

```yaml
openai:
  api:
    key: ${OPENAI_API_KEY}
  default:
    model: gpt-3.5-turbo
    max-tokens: 1000
    temperature: 0.7

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/chatgpt_client
    username: your-username
    password: your-password
```