package com.tiksem.pq;

import com.tiksem.pq.data.*;
import com.tiksem.pq.data.response.*;
import com.tiksem.pq.db.DBUtilities;
import com.tiksem.pq.db.DatabaseManager;
import com.tiksem.pq.db.OffsetLimit;
import com.tiksem.pq.db.RatingOrder;
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
import java.util.List;

/**
 * Created by CM on 10/24/2014.
 */
@Controller
@RequestMapping("/")
public class ApiHandler {
    @Autowired
    private HttpServletRequest request;

    private DatabaseManager getDatabaseManager() {
        return new DatabaseManager();
    }
    
    @RequestMapping("/")
    public String index() {
        return "redirect:/index.html";
    }

    @RequestMapping("/login")
    public @ResponseBody Object login(@RequestParam(value="login", required=true) String login,
                                      @RequestParam(value="password", required=true) String password,
                                      HttpServletResponse response){
        User user = getDatabaseManager().loginOrThrow(request, login, password);

        response.addCookie(HttpUtilities.createLocalhostUnexpiredCookie("login", login));
        response.addCookie(HttpUtilities.createLocalhostUnexpiredCookie("password", password));

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
                                      @RequestParam(value="location", required=true) String location,
                                      @RequestParam("file") MultipartFile avatar,
                                      HttpServletResponse response) throws IOException {
        User user = new User();
        user.setLogin(login);
        user.setPassword(password);
        user.setName(name);
        user.setLastName(lastName);
        user.setLocation(location);

        return getDatabaseManager().registerUser(request, user, avatar);
    }

    @RequestMapping(value = "/registerRandom", method = RequestMethod.GET)
    public @ResponseBody Object registerRandom(@RequestParam(value="count", required=true) Integer count,
                                         @RequestParam(value="startId", required=true) Integer startId,
                                         @RequestParam(value="password", required=false, defaultValue = "password")
                                         String password) throws IOException {

        List<User> users = getDatabaseManager().registerRandomUsers(request, startId, count, password);
        return new UsersList(users);
    }

    @RequestMapping("/users")
    public @ResponseBody Object getAllUsers(OffsetLimit offsetLimit) {
        Collection<User> users = getDatabaseManager().
                getAllUsersWithCheckingRelationShip(request, offsetLimit);
        return new UsersList(users);
    }

    @RequestMapping("/getUsersCount")
    public @ResponseBody Object getAllUsersCount() {
        return new CountResponse(getDatabaseManager().getAllUsersCount(request, false));
    }

    @RequestMapping("/getFriendsCount")
    public @ResponseBody Object getFriendsCount() {
        return new CountResponse(getDatabaseManager().getFriendsCount(request));
    }

    @RequestMapping("/getFriendRequestsCount")
    public @ResponseBody Object getFriendRequestsCount() {
        return new CountResponse(getDatabaseManager().getFriendRequestsCount(request));
    }

    @RequestMapping("/friends")
    public @ResponseBody Object getFriends(OffsetLimit offsetLimit) {
        Collection<User> users = getDatabaseManager().getFriends(request, offsetLimit);
        return new UsersList(users);
    }

    @RequestMapping("/deleteAllUsers")
    public @ResponseBody Object deleteAllUsers(OffsetLimit offsetLimit) {
        getDatabaseManager().deleteAllUsers(request, offsetLimit);
        return new Success();
    }

    @RequestMapping("/deleteAllPhotos")
    public @ResponseBody Object deleteAllPhotos() {
        getDatabaseManager().deleteAllPhotos();
        return new Success();
    }

    @RequestMapping("/createPhotoquest")
    public @ResponseBody Object createPhotoquest(
            @RequestParam(value = "name", required = true) String name) {
        return getDatabaseManager().createPhotoQuest(request, name);
    }

    @RequestMapping(value="/addPhotoToPhotoQuest", method= RequestMethod.POST)
    public @ResponseBody Object addPhotoToPhotoQuest(@RequestParam(value = "photoquest", required = true) Long id,
                                                 @RequestParam(value = "file", required = true) MultipartFile file)
            throws IOException {
        DatabaseManager databaseManager = getDatabaseManager();
        return databaseManager.addPhotoToPhotoquest(request, id, file);
    }

