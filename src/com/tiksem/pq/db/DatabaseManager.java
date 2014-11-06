package com.tiksem.pq.db;

import com.tiksem.pq.PhotosByPhotoQuest;
import com.tiksem.pq.data.Photo;
import com.tiksem.pq.data.Photoquest;
import com.tiksem.pq.data.User;
import com.tiksem.pq.db.exceptions.*;
import com.tiksem.pq.http.HttpUtilities;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by CM on 10/30/2014.
 */
public class DatabaseManager {
    private static DatabaseManager instance;
    private static final String DEFAULT_AVATAR_URL = "/images/empty_avatar.png";

    private final PersistenceManager persistenceManager;

    public static DatabaseManager getInstance() {
        if(instance == null){
            instance = new DatabaseManager();
        }

        return instance;
    }

    private DatabaseManager()     {
        PersistenceManagerFactory factory  = ObjectDBUtilities.createLocalConnectionFactory("PhotoQuest");
        persistenceManager = factory.getPersistenceManager();
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

    public User login(String login, String password) {
        User user = getUserByLogin(login);
        if(user != null && user.getPassword().equals(password)){
            return user;
        }

        return null;
    }

    public User loginOrThrow(String login, String password) {
        User user = login(login, password);
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

    public Collection<User> getAllUsers() {
        return ObjectDBUtilities.getAllObjectsOfClass(persistenceManager, User.class);
    }

    public void deleteAllUsers() {
        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        persistenceManager.deletePersistentAll(getAllUsers());
        transaction.commit();
    }

    public Photo addPhoto(Photo photo) {
        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        photo = persistenceManager.makePersistent(photo);
        transaction.commit();
        return photo;
    }

    public PhotosByPhotoQuest addPhotoToPhotoquest(long photoId, long photoquestId) {
        PhotosByPhotoQuest photosByPhotoQuest = new PhotosByPhotoQuest();
        photosByPhotoQuest.setPhotoId(photoId);
        photosByPhotoQuest.setPhotoQuestId(photoquestId);

        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();
        photosByPhotoQuest = persistenceManager.makePersistent(photosByPhotoQuest);
        transaction.commit();

        return photosByPhotoQuest;
    }

    public Photo getPhotoById(long id) {
        return ObjectDBUtilities.getObjectById(persistenceManager, Photo.class, id);
    }

    public String getDefaultAvatar(HttpServletRequest request) {
        return HttpUtilities.getBaseUrl(request) + "/" + DEFAULT_AVATAR_URL;
    }

    private void setDefaultAvatarIfNeed(HttpServletRequest request, Photoquest photoquest) {
        if(photoquest.getAvatar() == null){
            photoquest.setAvatar(getDefaultAvatar(request));
        }
    }

    private void setDefaultAvatarIfNeed(HttpServletRequest request, Iterable<Photoquest> photoquests) {
        for(Photoquest photoquest : photoquests){
            setDefaultAvatarIfNeed(request, photoquest);
        }
    }

    public Collection<Photoquest> getPhotoQuests(HttpServletRequest request) {
        Collection<Photoquest> result =
                ObjectDBUtilities.getAllObjectsOfClass(persistenceManager, Photoquest.class);
        setDefaultAvatarIfNeed(request, result);
        return result;
    }
}
