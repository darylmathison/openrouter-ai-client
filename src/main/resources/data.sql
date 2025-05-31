INSERT INTO external_tools (name, description, base_url, api_key, is_enabled, tool_type,
                            configuration, created_at, updated_at, last_used_at, usage_count)
VALUES ('Weather API', 'Get current weather information', 'https://api.openweathermap.org/data/2.5',
        'your-openweather-api-key-here', true, 'WEATHER',
        '{"endpoints": {"current": "/weather", "forecast": "/forecast"}, "units": "metric"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       ('News API', 'Fetch latest news articles', 'https://newsapi.org/v2',
        'your-news-api-key-here', true, 'NEWS',
        '{"endpoints": {"headlines": "/top-headlines", "everything": "/everything"}, "country": "us"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       ('Translation API', 'Translate text between languages',
        'https://api.mymemory.translated.net', NULL, true, 'TRANSLATION',
        '{"endpoints": {"translate": "/get"}, "free_tier": true}', CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP, NULL, 0),
       ('Stock API', 'Get stock market data', 'https://api.twelvedata.com',
        'your-stock-api-key-here', false, 'FINANCE',
        '{"endpoints": {"quote": "/quote", "time_series": "/time_series"}, "plan": "basic"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

-- Insert some sample saved prompts (matching saved_prompts schema)
INSERT INTO saved_prompts (name, prompt, description, system_message, model_name, max_tokens,
                           temperature, category, usage_count, created_at, updated_at, is_favorite,
                           tags)
VALUES ('Blog Writer',
        'Write a blog post about {{topic}} that is engaging and informative. Include an introduction, main points, and conclusion.',
        'Helps write engaging blog posts on any topic',
        'You are a professional content writer with expertise in creating engaging blog content. Write in a conversational tone that connects with readers.',
        'gpt-3.5-turbo', 1000, 0.7, 'Writing', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false,
        'blog,content,writing,article'),
       ('Code Reviewer',
        'Review this code and suggest improvements: {{code}}. Focus on best practices, performance, and readability.',
        'Reviews code for best practices and improvements',
        'You are a senior software engineer who specializes in code review and best practices. Provide constructive feedback and specific suggestions.',
        'gpt-4', 800, 0.3, 'Development', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true,
        'code,review,programming,development'),
       ('Email Assistant',
        'Write a professional email about {{subject}}. Make it clear, concise, and appropriate for {{recipient_type}}.',
        'Helps compose professional emails',
        'You are a professional communication expert who helps write clear and effective emails. Use appropriate tone and formatting.',
        'gpt-3.5-turbo', 500, 0.5, 'Communication', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false,
        'email,professional,business,communication'),
       ('Language Tutor',
        'Explain the grammar rule: {{rule}} in {{language}} and provide 3 practical examples with explanations.',
        'Helps with language learning and grammar',
        'You are a patient and knowledgeable language teacher who explains concepts clearly with practical examples.',
        'gpt-3.5-turbo', 600, 0.4, 'Education', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true,
        'language,learning,grammar,education'),
       ('Meeting Summarizer',
        'Summarize this meeting transcript: {{transcript}}. Include key decisions, action items, and next steps.',
        'Summarizes meeting notes and extracts action items',
        'You are an efficient executive assistant who excels at distilling meetings into clear, actionable summaries.',
        'gpt-3.5-turbo', 800, 0.2, 'Productivity', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false,
        'meeting,summary,productivity,business'),
       ('Creative Story Writer',
        'Write a {{genre}} story about {{prompt}}. Make it {{length}} and include {{elements}}.',
        'Creates creative stories in various genres',
        'You are a creative writer with a talent for storytelling. Create engaging narratives with vivid descriptions and compelling characters.',
        'gpt-4', 1500, 0.8, 'Creative', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false,
        'story,creative,fiction,writing');

-- Insert some sample chats (matching chats schema)
INSERT INTO chats (title, created_at, updated_at, total_tokens, estimated_cost, model_used)
VALUES ('Getting Started with AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 150, 0.003,
        'gpt-3.5-turbo'),
       ('Code Review Discussion', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 300, 0.006, 'gpt-4'),
       ('Weather App Planning', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 200, 0.004, 'gpt-3.5-turbo'),
       ('Email Template Creation', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 120, 0.0024,
        'gpt-3.5-turbo'),
       ('Language Learning Session', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 180, 0.0036,
        'gpt-3.5-turbo');

-- Insert some sample messages (matching messages schema)
-- Chat 1: Getting Started with AI
INSERT INTO messages (chat_id, content, role, created_at, tokens)
VALUES (1, 'Hello! Can you help me understand how AI works?', 'USER', CURRENT_TIMESTAMP, 12);
