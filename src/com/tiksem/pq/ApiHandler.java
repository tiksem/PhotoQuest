package com.tiksem.pq;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.mysqljava.OffsetLimit;
import com.tiksem.pq.data.*;
import com.tiksem.pq.data.response.*;
import com.tiksem.pq.data.response.android.*;
import com.tiksem.pq.db.*;
import com.tiksem.pq.db.advanced.SearchUsersParams;
import com.tiksem.pq.http.HttpUtilities;
import com.utils.framework.CollectionUtils;
import com.utils.framework.Reflection;
import com.utils.framework.io.IOUtilities;
import com.utils.framework.strings.Strings;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by CM on 10/24/2014.
 */
@Controller
@RequestMapping("/")
public class ApiHandler {
    private static final Pattern TAG_PATTERN = Pattern.compile("[\\w]{3,20}", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Set<String> uncheckedRequests = new HashSet<String>();

    static {
        List<String> list = Arrays.asList("initDatabase", "clearDatabase");
        for(String request : list){
            uncheckedRequests.add("//" + request);
            uncheckedRequests.add("/" + request);
        }
    }

    private static final int MAX_PHOTO_DESCRIPTION_LENGTH = 200;
    public static final int MAX_NAME_LENGTH = 20;
    public static final int MAX_LASTNAME_LENGTH = 40;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpSession httpSession;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ServletContext servletContext;

    private String getLang() {
        String language = LocaleContextHolder.getLocale().getLanguage();
        return Settings.getInstance().getLang(language);
    }

    private DatabaseManager getDatabaseManager() {
        Settings settings = Settings.getInstance();
        long delay = settings.getRequestDelay();
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        MysqlObjectMapper mapper = getMapper();
        String requestURI = request.getRequestURI();
        if(settings.isEnableRPS() && !uncheckedRequests.contains(requestURI)){
            settings.getRpsGuard().commitUserRequest(mapper, request.getRemoteAddr(),
                    requestURI);
        }

        Cookie langCookie = HttpUtilities.getCookie("lang", request.getCookies());
        String lang = langCookie == null ? getLang() : langCookie.getValue();

        final DatabaseManager databaseManager = new DatabaseManager(request, mapper, lang);
        RequestContextHolder.currentRequestAttributes().
                registerDestructionCallback("DatabaseManager", new Runnable() {
                    @Override
                    public void run() {
                        databaseManager.destroy();
                    }
                }, 0);
        return databaseManager;
    }

    public void checkAdminPermissions(DatabaseManager databaseManager) {
        if(!Settings.getInstance().getBoolean("debug")){
            databaseManager.checkAdminPermissions();
        }
    }

    private boolean isMobileClient() {
        Cookie mobile = HttpUtilities.getCookie("mobile", request.getCookies());
        return mobile != null && mobile.getValue().equals("true");
    }

    private Object getUsersResponse(Collection<User> users) {
        return getUsersResponse(users, null, null, null);
    }

    private Object getPhotosResponse(Collection<Photo> photos) {
        if(isMobileClient()){
            return new MobilePhotoList(photos);
        } else {
            return new PhotosList(photos);
        }
    }

    private Object getDialogsResponse(Collection<Dialog> dialogs) {
        if(isMobileClient()){
            return new MobileDialogList(dialogs);
        } else {
            return new DialogsList(dialogs);
        }
    }

    private Object getMessagesResponse(DatabaseManager databaseManager, Collection<Message> messages) {
        User user = databaseManager.getSignedInUserOrThrow();
        if(isMobileClient()){
            return new MobileMessagesList(messages, user);
        } else {
            databaseManager.setAvatar(user);
            DialogMessages dialogMessages = new DialogMessages();
            dialogMessages.messages = messages;
            dialogMessages.user = user;
            return dialogMessages;
        }
    }

    private Object getSendMessageResponse(Message message) {
        if(isMobileClient()){
            return new MobileMessage(message, true);
        } else {
            return message;
        }
    }

    private Object getRepliesResponse(Collection<ReplyResponse> replies) {
        if(isMobileClient()){
            return new MobileReplyList(replies);
        } else {
            return new RepliesList(replies);
        }
    }

    private Object getUserResponse(User user) {
        if(isMobileClient()){
            return new MobileUser(user);
        }

        return user;
    }

    private Object getUsersResponse(Collection<User> users, DatabaseManager databaseManager,
                                    Integer countryId, Integer cityId) {
        if(isMobileClient()){
            return new MobileUserList(users);
        } else {
            UsersList usersList = new UsersList(users);
            if (cityId != null) {
                usersList.location = databaseManager.getLocation(cityId);
            } else if(countryId != null) {
                usersList.location = new DatabaseManager.Location();
                usersList.location.countryId = countryId;
                usersList.location.countryName = databaseManager.getCountryByIdOrThrow(countryId).getEnName();
            }

            return usersList;
        }
    }

    private Object getPhotoquestsResponse(Collection<Photoquest> photoquests) {
        if(isMobileClient()){
            return new MobilePhotoquestList(photoquests);
        } else {
            return new PhotoquestsList(photoquests);
        }
    }

    private Object getFeedResponse(Collection<Action> actions) {
        if(isMobileClient()){
            return new MobileFeedList(actions);
        } else {
            return new FeedList(actions);
        }
    }

    static String indexHtml = null;

    @RequestMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String index() {
        if(indexHtml == null){
            String indexHtmlPath = servletContext.getRealPath("/WEB-INF/resources/index.html");
            try {
                indexHtml = IOUtilities.readStringFromUrl(indexHtmlPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return indexHtml;
    }

    @RequestMapping("/login")
    public @ResponseBody Object login(@RequestParam(value="login", required=true) String login,
                                      @RequestParam(value="password", required=true) String password,
                                      @RequestParam(value="mobile", required=false, defaultValue = "false")
                                      boolean mobile,
                                      HttpServletResponse response){
        User user = getDatabaseManager().loginOrThrow(login, password);

        setLoginCookies(response, user);

        if(mobile){
            response.addCookie(HttpUtilities.createLocalhostUnexpiredCookie("mobile", "true"));
        }

        return user;
    }

    @RequestMapping("/logout")
    @ResponseBody public Object logout(HttpServletResponse response) {
        HttpUtilities.removeCookie(response, "login");
        HttpUtilities.removeCookie(response, "password");
        return new Success();
    }

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public @ResponseBody Object register(@RequestParam(value="login", required=true) String login,
                                      @RequestParam(value="password", required=true) String password,
                                      @RequestParam(value="name", required=true) String name,
                                      @RequestParam(value="lastName", required=true) String lastName,
                                      @RequestParam(value="cityId", required=true) Integer cityId,
                                      @RequestParam(value="gender", required=true) boolean gender,
                                      @RequestParam(value="about", required=false) String about,
                                      @RequestParam(value="captcha", required=true) long captcha,
                                      @RequestParam(value="answer", required=true) String answer)
            throws IOException {
        DatabaseManager databaseManager = getDatabaseManager();
        databaseManager.checkCaptcha(captcha, answer);

        if(name.length() > MAX_NAME_LENGTH){
            throw new IllegalArgumentException("name.length() > " + MAX_NAME_LENGTH);
        }

        if(lastName.length() > MAX_LASTNAME_LENGTH){
            throw new IllegalArgumentException("lastName.length() > " + MAX_LASTNAME_LENGTH);
        }

        User user = new User();
        user.setLogin(login);
        user.setPassword(password);
        user.setNameAndLastName(name, lastName);
        user.setCityId(cityId);
        user.setGender(gender);
        user.setAbout(about);

        return databaseManager.registerUser(user, (InputStream) null);
    }

    @RequestMapping(value = "/editProfile", method = RequestMethod.GET)
    public @ResponseBody Object editProfile(
                                         @RequestParam(value="name", required=false) String name,
                                         @RequestParam(value="lastName", required=false) String lastName,
                                         @RequestParam(value="cityId", required=false) Integer cityId,
                                         @RequestParam(value="about", required=false) String about)
            throws IOException {
        DatabaseManager databaseManager = getDatabaseManager();
        return databaseManager.editProfile(name, lastName, cityId, about);
    }

    @RequestMapping(value = "/changePassword", method = RequestMethod.POST)
    public @ResponseBody Object changePassword(
            @RequestParam(value="old", required=true) String oldPassword,
            @RequestParam(value="new", required=true) String newPassword,
            HttpServletResponse response)
            throws IOException {
        DatabaseManager databaseManager = getDatabaseManager();
        User user = databaseManager.changePassword(newPassword, oldPassword);

        setLoginCookies(response, user);

        return user;
    }

    private void setLoginCookies(HttpServletResponse response, User user) {
        response.addCookie(HttpUtilities.createLocalhostUnexpiredCookie("login", user.getLogin()));
        response.addCookie(HttpUtilities.createLocalhostUnexpiredCookie("password", user.getPassword()));
    }

    @RequestMapping(value = "/registerRandom", method = RequestMethod.GET)
    public @ResponseBody Object registerRandom(@RequestParam(value="count", required=true) Integer count,
                                         @RequestParam(value="startId", required=true) Integer startId,
                                         @RequestParam(value="password", required=false, defaultValue = "password")
                                         String password) throws IOException {

        DatabaseManager databaseManager = getDatabaseManager();
        checkAdminPermissions(databaseManager);
        List<User> users = databaseManager.registerRandomUsers(startId, count, password);
        return getUsersResponse(users);
    }

    @RequestMapping("/users")
    public @ResponseBody Object getAllUsers(
            SearchUsersParams searchParams,
            OffsetLimit offsetLimit,
            @RequestParam(value = "order", required = false, defaultValue = "newest")
            RatingOrder order) {
        DatabaseManager databaseManager = getDatabaseManager();
        Collection<User> users
                = databaseManager.searchUsers(searchParams, offsetLimit, order);

        return getUsersResponse(users, databaseManager, searchParams.countryId, searchParams.cityId);
    }

    @RequestMapping("/getUsersCount")
    public @ResponseBody Object getAllUsersCount(SearchUsersParams searchParams) {
        long count = getDatabaseManager().getSearchUsersCount(searchParams);

        return new CountResponse(count);
    }

    @RequestMapping("/getFriendsCount")
    public @ResponseBody Object getFriendsCount(SearchUsersParams searchParams,
                                                @RequestParam(value = "id", required = false)
                                                Long id) {
        return new CountResponse(getDatabaseManager().getFriendsCount(searchParams, id));
    }

    @RequestMapping("/getReceivedRequestsCount")
    public @ResponseBody Object getReceivedRequestsCount(SearchUsersParams searchParams) {
        return new CountResponse(getDatabaseManager().
                getReceivedRequestsCount(searchParams, null));
    }

    @RequestMapping("/getSentRequestsCount")
    public @ResponseBody Object getSentRequestsCount(SearchUsersParams searchParams) {
        return new CountResponse(getDatabaseManager().
                getSentRequestsCount(searchParams, null));
    }

    @RequestMapping("/friends")
    public @ResponseBody Object getFriends(SearchUsersParams searchParams,
                                           OffsetLimit offsetLimit,
                                           @RequestParam(value = "order", required = false, defaultValue = "newest")
                                           RatingOrder order,
                                           @RequestParam(value = "id", required = false)
                                           Long id) {
        DatabaseManager databaseManager = getDatabaseManager();
        Collection<User> users
                = databaseManager.getFriends(searchParams, offsetLimit, order, id);

        return getUsersResponse(users, databaseManager, searchParams.countryId, searchParams.cityId);
    }

    @RequestMapping("/followers")
    public @ResponseBody Object getFollowers(OffsetLimit offsetLimit) {
        Collection<User> users = getDatabaseManager().getFollowers(offsetLimit,
                RatingOrder.newest);
        return getUsersResponse(users);
    }

    @RequestMapping("/getFollowingUsers")
    public @ResponseBody Object getFollowingUsers(OffsetLimit offsetLimit) {
        Collection<User> users = getDatabaseManager().getFollowingUsers(offsetLimit,
                RatingOrder.newest);
        return getUsersResponse(users);
    }

    @RequestMapping("/getReceivedFriendRequests")
    public @ResponseBody Object getReceivedFriendRequests(SearchUsersParams searchParams,
                                                          OffsetLimit offsetLimit,
                                                          @RequestParam(value = "order", required = false,
                                                                  defaultValue = "newest")
                                                          RatingOrder order) {
        Collection<User> users = getDatabaseManager().getReceivedFriendRequests(searchParams, offsetLimit, order);
        return getUsersResponse(users);
    }

    @RequestMapping("/getSentFriendRequests")
    public @ResponseBody Object getSentFriendRequests(SearchUsersParams searchParams,
                                                      OffsetLimit offsetLimit,
                                                      @RequestParam(value = "order", required = false,
                                                              defaultValue = "newest")
                                                      RatingOrder order) {
        Collection<User> users = getDatabaseManager().getSentFriendRequests(searchParams, offsetLimit, order);
        return getUsersResponse(users);
    }

    @RequestMapping("/createPhotoquest")
    public @ResponseBody Object createPhotoquest(
            @RequestParam(value = "name", required = true) String name,
            @RequestParam(value = "follow", required = false, defaultValue = "false") boolean follow,
            @RequestParam(value = "tags", required = false, defaultValue = "") String tagsString) {
        tagsString = tagsString.toLowerCase();
        List<String> tags;
        if (!Strings.isEmpty(tagsString)) {
            tags = Arrays.asList(tagsString.split(" +"));
        } else {
            tags = Collections.emptyList();
        }
        for(String tag : tags){
            if(!TAG_PATTERN.matcher(tag).matches()){
                throw new IllegalArgumentException("Wrong tag, should match " + TAG_PATTERN.toString() + " pattern");
            }
        }

        name = name.trim();
        if(name.isEmpty()){
            throw new IllegalArgumentException("Photoquest name is empty");
        }

        return getDatabaseManager().createPhotoQuest(name, tags, follow);
    }

    @RequestMapping(value="/addPhotoToPhotoQuest", method= RequestMethod.POST)
    public @ResponseBody Object addPhotoToPhotoQuest(@RequestParam(value = "photoquest", required = true) Long id,
                                                     @RequestParam(value = "message", required = false) String message,
                                                     @RequestParam(value = "follow",
                                                             defaultValue = "false",
                                                             required = false) boolean follow,
                                                 @RequestParam(value = "file", required = true) MultipartFile file)
            throws IOException {
        if (message != null) {
            if(message.length() > MAX_PHOTO_DESCRIPTION_LENGTH) {
                throw new IllegalArgumentException("Photo description message is bigger than " +
                        MAX_PHOTO_DESCRIPTION_LENGTH);
            }
        }

        DatabaseManager databaseManager = getDatabaseManager();
        return databaseManager.addPhotoToPhotoquest(id, file, message, follow);
    }

    @RequestMapping(value="/changeAvatar", method= RequestMethod.POST)
    public @ResponseBody Object changeAvatar(@RequestParam(value = "file", required = true) MultipartFile file)
            throws IOException {
        DatabaseManager databaseManager = getDatabaseManager();
        return databaseManager.changeAvatar(file);
    }

    @RequestMapping(value = Photo.IMAGE_URL_PATH + "{id}.jpg", method = RequestMethod.GET,
            headers = "Accept=image/jpeg, image/jpg, image/png, image/gif")
    public void
    getImageById(@PathVariable Long id,
                 @RequestParam(value = "size", required = false) Integer size,
                 @RequestParam(value = "display", defaultValue = "false") boolean display,
                 OutputStream outputStream)
            throws IOException {
        InputStream inputStream = null;
        try {
            DatabaseManager databaseManager = getDatabaseManager();
            if (display) {
                inputStream = databaseManager.getDisplayImageById(id);
            } else if(size == null) {
                inputStream = databaseManager.getBitmapDataByPhotoIdOrThrow(id);
            } else {
                inputStream = databaseManager.getThumbnailByPhotoIdOrThrow(id, size);
            }

            IOUtils.copyLarge(inputStream, outputStream, new byte[1024 * 64]);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            if(inputStream != null){
                inputStream.close();
            }
        }
    }

    @RequestMapping(value = "/captcha/{id}", method = RequestMethod.GET,
            headers = "Accept=image/jpeg, image/jpg, image/png, image/gif")
    public void
    getCaptchaById(@PathVariable long id,
                 OutputStream outputStream)
            throws IOException {
        InputStream inputStream = null;
        try {
            DatabaseManager databaseManager = getDatabaseManager();
            inputStream = databaseManager.getCaptchaImage(id);
            IOUtils.copyLarge(inputStream, outputStream, new byte[1024 * 64]);
        } finally {
            if(inputStream != null){
                inputStream.close();
            }
        }
    }

    @RequestMapping("/deletePhoto")
    public @ResponseBody Object deletePhoto(@RequestParam(value = "photoquestId", required = false)
                                                Long photoquestId,
                                            @RequestParam(value = "userId", required = false) Long userId,
                                            @RequestParam("photoId") Long photoId,
                                            @RequestParam("next") boolean next,
                                            @RequestParam(value = "category", defaultValue = "all")
                                            PhotoCategory category,
                                            @RequestParam(value = "order", required = false,
                                                    defaultValue = "newest")
                                            RatingOrder order) {
        DatabaseManager databaseManager = getDatabaseManager();
        Object result = null;
        if (userId != null || photoquestId != null) {
            result = getNextPrevPhoto(photoquestId, userId, photoId, next, category, order);
        }
        databaseManager.deletePhoto(photoId);
        return result;
    }

    @RequestMapping("/setAvatar")
    public @ResponseBody Object setAvatar(@RequestParam(value = "photoId") Long id) {
        return getDatabaseManager().setAvatar(id);
    }

    @RequestMapping("/getCaptcha")
    public @ResponseBody Object getCaptcha() {
        return new Object(){
            public long id = getDatabaseManager().getNewCaptcha();
        };
    }

    @RequestMapping("/getPhotoquests")
    public @ResponseBody Object getPhotoquests(OffsetLimit offsetLimit,
                                               @RequestParam(value = "order", required = false, defaultValue = "newest")
                                               RatingOrder order,
                                               @RequestParam(value = "filter", required = false) String filter){
        final Collection<Photoquest> photoquests;
        if(Strings.isEmpty(filter)){
            photoquests = getDatabaseManager().getPhotoQuests(offsetLimit, order);
        } else {
            photoquests = getDatabaseManager().searchPhotoquests(filter, offsetLimit, order);
        }
        return getPhotoquestsResponse(photoquests);
    }

    @RequestMapping("/getCreatedPhotoquests")
    public @ResponseBody Object getCreatedPhotoquests(
            @RequestParam(value = "userId", required = true) Long userId,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "order", required = false, defaultValue = "newest") RatingOrder order,
            OffsetLimit offsetLimit){
        Collection<Photoquest> photoquests =
                getDatabaseManager().getPhotoquestsCreatedByUser(filter, userId, offsetLimit, order);

        return getPhotoquestsResponse(photoquests);
    }

    @RequestMapping("/getCreatedPhotoquestsCount")
    public @ResponseBody Object getCreatedPhotoquestsCount(
            @RequestParam(value = "userId", required = true) Long userId,
            @RequestParam(value = "filter", required = false) String filter){
        long count = getDatabaseManager().getPhotoquestsCreatedByUserCount(filter, userId);
        return new CountResponse(count);
    }

    @RequestMapping("/getPerformedPhotoquests")
    public @ResponseBody Object getPerformedPhotoquests(
            @RequestParam(value = "userId", required = true) Long userId,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "order", required = false, defaultValue = "newest") RatingOrder order,
            OffsetLimit offsetLimit){
        Collection<Photoquest> photoquests =
                getDatabaseManager().getPerformedPhotoquests(userId, offsetLimit, filter, order);

        return getPhotoquestsResponse(photoquests);
    }

    @RequestMapping("/getPerformedPhotoquestsCount")
    public @ResponseBody Object getPerformedPhotoquestsCount(
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "userId", required = true) Long userId){
        long count = getDatabaseManager().getPerformedPhotoquestsCount(filter, userId);
        return new CountResponse(count);
    }

    @RequestMapping("/getFollowingPhotoquests")
    public @ResponseBody Object getFollowingPhotoquests(
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "userId", required = true) Long userId,
            @RequestParam(value = "order", required = false, defaultValue = "newest") RatingOrder order,
            OffsetLimit offsetLimit){
        final Collection<Photoquest> photoquests =
                getDatabaseManager().getFollowingPhotoquests(userId, filter, offsetLimit, order);
        return getPhotoquestsResponse(photoquests);
    }

