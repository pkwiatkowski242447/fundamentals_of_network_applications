package pl.pas.gr3.cinema.repositories;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pas.gr3.cinema.consts.model.UserConstants;
import pl.pas.gr3.cinema.exceptions.repositories.*;
import pl.pas.gr3.cinema.exceptions.repositories.crud.user.*;
import pl.pas.gr3.cinema.exceptions.repositories.other.client.UserActivationException;
import pl.pas.gr3.cinema.exceptions.repositories.other.client.UserDeactivationException;
import pl.pas.gr3.cinema.model.users.Admin;
import pl.pas.gr3.cinema.model.users.Client;
import pl.pas.gr3.cinema.model.users.Staff;
import pl.pas.gr3.cinema.repositories.implementations.UserRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryTest {

    private final static String databaseName = "test";
    private final static Logger logger = LoggerFactory.getLogger(UserRepositoryTest.class);
    private static UserRepository userRepositoryForTests;

    private Client clientNo1;
    private Client clientNo2;
    private Admin adminNo1;
    private Admin adminNo2;
    private Staff staffNo1;
    private Staff staffNo2;

    @BeforeAll
    public static void init() {
        userRepositoryForTests = new UserRepository(databaseName);
    }

    @BeforeEach
    public void addExampleClients() {
        try {
            clientNo1 = userRepositoryForTests.createClient("ClientLoginNo1", "ClientPasswordNo1");
            clientNo2 = userRepositoryForTests.createClient("ClientLoginNo2", "ClientPasswordNo2");
            adminNo1 = userRepositoryForTests.createAdmin("AdminLoginNo1", "AdminPasswordNo1");
            adminNo2 = userRepositoryForTests.createAdmin("AdminLoginNo2", "AdminPasswordNo2");
            staffNo1 = userRepositoryForTests.createStaff("StaffLoginNo1", "StaffPasswordNo1");
            staffNo2 = userRepositoryForTests.createStaff("StaffLoginNo2", "StaffPasswordNo2");
        } catch (UserRepositoryException exception) {
            throw new RuntimeException("Could not initialize test database while adding clients to it.", exception);
        }
    }

    @AfterEach
    public void removeExampleClients() {
        try {
            List<Client> listOfAllClients = userRepositoryForTests.findAllClients();
            for (Client client : listOfAllClients) {
                userRepositoryForTests.delete(client.getUserID(), "client");
            }

            List<Admin> listOfAllAdmins = userRepositoryForTests.findAllAdmins();
            for (Admin admin : listOfAllAdmins) {
                userRepositoryForTests.delete(admin.getUserID(), "admin");
            }

            List<Staff> listOfAllStaffs = userRepositoryForTests.findAllStaffs();
            for (Staff staff : listOfAllStaffs) {
                userRepositoryForTests.delete(staff.getUserID(), "staff");
            }
        } catch (UserRepositoryException exception) {
            throw new RuntimeException("Could not remove all clients from the test database after client repository tests.", exception);
        }
    }

    @AfterAll
    public static void destroy() {
        userRepositoryForTests.close();
    }

    // Some mongo tests

    @Test
    public void mongoRepositoryFindClientWithClientIDTestPositive() throws UserRepositoryException {
        Client foundClient = userRepositoryForTests.findClientByUUID(clientNo1.getUserID());
        assertNotNull(foundClient);
        assertEquals(clientNo1, foundClient);
        assertEquals(clientNo1.getClass(), foundClient.getClass());
        assertEquals(foundClient.getClass(), Client.class);
    }

    @Test
    public void mongoRepositoryFindClientThatIsNotInTheDatabaseWithClientIDTestNegative() {
        Client client = new Client(UUID.randomUUID(), clientNo1.getUserLogin(), clientNo1.getUserPassword(), clientNo1.isUserStatusActive());
        assertNotNull(client);
        assertThrows(UserRepositoryReadException.class, () -> userRepositoryForTests.findClientByUUID(client.getUserID()));
    }

    @Test
    public void mongoRepositoryFindAdminWithAdminIDTestPositive() throws UserRepositoryException {
        Admin foudnAdmin = userRepositoryForTests.findAdminByUUID(adminNo1.getUserID());
        assertNotNull(foudnAdmin);
        assertEquals(adminNo1, foudnAdmin);
        assertEquals(adminNo1.getClass(), foudnAdmin.getClass());
        assertEquals(foudnAdmin.getClass(), Admin.class);
    }

    @Test
    public void mongoRepositoryFindAdminWithAdminThatIsNotInTheDatabaseIDTestNegative() {
        Admin admin = new Admin(UUID.randomUUID(), clientNo1.getUserLogin(), clientNo1.getUserPassword(), clientNo1.isUserStatusActive());
        assertNotNull(admin);
        assertThrows(UserRepositoryReadException.class, () -> userRepositoryForTests.findAdminByUUID(admin.getUserID()));
    }

    @Test
    public void mongoRepositoryFindStaffWithStaffIDTestPositive() throws UserRepositoryException {
        Staff foundStaff = userRepositoryForTests.findStaffByUUID(staffNo1.getUserID());
        assertNotNull(foundStaff);
        assertEquals(staffNo1, foundStaff);
        assertEquals(staffNo1.getClass(), foundStaff.getClass());
        assertEquals(foundStaff.getClass(), Staff.class);
    }

    @Test
    public void mongoRepositoryFindStaffThatIsNotInTheDatabaseWithStaffIDTestNegative() {
        Staff staff = new Staff(UUID.randomUUID(), clientNo1.getUserLogin(), clientNo1.getUserPassword(), clientNo1.isUserStatusActive());
        assertNotNull(staff);
        assertThrows(UserRepositoryReadException.class, () -> userRepositoryForTests.findByUUID(staff.getUserID()));
    }

    // Client create tests

    @Test
    public void userRepositoryCreateClientTestPositive() throws UserRepositoryException {
        Client client = userRepositoryForTests.createClient("SomeLogin", "SomePassword");
        assertNotNull(client);
        Client foundClient = userRepositoryForTests.findClientByUUID(client.getUserID());
        assertNotNull(foundClient);
        assertEquals(client, foundClient);
    }

    @Test
    public void userRepositoryCreateClientWithLoginAlreadyInTheDatabaseTestNegative() {
        assertThrows(UserRepositoryCreateUserDuplicateLoginException.class, () -> userRepositoryForTests.createClient(clientNo1.getUserLogin(), "SomePassword"));
    }

    @Test
    public void userRepositoryCreateClientWithNullLoginTestNegative() {
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createClient(null, "SomePassword"));
    }

    @Test
    public void userRepositoryCreateClientWithEmptyLoginTestNegative() {
        String clientLogin = "";
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createClient(clientLogin, "SomePassword"));
    }

    @Test
    public void userRepositoryCreateClientWithLoginTooShortTestNegative() {
        String clientLogin = "dddf";
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createClient(clientLogin, "SomePassword"));
    }

    @Test
    public void userRepositoryCreateClientWithLoginTooLongTestNegative() {
        String clientLogin = "ddddfddddfddddfddddfd";
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createClient(clientLogin, "SomePassword"));
    }

    @Test
    public void userRepositoryCreateClientWithLoginLengthEqualTo8TestNegative() throws UserRepositoryException {
        String clientLogin = "dddfdddf";
        Client client = userRepositoryForTests.createClient(clientLogin, "SomePassword");
        assertNotNull(client);
        Client foundClient = userRepositoryForTests.findClientByUUID(client.getUserID());
        assertNotNull(foundClient);
        assertEquals(client, foundClient);
    }

    @Test
    public void userRepositoryCreateClientWithLoginLengthEqualTo20TestNegative() throws UserRepositoryException {
        String clientLogin = "dddfdddfdddfdddfdddf";
        Client client = userRepositoryForTests.createClient(clientLogin, "SomePassword");
        assertNotNull(client);
        Client foundClient = userRepositoryForTests.findClientByUUID(client.getUserID());
        assertNotNull(foundClient);
        assertEquals(client, foundClient);
    }

    @Test
    public void userRepositoryCreateClientWithLoginThatViolatesRegExTestNegative() {
        String clientLogin = "Some Login";
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createClient(clientLogin, "SomePassword"));
    }

    // Admin create tests

    @Test
    public void userRepositoryCreateAdminTestPositive() throws UserRepositoryException {
        Admin admin = userRepositoryForTests.createAdmin("SomeLogin", "SomePassword");
        assertNotNull(admin);
        Admin foundAdmin = userRepositoryForTests.findAdminByUUID(admin.getUserID());
        assertNotNull(foundAdmin);
        assertEquals(admin, foundAdmin);
    }

    @Test
    public void userRepositoryCreateAdminWithLoginAlreadyInTheDatabaseTestNegative() {
        assertThrows(UserRepositoryCreateUserDuplicateLoginException.class, () -> userRepositoryForTests.createAdmin(adminNo1.getUserLogin(), "SomePassword"));
    }

    @Test
    public void userRepositoryCreateAdminWithNullLoginTestNegative() {
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createAdmin(null, "SomePassword"));
    }

    @Test
    public void userRepositoryCreateAdminWithEmptyLoginTestNegative() {
        String adminLogin = "";
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createAdmin(adminLogin, "SomePassword"));
    }

    @Test
    public void userRepositoryCreateAdminWithLoginTooShortTestNegative() {
        String adminLogin = "dddf";
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createAdmin(adminLogin, "SomePassword"));
    }

    @Test
    public void userRepositoryCreateAdminWithLoginTooLongTestNegative() {
        String adminLogin = "ddddfddddfddddfddddfd";
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createAdmin(adminLogin, "SomePassword"));
    }

    @Test
    public void userRepositoryCreateAdminWithLoginLengthEqualTo8TestNegative() throws UserRepositoryException {
        String adminLogin = "dddfdddf";
        Admin admin = userRepositoryForTests.createAdmin(adminLogin, "SomePassword");
        assertNotNull(admin);
        Admin foundAdmin = userRepositoryForTests.findAdminByUUID(admin.getUserID());
        assertNotNull(foundAdmin);
        assertEquals(admin, foundAdmin);
    }

    @Test
    public void userRepositoryCreateAdminWithLoginLengthEqualTo20TestNegative() throws UserRepositoryException {
        String adminLogin = "dddfdddfdddfdddfdddf";
        Admin admin = userRepositoryForTests.createAdmin(adminLogin, "SomePassword");
        assertNotNull(admin);
        Admin foundAdmin = userRepositoryForTests.findAdminByUUID(admin.getUserID());
        assertNotNull(foundAdmin);
        assertEquals(admin, foundAdmin);
    }

    @Test
    public void userRepositoryCreateAdminWithLoginThatViolatesRegExTestNegative() {
        String adminLogin = "Some Login";
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createAdmin(adminLogin, "SomePassword"));
    }

    // Staff create tests

    @Test
    public void userRepositoryCreateStaffTestPositive() throws UserRepositoryException {
        Staff staff = userRepositoryForTests.createStaff("SomeLogin", "SomePassword");
        assertNotNull(staff);
        Staff foundStaff = userRepositoryForTests.findStaffByUUID(staff.getUserID());
        assertNotNull(foundStaff);
        assertEquals(staff, foundStaff);
    }

    @Test
    public void userRepositoryCreateStaffWithLoginAlreadyInTheDatabaseTestNegative() {
        assertThrows(UserRepositoryCreateUserDuplicateLoginException.class, () -> userRepositoryForTests.createStaff(staffNo1.getUserLogin(), "SomePassword"));
    }

    @Test
    public void userRepositoryCreateStaffWithNullLoginTestNegative() {
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createStaff(null, "SomePassword"));
    }

    @Test
    public void userRepositoryCreateStaffWithEmptyLoginTestNegative() {
        String staffLogin = "";
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createStaff(staffLogin, "SomePassword"));
    }

    @Test
    public void userRepositoryCreateStaffWithLoginTooShortTestNegative() {
        String staffLogin = "dddf";
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createStaff(staffLogin, "SomePassword"));
    }

    @Test
    public void userRepositoryCreateStaffWithLoginTooLongTestNegative() {
        String staffLogin = "ddddfddddfddddfddddfd";
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createStaff(staffLogin, "SomePassword"));
    }

    @Test
    public void userRepositoryCreateStaffWithLoginLengthEqualTo8TestNegative() throws UserRepositoryException {
        String staffLogin = "dddfdddf";
        Staff staff = userRepositoryForTests.createStaff(staffLogin, "SomePassword");
        assertNotNull(staff);
        Staff foundStaff = userRepositoryForTests.findStaffByUUID(staff.getUserID());
        assertNotNull(foundStaff);
        assertEquals(staff, foundStaff);
    }

    @Test
    public void userRepositoryCreateStaffWithLoginLengthEqualTo20TestNegative() throws UserRepositoryException {
        String staffLogin = "dddfdddfdddfdddfdddf";
        Staff staff = userRepositoryForTests.createStaff(staffLogin, "SomePassword");
        assertNotNull(staff);
        Staff foundStaff = userRepositoryForTests.findStaffByUUID(staff.getUserID());
        assertNotNull(foundStaff);
        assertEquals(staff, foundStaff);
    }

    @Test
    public void userRepositoryCreateStaffWithLoginThatViolatesRegExTestNegative() {
        String staffLogin = "Some Login";
        assertThrows(UserRepositoryCreateException.class, () -> userRepositoryForTests.createStaff(staffLogin, "SomePassword"));
    }

    // Find client by ID tests

    @Test
    public void userRepositoryFindClientByIDTestPositive() throws UserRepositoryException {
        Client foundClient = userRepositoryForTests.findClientByUUID(clientNo1.getUserID());
        assertNotNull(foundClient);
        assertEquals(clientNo1, foundClient);
    }

    @Test
    public void userRepositoryFindClientByIDThatIsNotInTheDatabaseTestNegative() {
        Client client = new Client(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertNotNull(client);
        assertThrows(UserRepositoryUserNotFoundException.class, () -> userRepositoryForTests.findClientByUUID(client.getUserID()));
    }

    @Test
    public void userRepositoryFindAdminByIDTestPositive() throws UserRepositoryException {
        Admin foundAdmin = userRepositoryForTests.findAdminByUUID(adminNo1.getUserID());
        assertNotNull(foundAdmin);
        assertEquals(adminNo1, foundAdmin);
    }

    @Test
    public void userRepositoryFindAdminByIDThatIsNotInTheDatabaseTestNegative() {
        Admin admin = new Admin(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertNotNull(admin);
        assertThrows(UserRepositoryAdminNotFoundException.class, () -> userRepositoryForTests.findAdminByUUID(admin.getUserID()));
    }

    @Test
    public void userRepositoryFindStaffByIDTestPositive() throws UserRepositoryException {
        Staff foundStaff = userRepositoryForTests.findStaffByUUID(staffNo1.getUserID());
        assertNotNull(foundStaff);
        assertEquals(staffNo1, foundStaff);
    }

    @Test
    public void userRepositoryFindStaffByIDThatIsNotInTheDatabaseTestNegative() {
        Staff staff = new Staff(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertNotNull(staff);
        assertThrows(UserRepositoryStaffNotFoundException.class, () -> userRepositoryForTests.findStaffByUUID(staff.getUserID()));
    }

    // Find all users

    @Test
    public void userRepositoryFindAllClientsTestPositive() throws UserRepositoryException {
        List<Client> listOfAllClients = userRepositoryForTests.findAllClients();
        assertNotNull(listOfAllClients);
        assertFalse(listOfAllClients.isEmpty());
        assertEquals(2, listOfAllClients.size());
    }

    @Test
    public void userRepositoryFindAllAdminsTestPositive() throws UserRepositoryException {
        List<Admin> listOfAllAdmins = userRepositoryForTests.findAllAdmins();
        assertNotNull(listOfAllAdmins);
        assertFalse(listOfAllAdmins.isEmpty());
        assertEquals(2, listOfAllAdmins.size());
    }

    @Test
    public void userRepositoryFindAllStaffsTestPositive() throws UserRepositoryException {
        List<Staff> listOfAllStaffs = userRepositoryForTests.findAllStaffs();
        assertNotNull(listOfAllStaffs);
        assertFalse(listOfAllStaffs.isEmpty());
        assertEquals(2, listOfAllStaffs.size());
    }

    // Find users by logins

    @Test
    public void userRepositoryFindClientByLoginTestPositive() throws UserRepositoryException {
        String clientLogin = clientNo1.getUserLogin();
        Client foundClient = userRepositoryForTests.findClientByLogin(clientLogin);
        assertNotNull(foundClient);
        assertEquals(clientNo1, foundClient);
    }

    @Test
    public void userRepositoryFindAdminByLoginTestPositive() throws UserRepositoryException {
        String adminLogin = adminNo1.getUserLogin();
        Admin foundAdmin = userRepositoryForTests.findAdminByLogin(adminLogin);
        assertNotNull(foundAdmin);
        assertEquals(adminNo1, foundAdmin);
    }

    @Test
    public void userRepositoryFindStaffByLoginTestPositive() throws UserRepositoryException {
        String staffLogin = staffNo1.getUserLogin();
        Staff foundStaff = userRepositoryForTests.findStaffByLogin(staffLogin);
        assertNotNull(foundStaff);
        assertEquals(staffNo1, foundStaff);
    }

    // Find users by login that is not in the database

    @Test
    public void userRepositoryFindClientByLoginThatIsNotInTheDatabaseTestNegative() {
        Client client = new Client(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertNotNull(client);
        assertThrows(UserRepositoryUserNotFoundException.class, () -> userRepositoryForTests.findClientByLogin(client.getUserLogin()));
    }

    @Test
    public void userRepositoryFindAdminByLoginThatIsNotInTheDatabaseTestNegative() {
        Admin admin = new Admin(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertNotNull(admin);
        assertThrows(UserRepositoryAdminNotFoundException.class, () -> userRepositoryForTests.findAdminByLogin(admin.getUserLogin()));
    }

    @Test
    public void userRepositoryFindStaffByLoginThatIsNotInTheDatabaseTestNegative() {
        Staff staff = new Staff(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertNotNull(staff);
        assertThrows(UserRepositoryStaffNotFoundException.class, () -> userRepositoryForTests.findStaffByLogin(staff.getUserLogin()));
    }

    // Find all users matching login

    @Test
    public void userRepositoryFindAllClientsMatchingLoginTestPositive() throws UserRepositoryException {
        String clientLogin = "Client";
        List<Client> listOfFoundClients = userRepositoryForTests.findAllClientsMatchingLogin(clientLogin);
        assertNotNull(listOfFoundClients);
        assertFalse(listOfFoundClients.isEmpty());
        assertEquals(2, listOfFoundClients.size());
    }

    @Test
    public void userRepositoryFindAllClientsMatchingLoginWhenLoginIsNotInTheDatabaseTestPositive() throws UserRepositoryException {
        String clientLogin = "NonExistentLogin";
        List<Client> listOfFoundClients = userRepositoryForTests.findAllClientsMatchingLogin(clientLogin);
        assertNotNull(listOfFoundClients);
        assertTrue(listOfFoundClients.isEmpty());
    }

    @Test
    public void userRepositoryFindAllAdminsMatchingLoginTestPositive() throws UserRepositoryException {
        String adminLogin = "Admin";
        List<Admin> listOfFoundAdmins = userRepositoryForTests.findAllAdminsMatchingLogin(adminLogin);
        assertNotNull(listOfFoundAdmins);
        assertFalse(listOfFoundAdmins.isEmpty());
        assertEquals(2, listOfFoundAdmins.size());
    }

    @Test
    public void userRepositoryFindAllAdminsMatchingLoginWhenLoginIsNotInTheDatabaseTestPositive() throws UserRepositoryException {
        String adminLogin = "NonExistentLogin";
        List<Admin> listOfFoundAdmins = userRepositoryForTests.findAllAdminsMatchingLogin(adminLogin);
        assertNotNull(listOfFoundAdmins);
        assertTrue(listOfFoundAdmins.isEmpty());
    }

    @Test
    public void userRepositoryFindAllStaffsMatchingLoginTestPositive() throws UserRepositoryException {
        String staffLogin = "Staff";
        List<Staff> listOfFoundStaffs = userRepositoryForTests.findAllStaffsMatchingLogin(staffLogin);
        assertNotNull(listOfFoundStaffs);
        assertFalse(listOfFoundStaffs.isEmpty());
        assertEquals(2, listOfFoundStaffs.size());
    }

    @Test
    public void userRepositoryFindAllStaffsMatchingLoginWhenLoginIsNotInTheDatabaseTestPositive() throws UserRepositoryException {
        String staffLogin = "NonExistentLogin";
        List<Staff> listOfFoundStaffs = userRepositoryForTests.findAllStaffsMatchingLogin(staffLogin);
        assertNotNull(listOfFoundStaffs);
        assertTrue(listOfFoundStaffs.isEmpty());
    }

    // Activate users tests positive

    @Test
    public void userRepositoryActivateClientTestPositive() throws UserRepositoryException {
        clientNo1.setUserStatusActive(false);
        userRepositoryForTests.updateClient(clientNo1);
        Client foundClient = userRepositoryForTests.findClientByUUID(clientNo1.getUserID());
        assertNotNull(foundClient);
        assertFalse(foundClient.isUserStatusActive());
        userRepositoryForTests.activate(clientNo1, UserConstants.CLIENT_DISCRIMINATOR);
        foundClient = userRepositoryForTests.findClientByUUID(clientNo1.getUserID());
        assertNotNull(foundClient);
        assertTrue(foundClient.isUserStatusActive());
    }

    @Test
    public void userRepositoryActivateAdminTestPositive() throws UserRepositoryException {
        adminNo1.setUserStatusActive(false);
        userRepositoryForTests.updateAdmin(adminNo1);
        Admin foundAdmin = userRepositoryForTests.findAdminByUUID(adminNo1.getUserID());
        assertNotNull(foundAdmin);
        assertFalse(foundAdmin.isUserStatusActive());
        userRepositoryForTests.activate(adminNo1, UserConstants.ADMIN_DISCRIMINATOR);
        foundAdmin = userRepositoryForTests.findAdminByUUID(adminNo1.getUserID());
        assertNotNull(foundAdmin);
        assertTrue(foundAdmin.isUserStatusActive());
    }

    @Test
    public void userRepositoryActivateStaffTestPositive() throws UserRepositoryException {
        staffNo1.setUserStatusActive(false);
        userRepositoryForTests.updateStaff(staffNo1);
        Staff foundStaff = userRepositoryForTests.findStaffByUUID(staffNo1.getUserID());
        assertNotNull(foundStaff);
        assertFalse(foundStaff.isUserStatusActive());
        userRepositoryForTests.activate(staffNo1, UserConstants.STAFF_DISCRIMINATOR);
        foundStaff = userRepositoryForTests.findStaffByUUID(staffNo1.getUserID());
        assertNotNull(foundStaff);
        assertTrue(foundStaff.isUserStatusActive());
    }

    // Deactivate users test negative

    @Test
    public void userRepositoryActivateClientThatIsNotInTheDatabaseTestNegative() {
        Client client = new Client(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertThrows(UserActivationException.class, () -> userRepositoryForTests.activate(client, UserConstants.CLIENT_DISCRIMINATOR));
    }

    @Test
    public void userRepositoryActivateAdminThatIsNotInTheDatabaseTestNegative() {
        Admin admin = new Admin(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertThrows(UserActivationException.class, () -> userRepositoryForTests.activate(admin, UserConstants.ADMIN_DISCRIMINATOR));
    }

    @Test
    public void userRepositoryActivateStaffThatIsNotInTheDatabaseTestNegative() {
        Staff staff = new Staff(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertThrows(UserActivationException.class, () -> userRepositoryForTests.activate(staff, UserConstants.STAFF_DISCRIMINATOR));
    }

    // Deactivate users tests positive

    @Test
    public void userRepositoryDeactivateClientTestPositive() throws UserRepositoryException {
        Client foundClient = userRepositoryForTests.findClientByUUID(clientNo1.getUserID());
        assertNotNull(foundClient);
        assertTrue(foundClient.isUserStatusActive());
        userRepositoryForTests.deactivate(clientNo1, UserConstants.CLIENT_DISCRIMINATOR);
        foundClient = userRepositoryForTests.findClientByUUID(clientNo1.getUserID());
        assertNotNull(foundClient);
        assertFalse(foundClient.isUserStatusActive());
    }

    @Test
    public void userRepositoryDeactivateAdminTestPositive() throws UserRepositoryException {
        Admin foundAdmin = userRepositoryForTests.findAdminByUUID(adminNo1.getUserID());
        assertNotNull(foundAdmin);
        assertTrue(foundAdmin.isUserStatusActive());
        userRepositoryForTests.deactivate(adminNo1, UserConstants.ADMIN_DISCRIMINATOR);
        foundAdmin = userRepositoryForTests.findAdminByUUID(adminNo1.getUserID());
        assertNotNull(foundAdmin);
        assertFalse(foundAdmin.isUserStatusActive());
    }

    @Test
    public void userRepositoryDeactivateStaffTestPositive() throws UserRepositoryException {
        Staff foundStaff = userRepositoryForTests.findStaffByUUID(staffNo1.getUserID());
        assertNotNull(foundStaff);
        assertTrue(foundStaff.isUserStatusActive());
        userRepositoryForTests.deactivate(staffNo1, UserConstants.STAFF_DISCRIMINATOR);
        foundStaff = userRepositoryForTests.findStaffByUUID(staffNo1.getUserID());
        assertNotNull(foundStaff);
        assertFalse(foundStaff.isUserStatusActive());
    }

    // Deactivate users test negative

    @Test
    public void userRepositoryDeactivateClientThatIsNotInTheDatabaseTestNegative() {
        Client client = new Client(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertThrows(UserDeactivationException.class, () -> userRepositoryForTests.deactivate(client, UserConstants.CLIENT_DISCRIMINATOR));
    }

    @Test
    public void userRepositoryDeactivateAdminThatIsNotInTheDatabaseTestNegative() {
        Admin admin = new Admin(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertThrows(UserDeactivationException.class, () -> userRepositoryForTests.deactivate(admin, UserConstants.ADMIN_DISCRIMINATOR));
    }

    @Test
    public void userRepositoryDeactivateStaffThatIsNotInTheDatabaseTestNegative() {
        Staff staff = new Staff(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertThrows(UserDeactivationException.class, () -> userRepositoryForTests.deactivate(staff, UserConstants.STAFF_DISCRIMINATOR));
    }

    // Update client tests

    @Test
    public void userRepositoryUpdateClientTestPositive() throws UserRepositoryException {
        String newLogin = "NewLogin";
        String newPassword = "NewPassword";
        String loginBefore = clientNo1.getUserLogin();
        String passwordBefore = clientNo1.getUserPassword();
        clientNo1.setUserLogin(newLogin);
        clientNo1.setUserPassword(newPassword);
        userRepositoryForTests.updateClient(clientNo1);
        Client foundClient = userRepositoryForTests.findClientByUUID(clientNo1.getUserID());
        String loginAfter = foundClient.getUserLogin();
        String passwordAfter = foundClient.getUserPassword();
        assertNotNull(loginAfter);
        assertNotNull(passwordAfter);
        assertEquals(newLogin, loginAfter);
        assertEquals(newPassword, passwordAfter);
        assertNotEquals(loginBefore, loginAfter);
        assertNotEquals(passwordBefore, passwordAfter);
    }

    @Test
    public void userRepositoryUpdateClientThatIsNotInTheDatabaseTestNegative() {
        Client client = new Client(UUID.randomUUID(), clientNo1.getUserLogin(), clientNo1.getUserPassword(), clientNo1.isUserStatusActive());
        assertNotNull(client);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateClient(client));
    }

    @Test
    public void userRepositoryUpdateClientWithNullLoginTestNegative() {
        clientNo1.setUserLogin(null);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateClient(clientNo1));
    }

    @Test
    public void userRepositoryUpdateClientWithEmptyLoginTestNegative() {
        String newLogin = "";
        clientNo1.setUserLogin(newLogin);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateClient(clientNo1));
    }

    @Test
    public void userRepositoryUpdateClientWithLoginTooShortTestNegative() {
        String newLogin = "dddf";
        clientNo1.setUserLogin(newLogin);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateClient(clientNo1));
    }

    @Test
    public void userRepositoryUpdateClientWithLoginTooLongTestNegative() {
        String newLogin = "ddddfddddfddddfddddfd";
        clientNo1.setUserLogin(newLogin);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateClient(clientNo1));
    }

    @Test
    public void userRepositoryUpdateClientWithLoginLengthEqualTo8TestNegative() throws UserRepositoryException {
        String newLogin = "dddfdddf";
        String loginBefore = clientNo1.getUserLogin();
        clientNo1.setUserLogin(newLogin);
        userRepositoryForTests.updateClient(clientNo1);
        Client foundClient = userRepositoryForTests.findClientByUUID(clientNo1.getUserID());
        String loginAfter = foundClient.getUserLogin();
        assertNotNull(loginAfter);
        assertEquals(newLogin, loginAfter);
        assertNotEquals(loginBefore, loginAfter);
    }

    @Test
    public void userRepositoryUpdateClientWithLoginLengthEqualTo20TestNegative() throws UserRepositoryException {
        String newLogin = "dddfdddfdddfdddfdddf";
        String loginBefore = clientNo1.getUserLogin();
        clientNo1.setUserLogin(newLogin);
        userRepositoryForTests.updateClient(clientNo1);
        Client foundClient = userRepositoryForTests.findClientByUUID(clientNo1.getUserID());
        String loginAfter = foundClient.getUserLogin();
        assertNotNull(loginAfter);
        assertEquals(newLogin, loginAfter);
        assertNotEquals(loginBefore, loginAfter);
    }

    @Test
    public void userRepositoryUpdateClientWithLoginThatViolatesRegExTestNegative() {
        String clientLogin = "Some Login";
        clientNo1.setUserLogin(clientLogin);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateClient(clientNo1));
    }

    // Update admin tests

    @Test
    public void userRepositoryUpdateAdminTestPositive() throws UserRepositoryException {
        String newLogin = "NewLogin";
        String newPassword = "NewPassword";
        String loginBefore = adminNo1.getUserLogin();
        String passwordBefore = adminNo1.getUserPassword();
        adminNo1.setUserLogin(newLogin);
        adminNo1.setUserPassword(newPassword);
        userRepositoryForTests.updateAdmin(adminNo1);
        Admin foundAdmin = userRepositoryForTests.findAdminByUUID(adminNo1.getUserID());
        String loginAfter = foundAdmin.getUserLogin();
        String passwordAfter = foundAdmin.getUserPassword();
        assertNotNull(loginAfter);
        assertNotNull(passwordAfter);
        assertEquals(newLogin, loginAfter);
        assertEquals(newPassword, passwordAfter);
        assertNotEquals(loginBefore, loginAfter);
        assertNotEquals(passwordBefore, passwordAfter);
    }

    @Test
    public void userRepositoryUpdateAdminThatIsNotInTheDatabaseTestNegative() {
        Admin admin = new Admin(UUID.randomUUID(), adminNo1.getUserLogin(), adminNo1.getUserPassword(), adminNo1.isUserStatusActive());
        assertNotNull(admin);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateAdmin(admin));
    }

    @Test
    public void userRepositoryUpdateAdminWithNullLoginTestNegative() {
        adminNo1.setUserLogin(null);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateAdmin(adminNo1));
    }

    @Test
    public void userRepositoryUpdateAdminWithEmptyLoginTestNegative() {
        String newLogin = "";
        adminNo1.setUserLogin(newLogin);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateAdmin(adminNo1));
    }

    @Test
    public void userRepositoryUpdateAdminWithLoginTooShortTestNegative() {
        String newLogin = "dddf";
        adminNo1.setUserLogin(newLogin);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateAdmin(adminNo1));
    }

    @Test
    public void userRepositoryUpdateAdminWithLoginTooLongTestNegative() {
        String newLogin = "ddddfddddfddddfddddfd";
        adminNo1.setUserLogin(newLogin);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateAdmin(adminNo1));
    }

    @Test
    public void userRepositoryUpdateAdminWithLoginLengthEqualTo8TestNegative() throws UserRepositoryException {
        String newLogin = "dddfdddf";
        String loginBefore = adminNo1.getUserLogin();
        adminNo1.setUserLogin(newLogin);
        userRepositoryForTests.updateAdmin(adminNo1);
        Admin foundAdmin = userRepositoryForTests.findAdminByUUID(adminNo1.getUserID());
        String loginAfter = foundAdmin.getUserLogin();
        assertNotNull(loginAfter);
        assertEquals(newLogin, loginAfter);
        assertNotEquals(loginBefore, loginAfter);
    }

    @Test
    public void userRepositoryUpdateAdminWithLoginLengthEqualTo20TestNegative() throws UserRepositoryException {
        String newLogin = "dddfdddfdddfdddfdddf";
        String loginBefore = adminNo1.getUserLogin();
        adminNo1.setUserLogin(newLogin);
        userRepositoryForTests.updateAdmin(adminNo1);
        Admin foundAdmin = userRepositoryForTests.findAdminByUUID(adminNo1.getUserID());
        String loginAfter = foundAdmin.getUserLogin();
        assertNotNull(loginAfter);
        assertEquals(newLogin, loginAfter);
        assertNotEquals(loginBefore, loginAfter);
    }

    @Test
    public void userRepositoryUpdateAdminWithLoginThatViolatesRegExTestNegative() {
        String clientLogin = "Some Login";
        adminNo1.setUserLogin(clientLogin);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateAdmin(adminNo1));
    }

    // Staff update tests

    @Test
    public void userRepositoryUpdateStaffTestPositive() throws UserRepositoryException {
        String newLogin = "NewLogin";
        String newPassword = "NewPassword";
        String loginBefore = staffNo1.getUserLogin();
        String passwordBefore = staffNo1.getUserPassword();
        staffNo1.setUserLogin(newLogin);
        staffNo1.setUserPassword(newPassword);
        userRepositoryForTests.updateStaff(staffNo1);
        Staff foundStaff = userRepositoryForTests.findStaffByUUID(staffNo1.getUserID());
        String loginAfter = foundStaff.getUserLogin();
        String passwordAfter = foundStaff.getUserPassword();
        assertNotNull(loginAfter);
        assertNotNull(passwordAfter);
        assertEquals(newLogin, loginAfter);
        assertEquals(newPassword, passwordAfter);
        assertNotEquals(loginBefore, loginAfter);
        assertNotEquals(passwordBefore, passwordAfter);
    }

    @Test
    public void userRepositoryUpdateStaffThatIsNotInTheDatabaseTestNegative() {
        Staff staff = new Staff(UUID.randomUUID(), adminNo1.getUserLogin(), adminNo1.getUserPassword(), adminNo1.isUserStatusActive());
        assertNotNull(staff);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateStaff(staff));
    }

    @Test
    public void userRepositoryUpdateStaffWithNullLoginTestNegative() {
        staffNo1.setUserLogin(null);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateStaff(staffNo1));
    }

    @Test
    public void userRepositoryUpdateStaffWithEmptyLoginTestNegative() {
        String newLogin = "";
        staffNo1.setUserLogin(newLogin);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateStaff(staffNo1));
    }

    @Test
    public void userRepositoryUpdateStaffWithLoginTooShortTestNegative() {
        String newLogin = "dddf";
        staffNo1.setUserLogin(newLogin);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateStaff(staffNo1));
    }

    @Test
    public void userRepositoryUpdateStaffWithLoginTooLongTestNegative() {
        String newLogin = "ddddfddddfddddfddddfd";
        staffNo1.setUserLogin(newLogin);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateStaff(staffNo1));
    }

    @Test
    public void userRepositoryUpdateStaffWithLoginLengthEqualTo8TestNegative() throws UserRepositoryException {
        String newLogin = "dddfdddf";
        String loginBefore = staffNo1.getUserLogin();
        staffNo1.setUserLogin(newLogin);
        userRepositoryForTests.updateStaff(staffNo1);
        Staff foundStaff = userRepositoryForTests.findStaffByUUID(staffNo1.getUserID());
        String loginAfter = foundStaff.getUserLogin();
        assertNotNull(loginAfter);
        assertEquals(newLogin, loginAfter);
        assertNotEquals(loginBefore, loginAfter);
    }

    @Test
    public void userRepositoryUpdateStaffWithLoginLengthEqualTo20TestNegative() throws UserRepositoryException {
        String newLogin = "dddfdddfdddfdddfdddf";
        String loginBefore = staffNo1.getUserLogin();
        staffNo1.setUserLogin(newLogin);
        userRepositoryForTests.updateStaff(staffNo1);
        Staff foundStaff = userRepositoryForTests.findStaffByUUID(staffNo1.getUserID());
        String loginAfter = foundStaff.getUserLogin();
        assertNotNull(loginAfter);
        assertEquals(newLogin, loginAfter);
        assertNotEquals(loginBefore, loginAfter);
    }

    @Test
    public void userRepositoryUpdateStaffWithLoginThatViolatesRegExTestNegative() {
        String clientLogin = "Some Login";
        staffNo1.setUserLogin(clientLogin);
        assertThrows(UserRepositoryUpdateException.class, () -> userRepositoryForTests.updateStaff(staffNo1));
    }

    // Client delete tests

    @Test
    public void userRepositoryDeleteClientTestPositive() throws UserRepositoryException {
        int numOfClientsBefore = userRepositoryForTests.findAllClients().size();
        UUID removedClientID = clientNo1.getUserID();
        userRepositoryForTests.delete(clientNo1.getUserID(), UserConstants.CLIENT_DISCRIMINATOR);
        int numOfClientsAfter = userRepositoryForTests.findAllClients().size();
        assertNotEquals(numOfClientsBefore, numOfClientsAfter);
        assertEquals(2, numOfClientsBefore);
        assertEquals(1, numOfClientsAfter);
        assertThrows(UserRepositoryReadException.class, () -> userRepositoryForTests.findByUUID(removedClientID));
    }

    @Test
    public void userRepositoryDeleteClientThatIsNotInTheDatabaseTestNegative() {
        Client client = new Client(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertThrows(UserRepositoryDeleteException.class, () -> userRepositoryForTests.delete(client.getUserID(), UserConstants.CLIENT_DISCRIMINATOR));
    }

    // Admin delete tests

    @Test
    public void userRepositoryDeleteAdminTestPositive() throws UserRepositoryException {
        int numOfAdminsBefore = userRepositoryForTests.findAllAdmins().size();
        UUID removedAdminID = adminNo1.getUserID();
        userRepositoryForTests.delete(adminNo1.getUserID(), UserConstants.ADMIN_DISCRIMINATOR);
        int numOfAdminsAfter = userRepositoryForTests.findAllAdmins().size();
        assertNotEquals(numOfAdminsBefore, numOfAdminsAfter);
        assertEquals(2, numOfAdminsBefore);
        assertEquals(1, numOfAdminsAfter);
        assertThrows(UserRepositoryReadException.class, () -> userRepositoryForTests.findAdminByUUID(removedAdminID));
    }

    @Test
    public void userRepositoryDeleteAdminThatIsNotInTheDatabaseTestNegative() {
        Admin admin = new Admin(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertThrows(UserRepositoryDeleteException.class, () -> userRepositoryForTests.delete(admin.getUserID(), UserConstants.ADMIN_DISCRIMINATOR));
    }

    // Staff delete tests

    @Test
    public void userRepositoryDeleteStaffTestPositive() throws UserRepositoryException {
        int numOfStaffsBefore = userRepositoryForTests.findAllStaffs().size();
        UUID removedStaffID = staffNo1.getUserID();
        userRepositoryForTests.delete(staffNo1.getUserID(), UserConstants.STAFF_DISCRIMINATOR);
        int numOfStaffsAfter = userRepositoryForTests.findAllStaffs().size();
        assertNotEquals(numOfStaffsBefore, numOfStaffsAfter);
        assertEquals(2, numOfStaffsBefore);
        assertEquals(1, numOfStaffsAfter);
        assertThrows(UserRepositoryReadException.class, () -> userRepositoryForTests.findStaffByUUID(removedStaffID));
    }

    @Test
    public void userRepositoryDeleteStaffThatIsNotInTheDatabaseTestNegative() {
        Staff staff = new Staff(UUID.randomUUID(), "SomeLogin", "SomePassword");
        assertThrows(UserRepositoryDeleteException.class, () -> userRepositoryForTests.delete(staff.getUserID(), UserConstants.STAFF_DISCRIMINATOR));
    }

    // Delete user with wrong discriminator

    @Test
    public void userRepositoryTryDeletingClientWithAdminDiscriminatorTestNegative() {
        assertThrows(UserRepositoryDeleteException.class, () -> userRepositoryForTests.delete(clientNo1.getUserID(), UserConstants.ADMIN_DISCRIMINATOR));
    }

    @Test
    public void userRepositoryTryDeletingClientWithStaffDiscriminatorTestNegative() {
        assertThrows(UserRepositoryDeleteException.class, () -> userRepositoryForTests.delete(clientNo1.getUserID(), UserConstants.STAFF_DISCRIMINATOR));
    }

    @Test
    public void userRepositoryTryDeletingAdminWithClientDiscriminatorTestNegative() {
        assertThrows(UserRepositoryDeleteException.class, () -> userRepositoryForTests.delete(adminNo1.getUserID(), UserConstants.CLIENT_DISCRIMINATOR));
    }

    @Test
    public void userRepositoryTryDeletingAdminWithStaffDiscriminatorTestNegative() {
        assertThrows(UserRepositoryDeleteException.class, () -> userRepositoryForTests.delete(adminNo1.getUserID(), UserConstants.STAFF_DISCRIMINATOR));
    }

    @Test
    public void userRepositoryTryDeletingStaffWithClientDiscriminatorTestNegative() {
        assertThrows(UserRepositoryDeleteException.class, () -> userRepositoryForTests.delete(staffNo1.getUserID(), UserConstants.CLIENT_DISCRIMINATOR));
    }

    @Test
    public void userRepositoryTryDeletingStaffWithAdminDiscriminatorTestNegative() {
        assertThrows(UserRepositoryDeleteException.class, () -> userRepositoryForTests.delete(staffNo1.getUserID(), UserConstants.ADMIN_DISCRIMINATOR));
    }
}