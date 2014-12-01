package com.tiksem.pq.db;

import com.tiksem.pq.data.*;
import com.tiksem.pq.data.Likable;
import com.tiksem.pq.db.exceptions.*;
import com.tiksem.pq.http.HttpUtilities;
import com.utils.framework.google.places.*;
import com.utils.framework.io.Network;
import com.utils.framework.randomuser.RandomUserGenerator;
import com.utils.framework.randomuser.Response;
import org.springframework.web.multipart.MultipartFile;

import javax.jdo.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

/**
 * Created by CM on 10/30/2014.
 */
public class DatabaseManager {
    private static final PersistenceManagerFactory factory;

    private static final String DEFAULT_AVATAR_URL = "/images/empty_avatar.png";
    public static final String AVATAR_QUEST_NAME = "Avatar";

    private static final String MOST_RATED_PHOTO_MAX_ORDERING = "likesCount descending, addingDate descending, " +
            "id descending";

    private static final String GOOGLE_API_KEY = "AIzaSyAfhfIJpCrb29TbTafQ1UWSqqaSaOuVCIg";

    private final PersistenceManager persistenceManager;

    private ImageManager imageManager = new FileSystemImageManager("images");
    private GooglePlacesSearcher googlePlacesSearcher = new GooglePlacesSearcher(GOOGLE_API_KEY);

    static {
        factory = DBUtilities.createMySQLConnectionFactory("PhotoQuest");
    }

    public DatabaseManager() {
        persistenceManager = factory.getPersistenceManager();
    }

    private <T> T makePersistent(T object) {
        return DBUtilities.makePersistent(persistenceManager, object);
    }

    private <T> T[] makeAllPersistent(T... objects) {
        return DBUtilities.makeAllPersistent(persistenceManager, objects);
    }

    private void deletePersistent(Object object) {
        DBUtilities.deletePersistent(persistenceManager, object);
    }

    private void deleteAllPersistent(Collection objects) {
        DBUtilities.deleteAllPersistent(persistenceManager, objects);
    }

    public User getUserById(long id) {
        return DBUtilities.getObjectById(persistenceManager, User.class, id);
    }

    public User getUserByIdOrThrow(long id) {
        User user = getUserById(id);
        if (user == null) {
            throw new UnknownUserException(String.valueOf(id));
        }

        return user;
    }