    @RequestMapping(value = Photo.IMAGE_URL_PATH + "{id}", method = RequestMethod.GET,
            produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody Object
    getImageById(@PathVariable Long id,
                 @RequestParam(value = "width", required = false) Integer width,
                 @RequestParam(value = "height", required = false) Integer height)
            throws IOException {
        try {
            byte[] image = getDatabaseManager().getBitmapDataByPhotoIdOrThrow(id);

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
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping("/getPhotoquests")
    public @ResponseBody Object getPhotoquests(OffsetLimit offsetLimit,
                                               @RequestParam(value = "order", required = false, defaultValue = "newest")
                                               RatingOrder order){
        final Collection<Photoquest> photoquests = getDatabaseManager().getPhotoQuests(request, offsetLimit, order);
        return new PhotoquestsList(photoquests);
    }

    @RequestMapping("/getPhotoquestsCount")
    public @ResponseBody Object getPhotoquestsCount(){
        long count = getDatabaseManager().getPhotoQuestsCount();
        return new CountResponse(count);
    }

    @RequestMapping("/getMyPhotoquests")
    public @ResponseBody Object getMyPhotoquests(
            @RequestParam(value = "order", required = false, defaultValue = "newest") RatingOrder order,
                                                 OffsetLimit offsetLimit){
        final Collection<Photoquest> photoquests = getDatabaseManager().
                getPhotoquestsCreatedBySignedInUser(request, offsetLimit, order);
        return new PhotoquestsList(photoquests);
    }

    @RequestMapping("/getMyPerformedPhotoquests")
    public @ResponseBody Object getMyPerformedPhotoquests(OffsetLimit offsetLimit){
        final Collection<Photoquest> photoquests = getDatabaseManager().
                getPhotoquestsPerformedBySignedInUser(request, offsetLimit);
        return new PhotoquestsList(photoquests);
    }

    @RequestMapping("/getPhotoquestById")
    public @ResponseBody Object getPhotoquestById(@RequestParam("id") Long id){
        return getDatabaseManager().getPhotoQuestByIdOrThrow(id);
    }

    @RequestMapping("/getPhotoById")
    public @ResponseBody Object getPhotoById(@RequestParam("id") Long id){
        DatabaseManager databaseManager = getDatabaseManager();
        return databaseManager.getPhotoAndFillInfo(request, id);
    }

    @RequestMapping("/getPhotoPosition")
    public @ResponseBody Object getPhotoPosition(@RequestParam("id") Long photoId,
                                                 @RequestParam(value = "order", required = false,
                                                         defaultValue = "newest") RatingOrder order){
        DatabaseManager databaseManager = getDatabaseManager();
        return new LongResult(databaseManager.getPhotoInPhotoquestPosition(photoId, order));
    }

    @RequestMapping("/getUserById")
    public @ResponseBody Object getUserById(@RequestParam("id") Long id){
        DatabaseManager databaseManager = getDatabaseManager();
        User user = databaseManager.getUserByIdOrThrow(id);
        databaseManager.setAvatar(request, user);
        return user;
    }

    @RequestMapping("/getUnreadMessagesCount")
    public @ResponseBody Object getUnreadMessagesCount(){
        DatabaseManager databaseManager = getDatabaseManager();
        final User user = databaseManager.getSignedInUserOrThrow(request);
        return new CountResponse(user.getUnreadMessagesCount());
    }

    @ExceptionHandler(Throwable.class)
    public @ResponseBody ExceptionResponse handleError(HttpServletRequest request, Throwable e) {
        return new ExceptionResponse(e);
    }

    @RequestMapping("/getPhotosOfPhotoquest")
    public @ResponseBody Object getPhotosOfPhotoquest(@RequestParam("id") Long photoquestId,
                                                      @RequestParam(value = "order", required = false,
                                                              defaultValue = "newest")
                                                      RatingOrder order,
                                                      OffsetLimit offsetLimit){
        Collection<Photo> photos = getDatabaseManager().
                getPhotosOfPhotoquest(request, photoquestId, offsetLimit, order);
        return new PhotosList(photos);
    }

    @RequestMapping("/getPhotosOfPhotoquestCount")
    public @ResponseBody Object getPhotosOfPhotoquestCount(@RequestParam("id") Long photoquestId){
        long count = getDatabaseManager().
                getPhotosOfPhotoquestCount(request, photoquestId);
        return new CountResponse(count);
    }

    @RequestMapping("/addFriend")
    public @ResponseBody Object addFriend(@RequestParam("id") Long id){
        getDatabaseManager().addFriend(request, id);
        return new Success();
    }

    @RequestMapping("/removeFriend")
    public @ResponseBody Object removeFriend(@RequestParam("id") Long id){
        getDatabaseManager().removeFriend(request, id);
        return new Success();
    }

    @RequestMapping("/putComment")
    public @ResponseBody Object putComment(
            @RequestParam(value = "photoId", required = false) Long photoId,
            @RequestParam(value = "message", required = true) String message,
            @RequestParam(value = "commentId", required = false) Long toCommentId) {
        checkLikeCommentParams(photoId, toCommentId);
        return getDatabaseManager().addComment(request, photoId, message, toCommentId);
    }

    @RequestMapping("/sendMessage")
    public @ResponseBody Object sendMessage(
            @RequestParam(value = "toUserId", required = true) Long toUserId,
            @RequestParam(value = "message", required = true) String message) {
        return getDatabaseManager().addMessage(request, toUserId, message);
    }

    @RequestMapping("/getAllMessages")
    public @ResponseBody Object getAllMessages(OffsetLimit offsetLimit) {
        return getDatabaseManager().getMessagesOfSignedInUser(request, offsetLimit);
    }

    @RequestMapping("/getLocationSuggestions")
    public @ResponseBody Object getLocationSuggestions(@RequestParam(
            value = "query", required = true) String query) throws IOException {
        return new LocationSuggestions(getDatabaseManager().getLocationSuggestions(query));
    }

    @RequestMapping("/messages")
    public @ResponseBody Object getMessagesWithUser(@RequestParam(value = "userId", required = true) Long userId,
                                               OffsetLimit offsetLimit) {
        DatabaseManager databaseManager = getDatabaseManager();
        User user = databaseManager.getUserByIdOrThrow(userId);
        Collection<Message> result =
                databaseManager.getDialogMessages(request, userId, offsetLimit);
        databaseManager.setAvatar(request, user);
        DialogMessages dialogMessages = new DialogMessages();
        dialogMessages.messages = result;
        dialogMessages.user = user;
        return dialogMessages;
    }

    @RequestMapping("/getDialogs")
    public @ResponseBody Object getDialogs(OffsetLimit offsetLimit) {
        Collection<Dialog> dialogs = getDatabaseManager().getDialogs(request, offsetLimit);
        return new DialogsList(dialogs);
    }

    @RequestMapping("/deleteComment")
    public @ResponseBody Object deleteComment(OffsetLimit offsetLimit,
            @RequestParam(value = "id", required = true) Long commentId) {
        getDatabaseManager().deleteComment(request, commentId, offsetLimit);
        return new Success();
    }

    @RequestMapping("/getCommentsOnPhoto")
    public @ResponseBody Object getCommentsOnPhoto(@RequestParam("photoId") Long photoId, OffsetLimit offsetLimit){
        Collection<Comment> comments = getDatabaseManager().
                getCommentsOnPhotoAndFillData(request, photoId, offsetLimit);
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

        DatabaseManager databaseManager = getDatabaseManager();
        if(photoId != null){
            return databaseManager.likePhoto(request, photoId);
        } else {
            return databaseManager.likeComment(request, commentId);
        }
    }

    @RequestMapping("/unlike")
    public @ResponseBody Object unlike(@RequestParam("id") Long likeId) {
        getDatabaseManager().unlike(request, likeId);
        return new Success();
    }

    @RequestMapping("/photos")
    public @ResponseBody Object photos(OffsetLimit offsetLimit) {
        return getDatabaseManager().getAllPhotos(request, offsetLimit);
    }

    @RequestMapping("/likes")
    public @ResponseBody Object likes(OffsetLimit offsetLimit) {
        return getDatabaseManager().getAllLikes(request, offsetLimit);
    }

    @RequestMapping("/comments")
    public @ResponseBody Object comments(OffsetLimit offsetLimit) {
        return getDatabaseManager().getAllComments(offsetLimit);
    }

    @RequestMapping("/refreshDatabase")
    public @ResponseBody Object refreshDatabase() {
        DBUtilities.enhanceClassesInPackage("com.tiksem.pq.data");
        return new Success();
    }

    @RequestMapping("/getUserStats")
    public @ResponseBody Object getUserStats() {
        return getDatabaseManager().getUserStats(request);
    }
}
