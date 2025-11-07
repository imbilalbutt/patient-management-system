import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class AuthIntegrationTest {

    @BeforeAll
    static void setUp(){
        RestAssured.baseURI = "http://localhost:4004"; // 4004 is address of api gateway
    }

    @Test
    public void shouldReturnOKWithValidToken(){
//        1. Arrange: it means you do any setup this test needs to work 100 percent of the time i.e., setting up data
//        2. Act: this is the code we write that actually triggers the thing that we are testing i.e., calling login endpoint
//        3. Assert: we assert the result on stage 2

        String loginPayload = """
                    {
                        "email":"test@test.com",
                        "password":"password123"
                    }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .body(loginPayload)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .extract().response();

        System.out.println("Generated token : " + response.jsonPath().getString("token"));
    }
}