    @RequestMapping("/getFollowingPhotoquestsCount")
    public @ResponseBody Object getFollowingPhotoquestsCount(
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "userId", required = true) Long userId){
        long count = getDatabaseManager().getFollowingPhotoquestsCount(filter, userId);
        return new CountResponse(count);
    }

    @RequestMapping("/followQuest")
    public @ResponseBody Object followQuest(@RequestParam("questId") Long questId){
        getDatabaseManager().followPhotoquest(questId);
        return new Success();
    }

    @RequestMapping("/unfollowQuest")
    public @ResponseBody Object unfollowQuest(@RequestParam("questId") Long questId){
        getDatabaseManager().unfollowPhotoquest(questId);
        return new Success();
    }

    @RequestMapping("/getPhotoquestsCount")
    public @ResponseBody Object getPhotoquestsCount(@RequestParam(value = "filter", required = false) String filter){
        long count = getDatabaseManager().getPhotoQuestsCount(filter);
        return new CountResponse(count);
    }

    @RequestMapping("/getPhotoquestById")
    public @ResponseBody Object getPhotoquestById(@RequestParam("id") Long id){
        return getDatabaseManager().getPhotoQuestAndFillInfo(id);
    }

    @RequestMapping("/getPhotoById")
    public @ResponseBody Object getPhotoById(@RequestParam("id") Long id,
                                             @RequestParam(value = "userId", required = false) Long userId,
                                             @RequestParam(value = "photoquestId", required = false)
                                             Long photoquestId,
                                             @RequestParam(value = "category", required = false)
                                             PhotoCategory category){
        DatabaseManager databaseManager = getDatabaseManager();
        DatabaseManager.PhotoFillParams params = new DatabaseManager.PhotoFillParams();
        params.category = category;
        params.photoquestId = photoquestId;
        params.userId = userId;
        return databaseManager.getPhotoAndFillInfo(id, params);
    }

    @RequestMapping("/getPhotoPosition")
    public @ResponseBody Object getPhotoPosition(@RequestParam("id") Long photoId,
                                                 @RequestParam(value = "order", required = false,
                                                         defaultValue = "newest") RatingOrder order){
        DatabaseManager databaseManager = getDatabaseManager();
        return new LongResult(databaseManager.getPhotoInPhotoquestPosition(photoId, order));
    }

    @RequestMapping("/getUserById")
    public @ResponseBody Object getUserById(@RequestParam("id") long id){
        DatabaseManager databaseManager = getDatabaseManager();
        return getUserResponse(databaseManager.requestUserProfileData(id));
    }

    @RequestMapping("/getUnreadMessagesCount")
    public @ResponseBody Object getUnreadMessagesCount(){
        DatabaseManager databaseManager = getDatabaseManager();
        final User user = databaseManager.getSignedInUserOrThrow();
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
                                                      @RequestParam(value = "category", defaultValue = "all")
                                                      PhotoCategory category,
                                                      OffsetLimit offsetLimit){
        Collection<Photo> photos = null;
        if (category == PhotoCategory.all) {
            photos = getDatabaseManager().
                    getPhotosOfPhotoquest(photoquestId, offsetLimit, order);
        } else if(category == PhotoCategory.friends) {
            photos = getDatabaseManager().getPhotosOfFriendsByPhotoquest(photoquestId, order, offsetLimit);
        } else if(category == PhotoCategory.mine) {
            photos = getDatabaseManager().getPhotosOfSignedInUserByPhotoquest(photoquestId, order, offsetLimit);
        }

        return getPhotosResponse(photos);
    }

    @RequestMapping("/getFiendsPhotosOfPhotoquest")
    public @ResponseBody Object getFiendsPhotosOfPhotoquest(@RequestParam("id") Long photoquestId,
                                                      @RequestParam(value = "order", required = false,
                                                              defaultValue = "newest")
                                                      RatingOrder order,
                                                      OffsetLimit offsetLimit){
        Collection<Photo> photos = getDatabaseManager().
                getPhotosOfFriendsByPhotoquest(photoquestId, order, offsetLimit);
        return new PhotosList(photos);
    }

    @RequestMapping("/getFiendsPhotosOfPhotoquestCount")
    public @ResponseBody Object getFiendsPhotosOfPhotoquestCount(@RequestParam("id") Long photoquestId){
        long count = getDatabaseManager().
                getPhotosOfFriendsByPhotoquestCount(photoquestId);
        return new CountResponse(count);
    }

    @RequestMapping("/getUserPhotosOfPhotoquest")
    public @ResponseBody Object getUserPhotosOfPhotoquest(@RequestParam("id") Long photoquestId,
                                                            @RequestParam(value = "order", required = false,
                                                                    defaultValue = "newest")
                                                            RatingOrder order,
                                                            OffsetLimit offsetLimit){
        Collection<Photo> photos = getDatabaseManager().
                getPhotosOfSignedInUserByPhotoquest(photoquestId, order, offsetLimit);
        return new PhotosList(photos);
    }

    @RequestMapping("/getUserPhotosOfPhotoquestCount")
    public @ResponseBody Object getUserPhotosOfPhotoquestCount(@RequestParam("id") Long photoquestId){
        long count = getDatabaseManager().
                getPhotosOfSignedInUserByPhotoquestCount(photoquestId);
        return new CountResponse(count);
    }

    @RequestMapping("/getNextPrevPhoto")
    public @ResponseBody Object getNextPrevPhoto(@RequestParam(value = "photoquestId", required = false)
                                                     Long photoquestId,
                                                 @RequestParam(value = "userId", required = false) Long userId,
                                                 @RequestParam("photoId") Long photoId,
                                                 @RequestParam("next") boolean next,
                                                 @RequestParam(value = "category", defaultValue = "all")
                                                 PhotoCategory category,
                                                 @RequestParam(value = "order", required = false,
                                                                     defaultValue = "newest")
                                                 RatingOrder order) {
        if(userId != null) {
            if (category == PhotoCategory.avatar) {
                return getNextPrevAvatar(userId, photoId, next, order);
            } else {
                return getNextPrevPhotoOfUser(userId, photoId, next, order);
            }
        } else if(photoquestId != null) {
            return getNextPrevPhotoOfPhotoquest(photoquestId, photoId, next, category, order);
        } else {
            throw new IllegalArgumentException("Specify userId or photoquestId");
        }
    }


    @RequestMapping("/getNextPrevPhotoOfPhotoquest")
    public @ResponseBody Object getNextPrevPhotoOfPhotoquest(@RequestParam("photoquestId") Long photoquestId,
                                                             @RequestParam("photoId") Long photoId,
                                                             @RequestParam("next") boolean next,
                                                             @RequestParam(value = "category", defaultValue = "all")
                                                             PhotoCategory category,
                                                             @RequestParam(value = "order", required = false,
                                                                     defaultValue = "newest")
                                                             RatingOrder order){
        if (category == PhotoCategory.all) {
            return getDatabaseManager().getNextPrevPhotoOfPhotoquest(
                    photoquestId, photoId, order, next);
        } else if(category == PhotoCategory.mine) {
            return getDatabaseManager().getNextPrevPhotoOfSignedInUserInPhotoquest
                    (photoquestId, photoId, order, next);
        } else if (category == PhotoCategory.friends) {
            return getDatabaseManager().getNextPrevPhotoOfFriendsInPhotoquest(
                    photoquestId, photoId, order, next);
        }

        throw new UnsupportedOperationException("Unsupported category");
    }

    @RequestMapping("/getNextPrevPhotoOfFriendsInPhotoquest")
    public @ResponseBody Object getNextPrevPhotoOfFriendsInPhotoquest(@RequestParam("photoquestId") Long photoquestId,
                                                             @RequestParam("photoId") Long photoId,
                                                             @RequestParam("next") boolean next,
                                                             @RequestParam(value = "order", required = false,
                                                                     defaultValue = "newest")
                                                             RatingOrder order){
        return getDatabaseManager().getNextPrevPhotoOfFriendsInPhotoquest(
                photoquestId, photoId, order, next);
    }

    @RequestMapping("/getNextPrevPhotoOfUserInPhotoquest")
    public @ResponseBody Object getNextPrevPhotoOfUserInPhotoquest(@RequestParam("photoquestId") Long photoquestId,
                                                                      @RequestParam("photoId") Long photoId,
                                                                      @RequestParam("next") boolean next,
                                                                      @RequestParam(value = "order", required = false,
                                                                              defaultValue = "newest")
                                                                      RatingOrder order){
        return getDatabaseManager().getNextPrevPhotoOfSignedInUserInPhotoquest
                (photoquestId, photoId, order, next);
    }

    @RequestMapping("/getNextPrevPhotoOfUser")
    public @ResponseBody Object getNextPrevPhotoOfUser(@RequestParam("userId") Long userId,
                                                             @RequestParam("photoId") Long photoId,
                                                             @RequestParam("next") boolean next,
                                                             @RequestParam(value = "order", required = false,
                                                                     defaultValue = "newest")
                                                             RatingOrder order){
        return getDatabaseManager().getNextPrevPhotoOfUser(userId, photoId, order, next);
    }

    @RequestMapping("/getNextPrevAvatar")
    public @ResponseBody Object getNextPrevAvatar(@RequestParam("userId") Long userId,
                                                       @RequestParam("photoId") Long photoId,
                                                       @RequestParam("next") boolean next,
                                                       @RequestParam(value = "order", required = false,
                                                               defaultValue = "newest")
                                                       RatingOrder order){
        return getDatabaseManager().getNextPrevAvatar(userId, photoId, order, next);
    }

    @RequestMapping("/getPhotosOfPhotoquestCount")
    public @ResponseBody Object getPhotosOfPhotoquestCount(@RequestParam("id") Long photoquestId){
        long count = getDatabaseManager().
                getPhotosOfPhotoquestCount(photoquestId);
        return new CountResponse(count);
    }

    @RequestMapping("/getPhotosOfUser")
    public @ResponseBody Object getPhotosOfUser(@RequestParam("userId") Long userId,
                                                      @RequestParam(value = "order", required = false,
                                                              defaultValue = "newest")
                                                      RatingOrder order,
                                                      OffsetLimit offsetLimit){
        Collection<Photo> photos = getDatabaseManager().
                getPhotosOfUser(userId, offsetLimit, order);
        return new PhotosList(photos);
    }

    @RequestMapping("/getPhotosOfUserCount")
    public @ResponseBody Object getPhotosOfUserCount(@RequestParam("userId") Long userId){
        long count = getDatabaseManager().
                getPhotosOfUserCount(userId);
        return new CountResponse(count);
    }

    @RequestMapping("/addFriend")
    public @ResponseBody Object addFriend(@RequestParam("id") Long id){
        getDatabaseManager().addFriend(id);
        return new Success();
    }

    @RequestMapping("/removeFriend")
    public @ResponseBody Object removeFriend(@RequestParam("id") Long id){
        getDatabaseManager().removeFriend(id);
        return new Success();
    }

    @RequestMapping("/putComment")
    public @ResponseBody Object putComment(
            @RequestParam(value = "photoId", required = false) Long photoId,
            @RequestParam(value = "message", required = true) String message,
            @RequestParam(value = "commentId", required = false) Long toCommentId) {
        checkLikeCommentParams(photoId, toCommentId);
        return getDatabaseManager().addComment(photoId, message, toCommentId);
    }

    @RequestMapping("/sendMessage")
    public @ResponseBody Object sendMessage(
            @RequestParam(value = "toUserId", required = true) Long toUserId,
            @RequestParam(value = "message", required = true) String message) {
        if(message.length() > 255){
            throw new IllegalArgumentException("message.length > 255");
        }

        return getSendMessageResponse(getDatabaseManager().addMessage(toUserId, message));
    }

    @RequestMapping("/getLocationSuggestions")
    public @ResponseBody Object getLocationSuggestions(@RequestParam(
            value = "query", required = true) String query) throws IOException {
        return new LocationSuggestions(getDatabaseManager().getGoogleLocationSuggestions(query));
    }

    @RequestMapping("/getCountrySuggestions")
    public @ResponseBody Object getCountrySuggestions(@RequestParam(
            value = "query", required = true) String query) throws IOException {
        return new Suggestions(getDatabaseManager().getCountrySuggestions(query));
    }

    @RequestMapping("/getCountryByName")
    public @ResponseBody Object getCountryByName(@RequestParam(
            value = "name", required = true) String name) {
        return getDatabaseManager().getCountryByNameOrThrow(name);
    }

    @RequestMapping("/getCitySuggestions")
    public @ResponseBody Object getCitySuggestions(
            @RequestParam(value = "query", required = false, defaultValue = "") String query,
            @RequestParam(value = "countryId", required = true) Integer countryId)
            throws IOException {
        return new Suggestions(getDatabaseManager().getCitySuggestions(countryId, query));
    }

    @RequestMapping("/messages")
    public @ResponseBody Object getMessagesWithUser(@RequestParam(value = "userId", required = true) Long userId,
                                                    @RequestParam(value = "afterId", required = false) Long afterId,
                                               OffsetLimit offsetLimit) {
        DatabaseManager databaseManager = getDatabaseManager();
        Collection<Message> result =
                databaseManager.getMessagesWithUser(userId, offsetLimit, afterId);
        return getMessagesResponse(databaseManager, result);
    }

    @RequestMapping("/getDialogs")
    public @ResponseBody Object getDialogs(OffsetLimit offsetLimit) {
        Collection<Dialog> dialogs = getDatabaseManager().getDialogs(offsetLimit);
        return getDialogsResponse(dialogs);
    }

    @RequestMapping("/getDialogsCount")
    public @ResponseBody Object getDialogsCount() {
        long count = getDatabaseManager().getDialogsCount();
        return new CountResponse(count);
    }

    @RequestMapping("/deleteComment")
    public @ResponseBody Object deleteComment(
            @RequestParam(value = "id", required = true) Long commentId) {
        getDatabaseManager().deleteComment(commentId);
        return new Success();
    }

    @RequestMapping("/getCommentsOnPhoto")
    public @ResponseBody Object getCommentsOnPhoto(@RequestParam("photoId") Long photoId,
                                                   @RequestParam(value = "afterId", required = false)
                                                   Long afterId,
                                                   OffsetLimit offsetLimit){
        Collection<Comment> comments = getDatabaseManager().
                getCommentsOnPhotoAndFillData(photoId, afterId, offsetLimit);
        return new CommentsList(comments);
    }

    private void checkLikeCommentParams(Long photoId, Long commentId) {
        if((photoId == null && commentId == null) || (commentId != null && photoId != null)){
            throw new IllegalArgumentException("Specify one, photoId or commentId");
        }
    }

    @RequestMapping("/like")
    public @ResponseBody Object like(@RequestParam(value = "photoId", required = false) Long photoId,
                                     @RequestParam(value = "commentId", required = false) Long commentId) {
        long currentTimeMillis = System.currentTimeMillis();
        long creationTime = 0;
        try {
            checkLikeCommentParams(photoId, commentId);

            DatabaseManager databaseManager = getDatabaseManager();
            creationTime = System.currentTimeMillis() - currentTimeMillis;
            if (photoId != null) {
                return databaseManager.likePhoto(photoId);
            } else {
                return databaseManager.likeComment(commentId);
            }
        } finally {
            long a = System.currentTimeMillis() - currentTimeMillis;
            a++;
            creationTime++;
        }
    }

    @RequestMapping("/unlike")
    public @ResponseBody Object unlike(@RequestParam("id") Long likeId) {
        getDatabaseManager().unlike(likeId);
        return new Success();
    }

    @RequestMapping("/initDatabase")
    public @ResponseBody Object initDatabase() {
        DatabaseManager databaseManager = getDatabaseManager();
        checkAdminPermissions(databaseManager);
        databaseManager.initDatabase();
        return new Success();
    }

    @RequestMapping("/getTables")
    public @ResponseBody Object getTables() {
        checkAdminPermissions(getDatabaseManager());
        return CollectionUtils.transform(Reflection.findClassesInPackage("com.tiksem.pq.data"),
                new CollectionUtils.Transformer<Class<?>, String>() {
                    @Override
                    public String get(Class<?> aClass) {
                        return aClass.getSimpleName();
                    }
                });
    }

    @RequestMapping("/clearDatabase")
    public @ResponseBody Object clearDatabase() throws IOException {
        DatabaseManager databaseManager = getDatabaseManager();
        checkAdminPermissions(databaseManager);
        databaseManager.clearDatabase();
        return new Success();
    }

    @RequestMapping("/dropTables")
    public @ResponseBody Object dropDatabase() {
        DatabaseManager databaseManager = getDatabaseManager();
        checkAdminPermissions(databaseManager);
        databaseManager.dropTables();
        return new Success();
    }

    @RequestMapping("/getUserStats")
    public @ResponseBody Object getUserStats() {
        return getDatabaseManager().getUserStats();
    }

    @RequestMapping("/getReplies")
    public @ResponseBody Object getReplies(OffsetLimit offsetLimit) {
        return getRepliesResponse(getDatabaseManager().getRepliesWithFullInfo(offsetLimit));
    }

    @RequestMapping("/getRepliesCount")
    public @ResponseBody Object getRepliesCount() {
        return new CountResponse(getDatabaseManager().getRepliesCount());
    }

    @RequestMapping("/getNews")
    public @ResponseBody Object getNews(
            @RequestParam(value = "userId", required = false) Long userId,
            OffsetLimit offsetLimit) {
        Collection<Action> news;
        if(userId == null){
            news = getDatabaseManager().getNews(offsetLimit);
        } else {
            news = getDatabaseManager().getUserNews(userId, offsetLimit);
        }

        return getFeedResponse(news);
    }

    @RequestMapping("/getNewsCount")
    public @ResponseBody Object getNewsCount(
            @RequestParam(value = "userId", required = false) Long userId) {
        long count;
        if(userId == null){
            count = getDatabaseManager().getNewsCount();
        } else {
            count = getDatabaseManager().getUserNewsCount(userId);
        }

        return new CountResponse(count);
    }

    @RequestMapping("/executeSQL")
    public @ResponseBody Object executeSQL(@RequestParam("sql") String sql) {
        DatabaseManager databaseManager = getDatabaseManager();
        checkAdminPermissions(databaseManager);
        MysqlObjectMapper mapper = databaseManager.getMapper();
        return mapper.executeSelectSql(sql);
    }

    @RequestMapping("/test")
    public @ResponseBody Object executeSQL(@RequestParam("sql") String sql,
                                           @RequestParam("count") int count) {
        DatabaseManager databaseManager = getDatabaseManager();
        checkAdminPermissions(databaseManager);
        MysqlObjectMapper mapper = databaseManager.getMapper();
        final long time = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            mapper.executeNonSelectSQL(sql);
        }

        return new HashMap<String, Object>() {
            {
                put("time", System.currentTimeMillis() - time);
            }
        };
    }

    private MysqlObjectMapper getMapper() {
        MysqlObjectMapper mapper;
        try {
            mapper = new MysqlObjectMapper(PhotoquestDataSource.getInstance().getConnection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return mapper;
    }

    @RequestMapping("/refreshSettings")
    public @ResponseBody Object refreshSettings() {
        checkAdminPermissions(getDatabaseManager());
        Settings instance = Settings.getInstance();
        instance.update();
        instance.updateRps();
        return new Success();
    }

    @RequestMapping("/initLocationsFromJSON")
    public @ResponseBody Object initLocationsFromJSON() throws IOException, JSONException {
        DatabaseManager databaseManager = getDatabaseManager();
        checkAdminPermissions(databaseManager);
        MysqlObjectMapper mapper = databaseManager.getMapper();
        LocationsCreatorFromJSON creatorFromJSON = new LocationsCreatorFromJSON(mapper, request, "locations.txt");
        creatorFromJSON.initLocations();
        return new Success();
    }

    @RequestMapping("/progress")
    public @ResponseBody Object progress() {
        final DatabaseManager databaseManager = getDatabaseManager();
        checkAdminPermissions(databaseManager);
        return new Object(){
            public List<ProgressOperation> operations = databaseManager.getProgressOperations();
        };
    }
}
