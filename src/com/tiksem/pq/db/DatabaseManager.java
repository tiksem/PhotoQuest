package com.tiksem.pq.db;

import com.tiksem.pq.data.*;
import com.tiksem.pq.db.exceptions.*;
import com.tiksem.pq.http.HttpUtilities;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Created by CM on 10/30/2014.
 */
public class DatabaseManager {
    private static DatabaseManager instance;
    private static final String DEFAULT_AVATAR_URL = "/images/empty_avatar.png";
    public static final String AVATAR_QUEST_NAME = "Avatar";

    private final PersistenceManager persistenceManager;

    public static DatabaseManager getInstance() {
        if(instance == null ){
            instance = new DatabaseManager();
        }

        return instance;
    }

    private DatabaseManager()     {
        PersistenceManagerFactory factory  = ObjectDBUtilities.createLocalConnectionFactory("PhotoQuest");
        persistenceManager = factory.getPersistenceManager();
    }

    public User getUserById(long id) {
        return ObjectDBUtilities.getObjectById(persistenceManager, User.class, id);
    }

    public User getUserByIdOrThrow(long id) {
        User user = getUserById(id);
        if(user == null){
            throw new UnknownUserException(String.valueOf(id));
        }

        return user;
    }

    public User getUserByLogin(String login) {
        User user = new User();
        user.setLogin(login);
        return ObjectDBUtilities.getObjectByPattern(persistenceManager, user);
    }

    public User getUserByLoginOrThrow(String login) {
        User user = getUserByLogin(login);
        if(user == null){
            throw new UnknownUserException(login);
        }

        return user;
    }

    public User getUserByLoginAndPassword(String login, String password) {
        User user = getUserByLogin(login);
        if(user != null && user.getPassword().equals(password)){
            return user;
        }

        return null;
    }

    public User login(HttpServletRequest request, String login, String password) {
        User user = getUserByLogin(login);
        if(user != null && user.getPassword().equals(password)){
            setAvatar(request, user);
            return user;
        }

        return null;
    }

    public User loginOrThrow(HttpServletRequest request, String login, String password) {
        User user = login(request, login, password);
        if(user == null){
            throw new LoginFailedException();
        }

        return user;
    }

    public User getSignedInUser(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        Cookie loginCookie = HttpUtilities.getCookie("login", cookies);
        if(loginCookie == null){
            return null;
        }

        Cookie passwordCookie = HttpUtilities.getCookie("password", cookies);
        if(passwordCookie == null){
            return null;
        }

        User user = getUserByLoginAndPassword(loginCookie.getValue(),
                passwordCookie.getValue());

        return user;
    }

    public User getSignedInUserOrThrow(HttpServletRequest request){
        User user = getSignedInUser(request);
        if(user == null){
            throw new UserIsNotSignInException();
        }

        return user;
    }

    public Photoquest getPhotoQuestByName(String photoquestName) {
        Photoquest photoquest = new Photoquest();
        photoquest.setName(photoquestName);
        return ObjectDBUtilities.getObjectByPattern(persistenceManager, photoquest);
    }

    public Photoquest getPhotoQuestById(long id) {
        Photoquest photoquest = new Photoquest();
        photoquest.setId(id);
        return ObjectDBUtilities.getObjectByPattern(persistenceManager, photoquest);
    }

    public Photoquest getPhotoQuestByIdOrThrow(long id) {
        Photoquest photoquest = getPhotoQuestById(id);
        if(photoquest == null){
            throw new PhotoquestNotFoundException(String.valueOf(id));
        }

        return photoquest;
    }

    public Photoquest createSystemPhotoquest(String photoquestName) {
        Photoquest photoquest = Photoquest.withZeroViewsAndLikes(photoquestName);

        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        photoquest = persistenceManager.makePersistent(photoquest);
        transaction.commit();

        return photoquest;
    }

    public Photoquest createPhotoQuest(HttpServletRequest request, String photoquestName) {
        User user = getSignedInUserOrThrow(request);
        Photoquest photoquest = getPhotoQuestByName(photoquestName);
        if(photoquest != null){
            throw new PhotoquestExistsException(photoquestName);
        }

        photoquest = Photoquest.withZeroViewsAndLikes(photoquestName);
        photoquest.setUserId(user.getId());

        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        photoquest = persistenceManager.makePersistent(photoquest);
        transaction.commit();

        return photoquest;
    }

    public List<Photoquest> getPhotoquestsCreatedByUser(long userId) {
        Photoquest photoquest = new Photoquest();
        photoquest.setUserId(userId);
        Collection<Photoquest> photoquests = ObjectDBUtilities.queryByPattern(persistenceManager, photoquest);
        return new ArrayList<Photoquest>(photoquests);
    }

