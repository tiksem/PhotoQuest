package com.tiksem.pq;

import com.tiksem.pq.data.*;
import com.tiksem.pq.data.response.CommentsList;
import com.tiksem.pq.data.response.PhotoquestsList;
import com.tiksem.pq.data.response.PhotosList;
import com.tiksem.pq.db.DatabaseManager;
import com.tiksem.pq.db.exceptions.FileIsEmptyException;
import com.tiksem.pq.db.exceptions.PhotoquestNotFoundException;
import com.tiksem.pq.db.exceptions.ResourceNotFoundException;
import com.tiksem.pq.http.HttpUtilities;
import com.tiksem.pq.image.ScaleType;
import com.tiksem.pq.utils.MimeTypeUtils;
import net.coobird.thumbnailator.Thumbnailator;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.crypto.Data;
import java.awt.image.BufferedImage;
import java.io.*;
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
        return "redirect:/index.html";
    }

    @RequestMapping("/login")
    public @ResponseBody Object login(@RequestParam(value="login", required=true) String login,
                                      @RequestParam(value="password", required=true) String password,
                                      HttpServletResponse response){
        User user = DatabaseManager.getInstance().loginOrThrow(request, login, password);

        response.addCookie(new Cookie("login", login));
        response.addCookie(new Cookie("password", password));
        return user;
    }

    @RequestMapping("/logout")
    @ResponseBody public Object logout(HttpServletResponse response) {
        HttpUtilities.removeCookie(response, "login");
        HttpUtilities.removeCookie(response, "password");
        return new Success();
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
        Collection<User> users = DatabaseManager.getInstance().getAllUsersWithCheckingRelationShip(request);
        return new UsersList(users);
    }

    @RequestMapping("/friends")
    public @ResponseBody Object getFriends() {
        Collection<User> users = DatabaseManager.getInstance().getFriends(request);
        return new UsersList(users);
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
    public @ResponseBody Object addPhotoToPhotoQuest(@RequestParam(value = "photoquest", required = true) Long id,
                                                 @RequestParam(value = "file", required = true) MultipartFile file)
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
    public @ResponseBody HttpEntity<byte[]>
    getImageById(@PathVariable Long id,
                 @RequestParam(value = "width", required = false) Integer width,
                 @RequestParam(value = "height", required = false) Integer height)
            throws IOException {
        byte[] image = DatabaseManager.getInstance().getBitmapDataByPhotoIdOrThrow(id);



        if(width != null || height != null) {
            InputStream in = new ByteArrayInputStream(image);
            BufferedImage bufferedImage = ImageIO.read(in);

            if(width == null){
                width = bufferedImage.getWidth();
            } else if(height == null) {
                height = bufferedImage.getHeight();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(Thumbnails.of(bufferedImage).size(width, height).asBufferedImage(), "jpg", baos);
            image = baos.toByteArray();
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

    @RequestMapping("/addFriend")
    public @ResponseBody Object addFriend(@RequestParam("id") Long id){
        DatabaseManager.getInstance().addFriend(request, id);
        return new Success();
    }

    @RequestMapping("/removeFriend")
    public @ResponseBody Object removeFriend(@RequestParam("id") Long id){
        DatabaseManager.getInstance().removeFriend(request, id);
        return new Success();
    }

    @RequestMapping("/putComment")
    public @ResponseBody Object putComment(
            @RequestParam(value = "photoId", required = true) Long photoId,
            @RequestParam(value = "message", required = true) String message,
            @RequestParam(value = "toCommentId", required = false) Long toCommentId) {
        return DatabaseManager.getInstance().addComment(request, photoId, message, toCommentId);
    }

    @RequestMapping("/getCommentsOnPhoto")
    public @ResponseBody Object getCommentsOnPhoto(@RequestParam("photoId") Long photoId){
        Collection<Comment> comments = DatabaseManager.getInstance().
                getCommentsOnPhotoAndFillData(request, photoId);
        return new CommentsList(comments);
    }
}
