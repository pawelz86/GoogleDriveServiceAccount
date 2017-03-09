/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pawelz.pl.googledriveserviceaccount.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Permissions.Delete;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author p.zachwieja
 */
@Service
@ConfigurationProperties(prefix = "drive")
public class GoogleDriveService {
    
    @Inject
    Drive drive;
    
    private String appFolder;
    private String fieldsToGet;
            
    private final String folderMimeType = "application/vnd.google-apps.folder";
    private final String spreadsheetMimeType = "application/vnd.google-apps.spreadsheet";
    
    private static final Logger LOGGER = LogManager.getLogger(GoogleDriveService.class);
    
    /**
     * Gets file with given id
     * @param id Id of file to get
     * @param fields Fields to get, if null fields are id,webContentLink,name,webViewLink,parents,permissions
     * @return Founded file
     */
    public File getFile(String id, String fields) throws IOException {
        File result = null;
        Drive.Files.Get get;
        if(fields == null){
            fields = fieldsToGet;
        }
        try {
            get = drive.files().get(id);
            get.setFields(fields);
            result = get.execute();
        }
         catch (GoogleJsonResponseException e){
                if(e.getStatusCode() == 404){
                     LOGGER.debug("File not found: " + id);
                }
                else {
                     LOGGER.error("Google sends response error: " + e.getMessage());
                     e.printStackTrace(System.err);
                }
        }
        return result;
    }
    
    /**
     * Downloads given file
     * @param file File from Google Drive
     * @return InputStream
     */
    public InputStream downloadFile(File file) throws IOException {
        if (file.getWebContentLink() != null && !file.getWebContentLink().isEmpty()) {
            LOGGER.debug("Getting input stream for file: " + file.getId());
            return drive.files().get(file.getId()).executeMediaAsInputStream();
        }
        else {
            LOGGER.info("No content");
            // The file doesn't have any content stored on Drive.
            return null;
        }
    }
    
    /**
     * 
     * @param parentId Id of a parent
     * @return Child list
     */
    public List<File> childList(String parentId) throws IOException {
        List<File> result = null;
        try {
            FileList files = drive.files().list().setQ("fullText contains \'" +parentId+"\'").execute();
            if ((files == null) || files.isEmpty()) {
                LOGGER.warn("No child files found for id: " + parentId);
            }
            else
                result = files.getFiles();
        } catch (GoogleJsonResponseException e){
                if(e.getStatusCode() == 404){
                     LOGGER.debug("Parent file not found: " + parentId);
                }
                else {
                     LOGGER.error("Response error: " + e.getMessage());
                     e.printStackTrace(System.err);
                }
        }
        return result;
    }
    
    /**
     * Deletes file from Google Drive
     * @param fileId Id pliku do usunięcia
     * @return 0 if success
     */
    public int deleteGoogleFile(String fileId) {
        int result = -1;
        try {
            LOGGER.trace("Deleting file id: " + fileId);
            drive.files().delete(fileId).execute();
            LOGGER.trace("File deleted successfully: " + fileId);
            result = 0;
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                LOGGER.warn("No files found for " + fileId);
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            LOGGER.error("Error occurred: " + e);
        }
        return result;
    }
    
    /**
     * 
     * @param name Name of folder to create
     * @return File object
     */
    public File createFolder(String name) throws IOException {
        
        File fileMetadata = new File();
        File file = null;
        fileMetadata.setName(name);
        
        fileMetadata = setFolderMimeType(fileMetadata);
        
        try {
            fileMetadata.setParents(Arrays.asList(appFolder));
            file = drive.files().create(fileMetadata).execute();
        } 
        catch (SocketTimeoutException ex) {
            LOGGER.info("Socket timeout while creating folder " + name);
            return null;
        }

        LOGGER.info("Folder " + name + " created! ID: " + file.getId());
        return file;
    }
    
