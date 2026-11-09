package com.example.hbstester;

public class RenderRequest {
    private String template;
    private String payload;
    private String helpers;

    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getHelpers() { return helpers; }
    public void setHelpers(String helpers) { this.helpers = helpers; }
}
