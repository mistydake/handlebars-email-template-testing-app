package com.example.hbstester;

public class RenderResponse {
    private boolean success;
    private String html;
    private String cleanedPayload;
    private String errorMessage;
    private int    errorLine;
    private int    errorColumn;
    private String errorSource = "template";

    public static RenderResponse ok(String html, String cleanedPayload) {
        RenderResponse r = new RenderResponse();
        r.success        = true;
        r.html           = html;
        r.cleanedPayload = cleanedPayload;
        return r;
    }

    public static RenderResponse error(String message, int line, int column, String cleanedPayload) {
        RenderResponse r = new RenderResponse();
        r.success        = false;
        r.errorMessage   = message;
        r.errorLine      = line;
        r.errorColumn    = column;
        r.cleanedPayload = cleanedPayload;
        return r;
    }

    public RenderResponse withSource(String source) { this.errorSource = source; return this; }

    public boolean isSuccess()        { return success; }
    public String  getHtml()          { return html; }
    public String  getCleanedPayload(){ return cleanedPayload; }
    public String  getErrorMessage()  { return errorMessage; }
    public int     getErrorLine()     { return errorLine; }
    public int     getErrorColumn()   { return errorColumn; }
    public String  getErrorSource()   { return errorSource; }
}