    public User getUserByLogin(String login) {
        User user = new User();
        user.setLogin(login);
        return DBUtilities.getObjectByPattern(persistenceManager, user);
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

    public User login(HttpServletRequest request, String login, String password) {
        User user = getUserByLogin(login);
        if (user != null && user.getPassword().equals(password)) {
            setAvatar(request, user);
            return user;
        }

        return null;
    }

    public User loginOrThrow(HttpServletRequest request, String login, String password) {
        User user = login(request, login, password);
        if (user == null) {
            throw new LoginFailedException();
        }

        return user;
    }

    public User getSignedInUser(HttpServletRequest request) {
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

        return user;
    }

    public User getSignedInUserOrThrow(HttpServletRequest request) {
        User user = getSignedInUser(request);
        if (user == null) {
            throw new UserIsNotSignInException();
        }

        return user;
    }

    public Photoquest getPhotoQuestByName(String photoquestName) {
        Photoquest photoquest = new Photoquest();
        photoquest.setName(photoquestName);
        return DBUtilities.getObjectByPattern(persistenceManager, photoquest);
    }

    public Photoquest getPhotoQuestById(long id) {
        Photoquest photoquest = new Photoquest();
        photoquest.setId(id);
        return DBUtilities.getObjectByPattern(persistenceManager, photoquest);
    }

    public Photoquest getPhotoQuestByIdOrThrow(long id) {
        Photoquest photoquest = getPhotoQuestById(id);
        if (photoquest == null) {
            throw new PhotoquestNotFoundException(String.valueOf(id));
        }

        return photoquest;
    }

    public Photoquest createSystemPhotoquest(String photoquestName) {
        Photoquest photoquest = Photoquest.withZeroViewsAndLikes(photoquestName);
        return makePersistent(photoquest);
    }

    public Photoquest createPhotoQuest(HttpServletRequest request, String photoquestName) {
        User user = getSignedInUserOrThrow(request);
        Photoquest photoquest = getPhotoQuestByName(photoquestName);
        if (photoquest != null) {
            throw new PhotoquestExistsException(photoquestName);
        }

        photoquest = Photoquest.withZeroViewsAndLikes(photoquestName);
        photoquest.setUserId(user.getId());

        return makePersistent(photoquest);
    }

    public Collection<Photoquest> getPhotoquestsCreatedByUser(long userId, OffsetLimit offsetLimit, RatingOrder order) {
        Photoquest photoquest = new Photoquest();
        photoquest.setUserId(userId);
        return DBUtilities.queryByPattern(persistenceManager, photoquest, offsetLimit);
    }

    public long getPhotoquestsCountCreatedByUser(long userId) {
        Photoquest photoquest = new Photoquest();
        photoquest.setUserId(userId);
        return DBUtilities.queryCountByPattern(persistenceManager, photoquest);
    }

    private Collection<PerformedPhotoquest> getPerformedPhotoquestsByUser(long userId, OffsetLimit offsetLimit) {
        getUserByIdOrThrow(userId);
        PerformedPhotoquest performedPhotoquest = new PerformedPhotoquest();
        performedPhotoquest.setUserId(userId);
        return DBUtilities.queryByPattern(persistenceManager, performedPhotoquest, offsetLimit);
    }

    public Collection<Photoquest> getPhotoquestsPerformedByUser(HttpServletRequest request, long userId,
                                                                OffsetLimit offsetLimit) {
        Collection<PerformedPhotoquest> performedPhotoquests = getPerformedPhotoquestsByUser(userId, offsetLimit);
        Collection<Photoquest> result = new ArrayList<Photoquest>();
        for (PerformedPhotoquest performedPhotoquest : performedPhotoquests) {
            Photoquest photoquest = getPhotoQuestByIdOrThrow(performedPhotoquest.getPhotoquestId());
            result.add(photoquest);
        }

        setAvatar(request, result);
        return result;
    }

    public Collection<Photoquest> getPhotoquestsPerformedBySignedInUser(HttpServletRequest request,
                                                                        OffsetLimit offsetLimit) {
        User user = getSignedInUserOrThrow(request);
        return getPhotoquestsPerformedByUser(request, user.getId(), offsetLimit);
    }

    public Collection<Photoquest> getPhotoquestsCreatedBySignedInUser(HttpServletRequest request,
                                                                      OffsetLimit offsetLimit,
                                                                      RatingOrder order) {
        return getPhotoquestsCreatedByUser(getSignedInUserOrThrow(request).getId(), offsetLimit, order);
    }

    public long getPhotoquestsCountCreatedBySignedInUser(HttpServletRequest request) {
        return getPhotoquestsCountCreatedByUser(getSignedInUserOrThrow(request).getId());
    }

    public void update(HttpServletRequest request, Object... objects) {
        makeAllPersistent(objects);

        for (Object object : objects) {
            if (object instanceof WithAvatar) {
                WithAvatar withAvatar = (WithAvatar) object;
                setAvatar(request, withAvatar);
            }
        }
    }

    private Location getLocationById(String id) {
        Location location = new Location();
        location.setId(id);
        return DBUtilities.getObjectByPattern(persistenceManager, location);
    }

    private Location getLocationByIdOrThrow(String id) {
        Location location = getLocationById(id);
        if(location == null){
            throw new LocationNotFoundException(id);
        }

        return location;
    }

    private Location addLocation(String placeId, City city) {
        Location location = new Location();
        location.setId(placeId);
        LocationInfo locationInfo = new LocationInfo();
        locationInfo.setCity(city.name);
        locationInfo.setCountry(city.country);
        location.setInfo(Language.en, locationInfo);
        return makePersistent(location);
    }

    private User registerUser(User user) throws IOException {
        String login = user.getLogin();

        if (getUserByLogin(login) != null) {
            throw new UserExistsRegisterException(login);
        }

        String locationId = user.getLocation();
        Location location = getLocationById(locationId);
        if(location == null){
            City city = getCityOrThrow(locationId);
            location = addLocation(locationId, city);
        }

        user.setCountryCode(location.getCountryCode());

        return makePersistent(user);
    }

    public City getCityOrThrow(String location) throws IOException {
        try {
            City city = googlePlacesSearcher.getCityByPlaceId(location);
            if(city == null){
                throw new InvalidLocationException();
            }

            return city;
        } catch (PlaceIsNotCityException e) {
            throw new InvalidLocationException(e);
        }
    }

    public User registerUser(HttpServletRequest request, User user, MultipartFile avatar) throws IOException {
        byte[] avatarBytes = null;
        try {
            avatarBytes = avatar.getBytes();
        } catch (IOException e) {

        }

        return registerUser(request, user, avatarBytes);
    }

    public User registerUser(HttpServletRequest request, User user, byte[] avatar) throws IOException {
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
            photo = addPhoto(request, photo, avatar);
            user.setAvatarId(photo.getId());

            if (user.getLogin() == null) {
                throw new RuntimeException("WTF?");
            }

            update(request, user);
        }

        return user;
    }

    public Collection<User> getAllUsers(HttpServletRequest request, OffsetLimit offsetLimit) {
        return getAllUsers(request, true, false, offsetLimit);
    }

    public Collection<User> getAllUsersWithCheckingRelationShip(HttpServletRequest request, OffsetLimit offsetLimit) {
        return getAllUsers(request, false, true, offsetLimit);
    }

    public long getAllUsersCount(HttpServletRequest request, boolean includeSignedInUser) {
        long result = DBUtilities.getAllObjectsOfClassCount(persistenceManager,
                User.class);
        if(includeSignedInUser && getSignedInUser(request) != null){
            result--;
        }

        return result;
    }