    public  List<Photoquest> getPhotoquestsCreatedByUser(String login) {
        User user = getUserByLoginOrThrow(login);
        return getPhotoquestsCreatedByUser(user.getId());
    }

    public void update(HttpServletRequest request, Object object) {
        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        persistenceManager.makePersistent(object);
        transaction.commit();

        if(object instanceof WithAvatar){
            WithAvatar withAvatar = (WithAvatar) object;
            setAvatar(request, withAvatar);
        }
    }

    public User registerUser(User user) {
        String login = user.getLogin();
        String password = user.getPassword();

        if(getUserByLogin(login) != null){
            throw new UserExistsRegisterException(login);
        }

        FieldsCheckingUtilities.checkLoginAndPassword(login, password);
        FieldsCheckingUtilities.fixFields(user);

        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        user = persistenceManager.makePersistent(user);
        transaction.commit();

        return user;
    }

    public Collection<User> getAllUsers(HttpServletRequest request) {
        return getAllUsers(request, true, false);
    }

    public Collection<User> getAllUsersWithCheckingRelationShip(HttpServletRequest request) {
        return getAllUsers(request, false, true);
    }

    public Collection<User> getAllUsers(HttpServletRequest request, boolean includeSignedInUser,
                                        boolean fillRelationshipData) {
        Collection<User> users = null;
        User signedInUser = null;

        if (includeSignedInUser) {
            users = ObjectDBUtilities.getAllObjectsOfClass(persistenceManager,
                    User.class);
        } else {
            signedInUser = getSignedInUser(request);
            if(signedInUser == null){
                users = ObjectDBUtilities.getAllObjectsOfClass(persistenceManager,
                        User.class);
            } else {
                User user = new User();
                user.setId(signedInUser.getId());
                users = ObjectDBUtilities.queryByExcludePattern(persistenceManager, user);
            }
        }

        setAvatar(request, users);

        if(fillRelationshipData && signedInUser != null){
            for(User user : users){
                boolean areFriends = hasFriendship(signedInUser.getId(), user.getId());
                user.setIsFriend(areFriends);
            }
        }

        return users;
    }

    public void deleteAllUsers(HttpServletRequest request) {
        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        persistenceManager.deletePersistentAll(getAllUsers(request));
        transaction.commit();
    }

    public void deleteAllPhotoquests(HttpServletRequest request) {
        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        persistenceManager.deletePersistentAll(getPhotoQuests(request));
        transaction.commit();
    }

    public void deleteAllPhotos() {
        ObjectDBUtilities.deleteAllObjectsOfClass(persistenceManager, Photo.class);
        ObjectDBUtilities.deleteAllObjectsOfClass(persistenceManager, BitmapData.class);
    }

    public Photo addPhoto(HttpServletRequest request, Photo photo, byte[] bitmapData) {
        Long userId = photo.getUserId();
        if(userId == null){
            User user = getSignedInUserOrThrow(request);
            userId = user.getId();
        }

        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        photo.setLikesCount(0l);
        photo.setUserId(userId);
        photo = persistenceManager.makePersistent(photo);
        transaction.commit();
        transaction = persistenceManager.currentTransaction();
        transaction.begin();
        BitmapData data = new BitmapData();
        data.setId(photo.getId());
        data.setImage(bitmapData);
        persistenceManager.makePersistent(data);
        transaction.commit();
        return photo;
    }

    public Photo getPhotoById(long id) {
        return ObjectDBUtilities.getObjectById(persistenceManager, Photo.class, id);
    }

    public Photo getPhotoByIdOrThrow(long id) {
        Photo photo = getPhotoById(id);
        if(photo == null){
            throw new PhotoNotFoundException(id);
        }

        return photo;
    }

