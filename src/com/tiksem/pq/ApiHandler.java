package com.tiksem.pq;

import com.tiksem.pq.data.Photo;
import com.tiksem.pq.data.Photoquest;
import com.tiksem.pq.data.Success;
import com.tiksem.pq.data.User;
import com.tiksem.pq.data.response.PhotoquestsList;
import com.tiksem.pq.db.DatabaseManager;
import com.tiksem.pq.db.exceptions.FileIsEmptyException;
import com.tiksem.pq.db.exceptions.PhotoquestNotFoundException;
import com.tiksem.pq.db.exceptions.ResourceNotFoundException;
import com.tiksem.pq.utils.MimeTypeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by CM on 10/24/2014.
 */
@Controller
@RequestMapping("/")
public class ApiHandler {
    @Autowired
    private HttpServletRequest request;

    @RequestMapping("/")
    public String index() {
        return "redirect:/login_page.html";
    }

    @RequestMapping("/login")
    public @ResponseBody Object login(@RequestParam(value="login", required=true) String login,
                                      @RequestParam(value="password", required=true) String password,
                                      HttpServletResponse response){
        User user = DatabaseManager.getInstance().loginOrThrow(login, password);

        response.addCookie(new Cookie("login", login));
        response.addCookie(new Cookie("password", password));
        return user;
    }

    @RequestMapping("/register")
    public @ResponseBody Object register(@RequestParam(value="login", required=true) String login,
                                      @RequestParam(value="password", required=true) String password,
                                      HttpServletResponse response) {
        User user = new User();
        user.setLogin(login);
        user.setPassword(password);

        return DatabaseManager.getInstance().registerUser(user);
    }

    @RequestMapping("/users")
    public @ResponseBody Object getAllUsers() {
        return DatabaseManager.getInstance().getAllUsers();
    }

    @RequestMapping("/deleteAllUsers")
    public @ResponseBody Object deleteAllUsers() {
        DatabaseManager.getInstance().deleteAllUsers();
        return new Success();
    }

    @RequestMapping("/createPhotoquest")
    public @ResponseBody Object createPhotoquest(
            @RequestParam(value = "name", required = true) String name) {
        return DatabaseManager.getInstance().createPhotoQuest(request, name);
    }

    @RequestMapping(value="/addPhotoToPhotoQuest", method= RequestMethod.POST)
    public @ResponseBody Object addPhotoToPhotoQuest(@RequestParam("photoquest") String photoquestName,
                                                 @RequestParam("file") MultipartFile file)
            throws IOException {
        DatabaseManager databaseManager = DatabaseManager.getInstance();

        Photoquest photoquest = databaseManager.getPhotoQuestByName(photoquestName);
        if(photoquest == null){
            throw new PhotoquestNotFoundException(photoquestName);
        }

        if (!file.isEmpty()) {
            Photo photo = new Photo();
            byte[] bytes = file.getBytes();
            photo.setImage(bytes);
            photo = databaseManager.addPhoto(photo);
            long photoquestId = photoquest.getId();
            long photoId = photo.getId();
            databaseManager.addPhotoToPhotoquest(photoId, photoquestId);
        } else {
            throw new FileIsEmptyException();
        }

        return new Success();
    }

    @RequestMapping(value = Photo.IMAGE_URL_PATH + "{id}", method = RequestMethod.GET,
            produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody HttpEntity<byte[]> getImageById(@PathVariable Long id)
            throws IOException {
        Photo photo = DatabaseManager.getInstance().getPhotoById(id);
        if(photo == null){
            throw new ResourceNotFoundException();
        }

        byte[] image = photo.getImage();
        if(image == null){
            throw new ResourceNotFoundException();
        }

        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = MimeTypeUtils.getMediaTypeFromByteArray(image);
        headers.setContentType(mediaType);
        headers.setContentLength(image.length);
        return new HttpEntity<byte[]>(image, headers);
    }

    @RequestMapping("/getPhotoquests")
    public @ResponseBody Object getPhotoquests(){
        final Collection<Photoquest> photoquests = DatabaseManager.getInstance().getPhotoQuests(request);
        return new PhotoquestsList(photoquests);
    }
}
