// Custom ChatGPT Client - Frontend JavaScript

class ChatGPTClient {
    constructor() {
        this.currentChatId = null;
        this.attachedFiles = [];
        this.apiBase = '/api';
        this.currentModel = 'openai/gpt-3.5-turbo';
        this.availableModels = [];
        this.selectedModels = [];
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadChatHistory();
        this.loadSavedPrompts();
        this.updateBalance();
        this.setupAutoResize();
        this.loadModels();
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

        // Settings
        document.getElementById('settings-btn').addEventListener('click', () => this.showSettingsModal());
        document.getElementById('cancel-settings').addEventListener('click', () => this.hideSettingsModal());
        document.getElementById('save-settings').addEventListener('click', () => this.saveSettings());

        // Settings tabs
        document.querySelectorAll('.settings-tab-btn').forEach(btn => {
            btn.addEventListener('click', (e) => this.switchSettingsTab(e.target.dataset.tab));
        });

        // External tools
        document.getElementById('add-tool-btn').addEventListener('click', () => this.showToolForm());
        document.getElementById('cancel-tool').addEventListener('click', () => this.hideToolForm());
        document.getElementById('save-tool').addEventListener('click', () => this.saveTool());
        document.getElementById('tool-auth-type').addEventListener('change', () => this.updateAuthConfigFields());

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

        // Create header with role and actions
        const headerDiv = document.createElement('div');
        headerDiv.className = 'message-header flex justify-between items-center mb-2';

        // Role indicator
        const roleSpan = document.createElement('span');
        roleSpan.className = 'text-xs font-medium text-gray-500';
        roleSpan.textContent = role.charAt(0).toUpperCase() + role.slice(1);
        headerDiv.appendChild(roleSpan);

        // Action buttons
        const actionsDiv = document.createElement('div');
        actionsDiv.className = 'message-actions flex space-x-2';

        // Only add save/update buttons to user messages
        if (role === 'user') {
            // Save as prompt button
            const saveBtn = document.createElement('button');
            saveBtn.className = 'text-xs text-blue-600 hover:text-blue-800';
            saveBtn.textContent = 'Save as Prompt';
            saveBtn.addEventListener('click', () => this.showPromptModal(content, role));
            actionsDiv.appendChild(saveBtn);
        }

        headerDiv.appendChild(actionsDiv);
        messageDiv.appendChild(headerDiv);

        // Message content
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
            metaDiv.className = 'cost-info mt-2';
            metaDiv.textContent = `Tokens: ${metadata.tokenUsage.totalTokens} • Cost: $${metadata.estimatedCost?.toFixed(4) || '0.0000'}`;

            // Add model info
            if (metadata.model) {
                metaDiv.textContent += ` • Model: ${this.formatModelName(metadata.model)}`;
            }

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
            'openai/gpt-3.5-turbo': 0.001,
            'openai/gpt-4': 0.03,
            'openai/gpt-4-turbo': 0.01,
            'openai/gpt-4o': 0.005,
            'openai/gpt-4o-mini': 0.0001,
            'anthropic/claude-3-opus': 0.03,
            'anthropic/claude-3-sonnet': 0.01,
            'google/gemini-pro': 0.001
        };

