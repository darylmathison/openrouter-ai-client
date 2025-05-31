// Custom ChatGPT Client - Frontend JavaScript

class ChatGPTClient {
    constructor() {
        this.currentChatId = null;
        this.attachedFiles = [];
        this.apiBase = '/api';
        this.currentModel = 'gpt-3.5-turbo';
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadChatHistory();
        this.loadSavedPrompts();
        this.updateBalance();
        this.setupAutoResize();
    }

    setupEventListeners() {
        // Send message
        document.getElementById('send-btn').addEventListener('click', () => this.sendMessage());
        document.getElementById('message-input').addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });

        // Model selector
        document.getElementById('model-selector').addEventListener('change', (e) => {
            this.currentModel = e.target.value;
            this.updateCostEstimate();
        });

        // File attachment
        document.getElementById('attach-file-btn').addEventListener('click', () => {
            document.getElementById('file-input').click();
        });
        document.getElementById('file-input').addEventListener('change', (e) => this.handleFileAttachment(e));

        // New chat
        document.getElementById('new-chat-btn').addEventListener('click', () => this.newChat());

        // Export chat
        document.getElementById('export-chat-btn').addEventListener('click', () => this.exportChat());

        // Generate prompt
        document.getElementById('generate-prompt-btn').addEventListener('click', () => this.generatePrompt());

        // Saved prompts
        document.getElementById('new-prompt-btn').addEventListener('click', () => this.showPromptModal());
        document.getElementById('cancel-prompt').addEventListener('click', () => this.hidePromptModal());
        document.getElementById('prompt-form').addEventListener('submit', (e) => this.savePrompt(e));

        // Message input changes
        document.getElementById('message-input').addEventListener('input', () => this.updateSendButton());
    }

    setupAutoResize() {
        const textarea = document.getElementById('message-input');
        textarea.addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = Math.min(this.scrollHeight, 120) + 'px';
        });
    }

    async sendMessage() {
        const messageInput = document.getElementById('message-input');
        const message = messageInput.value.trim();

        if (!message && this.attachedFiles.length === 0) return;

        // Disable send button
        const sendBtn = document.getElementById('send-btn');
        sendBtn.disabled = true;
        sendBtn.textContent = 'Sending...';

        // Add user message to UI
        this.addMessageToUI('user', message);

        // Clear input
        messageInput.value = '';
        messageInput.style.height = 'auto';
        this.attachedFiles = [];
        this.updateAttachedFilesDisplay();
        this.updateSendButton();

        try {
            const requestBody = {
              messages: [{content: message, role: 'USER'}],
              model: this.currentModel,
              maxTokens: 1000,
              temperature: 0.7
            };


            // Use the correct endpoints from your ChatController
            const url = this.currentChatId ?`${this.apiBase}/chats/${this.currentChatId}/messages` :`${this.apiBase}/chats`;


            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to send message: ${response.status} - ${errorText}`);
            }

            const data = await response.json();

            // Update current chat ID if this was a new chat
            if (!this.currentChatId) {
                this.currentChatId = data.chatId;
                this.updateChatTitle();
            }

            // Add assistant response to UI
            this.addMessageToUI('assistant', data.content, data);

            // Update cost display
            this.updateCostDisplay(data.estimatedCost);

            // Reload chat history
            this.loadChatHistory();

        } catch (error) {
            console.error('Error sending message:', error);
            this.addMessageToUI('assistant', 'Sorry, there was an error processing your request. Please try again.');
        } finally {
            sendBtn.disabled = false;
            sendBtn.textContent = 'Send';
        }
    }

    addMessageToUI(role, content, metadata = {}) {
        const messagesContainer = document.getElementById('messages-container');
        const messageDiv = document.createElement('div');
        messageDiv.className = `message message-${role}`;

        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';

        if (role === 'assistant') {
            // Render markdown for assistant messages
            contentDiv.innerHTML = marked.parse(content);
            // Highlight code blocks
            contentDiv.querySelectorAll('pre code').forEach(block => {
                hljs.highlightElement(block);
            });
        } else {
            contentDiv.textContent = content;
        }

        messageDiv.appendChild(contentDiv);

        // Add metadata for assistant messages
        if (role === 'assistant' && metadata.tokenUsage) {
            const metaDiv = document.createElement('div');
            metaDiv.className = 'cost-info';
            metaDiv.textContent = `Tokens: ${metadata.tokenUsage.totalTokens} • Cost: $${metadata.estimatedCost?.toFixed(4) || '0.0000'}`;
            messageDiv.appendChild(metaDiv);
        }

        messagesContainer.appendChild(messageDiv);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    updateSendButton() {
        const messageInput = document.getElementById('message-input');
        const sendBtn = document.getElementById('send-btn');
        const hasMessage = messageInput.value.trim().length > 0;
        const hasFiles = this.attachedFiles.length > 0;

        sendBtn.disabled = !hasMessage && !hasFiles;
    }

    updateBalance() {
        document.getElementById('balance-display').textContent = '$10.00 remaining';
    }

    updateCostEstimate() {
        const costs = {
            'gpt-3.5-turbo': 0.001,
            'gpt-4': 0.03,
            'gpt-4-turbo': 0.01,
            'gpt-4o': 0.005,
            'gpt-4o-mini': 0.0001
        };

        const estimatedCost = costs[this.currentModel] || 0.001;
        document.getElementById('cost-estimate').textContent = `~$${estimatedCost.toFixed(4)}`;
    }

    updateCostDisplay(cost) {
        if (cost) {
            console.log(`Message cost: $${cost.toFixed(4)}`);
        }
    }

    updateChatTitle(title = 'New Chat') {
        document.getElementById('chat-title').textContent = title;
    }

    showPromptModal() {
        document.getElementById('prompt-modal').classList.remove('hidden');
    }

    hidePromptModal() {
        document.getElementById('prompt-modal').classList.add('hidden');
    }

    async savePrompt(e) {
        e.preventDefault();

        try {
            const formData = {
                name: document.getElementById('prompt-name').value,
                description: document.getElementById('prompt-description').value,
                prompt: document.getElementById('prompt-content').value,
                systemMessage: document.getElementById('prompt-system').value,
                modelName: document.getElementById('prompt-model').value,
                maxTokens: parseInt(document.getElementById('prompt-max-tokens').value) || 1000,
                temperature: parseFloat(document.getElementById('prompt-temperature').value) || 0.7,
                category: 'General'
            };

            const response = await fetch(`${this.apiBase}/prompts`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(formData)
            });

            if (!response.ok) {
                throw new Error('Failed to save prompt');
            }

            this.hidePromptModal();
            this.loadSavedPrompts();

            // Clear form
            document.getElementById('prompt-form').reset();

        } catch (error) {
            console.error('Error saving prompt:', error);
            alert('Failed to save prompt. Please try again.');
        }
    }

    updateAttachedFilesDisplay() {
        const container = document.getElementById('attached-files');
        container.innerHTML = '';

        this.attachedFiles.forEach((file, index) => {
            const fileDiv = document.createElement('div');
            fileDiv.className = 'attached-file';

            const fileName = document.createElement('span');
            fileName.textContent = file.name;

            const removeBtn = document.createElement('span');
            removeBtn.className = 'remove-file';
            removeBtn.textContent = '×';
            removeBtn.addEventListener('click', () => {
                this.attachedFiles.splice(index, 1);
                this.updateAttachedFilesDisplay();
                this.updateSendButton();
            });

            fileDiv.appendChild(fileName);
            fileDiv.appendChild(removeBtn);
            container.appendChild(fileDiv);
        });
    }

    handleFileAttachment(event) {
        const files = Array.from(event.target.files);
        this.attachedFiles.push(...files);
        this.updateAttachedFilesDisplay();
        this.updateSendButton();
    }

    async loadChatHistory() {
        try {
            console.log('Loading chat history...');
            const response = await fetch(`${this.apiBase}/chats`);

            if (!response.ok) {
                console.error('Failed to load chat history:', response.status, response.statusText);
                throw new Error(`Failed to load chat history: ${response.status}`);
            }

            const chats = await response.json();
            console.log('Loaded chats:', chats);

            const chatHistoryContainer = document.getElementById('chat-history');
            chatHistoryContainer.innerHTML = '';

            if (chats.length === 0) {
                chatHistoryContainer.innerHTML = '<div class="text-gray-500 text-sm p-2">No chats yet</div>';
                return;
            }

            chats.forEach(chat => {
                const chatItem = document.createElement('div');
                chatItem.className = `chat-item ${chat.id === this.currentChatId ? 'active' : ''}`;
                chatItem.textContent = chat.title;
                chatItem.addEventListener('click', () => this.loadChat(chat.id));
                chatHistoryContainer.appendChild(chatItem);
            });
        } catch (error) {
            console.error('Error loading chat history:', error);
            const chatHistoryContainer = document.getElementById('chat-history');
            chatHistoryContainer.innerHTML = '<div class="text-red-500 text-sm p-2">Error loading chats</div>';
        }
    }

    async loadSavedPrompts() {
        try {
            console.log('Loading saved prompts...');
            const response = await fetch(`${this.apiBase}/prompts`);

            if (!response.ok) {
                console.error('Failed to load saved prompts:', response.status, response.statusText);
                if (response.status === 404) {
                    console.log('Prompts endpoint not found, skipping...');
                    return;
                }
                throw new Error(`Failed to load saved prompts: ${response.status}`);
            }

            const prompts = await response.json();
            console.log('Loaded prompts:', prompts);

            const promptsContainer = document.getElementById('saved-prompts-list');
            promptsContainer.innerHTML = '';

            if (prompts.length === 0) {
                promptsContainer.innerHTML = '<div class="text-gray-500 text-sm p-2">No saved prompts</div>';
                return;
            }

            prompts.forEach(prompt => {
                const promptItem = document.createElement('div');
                promptItem.className = 'prompt-item';

                const nameSpan = document.createElement('span');
                nameSpan.textContent = prompt.name;
                nameSpan.title = prompt.description || prompt.name;

                const executeBtn = document.createElement('button');
                executeBtn.className = 'prompt-execute-btn';
                executeBtn.textContent = '▶';
                executeBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    this.executePrompt(prompt.id);
                });

                promptItem.appendChild(nameSpan);
                promptItem.appendChild(executeBtn);
                promptsContainer.appendChild(promptItem);
            });
        } catch (error) {
            console.error('Error loading saved prompts:', error);
            const promptsContainer = document.getElementById('saved-prompts-list');
            promptsContainer.innerHTML = '<div class="text-red-500 text-sm p-2">Error loading prompts</div>';
        }
    }

    async loadChat(chatId) {
        try {
            const response = await fetch(`${this.apiBase}/chats/${chatId}`);
            if (!response.ok) throw new Error('Failed to load chat');

            const chat = await response.json();
            this.currentChatId = chatId;

            // Clear current messages
            document.getElementById('messages-container').innerHTML = '';

            // Load messages
            if (chat.messages) {
                chat.messages.forEach(message => {
                    if (message.role !== 'SYSTEM') {
                        this.addMessageToUI(message.role.toLowerCase(), message.content);
                    }
                });
            }

            this.updateChatTitle(chat.title);
            this.loadChatHistory(); // Refresh to update active state
        } catch (error) {
            console.error('Error loading chat:', error);
        }
    }

    async executePrompt(promptId) {
    try {
        console.log('Executing prompt:', promptId);

        const response = await fetch(`${this.apiBase}/prompts/${promptId}/execute`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Failed to execute prompt: ${response.status} - ${errorText}`);
        }

        const data = await response.json();
        console.log('Prompt execution response:', data);

        // CRITICAL: Set the current chat ID from the response
        if (data.chatId) {
            this.currentChatId = data.chatId;
            console.log('Set currentChatId to:', this.currentChatId);
        } else {
            console.error('No chatId in response!', data);
        }

        // Clear existing messages and show the new conversation
        document.getElementById('messages-container').innerHTML = '';

        // Add the prompt content as user message
        if (data.generatedPrompt) {
            this.addMessageToUI('user', data.generatedPrompt);
        }

        // Add assistant response
        this.addMessageToUI('assistant', data.content, data);

        // Update cost display
        this.updateCostDisplay(data.estimatedCost);

        // Update chat title based on the prompt content
        const chatTitle = data.generatedPrompt ?
            this.generateChatTitle(data.generatedPrompt) :
            'Executed Prompt';
        this.updateChatTitle(chatTitle);

        // Reload chat history to show the new chat
        this.loadChatHistory();

        console.log('Prompt executed successfully, currentChatId:', this.currentChatId);

    } catch (error) {
        console.error('Error executing prompt:', error);
        alert('Failed to execute prompt: ' + error.message);
    }
}

    generateChatTitle(message) {
        if (!message) return 'New Chat';

        let title = message.trim();
        if (title.length > 50) {
            title = title.substring(0, 47) + "...";
        }
        return title;
    }

    newChat() {
        this.currentChatId = null;
        document.getElementById('messages-container').innerHTML = '';
        this.updateChatTitle('New Chat');
        this.loadChatHistory();
    }

    async exportChat() {
        // Check if there's a current chat
        if (!this.currentChatId) {
            alert('Please select a chat to export');
            return;
        }

        try {
            console.log('Exporting chat:', this.currentChatId);

            const response = await fetch(`${this.apiBase}/chats/${this.currentChatId}/export`);

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to export chat: ${response.status} - ${errorText}`);
            }

            const markdown = await response.text();

            // Create and download the file
            const blob = new Blob([markdown], { type: 'text/markdown' });
            const url = URL.createObjectURL(blob);

            const a = document.createElement('a');
            a.href = url;
            a.download = `chat-${this.currentChatId}.md`;
            a.style.display = 'none';

            // Add to DOM, click, and remove
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);

            // Clean up the URL object
            URL.revokeObjectURL(url);

            console.log('Chat exported successfully');

        } catch (error) {
            console.error('Error exporting chat:', error);
            alert('Failed to export chat: ' + error.message);
        }
    }

    async generatePrompt() {
        if (!this.currentChatId) return;

        try {
            const response = await fetch(`${this.apiBase}/chats/${this.currentChatId}/generate-prompt`, {
                method: 'POST'
            });

            if (!response.ok) throw new Error('Failed to generate prompt');

            const prompt = await response.text();

            // Show in a new window or modal
            const newWindow = window.open('', '_blank');
            newWindow.document.write(`
                <html>
                    <head><title>Generated Prompt</title></head>
                    <body style="font-family: Arial, sans-serif; padding: 20px; line-height: 1.6;">
                        <h2>Generated Prompt</h2>
                        <pre style="background: #f5f5f5; padding: 15px; border-radius: 5px; white-space: pre-wrap;">${prompt}</pre>
                    </body>
                </html>
            `);
        } catch (error) {
            console.error('Error generating prompt:', error);
        }
    }
}

// Initialize the app
const app = new ChatGPTClient();