package Builder.stepBuilder;

public class Main {
    public static void main(String[] args) {
        HttpRequest stepRequest = HttpRequest.HttpRequestStepBuilder.getBuilder()
                .withUrl("https://api.example.com/products")
                .withMethod("POST")
                .withHeader("Content-Type", "application/json")
                .withBody("{\"product\": \"Laptop\", \"price\": 49999}")
                .withTimeout(45)
                .build();

        stepRequest.execute();

        // HttpRequest.UrlStep builder =
        // HttpRequest.HttpRequestStepBuilder.getBuilder();
        // HttpRequest.MethodStep builder1 =
        // builder.withUrl("https://api.example.com/products");
        // if(builder == builder1) {
        // System.out.println("Same object");
        // } else {
        // System.out.println("Different object");
        // }

        //. will give us Error, since we are trying to call withMethod() on UrlStep, which is not allowed
        // HttpRequest.UrlStep builder2 = HttpRequest.HttpRequestStepBuilder.getBuilder().withUrl("GET");
    }
}