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
import com.utils.framework.io.Network;
import com.utils.framework.strings.Strings;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.IOUtils;
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
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by CM on 10/24/2014.
 */
@Controller
@RequestMapping("/")
public class ApiHandler {
    private static final Pattern TAG_PATTERN = Pattern.compile("\\w{3,20}", Pattern.UNICODE_CHARACTER_CLASS);

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
    public @ResponseBody Object getAllUsers(
            @RequestParam(value = "filter", required = false) String filter,
            OffsetLimit offsetLimit) {
        Collection<User> users;
        if(Strings.isEmpty(filter)){
            users = getDatabaseManager().
                    getAllUsersWithCheckingRelationShip(request, offsetLimit);
        } else {
            users = getDatabaseManager().searchUsers(request, filter, offsetLimit);
        }

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
        return new CountResponse(getDatabaseManager().getSignedInUserOrThrow(request).getReceivedRequestsCount());
    }

    @RequestMapping("/friends")
    public @ResponseBody Object getFriends(OffsetLimit offsetLimit) {
        Collection<User> users = getDatabaseManager().getFriends(request, offsetLimit);
        return new UsersList(users);
    }

    @RequestMapping("/followers")
    public @ResponseBody Object getFollowers(OffsetLimit offsetLimit) {
        Collection<User> users = getDatabaseManager().getFollowers(request, offsetLimit);
        return new UsersList(users);
    }

    @RequestMapping("/getFollowingUsers")
    public @ResponseBody Object getFollowingUsers(OffsetLimit offsetLimit) {
        Collection<User> users = getDatabaseManager().getFollowingUsers(request, offsetLimit);
        return new UsersList(users);
    }

    @RequestMapping("/getReceivedFriendRequests")
    public @ResponseBody Object getReceivedFriendRequests(OffsetLimit offsetLimit) {
        Collection<User> users = getDatabaseManager().getReceivedFriendRequests(request, offsetLimit);
        return new UsersList(users);
    }

    @RequestMapping("/getSentFriendRequests")
    public @ResponseBody Object getSentFriendRequests(OffsetLimit offsetLimit) {
        Collection<User> users = getDatabaseManager().getSentFriendRequests(request, offsetLimit);
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
            @RequestParam(value = "name", required = true) String name,
            @RequestParam(value = "tags", required = false, defaultValue = "") String tagsString) {
        tagsString = tagsString.toLowerCase();
        List<String> tags = Arrays.asList(tagsString.split(" +"));
        for(String tag : tags){
            if(!TAG_PATTERN.matcher(tag).matches()){
                throw new IllegalArgumentException("Wrong tag, should match [A-Za-z]{3,20} pattern");
            }
        }
        return getDatabaseManager().createPhotoQuest(request, name, tags);
    }

    @RequestMapping(value="/addPhotoToPhotoQuest", method= RequestMethod.POST)
    public @ResponseBody Object addPhotoToPhotoQuest(@RequestParam(value = "photoquest", required = true) Long id,
                                                 @RequestParam(value = "file", required = true) MultipartFile file)
            throws IOException {
        DatabaseManager databaseManager = getDatabaseManager();
        return databaseManager.addPhotoToPhotoquest(request, id, file);
    }