        const estimatedCost = costs[this.currentModel] || 0.001;
        document.getElementById('cost-estimate').textContent = `~$${estimatedCost.toFixed(4)}`;
    }

    async loadModels() {
        try {
            // Load selected models
            const selectedResponse = await fetch(`${this.apiBase}/models/selected`);
            if (selectedResponse.ok) {
                this.selectedModels = await selectedResponse.json();
                console.log('Loaded selected models:', this.selectedModels);
                this.updateModelSelector();
            }

            // Load available models
            const availableResponse = await fetch(`${this.apiBase}/models/available`);
            if (availableResponse.ok) {
                this.availableModels = await availableResponse.json();
                console.log('Loaded available models:', this.availableModels);
            }
        } catch (error) {
            console.error('Error loading models:', error);
        }
    }

    async loadExternalTools() {
        try {
            const response = await fetch(`${this.apiBase}/tools`);
            if (response.ok) {
                const tools = await response.json();
                console.log('Loaded external tools:', tools);
                this.displayExternalTools(tools);
            }
        } catch (error) {
            console.error('Error loading external tools:', error);
            document.getElementById('tools-list').innerHTML = 
                '<div class="text-red-500">Error loading tools. Please try again.</div>';
        }
    }

    displayExternalTools(tools) {
        const container = document.getElementById('tools-list');
        container.innerHTML = '';

        if (tools.length === 0) {
            container.innerHTML = '<div class="text-gray-500">No external tools configured.</div>';
            return;
        }

        tools.forEach(tool => {
            const toolDiv = document.createElement('div');
            toolDiv.className = 'tool-item border rounded p-3';

            const header = document.createElement('div');
            header.className = 'flex justify-between items-center';

            const title = document.createElement('h5');
            title.className = 'font-medium';
            title.textContent = tool.name;

            const actions = document.createElement('div');
            actions.className = 'flex space-x-2';

            const editBtn = document.createElement('button');
            editBtn.className = 'text-blue-600 hover:text-blue-800 text-sm';
            editBtn.textContent = 'Edit';
            editBtn.addEventListener('click', () => this.editTool(tool));

            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'text-red-600 hover:text-red-800 text-sm';
            deleteBtn.textContent = 'Delete';
            deleteBtn.addEventListener('click', () => this.deleteTool(tool.id));

            actions.appendChild(editBtn);
            actions.appendChild(deleteBtn);

            header.appendChild(title);
            header.appendChild(actions);

            const description = document.createElement('p');
            description.className = 'text-sm text-gray-600 mt-1';
            description.textContent = tool.description;

            const details = document.createElement('div');
            details.className = 'text-xs text-gray-500 mt-2';
            details.textContent = `${tool.httpMethod} ${tool.endpointUrl}`;

            toolDiv.appendChild(header);
            toolDiv.appendChild(description);
            toolDiv.appendChild(details);

            container.appendChild(toolDiv);
        });
    }

    switchSettingsTab(tabId) {
        // Hide all tabs
        document.querySelectorAll('.settings-tab').forEach(tab => {
            tab.classList.add('hidden');
        });

        // Remove active class from all tab buttons
        document.querySelectorAll('.settings-tab-btn').forEach(btn => {
            btn.classList.remove('active');
            btn.classList.remove('border-blue-600');
            btn.classList.add('border-transparent');
        });

        // Show selected tab
        document.getElementById(tabId).classList.remove('hidden');

        // Add active class to selected tab button
        document.querySelector(`.settings-tab-btn[data-tab="${tabId}"]`).classList.add('active');
        document.querySelector(`.settings-tab-btn[data-tab="${tabId}"]`).classList.remove('border-transparent');
        document.querySelector(`.settings-tab-btn[data-tab="${tabId}"]`).classList.add('border-blue-600');

        // Load data for the tab if needed
        if (tabId === 'tools-tab') {
            this.loadExternalTools();
        }
    }

    updateModelSelector() {
        const selector = document.getElementById('model-selector');
        selector.innerHTML = '';

        // Add selected models to the dropdown
        this.selectedModels.forEach(model => {
            const option = document.createElement('option');
            option.value = model;
            option.textContent = this.formatModelName(model);
            selector.appendChild(option);
        });

        // Set current model to first in list if it's not in the selected models
        if (!this.selectedModels.includes(this.currentModel) && this.selectedModels.length > 0) {
            this.currentModel = this.selectedModels[0];
            this.updateCostEstimate();
        }
    }

    formatModelName(modelId) {
        // Convert "provider/model-name" to a more readable format
        // e.g., "openai/gpt-4" -> "OpenAI GPT-4"
        if (!modelId.includes('/')) return modelId;

        const [provider, model] = modelId.split('/');
        return `${provider.charAt(0).toUpperCase() + provider.slice(1)} ${model.toUpperCase()}`;
    }

    updateCostDisplay(cost) {
        if (cost) {
            console.log(`Message cost: $${cost.toFixed(4)}`);
        }
    }

    updateChatTitle(title = 'New Chat') {
        document.getElementById('chat-title').textContent = title;
    }

    showPromptModal(messageContent = '', messageRole = '', existingPromptId = null) {
        // Populate the form with message content if provided
        if (messageContent) {
            document.getElementById('prompt-content').value = messageContent;
        }

        // Set the prompt model dropdown options
        const modelSelect = document.getElementById('prompt-model');
        modelSelect.innerHTML = '';

        this.selectedModels.forEach(model => {
            const option = document.createElement('option');
            option.value = model;
            option.textContent = this.formatModelName(model);
            modelSelect.appendChild(option);
        });

        // Set current model as selected
        if (this.selectedModels.includes(this.currentModel)) {
            modelSelect.value = this.currentModel;
        }

        // Store the existing prompt ID if we're updating
        this.editingPromptId = existingPromptId;

        // Update modal title based on whether we're creating or updating
        document.querySelector('#prompt-modal h3').textContent = 
            existingPromptId ? 'Update Prompt' : 'Save Prompt';

        // Show the modal
        document.getElementById('prompt-modal').classList.remove('hidden');
    }

    hidePromptModal() {
        document.getElementById('prompt-modal').classList.add('hidden');
    }

    showSettingsModal() {
        // Load available models into the settings modal
        this.populateModelSelection();
        document.getElementById('settings-modal').classList.remove('hidden');

        // Default to models tab
        this.switchSettingsTab('models-tab');
    }

    hideSettingsModal() {
        document.getElementById('settings-modal').classList.add('hidden');
        this.hideToolForm();
    }

    showToolForm(tool = null) {
        // Reset form
        document.getElementById('tool-name').value = '';
        document.getElementById('tool-description').value = '';
        document.getElementById('tool-endpoint').value = '';
        document.getElementById('tool-method').value = 'GET';
        document.getElementById('tool-auth-type').value = 'NONE';
        document.getElementById('tool-request-template').value = '';
        document.getElementById('tool-response-mapping').value = '';

        // If editing an existing tool, populate the form
        if (tool) {
            this.editingToolId = tool.id;
            document.getElementById('tool-name').value = tool.name;
            document.getElementById('tool-description').value = tool.description;
            document.getElementById('tool-endpoint').value = tool.endpointUrl;
            document.getElementById('tool-method').value = tool.httpMethod;
            document.getElementById('tool-auth-type').value = tool.authType;
            document.getElementById('tool-request-template').value = tool.requestTemplate || '';
            document.getElementById('tool-response-mapping').value = tool.responseMapping || '';

            // Update auth config fields
            this.updateAuthConfigFields(tool.authConfig);
        } else {
            this.editingToolId = null;
            this.updateAuthConfigFields();
        }

        // Show the form
        document.getElementById('tool-form').classList.remove('hidden');
    }

    hideToolForm() {
        document.getElementById('tool-form').classList.add('hidden');
        this.editingToolId = null;
    }

    updateAuthConfigFields(authConfig = null) {
        const authType = document.getElementById('tool-auth-type').value;
        const container = document.getElementById('auth-config-container');
        container.innerHTML = '';

        if (authType === 'NONE') {
            container.classList.add('hidden');
            return;
        }

        container.classList.remove('hidden');

        let authConfigObj = {};
        if (authConfig) {
            try {
                authConfigObj = JSON.parse(authConfig);
            } catch (e) {
                console.error('Failed to parse auth config:', e);
            }
        }

        switch (authType) {
            case 'API_KEY':
                // API Key field
                const apiKeyDiv = document.createElement('div');
                const apiKeyLabel = document.createElement('label');
                apiKeyLabel.className = 'block text-sm font-medium text-gray-700';
                apiKeyLabel.textContent = 'API Key';

                const apiKeyInput = document.createElement('input');
                apiKeyInput.type = 'text';
                apiKeyInput.id = 'tool-api-key';
                apiKeyInput.className = 'mt-1 w-full p-2 border rounded';
                apiKeyInput.value = authConfigObj.apiKey || '';

                apiKeyDiv.appendChild(apiKeyLabel);
                apiKeyDiv.appendChild(apiKeyInput);

                // Header name field
                const headerDiv = document.createElement('div');
                const headerLabel = document.createElement('label');
                headerLabel.className = 'block text-sm font-medium text-gray-700 mt-2';
                headerLabel.textContent = 'Header Name';

                const headerInput = document.createElement('input');
                headerInput.type = 'text';
                headerInput.id = 'tool-header-name';
                headerInput.className = 'mt-1 w-full p-2 border rounded';
                headerInput.value = authConfigObj.headerName || 'X-API-Key';

                headerDiv.appendChild(headerLabel);
                headerDiv.appendChild(headerInput);

                container.appendChild(apiKeyDiv);
                container.appendChild(headerDiv);
                break;

            case 'BEARER_TOKEN':
                const tokenDiv = document.createElement('div');
                const tokenLabel = document.createElement('label');
                tokenLabel.className = 'block text-sm font-medium text-gray-700';
                tokenLabel.textContent = 'Bearer Token';

                const tokenInput = document.createElement('input');
                tokenInput.type = 'text';
                tokenInput.id = 'tool-bearer-token';
                tokenInput.className = 'mt-1 w-full p-2 border rounded';
                tokenInput.value = authConfigObj.token || '';

                tokenDiv.appendChild(tokenLabel);
                tokenDiv.appendChild(tokenInput);
                container.appendChild(tokenDiv);
                break;

            case 'BASIC_AUTH':
                // Username field
                const usernameDiv = document.createElement('div');
                const usernameLabel = document.createElement('label');
                usernameLabel.className = 'block text-sm font-medium text-gray-700';
                usernameLabel.textContent = 'Username';

                const usernameInput = document.createElement('input');
                usernameInput.type = 'text';
                usernameInput.id = 'tool-username';
                usernameInput.className = 'mt-1 w-full p-2 border rounded';
                usernameInput.value = authConfigObj.username || '';

                usernameDiv.appendChild(usernameLabel);
                usernameDiv.appendChild(usernameInput);

                // Password field
                const passwordDiv = document.createElement('div');
                const passwordLabel = document.createElement('label');
                passwordLabel.className = 'block text-sm font-medium text-gray-700 mt-2';
                passwordLabel.textContent = 'Password';

                const passwordInput = document.createElement('input');
                passwordInput.type = 'password';
                passwordInput.id = 'tool-password';
                passwordInput.className = 'mt-1 w-full p-2 border rounded';
                passwordInput.value = authConfigObj.password || '';

                passwordDiv.appendChild(passwordLabel);
                passwordDiv.appendChild(passwordInput);

                container.appendChild(usernameDiv);
                container.appendChild(passwordDiv);
                break;
        }
    }

    populateModelSelection() {
        const container = document.getElementById('model-selection');
        container.innerHTML = '';

        // Create checkboxes for each available model
        this.availableModels.forEach(model => {
            const div = document.createElement('div');
            div.className = 'flex items-center';

            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.id = `model-${model}`;
            checkbox.value = model;
            checkbox.className = 'model-checkbox mr-2';
            checkbox.checked = this.selectedModels.includes(model);

            // Limit selection to 5 models
            checkbox.addEventListener('change', () => {
                const checkedBoxes = document.querySelectorAll('.model-checkbox:checked');
                if (checkedBoxes.length > 5) {
                    checkbox.checked = false;
                    document.getElementById('model-selection-error').classList.remove('hidden');
                } else {
                    document.getElementById('model-selection-error').classList.add('hidden');
                }
            });

            const label = document.createElement('label');
            label.htmlFor = `model-${model}`;
            label.textContent = this.formatModelName(model);

            div.appendChild(checkbox);
            div.appendChild(label);
            container.appendChild(div);
        });
    }

    async saveSettings() {
        try {
            // Get selected models
            const checkedBoxes = document.querySelectorAll('.model-checkbox:checked');
            if (checkedBoxes.length === 0) {
                alert('Please select at least one model.');
                return;
            }

            if (checkedBoxes.length > 5) {
                document.getElementById('model-selection-error').classList.remove('hidden');
                return;
            }

            const selectedModels = Array.from(checkedBoxes).map(cb => cb.value);

            // Save selected models
            const response = await fetch(`${this.apiBase}/models/selected`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(selectedModels)
            });

            if (!response.ok) {
                throw new Error('Failed to save selected models');
            }

            // Update local state
            this.selectedModels = selectedModels;
            this.updateModelSelector();

            // Close modal
            this.hideSettingsModal();

        } catch (error) {
            console.error('Error saving settings:', error);
            alert('Failed to save settings. Please try again.');
        }
    }

    async saveTool() {
        try {
            // Validate required fields
            const name = document.getElementById('tool-name').value.trim();
            const description = document.getElementById('tool-description').value.trim();
            const endpointUrl = document.getElementById('tool-endpoint').value.trim();

            if (!name || !endpointUrl) {
                alert('Name and Endpoint URL are required.');
                return;
            }

            // Build auth config based on selected auth type
            const authType = document.getElementById('tool-auth-type').value;
            let authConfig = null;

            if (authType !== 'NONE') {
                switch (authType) {
                    case 'API_KEY':
                        const apiKey = document.getElementById('tool-api-key').value.trim();
                        const headerName = document.getElementById('tool-header-name').value.trim();
                        if (!apiKey) {
                            alert('API Key is required.');
                            return;
                        }
                        authConfig = JSON.stringify({
                            apiKey: apiKey,
                            headerName: headerName || 'X-API-Key'
                        });
                        break;

                    case 'BEARER_TOKEN':
                        const token = document.getElementById('tool-bearer-token').value.trim();
                        if (!token) {
                            alert('Bearer Token is required.');
                            return;
                        }
                        authConfig = JSON.stringify({
                            token: token
                        });
                        break;

                    case 'BASIC_AUTH':
                        const username = document.getElementById('tool-username').value.trim();
                        const password = document.getElementById('tool-password').value.trim();
                        if (!username || !password) {
                            alert('Username and Password are required.');
                            return;
                        }
                        authConfig = JSON.stringify({
                            username: username,
                            password: password
                        });
                        break;
                }
            }

            // Build tool data
            const toolData = {
                name: name,
                description: description,
                endpointUrl: endpointUrl,
                httpMethod: document.getElementById('tool-method').value,
                authType: authType,
                authConfig: authConfig,
                requestTemplate: document.getElementById('tool-request-template').value.trim(),
                responseMapping: document.getElementById('tool-response-mapping').value.trim(),
                isActive: true,
                toolType: 'API'
            };

            // Determine if we're creating or updating
            let url = `${this.apiBase}/tools`;
            let method = 'POST';

            if (this.editingToolId) {
                url = `${this.apiBase}/tools/${this.editingToolId}`;
                method = 'PUT';
                toolData.id = this.editingToolId;
            }

            // Send request
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(toolData)
            });

            if (!response.ok) {
                throw new Error(`Failed to ${this.editingToolId ? 'update' : 'save'} tool`);
            }

            // Hide form and reload tools
            this.hideToolForm();
            this.loadExternalTools();

        } catch (error) {
            console.error('Error saving tool:', error);
            alert('Failed to save tool. Please try again.');
        }
    }

    async deleteTool(toolId) {
        if (!confirm('Are you sure you want to delete this tool?')) {
            return;
        }

        try {
            const response = await fetch(`${this.apiBase}/tools/${toolId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error('Failed to delete tool');
            }

            // Reload tools
            this.loadExternalTools();

        } catch (error) {
            console.error('Error deleting tool:', error);
            alert('Failed to delete tool. Please try again.');
        }
    }

    editTool(tool) {
        this.showToolForm(tool);
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

            let url = `${this.apiBase}/prompts`;
            let method = 'POST';

            // If we're updating an existing prompt
            if (this.editingPromptId) {
                url = `${this.apiBase}/prompts/${this.editingPromptId}`;
                method = 'PUT';
            }

            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(formData)
            });

            if (!response.ok) {
                throw new Error(`Failed to ${this.editingPromptId ? 'update' : 'save'} prompt`);
            }

            this.hidePromptModal();
            this.loadSavedPrompts();

            // Clear form and reset editing state
            document.getElementById('prompt-form').reset();
            this.editingPromptId = null;

            // Show success message
            alert(`Prompt ${this.editingPromptId ? 'updated' : 'saved'} successfully!`);

        } catch (error) {
            console.error(`Error ${this.editingPromptId ? 'updating' : 'saving'} prompt:`, error);
            alert(`Failed to ${this.editingPromptId ? 'update' : 'save'} prompt. Please try again.`);
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
