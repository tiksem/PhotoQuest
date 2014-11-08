package com.tiksem.pq;

import com.tiksem.pq.data.*;
import com.tiksem.pq.data.response.PhotoquestsList;
import com.tiksem.pq.data.response.PhotosList;
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

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public @ResponseBody Object register(@RequestParam(value="login", required=true) String login,
                                      @RequestParam(value="password", required=true) String password,
                                      @RequestParam(value="name", required=true) String name,
                                      @RequestParam(value="lastName", required=true) String lastName,
                                      @RequestParam("file") MultipartFile avatar,
                                      HttpServletResponse response) {
        User user = new User();
        user.setLogin(login);
        user.setPassword(password);
        user.setName(name);
        user.setLastName(lastName);

        DatabaseManager databaseManager = DatabaseManager.getInstance();
        user = databaseManager.registerUser(user);

        if (!avatar.isEmpty()) {
            Photo photo = new Photo();
            Photoquest avatarPhotoQuest = databaseManager.
                    getOrCreateSystemPhotoQuest(DatabaseManager.AVATAR_QUEST_NAME);
            photo.setPhotoquestId(avatarPhotoQuest.getId());
            photo.setUserId(user.getId());
            try {
                byte[] bytes = avatar.getBytes();
                photo = databaseManager.addPhoto(request, photo, bytes);
                user.setAvatarId(photo.getId());

                DatabaseManager.getInstance().update(request, user);
            } catch (IOException e) {

            }
        }

        return user;
    }

    @RequestMapping("/users")
    public @ResponseBody Object getAllUsers() {
        return DatabaseManager.getInstance().getAllUsers(request);
    }

    @RequestMapping("/deleteAllUsers")
    public @ResponseBody Object deleteAllUsers() {
        DatabaseManager.getInstance().deleteAllUsers(request);
        return new Success();
    }

    @RequestMapping("/deleteAllPhotos")
    public @ResponseBody Object deleteAllPhotos() {
        DatabaseManager.getInstance().deleteAllPhotos();
        return new Success();
    }

    @RequestMapping("/createPhotoquest")
    public @ResponseBody Object createPhotoquest(
            @RequestParam(value = "name", required = true) String name) {
        return DatabaseManager.getInstance().createPhotoQuest(request, name);
    }

    @RequestMapping(value="/addPhotoToPhotoQuest", method= RequestMethod.POST)
    public @ResponseBody Object addPhotoToPhotoQuest(@RequestParam("photoquest") Long id,
                                                 @RequestParam("file") MultipartFile file)
            throws IOException {
        DatabaseManager databaseManager = DatabaseManager.getInstance();

        Photoquest photoquest = databaseManager.getPhotoQuestByIdOrThrow(id);

        if (!file.isEmpty()) {
            Photo photo = new Photo();
            photo.setPhotoquestId(id);
            byte[] bytes = file.getBytes();
            databaseManager.addPhoto(request, photo, bytes);
        } else {
            throw new FileIsEmptyException();
        }

        return new Success();
    }

    @RequestMapping(value = Photo.IMAGE_URL_PATH + "{id}", method = RequestMethod.GET,
            produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody HttpEntity<byte[]> getImageById(@PathVariable Long id)
            throws IOException {
        byte[] image = DatabaseManager.getInstance().getBitmapDataByPhotoIdOrThrow(id);

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

    @RequestMapping("/getPhotoquestById")
    public @ResponseBody Object getPhotoquestById(@RequestParam("id") Long id){
        return DatabaseManager.getInstance().getPhotoQuestByIdOrThrow(id);
    }

    @ExceptionHandler(Throwable.class)
    public @ResponseBody ExceptionResponse handleError(HttpServletRequest request, Throwable e) {
        return new ExceptionResponse(e);
    }

    @RequestMapping("/getPhotosOfPhotoquest")
    public @ResponseBody Object getPhotosOfPhotoquest(@RequestParam("id") Long photoquestId){
        Collection<Photo> photos = DatabaseManager.getInstance().getPhotosOfPhotoquest(request, photoquestId);
        return new PhotosList(photos);
    }
}