    public Collection<User> getAllUsers(HttpServletRequest request, boolean includeSignedInUser,
                                        boolean fillRelationshipData, OffsetLimit offsetLimit) {
        Collection<User> users = null;
        User signedInUser = null;

        long signedInUserId = 0;

        if (includeSignedInUser) {
            users = DBUtilities.getAllObjectsOfClass(persistenceManager,
                    User.class, offsetLimit);
        } else {
            signedInUser = getSignedInUser(request);
            if(signedInUser == null){
                users = DBUtilities.getAllObjectsOfClass(persistenceManager,
                        User.class, offsetLimit);
            } else {
                signedInUserId = signedInUser.getId();
                User user = new User();
                user.setId(signedInUserId);
                users = DBUtilities.queryByExcludePattern(persistenceManager, user, offsetLimit);
            }
        }

        setAvatar(request, users);

        if(fillRelationshipData && signedInUser != null){
            for(User user : users){
                Long userId = user.getId();
                boolean areFriends = hasFriendship(signedInUserId, userId);
                if (areFriends) {
                    user.setRelation(RelationStatus.friend);
                } else {
                    if(getFriendRequest(signedInUserId, userId) != null){
                        user.setRelation(RelationStatus.request_sent);
                    } else if(getFriendRequest(userId, signedInUserId) != null) {
                        user.setRelation(RelationStatus.request_received);
                    }
                }
            }
        }

        return users;
    }

    public void deleteAllUsers(HttpServletRequest request, OffsetLimit offsetLimit) {
        deleteAllPersistent(getAllUsers(request, offsetLimit));
    }

    public void deleteAllPhotos() {
        DBUtilities.deleteAllObjectsOfClass(persistenceManager, Photo.class);
        DBUtilities.deleteAllObjectsOfClass(persistenceManager, BitmapData.class);
    }

    public PerformedPhotoquest getOrCreatePerformedPhotoquest(long userId, long photoquestId) {
        PerformedPhotoquest pattern = new PerformedPhotoquest();
        pattern.setUserId(userId);
        pattern.setPhotoquestId(photoquestId);
        PerformedPhotoquest performedPhotoquest = DBUtilities.getObjectByPattern(persistenceManager, pattern);
        if(performedPhotoquest == null){
            return pattern;
        }

        return performedPhotoquest;
    }

    private Photo addPhoto(HttpServletRequest request, Photo photo, byte[] bitmapData) {
        Long userId = photo.getUserId();
        if(userId == null){
            User user = getSignedInUserOrThrow(request);
            userId = user.getId();
        }

        photo.setLikesCount(0l);
        photo.setUserId(userId);

        photo = makePersistent(photo);
        imageManager.saveImage(photo.getId(), bitmapData);
        makePersistent(getOrCreatePerformedPhotoquest(userId, photo.getPhotoquestId()));

        return photo;
    }

    public Photo addPhotoToPhotoquest(HttpServletRequest request,
                                      long photoquestId, MultipartFile file) throws IOException {
        Photoquest photoquest = getPhotoQuestByIdOrThrow(photoquestId);

        if (!file.isEmpty()) {
            Photo photo = new Photo();
            photo.setPhotoquestId(photoquestId);
            byte[] bytes = file.getBytes();
            addPhoto(request, photo, bytes);
            updatePhotoquestAvatar(photoquest);
            return photo;
        } else {
            throw new FileIsEmptyException();
        }
    }

    public Photo getPhotoById(long id) {
        return DBUtilities.getObjectById(persistenceManager, Photo.class, id);
    }

    public Photo getPhotoByIdOrThrow(long id) {
        Photo photo = getPhotoById(id);
        if(photo == null){
            throw new PhotoNotFoundException(id);
        }

        return photo;
    }

    public Message getMessageById(long id) {
        return DBUtilities.getObjectById(persistenceManager, Message.class, id);
    }

    public Message getMessageByIdOrThrow(long id) {
        Message message = getMessageById(id);
        if(message == null){
            throw new MessageNotFoundException(id);
        }

        return message;
    }

    public byte[] getBitmapDataByPhotoId(long id) {
        return imageManager.getImageById(id);
    }

    public byte[] getBitmapDataByPhotoIdOrThrow(long id) {
        byte[] result = getBitmapDataByPhotoId(id);
        if(result == null){
            throw new ResourceNotFoundException();
        }

        return result;
    }

    private void initPhotoUrl(Photo photo, HttpServletRequest request) {
        Long photoId = photo.getId();
        if(photoId == null){
            return;
        }

        photo.setUrl(HttpUtilities.getBaseUrl(request) + Photo.IMAGE_URL_PATH + photoId);
    }

    private void initPhotosUrl(Iterable<Photo> photos, HttpServletRequest request) {
        for(Photo photo : photos){
            initPhotoUrl(photo, request);
        }
    }

    public void initYourLikeParameter(HttpServletRequest request, Iterable<Photo> photos) {
        User signedInUser = getSignedInUser(request);
        if(signedInUser != null){
            Long signedInUserId = signedInUser.getId();
            for(Photo photo : photos){
                Like like = getLikeByUserAndPhotoId(signedInUserId, photo.getId());
                photo.setYourLike(like);
            }
        }
    }

    public void initYourLikeParameter(HttpServletRequest request, Photo photos) {
        initYourLikeParameter(request, Collections.singletonList(photos));
    }

    public Collection<Photo> getAllPhotos(HttpServletRequest request, OffsetLimit offsetLimit) {
        Collection<Photo> photos = DBUtilities.getAllObjectsOfClass(persistenceManager, Photo.class, offsetLimit);
        initPhotosUrl(photos, request);
        return photos;
    }