    public byte[] getBitmapDataByPhotoId(long id) {
        BitmapData bitmapData =
                ObjectDBUtilities.getObjectById(persistenceManager, BitmapData.class, id);
        if(bitmapData == null){
            return null;
        }

        return bitmapData.getImage();
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

    public Collection<Photo> getPhotosOfPhotoquest(HttpServletRequest request, long photoQuestId) {
        Photo photo = new Photo();
        photo.setPhotoquestId(photoQuestId);
        Collection<Photo> photos = ObjectDBUtilities.queryByPattern(persistenceManager, photo);
        initPhotosUrl(photos, request);
        return photos;
    }

    public String getDefaultAvatar(HttpServletRequest request) {
        return HttpUtilities.getBaseUrl(request) + "/" + DEFAULT_AVATAR_URL;
    }

    private void setAvatar(HttpServletRequest request, WithAvatar withAvatar) {
        Long avatarId = withAvatar.getAvatarId();
        if(avatarId == null){
            withAvatar.setAvatar(getDefaultAvatar(request));
        } else {
            withAvatar.setAvatar(HttpUtilities.getBaseUrl(request) + Photo.IMAGE_URL_PATH + avatarId);
        }
    }

    private void setAvatar(HttpServletRequest request, Iterable<? extends WithAvatar> withAvatars) {
        for(WithAvatar withAvatar : withAvatars){
            setAvatar(request, withAvatar);
        }
    }

    public Collection<Photoquest> getPhotoQuests(HttpServletRequest request) {
        Collection<Photoquest> result =
                ObjectDBUtilities.getAllObjectsOfClass(persistenceManager, Photoquest.class);
        setAvatar(request, result);
        return result;
    }

    public boolean hasFriendship(long user1Id, long user2Id) {
        return getFriendship(user1Id, user2Id) != null;
    }

    public Friendship getFriendship(long user1Id, long user2Id) {
        Query query = persistenceManager.newQuery(Friendship.class);
        query.declareParameters("long user1, long user2");

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("user1", user1Id);
        args.put("user2", user2Id);

        String filter = "(this.user1 == user1 && this.user2 == user2) || " +
                "(this.user1 == user2 && this.user2 == user1)";
        query.setFilter(filter);

        Collection<Friendship> result = (Collection<Friendship>) query.executeWithMap(args);
        if(result.isEmpty()){
            return null;
        }

        return result.iterator().next();
    }

    public Friendship getFriendshipOrThrow(long user1Id, long user2Id) {
        Friendship friendship = getFriendship(user1Id, user2Id);
        if(friendship == null){
            throw new FriendshipNotExistsException(user1Id, user2Id);
        }

        return friendship;
    }

    public void addFriend(HttpServletRequest request, long userId) {
        User user = getSignedInUserOrThrow(request);
        getUserByIdOrThrow(userId);
        long signedInUserId = user.getId();

        if(hasFriendship(userId, signedInUserId)){
            throw new FriendshipExistsException(userId, signedInUserId);
        }

        Friendship friendship = new Friendship(userId, signedInUserId);
        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        persistenceManager.makePersistent(friendship);
        transaction.commit();
    }

    public void removeFriend(HttpServletRequest request, long userId) {
        User user = getSignedInUserOrThrow(request);
        getUserByIdOrThrow(userId);
        long signedInUserId = user.getId();

        Friendship friendship = getFriendshipOrThrow(userId, signedInUserId);
        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        persistenceManager.deletePersistent(friendship);
        transaction.commit();
    }

    public List<Long> getFriendsIdesOf(long userId) {
        Query query = persistenceManager.newQuery(Friendship.class);
        query.declareParameters("long userId");

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("userId", userId);

        String filter = "this.user1 == userId || this.user2 == userId";
        query.setFilter(filter);

        Collection<Friendship> friendships = (Collection<Friendship>) query.executeWithMap(args);
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

    public List<User> getFriendsOf(long userId) {
        return getUsersByIdes(getFriendsIdesOf(userId));
    }

    public List<User> getFriends(HttpServletRequest request) {
        return getFriends(request, true);
    }

    public List<User> getFriends(HttpServletRequest request, boolean fillFriendShipData) {
        List<User> friends = getFriendsOf(getSignedInUserOrThrow(request).getId());
        if (fillFriendShipData) {
            for(User friend : friends){
                friend.setIsFriend(true);
            }
        }

        return friends;
    }

    public List<Long> getFriendsIdes(HttpServletRequest request) {
        return getFriendsIdesOf(getSignedInUserOrThrow(request).getId());
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
        return ObjectDBUtilities.getObjectById(persistenceManager, Comment.class, id);
    }

    public Comment getCommentByIdOrThrow(long id) {
        Comment comment = getCommentById(id);
        if(comment == null){
            throw new CommentNotFoundException(id);
        }

        return comment;
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

    public Comment addComment(HttpServletRequest request,
                           long photoId, String message, Long toCommentId) {
        getPhotoByIdOrThrow(photoId);
        User signedInUser = getSignedInUserOrThrow(request);

        Comment comment = new Comment();
        comment.setMessage(message);
        comment.setPhotoId(photoId);
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
        }

        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        persistenceManager.makePersistent(comment);
        transaction.commit();

        return comment;
    }

    private void removeComment(Comment comment) {
        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        persistenceManager.deletePersistent(comment);
        transaction.commit();
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

    public Collection<Comment> getCommentsOnPhoto(long photoId) {
        Comment comment = new Comment();
        comment.setPhotoId(photoId);
        return ObjectDBUtilities.queryByPattern(persistenceManager, comment);
    }

    public Collection<Comment> getCommentsOnPhotoAndFillData(HttpServletRequest request, long photoId) {
        Collection<Comment> comments = getCommentsOnPhoto(photoId);
        fillCommentsData(request, comments);
        return comments;
    }
}
