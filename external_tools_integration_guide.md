
# External Tools Integration Guide

## Overview

The Custom ChatGPT Client supports integration with external tools and services, allowing you to extend the AI's capabilities beyond text generation. This guide covers how to configure, integrate, and use external tools through the Model, Chat, Plugin (MCP) protocol, which provides a standardized way to interact with external tools through the chat interface.

## Architecture

External tools are integrated through the Model, Chat, Plugin (MCP) protocol, which provides a standardized way to interact with external tools through the chat interface. The MCP protocol supports:

- REST API endpoints
- Various authentication methods
- Request/response templating
- Error handling and retry logic
- Asynchronous execution
- Dynamic context expansion to accommodate information from external tools

### MCP Protocol

The MCP (Model, Chat, Plugin) protocol is a standardized way to interact with external tools through the chat interface. It provides a consistent format for tool calls and responses, making it easier to integrate new tools and services.

Key features of the MCP protocol:

1. **Standardized Tool Calls**: All external tool calls use the same format: `@{{toolName}} input`
2. **Structured Responses**: Tool responses include metadata and context information
3. **Dynamic Context Expansion**: The context can be dynamically expanded to accommodate information from external tools
4. **REST Server Wrappers**: REST servers can be wrapped with MCP to make them accessible through the chat interface

## Tool Configuration

### Basic Tool Setup

1. Access Tool Management:
   - Navigate to Settings > External Tools
   - Click "Add New Tool"

2. Basic Information:
   Name: Weather Service
   Description: Get current weather information for any city
   Endpoint URL: https://api.openweathermap.org/data/2.5/weather
   HTTP Method: GET

3. Authentication Configuration:
   Choose from supported authentication types:
   - None: No authentication required
   - API Key: Header-based API key authentication
   - Bearer Token: OAuth-style bearer token
   - Basic Auth: Username/password authentication

### Authentication Examples

#### API Key Authentication
{
  "apiKey": "your-api-key-here",
  "headerName": "X-API-Key"
}

#### Bearer Token Authentication
{
  "token": "your-bearer-token-here"
}

#### Basic Authentication
{
  "username": "your-username",
  "password": "your-password"
}

### Request Templates

Request templates define how to format API calls. Use placeholders for dynamic values:

#### GET Request Template
?q={{city}}&appid={{api_key}}&units=metric

#### POST Request Template
{
  "query": "{{user_input}}",
  "max_tokens": {{max_tokens}},
  "temperature": {{temperature}}
}

#### Advanced Template Example
{
  "model": "{{model_name}}",
  "messages": [
    {
      "role": "system",
      "content": "You are a helpful assistant specialized in {{domain}}"
    },
    {
      "role": "user", 
      "content": "{{user_query}}"
    }
  ],
  "parameters": {
    "max_tokens": {{max_tokens:1000}},
    "temperature": {{temperature:0.7}}
  }
}

### Response Mapping

Configure how to process API responses:

#### Simple Response Mapping
{
  "content_path": "choices[0].message.content",
  "error_path": "error.message"
}

#### Complex Response Mapping
{
  "success_indicator": "status",
  "success_value": "success",
  "data_path": "data.result",
  "error_path": "error.details",
  "transform": {
    "format": "markdown",
    "include_metadata": true
  }
}

## Specific Tool Integrations

### CrewAI Integration

CrewAI is a multi-agent framework that can be integrated as an external tool.

#### 1. CrewAI Service Setup

First, ensure your CrewAI service is running and accessible:

# crew_api.py
from flask import Flask, request, jsonify
from crewai import Agent, Task, Crew

app = Flask(__name__)