    public Collection<Photo> getPhotosOfPhotoquest(HttpServletRequest request, long photoQuestId,
                                                   OffsetLimit offsetLimit, RatingOrder order) {
        Photoquest photoquest = getPhotoQuestByIdOrThrow(photoQuestId);
        photoquest.incrementViewsCount();
        update(request, photoquest);

        Photo photoPattern = new Photo();
        photoPattern.setPhotoquestId(photoQuestId);
        String orderString = getRatingOrderingString(order);
        DBUtilities.QueryParams params = new DBUtilities.QueryParams();
        params.offsetLimit = offsetLimit;
        params.ordering = orderString;
        Collection<Photo> photos = DBUtilities.queryByPattern(persistenceManager, photoPattern, params);
        initPhotosUrl(photos, request);

        initYourLikeParameter(request, photos);

        return photos;

    }

    public long getPhotosOfPhotoquestCount(HttpServletRequest request, long photoQuestId) {
        Photo photoPattern = new Photo();
        photoPattern.setPhotoquestId(photoQuestId);
        return DBUtilities.queryCountByPattern(persistenceManager, photoPattern);
    }

    public String getDefaultAvatar(HttpServletRequest request) {
        return HttpUtilities.getBaseUrl(request) + "/" + DEFAULT_AVATAR_URL;
    }

    public void setAvatar(HttpServletRequest request, WithAvatar withAvatar) {
        Long avatarId = withAvatar.getAvatarId();
        if(avatarId == null){
            withAvatar.setAvatar(getDefaultAvatar(request));
        } else {
            withAvatar.setAvatar(HttpUtilities.getBaseUrl(request) + Photo.IMAGE_URL_PATH + avatarId);
        }
    }

    public void setAvatar(HttpServletRequest request, Iterable<? extends WithAvatar> withAvatars) {
        for(WithAvatar withAvatar : withAvatars){
            setAvatar(request, withAvatar);
        }
    }

    private String getRatingOrderingString(RatingOrder order) {
        String orderString;
        switch (order) {
            case hottest:
                throw new UnsupportedOperationException("hottest is not supported yet");
            case rated:
                orderString = MOST_RATED_PHOTO_MAX_ORDERING;
                break;
            default:
                orderString = "addingDate descending";
                break;
        }

        return orderString;
    }

    public Collection<Photoquest> getPhotoQuests(HttpServletRequest request, OffsetLimit offsetLimit,
                                                 RatingOrder order) {
        String orderString = getRatingOrderingString(order);

        Collection<Photoquest> result =
                DBUtilities.getAllObjectsOfClass(persistenceManager, Photoquest.class,
                        offsetLimit, orderString);
        setAvatar(request, result);
        return result;
    }

    public long getPhotoQuestsCount() {
        return DBUtilities.getAllObjectsOfClassCount(persistenceManager, Photoquest.class);
    }

    public boolean hasFriendship(long user1Id, long user2Id) {
        return getFriendship(user1Id, user2Id) != null;
    }

    public Friendship getFriendship(long user1Id, long user2Id) {
        Friendship friendship = new Friendship();
        friendship.setUser1(user1Id);
        friendship.setUser2(user2Id);
        return DBUtilities.getObjectByPattern(persistenceManager, friendship);
    }

    public Friendship getFriendshipOrThrow(long user1Id, long user2Id) {
        Friendship friendship = getFriendship(user1Id, user2Id);
        if(friendship == null){
            throw new FriendshipNotExistsException(user1Id, user2Id);
        }

        return friendship;
    }

    private void addFriendShip(User user1, User user2) {
        Friendship friendship = new Friendship(user1.getId(), user2.getId());
        user1.incrementFriendsCount();
        user2.incrementFriendsCount();
        makeAllPersistent(friendship, user1, user2);
    }

    public void addFriend(HttpServletRequest request, long userId) {
        try {
            acceptFriendRequest(request, userId);
        } catch (FriendRequestNotFoundException e) {
            sendFriendRequest(request, userId);
        }
    }

    public void removeFriend(HttpServletRequest request, long userId) {
        try {
            declineFriendRequest(request, userId);
        } catch (FriendRequestNotFoundException e) {
            try {
                cancelFriendRequest(request, userId);
            } catch (FriendRequestNotFoundException e1) {
                removeFriendShip(request, userId);
            }
        }
    }

    private void removeFriendShip(HttpServletRequest request, long userId) {
        User user = getSignedInUserOrThrow(request);
        getUserByIdOrThrow(userId);
        long signedInUserId = user.getId();

        Friendship friendship = getFriendshipOrThrow(userId, signedInUserId);
        deletePersistent(friendship);
    }

    public List<Long> getFriendsIdesOf(long userId, OffsetLimit offsetLimit) {
        Friendship friendshipPattern = new Friendship();
        friendshipPattern.setUser1(userId);
        Collection<Friendship> friendships =
                DBUtilities.queryByPattern(persistenceManager, friendshipPattern, offsetLimit);
        ArrayList<Long> result = new ArrayList<Long>(friendships.size());

        for(Friendship friendship : friendships){
            Long user1 = friendship.getUser1();
            Long user2 = friendship.getUser2();

            if(user1 == userId){
                result.add(user2);
            } else if(user2 == userId) {
                result.add(user1);
            } else {
                throw new RuntimeException("WTF? It's impossible!");
            }
        }

        return result;
    }

