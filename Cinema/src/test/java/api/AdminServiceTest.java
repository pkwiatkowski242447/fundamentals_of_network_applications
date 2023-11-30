package api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pas.gr3.cinema.dto.users.AdminDTO;
import pl.pas.gr3.cinema.dto.users.AdminInputDTO;
import pl.pas.gr3.cinema.dto.users.AdminPasswordDTO;
import pl.pas.gr3.cinema.exceptions.managers.crud.admin.AdminManagerCreateException;
import pl.pas.gr3.cinema.exceptions.managers.crud.admin.AdminManagerDeleteException;
import pl.pas.gr3.cinema.exceptions.managers.crud.admin.AdminManagerReadException;
import pl.pas.gr3.cinema.managers.implementations.AdminManager;
import pl.pas.gr3.cinema.model.users.Admin;
import pl.pas.gr3.cinema.repositories.implementations.ClientRepository;

import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AdminServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceTest.class);
    private static String adminsBaseURL;

    private static final String databaseName = "default";
    private static ClientRepository clientRepository;
    private static AdminManager adminManager;

    private Admin adminNo1;
    private Admin adminNo2;

    @BeforeAll
    public static void init() {
        clientRepository = new ClientRepository(databaseName);
        adminManager = new AdminManager(clientRepository);

        adminsBaseURL = "http://localhost:8080/Cinema-1.0-SNAPSHOT/api/admins";
        RestAssured.baseURI = adminsBaseURL;
    }

    @BeforeEach
    public void initializeSampleData() {
        this.clearCollection();
        try {
            adminNo1 = adminManager.create("UniqueAdminLoginNo1", "UniqueAdminPasswordNo1");
            adminNo2 = adminManager.create("UniqueAdminLoginNo2", "UniqueAdminPasswordNo2");
        } catch (AdminManagerCreateException exception) {
            logger.debug(exception.getMessage());
        }
    }

    @AfterEach
    public void destroySampleData() {
        this.clearCollection();
    }

    private void clearCollection() {
        try {
            List<Admin> listOfAdmins = adminManager.findAll();
            for (Admin admin : listOfAdmins) {
                adminManager.delete(admin.getClientID());
            }
        } catch (AdminManagerReadException | AdminManagerDeleteException exception) {
            logger.debug(exception.getMessage());
        }
    }

    @AfterAll
    public static void destroy() {
        adminManager.close();
    }

    // Create tests

    @Test
    public void adminServiceCreateAdminTestPositive() throws Exception {
        String adminLogin = "SecretAdminLoginNo1";
        String adminPassword = "SecretAdminPasswordNo1";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(201);

            AdminDTO createdObject = jsonb.fromJson(response.asString(), AdminDTO.class);

            assertNotNull(createdObject);
            assertNotNull(createdObject.getAdminID());
            assertEquals(createdObject.getAdminLogin(), adminInputDTO.getLogin());
            assertTrue(createdObject.isAdminStatusActive());
        }
    }

    @Test
    public void adminServiceCreateAdminWithNullLoginThatTestNegative() throws Exception {
        String adminLogin = null;
        String adminPassword = "SecretAdminPasswordNo1";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceCreateAdminWithEmptyLoginThatTestNegative() throws Exception {
        String adminLogin = "";
        String adminPassword = "SecretAdminPasswordNo1";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceCreateAdminWithLoginTooShortThatTestNegative() throws Exception {
        String adminLogin = "ddddfdd";
        String adminPassword = "SecretAdminPasswordNo1";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceCreateAdminWithLoginTooLongThatTestNegative() throws Exception {
        String adminLogin = "ddddfddddfddddfddddfd";
        String adminPassword = "SecretAdminPasswordNo1";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceCreateAdminWithLoginLengthEqualTo8ThatTestPositive() throws Exception {
        String adminLogin = "ddddfddd";
        String adminPassword = "SecretAdminPasswordNo1";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);
            logger.info(response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(201);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            AdminDTO createdObject = jsonb.fromJson(response.asString(), AdminDTO.class);

            assertNotNull(createdObject);
            assertNotNull(createdObject.getAdminID());
            assertEquals(createdObject.getAdminLogin(), adminInputDTO.getLogin());
            assertTrue(createdObject.isAdminStatusActive());
        }
    }

    @Test
    public void adminServiceCreateAdminWithLoginLengthEqualTo20ThatTestPositive() throws Exception {
        String adminLogin = "ddddfddddfddddfddddf";
        String adminPassword = "SecretAdminPasswordNo1";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);
            logger.info(response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(201);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            AdminDTO createdObject = jsonb.fromJson(response.asString(), AdminDTO.class);

            assertNotNull(createdObject);
            assertNotNull(createdObject.getAdminID());
            assertEquals(createdObject.getAdminLogin(), adminInputDTO.getLogin());
            assertTrue(createdObject.isAdminStatusActive());
        }
    }

    @Test
    public void adminServiceCreateAdminWithLoginThatDoesNotMeetRegExTestNegative() throws Exception {
        String adminLogin = "Some Invalid Login";
        String adminPassword = "SecretAdminPasswordNo1";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceCreateAdminWithLoginThatIsAlreadyInTheDatabaseTestNegative() throws Exception {
        String adminLogin = adminNo1.getClientLogin();
        String adminPassword = "SecretAdminPasswordNo1";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceCreateAdminWithNullPasswordThatTestNegative() throws Exception {
        String adminLogin = "SecretAdminLoginNo1";
        String adminPassword = null;
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceCreateAdminWithEmptyPasswordThatTestNegative() throws Exception {
        String adminLogin = "SecretAdminLoginNo1";
        String adminPassword = "";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceCreateAdminWithPasswordTooShortThatTestNegative() throws Exception {
        String adminLogin = "SecretAdminLoginNo1";
        String adminPassword = "ddddfdd";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceCreateAdminWithPasswordTooLongThatTestNegative() throws Exception {
        String adminLogin = "SecretAdminLoginNo1";
        String adminPassword = "ddddfddddfddddfddddfddddfddddfddddfddddfd";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceCreateAdminWithPasswordLengthEqualTo8ThatTestPositive() throws Exception {
        String adminLogin = "SecretAdminLoginNo1";
        String adminPassword = "ddddfddd";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);
            logger.info(response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(201);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            AdminDTO createdObject = jsonb.fromJson(response.asString(), AdminDTO.class);

            assertNotNull(createdObject);
            assertNotNull(createdObject.getAdminID());
            assertEquals(createdObject.getAdminLogin(), adminInputDTO.getLogin());
            assertTrue(createdObject.isAdminStatusActive());
        }
    }

    @Test
    public void adminServiceCreateAdminWithPasswordLengthEqualTo40ThatTestPositive() throws Exception {
        String adminLogin = "SecretAdminLoginNo1";
        String adminPassword = "ddddfddddfddddfddddfddddfddddfddddfddddf";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);
            logger.info(response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(201);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            AdminDTO createdObject = jsonb.fromJson(response.asString(), AdminDTO.class);

            assertNotNull(createdObject);
            assertNotNull(createdObject.getAdminID());
            assertEquals(createdObject.getAdminLogin(), adminInputDTO.getLogin());
            assertTrue(createdObject.isAdminStatusActive());
        }
    }

    @Test
    public void adminServiceCreateAdminWithPasswordThatDoesNotMeetRegExTestNegative() throws Exception {
        String adminLogin = "SecretAdminLoginNo1";
        String adminPassword = "Some Invalid Password";
        AdminInputDTO adminInputDTO = new AdminInputDTO(adminLogin, adminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String jsonPayload = jsonb.toJson(adminInputDTO);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.post(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    // Read tests

    @Test
    public void adminServiceFindAdminByIDTestPositive() throws Exception {
        UUID searchedAdminID = adminNo1.getClientID();
        try (Jsonb jsonb = JsonbBuilder.create()) {
            String path = adminsBaseURL + "/" + searchedAdminID;

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.urlEncodingEnabled(false);
            requestSpecification.accept(ContentType.JSON);
            requestSpecification.get(path);

            Response response = requestSpecification.get(path);
            logger.info(response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(200);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            AdminDTO adminDTO = jsonb.fromJson(response.asString(), AdminDTO.class);

            assertEquals(adminNo1.getClientID(), adminDTO.getAdminID());
            assertEquals(adminNo1.getClientLogin(), adminDTO.getAdminLogin());
            assertEquals(adminNo1.isClientStatusActive(), adminDTO.isAdminStatusActive());
        }
    }

    @Test
    public void adminServiceFindAdminByIDThatIsNotInTheDatabaseTestNegative() throws URISyntaxException {
        UUID searchedAdminID = UUID.randomUUID();
        String path = adminsBaseURL + "/" + searchedAdminID;

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.accept(ContentType.JSON);

        Response response = requestSpecification.get(path);

        assertNotNull(response.asString());
        logger.info("Response: " + response.asString());

        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }

    @Test
    public void adminServiceFindAdminByLoginTestPositive() throws Exception {
        String searchedAdminLogin = adminNo1.getClientLogin();
        try (Jsonb jsonb = JsonbBuilder.create()) {
            String path = adminsBaseURL + "/login/" + searchedAdminLogin;

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.accept(ContentType.JSON);

            Response response = requestSpecification.get(path);
            logger.info(response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(200);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            AdminDTO adminDTO = jsonb.fromJson(response.asString(), AdminDTO.class);

            assertEquals(adminNo1.getClientID(), adminDTO.getAdminID());
            assertEquals(adminNo1.getClientLogin(), adminDTO.getAdminLogin());
            assertEquals(adminNo1.isClientStatusActive(), adminDTO.isAdminStatusActive());
        }
    }

    @Test
    public void adminServiceFindAdminByLoginThatIsNotInTheDatabaseTestNegative() {
        String searchedAdminLogin = "SomeNonExistentLogin";
        String path = adminsBaseURL + "/login/" + searchedAdminLogin;

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.accept(ContentType.JSON);

        Response response = requestSpecification.get(path);
        assertNotNull(response.asString());
        logger.info("Response: " + response.asString());

        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }

    @Test
    public void adminServiceFindAllAdminsMatchingLoginTestPositive() throws Exception {
        adminManager.create("ExtraAdminLogin", "ExtraAdminPassword");
        String matchedLogin = "Extra";
        String path = adminsBaseURL + "?match=" + matchedLogin;

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.accept(ContentType.JSON);

        Response response = requestSpecification.get(path);
        logger.info("Response: " + response.asString());

        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);
    }

    @Test
    public void adminServiceFindAllAdminsTestPositive() throws Exception {
        String path = adminsBaseURL + "/all";

        RequestSpecification requestSpecification = RestAssured.given();
        requestSpecification.accept(ContentType.JSON);

        Response response = requestSpecification.get(path);
        logger.info("Response: " + response.asString());

        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(200);
    }

    // Update tests

    @Test
    public void adminServiceUpdateAdminTestPositive() throws Exception {
        String adminLoginBefore = adminNo1.getClientLogin();
        String adminPasswordBefore = adminNo1.getClientPassword();
        String newAdminLogin = "SomeNewAdminLoginNo1";
        String newAdminPassword = "SomeNewAdminPasswordNo1";

        adminNo1.setClientLogin(newAdminLogin);
        adminNo1.setClientPassword(newAdminPassword);

        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertTrue(response.asString().isEmpty());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(204);
        }

        Admin foundAdmin = adminManager.findByUUID(adminNo1.getClientID());

        String adminLoginAfter = foundAdmin.getClientLogin();
        String adminPasswordAfter = foundAdmin.getClientPassword();

        assertEquals(newAdminLogin, adminLoginAfter);
        assertEquals(newAdminPassword, adminPasswordAfter);
        assertNotEquals(adminLoginBefore, adminLoginAfter);
        assertNotEquals(adminPasswordBefore, adminPasswordAfter);
    }

    @Test
    public void adminServiceUpdateAdminWithNullLoginTestNegative() throws Exception {
        String newAdminLogin = null;
        adminNo1.setClientLogin(newAdminLogin);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceUpdateAdminWithEmptyLoginTestNegative() throws Exception {
        String newAdminLogin = "";
        adminNo1.setClientLogin(newAdminLogin);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceUpdateAdminWithLoginTooShortTestNegative() throws Exception {
        String newAdminLogin = "ddddfdd";
        adminNo1.setClientLogin(newAdminLogin);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceUpdateAdminWithLoginTooLongTestNegative() throws Exception {
        String newAdminLogin = "ddddfddddfddddfddddfddddfddddfddddfddddfd";
        adminNo1.setClientLogin(newAdminLogin);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceUpdateAdminWithLoginLengthEqualTo8TestPositive() throws Exception {
        String adminLoginBefore = adminNo1.getClientLogin();
        String newAdminLogin = "ddddfddd";
        adminNo1.setClientLogin(newAdminLogin);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertTrue(response.asString().isEmpty());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(204);
        }

        Admin foundAdmin = adminManager.findByUUID(adminNo1.getClientID());
        String adminLoginAfter = foundAdmin.getClientLogin();

        assertEquals(newAdminLogin, adminLoginAfter);
        assertNotEquals(adminLoginBefore, adminLoginAfter);
    }

    @Test
    public void adminServiceUpdateAdminWithLoginLengthEqualTo20TestPositive() throws Exception {
        String adminLoginBefore = adminNo1.getClientLogin();
        String newAdminLogin = "ddddfddddfddddfddddf";
        adminNo1.setClientLogin(newAdminLogin);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertTrue(response.asString().isEmpty());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(204);
        }

        Admin foundAdmin = adminManager.findByUUID(adminNo1.getClientID());
        String adminLoginAfter = foundAdmin.getClientLogin();

        assertEquals(newAdminLogin, adminLoginAfter);
        assertNotEquals(adminLoginBefore, adminLoginAfter);
    }

    @Test
    public void adminServiceUpdateAdminWithLoginThatViolatesRegExTestNegative() throws Exception {
        String newAdminLogin = "Some Invalid Login";
        adminNo1.setClientLogin(newAdminLogin);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceUpdateAdminWithNullPasswordTestNegative() throws Exception {
        String newAdminPassword = null;
        adminNo1.setClientPassword(newAdminPassword);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceUpdateAdminWithEmptyPasswordTestNegative() throws Exception {
        String newAdminPassword = "";
        adminNo1.setClientPassword(newAdminPassword);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceUpdateAdminWithPasswordTooShortTestNegative() throws Exception {
        String newAdminPassword = "ddddfdd";
        adminNo1.setClientPassword(newAdminPassword);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceUpdateAdminWithPasswordTooLongTestNegative() throws Exception {
        String newAdminPassword = "ddddfddddfddddfddddfddddfddddfddddfddddfd";
        adminNo1.setClientPassword(newAdminPassword);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    @Test
    public void adminServiceUpdateAdminWithPasswordLengthEqualTo8TestPositive() throws Exception {
        String adminPasswordBefore = adminNo1.getClientPassword();
        String newAdminPassword = "ddddfddd";
        adminNo1.setClientPassword(newAdminPassword);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertTrue(response.asString().isEmpty());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(204);
        }

        Admin foundAdmin = adminManager.findByUUID(adminNo1.getClientID());
        String adminPasswordAfter = foundAdmin.getClientPassword();

        assertEquals(newAdminPassword, adminPasswordAfter);
        assertNotEquals(adminPasswordBefore, adminPasswordAfter);
    }

    @Test
    public void adminServiceUpdateAdminWithPasswordLengthEqualTo40TestPositive() throws Exception {
        String adminPasswordBefore = adminNo1.getClientPassword();
        String newAdminPassword = "ddddfddddfddddfddddfddddfddddfddddfddddf";
        adminNo1.setClientPassword(newAdminPassword);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertTrue(response.asString().isEmpty());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(204);
        }

        Admin foundAdmin = adminManager.findByUUID(adminNo1.getClientID());
        String adminPasswordAfter = foundAdmin.getClientPassword();

        assertEquals(newAdminPassword, adminPasswordAfter);
        assertNotEquals(adminPasswordBefore, adminPasswordAfter);
    }

    @Test
    public void adminServiceUpdateAdminWithPasswordThatViolatesRegExTestNegative() throws Exception {
        String newAdminPassword = "Some Invalid Password";
        adminNo1.setClientPassword(newAdminPassword);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            AdminPasswordDTO adminPasswordDTO = new AdminPasswordDTO(adminNo1.getClientID(), adminNo1.getClientLogin(), adminNo1.getClientPassword(), adminNo1.isClientStatusActive());
            AdminPasswordDTO[] arr = new AdminPasswordDTO[1];
            arr[0] = adminPasswordDTO;
            String jsonPayload = jsonb.toJson(arr[0]);
            logger.info("Json: " + jsonPayload);

            RequestSpecification requestSpecification = RestAssured.given();
            requestSpecification.contentType(ContentType.JSON);
            requestSpecification.body(jsonPayload);

            Response response = requestSpecification.put(adminsBaseURL);

            assertNotNull(response.asString());
            logger.info("Response: " + response.asString());

            ValidatableResponse validatableResponse = response.then();
            validatableResponse.statusCode(400);
        }
    }

    // Activate tests

    @Test
    public void adminServiceActivateAdminTestPositive() throws Exception {
        UUID activatedAdminID = adminNo1.getClientID();
        String path = adminsBaseURL + "/" + activatedAdminID + "/deactivate";

        RequestSpecification requestSpecification = RestAssured.given();

        Response response = requestSpecification.post(path);

        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(204);

        Admin foundAdmin = adminManager.findByUUID(activatedAdminID);
        boolean adminStatusActiveBefore = foundAdmin.isClientStatusActive();

        path = adminsBaseURL + "/" + activatedAdminID + "/activate";

        requestSpecification = RestAssured.given();

        response = requestSpecification.post(path);

        validatableResponse = response.then();
        validatableResponse.statusCode(204);

        foundAdmin = adminManager.findByUUID(activatedAdminID);
        boolean adminStatusActiveAfter = foundAdmin.isClientStatusActive();

        assertTrue(adminStatusActiveAfter);
        assertFalse(adminStatusActiveBefore);
    }

    @Test
    public void adminServiceActivateAdminThatIsNotInTheDatabaseTestNegative() {
        UUID activatedAdminID = UUID.randomUUID();
        String path = adminsBaseURL + "/" + activatedAdminID + "/activate";

        RequestSpecification requestSpecification = RestAssured.given();

        Response response = requestSpecification.post(path);

        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }

    // Deactivate tests

    @Test
    public void adminServiceDeactivateAdminTestPositive() throws Exception {
        boolean adminStatusActiveBefore = adminNo1.isClientStatusActive();
        UUID deactivatedAdminID = adminNo1.getClientID();
        String path = adminsBaseURL + "/" + deactivatedAdminID + "/deactivate";

        RequestSpecification requestSpecification = RestAssured.given();

        Response response = requestSpecification.post(path);

        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(204);

        Admin foundAdmin = adminManager.findByUUID(deactivatedAdminID);
        boolean adminStatusActiveAfter = foundAdmin.isClientStatusActive();

        assertFalse(adminStatusActiveAfter);
        assertTrue(adminStatusActiveBefore);
    }

    @Test
    public void adminServiceDeactivateAdminThatIsNotInTheDatabaseTestNegative() {
        UUID deactivatedAdminID = UUID.randomUUID();
        String path = adminsBaseURL + "/" + deactivatedAdminID + "/deactivate";

        RequestSpecification requestSpecification = RestAssured.given();

        Response response = requestSpecification.post(path);

        ValidatableResponse validatableResponse = response.then();
        validatableResponse.statusCode(400);
    }
}
