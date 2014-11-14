package com.tiksem.pq;

import com.tiksem.pq.data.*;
import com.tiksem.pq.data.response.CommentsList;
import com.tiksem.pq.data.response.PhotoquestsList;
import com.tiksem.pq.data.response.PhotosList;
import com.tiksem.pq.data.response.UsersList;
import com.tiksem.pq.db.DatabaseManager;
import com.tiksem.pq.db.exceptions.FileIsEmptyException;
import com.tiksem.pq.http.HttpUtilities;
import com.tiksem.pq.utils.MimeTypeUtils;
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
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Collection;

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

        InputStream in = new ByteArrayInputStream(image);
        BufferedImage bufferedImage = ImageIO.read(in);

        if(width == null){
            width = bufferedImage.getWidth();
        }
        if(height == null) {
            height = bufferedImage.getHeight();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(Thumbnails.of(bufferedImage).size(width, height).asBufferedImage(), "png", baos);
        image = baos.toByteArray();

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

    @RequestMapping("/getPhotoById")
    public @ResponseBody Object getPhotoById(@RequestParam("id") Long id){
        Photo photo = DatabaseManager.getInstance().getPhotoByIdOrThrow(id);
        DatabaseManager.getInstance().initYourLikeParameter(request, photo);
        return photo;
    }

    @RequestMapping("/getUserById")
    public @ResponseBody Object getUserById(@RequestParam("id") Long id){
        User user = DatabaseManager.getInstance().getUserByIdOrThrow(id);
        DatabaseManager.getInstance().setAvatar(request, user);
        return user;
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
            @RequestParam(value = "photoId", required = false) Long photoId,
            @RequestParam(value = "message", required = true) String message,
            @RequestParam(value = "commentId", required = false) Long toCommentId) {
        checkLikeCommentParams(photoId, toCommentId);
        return DatabaseManager.getInstance().addComment(request, photoId, message, toCommentId);
    }

    @RequestMapping("/sendMessage")
    public @ResponseBody Object sendMessage(
            @RequestParam(value = "toUserId", required = true) Long toUserId,
            @RequestParam(value = "message", required = true) String message) {
        return DatabaseManager.getInstance().addMessage(request, toUserId, message);
    }

    @RequestMapping("/getMessages")
    public @ResponseBody Object getMessages() {
        return DatabaseManager.getInstance().getMessagesOfSignedInUser(request);
    }

    @RequestMapping("/deleteComment")
    public @ResponseBody Object deleteComment(
            @RequestParam(value = "id", required = true) Long commentId) {
        DatabaseManager.getInstance().deleteComment(request, commentId);
        return new Success();
    }

    @RequestMapping("/getCommentsOnPhoto")
    public @ResponseBody Object getCommentsOnPhoto(@RequestParam("photoId") Long photoId){
        Collection<Comment> comments = DatabaseManager.getInstance().
                getCommentsOnPhotoAndFillData(request, photoId);
        return new CommentsList(comments);
    }

    private void checkLikeCommentParams(Long photoId, Long commentId) {
        if((photoId == null && commentId == null) || (commentId != null && photoId != null)){
            throw new IllegalArgumentException("Specify one, photoId or commentId");
        }
    }

    @RequestMapping("/like")
    public @ResponseBody Object like(@RequestParam(value = "photoId", required = false) Long photoId,
                                     @RequestParam(value = "commentId", required = false) Long commentId){
        checkLikeCommentParams(photoId, commentId);

        if(photoId != null){
            return DatabaseManager.getInstance().likePhoto(request, photoId);
        } else {
            return DatabaseManager.getInstance().likeComment(request, commentId);
        }
    }

    @RequestMapping("/unlike")
    public @ResponseBody Object unlike(@RequestParam("id") Long likeId) {
        DatabaseManager.getInstance().unlike(request, likeId);
        return new Success();
    }

    @RequestMapping("/photos")
    public @ResponseBody Object photos() {
        return DatabaseManager.getInstance().getAllPhotos(request);
    }

    @RequestMapping("/likes")
    public @ResponseBody Object likes() {
        return DatabaseManager.getInstance().getAllLikes(request);
    }

    @RequestMapping("/comments")
    public @ResponseBody Object comments() {
        return DatabaseManager.getInstance().getAllComments();
    }
}