    public List<User> getUsersByIdes(Iterable<Long> ides) {
        List<User> result = new ArrayList<User>();
        for(Long id : ides){
            User user = getUserByIdOrThrow(id);
            result.add(user);
        }

        return result;
    }

    public List<User> getFriendsOf(long userId, OffsetLimit offsetLimit) {
        return getUsersByIdes(getFriendsIdesOf(userId, offsetLimit));
    }

    public List<User> getFriends(HttpServletRequest request, OffsetLimit offsetLimit) {
        return getFriends(request, offsetLimit, true);
    }

    public long getFriendsCount(HttpServletRequest request) {
        Friendship friendshipPattern = new Friendship();
        friendshipPattern.setUser1(getSignedInUserOrThrow(request).getId());
        return DBUtilities.queryCountByPattern(persistenceManager, friendshipPattern);
    }

    public List<User> getFriends(HttpServletRequest request, OffsetLimit offsetLimit, boolean fillFriendShipData) {
        List<User> friends = getFriendsOf(getSignedInUserOrThrow(request).getId(), offsetLimit);
        if (fillFriendShipData) {
            for(User friend : friends){
                friend.setRelation(RelationStatus.friend);
                setAvatar(request, friend);
            }
        }

        return friends;
    }

    public List<Long> getFriendsIdes(HttpServletRequest request, OffsetLimit offsetLimit) {
        return getFriendsIdesOf(getSignedInUserOrThrow(request).getId(), offsetLimit);
    }

    public Photoquest getOrCreateSystemPhotoQuest(String photoquestName) {
        Photoquest photoquest = getPhotoQuestByName(photoquestName);
        if(photoquest == null){
            return createSystemPhotoquest(photoquestName);
        }

        return photoquest;
    }

    public void beginTransaction() {
        persistenceManager.currentTransaction().begin();
    }

    public void endTransaction() {
        persistenceManager.currentTransaction().commit();
    }

    public Comment getCommentById(long id) {
        return DBUtilities.getObjectById(persistenceManager, Comment.class, id);
    }

    public Comment getCommentByIdOrThrow(long id) {
        Comment comment = getCommentById(id);
        if(comment == null){
            throw new CommentNotFoundException(id);
        }

        return comment;
    }

    public FriendRequest getFriendRequestById(long id) {
        return DBUtilities.getObjectById(persistenceManager, FriendRequest.class, id);
    }

    public FriendRequest getFriendRequestByIdOrThrow(long id) {
        FriendRequest friendRequest = getFriendRequestById(id);
        if(friendRequest == null){
            throw new FriendRequestNotFoundException();
        }

        return friendRequest;
    }

    public Like getLikeById(long id) {
        return DBUtilities.getObjectById(persistenceManager, Like.class, id);
    }

    public Like getLikeByUserAndPhotoId(long userId, long photoId) {
        Like like = new Like();
        like.setUserId(userId);
        like.setPhotoId(photoId);
        return DBUtilities.getObjectByPattern(persistenceManager, like);
    }

    public Like getLikeByUserAndCommentId(long userId, long commentId) {
        Like like = new Like();
        like.setUserId(userId);
        like.setCommentId(commentId);
        return DBUtilities.getObjectByPattern(persistenceManager, like);
    }

    public Like getLikeByIdOrThrow(long id) {
        Like like = getLikeById(id);
        if(like == null){
            throw new LikeNotFoundException(id);
        }

        return like;
    }

    public void fillCommentsData(HttpServletRequest request, Collection<Comment> comments) {
        for (Comment comment : comments) {
            fillCommentData(request, comment);
        }
    }

    public void fillCommentData(HttpServletRequest request, Comment comment) {
        Long toUserId = comment.getToUserId();
        if(toUserId != null){
            User toUser = getUserByIdOrThrow(toUserId);
            setAvatar(request, toUser);
            comment.setToUser(toUser);
        }

        Long userId = comment.getUserId();
        User user = getUserByIdOrThrow(userId);
        setAvatar(request, user);
        comment.setUser(user);
    }

    public Comment addComment(HttpServletRequest request,
                              long photoId, String message) {
        return addComment(request, photoId, message, null);
    }

    public Collection<Like> getCommentLikes(long commentId, OffsetLimit offsetLimit) {
        Like like = new Like();
        like.setCommentId(commentId);
        return DBUtilities.queryByPattern(persistenceManager, like, offsetLimit);
    }

    public Collection<Comment> getCommentsOnComment(long commentId, OffsetLimit offsetLimit) {
        Comment comment = new Comment();
        comment.setToCommentId(commentId);
        return DBUtilities.queryByPattern(persistenceManager, comment, offsetLimit);
    }

