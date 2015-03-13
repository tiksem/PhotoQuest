package com.tiksem.pq.db;

import com.tiksem.mysqljava.*;
import com.tiksem.mysqljava.security.ApiRequest;
import com.tiksem.pq.Settings;
import com.tiksem.pq.data.*;
import com.tiksem.pq.data.City;
import com.tiksem.pq.data.Likable;
import com.tiksem.pq.data.response.LocationSuggestion;
import com.tiksem.pq.data.response.ReplyResponse;
import com.tiksem.pq.data.response.UserStats;
import com.tiksem.pq.db.advanced.AdvancedRequestsManager;
import com.tiksem.pq.db.advanced.SearchUsersParams;
import com.tiksem.pq.exceptions.*;
import com.tiksem.pq.http.HttpUtilities;
import com.utils.framework.MathUtils;
import com.utils.framework.Reflection;
import com.utils.framework.google.places.*;
import com.utils.framework.io.IOUtilities;
import com.utils.framework.randomuser.Gender;
import com.utils.framework.randomuser.RandomUserGenerator;
import com.utils.framework.randomuser.Response;
import com.utils.framework.strings.Strings;
import org.apache.commons.io.FileUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;

/**
 * Created by CM on 10/30/2014.
 */
public class DatabaseManager {
    public static final String AVATAR_QUEST_NAME = "Avatar";

    private static final String GOOGLE_API_KEY = "AIzaSyAfhfIJpCrb29TbTafQ1UWSqqaSaOuVCIg";

    private static final int MAX_KEYWORDS_COUNT = 7;

    private static final long PHOTOQUEST_VIEW_PERIOD = 30 * 1000;
    private static final long PROFILE_VIEW_PERIOD = 30 * 1000;
    private static final long PHOTO_VIEW_PERIOD = 30 * 1000;
    public static final String PHOTOQUEST_SET_NULL_AVATAR = "UPDATE Photoquest SET avatarId = NULL WHERE id = :id";

    private MysqlObjectMapper mapper;

    private ImageManager imageManager;
    private CaptchaManager captchaManager;
    private GooglePlacesSearcher googlePlacesSearcher = new GooglePlacesSearcher(GOOGLE_API_KEY);
    private AdvancedRequestsManager advancedRequestsManager;
    private DatabaseAsyncTaskManager.Handler asyncTaskHandler;
    private String lang;
    private String defaultAvatarPath = Settings.getInstance().get("defaultAvatarPath");
    private HttpServletRequest request;

    // do not use directly
    private User _signedInUser;

    public DatabaseManager(HttpServletRequest request, MysqlObjectMapper mapper, String lang) {
        this.mapper = mapper;
        advancedRequestsManager = new AdvancedRequestsManager(mapper);
        DatabaseAsyncTaskManager asyncTaskManager = DatabaseAsyncTaskManager.getInstance();
        if (request != null) {
            asyncTaskHandler = asyncTaskManager.createHandler(lang, request);
        }
        captchaManager = new DatabaseCaptchaManager(mapper);
        imageManager = new FileSystemImageManager(Settings.getInstance().getImageManagerSettings());
        this.lang = lang;
        this.request = request;

        Queue<RuntimeException> exceptions = asyncTaskManager.getExceptions();
        if(!exceptions.isEmpty()){
            throw exceptions.remove();
        }
    }

    private <T> T replace(T object) {
        mapper.replace(object);
        return object;
    }

    private void replaceAll(Object... objects) {
        mapper.replaceAll(Arrays.asList(objects));
    }

    private void replaceAll(List<Object> objects) {
        mapper.replaceAll(objects);
    }

    private int insert(Object object) {
        return mapper.insert(object);
    }

    private int insertIgnore(Object object) {
        return mapper.insert(object, true);
    }

    private void insertAll(Object... objects) {
        mapper.insertAll(Arrays.asList(objects));
    }

    private int delete(Object pattern) {
        return mapper.delete(pattern);
    }

    private void deleteAll(Object... objects) {
        mapper.deleteAll(objects);
    }

    private void deleteAll(Iterable<Object> objects) {
        mapper.deleteAll(objects);
    }

    public User getUserById(long id) {
        return mapper.getObjectById(User.class, id);
    }

    public User getUserByIdOrThrow(long id) {
        User user = getUserById(id);
        if (user == null) {
            throw new UnknownUserException(String.valueOf(id));
        }

        return user;
    }

    public User requestUserProfileData(long id) {
        User user = getUserByIdOrThrow(id);

        User signedInUser = getSignedInUser();
        if(signedInUser != null && signedInUser.getId() != id){
            ProfileView pattern = new ProfileView();
            pattern.setVisitorId(signedInUser.getId());
            pattern.setUserId(id);
            ProfileView profileView = mapper.getObjectByPattern(pattern);
            if(profileView == null){
                profileView = pattern;
            }

            long currentTimeMillis = System.currentTimeMillis();
            if(profileView == pattern || currentTimeMillis -
                    profileView.getAddingDate() >= PROFILE_VIEW_PERIOD){
                profileView.setAddingDate(currentTimeMillis);
                user.incrementRating();
                replace(user);
                if (profileView == pattern) {
                    insert(profileView);
                } else {
                    replace(profileView);
                }
            }
        }

        setUsersInfoAndRelationStatus(Collections.singletonList(user));

        return user;
    }

    public User getUserByLogin(String login) {
        User user = new User();
        user.setLogin(login);
        return mapper.getObjectByPattern(user);
    }

    public User getUserByLoginOrThrow(String login) {
        User user = getUserByLogin(login);
        if (user == null) {
            throw new UnknownUserException(login);
        }

        return user;
    }


    public User getUserByLoginAndPassword(String login, String password) {
        User user = getUserByLogin(login);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }

