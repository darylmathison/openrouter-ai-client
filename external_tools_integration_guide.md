
# External Tools Integration Guide

## Overview

The Custom ChatGPT Client supports integration with external tools and services, allowing you to extend the AI's capabilities beyond text generation. This guide covers how to configure, integrate, and use external tools like CrewAI, custom APIs, and other services.

## Architecture

External tools are integrated through a flexible HTTP-based system that supports:
- REST API endpoints
- Various authentication methods
- Request/response templating
- Error handling and retry logic
- Asynchronous execution

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

There are two ways to use external tools in your chats:

##### Method 1: Using the @{{name}} Format

You can directly call an external tool in your message using the @{{name}} format:

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
- The tool will be executed and its output will be sent to the AI model

##### Method 2: Using CrewAI (Legacy Method)

You can also use CrewAI in your chats by mentioning it:

User: Use CrewAI to create a blog post about "The Future of AI in Healthcare"

AI: I'll use the CrewAI tool to generate a comprehensive blog post for you.

[Tool executes and returns content]

Here's the blog post created by the CrewAI research and writing team:

# The Future of AI in Healthcare

[Generated content appears here in markdown format]

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

### Tool Chaining

You can chain multiple tools together for complex workflows:

1. Sequential Execution:
   User: First, get the weather for New York, then use CrewAI to write a travel blog post based on the weather conditions.

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