    private void addCommentToDeleteStack(List<Object> deleteStack, Comment comment, OffsetLimit offsetLimit) {
        Long commentId = comment.getId();
        Collection<Like> likes = getCommentLikes(commentId, offsetLimit);
        deleteStack.addAll(likes);
        Collection<Comment> comments = getCommentsOnComment(commentId, offsetLimit);
        deleteStack.addAll(comments);

        for(Comment innerComment: comments){
            addCommentToDeleteStack(deleteStack, innerComment, offsetLimit);
        }
    }

    public void deleteComment(HttpServletRequest request, long commentId, OffsetLimit offsetLimit) {
        Comment comment = getCommentByIdOrThrow(commentId);
        List<Object> deleteStack = new ArrayList<Object>();
        addCommentToDeleteStack(deleteStack, comment, offsetLimit);
        deleteStack.add(comment);
        deleteAllPersistent(deleteStack);
    }

    public Comment addComment(HttpServletRequest request,
                           Long photoId, String message, Long toCommentId) {
        User signedInUser = getSignedInUserOrThrow(request);

        Comment comment = new Comment();
        comment.setMessage(message);
        comment.setUserId(signedInUser.getId());
        setAvatar(request, signedInUser);
        comment.setUser(signedInUser);

        if(toCommentId != null){
            Comment toComment = getCommentByIdOrThrow(toCommentId);
            comment.setToCommentId(toCommentId);
            User toUser = getUserByIdOrThrow(toComment.getUserId());
            comment.setToUserId(toUser.getId());
            setAvatar(request, toUser);
            comment.setToUser(toUser);

            photoId = toComment.getPhotoId();
            getPhotoByIdOrThrow(photoId);
        } else {
            getPhotoByIdOrThrow(photoId);
        }

        comment.setPhotoId(photoId);

        return makePersistent(comment);
    }

    private void removeComment(Comment comment) {
        deletePersistent(comment);
    }

    public void removeComment(long commentId) {
        Comment comment = getCommentByIdOrThrow(commentId);
        removeComment(comment);
    }

    public void removeCommentOfSignedInUser(HttpServletRequest request, long commentId) {
        User user = getSignedInUserOrThrow(request);

        Comment comment = getCommentByIdOrThrow(commentId);
        if(!user.getId().equals(comment.getUserId())){
            throw new PermissionDeniedException("Trying to delete comment," +
                    " witch is not created by signed in user");
        }

        removeComment(comment);
    }

    public Collection<Comment> getCommentsOnPhoto(HttpServletRequest request, long photoId, OffsetLimit offsetLimit) {
        Comment commentPattern = new Comment();
        commentPattern.setPhotoId(photoId);

        Collection<Comment> comments =
                queryByAddingDate(commentPattern, offsetLimit);

        User signedInUser = getSignedInUser(request);
        if(signedInUser != null){
            long userId = signedInUser.getId();
            for(Comment comment : comments){
                Like like = getLikeByUserAndCommentId(userId, comment.getId());
                comment.setYourLike(like);
            }
        }

        return comments;
    }

    public Collection<Comment> getCommentsOnPhotoAndFillData(HttpServletRequest request, long photoId,
                                                             OffsetLimit offsetLimit) {
        Collection<Comment> comments = getCommentsOnPhoto(request, photoId, offsetLimit);
        fillCommentsData(request, comments);
        return comments;
    }

    public Like likePhoto(HttpServletRequest request, long photoId) {
        Photo photo = getPhotoByIdOrThrow(photoId);

        Like like = new Like();
        like.setPhotoId(photoId);

        like = like(request, like);

        Photoquest photoquest = getPhotoQuestByIdOrThrow(photo.getPhotoquestId());
        incrementLikesCount(request, photo, photoquest);
        updatePhotoquestAvatar(photoquest);

        return like;
    }

    public Like likeComment(HttpServletRequest request, long commentId) {
        Comment comment = getCommentByIdOrThrow(commentId);

        Like like = new Like();
        like.setCommentId(commentId);

        like = like(request, like);

        incrementLikesCount(request, comment);

        return like;
    }

    private void incrementLikesCount(HttpServletRequest request, Likable... likables) {
        for (Likable likable : likables) {
            likable.incrementLikesCount();
        }

        update(request, likables);
    }

    private void decrementLikesCount(HttpServletRequest request, Likable... likables) {
        for (Likable likable : likables) {
            likable.decrementLikesCount();
        }

        update(request, likables);
    }

    private Like like(HttpServletRequest request, Like like) {
        User signedInUser = getSignedInUserOrThrow(request);
        like.setUserId(signedInUser.getId());

        if(DBUtilities.getObjectByPattern(persistenceManager, like) != null){
            throw new LikeExistsException(like);
        }

        like = makePersistent(like);
        like.setUser(signedInUser);

        return like;
    }

    public void unlike(HttpServletRequest request, long likeId) {
        Like like = getLikeByIdOrThrow(likeId);

        Long photoId = like.getPhotoId();
        Long commentId = like.getCommentId();

        if(photoId != null){
            if(commentId != null){
                throw new RuntimeException("WTF?");
            }

            Photo photo = getPhotoByIdOrThrow(photoId);
            Photoquest photoquest = getPhotoQuestByIdOrThrow(photo.getPhotoquestId());
            decrementLikesCount(request, photo, photoquest);
            updatePhotoquestAvatar(photoquest);

        } else if(commentId != null) {
            Comment comment = getCommentByIdOrThrow(commentId);
            decrementLikesCount(request, comment);
        } else {
            throw new RuntimeException("WTF?");
        }

        deletePersistent(like);
    }