        return null;
    }

    public User login(String login, String password) {
        User user = getUserByLogin(login);
        if (user != null && user.getPassword().equals(password)) {
            setUserInfo(user);
            user.setSignedInUser(true);
            return user;
        }

        return null;
    }

    public User loginOrThrow(String login, String password) {
        User user = login(login, password);
        if (user == null) {
            throw new LoginFailedException();
        }

        return user;
    }

    public User getSignedInUser() {
        if(_signedInUser != null){
            return _signedInUser;
        }

        Cookie[] cookies = request.getCookies();
        Cookie loginCookie = HttpUtilities.getCookie("login", cookies);
        if (loginCookie == null) {
            return null;
        }

        Cookie passwordCookie = HttpUtilities.getCookie("password", cookies);
        if (passwordCookie == null) {
            return null;
        }

        User user = getUserByLoginAndPassword(loginCookie.getValue(),
                passwordCookie.getValue());

        _signedInUser = user;
        return user;
    }

    public User getSignedInUserOrThrow() {
        User user = getSignedInUser();
        if (user == null) {
            throw new UserIsNotSignInException();
        }

        return user;
    }

    public Photoquest getPhotoQuestByName(String photoquestName) {
        Photoquest photoquest = new Photoquest();
        photoquest.setName(photoquestName);
        return mapper.getObjectByPattern(photoquest);
    }

    public Photoquest getPhotoQuestById(long id) {
        return mapper.getObjectById(Photoquest.class, id);
    }

    public Photoquest getPhotoQuestByIdOrThrow(long id) {
        Photoquest photoquest = getPhotoQuestById(id);
        if (photoquest == null) {
            throw new PhotoquestNotFoundException(String.valueOf(id));
        }

        return photoquest;
    }

    public Photoquest getPhotoQuestAndFillInfo(long id) {
        Photoquest photoquest = getPhotoQuestByIdOrThrow(id);
        setPhotoquestsFollowingParamIfSignedIn(Collections.singletonList(photoquest));
        setAvatar(photoquest);
        return photoquest;
    }

    public Photoquest createSystemPhotoquest(String photoquestName) {
        Photoquest photoquest = Photoquest.withZeroViewsAndLikes(photoquestName);
        insert(photoquest);
        setPhotoquestKeywords(photoquest, Collections.<String>emptyList());
        return photoquest;
    }

    private void setPhotoquestKeywords(long photoquestId, String keywords) {
        PhotoquestSearch photoquestSearch = new PhotoquestSearch();
        photoquestSearch.setKeywords(keywords);
        photoquestSearch.setPhotoquestId(photoquestId);
        insert(photoquestSearch);
    }

    private void setPhotoquestKeywords(Photoquest photoquest, List<String> keywords) {
        String keywordConcat = photoquest.getName() + " " + Strings.join(" ", keywords);
        setPhotoquestKeywords(photoquest.getId(), keywordConcat);
    }

    public Photoquest createPhotoQuest(String photoquestName, List<String> keywords,
                                       boolean follow) {
        if(keywords.size() > MAX_KEYWORDS_COUNT){
            throw new IllegalArgumentException("Too much tags, only " + MAX_KEYWORDS_COUNT + " are allowed");
        }

        User user = getSignedInUserOrThrow();
        Photoquest photoquest = getPhotoQuestByName(photoquestName);
        if (photoquest != null) {
            throw new PhotoquestExistsException(photoquestName);
        }

        photoquest = Photoquest.withZeroViewsAndLikes(photoquestName);
        Long userId = user.getId();
        photoquest.setUserId(userId);
        photoquest.setIsNew(true);

        insert(photoquest);
        Action action = new Action();
        action.setPhotoquestId(photoquest.getId());
        action.setUserId(userId);
        insertAction(action);

        setPhotoquestKeywords(photoquest, keywords);

        if(follow){
            followPhotoquest(photoquest);
        }

        return photoquest;
    }

    public Collection<Photoquest> getPhotoquestsCreatedByUser(
            String filter,
            long userId, OffsetLimit offsetLimit, RatingOrder order) {
        String orderBy = getPhotoOrderBy(order);
        List<Photoquest> result;
        if (Strings.isEmpty(filter)) {
            SelectParams params = new SelectParams();
            params.offsetLimit = offsetLimit;
            params.ordering = orderBy;
            params.foreignFieldsToFill = MysqlObjectMapper.ALL_FOREIGN;
            Photoquest pattern = new Photoquest();
            pattern.setUserId(userId);
            result = mapper.queryByPattern(pattern, params);
        } else {
            result = advancedRequestsManager.getCreatedPhotoquestsByQuery(filter, offsetLimit, orderBy, userId);
        }

        initPhotoquestsInfo(result);
        return result;
    }

    public long getPhotoquestsCreatedByUserCount(String filter, long userId) {
        if (Strings.isEmpty(filter)) {
            Photoquest photoquest = new Photoquest();
            photoquest.setUserId(userId);
            return mapper.getCountByPattern(photoquest);
        } else {
            return advancedRequestsManager.getCreatedPhotoquestsByQueryCount(filter, userId);
        }
    }

    private void initUserAvatars(Collection<Photoquest> photoquests) {
        for (Photoquest photoquest : photoquests) {
            User user = photoquest.getUser();
            if(user != null){
                setAvatar(user);
            }
        }
    }

    public void initPhotoquestsInfo(Collection<Photoquest> photoquests) {
        setAvatar(photoquests);
        initUserAvatars(photoquests);
        setPhotoquestsFollowingParamIfSignedIn(photoquests);
    }

    public List<Photo> getPhotosOfFriendsByPhotoquest(
                                                      long photoquestId,
                                                      RatingOrder order,
                                                      OffsetLimit offsetLimit) {
        Photoquest photoquest = getPhotoQuestByIdOrThrow(photoquestId);
        User signedInUser = getSignedInUserOrThrow();
        String orderBy = getPhotoOrderBy(order);
        List<Photo> photos = advancedRequestsManager.getPhotosOfFriendsByPhotoquest(signedInUser.getId(),
                photoquestId, orderBy, offsetLimit);
        initPhotosOfPhotoquestInfo(photos, photoquest);
        return photos;
    }

    public long getPhotosOfFriendsByPhotoquestCount(long photoquestId) {
        User signedInUser = getSignedInUserOrThrow();
        return advancedRequestsManager.getPhotosOfFriendsByPhotoquestCount(signedInUser.getId(),
                photoquestId);
    }

    public List<Photo> getPhotosOfSignedInUserByPhotoquest(
                                                           long photoquestId,
                                                           RatingOrder order,
                                                           OffsetLimit offsetLimit) {
        Photoquest photoquest = getPhotoQuestByIdOrThrow(photoquestId);
        User signedInUser = getSignedInUserOrThrow();
        String orderBy = getPhotoOrderBy(order);
        Photo pattern = new Photo();
        pattern.setUserId(signedInUser.getId());
        pattern.setPhotoquestId(photoquestId);
        List<Photo> photos = mapper.queryByPattern(pattern, offsetLimit, orderBy);
        initPhotosOfPhotoquestInfo(photos, photoquest);
        return photos;
    }

    public long getPhotosOfSignedInUserByPhotoquestCount(
                                                           long photoquestId) {
        getPhotoQuestByIdOrThrow(photoquestId);
        User signedInUser = getSignedInUserOrThrow();
        Photo pattern = new Photo();
        pattern.setUserId(signedInUser.getId());
        pattern.setPhotoquestId(photoquestId);
        return mapper.getCountByPattern(pattern);
    }

    public Collection<Photoquest> searchPhotoquests(final String query,
                                                    OffsetLimit offsetLimit, RatingOrder order) {
        String ordering = getPhotoOrderBy(order);
        List<Photoquest> photoquests = advancedRequestsManager.getPhotoquestsByQuery(query, offsetLimit,
                ordering);
        initPhotoquestsInfo(photoquests);
        return photoquests;
    }

    public City getCityById(Integer id) {
        return mapper.getObjectById(City.class, id);
    }

    public City getCityByIdOrThrow(Integer id) {
        City city = getCityById(id);
        if(city == null){
            throw new CityNotFoundException(id);
        }

        return city;
    }

    public Country getCountryById(Integer id) {
        return mapper.getObjectById(Country.class, id);
    }

    public Country getCountryByIdOrThrow(Integer id) {
        Country country = getCountryById(id);
        if(country == null){
            throw new CountryNotFoundException(id);
        }

        return country;
    }

    public Country getCountryByName(String name) {
        Country pattern = new Country();
        pattern.setEnName(name);
        Country country = mapper.getObjectByPattern(pattern);
        if(country != null){
            return country;
        }

        pattern = new Country();
        pattern.setRuName(name);
        return mapper.getObjectByPattern(pattern);
    }

    public Country getCountryByNameOrThrow(String name) {
        Country country = getCountryByName(name);
        if(country == null){
            throw new CountryNotFoundException(name);
        }

        return country;
    }

    public void checkLocation(User user) {
        City city = getCityByIdOrThrow(user.getCityId());
        Country country = getCountryByIdOrThrow(city.getCountryId());
        user.setCountryId(country.getId());
    }

    private User registerUser(User user) throws IOException {
        String login = user.getLogin();

        if (getUserByLogin(login) != null) {
            throw new UserExistsRegisterException(login);
        }

        checkLocation(user);

        insert(user);
        return user;
    }

    public User editProfile(
                            String name, String lastName,
                            Integer cityId) throws IOException {
        User user = getSignedInUserOrThrow();

        if (name != null) {
            user.setNameAndLastName(name, user.getLastName());
        }
        if (lastName != null) {
            user.setNameAndLastName(user.getName(), lastName);
        }
        if (cityId != null) {
            user.setCityId(cityId);
        }
        checkLocation(user);
        setUserInfo(user);

        return replace(user);
    }

    public User changePassword(String newPassword, String oldPassword) {
        User user = getSignedInUserOrThrow();
        if(!user.getPassword().equals(oldPassword)){
            throw new PermissionDeniedException("Invalid password!");
        }
        user.setPassword(newPassword);

        replace(user);
        return loginOrThrow(user.getLogin(), user.getPassword());
    }

    public User registerUser(User user, MultipartFile avatar) throws IOException {
        InputStream avatarInputStream = null;
        try {
            avatarInputStream = avatar.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return registerUser(user, avatarInputStream);
    }

    public User registerUser(User user, InputStream avatar) throws IOException {
        user = registerUser(user);

        if (user.getLogin() == null) {
            throw new RuntimeException("WTF?");
        }

        if (avatar != null) {
            Photo photo = new Photo();
            Photoquest avatarPhotoQuest =
                    getOrCreateSystemPhotoQuest(DatabaseManager.AVATAR_QUEST_NAME);
            photo.setPhotoquestId(avatarPhotoQuest.getId());
            photo.setUserId(user.getId());
            photo = addPhoto(photo, avatar);
            user.setAvatarId(photo.getId());
            updatePhotoquestAvatar(avatarPhotoQuest);

            if (user.getLogin() == null) {
                throw new RuntimeException("WTF?");
            }

            replace(user);
        }

        return user;
    }

    public long getAllUsersCount() {
        return mapper.getAllObjectsCount(User.class);
    }

    private void setRelationStatus(User signedInUser, Collection<User> users) {
        Long signedInUserId = signedInUser.getId();

        for(User user : users){
            Long userId = user.getId();
            Relationship pattern = new Relationship();
            pattern.setFromUserId(signedInUserId);
            pattern.setToUserId(userId);

            Relationship relationship = mapper.getObjectByPattern(pattern);
            if (relationship != null) {
                Integer type = relationship.getType();
                if(type == Relationship.FRIENDSHIP){
                    user.setRelation(RelationStatus.friend);
                } else if(type == Relationship.FOLLOWS){
                    user.setRelation(RelationStatus.follows);
                } else if(type == Relationship.FRIEND_REQUEST) {
                    user.setRelation(RelationStatus.request_sent);
                } else {
                    throw new RuntimeException("Unexpected WTF");
                }
            } else {
                pattern.setFromUserId(userId);
                pattern.setToUserId(signedInUserId);

                relationship = mapper.getObjectByPattern(pattern);
                if(relationship != null){
                    Integer type = relationship.getType();
                    if(type == Relationship.FOLLOWS){
                        user.setRelation(RelationStatus.followed);
                    } else if(type == Relationship.FRIEND_REQUEST) {
                        user.setRelation(RelationStatus.request_received);
                    } else {
                        throw new RuntimeException("Unexpected WTF");
                    }
                }
            }
        }
    }

    public Collection<User> getAllUsers(
                                        boolean fillRelationshipData,
                                        OffsetLimit offsetLimit,
                                        RatingOrder order) {
        Collection<User> users = mapper.queryAllObjects(User.class, offsetLimit,
                getPeopleOrderBy(order));

        setUsersInfo(users);

        if(fillRelationshipData){
            User signedInUser = getSignedInUser();
            if (signedInUser != null) {
                setRelationStatus(signedInUser, users);
            }
        }

        return users;
    }

    public void setUsersInfoAndRelationStatus(Collection<User> users) {
        User signedInUser = getSignedInUser();
        setUsersInfo(users);
        if (signedInUser != null) {
            setRelationStatus(signedInUser, users);
        }
    }

    public long getReceivedRequestsCount(SearchUsersParams params, Long userId) {
        if(userId == null){
            userId = getSignedInUserOrThrow().getId();
        }

        return advancedRequestsManager.getSearchReceivedRequestsCount(params, userId);
    }

    public long getSentRequestsCount(SearchUsersParams params, Long userId) {
        if(userId == null){
            userId = getSignedInUserOrThrow().getId();
        }

        return advancedRequestsManager.getSearchSentRequestsCount(params, userId);
    }

    public void checkAdminPermissions() {
        User user = getSignedInUserOrThrow();
        if(!user.getLogin().equals(Settings.getInstance().get("admin"))){
            throw new PermissionDeniedException("Hey dude, you are not admin");
        }
    }

    public void checkDebug() {
        if(!Settings.getInstance().getBoolean("debug")){
            throw new PermissionDeniedException("unable to use this method on release build");
        }
    }

    private interface UsersSearcher {
        public Collection<User> search(String orderBy);
    }

    public Collection<User> searchUsers(
                                        RatingOrder order,
                                        UsersSearcher usersSearcher) {
        String orderBy = getPeopleOrderBy(order);
        Collection<User> users = usersSearcher.search(orderBy);
        setUsersInfoAndRelationStatus(users);

        return users;
    }

    public Collection<User> searchUsers(
                                        final SearchUsersParams searchParams,
                                        final OffsetLimit offsetLimit,
                                        RatingOrder order) {
        return searchUsers(order, new UsersSearcher() {
            @Override
            public Collection<User> search(String orderBy) {
                return advancedRequestsManager.searchUsers(searchParams, orderBy, offsetLimit);
            }
        });
    }

    public long getSearchUsersCount(SearchUsersParams searchParams) {
        return advancedRequestsManager.getSearchUsersCount(searchParams);
    }

    public PerformedPhotoquest getOrCreatePerformedPhotoquest(long userId, long photoquestId) {
        PerformedPhotoquest pattern = new PerformedPhotoquest();
        pattern.setUserId(userId);
        pattern.setPhotoquestId(photoquestId);
        PerformedPhotoquest performedPhotoquest = mapper.getObjectByPattern(pattern);
        if(performedPhotoquest == null){
            insert(pattern);
            return pattern;
        }

        return performedPhotoquest;
    }

    private Photo addPhoto(Photo photo, InputStream bitmapData) {
        Long userId = photo.getUserId();
        if(userId == null){
            User user = getSignedInUserOrThrow();
            userId = user.getId();
        }

        photo.setLikesCount(0l);
        photo.setUserId(userId);
        photo.setIsNew(true);

        insert(photo);
        try {
            imageManager.saveImage(photo.getId(), bitmapData);
        } catch (ImageSizeException e) {
            delete(photo);
            throw e;
        }

        getOrCreatePerformedPhotoquest(userId, photo.getPhotoquestId());

        commitAddPhotoAction(photo);

        return photo;
    }

    private void insertAction(Action action) {
        insert(action);
        advancedRequestsManager.insertActionFeed(action);
    }

    private void commitAddPhotoAction(Photo photo) {
        Action action = new Action();
        action.setPhotoquestId(photo.getPhotoquestId());
        action.setPhotoId(photo.getId());
        action.setUserId(photo.getUserId());
        insertAction(action);
    }

    public Photo setAvatar(long photoId) {
        User signedInUser = getSignedInUserOrThrow();
        Photo photo = getPhotoByIdOrThrow(photoId);
        Photoquest photoquest = getPhotoQuestByIdOrThrow(photo.getPhotoquestId());
        if(!photoquest.getName().equals(AVATAR_QUEST_NAME)){
            throw new IllegalArgumentException("The photo is not in avatar photoquest");
        }

        if(!photo.getUserId().equals(signedInUser.getId())){
            throw new PermissionDeniedException("Unable to set as avatar photo, which is not owned by current user");
        }

        signedInUser.setAvatarId(photo.getId());
        replace(signedInUser);
        initPhotoUrl(photo);

        return photo;
    }

    public Photo changeAvatar(MultipartFile file) throws IOException {
        Photoquest photoquest = getOrCreateSystemPhotoQuest(AVATAR_QUEST_NAME);
        return addPhotoToPhotoquest(photoquest.getId(), file, null, false);
    }

    public Photo addPhotoToPhotoquest(
                                      long photoquestId, MultipartFile file,
                                      String message,
                                      boolean follow) throws IOException {
        Photoquest photoquest = getPhotoQuestByIdOrThrow(photoquestId);

        Photo photo;
        if (!file.isEmpty()) {
            photo = new Photo();
            photo.setPhotoquestId(photoquestId);
            photo.setMessage(message);
            InputStream inputStream = file.getInputStream();
            photo = addPhoto(photo, inputStream);
            updatePhotoquestAvatar(photoquest);

            if (follow) {
                followPhotoquest(photoquestId);
            }
        } else {
            throw new FileIsEmptyException();
        }

        if (photoquest.getName().equals(AVATAR_QUEST_NAME)) {
            User signedInUser = getSignedInUserOrThrow();
            signedInUser.setAvatarId(photo.getId());
            replace(signedInUser);
        }

        initPhotoUrl(photo);

        return photo;
    }

    public Photo getPhotoById(long id) {
        return mapper.getObjectById(Photo.class, id);
    }

    public Photo getPhotoByIdOrThrow(long id) {
        Photo photo = getPhotoById(id);
        if(photo == null){
            throw new PhotoNotFoundException(id);
        }

        return photo;
    }

    private List<Like> getLikesOfPhoto(long photoId) {
        Like like = new Like();
        like.setPhotoId(photoId);
        return mapper.queryByPattern(like);
    }

    private void deletePhotoComments(long photoId) {
        Comment pattern = new Comment();
        pattern.setPhotoId(photoId);
        List<Comment> comments = mapper.queryByPattern(pattern);
        for(Comment comment : comments){
            deleteComment(comment.getId(), comment.getUserId());
        }
    }

    private void deletePhotoLikes(long photoId) {
        List<Like> likes = getLikesOfPhoto(photoId);

        Like pattern = new Like();
        pattern.setPhotoId(photoId);
        mapper.delete(pattern);

        Set<Long> userIdes = new HashSet<Long>();
        for(Like like : likes){
            userIdes.add(like.getUserId());
            Reply reply = new Reply();
            reply.setId(like.getId());
            reply.setType(Reply.LIKE);
            delete(reply);
        }

        for(Long userId : userIdes){
            advancedRequestsManager.updateUnreadRepliesCount(userId);
        }
    }

    public void deletePhoto(long id) {
        Photo photo = getPhotoByIdOrThrow(id);

        User signedInUser = getSignedInUserOrThrow();
        if(!photo.getUserId().equals(signedInUser.getId())){
            throw new PermissionDeniedException("Unable to delete photo, which is not owned by current user");
        }

        if(photo.getId().equals(signedInUser.getAvatarId())){
            Map<String, Object> args = Collections.singletonMap("userId", (Object)photo.getUserId());
            mapper.executeModifySQL("update user set avatarId = null WHERE id = :userId", args);
        }

        Long photoquestId = photo.getPhotoquestId();
        Photoquest photoquest = getPhotoQuestByIdOrThrow(photoquestId);

        imageManager.deleteImage(id);
        delete(photo);

        long likesCount = photo.getLikesCount();
        mapper.changeValue(photoquest, "likesCount", -likesCount);
        updatePhotoquestAvatar(photoquest);

        Action action = new Action();
        action.setPhotoId(id);
        mapper.delete(action);

        deletePhotoLikes(id);
        deletePhotoComments(id);
    }

    public Message getMessageById(long id) {
        return mapper.getObjectById(Message.class, id);
    }

    public Message getMessageByIdOrThrow(long id) {
        Message message = getMessageById(id);
        if(message == null){
            throw new MessageNotFoundException(id);
        }

        return message;
    }

    public InputStream getBitmapDataByPhotoId(long id) {
        return imageManager.getImageById(id);
    }

    public InputStream getBitmapDataByPhotoIdOrThrow(long id) {
        InputStream result = getBitmapDataByPhotoId(id);
        if(result == null){
            throw new ResourceNotFoundException();
        }

        return result;
    }

    public InputStream getDisplayImageById(long id) {
        return imageManager.getDisplayImage(id);
    }

    public InputStream getBitmapDataByPhotoIdOrThrow(long id, int maxWidth, int maxHeight) {
        InputStream result = imageManager.getImageById(id, maxWidth, maxHeight);
        if(result == null){
            throw new ResourceNotFoundException();
        }

        return result;
    }

    public InputStream getThumbnailByPhotoId(long id, int size) {
        return imageManager.getThumbnailOfImage(id, size);
    }

    public InputStream getThumbnailByPhotoIdOrThrow(long id, int size) {
        InputStream result = getThumbnailByPhotoId(id, size);
        if(result == null){
            throw new ResourceNotFoundException();
        }

        return result;
    }

    public InputStream getCaptchaImage(long key) {
        return captchaManager.getCaptchaImage(key);
    }

    public long getNewCaptcha() {
        return captchaManager.saveCaptcha();
    }

    public void checkCaptcha(long key, String answer) {
        if(!captchaManager.checkCaptcha(key, answer)){
            throw new InvalidCaptchaException();
        }
    }

    private void initPhotoUrl(Photo photo) {
        Long photoId = photo.getId();
        if(photoId == null){
            return;
        }

        photo.setUrl(HttpUtilities.getBaseUrl(request) + Photo.IMAGE_URL_PATH + photoId + ".jpg");
    }

    private void initPhotosUrl(Iterable<Photo> photos) {
        for(Photo photo : photos){
            initPhotoUrl(photo);
        }
    }

    public void initYourLikeParameter(Iterable<Photo> photos) {
        User signedInUser = getSignedInUser();
        if(signedInUser != null){
            Long signedInUserId = signedInUser.getId();
            for(Photo photo : photos){
                Like like = getLikeByUserAndPhotoId(signedInUserId, photo.getId());
                photo.setYourLike(like);
            }
        }
    }

    public void initYourLikeParameter(Photo photo) {
        initYourLikeParameter(Collections.singletonList(photo));
    }

    private void addPhotoquestViewIfNeed(Photoquest photoquest) {
        User signedInUser = getSignedInUser();
        if(signedInUser == null){
            return;
        }

        PhotoquestView pattern = new PhotoquestView();
        pattern.setPhotoquestId(photoquest.getId());
        pattern.setUserId(signedInUser.getId());
        PhotoquestView photoquestView = mapper.getObjectByPattern(pattern);
        if(photoquestView == null){
            photoquestView = pattern;
        }

        long currentTimeMillis = System.currentTimeMillis();
        if(photoquestView == pattern || currentTimeMillis -
                photoquestView.getAddingDate() >= PHOTOQUEST_VIEW_PERIOD){
            photoquestView.setAddingDate(currentTimeMillis);
            photoquest.incrementViewsCount();
            replace(photoquest);
            if(photoquestView == pattern){
                insert(photoquestView);
            } else {
                replace(photoquestView);
            }
        }
    }

    public void destroy() {
        mapper.destroy();
    }

    private interface NextPhotoPatternProvider {
        Photo getPattern();
    }

    private void setPhotoInfo(Photo photo) {
        initPhotoUrl(photo);
        initYourLikeParameter(photo);
        User user = photo.getUser();
        setAvatar(user);

        Photoquest photoquest = photo.getPhotoquest();
        setAvatar(photoquest);
    }

    private Photo getNextPrevPhoto(
                                   RatingOrder order,
                                   long photoId,
                                   NextPhotoPatternProvider patternProvider,
                                   boolean next) {
        String orderString = getPhotoOrderBy(order);
        Photo photo = getPhotoByIdOrThrow(photoId);
        Photo pattern = patternProvider.getPattern();

        Photo nextPhoto = mapper.getNextPrev(pattern, photo, orderString, MysqlObjectMapper.ALL_FOREIGN,
                next, true);
        setPhotoInfo(nextPhoto);

        nextPhoto.setShowNextPrevButtons(true);

        return nextPhoto;
    }

    public Photo getNextPrevPhotoOfPhotoquest(final long photoQuestId, long photoId,
                                          RatingOrder order, boolean next) {
        return getNextPrevPhoto(order, photoId, new NextPhotoPatternProvider() {
            @Override
            public Photo getPattern() {
                Photo pattern = new Photo();
                pattern.setPhotoquestId(photoQuestId);
                return pattern;
            }
        }, next);
    }

    public Photo getNextPrevPhotoOfUser(final long userId, long photoId,
                                              RatingOrder order, boolean next) {
        return getNextPrevPhoto(order, photoId, new NextPhotoPatternProvider() {
            @Override
            public Photo getPattern() {
                Photo pattern = new Photo();
                pattern.setUserId(userId);
                return pattern;
            }
        }, next);
    }

    public Photo getNextPrevAvatar(
                                   long userId, long photoId, RatingOrder order, boolean next) {
        Photoquest photoquest = getPhotoQuestByName(AVATAR_QUEST_NAME);
        return getNextPrevPhotoOfUserInPhotoquest(userId, photoId, photoquest.getId(),
                order, next);
    }

    public Photo getNextPrevPhotoOfUserInPhotoquest(
                                                    final long userId,
                                                    long photoId,
                                                    final long photoquestId,
                                                    RatingOrder order,
                                                    boolean next) {
        return getNextPrevPhoto(order, photoId, new NextPhotoPatternProvider() {
            @Override
            public Photo getPattern() {
                Photo pattern = new Photo();
                pattern.setUserId(userId);
                pattern.setPhotoquestId(photoquestId);
                return pattern;
            }
        }, next);
    }

    public Photo getNextPrevPhotoOfSignedInUserInPhotoquest(final long photoquestId, long photoId,
                                        RatingOrder order, boolean next) {
        return getNextPrevPhoto(order, photoId, new NextPhotoPatternProvider() {
            @Override
            public Photo getPattern() {
                User signedInUser = getSignedInUserOrThrow();
                Photo pattern = new Photo();
                pattern.setUserId(signedInUser.getId());
                pattern.setPhotoquestId(photoquestId);
                return pattern;
            }
        }, next);
    }

    public Photo getNextPrevPhotoOfFriendsInPhotoquest(final long photoquestId, long photoId,
                                                       RatingOrder order, boolean next) {
        String orderBy;
        if(order == RatingOrder.newest){
            orderBy = "id";
        } else if(order == RatingOrder.hottest) {
            orderBy = "viewsCount";
        } else {
            orderBy = "likesCount";
        }

        Photo photo = getPhotoByIdOrThrow(photoId);
        Object orderByValue = Reflection.getFieldValueUsingGetter(photo, orderBy);

        User signedInUser = getSignedInUserOrThrow();
        return advancedRequestsManager.getNextPrevPhotoOfFriends(signedInUser.getId(), photoquestId, orderByValue,
                orderBy, next);
    }

    private void initPhotosOfPhotoquestInfo(Collection<Photo> photos, Photoquest photoquest) {
        initPhotosUrl(photos);

        initYourLikeParameter(photos);
        addPhotoquestViewIfNeed(photoquest);
    }

    public Collection<Photo> getPhotosOfPhotoquest(long photoQuestId,
                                                   OffsetLimit offsetLimit, RatingOrder order) {
        Photoquest photoquest = getPhotoQuestByIdOrThrow(photoQuestId);
        addPhotoquestViewIfNeed(photoquest);

        Photo photoPattern = new Photo();
        photoPattern.setPhotoquestId(photoQuestId);
        String orderString = getPhotoOrderBy(order);

        Collection<Photo> photos = mapper.queryByPattern(photoPattern, offsetLimit, orderString);
        initPhotosOfPhotoquestInfo(photos, photoquest);

        return photos;

    }

    public Collection<Photo> getPhotosOfUser(long userId,
                                             OffsetLimit offsetLimit, RatingOrder order) {
        Photo pattern = new Photo();
        pattern.setUserId(userId);
        String ordering = getPhotoOrderBy(order);
        Collection<Photo> photos =
                mapper.queryByPattern(pattern, offsetLimit, ordering);
        initPhotosUrl(photos);
        initYourLikeParameter(photos);

        return photos;
    }

    public long getPhotosOfUserCount(long userId) {
        Photo pattern = new Photo();
        pattern.setUserId(userId);
        return mapper.getCountByPattern(pattern);
    }

    public long getPhotosOfPhotoquestCount(long photoQuestId) {
        Photo photoPattern = new Photo();
        photoPattern.setPhotoquestId(photoQuestId);
        return mapper.getCountByPattern(photoPattern);
    }

    public String getDefaultAvatar() {
        return HttpUtilities.getBaseUrl(request) + "/" + defaultAvatarPath;
    }

    public void setAvatar(WithAvatar withAvatar) {
        Long avatarId = withAvatar.getAvatarId();
        if(avatarId == null){
            withAvatar.setAvatar(getDefaultAvatar());
        } else {
            withAvatar.setAvatar(HttpUtilities.getBaseUrl(request) + Photo.IMAGE_URL_PATH + avatarId + ".jpg");
        }
    }

    public static class Location {
        public String cityName;
        public String countryName;
        public Integer countryId;
        public Integer cityId;
    }

    public Location getLocation(Integer cityId) {
        Location location = new Location();
        location.cityId = cityId;
        City city = getCityByIdOrThrow(cityId);
        Integer countryId = city.getCountryId();
        Country country = getCountryByIdOrThrow(countryId);
        location.cityName = city.getEnName();
        location.countryName = country.getEnName();
        location.countryId = countryId;
        return location;
    }

    public void setUserInfo(User user) {
        Location location = getLocation(user.getCityId());

        user.setCountry(location.countryName);
        user.setCity(location.cityName);
        setAvatar(user);
    }

    public void setUsersInfo(Iterable<User> users) {
        for(User user : users){
            setUserInfo(user);
        }
    }

    public void setAvatar(Iterable<? extends WithAvatar> withAvatars) {
        for(WithAvatar withAvatar : withAvatars){
            setAvatar(withAvatar);
        }
    }

    private String getPhotoOrderBy(RatingOrder order) {
        String orderString;
        switch (order) {
            case hottest:
                orderString = "isNew desc, viewsCount desc, id desc";
                break;
            case rated:
                orderString = "likesCount desc, id desc";
                break;
            default:
                orderString = "id desc";
                break;
        }

        return orderString;
    }

    private String getPeopleOrderBy(RatingOrder order) {
        String orderString;
        switch (order) {
            case hottest:
                throw new UnsupportedOperationException("hottest is not supported yet");
            case rated:
                orderString = "rating desc, id desc";
                break;
            default:
                orderString = "id desc";
                break;
        }

        return orderString;
    }

    private void setPhotoquestFollowingParam(Photoquest photoquest, long signedInUserId) {
        boolean isFollowing = getFollowingPhotoquest(signedInUserId, photoquest.getId()) != null;
        photoquest.setIsFollowing(isFollowing);
    }

    private void setPhotoquestsFollowingParam(
                                              Iterable<Photoquest> photoquests,
                                              long signedInUserId) {
        for(Photoquest photoquest : photoquests){
            setPhotoquestFollowingParam(photoquest, signedInUserId);
        }
    }

    private void setPhotoquestsFollowingParamIfSignedIn(
                                              Iterable<Photoquest> photoquests) {
        User signedInUser = getSignedInUser();
        if(signedInUser != null){
            setPhotoquestsFollowingParam(photoquests, signedInUser.getId());
        }
    }

    public Collection<Photoquest> getPhotoQuests(OffsetLimit offsetLimit,
                                                 RatingOrder order) {
        String orderString = getPhotoOrderBy(order);

        SelectParams params = new SelectParams();
        params.offsetLimit = offsetLimit;
        params.ordering = orderString;
        params.foreignFieldsToFill = MysqlObjectMapper.ALL_FOREIGN;

        Collection<Photoquest> result =
                mapper.queryAllObjects(Photoquest.class, params);
        initPhotoquestsInfo(result);

        return result;
    }

    public long getPhotoQuestsCount(String filter) {
        if (Strings.isEmpty(filter)) {
            return mapper.getAllObjectsCount(Photoquest.class);
        } else {
            return advancedRequestsManager.getPhotoquestsByQueryCount(filter);
        }
    }

    public boolean hasFriendship(long user1Id, long user2Id) {
        return getFriendship(user1Id, user2Id) != null;
    }

    public Relationship getFriendship(long user1Id, long user2Id) {
        Relationship friendship = new Relationship();
        friendship.setFromUserId(user1Id);
        friendship.setToUserId(user2Id);
        return mapper.getObjectByPattern(friendship);
    }

    public Relationship getFriendshipOrThrow(long user1Id, long user2Id) {
        Relationship friendship = getFriendship(user1Id, user2Id);
        if(friendship == null){
            throw new FriendshipNotExistsException(user1Id, user2Id);
        }

        return friendship;
    }

    private void addFriendShip(User user1, User user2) {
        Relationship friendship1 = new Relationship();
        Relationship friendship2 = new Relationship();

        friendship1.setType(Relationship.FRIENDSHIP);
        friendship2.setType(Relationship.FRIENDSHIP);

        friendship1.setFromUserId(user1.getId());
        friendship1.setToUserId(user2.getId());
        friendship2.setFromUserId(user2.getId());
        friendship2.setToUserId(user1.getId());

        insertAll(friendship1, friendship2);
    }

    public void addFriend(long userId) {
        if(userId == getSignedInUserOrThrow().getId()){
            throw new IllegalArgumentException("Unable to add yourself as friend");
        }

        try {
            acceptFriendRequest(userId);
        } catch (RelationNotFoundException e) {
            sendFriendRequest(userId);
        }
    }

    public void removeFriend(long userId) {
        try {
            declineFriendRequest(userId);
        } catch (RelationNotFoundException e) {
            try {
                unfollowUser(userId);
            } catch (UserIsNotFollowingException e2) {
                try {
                    cancelFriendRequest(userId);
                } catch (RelationNotFoundException e3) {
                    removeFriendShip(userId);
                }
            }
        }
    }

    private void removeFriendShip(long userId) {
        User user = getSignedInUserOrThrow();
        getUserByIdOrThrow(userId);
        long signedInUserId = user.getId();

        Relationship friendship1 = getFriendshipOrThrow(userId, signedInUserId);
        Relationship friendship2 = getFriendshipOrThrow(signedInUserId, userId);
        deleteAll(friendship1, friendship2);
    }

    public List<User> getUsersByFromUserIdInRelation(long fromUserId, int relationType,
                                                     OffsetLimit offsetLimit,
                                                     RatingOrder order) {
        Relationship relationship = new Relationship();
        relationship.setType(relationType);
        relationship.setFromUserId(fromUserId);

        SelectParams selectParams = new SelectParams();
        selectParams.ordering = getPeopleOrderBy(order);
        selectParams.offsetLimit = offsetLimit;
        return mapper.queryByForeignPattern(relationship, User.class, "toUserId", selectParams);
    }

    public List<User> getUsersByToUserIdInRelation(long toUserId, int relationType,
                                                     OffsetLimit offsetLimit,
                                                     RatingOrder order) {
        Relationship relationship = new Relationship();
        relationship.setType(relationType);
        relationship.setToUserId(toUserId);

        SelectParams selectParams = new SelectParams();
        selectParams.ordering = getPeopleOrderBy(order);
        selectParams.offsetLimit = offsetLimit;
        return mapper.queryByForeignPattern(relationship, User.class, "fromUserId", selectParams);
    }

    public List<User> getFriendsOf(long userId, OffsetLimit offsetLimit, RatingOrder order) {
        return getUsersByFromUserIdInRelation(userId, Relationship.FRIENDSHIP, offsetLimit, order);
    }

    public long getFriendsCount(SearchUsersParams searchParams, Long userId) {
        if(userId == null){
            userId = getSignedInUserOrThrow().getId();
        }

        return advancedRequestsManager.getSearchFriendsCount(searchParams, userId);
    }

    private interface RelationsSearcher {
        List<User> search(String orderBy, long userId);
        RelationStatus getStatus();
    }

    private List<User> searchRelations(Long userId, RatingOrder order,
                                       RelationsSearcher searcher) {
        String orderBy = getPeopleOrderBy(order);
        if (userId == null) {
            User signedInUser = getSignedInUserOrThrow();
            userId = signedInUser.getId();
        }
        List<User> friends = searcher.search(orderBy, userId);

        for(User friend : friends){
            friend.setRelation(searcher.getStatus());
            setUserInfo(friend);
        }

        return friends;
    }

    public List<User> getFriends(final SearchUsersParams params,
                                 final OffsetLimit offsetLimit, RatingOrder order, Long userId) {
        return searchRelations(userId, order, new RelationsSearcher() {
            @Override
            public List<User> search(String orderBy, long userId) {
                return advancedRequestsManager.searchFriends(params, offsetLimit, orderBy, userId);
            }

            @Override
            public RelationStatus getStatus() {
                return RelationStatus.friend;
            }
        });
    }

    public Photoquest getOrCreateSystemPhotoQuest(String photoquestName) {
        Photoquest photoquest = getPhotoQuestByName(photoquestName);
        if(photoquest == null){
            return createSystemPhotoquest(photoquestName);
        }

        return photoquest;
    }

    public Comment getCommentById(long id) {
        return mapper.getObjectById(Comment.class, id);
    }

    public Comment getCommentByIdOrThrow(long id) {
        Comment comment = getCommentById(id);
        if(comment == null){
            throw new CommentNotFoundException(id);
        }

        return comment;
    }

    public Like getLikeById(long id) {
        return mapper.getObjectById(Like.class, id);
    }

    public Like getLikeByUserAndPhotoId(long userId, long photoId) {
        Like like = new Like();
        like.setUserId(userId);
        like.setPhotoId(photoId);
        return mapper.getObjectByPattern(like);
    }

    public Like getLikeByUserAndCommentId(long userId, long commentId) {
        Like like = new Like();
        like.setUserId(userId);
        like.setCommentId(commentId);
        return mapper.getObjectByPattern(like);
    }

    public Like getLikeByIdOrThrow(long id) {
        Like like = getLikeById(id);
        if(like == null){
            throw new LikeNotFoundException(id);
        }

        return like;
    }

    public void fillCommentsData(Collection<Comment> comments) {
        for (Comment comment : comments) {
            fillCommentData(comment);
        }
    }

    public void fillCommentData(Comment comment) {
        Long toUserId = comment.getToUserId();
        if(toUserId != null){
            User toUser = getUserByIdOrThrow(toUserId);
            setAvatar(toUser);
            comment.setToUser(toUser);
        }

        Long userId = comment.getUserId();
        User user = getUserByIdOrThrow(userId);
        setAvatar(user);
        comment.setUser(user);
    }

    public Comment addComment(
                              long photoId, String message) {
        return addComment(photoId, message, null);
    }

    public Collection<Like> getCommentLikes(long commentId, OffsetLimit offsetLimit) {
        Like like = new Like();
        like.setCommentId(commentId);
        return mapper.queryByPattern(like, offsetLimit);
    }

    public Collection<Comment> getCommentsOnComment(long commentId, OffsetLimit offsetLimit) {
        Comment comment = new Comment();
        comment.setToCommentId(commentId);
        return mapper.queryByPattern(comment, offsetLimit);
    }

    private Reply getReplyByLikeId(long likeId) {
        Reply reply = new Reply();
        reply.setType(Reply.LIKE);
        reply.setId(likeId);
        return mapper.getObjectByPattern(reply);
    }

    private Reply getReplyByCommentId(long commentId) {
        Reply reply = new Reply();
        reply.setType(Reply.COMMENT);
        reply.setId(commentId);
        return mapper.getObjectByPattern(reply);
    }

    public void deleteComment(long commentId) {
        User signedInUser = getSignedInUserOrThrow();
        Comment comment = getCommentByIdOrThrow(commentId);
        Long signedInUserId = signedInUser.getId();
        if(!signedInUserId.equals(comment.getUserId())){
            throw new PermissionDeniedException("Unable to delete comment, which is not owned by you");
        }

        deleteComment(commentId, signedInUserId);
    }

    private void deleteComment(long commentId, long userId) {
        advancedRequestsManager.deleteComment(commentId);
        advancedRequestsManager.updateUnreadRepliesCount(userId);
    }

    public Comment addComment(
                           Long photoId, String message, Long toCommentId) {
        User signedInUser = getSignedInUserOrThrow();

        Comment comment = new Comment();
        comment.setMessage(message);
        comment.setUserId(signedInUser.getId());
        setAvatar(signedInUser);
        comment.setUser(signedInUser);

        long toUserId;
        User toUser;
        if(toCommentId != null){
            Comment toComment = getCommentByIdOrThrow(toCommentId);
            comment.setToCommentId(toCommentId);
            toUserId = toComment.getUserId();
            toUser = getUserByIdOrThrow(toUserId);
            comment.setToUserId(toUser.getId());
            setAvatar(toUser);
            comment.setToUser(toUser);

            photoId = toComment.getPhotoId();
            getPhotoByIdOrThrow(photoId);
        } else {
            toUserId = getPhotoByIdOrThrow(photoId).getUserId();
            toUser = getUserByIdOrThrow(toUserId);
        }

        comment.setPhotoId(photoId);

        Reply reply = new Reply();
        reply.setType(Reply.COMMENT);
        reply.setUserId(toUserId);
        toUser.incrementUnreadRepliesCount();

        insert(comment);
        replace(toUser);
        reply.setId(comment.getId());
        insert(reply);

        return comment;
    }

    public Collection<Comment> getCommentsOnPhoto(
                                                  long photoId,
                                                  final Long afterId,
                                                  OffsetLimit offsetLimit) {
        Comment commentPattern = new Comment();
        commentPattern.setPhotoId(photoId);

        SelectParams selectParams = new SelectParams();
        selectParams.offsetLimit = offsetLimit;
        selectParams.ordering = "id desc";

        if (afterId != null) {
            selectParams.additionalWhereClosure = "id > " + afterId;
        }

        Collection<Comment> comments = mapper.queryByPattern(commentPattern, selectParams);

        User signedInUser = getSignedInUser();
        if(signedInUser != null){
            long userId = signedInUser.getId();
            for(Comment comment : comments){
                Like like = getLikeByUserAndCommentId(userId, comment.getId());
                comment.setYourLike(like);
            }
        }

        return comments;
    }

    public Collection<Comment> getCommentsOnPhotoAndFillData(long photoId,
                                                             Long afterId,
                                                             OffsetLimit offsetLimit) {
        Collection<Comment> comments = getCommentsOnPhoto(photoId, afterId, offsetLimit);
        fillCommentsData(comments);
        return comments;
    }

    public Like likePhoto(long photoId) {
        final Photo photo = getPhotoByIdOrThrow(photoId);

        Like like = new Like();
        like.setPhotoId(photoId);

        like = like(like, photo.getUserId());

        final Long photoquestId = photo.getPhotoquestId();
        asyncTaskHandler.execute(new Task() {
            @Override
            public void run(DatabaseManager databaseManager) {
                Photoquest photoquest = databaseManager.getPhotoQuestByIdOrThrow(photoquestId);
                databaseManager.incrementLikesCount(photo, photoquest);
                databaseManager.updatePhotoquestAvatar(photoquest);
            }
        });

        return like;
    }

    public Like likeComment(long commentId) {
        Comment comment = getCommentByIdOrThrow(commentId);

        Like like = new Like();
        like.setCommentId(commentId);
        like.setPhotoId(comment.getPhotoId());

        like = like(like, comment.getUserId());

        incrementLikesCount(comment);

        return like;
    }

    private void incrementLikesCount(Likable... likables) {
        for(Likable likable : likables){
            mapper.increment(likable, "likesCount");
        }
    }

    private void decrementLikesCount(Likable... likables) {
        for(Likable likable : likables){
            mapper.decrement(likable, "likesCount");
        }
    }

    private void incrementUnreadRepliesCount(User user) {
        mapper.increment(user, "unreadRepliesCount");
    }

    private Like like(final Like like, final long toUserId) {
        User signedInUser = getSignedInUserOrThrow();
        like.setUserId(signedInUser.getId());

        String additionalWhere = like.getCommentId() == null ? "commentId is NULL" : null;

        if(mapper.getObjectByPattern(like, additionalWhere) != null){
            throw new LikeExistsException(like);
        }

        insert(like);

        asyncTaskHandler.execute(new Task() {
            @Override
            public void run(DatabaseManager databaseManager) {
                Reply reply = new Reply();
                reply.setId(like.getId());
                reply.setUserId(toUserId);
                reply.setType(Reply.LIKE);

                User toUser = databaseManager.getUserByIdOrThrow(toUserId);
                databaseManager.incrementUnreadRepliesCount(toUser);
                databaseManager.insert(reply);
            }
        });

        like.setUser(signedInUser);

        return like;
    }

    public void unlike(final long likeId) {
        final Like like = getLikeByIdOrThrow(likeId);
        if(!like.getUserId().equals(getSignedInUserOrThrow().getId())){
            throw new PermissionDeniedException("Unable to unlike like owned by another user");
        }

        final Long photoId = like.getPhotoId();
        final Long commentId = like.getCommentId();

        if (delete(like) == 0) {
            throw new LikeNotFoundException(likeId);
        }

        asyncTaskHandler.execute(new Task() {
            @Override
            public void run(DatabaseManager databaseManager) {
                long replyUserId;
                if (commentId != null) {
                    Comment comment = databaseManager.getCommentByIdOrThrow(commentId);
                    databaseManager.decrementLikesCount(comment);
                    replyUserId = comment.getUserId();
                } else {
                    if (photoId == null) {
                        throw new RuntimeException("WTF?");
                    }

                    Photo photo = databaseManager.getPhotoByIdOrThrow(photoId);
                    Photoquest photoquest = databaseManager.getPhotoQuestByIdOrThrow(photo.getPhotoquestId());
                    databaseManager.decrementLikesCount(photo, photoquest);
                    databaseManager.updatePhotoquestAvatar(photoquest);
                    replyUserId = photo.getUserId();
                }

                Reply reply = new Reply();
                reply.setType(Reply.LIKE);
                reply.setUserId(replyUserId);
                reply.setId(likeId);
                databaseManager.deleteReply(reply);
            }
        });
    }

    private void deleteReply(Reply reply) {
        delete(reply);
        advancedRequestsManager.updateUnreadRepliesCount(reply.getUserId());
    }

    private Dialog updateDialog(long user1Id, long user2Id, long lastMessageTime, long lastMessageId) {
        Dialog dialog = new Dialog();
        dialog.setUser1Id(user1Id);
        dialog.setUser2Id(user2Id);
        Dialog storedDialog = mapper.getObjectByPattern(dialog);
        if(storedDialog != null){
            dialog.setId(storedDialog.getId());
        }

        dialog.setLastMessageId(lastMessageId);
        dialog.setLastMessageTime(lastMessageTime);

        if(storedDialog != null){
            Dialog pattern = new Dialog();
            pattern.setUser1Id(user1Id);
            pattern.setUser2Id(user2Id);
            mapper.updateUsingPattern(pattern, dialog);

            pattern.setUser2Id(user1Id);
            pattern.setUser1Id(user2Id);
            dialog.setUser2Id(user1Id);
            dialog.setUser1Id(user2Id);
            mapper.updateUsingPattern(pattern, dialog);
        } else {
            long maxId = mapper.max(Dialog.class, "id", 0l);
            dialog.setId(maxId + 1);
            insert(dialog);
            dialog.setUser2Id(user1Id);
            dialog.setUser1Id(user2Id);
            insert(dialog);
        }

        return dialog;
    }

    private Message addMessage(User fromUser, final User toUser, String messageText) {
        toUser.incrementUnreadMessagesCount();

        final long fromUserId = fromUser.getId();
        final long toUserId = toUser.getId();

        final Message message = new Message();
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setMessage(messageText);

        insert(message);

        asyncTaskHandler.execute(new Task() {
            @Override
            public void run(DatabaseManager databaseManager) {
                Dialog dialog = databaseManager.updateDialog(
                        fromUserId, toUserId, message.getAddingDate(), message.getId());
                message.setDialogId(dialog.getId());
                databaseManager.replace(message);
                databaseManager.replace(toUser);
            }
        });

        return message;
    }

    public Message addMessage(long toUserId, String messageText) {
        User signedInUser = getSignedInUserOrThrow();
        if(toUserId == signedInUser.getId()){
            throw new IllegalArgumentException("Unable to send message to yourself");
        }

        User toUser = getUserByIdOrThrow(toUserId);
        return addMessage(signedInUser, toUser, messageText);
    }

    public void deleteMessage(long id) {
        Message message = getMessageByIdOrThrow(id);
        delete(message);
    }

    public Collection<Message> getMessagesByUserId(long userId, OffsetLimit offsetLimit) {
        Message message = new Message();
        message.setFromUserId(userId);
        return mapper.queryByPattern(message, offsetLimit, "id desc");
    }

    private Dialog getDialogById(long dialogId) {
        Dialog dialog = new Dialog();
        dialog.setId(dialogId);
        return mapper.getObjectByPattern(dialog);
    }

    private Dialog getDialogByIdOrThrow(long dialogId) {
        Dialog dialog = getDialogById(dialogId);
        if(dialog == null){
            throw new DialogNotFoundException(dialogId);
        }

        return dialog;
    }

    public Collection<Message> getMessagesWithUser(long userId,
                                                     OffsetLimit offsetLimit, Long afterId) {
        Dialog dialog = new Dialog();
        dialog.setUser1Id(userId);
        dialog.setUser2Id(getSignedInUserOrThrow().getId());
        dialog = mapper.getObjectByPattern(dialog);
        if(dialog == null){
            return new ArrayList<Message>();
        }

        return getDialogMessages(dialog, offsetLimit, afterId);
    }

    public Collection<Message> getDialogMessages(
                                                 Dialog dialog,
                                                 OffsetLimit offsetLimit,
                                                 Long afterId) {
        User signedInUser = getSignedInUserOrThrow();
        Message pattern = new Message();
        pattern.setDialogId(dialog.getId());

        SelectParams params = new SelectParams();
        params.ordering = "id desc";
        params.offsetLimit = offsetLimit;
        if (afterId != null) {
            params.additionalWhereClosure = "id > " + afterId;
        }

        Collection<Message> messages = mapper.queryByPattern(pattern, params);
        List forUpdate = new ArrayList();

        int readMessagesCount = 0;
        for(Message message : messages){
            if(!message.getRead() && message.getToUserId().equals(signedInUser.getId())){
                readMessagesCount++;
                message.setRead(true);
                forUpdate.add(message);
            }
        }

        signedInUser.setUnreadMessagesCount(signedInUser.getUnreadMessagesCount() - readMessagesCount);
        forUpdate.add(signedInUser);
        replaceAll(forUpdate);

        return messages;
    }

    public Message getMessageByIdAndUserId(long userId, long messageId) {
        Message message = getMessageByIdOrThrow(messageId);
        if(message.getFromUserId() != userId && message.getToUserId() != userId){
            throw new MessageNotOwnedByUserException(userId, messageId);
        }

        return message;
    }

    public Relationship getFriendRequest(long fromUserId, long toUserId) {
        Relationship request = new Relationship();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setType(Relationship.FRIEND_REQUEST);
        return mapper.getObjectByPattern(request);
    }

    public Relationship getFriendRequestOrThrow(long fromUserId, long toUserId) {
        Relationship friendRequest = getFriendRequest(fromUserId, toUserId);
        if(friendRequest == null){
            throw new RelationNotFoundException();
        }

        return friendRequest;
    }

    public Relationship getRelationship(long fromUserId, long toUserId) {
        Relationship relationship = new Relationship();
        relationship.setFromUserId(fromUserId);
        relationship.setToUserId(toUserId);
        return mapper.getObjectByPattern(relationship);
    }

    public Relationship sendFriendRequest(long userId) {
        User signedInUser = getSignedInUserOrThrow();
        User friend = getUserByIdOrThrow(userId);

        long signedInUserId = signedInUser.getId();
        if(hasFriendship(userId, signedInUserId)){
            throw new FriendshipExistsException(userId, signedInUserId);
        }

        if(getFriendRequest(signedInUserId, userId) != null){
            throw new FriendRequestExistsException();
        }

        Relationship friendRequest = new Relationship();
        friendRequest.setToUserId(userId);
        friendRequest.setFromUserId(signedInUserId);
        friendRequest.setType(Relationship.FRIEND_REQUEST);

        signedInUser.incrementSentRequestsCount();
        friend.incrementReceivedRequestsCount();


        replaceAll(signedInUser, friend);
        insert(friendRequest);
        return friendRequest;
    }

    private Relationship deleteFriendRequestOrUnfollow(User fromUser, User toUser) {
        Relationship relationship = getRelationship(fromUser.getId(), toUser.getId());
        if(relationship == null){
            throw new RelationNotFoundException();
        }

        if(relationship.getType() == Relationship.FRIEND_REQUEST){
            fromUser.decrementSentRequestsCount();
            toUser.decrementReceivedRequestsCount();
            replaceAll(fromUser, toUser);
        }

        delete(relationship);
        return relationship;
    }

    private Relationship deleteFriendRequest(User fromUser, User toUser) {
        Relationship friendRequest = getFriendRequestOrThrow(fromUser.getId(),
                toUser.getId());
        delete(friendRequest);
        fromUser.decrementSentRequestsCount();
        toUser.decrementReceivedRequestsCount();
        replaceAll(fromUser, toUser);
        return friendRequest;
    }

    private void cancelFriendRequest(long userId) {
        User signedInUser = getSignedInUserOrThrow();
        User friend = getUserByIdOrThrow(userId);
        deleteFriendRequest(signedInUser, friend);
    }

    private void acceptFriendRequest(long userId) {
        User friend = getUserByIdOrThrow(userId);
        User signedInUser = getSignedInUserOrThrow();
        deleteFriendRequestOrUnfollow(friend, signedInUser);
        addFriendShip(friend, signedInUser);

        Reply reply = new Reply();
        reply.setId(signedInUser.getId());
        reply.setType(Reply.FRIEND_REQUEST_ACCEPTED);
        reply.setUserId(friend.getId());

        friend.incrementUnreadRepliesCount();
        replace(friend);
        insert(reply);
    }

    private void declineFriendRequest(long userId) {
        User friend = getUserByIdOrThrow(userId);
        User signedInUser = getSignedInUserOrThrow();
        deleteFriendRequest(friend, signedInUser);

        Long signedInUserId = signedInUser.getId();
        Long friendId = friend.getId();

        Reply reply = new Reply();
        reply.setId(signedInUserId);
        reply.setType(Reply.FRIEND_REQUEST_DECLINED);
        reply.setUserId(friendId);

        followUser(friendId, signedInUserId);

        friend.incrementUnreadRepliesCount();
        replace(friend);
        insert(reply);
    }

    public Collection<Dialog> getDialogs(OffsetLimit offsetLimit) {
        User signedInUser = getSignedInUserOrThrow();
        Dialog dialogPattern = new Dialog();
        long signedInUserId = signedInUser.getId();
        dialogPattern.setUser1Id(signedInUserId);

        SelectParams selectParams = new SelectParams();
        selectParams.offsetLimit = offsetLimit;
        selectParams.ordering = "lastMessageTime desc";
        selectParams.foreignFieldsToFill = MysqlObjectMapper.ALL_FOREIGN;
        Collection<Dialog> result = mapper.queryByPattern(dialogPattern, selectParams);

        for(Dialog dialog : result){
            if(dialog.getUser1().getId() != signedInUserId){
                dialog.setUser(dialog.getUser1());
            } else {
                dialog.setUser(dialog.getUser2());
            }

            setAvatar(dialog.getUser());
        }

        return result;
    }

    public long getDialogsCount() {
        User signedInUser = getSignedInUserOrThrow();
        Dialog dialogPattern = new Dialog();
        long signedInUserId = signedInUser.getId();
        dialogPattern.setUser1Id(signedInUserId);

        return mapper.getCountByPattern(dialogPattern);
    }

    private Integer getRandomCityId() {
        int max = mapper.executeFirstRowValueSelectSql("select max(id) from city", Integer.class);
        int min = mapper.executeFirstRowValueSelectSql("select min(id) from city", Integer.class);
        for (int i = 0; i < 100; i++) {
            int randomCityId = MathUtils.randInt(min, max);
            if (mapper.getObjectById(City.class, randomCityId) != null) {
                return randomCityId;
            }
        }

        throw new RuntimeException("Unexpected WTF, try again");
    }

    public List<User> registerRandomUsers(int startId, int count, String password)
            throws IOException {
        List<Response> data = new ArrayList<Response>();
        while (data.size() < count){
            data.addAll(RandomUserGenerator.generate(count - data.size()));
        }

        List<User> result = new ArrayList<User>(count);

        for(Response userData : data){
            User user = new User();
            user.setNameAndLastName(userData.name, userData.lastName);
            user.setLogin("user" + startId++);
            user.setPassword(password);
            user.setGender(userData.gender == Gender.male);

            user.setCityId(getRandomCityId());

            InputStream avatar = IOUtilities.getBufferedInputStreamFromUrl(userData.largeAvatar);
            user = registerUser(user, avatar);
            result.add(user);
        }

        return result;
    }

    public Photo getMostRatedPhotoOfPhotoquest(long photoQuestId) {
        Photo pattern = new Photo();
        pattern.setPhotoquestId(photoQuestId);
        return mapper.getObjectWithMaxFieldByPattern(pattern, "likesCount", null);
    }

    public void updatePhotoquestAvatar(Photoquest photoquest) {
        Long avatarId = photoquest.getAvatarId();
        Photo photo = getMostRatedPhotoOfPhotoquest(photoquest.getId());
        if (photo != null) {
            Long photoId = photo.getId();
            if(!photoId.equals(avatarId)){
                photoquest.setAvatarId(photoId);
                replace(photoquest);
            }
        } else if(avatarId != null) {
            photoquest.setAvatarId(null);
            mapper.executeModifySQL(PHOTOQUEST_SET_NULL_AVATAR,
                    Collections.singletonMap("id", (Object)photoquest.getId()));
        }
    }

    public long getPhotoInPhotoquestPosition(long photoId, RatingOrder order) {
        Photo photo = getPhotoByIdOrThrow(photoId);
        return getPhotoInPhotoquestPosition(photo, order);
    }

    private long getPhotoInPhotoquestPosition(Photo photo, RatingOrder order) {
        Photo pattern = new Photo();
        pattern.setPhotoquestId(photo.getPhotoquestId());
        return mapper.getObjectPosition(photo, pattern, getPhotoOrderBy(order));
    }

    public static class PhotoFillParams {
        public Long userId;
        public Long photoquestId;
        public PhotoCategory category;
    }

    private long getUserAvatarsCount(long userId) {
        Photoquest avatar = getOrCreateSystemPhotoQuest(AVATAR_QUEST_NAME);
        Photo photo = new Photo();
        photo.setUserId(userId);
        photo.setPhotoquestId(avatar.getId());
        return mapper.getCountByPattern(photo);
    }

    public Photo getPhotoAndFillInfo(long photoId, PhotoFillParams params) {
        Photo photo = mapper.getObjectById(Photo.class, photoId, MysqlObjectMapper.ALL_FOREIGN);
        if(photo == null){
            throw new PhotoNotFoundException(photoId);
        }

        User signedInUser = getSignedInUser();
        if(signedInUser != null){
            PhotoView pattern = new PhotoView();
            pattern.setPhotoId(photoId);
            pattern.setUserId(signedInUser.getId());
            PhotoView photoView = mapper.getObjectByPattern(pattern);
            if(photoView == null){
                photoView = pattern;
            }

            long currentTimeMillis = System.currentTimeMillis();
            if(photoView == pattern || currentTimeMillis -
                    photoView.getAddingDate() >= PHOTO_VIEW_PERIOD){
                photoView.setAddingDate(currentTimeMillis);
                photo.incrementViewsCount();
                replace(photo);

                if(photoView == pattern){
                    insert(photoView);
                } else {
                    replace(photoView);
                }
            }
        }

        setPhotoInfo(photo);

        if(params.userId != null){
            if (params.category != PhotoCategory.avatar) {
                photo.setShowNextPrevButtons(getPhotosOfUserCount(params.userId) > 1);
            } else {
                photo.setShowNextPrevButtons(getUserAvatarsCount(params.userId) > 1);
            }
        } else {
            Long photoquestId = params.photoquestId;
            if(photoquestId != null) {
                long count;
                PhotoCategory category = params.category;
                if(category == PhotoCategory.all){
                    count = getPhotosOfPhotoquestCount(photoquestId);
                } else if(category == PhotoCategory.friends) {
                    count = getPhotosOfFriendsByPhotoquestCount(photoquestId);
                } else {
                    count = getPhotosOfSignedInUserByPhotoquestCount(photoquestId);
                }

                photo.setShowNextPrevButtons(count > 1);
            }
        }

        return photo;
    }

    public List<AutoCompleteResult> getGoogleLocationSuggestions(String query) throws IOException {
        return googlePlacesSearcher.performAutoCompleteCitiesSearch(query);
    }

    public List<LocationSuggestion> getCountrySuggestions(String query) {
        return advancedRequestsManager.getCountrySuggestions(query, 10, lang);
    }

    public List<LocationSuggestion> getCitySuggestions(Integer countryId, String query) {
        return advancedRequestsManager.getCitySuggestions(countryId, query, 10, lang);
    }

    public UserStats getUserStats() {
        User user = getSignedInUserOrThrow();
        UserStats stats = new UserStats();
        stats.setFriendRequestsCount(user.getReceivedRequestsCount());
        stats.setUnreadMessagesCount(user.getUnreadMessagesCount());
        Long unreadRepliesCount = user.getUnreadRepliesCount();
        stats.setUnreadRepliesCount(unreadRepliesCount == null ? 0 : unreadRepliesCount);
        return stats;
    }

    private void readAllReplies(User user) {
        Reply pattern = new Reply();
        pattern.setRead(false);
        pattern.setUserId(user.getId());

        Reply values = new Reply();
        values.setRead(true);

        mapper.updateUsingPattern(pattern, values);
        user.setUnreadRepliesCount(0l);
        replace(user);
    }

    private Collection<Reply> getReplies(User user, OffsetLimit offsetLimit) {
        readAllReplies(user);

        Reply pattern = new Reply();
        pattern.setUserId(user.getId());

        SelectParams params = new SelectParams();
        params.ordering = "addingDate desc";
        params.offsetLimit = offsetLimit;

        return mapper.queryByPattern(pattern, params);
    }

    private void setPhoto(WithPhoto withPhoto) {
        Long photoId = withPhoto.getPhotoId();
        if (photoId != null) {
            withPhoto.setPhoto(HttpUtilities.getBaseUrl(request) +
                    Photo.IMAGE_URL_PATH + photoId + ".jpg");
        }
    }

    public Collection<ReplyResponse> getRepliesWithFullInfo(OffsetLimit offsetLimit) {
        User signedInUser = getSignedInUserOrThrow();
        signedInUser.setUnreadRepliesCount(0l);
        replace(signedInUser);

        Collection<Reply> replies = getReplies(signedInUser, offsetLimit);

        Collection<ReplyResponse> replyResponses = new ArrayList<ReplyResponse>(replies.size());
        for(Reply reply : replies){
            ReplyResponse replyResponse = new ReplyResponse();
            int type = reply.getType();

            User user;
            Long id = reply.getId();
            if(type == Reply.COMMENT){
                Comment comment = getCommentByIdOrThrow(id);
                setPhoto(comment);
                replyResponse.setComment(comment);
                user = getUserByIdOrThrow(comment.getUserId());
                setPhoto(comment);
            } else if(type == Reply.FRIEND_REQUEST_ACCEPTED || type == Reply.FRIEND_REQUEST_DECLINED) {
                user = getUserByIdOrThrow(id);
            } else if(type == Reply.LIKE) {
                Like like = getLikeByIdOrThrow(id);
                setPhoto(like);
                replyResponse.setLike(like);
                user = getUserByIdOrThrow(like.getUserId());
            } else {
                throw new RuntimeException("type is not supported, corrupted database");
            }

            replyResponse.setType(type);
            replyResponse.setAddingDate(reply.getAddingDate());
            setAvatar(user);
            replyResponse.setUser(user);
            replyResponses.add(replyResponse);
        }

        return replyResponses;
    }

    public long getRepliesCount() {
        User user = getSignedInUserOrThrow();
        Reply reply = new Reply();
        reply.setUserId(user.getId());
        return mapper.getCountByPattern(reply);
    }

    private List<User> getFriendRequests(OffsetLimit offsetLimit, boolean received) {
        User signedInUser = getSignedInUserOrThrow();
        Long signedInUserId = signedInUser.getId();

        Relationship pattern = new Relationship();
        pattern.setType(Relationship.FRIEND_REQUEST);
        if (received) {
            pattern.setToUserId(signedInUserId);
        } else {
            pattern.setFromUserId(signedInUserId);
        }

        SelectParams params = new SelectParams();
        params.ordering = "addingDate desc";
        params.offsetLimit = offsetLimit;
        if(received){
            params.foreignFieldsToFill = Arrays.asList("fromUser");
        } else {
            params.foreignFieldsToFill = Arrays.asList("toUser");
        }

        Collection<Relationship> friendRequests = mapper.queryByPattern(pattern, params);
        List<User> result = new ArrayList<User>(friendRequests.size());

        for(Relationship friendRequest : friendRequests){
            User friend = received ? friendRequest.getFromUser() : friendRequest.getToUser();
            setUserInfo(friend);
            friend.setRelation(received ? RelationStatus.request_received : RelationStatus.request_sent);
            result.add(friend);
        }

        return result;
    }

    public List<User> getReceivedFriendRequests(final SearchUsersParams searchParams,
                                                final OffsetLimit offsetLimit, RatingOrder order) {
        return searchRelations(null, order, new RelationsSearcher() {
            @Override
            public List<User> search(String orderBy, long userId) {
                return advancedRequestsManager.searchReceivedRequests(searchParams, offsetLimit, orderBy, userId);
            }

            @Override
            public RelationStatus getStatus() {
                return RelationStatus.request_received;
            }
        });
    }

    public List<User> getSentFriendRequests(final SearchUsersParams searchParams,
                                            final OffsetLimit offsetLimit, RatingOrder order) {
        return searchRelations(null, order, new RelationsSearcher() {
            @Override
            public List<User> search(String orderBy, long userId) {
                return advancedRequestsManager.searchSentRequests(searchParams, offsetLimit, orderBy, userId);
            }

            @Override
            public RelationStatus getStatus() {
                return RelationStatus.request_sent;
            }
        });
    }

    private FollowingPhotoquest getFollowingPhotoquest(long userId, long photoquestId) {
        FollowingPhotoquest pattern = new FollowingPhotoquest();
        pattern.setUserId(userId);
        pattern.setPhotoquestId(photoquestId);
        return mapper.getObjectByPattern(pattern);
    }

    private FollowingPhotoquest getFollowingPhotoquestOrThrow(long userId, long photoquestId) {
        FollowingPhotoquest result = getFollowingPhotoquest(userId, photoquestId);
        if(result == null){
            throw new PhotoquestIsNotFollowingException(userId, photoquestId);
        }

        return result;
    }

    public FollowingPhotoquest followPhotoquest(Photoquest photoquest) {
        User signedInUser = getSignedInUserOrThrow();
        Long signedInUserId = signedInUser.getId();

        long photoquestId = photoquest.getId();
        FollowingPhotoquest followingPhotoquest = getFollowingPhotoquest(signedInUserId, photoquestId);
        if(followingPhotoquest != null){
            throw new PhotoquestIsFollowingException();
        }

        followingPhotoquest = new FollowingPhotoquest();
        followingPhotoquest.setUserId(signedInUserId);
        followingPhotoquest.setPhotoquestId(photoquestId);

        insert(followingPhotoquest);
        return followingPhotoquest;
    }

    public FollowingPhotoquest followPhotoquest(long photoquestId) {
        Photoquest photoquest = getPhotoQuestByIdOrThrow(photoquestId);
        return followPhotoquest(photoquest);
    }

    public void unfollowPhotoquest(long photoquestId) {
        User signedInUser = getSignedInUserOrThrow();
        Long signedInUserId = signedInUser.getId();

        FollowingPhotoquest followingPhotoquest = getFollowingPhotoquestOrThrow(signedInUserId, photoquestId);
        delete(followingPhotoquest);
    }

    public Collection<Photoquest> getFollowingPhotoquests(long userId,
                                                          String filter,
                                                          OffsetLimit offsetLimit,
                                                          RatingOrder order) {
        String orderBy = getPhotoOrderBy(order);
        Collection<Photoquest> result =
                advancedRequestsManager.getFollowingPhotoquests(userId, orderBy, filter, offsetLimit);
        User signedInUser = getSignedInUser();
        for(Photoquest photoquest : result){
            if (signedInUser == null) {
                photoquest.setIsFollowing(null);
            } else {
                Long signedInUserId = signedInUser.getId();
                if(signedInUserId.equals(userId)){
                    photoquest.setIsFollowing(true);
                } else {
                    Long photoquestId = photoquest.getId();
                    photoquest.setIsFollowing(getFollowingPhotoquest(signedInUserId, photoquestId) != null);
                }
            }
            setAvatar(photoquest);
        }

        initUserAvatars(result);

        return result;
    }

    public long getPerformedPhotoquestsCount(String filter, long userId) {
        if (Strings.isEmpty(filter)) {
            PerformedPhotoquest pattern = new PerformedPhotoquest();
            pattern.setUserId(userId);

            return mapper.getCountByPattern(pattern);
        } else {
            return advancedRequestsManager.getPerformedPhotoquestsByQueryCount(filter, userId);
        }
    }

    public Collection<Photoquest> getPerformedPhotoquests(long userId,
                                                          OffsetLimit offsetLimit,
                                                          String filter,
                                                          RatingOrder order) {
        String orderBy = getPhotoOrderBy(order);
        Collection<Photoquest> result =
                advancedRequestsManager.getPerformedPhotoquests(userId, orderBy, filter, offsetLimit);

        initPhotoquestsInfo(result);

        return result;
    }

    public long getFollowingPhotoquestsCount(String filter, long userId) {
        if (Strings.isEmpty(filter)) {
            FollowingPhotoquest pattern = new FollowingPhotoquest();
            pattern.setUserId(userId);

            return mapper.getCountByPattern(pattern);
        } else {
            return advancedRequestsManager.getFollowingPhotoquestsByQueryCount(filter, userId);
        }
    }

    private Relationship followUser(long fromUserId, long toUserId) {
        Relationship relationship = new Relationship();
        relationship.setFromUserId(fromUserId);
        relationship.setToUserId(toUserId);
        relationship.setType(Relationship.FOLLOWS);
        insert(relationship);
        return relationship;
    }

    private void unfollowUser(long fromUserId, long toUserId) {
        Relationship relationship = new Relationship();
        relationship.setFromUserId(fromUserId);
        relationship.setToUserId(toUserId);
        relationship.setType(Relationship.FOLLOWS);

        if(mapper.getObjectByPattern(relationship) == null){
            throw new UserIsNotFollowingException();
        }

        delete(relationship);
    }

    private void setAvatarAndRelation(Collection<User> users, RelationStatus status) {
        for(User user : users){
            setAvatar(user);
            user.setRelation(status);
        }
    }

    private Collection<User> getFollowers(
            
            long followingUserId,
            OffsetLimit offsetLimit,
            RatingOrder order) {
        Collection<User> users = getUsersByToUserIdInRelation(followingUserId, Relationship.FOLLOWS,
                offsetLimit, order);
        setAvatarAndRelation(users, RelationStatus.followed);

        return users;
    }

    private Collection<User> getFollowingUsers(
            
            long followerUserId,
            OffsetLimit offsetLimit,
            RatingOrder order) {
        Collection<User> users = getUsersByFromUserIdInRelation(followerUserId, Relationship.FOLLOWS,
                offsetLimit, order);
        setAvatarAndRelation(users, RelationStatus.follows);

        return users;
    }

    public Collection<User> getFollowingUsers(
                                              OffsetLimit offsetLimit,
                                              RatingOrder order) {
        User signedInUser = getSignedInUserOrThrow();
        return getFollowingUsers(signedInUser.getId(), offsetLimit, order);
    }

    public Collection<User> getFollowers(
                                         OffsetLimit offsetLimit,
                                         RatingOrder order) {
        User signedInUser = getSignedInUserOrThrow();
        return getFollowers(signedInUser.getId(), offsetLimit, order);
    }

    public void unfollowUser(long toUserId) {
        Long fromUserId = getSignedInUserOrThrow().getId();
        unfollowUser(fromUserId, toUserId);
    }

    private void fillActions(Iterable<Action> actions) {
        for(Action action : actions){
            fillAction(action);
        }
    }

    private void fillAction(Action action) {

        Photo photo = action.getPhoto();
        if (photo != null) {
            initPhotoUrl(photo);
            initYourLikeParameter(photo);
        }

        User user = action.getUser();
        if (user != null) {
            setAvatar(user);
        }
    }

    public Collection<Action> getNews(OffsetLimit offsetLimit) {
        User signedInUser = getSignedInUserOrThrow();
        Collection<Action> actions = advancedRequestsManager.getNews(signedInUser.getId(), offsetLimit);
        fillActions(actions);
        return actions;
    }

    public long getNewsCount() {
        User signedInUser = getSignedInUserOrThrow();
        Feed feed = new Feed();
        feed.setUserId(signedInUser.getId());
        return mapper.getCountByPattern(feed);
    }

    public List<Action> getUserNews(long userId, OffsetLimit offsetLimit) {
        Action pattern = new Action();
        pattern.setUserId(userId);
        SelectParams params = new SelectParams();
        params.foreignFieldsToFill = Arrays.asList("photoquest", "photo");
        params.offsetLimit = offsetLimit;
        params.ordering = "id desc";
        List<Action> actions = mapper.queryByPattern(pattern, params);
        fillActions(actions);
        return actions;
    }

    public long getUserNewsCount(long userId) {
        Action pattern = new Action();
        pattern.setUserId(userId);
        return mapper.getCountByPattern(pattern);
    }

    public void clearRatingAndViews() {
        advancedRequestsManager.clearRatingAndViews();
    }

    public void initDatabase() {
        OutputStream progressStream = null;
        try {
            progressStream = new FileOutputStream("progress.txt");
        } catch (FileNotFoundException e) {

        }
        MysqlTablesCreator tablesCreator = new MysqlTablesCreator(mapper);
        tablesCreator.updateAndCreateTables(Arrays.asList(
                Action.class,
                Comment.class,
                Dialog.class,
                Feed.class,
                FollowingPhotoquest.class,
                Likable.class,
                Like.class,
                Message.class,
                PerformedPhotoquest.class,
                Photo.class,
                Photoquest.class,
                PhotoquestSearch.class,
                PhotoquestView.class,
                PhotoView.class,
                ProfileView.class,
                Relationship.class,
                RelationStatus.class,
                Reply.class,
                User.class,
                CaptchaInfo.class,
                com.tiksem.pq.data.City.class,
                com.tiksem.pq.data.Country.class,
                ProgressOperation.class,
                ApiRequest.class
        ), progressStream, "\n");
    }

    public void clearDatabase() throws IOException {
        new MysqlTablesCreator(mapper).clearDatabase("city", "country");
        FileUtils.deleteDirectory(new File("images"));
    }

    public void dropTables() {
        new MysqlTablesCreator(mapper).dropTables();
    }

    public List<ProgressOperation> getProgressOperations() {
        return mapper.queryAllObjects(ProgressOperation.class, new OffsetLimit(0, 500), "id desc");
    }

    public void updateNewFlag() {
        advancedRequestsManager.updateNewFlag();
    }

    public void clearOldCaptchas(long delay) {
        captchaManager.clearOldCaptchas(delay);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    public MysqlObjectMapper getMapper() {
        return mapper;
    }
}