    /**
     * 
     * @param useDirectUpload useDirectUpload
     * @param multipartFile Multupart file from browser
     * @param folderId Parent folder id
     * @param desc Description for file
     * @return Google file object of uploaded MultiparFile
     * @throws IOException IOException
     */
    public File uploadFileOnly(boolean useDirectUpload, 
                            MultipartFile multipartFile,
                            String folderId,
                            String desc) throws IOException {

        File fileMetadata = new File();
        fileMetadata.setName(multipartFile.getName());
        fileMetadata.setMimeType(multipartFile.getContentType());
        fileMetadata.setOriginalFilename(multipartFile.getOriginalFilename());
        
        InputStreamContent mediaContent = new InputStreamContent(multipartFile.getContentType(),
                                                                new BufferedInputStream(multipartFile.getInputStream()));

        File p = new File();
        p.setId(folderId);
        fileMetadata = addParentReference(fileMetadata, p);
        
        if ((desc != null) && (!desc.isEmpty())) {
            fileMetadata.setDescription(desc);
        }
        
        LOGGER.debug("Uploading file to Drive");
        fileMetadata = drive.files().create(fileMetadata, mediaContent).execute();
        LOGGER.info("File uploaded, id: " + fileMetadata.getId());

        return fileMetadata;
    }
    
        /**
     * Inserts new spreadsheet on Drive
     * @param useDirectUpload useDirectUpload
     * @param folderId Parent folder id
     * @param name File name
     * @return Google file object
     * @throws IOException IOException
     */
    public File insertSpreadSheet(boolean useDirectUpload, 
                            String folderId,
                            String name) throws IOException {

        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setMimeType(spreadsheetMimeType);

        File p = getFile(folderId, null);
        fileMetadata = addParentReference(fileMetadata, p);
        LOGGER.debug("Inserting new spreadsheet");
        fileMetadata = drive.files().create(fileMetadata).execute();
        LOGGER.debug("Spreadsheet inserted");
        return fileMetadata;
    }
    
    /**
     * Creates permission object
     * @param email E-mail address
     * @param role User role, one of: writer, reader. owner is not applicable 
     * @param type Permission type, on of: user, group. domain, anyone
     * @return Created Permission object
     */
    private Permission setPermForUser(String email, String role, String type) {
        Permission result = null;
        if (correct(email)) {
            result = new Permission();
            result.set("value", email);
            result.setEmailAddress(email);
            result.setRole(role);
            result.setType(type);
        }
        return result;
    }
    
    /**
     * Adds permission for given file, filemedatada needs to be executed
     * @param file Google file
     * @param p Permission object
     * @return Google file
     */
    private File addPermissionForFile(File file, Permission p){
        List<Permission> list;
        list = file.getPermissions();
        if(list != null){
            list.add(p);
        }
        else {
            list = new ArrayList<>();
            list.add(p);
        }
        file.setPermissions(list);
        return file;
    }
    
    /**
     * Adds parent for file
     * @param file Google file
     * @param pr Parent file object
     * @return Google file with included parent
     */
    private File addParentReference(File file, File pr){
        List<String> list;
            list = file.getParents();
        if(list != null){
            list.add(pr.getId());
        }
        else{
            list = new ArrayList<>();
            list.add(pr.getId());
        }
        file.setParents(list);
        return file;
    }

    
    /**
     * Context search on files on Drive
     * @param q Text to search
     * @return List of files containing text
     */
    public List<File> searchText(String q) throws IOException {
        q="fullText contains \'" +q+"\'";
        FileList result;
        result = drive.files().list().setQ(q).execute();
        return result.getFiles();
    }

    
    /**
     * Updates readers for give file id
     * @param googleId File id
     * @param emails List of new readers
     * @return 0 when success
     * @throws java.io.IOException IOException
     */
    public int resolveReaders(String googleId, List<String> emails) throws IOException{
        List<Permission> pList;
        if((googleId == null) || googleId.isEmpty() || (emails == null))
            return -1;
        File folder = getFile(googleId, null);
        if(folder == null)
            return -1;
        Delete delete;
        pList = drive.permissions().list(folder.getId()).execute().getPermissions().
                parallelStream().filter(e -> !e.getRole().equals("owner")).collect(Collectors.toList());
        LOGGER.debug("Deleting permissions for file: " + googleId);
        for(Permission p: pList){
            removeRightsForUser(googleId, p.getId());
        }
        LOGGER.debug("Permissions deleted for file: " + googleId);
        LOGGER.debug("Inserting permissions for file: " + googleId);
        addPermissionForUsers(googleId, emails, "reader", "user", false, null);
        LOGGER.debug("Inserted permissions for file: " + googleId);
        
        return 0;
    }
    