    public Collection<Like> getAllLikes(HttpServletRequest request, OffsetLimit offsetLimit) {
        return DBUtilities.getAllObjectsOfClass(persistenceManager, Like.class, offsetLimit);
    }

    public Collection<Comment> getAllComments(OffsetLimit offsetLimit) {
        return DBUtilities.getAllObjectsOfClass(persistenceManager, Comment.class, offsetLimit);
    }

    private Message addMessage(HttpServletRequest request, User fromUser, User toUser, String messageText) {
        toUser.incrementUnreadMessagesCount();

        long fromUserId = fromUser.getId();
        long toUserId = toUser.getId();

        Message message = new Message();
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setMessage(messageText);

        message = makePersistent(message);
        Dialog dialog = getOrCreateDialog(fromUserId, toUserId);
        dialog.setLastMessageId(message.getId());
        dialog.setLastMessageTime(message.getAddingDate());

        update(request, toUser, dialog);
        return message;
    }

    public Message addMessage(HttpServletRequest request, long toUserId, String messageText) {
        User signedInUser = getSignedInUserOrThrow(request);
        User toUser = getUserByIdOrThrow(toUserId);
        return addMessage(request, signedInUser, toUser, messageText);
    }

    public void deleteMessage(long id) {
        Message message = getMessageByIdOrThrow(id);
        deletePersistent(message);
    }

    public void readMessage(HttpServletRequest request, long messageId) {
        User user = getSignedInUserOrThrow(request);
        long userId = user.getId();
        Message message = getMessageByIdAndUserId(userId, messageId);
        if(userId == message.getFromUserId()){
            throw new MessageReadException("Message was sent by the user, it couldn't be marked as read");
        }

        user.decrementUnreadMessagesCount();
        message.setRead(true);
        update(request, message, user);
    }

    public void markMessageAsDeleted(HttpServletRequest request, long messageId) {
        User user = getSignedInUserOrThrow(request);
        long userId = user.getId();
        Message message = getMessageByIdAndUserId(userId, messageId);

        if(userId == message.getFromUserId()){
            message.setDeletedBySender(true);
        } else {
            message.setDeletedByReceiver(true);
        }
    }

    public Collection<Message> getMessagesByUserId(long userId, OffsetLimit offsetLimit, boolean includeDeleted) {
        Message message = new Message();
        message.setFromUserId(userId);
        return DBUtilities.queryByPattern(persistenceManager, message, offsetLimit);
    }

    private <T> Collection<T> queryByAddingDate(T pattern, OffsetLimit offsetLimit) {
        DBUtilities.QueryParams params = new DBUtilities.QueryParams();
        params.ordering = "addingDate descending";
        params.offsetLimit = offsetLimit;
        return DBUtilities.queryByPattern(persistenceManager, pattern, params);
    }

    public Collection<Message> getDialogMessages(HttpServletRequest request, long widthUserId,
                                                 OffsetLimit offsetLimit) {
        User signedInUser = getSignedInUserOrThrow(request);
        getUserByIdOrThrow(widthUserId);
        Message message = new Message();
        message.setFromUserId(widthUserId);
        message.setToUserId(signedInUser.getId());
        return queryByAddingDate(message, offsetLimit);
    }

    public Message getMessageByIdAndUserId(long userId, long messageId) {
        return getMessageByIdAndUserId(userId, messageId, false);
    }

    public Message getMessageByIdAndUserId(long userId, long messageId, boolean includeDeleted) {
        Message message = getMessageByIdOrThrow(messageId);
        if(message.getFromUserId() != userId && message.getToUserId() != userId){
            throw new MessageNotOwnedByUserException(userId, messageId);
        }

        return message;
    }

    public Collection<Message> getMessagesOfSignedInUser(HttpServletRequest request, OffsetLimit offsetLimit) {
        User signedInUser = getSignedInUserOrThrow(request);
        return getMessagesByUserId(signedInUser.getId(), offsetLimit, false);
    }

    public FriendRequest getFriendRequest(long fromUserId, long toUserId) {
        FriendRequest request = new FriendRequest();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        return DBUtilities.getObjectByPattern(persistenceManager, request);
    }

    public FriendRequest getFriendRequestOrThrow(long fromUserId, long toUserId) {
        FriendRequest friendRequest = getFriendRequest(fromUserId, toUserId);
        if(friendRequest == null){
            throw new FriendRequestNotFoundException();
        }

        return friendRequest;
    }

    public FriendRequest sendFriendRequest(HttpServletRequest request, long userId) {
        User signedInUser = getSignedInUserOrThrow(request);
        User friend = getUserByIdOrThrow(userId);

        long signedInUserId = signedInUser.getId();
        if(hasFriendship(userId, signedInUserId)){
            throw new FriendshipExistsException(userId, signedInUserId);
        }

        if(getFriendRequest(signedInUserId, userId) != null){
            throw new FriendRequestExistsException();
        }

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setToUserId(userId);
        friendRequest.setFromUserId(signedInUserId);

        signedInUser.incrementSentRequestsCount();
        friend.incrementReceivedRequestsCount();

        return (FriendRequest) makeAllPersistent(friendRequest, signedInUser, friend)[0];
    }

