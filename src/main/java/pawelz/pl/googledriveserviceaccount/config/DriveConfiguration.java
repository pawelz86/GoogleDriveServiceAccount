package pawelz.pl.googledriveserviceaccount.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import java.io.File;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Configuration
@ConfigurationProperties(prefix = "drive")
public class DriveConfiguration {

    private String emailAddress;
    private String p12Path;
    private String appName;
    private static final Logger logger = LogManager.getLogger(DriveConfiguration.class);

    @Bean
    public Drive getDriveService() throws GeneralSecurityException, IOException {
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = null;
        try {
            logger.debug("Path: {}",p12Path);
            credential = getCredential(jsonFactory, httpTransport);
        } catch (IOException | GeneralSecurityException ex) {
            logger.error(ex.getLocalizedMessage());
        }
        return new Drive.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName(appName)
                .build();
    }
    
    private Credential getCredential(JsonFactory jsonFactory, HttpTransport httpTransport) throws GeneralSecurityException, IOException {
        return new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setServiceAccountId(emailAddress)
                .setServiceAccountPrivateKeyFromP12File(new File(p12Path))
                .setServiceAccountScopes(Collections.singleton(DriveScopes.DRIVE))
                .build();
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setP12Path(String p12Path) {
        this.p12Path = p12Path;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
    
    
}