@app.route('/execute', methods=['POST'])
def execute_crew():
    data = request.json

    # Define agents
    researcher = Agent(
        role='Researcher',
        goal='Research the given topic thoroughly',
        backstory='You are an expert researcher with access to vast knowledge',
        verbose=True
    )

    writer = Agent(
        role='Writer', 
        goal='Write engaging content based on research',
        backstory='You are a skilled writer who creates compelling content',
        verbose=True
    )

    # Define tasks
    research_task = Task(
        description=f"Research: {data.get('topic', '')}",
        agent=researcher
    )

    write_task = Task(
        description=f"Write a {data.get('content_type', 'blog post')} about the research findings",
        agent=writer
    )

    # Create and run crew
    crew = Crew(
        agents=[researcher, writer],
        tasks=[research_task, write_task],
        verbose=True
    )

    result = crew.kickoff()

    return jsonify({
        'status': 'success',
        'result': str(result),
        'agents_used': ['researcher', 'writer']
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

#### 2. CrewAI Tool Configuration

Configure the CrewAI tool in your ChatGPT client:

Name: CrewAI Content Generator
Description: Multi-agent content generation using CrewAI
Endpoint URL: http://localhost:5000/execute
HTTP Method: POST
Authentication: None (or API Key if you add authentication)

Request Template:
{
  "topic": "{{topic}}",
  "content_type": "{{content_type:blog post}}",
  "tone": "{{tone:professional}}",
  "length": "{{length:medium}}"
}

Response Mapping:
{
  "content_path": "result",
  "status_path": "status",
  "metadata_path": "agents_used"
}

#### 3. Using External Tools in Chat

All external tool interactions are now handled through the MCP protocol, which provides a standardized way to interact with tools through the chat interface.

##### Using the MCP Protocol

You can call any external tool in your message using the @{{name}} format:

```
User: @{{Weather}} What's the weather like in New York today?

AI: I've checked the weather for New York:
Current temperature: 72°F
Conditions: Partly cloudy
Humidity: 65%
Wind: 8 mph NE
```

The format works as follows:
- Start with `@{{` followed by the exact name of the tool
- Close with `}}` 
- Add the input for the tool after the closing brackets
- The tool will be executed through the MCP protocol and its output will be sent to the AI model

##### MCP Response Format

When a tool is executed through the MCP protocol, the response includes metadata and context information:

```json
{
  "tool_name": "Weather",
  "tool_type": "API",
  "input": "What's the weather like in New York today?",
  "result": {
    "temperature": 72,
    "conditions": "Partly cloudy",
    "humidity": 65,
    "wind": "8 mph NE"
  },
  "mcp_version": "1.0"
}
```

This structured format makes it easier for the AI model to understand and use the information from the tool.

##### Using MCP REST Wrappers

You can also use REST servers through the MCP protocol by creating an MCP wrapper:

```
User: @{{MyRESTService}} Query the database for user information

AI: I've queried the database through the REST service:
User ID: 12345
Name: John Doe
Email: john.doe@example.com
Status: Active
```

The MCP wrapper handles the communication with the REST server and formats the response according to the MCP protocol.

### Weather API Integration

#### Tool Configuration
Name: OpenWeather API
Description: Get current weather and forecasts
Endpoint URL: https://api.openweathermap.org/data/2.5/weather
HTTP Method: GET
Authentication: API Key

Auth Config:
{
  "apiKey": "your-openweather-api-key",
  "headerName": "X-API-Key"
}

Request Template:
?q={{city}}&units={{units:metric}}&lang={{language:en}}

### Custom Business API Integration

#### Example: Customer Database API
Name: Customer Lookup
Description: Retrieve customer information from CRM
Endpoint URL: https://api.yourcompany.com/customers/search
HTTP Method: POST
Authentication: Bearer Token

Auth Config:
{
  "token": "your-jwt-token-here"
}

Request Template:
{
  "query": "{{customer_query}}",
  "fields": ["name", "email", "status", "last_contact"],
  "limit": {{limit:10}}
}

Response Mapping:
{
  "content_path": "customers",
  "error_path": "error.message",
  "transform": {
    "format": "table",
    "headers": ["Name", "Email", "Status", "Last Contact"]
  }
}

### Ollama Integration (Local AI Models)

#### Tool Configuration for Ollama
Name: Ollama Local AI
Description: Use local Ollama models for AI tasks
Endpoint URL: http://localhost:11434/api/generate
HTTP Method: POST
Authentication: None

Request Template:
{
  "model": "{{model:llama2}}",
  "prompt": "{{prompt}}",
  "stream": false,
  "options": {
    "temperature": {{temperature:0.7}},
    "top_p": {{top_p:0.9}}
  }
}

Response Mapping:
{
  "content_path": "response",
  "status_path": "done"
}

## Advanced Features

### Creating MCP Wrappers for REST Servers

You can create MCP wrappers for REST servers to make them accessible through the MCP protocol in the chat interface. This allows you to use any REST API as an external tool in your chats.

#### Using the API

To create an MCP wrapper for a REST server, use the following API endpoint:

```
POST /api/tools/mcp-wrapper?restServerUrl=https://api.example.com&restServerName=MyRESTService
```

Parameters:
- `restServerUrl`: The URL of the REST server
- `restServerName`: The name to use for the REST server in the chat interface

Response:
```json
{
  "id": 123,
  "name": "MyRESTService",
  "description": "MCP wrapper for REST server: https://api.example.com",
  "endpointUrl": "https://api.example.com",
  "httpMethod": "POST",
  "authType": "NONE",
  "requestTemplate": "{\"query\": \"{{input}}\", \"mcp_enabled\": true}",
  "responseMapping": "{\"extract\": \"/result\"}",
  "isActive": true,
  "toolType": "MCP_REST_WRAPPER",
  "isMcpEnabled": true
}
```

#### Listing MCP Wrappers

To list all MCP wrappers, use the following API endpoint:

```
GET /api/tools/mcp-wrappers
```

Response:
```json
[
  {
    "id": 123,
    "name": "MyRESTService",
    "description": "MCP wrapper for REST server: https://api.example.com",
    "endpointUrl": "https://api.example.com",
    "httpMethod": "POST",
    "authType": "NONE",
    "requestTemplate": "{\"query\": \"{{input}}\", \"mcp_enabled\": true}",
    "responseMapping": "{\"extract\": \"/result\"}",
    "isActive": true,
    "toolType": "MCP_REST_WRAPPER",
    "isMcpEnabled": true
  }
]
```

### Tool Chaining

You can chain multiple tools together for complex workflows:

1. Sequential Execution:
   User: First, get the weather for New York, then use the travel planner to suggest activities based on the weather conditions.

2. Conditional Logic:
   User: Check the customer status in the CRM. If they're a premium customer, use the premium content generator tool.

### Error Handling

Configure robust error handling for your tools:

#### Retry Configuration
{
  "retry_attempts": 3,
  "retry_delay": 1000,
  "timeout": 30000,
  "fallback_message": "Tool temporarily unavailable. Please try again later."
}

#### Error Response Mapping
{
  "error_indicators": ["error", "failure", "exception"],
  "error_message_path": "error.message",
  "error_code_path": "error.code",
  "friendly_errors": {
    "401": "Authentication failed. Please check your credentials.",
    "429": "Rate limit exceeded. Please wait before trying again.",
    "500": "Service temporarily unavailable."
  }
}

### Security Best Practices

1. API Key Management:
   - Store API keys securely (environment variables)
   - Rotate keys regularly
   - Use minimum required permissions

2. Request Validation:
   - Validate all input parameters
   - Sanitize user inputs
   - Implement rate limiting

3. Response Handling:
   - Don't expose sensitive data in responses
   - Log security events
   - Monitor for unusual patterns

### Monitoring and Logging

#### Tool Performance Metrics
- Response times
- Success/failure rates
- Usage patterns
- Cost per execution

#### Logging Configuration
{
  "log_requests": true,
  "log_responses": true,
  "log_errors": true,
  "log_performance": true,
  "sensitive_fields": ["api_key", "password", "token"]
}

## Troubleshooting

### Common Issues

1. Authentication Failures:
   - Verify API credentials
   - Check token expiration
   - Confirm authentication method

2. Connection Timeouts:
   - Increase timeout values
   - Check network connectivity
   - Verify endpoint availability

3. Invalid Responses:
   - Review response mapping configuration
   - Check API documentation for changes
   - Validate request format

4. Rate Limiting:
   - Implement exponential backoff
   - Monitor usage quotas
   - Consider caching responses

### Debugging Tools

1. Request Inspection:
   - Use browser dev tools
   - Check application logs
   - Enable debug mode

2. Response Analysis:
   - Validate JSON structure
   - Check response headers
   - Monitor error patterns

3. Performance Testing:
   - Measure response times
   - Test with various loads
   - Monitor resource usage

## Best Practices

### Tool Design

1. Clear Purpose:
   - Each tool should have a specific function
   - Avoid overly complex multi-purpose tools
   - Document tool capabilities clearly

2. Consistent Interface:
   - Use standard HTTP methods appropriately
   - Follow REST API conventions
   - Maintain consistent response formats

3. Error Resilience:
   - Implement proper error handling
   - Provide meaningful error messages
   - Plan for service degradation

### Performance Optimization

1. Caching:
   - Cache frequently requested data
   - Implement intelligent cache invalidation
   - Use appropriate cache TTL values

2. Async Processing:
   - Use asynchronous calls for long-running operations
   - Implement proper timeout handling
   - Provide progress indicators

3. Resource Management:
   - Monitor API quotas
   - Implement request queuing
   - Optimize payload sizes

### User Experience

1. Clear Feedback:
   - Show tool execution status
   - Provide progress indicators
   - Display meaningful results

2. Error Communication:
   - Use user-friendly error messages
   - Suggest corrective actions
   - Provide fallback options

3. Documentation:
   - Document tool usage patterns
   - Provide example requests
   - Maintain up-to-date help text

## Examples and Templates

### Tool Configuration Templates

#### REST API Template
{
  "name": "{{tool_name}}",
  "description": "{{tool_description}}",
  "endpoint_url": "{{api_endpoint}}",
  "http_method": "{{method}}",
  "auth_type": "{{auth_type}}",
  "auth_config": {
    "apiKey": "{{api_key}}",
    "headerName": "{{header_name}}"
  },
  "request_template": "{{request_template}}",
  "response_mapping": {
    "content_path": "{{content_path}}",
    "error_path": "{{error_path}}"
  }
}

#### GraphQL API Template
{
  "name": "GraphQL Query Tool",
  "description": "Execute GraphQL queries",
  "endpoint_url": "https://api.example.com/graphql",
  "http_method": "POST",
  "auth_type": "BEARER_TOKEN",
  "request_template": {
    "query": "{{graphql_query}}",
    "variables": "{{variables}}"
  }
}

This completes the External Tools Integration Guide. The system is designed to be flexible and extensible, allowing you to integrate virtually any HTTP-based service or API into your ChatGPT client workflow.