    private void deleteFriendRequest(HttpServletRequest request, User fromUser, User toUser) {
        FriendRequest friendRequest = getFriendRequestOrThrow(fromUser.getId(),
                toUser.getId());
        deletePersistent(friendRequest);
        fromUser.decrementSentRequestsCount();
        toUser.decrementReceivedRequestsCount();
        update(request, fromUser, toUser);
    }

    private void cancelFriendRequest(HttpServletRequest request, long userId) {
        User signedInUser = getSignedInUserOrThrow(request);
        User friend = getUserByIdOrThrow(userId);
        deleteFriendRequest(request, signedInUser, friend);
    }

    private void acceptFriendRequest(HttpServletRequest request, long userId) {
        User friend = getUserByIdOrThrow(userId);
        User signedInUser = getSignedInUserOrThrow(request);
        deleteFriendRequest(request, friend, signedInUser);
        addFriendShip(friend, signedInUser);
    }

    private void declineFriendRequest(HttpServletRequest request, long userId) {
        User friend = getUserByIdOrThrow(userId);
        User signedInUser = getSignedInUserOrThrow(request);
        deleteFriendRequest(request, friend, signedInUser);
    }

    private Dialog getOrCreateDialog(long user1Id, long user2Id) {
        Dialog dialog = new Dialog();
        dialog.setUser1(user1Id);
        dialog.setUser2(user2Id);
        Dialog result = DBUtilities.getObjectByPattern(persistenceManager, dialog);
        if(result != null){
            return result;
        }

        return dialog;
    }

    public Collection<Dialog> getDialogs(HttpServletRequest request, OffsetLimit offsetLimit) {
        User signedInUser = getSignedInUserOrThrow(request);
        Dialog dialogPattern = new Dialog();
        long signedInUserId = signedInUser.getId();
        dialogPattern.setUser1(signedInUserId);

        Collection<Dialog> result = DBUtilities.queryByPattern(persistenceManager, dialogPattern, offsetLimit);
        for(Dialog dialog : result){
            Message lastMessage = getMessageByIdOrThrow(dialog.getLastMessageId());

            long userId = dialog.getUser1();
            if(userId == signedInUserId){
                userId = dialog.getUser2();
            } else if(signedInUserId != dialog.getUser2()) {
                throw new RuntimeException("Broken database, dialog, associated with the signed in user " +
                        "doesn't have lastMassage, associated with him");
            }

            User user = getUserByIdOrThrow(userId);
            setAvatar(request, user);
            dialog.setLastMessage(lastMessage);
            dialog.setUser(user);
        }

        return result;
    }

    public List<User> registerRandomUsers(HttpServletRequest request, int startId, int count, String password)
            throws IOException {
        List<Response> data = RandomUserGenerator.generate(count);
        List<User> result = new ArrayList<User>(count);

        for(Response userData : data){
            User user = new User();
            user.setName(userData.name);
            user.setLastName(userData.lastName);
            user.setLogin("user" + startId++);
            user.setPassword(password);

            byte[] avatar = Network.getBytesFromUrl(userData.largeAvatar);
            user = registerUser(request, user, avatar);
            result.add(user);
        }

        return result;
    }

    public Photo getMostRatedPhotoOfPhotoquest(long photoQuestId) {
        Photo pattern = new Photo();
        pattern.setPhotoquestId(photoQuestId);
        return DBUtilities.getMaxByPattern(persistenceManager, pattern, MOST_RATED_PHOTO_MAX_ORDERING);
    }

    public void updatePhotoquestAvatar(Photoquest photoquest) {
        Long avatarId = photoquest.getAvatarId();
        Photo photo = getMostRatedPhotoOfPhotoquest(photoquest.getId());
        Long photoId = photo.getId();
        if(!photoId.equals(avatarId)){
            photoquest.setAvatarId(photoId);
            makePersistent(photoquest);
        }
    }

    public long getPhotoInPhotoquestPosition(long photoId) {
        Photo photo = getPhotoByIdOrThrow(photoId);
        return getPhotoInPhotoquestPosition(photo);
    }

    private long getPhotoInPhotoquestPosition(Photo photo) {
        Photo pattern = new Photo();
        pattern.setPhotoquestId(photo.getPhotoquestId());
        return DBUtilities.getPosition(persistenceManager, photo, MOST_RATED_PHOTO_MAX_ORDERING, pattern);
    }

    public Photo getPhotoAndFillInfo(HttpServletRequest request, long photoId) {
        Photo photo = getPhotoByIdOrThrow(photoId);
        initYourLikeParameter(request, photo);
        photo.setPosition(getPhotoInPhotoquestPosition(photo));
        return photo;
    }

    public List<AutoCompleteResult> getLocationSuggestions(String query) throws IOException {
        return googlePlacesSearcher.performAutoCompleteCitiesSearch(query);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        persistenceManager.close();
    }
}