    @RequestMapping(value = Photo.IMAGE_URL_PATH + "{id}", method = RequestMethod.GET,
            headers = "Accept=image/jpeg, image/jpg, image/png, image/gif")
    public @ResponseBody Object
    getImageById(@PathVariable Long id,
                 @RequestParam(value = "size", required = false) Integer size,
                 OutputStream outputStream)
            throws IOException {
        try {
            InputStream inputStream = null;
            if (size == null) {
                inputStream = getDatabaseManager().getBitmapDataByPhotoIdOrThrow(id);
            } else {
                inputStream = getDatabaseManager().getThumbnailByPhotoIdOrThrow(id, size);
            }

            byte[] image = Network.getBytesFromStream(inputStream);
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
                                               RatingOrder order,
                                               @RequestParam(value = "filter", required = false) String filter){
        final Collection<Photoquest> photoquests;
        if(Strings.isEmpty(filter)){
            photoquests = getDatabaseManager().getPhotoQuests(request, offsetLimit, order);
        } else {
            photoquests = getDatabaseManager().searchPhotoquests(request, filter, offsetLimit);
        }
        return new PhotoquestsList(photoquests);
    }

    @RequestMapping("/getCreatedPhotoquests")
    public @ResponseBody Object getCreatedPhotoquests(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "order", required = false, defaultValue = "newest") RatingOrder order,
            OffsetLimit offsetLimit){
        Collection<Photoquest> photoquests;
        if(userId != null){
            photoquests = getDatabaseManager().getPhotoquestsCreatedByUser(request, userId, offsetLimit, order);
        } else {
            photoquests = getDatabaseManager().getPhotoquestsCreatedBySignedInUser(request, offsetLimit, order);
        }

        return new PhotoquestsList(photoquests);
    }

    @RequestMapping("/getCreatedPhotoquestsCount")
    public @ResponseBody Object getCreatedPhotoquestsCount(
            @RequestParam(value = "userId", required = false) Long userId){
        long count;
        if(userId != null){
            count = getDatabaseManager().getPhotoquestsCreatedByUserCount(userId);
        } else {
            count = getDatabaseManager().getPhotoquestsCreatedBySignedInUserCount(request);
        }

        return new CountResponse(count);
    }

    @RequestMapping("/getFollowingPhotoquests")
    public @ResponseBody Object getFollowingPhotoquests(
            @RequestParam(value = "order", required = false, defaultValue = "newest") RatingOrder order,
            OffsetLimit offsetLimit){
        final Collection<Photoquest> photoquests =
                getDatabaseManager().getFollowingPhotoquests(request, offsetLimit, order);
        return new PhotoquestsList(photoquests);
    }

    @RequestMapping("/getFollowingPhotoquestsCount")
    public @ResponseBody Object getFollowingPhotoquestsCount(){
        long count = getDatabaseManager().getFollowingPhotoquestsCount(request);
        return new CountResponse(count);
    }

    @RequestMapping("/followQuest")
    public @ResponseBody Object followQuest(@RequestParam("questId") Long questId){
        getDatabaseManager().followPhotoquest(request, questId);
        return new Success();
    }

    @RequestMapping("/unfollowQuest")
    public @ResponseBody Object unfollowQuest(@RequestParam("questId") Long questId){
        getDatabaseManager().unfollowPhotoquest(request, questId);
        return new Success();
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

    @RequestMapping("/initDatabase")
    public @ResponseBody Object initDatabase() {
        getDatabaseManager().initDatabase();
        return new Success();
    }

    @RequestMapping("/getUserStats")
    public @ResponseBody Object getUserStats() {
        return getDatabaseManager().getUserStats(request);
    }

    @RequestMapping("/getReplies")
    public @ResponseBody Object getReplies(OffsetLimit offsetLimit) {
        return new RepliesList(getDatabaseManager().getRepliesWithFullInfo(request, offsetLimit));
    }

    @RequestMapping("/getRepliesCount")
    public @ResponseBody Object getRepliesCount() {
        return new CountResponse(getDatabaseManager().getRepliesCount(request));
    }

    @RequestMapping("/getNews")
    public @ResponseBody Object getNews(
            @RequestParam(value = "userId", required = false) Long userId,
            OffsetLimit offsetLimit) {
        Collection<Feed> news;
        if(userId == null){
            news = getDatabaseManager().getNews(request, offsetLimit);
        } else {
            news = getDatabaseManager().getUserNews(request, userId, offsetLimit);
        }

        return new FeedList(news);
    }

    @RequestMapping("/getNewsCount")
    public @ResponseBody Object getNewsCount(
            @RequestParam(value = "userId", required = false) Long userId) {
        long count;
        if(userId == null){
            count = getDatabaseManager().getNewsCount(request);
        } else {
            count = getDatabaseManager().getUserNewsCount(userId);
        }

        return new CountResponse(count);
    }
}
