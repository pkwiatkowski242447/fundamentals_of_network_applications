package pl.pas.gr3.cinema.api;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.pas.gr3.cinema.TestConstants;
import pl.pas.gr3.cinema.consts.model.UserConstants;
import pl.pas.gr3.cinema.exceptions.repositories.UserRepositoryException;
import pl.pas.gr3.cinema.model.users.Admin;
import pl.pas.gr3.cinema.model.users.Client;
import pl.pas.gr3.cinema.model.users.Staff;
import pl.pas.gr3.cinema.repositories.implementations.UserRepository;
import pl.pas.gr3.dto.auth.UserInputDTO;
import pl.pas.gr3.dto.output.MovieDTO;
import pl.pas.gr3.dto.input.MovieInputDTO;
import pl.pas.gr3.cinema.exceptions.services.crud.movie.MovieServiceCreateException;
import pl.pas.gr3.cinema.exceptions.services.crud.movie.MovieServiceDeleteException;
import pl.pas.gr3.cinema.exceptions.services.crud.movie.MovieServiceReadException;
import pl.pas.gr3.cinema.services.implementations.MovieService;
import pl.pas.gr3.cinema.model.Movie;
import pl.pas.gr3.cinema.repositories.implementations.MovieRepository;

import java.net.URL;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MovieControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(MovieControllerTest.class);

    private static MovieRepository movieRepository;
    private static UserRepository userRepository;
    private static MovieService movieService;
    private static PasswordEncoder passwordEncoder;

    private Movie movieNo1;
    private Movie movieNo2;
    private Client clientUser;
    private Admin adminUser;
    private Staff staffUser;
    private static String passwordNotHashed;

    @BeforeAll
    public static void init() {
        movieRepository = new MovieRepository(TestConstants.databaseName);
        userRepository = new UserRepository(TestConstants.databaseName);
        movieService = new MovieService(movieRepository);

        passwordEncoder = new BCryptPasswordEncoder();

        ClassLoader classLoader = AuthenticationControllerTest.class.getClassLoader();
        URL resourceURL = classLoader.getResource("pas-truststore.jks");

        RestAssured.config = RestAssuredConfig.newConfig().sslConfig(
                new SSLConfig().trustStore(resourceURL.getPath(), "password")
                        .and()
                        .port(8000)
                        .and()
                        .allowAllHostnames()
        );

        passwordNotHashed = "password";
    }

    @BeforeEach
    public void initializeSampleData() {
        this.clearCollection();

        try {
            clientUser = userRepository.createClient("ClientLoginX", passwordEncoder.encode(passwordNotHashed));
            staffUser = userRepository.createStaff("StaffLoginX", passwordEncoder.encode(passwordNotHashed));
            adminUser = userRepository.createAdmin("AdminLoginX", passwordEncoder.encode(passwordNotHashed));
        } catch (UserRepositoryException exception) {
            throw new RuntimeException("Could not create sample users with userRepository object.", exception);
        }

        try {
            movieNo1 = movieService.create("ExampleMovieTitleNo1", 38.45, 5, 40);
            movieNo2 = movieService.create("ExampleMovieTitleNo1", 32.60, 2, 75);
        } catch (MovieServiceCreateException exception) {
            throw new RuntimeException("Could not create sample movies with movieRepository object.", exception);
        }
    }

    @AfterEach
    public void destroySampleData() {
        this.clearCollection();
    }

    private void clearCollection() {
        try {
            List<Client> listOfClients = userRepository.findAllClients();
            for (Client client : listOfClients) {
                userRepository.delete(client.getUserID(), UserConstants.CLIENT_DISCRIMINATOR);
            }

            List<Admin> listOfAdmins = userRepository.findAllAdmins();
            for (Admin admin : listOfAdmins) {
                userRepository.delete(admin.getUserID(), UserConstants.ADMIN_DISCRIMINATOR);
            }

            List<Staff> listOfStaffs = userRepository.findAllStaffs();
            for (Staff staff : listOfStaffs) {
                userRepository.delete(staff.getUserID(), UserConstants.STAFF_DISCRIMINATOR);
            }
        } catch (UserRepositoryException exception) {
            throw new RuntimeException("Could not delete sample users with userRepository object.", exception);
        }

        try {
            List<Movie> listOfMovies = movieService.findAll();
            for (Movie movie : listOfMovies) {
                movieService.delete(movie.getMovieID());
            }
        } catch (MovieServiceReadException | MovieServiceDeleteException exception) {
            throw new RuntimeException("Could not delete sample movies with movieRepository object.", exception);
        }
    }

    @AfterAll
    public static void destroy() {
        movieRepository.close();
    }

    // Create tests

    @Test
    public void movieControllerCreateMovieAsAnUnauthenticatedUserTestNegative() {
        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(403);
    }

    @Test
    public void movieControllerCreateMovieAsAnAuthenticatedClientTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(clientUser.getUserLogin(), passwordNotHashed), TestConstants.clientLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(403);
    }

    @Test
    public void movieControllerCreateMovieAsAnAuthenticatedStaffTestPositive() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(201);

        MovieDTO movieDTO = response.getBody().as(MovieDTO.class);

        assertNotNull(movieDTO);
        assertEquals(movieTitle, movieDTO.getMovieTitle());
        assertEquals(movieBasePrice, movieDTO.getMovieBasePrice());
        assertEquals(scrRoomNumber, movieDTO.getScrRoomNumber());
        assertEquals(numberOfAvailableSeats, movieDTO.getNumberOfAvailableSeats());
    }

    @Test
    public void movieControllerCreateMovieAsAnAuthenticatedAdminTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(adminUser.getUserLogin(), passwordNotHashed), TestConstants.adminLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(403);
    }

    @Test
    public void movieControllerCreateMovieWithNullMovieTitleAsAnAuthenticatedStaffTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = null;
        double movieBasePrice = 50.25;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerCreateMovieWithEmptyMovieTitleAsAnAuthenticatedStaffTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerCreateMovieWithMovieTitleTooShortAsAnAuthenticatedStaffTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerCreateMovieWithMovieTitleTooLongAsAnAuthenticatedStaffTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "ddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfd";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerCreateMovieWithMovieTitleLengthEqualTo1AsAnAuthenticatedStaffTestPositive() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "d";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(201);

        MovieDTO movieDTO = response.getBody().as(MovieDTO.class);

        assertNotNull(movieDTO);
        assertEquals(movieTitle, movieDTO.getMovieTitle());
        assertEquals(movieBasePrice, movieDTO.getMovieBasePrice());
        assertEquals(scrRoomNumber, movieDTO.getScrRoomNumber());
        assertEquals(numberOfAvailableSeats, movieDTO.getNumberOfAvailableSeats());
    }

    @Test
    public void movieControllerCreateMovieWithMovieTitleLengthEqualTo150AsAnAuthenticatedStaffTestPositive() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "ddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddf";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(201);

        MovieDTO movieDTO = response.getBody().as(MovieDTO.class);

        assertNotNull(movieDTO);
        assertEquals(movieTitle, movieDTO.getMovieTitle());
        assertEquals(movieBasePrice, movieDTO.getMovieBasePrice());
        assertEquals(scrRoomNumber, movieDTO.getScrRoomNumber());
        assertEquals(numberOfAvailableSeats, movieDTO.getNumberOfAvailableSeats());
    }

    @Test
    public void movieControllerCreateMovieWithNegativeMovieBasePriceAsAnAuthenticatedStaffTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = -1;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerCreateMovieWithMovieBasePriceTooHighAsAnAuthenticatedStaffTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 101.00;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerCreateMovieWithMovieBasePriceEqualTo0AsAnAuthenticatedStaffTestPositive() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 0;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(201);

        MovieDTO movieDTO = response.getBody().as(MovieDTO.class);

        assertNotNull(movieDTO);
        assertEquals(movieTitle, movieDTO.getMovieTitle());
        assertEquals(movieBasePrice, movieDTO.getMovieBasePrice());
        assertEquals(scrRoomNumber, movieDTO.getScrRoomNumber());
        assertEquals(numberOfAvailableSeats, movieDTO.getNumberOfAvailableSeats());
    }

    @Test
    public void movieControllerCreateMovieWithMovieBasePriceEqualTo100AsAnAuthenticatedStaffTestPositive() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 100.00;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(201);

        MovieDTO movieDTO = response.getBody().as(MovieDTO.class);

        assertNotNull(movieDTO);
        assertEquals(movieTitle, movieDTO.getMovieTitle());
        assertEquals(movieBasePrice, movieDTO.getMovieBasePrice());
        assertEquals(scrRoomNumber, movieDTO.getScrRoomNumber());
        assertEquals(numberOfAvailableSeats, movieDTO.getNumberOfAvailableSeats());
    }

    @Test
    public void movieControllerCreateMovieWithScreeningRoomNumberEqualTo0AsAnAuthenticatedStaffTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 0;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerCreateMovieWithScreeningRoomNumberTooHighAsAnAuthenticatedStaffTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 31;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerCreateMovieWithScreeningRoomNumberEqualTo1AsAnAuthenticatedStaffTestPositive() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 1;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(201);

        MovieDTO movieDTO = response.getBody().as(MovieDTO.class);

        assertNotNull(movieDTO);
        assertEquals(movieTitle, movieDTO.getMovieTitle());
        assertEquals(movieBasePrice, movieDTO.getMovieBasePrice());
        assertEquals(scrRoomNumber, movieDTO.getScrRoomNumber());
        assertEquals(numberOfAvailableSeats, movieDTO.getNumberOfAvailableSeats());
    }

    @Test
    public void movieControllerCreateMovieWithScreeningRoomNumberEqualTo30TestPositive() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 30;
        int numberOfAvailableSeats = 37;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(201);

        MovieDTO movieDTO = response.getBody().as(MovieDTO.class);

        assertNotNull(movieDTO);
        assertEquals(movieTitle, movieDTO.getMovieTitle());
        assertEquals(movieBasePrice, movieDTO.getMovieBasePrice());
        assertEquals(scrRoomNumber, movieDTO.getScrRoomNumber());
        assertEquals(numberOfAvailableSeats, movieDTO.getNumberOfAvailableSeats());
    }

    @Test
    public void movieControllerCreateMovieWithNegativeNumberOfAvailableSeatsTestNegative() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = -1;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerCreateMovieWithNumberOfAvailableSeatsTooHighTestNegative() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 121;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerCreateMovieWithNumberOfAvailableSeatsEqualTo0TestPositive() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 0;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(201);

        MovieDTO movieDTO = response.getBody().as(MovieDTO.class);

        assertNotNull(movieDTO);
        assertEquals(movieTitle, movieDTO.getMovieTitle());
        assertEquals(movieBasePrice, movieDTO.getMovieBasePrice());
        assertEquals(scrRoomNumber, movieDTO.getScrRoomNumber());
        assertEquals(numberOfAvailableSeats, movieDTO.getNumberOfAvailableSeats());
    }

    @Test
    public void movieControllerCreateMovieWithNumberOfAvailableSeatsEqualTo120TestPositive() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String movieTitle = "OtherExampleMovieTitleNo1";
        double movieBasePrice = 50.25;
        int scrRoomNumber = 11;
        int numberOfAvailableSeats = 120;

        MovieInputDTO movieInputDTO = new MovieInputDTO(movieTitle, movieBasePrice, scrRoomNumber, numberOfAvailableSeats);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(movieInputDTO);

        Response response = requestSpecification.post(TestConstants.moviesURL);
        logger.debug("Response: " + response.asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(201);

        MovieDTO movieDTO = response.getBody().as(MovieDTO.class);

        assertNotNull(movieDTO);
        assertEquals(movieTitle, movieDTO.getMovieTitle());
        assertEquals(movieBasePrice, movieDTO.getMovieBasePrice());
        assertEquals(scrRoomNumber, movieDTO.getScrRoomNumber());
        assertEquals(numberOfAvailableSeats, movieDTO.getNumberOfAvailableSeats());
    }

    // Read tests

    @Test
    public void movieControllerFindMovieByIDAsAnUnauthenticatedUserTestNegative() {
        UUID searchedMovieID = movieNo1.getMovieID();
        String path = TestConstants.moviesURL + "/" + searchedMovieID;

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.accept(ContentType.JSON);

        Response response = requestSpecification.get(path);
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(403);
    }

    @Test
    public void movieControllerFindMovieByIDAsAnAuthenticatedClientTestPositive() {
        String accessToken = this.loginToAccount(new UserInputDTO(clientUser.getUserLogin(), passwordNotHashed), TestConstants.clientLoginURL);

        UUID searchedMovieID = movieNo1.getMovieID();
        String path = TestConstants.moviesURL + "/" + searchedMovieID;

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);

        Response response = requestSpecification.get(path);
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(200);

        MovieDTO movieDTO = response.getBody().as(MovieDTO.class);

        assertEquals(movieNo1.getMovieID(), movieDTO.getMovieID());
        assertEquals(movieNo1.getMovieTitle(), movieDTO.getMovieTitle());
        assertEquals(movieNo1.getScrRoomNumber(), movieDTO.getScrRoomNumber());
        assertEquals(movieNo1.getNumberOfAvailableSeats(), movieDTO.getNumberOfAvailableSeats());
    }

    @Test
    public void movieControllerFindMovieByIDAsAnAuthenticatedStaffTestPositive() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        UUID searchedMovieID = movieNo1.getMovieID();
        String path = TestConstants.moviesURL + "/" + searchedMovieID;

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);

        Response response = requestSpecification.get(path);
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(200);

        MovieDTO movieDTO = response.getBody().as(MovieDTO.class);

        assertEquals(movieNo1.getMovieID(), movieDTO.getMovieID());
        assertEquals(movieNo1.getMovieTitle(), movieDTO.getMovieTitle());
        assertEquals(movieNo1.getScrRoomNumber(), movieDTO.getScrRoomNumber());
        assertEquals(movieNo1.getNumberOfAvailableSeats(), movieDTO.getNumberOfAvailableSeats());
    }

    @Test
    public void movieControllerFindMovieByIDAsAnAuthenticatedStaffTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(adminUser.getUserLogin(), passwordNotHashed), TestConstants.adminLoginURL);

        UUID searchedMovieID = movieNo1.getMovieID();
        String path = TestConstants.moviesURL + "/" + searchedMovieID;

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);

        Response response = requestSpecification.get(path);
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(403);
    }

    @Test
    public void movieControllerFindMovieByIDThatIsNotInTheDatabaseAsAnAuthenticatedClientTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(clientUser.getUserLogin(), passwordNotHashed), TestConstants.clientLoginURL);

        UUID searchedMovieID = UUID.randomUUID();
        String path = TestConstants.moviesURL + "/" + searchedMovieID;

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);

        Response response = requestSpecification.get(path);
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(404);
    }

    @Test
    public void movieControllerFindMovieByIDThatIsNotInTheDatabaseAsAnAuthenticatedStaffTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        UUID searchedMovieID = UUID.randomUUID();
        String path = TestConstants.moviesURL + "/" + searchedMovieID;

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);

        Response response = requestSpecification.get(path);
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(404);
    }

    @Test
    public void movieControllerFindAllMoviesAsAnUnauthenticatedUserTestNegative() {
        String path = TestConstants.moviesURL + "/all";
        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(path);
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(403);
    }

    @Test
    public void movieControllerFindAllMoviesAsAnAuthenticatedClientTestPositive() {
        String accessToken = this.loginToAccount(new UserInputDTO(clientUser.getUserLogin(), passwordNotHashed), TestConstants.clientLoginURL);

        String path = TestConstants.moviesURL + "/all";
        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);

        Response response = requestSpecification.get(path);
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(200);
    }

    @Test
    public void movieControllerFindAllMoviesAsAnAuthenticatedStaffTestPositive() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        String path = TestConstants.moviesURL + "/all";
        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);

        Response response = requestSpecification.get(path);
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(200);
    }

    @Test
    public void movieControllerFindAllMoviesAsAnAuthenticatedAdminTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(adminUser.getUserLogin(), passwordNotHashed), TestConstants.adminLoginURL);

        String path = TestConstants.moviesURL + "/all";
        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);

        Response response = requestSpecification.get(path);
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(403);
    }

    // Update tests

    @Test
    public void movieControllerUpdateMovieAsAnUnauthenticatedUserTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String newMovieTitle = "SomeExampleMovieTitleNo1";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(403);
    }

    @Test
    public void movieControllerUpdateMovieAsAnAuthenticatedClientTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        accessToken = this.loginToAccount(new UserInputDTO(clientUser.getUserLogin(), passwordNotHashed), TestConstants.clientLoginURL);

        String newMovieTitle = "SomeExampleMovieTitleNo1";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(403);
    }

    @Test
    public void movieControllerUpdateMovieAsAnAuthenticatedStaffTestPositive() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String movieTitleBefore = movieNo1.getMovieTitle();
        double movieBasePriceBefore = movieNo1.getMovieBasePrice();
        int scrRoomNumberBefore = movieNo1.getScrRoomNumber();
        int numberOfAvailableSeatsBefore = movieNo1.getNumberOfAvailableSeats();

        String newMovieTitle = "SomeExampleMovieTitleNo1";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(204);

        Movie foundMovie = movieService.findByUUID(movieNo1.getMovieID());

        String movieTitleAfter = foundMovie.getMovieTitle();
        double movieBasePriceAfter = foundMovie.getMovieBasePrice();
        int scrRoomNumberAfter = foundMovie.getScrRoomNumber();
        int numberOfAvailableSeatsAfter = foundMovie.getNumberOfAvailableSeats();

        assertEquals(newMovieTitle, movieTitleAfter);
        assertEquals(newMovieBasePrice, movieBasePriceAfter);
        assertEquals(newScrRoomNumber, scrRoomNumberAfter);
        assertEquals(newNumberOfAvailableSeats, numberOfAvailableSeatsAfter);

        assertNotEquals(movieTitleBefore, movieTitleAfter);
        assertNotEquals(movieBasePriceBefore, movieBasePriceAfter);
        assertNotEquals(scrRoomNumberBefore, scrRoomNumberAfter);
        assertNotEquals(numberOfAvailableSeatsBefore, numberOfAvailableSeatsAfter);
    }

    @Test
    public void movieControllerUpdateMovieAsAnAuthenticatedAdminTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        accessToken = this.loginToAccount(new UserInputDTO(adminUser.getUserLogin(), passwordNotHashed), TestConstants.adminLoginURL);

        String newMovieTitle = "SomeExampleMovieTitleNo1";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(403);
    }

    @Test
    public void movieControllerUpdateMovieWithoutIfMatchHeaderAsAnAuthenticatedStaffTestPositive() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);

        String newMovieTitle = "SomeExampleMovieTitleNo1";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(412);
    }

    @Test
    public void movieControllerUpdateMovieWithChangedIDAsAnAuthenticatedStaffTestPositive() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        movieOutputDTO.setMovieID(UUID.randomUUID());

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerUpdateMovieWithNullMovieTitleAsAnAuthenticatedStaffTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String newMovieTitle = null;
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerUpdateMovieWithEmptyMovieTitleTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String newMovieTitle = "";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerUpdateMovieWithMovieTitleTooShortTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String newMovieTitle = "";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerUpdateMovieWithMovieTitleTooLongTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String newMovieTitle = "ddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfd";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerUpdateMovieWithMovieTitleLengthEqualTo1TestPositive() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String movieTitleBefore = movieNo1.getMovieTitle();
        double movieBasePriceBefore = movieNo1.getMovieBasePrice();
        int scrRoomNumberBefore = movieNo1.getScrRoomNumber();
        int numberOfAvailableSeatsBefore = movieNo1.getNumberOfAvailableSeats();

        String newMovieTitle = "d";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(204);

        Movie foundMovie = movieService.findByUUID(movieNo1.getMovieID());

        String movieTitleAfter = foundMovie.getMovieTitle();
        double movieBasePriceAfter = foundMovie.getMovieBasePrice();
        int scrRoomNumberAfter = foundMovie.getScrRoomNumber();
        int numberOfAvailableSeatsAfter = foundMovie.getNumberOfAvailableSeats();

        assertEquals(newMovieTitle, movieTitleAfter);
        assertEquals(newMovieBasePrice, movieBasePriceAfter);
        assertEquals(newScrRoomNumber, scrRoomNumberAfter);
        assertEquals(newNumberOfAvailableSeats, numberOfAvailableSeatsAfter);

        assertNotEquals(movieTitleBefore, movieTitleAfter);
        assertNotEquals(movieBasePriceBefore, movieBasePriceAfter);
        assertNotEquals(scrRoomNumberBefore, scrRoomNumberAfter);
        assertNotEquals(numberOfAvailableSeatsBefore, numberOfAvailableSeatsAfter);
    }

    @Test
    public void movieControllerUpdateMovieWithMovieTitleLengthEqualTo150TestPositive() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String movieTitleBefore = movieNo1.getMovieTitle();
        double movieBasePriceBefore = movieNo1.getMovieBasePrice();
        int scrRoomNumberBefore = movieNo1.getScrRoomNumber();
        int numberOfAvailableSeatsBefore = movieNo1.getNumberOfAvailableSeats();

        String newMovieTitle = "ddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddfddddf";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(204);

        Movie foundMovie = movieService.findByUUID(movieNo1.getMovieID());

        String movieTitleAfter = foundMovie.getMovieTitle();
        double movieBasePriceAfter = foundMovie.getMovieBasePrice();
        int scrRoomNumberAfter = foundMovie.getScrRoomNumber();
        int numberOfAvailableSeatsAfter = foundMovie.getNumberOfAvailableSeats();

        assertEquals(newMovieTitle, movieTitleAfter);
        assertEquals(newMovieBasePrice, movieBasePriceAfter);
        assertEquals(newScrRoomNumber, scrRoomNumberAfter);
        assertEquals(newNumberOfAvailableSeats, numberOfAvailableSeatsAfter);

        assertNotEquals(movieTitleBefore, movieTitleAfter);
        assertNotEquals(movieBasePriceBefore, movieBasePriceAfter);
        assertNotEquals(scrRoomNumberBefore, scrRoomNumberAfter);
        assertNotEquals(numberOfAvailableSeatsBefore, numberOfAvailableSeatsAfter);
    }

    @Test
    public void movieControllerUpdateMovieWithNegativeMovieBasePriceTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String newMovieTitle = "OtherExampleMovieTitleNo1";
        double newMovieBasePrice = -1;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerUpdateMovieWithMovieBasePriceTooHighTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String newMovieTitle = "OtherExampleMovieTitleNo1";
        double newMovieBasePrice = 101.00;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerUpdateMovieWithMovieBasePriceEqualTo0TestPositive() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String movieTitleBefore = movieNo1.getMovieTitle();
        double movieBasePriceBefore = movieNo1.getMovieBasePrice();
        int scrRoomNumberBefore = movieNo1.getScrRoomNumber();
        int numberOfAvailableSeatsBefore = movieNo1.getNumberOfAvailableSeats();

        String newMovieTitle = "SomeExampleMovieTitleNo1";
        double newMovieBasePrice = 0;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(204);

        Movie foundMovie = movieService.findByUUID(movieNo1.getMovieID());

        String movieTitleAfter = foundMovie.getMovieTitle();
        double movieBasePriceAfter = foundMovie.getMovieBasePrice();
        int scrRoomNumberAfter = foundMovie.getScrRoomNumber();
        int numberOfAvailableSeatsAfter = foundMovie.getNumberOfAvailableSeats();

        assertEquals(newMovieTitle, movieTitleAfter);
        assertEquals(newMovieBasePrice, movieBasePriceAfter);
        assertEquals(newScrRoomNumber, scrRoomNumberAfter);
        assertEquals(newNumberOfAvailableSeats, numberOfAvailableSeatsAfter);

        assertNotEquals(movieTitleBefore, movieTitleAfter);
        assertNotEquals(movieBasePriceBefore, movieBasePriceAfter);
        assertNotEquals(scrRoomNumberBefore, scrRoomNumberAfter);
        assertNotEquals(numberOfAvailableSeatsBefore, numberOfAvailableSeatsAfter);
    }

    @Test
    public void movieControllerUpdateMovieWithMovieBasePriceEqualTo100TestPositive() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String movieTitleBefore = movieNo1.getMovieTitle();
        double movieBasePriceBefore = movieNo1.getMovieBasePrice();
        int scrRoomNumberBefore = movieNo1.getScrRoomNumber();
        int numberOfAvailableSeatsBefore = movieNo1.getNumberOfAvailableSeats();

        String newMovieTitle = "SomeExampleMovieTitleNo1";
        double newMovieBasePrice = 100.00;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(204);

        Movie foundMovie = movieService.findByUUID(movieNo1.getMovieID());

        String movieTitleAfter = foundMovie.getMovieTitle();
        double movieBasePriceAfter = foundMovie.getMovieBasePrice();
        int scrRoomNumberAfter = foundMovie.getScrRoomNumber();
        int numberOfAvailableSeatsAfter = foundMovie.getNumberOfAvailableSeats();

        assertEquals(newMovieTitle, movieTitleAfter);
        assertEquals(newMovieBasePrice, movieBasePriceAfter);
        assertEquals(newScrRoomNumber, scrRoomNumberAfter);
        assertEquals(newNumberOfAvailableSeats, numberOfAvailableSeatsAfter);

        assertNotEquals(movieTitleBefore, movieTitleAfter);
        assertNotEquals(movieBasePriceBefore, movieBasePriceAfter);
        assertNotEquals(scrRoomNumberBefore, scrRoomNumberAfter);
        assertNotEquals(numberOfAvailableSeatsBefore, numberOfAvailableSeatsAfter);
    }

    @Test
    public void movieControllerUpdateMovieWithScreeningRoomNumberEqualTo0TestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String newMovieTitle = "OtherExampleMovieTitleNo1";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 0;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerUpdateMovieWithScreeningRoomNumberTooHighTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String newMovieTitle = "OtherExampleMovieTitleNo1";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 31;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerUpdateMovieWithScreeningRoomNumberEqualTo1TestPositive() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String movieTitleBefore = movieNo1.getMovieTitle();
        double movieBasePriceBefore = movieNo1.getMovieBasePrice();
        int scrRoomNumberBefore = movieNo1.getScrRoomNumber();
        int numberOfAvailableSeatsBefore = movieNo1.getNumberOfAvailableSeats();

        String newMovieTitle = "SomeExampleMovieTitleNo1";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 1;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(204);

        Movie foundMovie = movieService.findByUUID(movieNo1.getMovieID());

        String movieTitleAfter = foundMovie.getMovieTitle();
        double movieBasePriceAfter = foundMovie.getMovieBasePrice();
        int scrRoomNumberAfter = foundMovie.getScrRoomNumber();
        int numberOfAvailableSeatsAfter = foundMovie.getNumberOfAvailableSeats();

        assertEquals(newMovieTitle, movieTitleAfter);
        assertEquals(newMovieBasePrice, movieBasePriceAfter);
        assertEquals(newScrRoomNumber, scrRoomNumberAfter);
        assertEquals(newNumberOfAvailableSeats, numberOfAvailableSeatsAfter);

        assertNotEquals(movieTitleBefore, movieTitleAfter);
        assertNotEquals(movieBasePriceBefore, movieBasePriceAfter);
        assertNotEquals(scrRoomNumberBefore, scrRoomNumberAfter);
        assertNotEquals(numberOfAvailableSeatsBefore, numberOfAvailableSeatsAfter);
    }

    @Test
    public void movieControllerUpdateMovieWithScreeningRoomNumberEqualTo30TestPositive() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String movieTitleBefore = movieNo1.getMovieTitle();
        double movieBasePriceBefore = movieNo1.getMovieBasePrice();
        int scrRoomNumberBefore = movieNo1.getScrRoomNumber();
        int numberOfAvailableSeatsBefore = movieNo1.getNumberOfAvailableSeats();

        String newMovieTitle = "SomeExampleMovieTitleNo1";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 30;
        int newNumberOfAvailableSeats = 11;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(204);

        Movie foundMovie = movieService.findByUUID(movieNo1.getMovieID());

        String movieTitleAfter = foundMovie.getMovieTitle();
        double movieBasePriceAfter = foundMovie.getMovieBasePrice();
        int scrRoomNumberAfter = foundMovie.getScrRoomNumber();
        int numberOfAvailableSeatsAfter = foundMovie.getNumberOfAvailableSeats();

        assertEquals(newMovieTitle, movieTitleAfter);
        assertEquals(newMovieBasePrice, movieBasePriceAfter);
        assertEquals(newScrRoomNumber, scrRoomNumberAfter);
        assertEquals(newNumberOfAvailableSeats, numberOfAvailableSeatsAfter);

        assertNotEquals(movieTitleBefore, movieTitleAfter);
        assertNotEquals(movieBasePriceBefore, movieBasePriceAfter);
        assertNotEquals(scrRoomNumberBefore, scrRoomNumberAfter);
        assertNotEquals(numberOfAvailableSeatsBefore, numberOfAvailableSeatsAfter);
    }

    @Test
    public void movieControllerUpdateMovieWithNegativeNumberOfAvailableSeatsTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String newMovieTitle = "OtherExampleMovieTitleNo1";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = -1;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerUpdateMovieWithNumberOfAvailableSeatsTooHighTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String newMovieTitle = "OtherExampleMovieTitleNo1";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 121;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }

    @Test
    public void movieControllerUpdateMovieWithNumberOfAvailableSeatsEqualTo0TestPositive() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String movieTitleBefore = movieNo1.getMovieTitle();
        double movieBasePriceBefore = movieNo1.getMovieBasePrice();
        int scrRoomNumberBefore = movieNo1.getScrRoomNumber();
        int numberOfAvailableSeatsBefore = movieNo1.getNumberOfAvailableSeats();

        String newMovieTitle = "SomeExampleMovieTitleNo1";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 0;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(204);

        Movie foundMovie = movieService.findByUUID(movieNo1.getMovieID());

        String movieTitleAfter = foundMovie.getMovieTitle();
        double movieBasePriceAfter = foundMovie.getMovieBasePrice();
        int scrRoomNumberAfter = foundMovie.getScrRoomNumber();
        int numberOfAvailableSeatsAfter = foundMovie.getNumberOfAvailableSeats();

        assertEquals(newMovieTitle, movieTitleAfter);
        assertEquals(newMovieBasePrice, movieBasePriceAfter);
        assertEquals(newScrRoomNumber, scrRoomNumberAfter);
        assertEquals(newNumberOfAvailableSeats, numberOfAvailableSeatsAfter);

        assertNotEquals(movieTitleBefore, movieTitleAfter);
        assertNotEquals(movieBasePriceBefore, movieBasePriceAfter);
        assertNotEquals(scrRoomNumberBefore, scrRoomNumberAfter);
        assertNotEquals(numberOfAvailableSeatsBefore, numberOfAvailableSeatsAfter);
    }

    @Test
    public void movieControllerUpdateMovieWithNumberOfAvailableSeatsEqualTo120TestPositive() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.contentType(ContentType.JSON);

        Response response = requestSpecification.get(TestConstants.moviesURL + "/" + movieNo1.getMovieID());
        logger.debug("Response: " + response.getBody().asString());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);

        MovieDTO movieOutputDTO = response.getBody().as(MovieDTO.class);
        String etagContent = response.header("ETag");

        String movieTitleBefore = movieNo1.getMovieTitle();
        double movieBasePriceBefore = movieNo1.getMovieBasePrice();
        int scrRoomNumberBefore = movieNo1.getScrRoomNumber();
        int numberOfAvailableSeatsBefore = movieNo1.getNumberOfAvailableSeats();

        String newMovieTitle = "SomeExampleMovieTitleNo1";
        double newMovieBasePrice = 45.27;
        int newScrRoomNumber = 7;
        int newNumberOfAvailableSeats = 120;

        movieOutputDTO.setMovieTitle(newMovieTitle);
        movieOutputDTO.setMovieBasePrice(newMovieBasePrice);
        movieOutputDTO.setScrRoomNumber(newScrRoomNumber);
        movieOutputDTO.setNumberOfAvailableSeats(newNumberOfAvailableSeats);

        requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        requestSpecification.header("If-Match", etagContent);
        requestSpecification.body(movieOutputDTO);

        response = requestSpecification.put(TestConstants.moviesURL + "/update");

        validatableResponse = response.then();
        validatableResponse.statusCode(204);

        Movie foundMovie = movieService.findByUUID(movieNo1.getMovieID());

        String movieTitleAfter = foundMovie.getMovieTitle();
        double movieBasePriceAfter = foundMovie.getMovieBasePrice();
        int scrRoomNumberAfter = foundMovie.getScrRoomNumber();
        int numberOfAvailableSeatsAfter = foundMovie.getNumberOfAvailableSeats();

        assertEquals(newMovieTitle, movieTitleAfter);
        assertEquals(newMovieBasePrice, movieBasePriceAfter);
        assertEquals(newScrRoomNumber, scrRoomNumberAfter);
        assertEquals(newNumberOfAvailableSeats, numberOfAvailableSeatsAfter);

        assertNotEquals(movieTitleBefore, movieTitleAfter);
        assertNotEquals(movieBasePriceBefore, movieBasePriceAfter);
        assertNotEquals(scrRoomNumberBefore, scrRoomNumberAfter);
        assertNotEquals(numberOfAvailableSeatsBefore, numberOfAvailableSeatsAfter);
    }

    // Delete tests

    @Test
    public void movieControllerDeleteMovieAsAnUnauthenticatedClientTestNegative() throws Exception {
        UUID removedMovieID = movieNo1.getMovieID();
        String path = TestConstants.moviesURL + "/" + removedMovieID;

        Movie foundMovie = movieService.findByUUID(removedMovieID);

        assertNotNull(foundMovie);
        assertEquals(movieNo1.getMovieTitle(), foundMovie.getMovieTitle());
        assertEquals(movieNo1.getMovieBasePrice(), foundMovie.getMovieBasePrice());
        assertEquals(movieNo1.getScrRoomNumber(), foundMovie.getScrRoomNumber());
        assertEquals(movieNo1.getNumberOfAvailableSeats(), foundMovie.getNumberOfAvailableSeats());

        RequestSpecification requestSpecification = RestAssured.given();
        Response response = requestSpecification.delete(path);
        logger.debug("Response: " + response);
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(403);
    }

    @Test
    public void movieControllerDeleteMovieAsAnAuthenticatedClientTestNegative() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(clientUser.getUserLogin(), passwordNotHashed), TestConstants.clientLoginURL);

        UUID removedMovieID = movieNo1.getMovieID();
        String path = TestConstants.moviesURL + "/" + removedMovieID;

        Movie foundMovie = movieService.findByUUID(removedMovieID);

        assertNotNull(foundMovie);
        assertEquals(movieNo1.getMovieTitle(), foundMovie.getMovieTitle());
        assertEquals(movieNo1.getMovieBasePrice(), foundMovie.getMovieBasePrice());
        assertEquals(movieNo1.getScrRoomNumber(), foundMovie.getScrRoomNumber());
        assertEquals(movieNo1.getNumberOfAvailableSeats(), foundMovie.getNumberOfAvailableSeats());

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        Response response = requestSpecification.delete(path);
        logger.debug("Response: " + response);
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(403);
    }

    @Test
    public void movieControllerDeleteMovieAsAnAuthenticatedStaffTestPositive() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        UUID removedMovieID = movieNo1.getMovieID();
        String path = TestConstants.moviesURL + "/" + removedMovieID + "/delete";

        Movie foundMovie = movieService.findByUUID(removedMovieID);

        assertNotNull(foundMovie);
        assertEquals(movieNo1.getMovieTitle(), foundMovie.getMovieTitle());
        assertEquals(movieNo1.getMovieBasePrice(), foundMovie.getMovieBasePrice());
        assertEquals(movieNo1.getScrRoomNumber(), foundMovie.getScrRoomNumber());
        assertEquals(movieNo1.getNumberOfAvailableSeats(), foundMovie.getNumberOfAvailableSeats());

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        Response response = requestSpecification.delete(path);
        logger.debug("Response: " + response);
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(204);

        assertThrows(MovieServiceReadException.class, () -> movieService.findByUUID(removedMovieID));
    }

    @Test
    public void movieControllerDeleteMovieAsAnAuthenticatedAdminTestNegative() throws Exception {
        String accessToken = this.loginToAccount(new UserInputDTO(adminUser.getUserLogin(), passwordNotHashed), TestConstants.adminLoginURL);

        UUID removedMovieID = movieNo1.getMovieID();
        String path = TestConstants.moviesURL + "/" + removedMovieID + "/delete";

        Movie foundMovie = movieService.findByUUID(removedMovieID);

        assertNotNull(foundMovie);
        assertEquals(movieNo1.getMovieTitle(), foundMovie.getMovieTitle());
        assertEquals(movieNo1.getMovieBasePrice(), foundMovie.getMovieBasePrice());
        assertEquals(movieNo1.getScrRoomNumber(), foundMovie.getScrRoomNumber());
        assertEquals(movieNo1.getNumberOfAvailableSeats(), foundMovie.getNumberOfAvailableSeats());

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        Response response = requestSpecification.delete(path);
        logger.debug("Response: " + response);
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(403);
    }

    @Test
    public void movieControllerDeleteMovieThatIsNotInTheDatabaseTestNegative() {
        String accessToken = this.loginToAccount(new UserInputDTO(staffUser.getUserLogin(), passwordNotHashed), TestConstants.staffLoginURL);

        UUID removedMovieID = UUID.randomUUID();
        String path = TestConstants.moviesURL + "/" + removedMovieID + "/delete";

        assertThrows(MovieServiceReadException.class, () -> movieService.findByUUID(removedMovieID));

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.header("Authorization", "Bearer " + accessToken);
        Response response = requestSpecification.delete(path);
        logger.debug("Response: " + response);
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(400);
    }

    private String loginToAccount(UserInputDTO userInputDTO, String loginURL) {
        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.accept(ContentType.JSON);
        requestSpecification.body(new UserInputDTO(userInputDTO.getUserLogin(), userInputDTO.getUserPassword()));

        Response response = requestSpecification.post(loginURL);
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.statusCode(200);

        return response.getBody().asString();
    }
}