    /**
     * Checking if given e-mail is correct
     * @param address E-mail address to check
     * @return True if correct gmail address, false otherwise
     */
    private boolean correct(String address){
        if((address == null) || address.isEmpty())
            return false;
        String t = null;
        try {
            StringTokenizer st = new StringTokenizer(address,"@");
            st.nextToken();
            t = st.nextToken();
        }
        catch(Exception e){
            LOGGER.error("Could not validate email: " + e.getLocalizedMessage());
            return false;
        }
        return t.endsWith("gmail.com") || t.endsWith("p-r.com.pl");
    }
    
    /**
     * Inserts permission for user
     * @param fileId File id
     * @param email E-mail address
     * @param role User role, one of: writer, reader. owner is not applicable 
     * @param type Permission type, on of: user, group. domain, anyone
     * @param sendEmail Should user be noticed with e-mail message from Google
     * @param message Notification e-mail message
     * @return 0 when success, -1 otherwise
     * @throws IOException IOException
     */
    public int addPermissionForUser(String fileId, String email, String role, String type, boolean sendEmail, String message) throws IOException {
        int result = -1;
        if((fileId != null) && !fileId.isEmpty() && correct(email)){
            type = type == null ? "user" : type;
            role = role == null ? "reader" : role;
            File f = getFile(fileId, null);
            LOGGER.debug("Setting permission for file " + f.getId() + " for user " + email);
            Drive.Permissions.Create insert = drive.permissions().create(f.getId(), setPermForUser(email, role, type));
            insert.setSendNotificationEmail(sendEmail);
            if((sendEmail) && (message != null) && !message.isEmpty())
                insert.setEmailMessage(message);
            insert.execute();
            LOGGER.debug("Inserted permission for file " + f.getId() + " for user " + email);
            result = 0;
        }
        return result;
    }
    
    /**
     * Inserts permissions for e-mail List
     * @param fileId File id
     * @param emails List of e-mails
     * @param role User role, one of: writer, reader. owner is not applicable 
     * @param type Permission type, on of: user, group. domain, anyone
     * @param sendEmail Should user be noticed with e-mail message from Google
     * @param message Notification e-mail message
     * @return 0 on success, -1 otherwise
     * @throws IOException IOException
     */
    public int addPermissionForUsers(String fileId, List<String> emails, String role, String type, boolean sendEmail, String message) throws IOException{
        int result = -1;
        if((fileId != null) && !fileId.isEmpty() && (emails != null) && !emails.isEmpty()){
            for(String e: emails){
                result = addPermissionForUser(fileId, e, role, type, sendEmail, message);
            }
        }
        return result;
    }
    
    
    /**
     * 
     * @param fileId Google file id
     * @param idPermission Permission id to delete
     * @return 0 on success
     * @throws IOException IOException
     */
    public int removeRightsForUser(String fileId, String idPermission) throws IOException{
        File f = getFile(fileId, null);
        Drive.Permissions.Delete insert = null;
        LOGGER.debug("Revoking permissions for file " + f.getId());
        insert = drive.permissions().delete(f.getId(), idPermission);
        insert.execute();
        LOGGER.debug("Permissions revoked for file " + f.getId());
        return 0;
    }

        
    /**
     * 
     * @param plik Google file to set folder permission
     * @return  Google file with folder mime type
     */
    private File setFolderMimeType(File file){
        file.setMimeType(folderMimeType);
        return file;
    }
    
    public void setAppFolder(String appFolder) {
        this.appFolder = appFolder;
    }

    public void setFieldsToGet(String fieldsToGet) {
        this.fieldsToGet = fieldsToGet;
    }
    
    
}
