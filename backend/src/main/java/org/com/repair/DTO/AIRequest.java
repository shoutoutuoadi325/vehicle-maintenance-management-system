package org.com.repair.DTO;

import java.util.List;

/**
 * AI API请求DTO
 */
public class AIRequest {
    private String model;
    private List<Message> messages;
    
    public AIRequest() {
    }
    
    public AIRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
    
    public static class Message {
        private String role;
        private String content;
        
        public Message() {
        }
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
}
