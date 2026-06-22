
import java.util.*;

public class HTTPRequest {
    private String url;
    private String method;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private String body;
    private int timeout; // in seconds 

    //. this requires Constructor Overloading to handle different combinations of parameters
    public HTTPRequest(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public void execute() {
        System.out.println("Executing request to " + url);
    }
}
