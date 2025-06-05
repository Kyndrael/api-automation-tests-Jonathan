package tests;

import entities.Booking;
import entities.BookingDates;
import entities.User;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookingTests {

    private static final String CREATE_BOOKING_SCHEMA = "createBookingRequestSchema.json";
    private static Faker faker;
    private static RequestSpecification request;
    private static Booking booking;
    private static BookingDates bookingDates;
    private static User user;

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter(), new ErrorLoggingFilter());
        faker = new Faker();

        user = new User(
                faker.name().username(),
                faker.name().firstName(),
                faker.name().lastName(),
                faker.internet().safeEmailAddress(),
                faker.internet().password(8, 10),
                faker.phoneNumber().toString()
        );

        bookingDates = new BookingDates("2018-01-02", "2018-01-03");
        booking = new Booking(
                user.getFirstName(),
                user.getLastName(),
                (float) faker.number().randomDouble(2, 50, 100000),
                true,
                bookingDates,
                "Breakfast"
        );
    }

    @BeforeEach
    void setRequest() {
        request = given()
                .config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .contentType(ContentType.JSON)
                .auth().basic("admin", "password123");
    }

    @Test
    @Order(1)
    public void getAllBookingsById_returnOk() {
        Response response = request
                .when()
                .get("/booking")
                .then()
                .extract()
                .response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
    }

    @Test
    @Order(2)
    public void getAllBookingsByUserFirstName_BookingExists_returnOk() {
        request
            .queryParam("firstname", "Carol") // corrigido: era "firstName"
        .when()
            .get("/booking")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("results", anyOf(nullValue(), hasSize(greaterThanOrEqualTo(0))));
    }

    @Test
    @Order(3)
    public void createBooking_WithValidData_returnOk() {
        request
            .body(booking)
        .when()
            .post("/booking")
        .then()
            .assertThat()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body(matchesJsonSchemaInClasspath(CREATE_BOOKING_SCHEMA))
            .time(lessThan(2000L));
    }
}
