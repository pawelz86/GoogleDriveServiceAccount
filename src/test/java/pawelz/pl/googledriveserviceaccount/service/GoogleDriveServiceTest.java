/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pawelz.pl.googledriveserviceaccount.service;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author pawelz
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("dev")
@ContextConfiguration(locations = {
    "/TestContext.xml", 
}, initializers = ConfigFileApplicationContextInitializer.class)
@SpringBootTest
public class GoogleDriveServiceTest {
    
    @Inject 
    private GoogleDriveService googleDriveService;
    
    private String myTestFolder;
    
    private final String myEmail = "willtrywiththis@gmail.com";
    
    public GoogleDriveServiceTest() {
    }
    
    @BeforeClass
    public static void setUpClass(){
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() throws IOException {
        myTestFolder = googleDriveService.createFolder("myTestFolder").getId();
    }
    
    @After
    public void tearDown() throws IOException {
        googleDriveService.deleteGoogleFile(myTestFolder);
        
    }

   @Test
    public void testGetFile() throws IOException {
        File f = googleDriveService.getFile(myTestFolder, null);
        assertNotNull(f);
    }

    /**
     * Test of childList method, of class GoogleDriveService.
     */
    @Test
    public void testChildList() throws IOException {
        List<File> files = googleDriveService.childList(myTestFolder);
        assertNotNull(files);
    }

    /**
     * Test of createFolder method, of class GoogleDriveService.
     */
    @Test
    public void testCreateFolder() throws IOException {
        String name = "test";
        File result = googleDriveService.createFolder(name);
        assertNotNull(result);
        googleDriveService.deleteGoogleFile(result.getId());
    }

    /**
     * Test of uploadFileOnly method, of class GoogleDriveService.
     * @throws java.lang.Exception
     */
    @Test
    public void testUploadFileOnly() throws Exception {
        MultipartFile multipartFile = new MockMultipartFile("data", "filename.txt", "text/plain", "some xml".getBytes());
        File result = googleDriveService.uploadFileOnly(true, multipartFile, myTestFolder, "");
        assertNotNull(result);
        googleDriveService.deleteGoogleFile(result.getId());
    }

    /**
     * Test of insertSpreadSheet method, of class GoogleDriveService.
     * @throws java.lang.Exception
     */
    @Test
    public void testInsertSpreadSheet() throws Exception {
        File result = googleDriveService.insertSpreadSheet(true, myTestFolder, "test");
        assertNotNull(result);
        googleDriveService.deleteGoogleFile(result.getId());
    }

    /**
     * Test of searchText method, of class GoogleDriveService.
     */
    @Test
    public void testSearchText() throws IOException, InterruptedException {
        List<File> check = googleDriveService.searchText("aslkdjlzkxjclkzxc");
        if((check != null) && (!check.isEmpty())){
            for(File f: check)
            googleDriveService.deleteGoogleFile(f.getId());
        }
        assertEquals(0, googleDriveService.searchText("aslkdjlzkxjclkzxc").size());
        MultipartFile multipartFile = new MockMultipartFile("data", "filename.txt", "text/plain", "aslkdjlzkxjclkzxc".getBytes());
        File result = googleDriveService.uploadFileOnly(true, multipartFile, myTestFolder, "");
        assertNotNull(result);
//        Thread.sleep(5000);
        assertTrue(0 < googleDriveService.searchText("aslkdjlzkxjclkzxc").size());
        assertTrue(0 == googleDriveService.searchText(null).size());
        googleDriveService.deleteGoogleFile(result.getId());
    }

    /**
     * Test of resolveReaders method, of class GoogleDriveService.
     * @throws java.io.IOException
     */
    @Test
    public void testResolveReaders() throws IOException {
        
        googleDriveService.resolveReaders(null, null);
        googleDriveService.resolveReaders("", new ArrayList<>());
        googleDriveService.resolveReaders(myTestFolder, new ArrayList<>());
        
        assertTrue(googleDriveService.resolveReaders(myTestFolder, new ArrayList<>()) == 0);
        assertTrue(getPermissions(myTestFolder, myEmail).isEmpty());
        assertTrue(googleDriveService.resolveReaders(myTestFolder, Arrays.asList(myEmail)) == 0);
        assertTrue(getPermissions(myTestFolder, myEmail).size() > 0);
    }

    /**
     * Test of addPermissionForUser method, of class GoogleDriveService.
     */
    @Test
    public void testAddPermissionForUser() throws Exception {
        googleDriveService.addPermissionForUser(null, null, null, null, false, null);
        googleDriveService.addPermissionForUser("", null, null, null, false, null);
        googleDriveService.addPermissionForUser(null, "", null, null, false, null);
        googleDriveService.addPermissionForUser("", "", null, null, false, null);

        removePerms(myTestFolder, myEmail);
        assertTrue(0 == getPermissions(myTestFolder, myEmail).size());

        assertTrue(googleDriveService.addPermissionForUser(myTestFolder, myEmail, null, null, false, null) == 0);

        assertTrue(getPermissions(myTestFolder, myEmail).size() == 1);
    }

    /**
     * Test of addPermissionForUsers method, of class GoogleDriveService.
     * @throws java.io.IOException
     */
    @Test
    public void testAddPermissionForUsers() throws IOException {
        googleDriveService.addPermissionForUsers(null, null, null, null, false, null);
        googleDriveService.addPermissionForUsers("", null, null, null, false, null);
        googleDriveService.addPermissionForUsers(null, new ArrayList<>(), null, null, false, null);
        googleDriveService.addPermissionForUsers("", new ArrayList<>(), null, null, false, null);
        
        removePerms(myTestFolder, myEmail);
        assertTrue(0 == getPermissions(myTestFolder, myEmail).size());
        assertTrue(googleDriveService.addPermissionForUsers(myTestFolder, Arrays.asList(myEmail), null, null, false, null) == 0);

        assertTrue(getPermissions(myTestFolder, myEmail).size() == 1);
    }
    
    private void removePerms(String id, String email) throws IOException{
        List<Permission> pList = getPermissions(id, email);
        if (pList.size() > 0)
            googleDriveService.removeRightsForUser(id, pList.get(0).getId());
    }
    
    private List<Permission> getPermissions(String file, String email) throws IOException{
        return googleDriveService.getFile(file, null).getPermissions().parallelStream().filter(e -> e.getEmailAddress().equals(email)).collect(Collectors.toList());
    }
}
