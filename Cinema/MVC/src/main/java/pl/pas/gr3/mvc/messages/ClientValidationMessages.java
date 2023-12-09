package pl.pas.gr3.mvc.messages;

public class ClientValidationMessages {

    public final static String NULL_LOGIN = "No user login can be null.";
    public final static String LOGIN_TOO_SHORT = "No user login can be shorter than 8 characters.";
    public final static String LOGIN_TOO_LONG = "No user login can not be longer than 20 characters.";

    public final static String NULL_PASSWORD = "No user password can be null.";
    public final static String PASSWORD_TOO_SHORT = "No user password can be shorter than 8 characters.";
    public final static String PASSWORD_TOO_LONG = "No user password can be longer than 40 characters.";
}
