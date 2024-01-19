package pl.pas.gr3.cinema.model.users;

import lombok.*;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import pl.pas.gr3.cinema.consts.model.UserConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@BsonDiscriminator(key = UserConstants.USER_DISCRIMINATOR_NAME, value = UserConstants.CLIENT_DISCRIMINATOR)
public class Client extends User {

    // Constructors

    public Client(UUID clientID,
                  String clientLogin,
                  String clientPassword) {
        this.userID = clientID;
        this.userLogin = clientLogin;
        this.userPassword = clientPassword;
        this.userStatusActive = true;
    }

    @BsonCreator
    public Client(@BsonProperty(UserConstants.GENERAL_IDENTIFIER) UUID clientID,
                  @BsonProperty(UserConstants.USER_LOGIN) String clientLogin,
                  @BsonProperty(UserConstants.USER_PASSWORD) String clientPassword,
                  @BsonProperty(UserConstants.USER_STATUS_ACTIVE) boolean clientStatusActive) {
        this.userID = clientID;
        this.userLogin = clientLogin;
        this.userPassword = clientPassword;
        this.userStatusActive = clientStatusActive;
    }
}
