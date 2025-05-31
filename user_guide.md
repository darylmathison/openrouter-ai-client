# Custom ChatGPT Client - User Guide

## Overview

The Custom ChatGPT Client is a web-based application that provides a ChatGPT-like interface for interacting with OpenAI's API. It offers advanced features like saved prompts, external tool integration, file attachments, and comprehensive chat management.

## Features

### 1. **Chat Interface**
- ChatGPT-like interface for natural conversations
- Real-time responses from OpenAI models
- Context preservation throughout conversations
- Multiple model support (GPT-3.5, GPT-4, etc.)

### 2. **Saved Prompts (Agent-like Functionality)**
- Save frequently used prompts for one-click execution
- Customize model parameters (temperature, max tokens, etc.)
- Track usage statistics for each prompt
- Search and organize your prompt library

### 3. **Chat Management**
- Searchable chat history
- Export chats to markdown format
- Generate prompts from existing conversations
- Cost tracking and estimation

### 4. **External Tool Integration**
- Connect to CrewAI and other external services
- Configure HTTP endpoints with authentication
- Custom request/response mapping
- Support for various HTTP methods (GET, POST, PUT, DELETE)

### 5. **File Attachments**
- Attach files to chat messages
- Support for various file formats
- Content extraction and processing

### 6. **Cost Management**
- Real-time cost estimation
- Monthly cost tracking
- Per-message cost breakdown
- Budget monitoring

## Getting Started

### Prerequisites
- Java 21 or higher
- OpenAI API key
- (Optional) PostgreSQL database for production

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd chatgpt-client
   ```

2. **Set up environment variables**
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
```