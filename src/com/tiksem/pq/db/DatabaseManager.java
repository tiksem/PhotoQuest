package com.tiksem.pq.db;

import com.tiksem.pq.data.*;
import com.tiksem.pq.data.Likable;
import com.tiksem.pq.data.response.Feed;
import com.tiksem.pq.data.response.ReplyResponse;
import com.tiksem.pq.data.response.UserStats;
import com.tiksem.pq.db.exceptions.*;
import com.tiksem.pq.http.HttpUtilities;
import com.utils.framework.CollectionUtils;
import com.utils.framework.google.places.*;
import com.utils.framework.io.IOUtilities;
import com.utils.framework.randomuser.Gender;
import com.utils.framework.randomuser.RandomUserGenerator;
import com.utils.framework.randomuser.Response;
import com.utils.framework.strings.Strings;
import org.springframework.web.multipart.MultipartFile;

import javax.jdo.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by CM on 10/30/2014.
 */
public class DatabaseManager {
    private static final PersistenceManagerFactory factory;

    private static final String DEFAULT_AVATAR_URL = "/images/empty_avatar.png";
    public static final String AVATAR_QUEST_NAME = "Avatar";

    private static final String MOST_RATED_PHOTO_MAX_ORDERING = "likesCount descending, viewsCount descending, " +
            "addingDate descending";

    private static final String NEWEST_PHOTO_MAX_ORDERING = "addingDate descending, likesCount descending, " +
            "viewsCount descending";

    private static final String HOTTEST_PHOTO_MAX_ORDERING = "viewsCount descending, likesCount descending, " +
            "addingDate descending";

    private static final String RATING_PEOPLE_ORDERING = "rating descending";


    private static final String ADDING_DATE_ORDERING = "addingDate descending";

    private static final String GOOGLE_API_KEY = "AIzaSyAfhfIJpCrb29TbTafQ1UWSqqaSaOuVCIg";

    private static final int MAX_KEYWORDS_COUNT = 7;

    private static final long PHOTOQUEST_VIEW_PERIOD = 30 * 60 * 1000;
    private static final long PROFILE_VIEW_PERIOD = 30 * 60 * 1000;
    private static final long PHOTO_VIEW_PERIOD = 30 * 60 * 1000;

    private final PersistenceManager persistenceManager;

    private ImageManager imageManager = new FileSystemImageManager("images", "magic");
    private GooglePlacesSearcher googlePlacesSearcher = new GooglePlacesSearcher(GOOGLE_API_KEY);
    private AdvancedRequestsManager advancedRequestsManager;

    static {
        factory = DBUtilities.createMySQLConnectionFactory("PhotoQuest");
    }

    public DatabaseManager() {
        persistenceManager = factory.getPersistenceManager();
        advancedRequestsManager = new AdvancedRequestsManager(persistenceManager);
    }

    private <T> T makePersistent(T object) {
        return DBUtilities.makePersistent(persistenceManager, object);
    }

    private <T> T[] makeAllPersistent(T... objects) {
        return DBUtilities.makeAllPersistent(persistenceManager, objects);
    }

    private <T> T[] makeAllPersistent(Collection<T> objects) {
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

    public User requestUserProfileData(HttpServletRequest request, long id) {
        User user = getUserByIdOrThrow(id);

        User signedInUser = getSignedInUser(request);
        if(signedInUser != null && signedInUser.getId() != id){
            ProfileView pattern = new ProfileView();
            pattern.setVisitorId(signedInUser.getId());
            pattern.setUserId(id);
            ProfileView profileView = DBUtilities.getObjectByPattern(persistenceManager, pattern);
            if(profileView == null){
                profileView = pattern;
            }

            long currentTimeMillis = System.currentTimeMillis();
            if(profileView == pattern || currentTimeMillis -
                    profileView.getAddingDate() >= PROFILE_VIEW_PERIOD){
                profileView.setAddingDate(currentTimeMillis);
                user.incrementRating();
                makeAllPersistent(user, profileView);
            }
        }

        setUsersInfoAndRelationStatus(request, Collections.singletonList(user));

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
        photoquest = makePersistent(photoquest);
        setPhotoquestKeywords(photoquest, Collections.<String>emptyList());
        return photoquest;
    }

    private void setPhotoquestKeywords(long photoquestId, String keywords) {
        advancedRequestsManager.insertPhotoquestSearch(photoquestId, keywords);
    }

    private void setPhotoquestKeywords(Photoquest photoquest, List<String> keywords) {
        String keywordConcat = photoquest.getName() + " " + Strings.join(" ", keywords);
        setPhotoquestKeywords(photoquest.getId(), keywordConcat);
    }

    public Photoquest createPhotoQuest(HttpServletRequest request, String photoquestName, List<String> keywords) {
        if(keywords.size() > MAX_KEYWORDS_COUNT){
            throw new IllegalArgumentException("Too much tags, only " + MAX_KEYWORDS_COUNT + " are allowed");
        }

        User user = getSignedInUserOrThrow(request);
        Photoquest photoquest = getPhotoQuestByName(photoquestName);
        if (photoquest != null) {
            throw new PhotoquestExistsException(photoquestName);
        }

        photoquest = Photoquest.withZeroViewsAndLikes(photoquestName);
        Long userId = user.getId();
        photoquest.setUserId(userId);

        photoquest = makePersistent(photoquest);
        Action action = new Action();
        action.setPhotoquestId(photoquest.getId());
        action.setUserId(userId);
        makePersistent(action);
        setPhotoquestKeywords(photoquest, keywords);

        return photoquest;
    }

    public Collection<Photoquest> getPhotoquestsCreatedByUser(
            HttpServletRequest request,
            long userId, OffsetLimit offsetLimit, RatingOrder order) {
        Photoquest pattern = new Photoquest();
        pattern.setUserId(userId);
        Collection<Photoquest> photoquests =
                DBUtilities.queryByPattern(persistenceManager, pattern, offsetLimit, getPhotoRatingOrderingString(order));
        setPhotoquestsFollowingParamIfSignedIn(request, photoquests);
        setAvatar(request, photoquests);
        return photoquests;
    }

    public long getPhotoquestsCreatedByUserCount(long userId) {
        Photoquest photoquest = new Photoquest();
        photoquest.setUserId(userId);
        return DBUtilities.queryCountByPattern(persistenceManager, photoquest);
    }

    public void initPhotoquestsInfo(HttpServletRequest request, Collection<Photoquest> photoquests) {
        setAvatar(request, photoquests);
        setPhotoquestsFollowingParamIfSignedIn(request, photoquests);
    }

    public Collection<Photoquest> searchPhotoquests(final HttpServletRequest request, String query, OffsetLimit offsetLimit) {
        Photoquest photoquestByName = getPhotoQuestByName(query);
        Long excludeId = null;
        Collection<Photoquest> result = new ArrayList<Photoquest>();
        if(photoquestByName != null){
            excludeId = photoquestByName.getId();
            if(offsetLimit.getOffset() == 0){
                offsetLimit.setLimit(offsetLimit.getLimit() - 1);
            }
            result.add(photoquestByName);
        }

        List<Long> ides = advancedRequestsManager.getPhotoquestsByQuery(query, excludeId, offsetLimit);
        result.addAll(CollectionUtils.transform(ides,
                new CollectionUtils.Transformer<Long, Photoquest>() {
                    @Override
                    public Photoquest get(Long id) {
                        return getPhotoQuestByIdOrThrow(id);
                    }
                }));

        initPhotoquestsInfo(request, result);
        return result;
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
        return getPhotoquestsCreatedByUser(request, getSignedInUserOrThrow(request).getId(), offsetLimit, order);
    }

    public long getPhotoquestsCreatedBySignedInUserCount(HttpServletRequest request) {
        return getPhotoquestsCreatedByUserCount(getSignedInUserOrThrow(request).getId());
    }

    public void update(HttpServletRequest request, Object... objects) {
        update(request, Arrays.asList(objects));
    }

    public void update(HttpServletRequest request, Collection<Object> objects) {
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
        location.setCountryCode(city.countryCode);
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
        InputStream avatarInputStream = null
                ;
        try {
            avatarInputStream = avatar.getInputStream();
        } catch (IOException e) {

        }

        return registerUser(request, user, avatarInputStream);
    }

    public User registerUser(HttpServletRequest request, User user, InputStream avatar) throws IOException {
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
            updatePhotoquestAvatar(avatarPhotoQuest);

            if (user.getLogin() == null) {
                throw new RuntimeException("WTF?");
            }

            update(request, user);
        }

        return user;
    }

    public Collection<User> getAllUsers(HttpServletRequest request, OffsetLimit offsetLimit, RatingOrder order) {
        return getAllUsers(request, true, false, offsetLimit, order);
    }

    public Collection<User> getAllUsersWithCheckingRelationShip(HttpServletRequest request,
                                                                OffsetLimit offsetLimit,
                                                                RatingOrder order) {
        return getAllUsers(request, false, true, offsetLimit, order);
    }

    public long getAllUsersCount(HttpServletRequest request, boolean includeSignedInUser) {
        long result = DBUtilities.getAllObjectsOfClassCount(persistenceManager,
                User.class);
        if(includeSignedInUser && getSignedInUser(request) != null){
            result--;
        }

        return result;
    }

    private void setRelationStatus(User signedInUser, Collection<User> users) {
        Long signedInUserId = signedInUser.getId();

        for(User user : users){
            Long userId = user.getId();
            Relationship pattern = new Relationship();
            pattern.setFromUserId(signedInUserId);
            pattern.setToUserId(userId);

            Relationship relationship = DBUtilities.getObjectByPattern(persistenceManager, pattern);
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

                relationship = DBUtilities.getObjectByPattern(persistenceManager, pattern);
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

    public Collection<User> getAllUsers(HttpServletRequest request, boolean includeSignedInUser,
                                        boolean fillRelationshipData, OffsetLimit offsetLimit,
                                        RatingOrder order) {
        Collection<User> users = null;
        User signedInUser = null;

        long signedInUserId = 0;
        String orderingString = getPeopleOrderingString(order);

        if (includeSignedInUser) {
            users = DBUtilities.getAllObjectsOfClass(persistenceManager,
                    User.class, offsetLimit, orderingString);
        } else {
            signedInUser = getSignedInUser(request);
            if(signedInUser == null){
                users = DBUtilities.getAllObjectsOfClass(persistenceManager,
                        User.class, offsetLimit, orderingString);
            } else {
                signedInUserId = signedInUser.getId();
                User user = new User();
                user.setId(signedInUserId);
                users = DBUtilities.queryByExcludePattern(persistenceManager, user, offsetLimit, orderingString);
            }
        }

        setUsersInfo(request, users);

        if(fillRelationshipData && signedInUser != null){
            setRelationStatus(signedInUser, users);
        }

        return users;
    }

    public Collection<User> getUsersByLocation(HttpServletRequest request, String location,
                                               OffsetLimit offsetLimit,
                                               RatingOrder order) {
        User pattern = new User();
        pattern.setLocation(location);
        String orderingString = getPeopleOrderingString(order);
        Collection<User> users = DBUtilities.queryByPattern(persistenceManager, pattern, offsetLimit, orderingString);
        setUsersInfoAndRelationStatus(request, users);
        return users;
    }

    public void setUsersInfoAndRelationStatus(HttpServletRequest request, Collection<User> users) {
        User signedInUser = getSignedInUser(request);
        setUsersInfo(request, users);
        if (signedInUser != null) {
            setRelationStatus(signedInUser, users);
        }
    }

    public long getUsersByLocationCount(String location) {
        User pattern = new User();
        pattern.setLocation(location);
        return DBUtilities.queryCountByPattern(persistenceManager, pattern);
    }

    private Query searchUsersQuery(String queryString, String location,
                                   Map<String, Object> outArgs,
                                   RatingOrder order) {
        String[] queryParts = queryString.split(" +");
        for (int i = 0; i < queryParts.length; i++) {
            queryParts[i] = Strings.capitalizeAndCopy(queryParts[i].toLowerCase());
        }

        Query query = persistenceManager.newQuery(User.class);
        String filter;
        String parametersString;

        if(queryParts.length == 1){
            filter = "this.name == query || this.lastName == query";
            outArgs.put("query", queryParts[0]);
            parametersString = "String query";
        } else if(queryParts.length >= 2) {
            filter = "(this.name == query1 && this.lastName == query2) || " +
                    "(this.name == query2 && this.lastName == query1)";
            outArgs.put("query1", queryParts[0]);
            outArgs.put("query2", queryParts[1]);
            parametersString = "String query1, String query2";
        } else {
            throw new IllegalArgumentException("empty query");
        }

        if(!Strings.isEmpty(location)){
            outArgs.put("location", location);
            parametersString += ", String location";
            filter = "(" + filter + ") && (this.location == location)";
        }

        query.setFilter(filter);
        query.declareParameters(parametersString);
        if (order != null) {
            query.setOrdering(getPeopleOrderingString(order));
        }

        return query;
    }

    public Collection<User> searchUsers(HttpServletRequest request, String queryString, String location,
                                        OffsetLimit offsetLimit, RatingOrder order) {
        Map<String, Object> args = new HashMap<String, Object>();
        Query query = searchUsersQuery(queryString, location, args, order);
        offsetLimit.applyToQuery(query);

        Collection<User> users = (Collection<User>) query.executeWithMap(args);
        setUsersInfoAndRelationStatus(request, users);

        return users;
    }

    public long getSearchUsersCount(String queryString, String location) {
        Map<String, Object> args = new HashMap<String, Object>();
        Query query = searchUsersQuery(queryString, location, args, null);
        query.setResult("count(this)");
        return (Long)query.executeWithMap(args);
    }

    public void deleteAllPhotos() {
        DBUtilities.deleteAllObjectsOfClass(persistenceManager, Photo.class);
        new File("images").delete();
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

    private Photo addPhoto(HttpServletRequest request, Photo photo, InputStream bitmapData) {
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

        commitAddPhotoAction(photo);

        return photo;
    }

    private void commitAddPhotoAction(Photo photo) {
        Action action = new Action();
        action.setPhotoquestId(photo.getPhotoquestId());
        action.setPhotoId(photo.getId());
        action.setUserId(photo.getUserId());
        makePersistent(action);
    }

    public Photo addPhotoToPhotoquest(HttpServletRequest request,
                                      long photoquestId, MultipartFile file) throws IOException {
        Photoquest photoquest = getPhotoQuestByIdOrThrow(photoquestId);

        if (!file.isEmpty()) {
            Photo photo = new Photo();
            photo.setPhotoquestId(photoquestId);
            InputStream inputStream = file.getInputStream();
            photo = addPhoto(request, photo, inputStream);
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

    private void addPhotoquestViewIfNeed(HttpServletRequest request, Photoquest photoquest) {
        User signedInUser = getSignedInUser(request);
        if(signedInUser == null){
            return;
        }

        PhotoquestView pattern = new PhotoquestView();
        pattern.setPhotoquestId(photoquest.getId());
        pattern.setUserId(signedInUser.getId());
        PhotoquestView photoquestView = DBUtilities.getObjectByPattern(persistenceManager, pattern);
        if(photoquestView == null){
            photoquestView = pattern;
        }

        long currentTimeMillis = System.currentTimeMillis();
        if(photoquestView == pattern || currentTimeMillis -
                photoquestView.getAddingDate() >= PHOTOQUEST_VIEW_PERIOD){
            photoquestView.setAddingDate(currentTimeMillis);
            photoquest.incrementViewsCount();
            makeAllPersistent(photoquest, photoquestView);
        }
    }

    public Collection<Photo> getPhotosOfPhotoquest(HttpServletRequest request, long photoQuestId,
                                                   OffsetLimit offsetLimit, RatingOrder order) {
        Photoquest photoquest = getPhotoQuestByIdOrThrow(photoQuestId);
        addPhotoquestViewIfNeed(request, photoquest);

        Photo photoPattern = new Photo();
        photoPattern.setPhotoquestId(photoQuestId);
        String orderString = getPhotoRatingOrderingString(order);

        DBUtilities.QueryParams params = new DBUtilities.QueryParams();
        params.offsetLimit = offsetLimit;
        params.ordering = orderString;
        Collection<Photo> photos = DBUtilities.queryByPattern(persistenceManager, photoPattern, params);
        initPhotosUrl(photos, request);

        initYourLikeParameter(request, photos);
        addPhotoquestViewIfNeed(request, photoquest);

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

    public void setUserInfo(HttpServletRequest request, User user) {
        Location location = getLocationByIdOrThrow(user.getLocation());
        LocationInfo locationInfo = location.getInfo(Language.en);
        user.setCountry(locationInfo.getCountry());
        user.setCity(locationInfo.getCity());
        setAvatar(request, user);
    }

    public void setUsersInfo(HttpServletRequest request, Iterable<User> users) {
        for(User user : users){
            setUserInfo(request, user);
        }
    }

    public void setAvatar(HttpServletRequest request, Iterable<? extends WithAvatar> withAvatars) {
        for(WithAvatar withAvatar : withAvatars){
            setAvatar(request, withAvatar);
        }
    }

    private String getPhotoRatingOrderingString(RatingOrder order) {
        String orderString;
        switch (order) {
            case hottest:
                orderString = HOTTEST_PHOTO_MAX_ORDERING;
                break;
            case rated:
                orderString = MOST_RATED_PHOTO_MAX_ORDERING;
                break;
            default:
                orderString = NEWEST_PHOTO_MAX_ORDERING;
                break;
        }

        return orderString;
    }

    private String getPeopleOrderingString(RatingOrder order) {
        String orderString;
        switch (order) {
            case hottest:
                throw new UnsupportedOperationException("hottest is not supported yet");
            case rated:
                orderString = RATING_PEOPLE_ORDERING;
                break;
            default:
                orderString = ADDING_DATE_ORDERING;
                break;
        }

        return orderString;
    }

    private void setPhotoquestFollowingParam(HttpServletRequest request, Photoquest photoquest, long signedInUserId) {
        boolean isFollowing = getFollowingPhotoquest(signedInUserId, photoquest.getId()) != null;
        photoquest.setIsFollowing(isFollowing);
    }

    private void setPhotoquestsFollowingParam(HttpServletRequest request,
                                              Iterable<Photoquest> photoquests,
                                              long signedInUserId) {
        for(Photoquest photoquest : photoquests){
            setPhotoquestFollowingParam(request, photoquest, signedInUserId);
        }
    }

    private void setPhotoquestsFollowingParamIfSignedIn(HttpServletRequest request,
                                              Iterable<Photoquest> photoquests) {
        User signedInUser = getSignedInUser(request);
        if(signedInUser != null){
            setPhotoquestsFollowingParam(request, photoquests, signedInUser.getId());
        }
    }

    public Collection<Photoquest> getPhotoQuests(HttpServletRequest request, OffsetLimit offsetLimit,
                                                 RatingOrder order) {
        String orderString = getPhotoRatingOrderingString(order);

        Collection<Photoquest> result =
                DBUtilities.getAllObjectsOfClass(persistenceManager, Photoquest.class,
                        offsetLimit, orderString);
        initPhotoquestsInfo(request, result);

        return result;
    }

    public long getPhotoQuestsCount() {
        return DBUtilities.getAllObjectsOfClassCount(persistenceManager, Photoquest.class);
    }

    public boolean hasFriendship(long user1Id, long user2Id) {
        return getFriendship(user1Id, user2Id) != null;
    }

    public Relationship getFriendship(long user1Id, long user2Id) {
        Relationship friendship = new Relationship();
        friendship.setFromUserId(user1Id);
        friendship.setToUserId(user2Id);
        return DBUtilities.getObjectByPattern(persistenceManager, friendship);
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

        makeAllPersistent(friendship1, friendship2);
    }

    public void addFriend(HttpServletRequest request, long userId) {
        try {
            acceptFriendRequest(request, userId);
        } catch (RelationNotFoundException e) {
            sendFriendRequest(request, userId);
        }
    }

    public void removeFriend(HttpServletRequest request, long userId) {
        try {
            declineFriendRequest(request, userId);
        } catch (RelationNotFoundException e) {
            try {
                cancelFriendRequest(request, userId);
            } catch (RelationNotFoundException e1) {
                removeFriendShip(request, userId);
            }
        }
    }

    private void removeFriendShip(HttpServletRequest request, long userId) {
        User user = getSignedInUserOrThrow(request);
        getUserByIdOrThrow(userId);
        long signedInUserId = user.getId();

        Relationship friendship1 = getFriendshipOrThrow(userId, signedInUserId);
        Relationship friendship2 = getFriendshipOrThrow(signedInUserId, userId);
        deleteAllPersistent(Arrays.asList(friendship1, friendship2));
    }

    public List<Long> getRelationsIdesOf(long userId, OffsetLimit offsetLimit, int relationType, boolean asFromUser) {
        Relationship pattern = new Relationship();
        if (asFromUser) {
            pattern.setFromUserId(userId);
        } else {
            pattern.setToUserId(userId);
        }
        pattern.setType(relationType);

        Collection<Relationship> friendships =
                DBUtilities.queryByPattern(persistenceManager, pattern, offsetLimit, ADDING_DATE_ORDERING);
        ArrayList<Long> result = new ArrayList<Long>(friendships.size());

        for(Relationship friendship : friendships){
            result.add(friendship.getToUserId());
        }

        return result;
    }

    public List<Long> getFriendsIdesOf(long userId, OffsetLimit offsetLimit) {
        return getRelationsIdesOf(userId, offsetLimit, Relationship.FRIENDSHIP, true);
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
        Relationship friendshipPattern = new Relationship();
        friendshipPattern.setType(Relationship.FRIENDSHIP);
        friendshipPattern.setFromUserId(getSignedInUserOrThrow(request).getId());
        return DBUtilities.queryCountByPattern(persistenceManager, friendshipPattern);
    }

    public List<User> getFriends(HttpServletRequest request, OffsetLimit offsetLimit, boolean fillFriendShipData) {
        List<User> friends = getFriendsOf(getSignedInUserOrThrow(request).getId(), offsetLimit);
        setUsersInfo(request, friends);
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

        long toUserId;
        User toUser;
        if(toCommentId != null){
            Comment toComment = getCommentByIdOrThrow(toCommentId);
            comment.setToCommentId(toCommentId);
            toUserId = toComment.getUserId();
            toUser = getUserByIdOrThrow(toUserId);
            comment.setToUserId(toUser.getId());
            setAvatar(request, toUser);
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

        comment = (Comment) makeAllPersistent(comment, toUser)[0];
        reply.setId(comment.getId());
        makePersistent(reply);

        return comment;
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
        Message pattern = new Message();
        pattern.setFromUserId(widthUserId);
        pattern.setToUserId(signedInUser.getId());
        Collection<Message> messages = queryByAddingDate(pattern, offsetLimit);
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
        update(request, forUpdate);

        return messages;
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

    public Relationship getFriendRequest(long fromUserId, long toUserId) {
        Relationship request = new Relationship();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setType(Relationship.FRIEND_REQUEST);
        return DBUtilities.getObjectByPattern(persistenceManager, request);
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
        return DBUtilities.getObjectByPattern(persistenceManager, relationship);
    }

    public Relationship sendFriendRequest(HttpServletRequest request, long userId) {
        User signedInUser = getSignedInUserOrThrow(request);
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

        return (Relationship) makeAllPersistent(friendRequest, signedInUser, friend)[0];
    }

    private Relationship deleteFriendRequestOrUnfollow(HttpServletRequest request, User fromUser, User toUser) {
        Relationship relationship = getRelationship(fromUser.getId(), toUser.getId());
        if(relationship == null){
            throw new RelationNotFoundException();
        }

        if(relationship.getType() == Relationship.FRIEND_REQUEST){
            fromUser.decrementSentRequestsCount();
            toUser.decrementReceivedRequestsCount();
            update(request, fromUser, toUser);
        }

        deletePersistent(relationship);
        return relationship;
    }

    private Relationship deleteFriendRequest(HttpServletRequest request, User fromUser, User toUser) {
        Relationship friendRequest = getFriendRequestOrThrow(fromUser.getId(),
                toUser.getId());
        deletePersistent(friendRequest);
        fromUser.decrementSentRequestsCount();
        toUser.decrementReceivedRequestsCount();
        update(request, fromUser, toUser);
        return friendRequest;
    }

    private void cancelFriendRequest(HttpServletRequest request, long userId) {
        User signedInUser = getSignedInUserOrThrow(request);
        User friend = getUserByIdOrThrow(userId);
        deleteFriendRequest(request, signedInUser, friend);
    }

    private void acceptFriendRequest(HttpServletRequest request, long userId) {
        User friend = getUserByIdOrThrow(userId);
        User signedInUser = getSignedInUserOrThrow(request);
        deleteFriendRequestOrUnfollow(request, friend, signedInUser);
        addFriendShip(friend, signedInUser);

        Reply reply = new Reply();
        reply.setId(signedInUser.getId());
        reply.setType(Reply.FRIEND_REQUEST_ACCEPTED);
        reply.setUserId(friend.getId());

        friend.incrementUnreadRepliesCount();
        update(request, friend, reply);
    }

    private void declineFriendRequest(HttpServletRequest request, long userId) {
        User friend = getUserByIdOrThrow(userId);
        User signedInUser = getSignedInUserOrThrow(request);
        deleteFriendRequest(request, friend, signedInUser);

        Long signedInUserId = signedInUser.getId();
        Long friendId = friend.getId();

        Reply reply = new Reply();
        reply.setId(signedInUserId);
        reply.setType(Reply.FRIEND_REQUEST_DECLINED);
        reply.setUserId(friendId);

        followUser(friendId, signedInUserId);

        friend.incrementUnreadRepliesCount();
        update(request, friend, reply);
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
        List<Response> data = new ArrayList<Response>();
        while (data.size() < count){
            data.addAll(RandomUserGenerator.generate(count - data.size()));
        }

        List<User> result = new ArrayList<User>(count);

        for(Response userData : data){
            User user = new User();
            user.setName(userData.name);
            user.setLastName(userData.lastName);
            user.setLogin("user" + startId++);
            user.setPassword(password);
            user.setGender(userData.gender == Gender.male);

            AutoCompleteResult location =
                    googlePlacesSearcher.performAutoCompleteCitiesSearch(userData.city).get(0);
            user.setLocation(location.placeId);


            InputStream avatar = IOUtilities.getBufferedInputStreamFromUrl(userData.largeAvatar);
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

    public long getPhotoInPhotoquestPosition(long photoId, RatingOrder order) {
        Photo photo = getPhotoByIdOrThrow(photoId);
        return getPhotoInPhotoquestPosition(photo, order);
    }

    private long getPhotoInPhotoquestPosition(Photo photo, RatingOrder order) {
        Photo pattern = new Photo();
        pattern.setPhotoquestId(photo.getPhotoquestId());
        return DBUtilities.getPosition(persistenceManager, photo, getPhotoRatingOrderingString(order), pattern);
    }

    public Photo getPhotoAndFillInfo(HttpServletRequest request, long photoId) {
        Photo photo = getPhotoByIdOrThrow(photoId);

        User signedInUser = getSignedInUser(request);
        if(signedInUser != null){
            PhotoView pattern = new PhotoView();
            pattern.setPhotoId(photoId);
            pattern.setUserId(signedInUser.getId());
            PhotoView photoView = DBUtilities.getObjectByPattern(persistenceManager, pattern);
            if(photoView == null){
                photoView = pattern;
            }

            long currentTimeMillis = System.currentTimeMillis();
            if(photoView == pattern || currentTimeMillis -
                    photoView.getAddingDate() >= PHOTO_VIEW_PERIOD){
                photoView.setAddingDate(currentTimeMillis);
                photo.incrementViewsCount();
                makeAllPersistent(photo, photoView);
            }
        }

        initYourLikeParameter(request, photo);
        photo.setPosition(getPhotoInPhotoquestPosition(photo, RatingOrder.rated));
        return photo;
    }

    public List<AutoCompleteResult> getLocationSuggestions(String query) throws IOException {
        return googlePlacesSearcher.performAutoCompleteCitiesSearch(query);
    }

    public UserStats getUserStats(HttpServletRequest request) {
        User user = getSignedInUserOrThrow(request);
        UserStats stats = new UserStats();
        stats.setFriendRequestsCount(user.getReceivedRequestsCount());
        stats.setUnreadMessagesCount(user.getUnreadMessagesCount());
        Long unreadRepliesCount = user.getUnreadRepliesCount();
        stats.setUnreadRepliesCount(unreadRepliesCount == null ? 0 : unreadRepliesCount);
        return stats;
    }

    private Collection<Reply> getReplies(User user, HttpServletRequest request, OffsetLimit offsetLimit) {
        Reply reply = new Reply();
        reply.setUserId(user.getId());

        DBUtilities.QueryParams params = new DBUtilities.QueryParams();
        params.ordering = "addingDate descending";
        params.offsetLimit = offsetLimit;

        return DBUtilities.queryByPattern(persistenceManager, reply, params);
    }

    public Collection<ReplyResponse> getRepliesWithFullInfo(HttpServletRequest request, OffsetLimit offsetLimit) {
        User signedInUser = getSignedInUserOrThrow(request);
        signedInUser.setUnreadRepliesCount(0l);
        update(request, signedInUser);

        Collection<Reply> replies = getReplies(signedInUser, request, offsetLimit);

        Collection<ReplyResponse> replyResponses = new ArrayList<ReplyResponse>(replies.size());
        for(Reply reply : replies){
            ReplyResponse replyResponse = new ReplyResponse();
            int type = reply.getType();
            replyResponse.setType(type);

            User user;
            Long id = reply.getId();
            if(type == Reply.COMMENT){
                Comment comment = getCommentByIdOrThrow(id);
                comment.setPhoto(HttpUtilities.getBaseUrl(request) +
                        Photo.IMAGE_URL_PATH + comment.getPhotoId());
                replyResponse.setComment(comment);
                user = getUserByIdOrThrow(comment.getUserId());
            } else if(type == Reply.FRIEND_REQUEST_ACCEPTED || type == Reply.FRIEND_REQUEST_DECLINED) {
                user = getUserByIdOrThrow(id);
            } else {
                throw new RuntimeException("type is not supported, corrupted database");
            }

            setAvatar(request, user);
            replyResponse.setUser(user);
            replyResponses.add(replyResponse);
        }

        return replyResponses;
    }

    public long getRepliesCount(HttpServletRequest request) {
        User user = getSignedInUserOrThrow(request);
        Reply reply = new Reply();
        reply.setUserId(user.getId());
        return DBUtilities.queryCountByPattern(persistenceManager, reply);
    }

    private List<User> getFriendRequests(HttpServletRequest request, OffsetLimit offsetLimit, boolean received) {
        User signedInUser = getSignedInUserOrThrow(request);
        Long signedInUserId = signedInUser.getId();

        Relationship pattern = new Relationship();
        pattern.setType(Relationship.FRIEND_REQUEST);
        if (received) {
            pattern.setToUserId(signedInUserId);
        } else {
            pattern.setFromUserId(signedInUserId);
        }

        Collection<Relationship> friendRequests = DBUtilities.queryByPattern(persistenceManager, pattern,
                offsetLimit,
                ADDING_DATE_ORDERING);
        List<User> result = new ArrayList<User>(friendRequests.size());

        for(Relationship friendRequest : friendRequests){
            long friendId = received ? friendRequest.getFromUserId() : friendRequest.getToUserId();

            User friend = getUserByIdOrThrow(friendId);
            setAvatar(request, friend);
            friend.setRelation(received ? RelationStatus.request_received : RelationStatus.request_sent);
            result.add(friend);
        }

        return result;
    }

    public List<User> getReceivedFriendRequests(HttpServletRequest request, OffsetLimit offsetLimit) {
        return getFriendRequests(request, offsetLimit, true);
    }

    public List<User> getSentFriendRequests(HttpServletRequest request, OffsetLimit offsetLimit) {
        return getFriendRequests(request, offsetLimit, false);
    }

    private FollowingPhotoquest getFollowingPhotoquest(long userId, long photoquestId) {
        FollowingPhotoquest pattern = new FollowingPhotoquest();
        pattern.setUserId(userId);
        pattern.setPhotoquestId(photoquestId);
        return DBUtilities.getObjectByPattern(persistenceManager, pattern);
    }

    private FollowingPhotoquest getFollowingPhotoquestOrThrow(long userId, long photoquestId) {
        FollowingPhotoquest result = getFollowingPhotoquest(userId, photoquestId);
        if(result == null){
            throw new PhotoquestIsNotFollowingException(userId, photoquestId);
        }

        return result;
    }

    public FollowingPhotoquest followPhotoquest(HttpServletRequest request, long photoquestId) {
        Photoquest photoquest = getPhotoQuestByIdOrThrow(photoquestId);
        User signedInUser = getSignedInUserOrThrow(request);
        Long signedInUserId = signedInUser.getId();

        FollowingPhotoquest followingPhotoquest = getFollowingPhotoquest(signedInUserId, photoquestId);
        if(followingPhotoquest != null){
            throw new PhotoquestIsFollowingException();
        }

        followingPhotoquest = new FollowingPhotoquest();
        followingPhotoquest.setUserId(signedInUserId);
        followingPhotoquest.setPhotoquestId(photoquestId);

        return makePersistent(followingPhotoquest);
    }

    public void unfollowPhotoquest(HttpServletRequest request, long photoquestId) {
        User signedInUser = getSignedInUserOrThrow(request);
        Long signedInUserId = signedInUser.getId();

        FollowingPhotoquest followingPhotoquest = getFollowingPhotoquestOrThrow(signedInUserId, photoquestId);
        deletePersistent(followingPhotoquest);
    }

    public Collection<Photoquest> getFollowingPhotoquests(HttpServletRequest request, OffsetLimit offsetLimit,
                                                          RatingOrder order) {
        User signedInUser = getSignedInUserOrThrow(request);
        Collection<Photoquest> result =
                advancedRequestsManager.getFollowingPhotoquests(signedInUser.getId(), order, offsetLimit);
        for(Photoquest photoquest : result){
            photoquest.setIsFollowing(true);
            setAvatar(request, photoquest);
        }

        return result;
    }

    public long getFollowingPhotoquestsCount(HttpServletRequest request) {
        User signedInUser = getSignedInUserOrThrow(request);
        FollowingPhotoquest pattern = new FollowingPhotoquest();
        pattern.setUserId(signedInUser.getId());

        return DBUtilities.queryCountByPattern(persistenceManager, pattern);
    }

    private Relationship followUser(long fromUserId, long toUserId) {
        Relationship relationship = new Relationship();
        relationship.setFromUserId(fromUserId);
        relationship.setToUserId(toUserId);
        relationship.setType(Relationship.FOLLOWS);
        return makePersistent(relationship);
    }

    private void unfollowUser(long fromUserId, long toUserId) {
        Relationship relationship = new Relationship();
        relationship.setFromUserId(fromUserId);
        relationship.setToUserId(toUserId);
        relationship.setType(Relationship.FOLLOWS);

        if(DBUtilities.getObjectByPattern(persistenceManager, relationship) == null){
            throw new UserIsNotFollowingException();
        }

        deletePersistent(relationship);
    }

    private void setAvatarAndRelation(HttpServletRequest request, Collection<User> users, RelationStatus status) {
        for(User user : users){
            setAvatar(request, user);
            user.setRelation(status);
        }
    }

    private Collection<User> getFollowers(
            HttpServletRequest request,
            long followingUserId, OffsetLimit offsetLimit) {
        Collection<User> users =
                getUsersByIdes(getRelationsIdesOf(followingUserId, offsetLimit, Relationship.FOLLOWS, false));
        setAvatarAndRelation(request, users, RelationStatus.followed);

        return users;
    }

    private Collection<User> getFollowingUsers(
            HttpServletRequest request,
            long followerUserId, OffsetLimit offsetLimit) {
        Collection<User> users =
                getUsersByIdes(getRelationsIdesOf(followerUserId, offsetLimit, Relationship.FOLLOWS, true));
        setAvatarAndRelation(request, users, RelationStatus.follows);

        return users;
    }

    public Collection<User> getFollowingUsers(HttpServletRequest request, OffsetLimit offsetLimit) {
        User signedInUser = getSignedInUserOrThrow(request);
        return getFollowingUsers(request, signedInUser.getId(), offsetLimit);
    }

    public Collection<User> getFollowers(HttpServletRequest request, OffsetLimit offsetLimit) {
        User signedInUser = getSignedInUserOrThrow(request);
        return getFollowers(request, signedInUser.getId(), offsetLimit);
    }

    public void unfollowUser(HttpServletRequest request, long toUserId) {
        Long fromUserId = getSignedInUserOrThrow(request).getId();
        unfollowUser(fromUserId, toUserId);
    }

    private Feed createFeedFromAction(HttpServletRequest request, Action action, boolean includeUser) {
        Feed feed = new Feed();
        feed.setAddingDate(action.getAddingDate());

        Long photoId = action.getPhotoId();
        if (photoId != null) {
            Photo photo = getPhotoByIdOrThrow(photoId);
            initPhotoUrl(photo, request);
            feed.setPhoto(photo);
        }

        Photoquest photoquest = getPhotoQuestByIdOrThrow(action.getPhotoquestId());
        feed.setPhotoquest(photoquest);

        if (includeUser) {
            User user = getUserByIdOrThrow(action.getUserId());
            feed.setUser(user);
            setAvatar(request, user);
        }

        return feed;
    }

    private List<Feed> actionsToFeeds(HttpServletRequest request,
                                      Iterable<Action> actions, boolean includeUser) {
        List<Feed> feeds = new ArrayList<Feed>();
        for(Action action : actions){
            feeds.add(createFeedFromAction(request, action, includeUser));
        }

        return feeds;
    }

    public List<Feed> getNews(HttpServletRequest request, OffsetLimit offsetLimit) {
        User signedInUser = getSignedInUserOrThrow(request);
        Collection<Action> actions = advancedRequestsManager.getNews(signedInUser.getId(), offsetLimit);
        return actionsToFeeds(request, actions, true);
    }

    public long getNewsCount(HttpServletRequest request) {
        User signedInUser = getSignedInUserOrThrow(request);
        return advancedRequestsManager.getNewsCount(signedInUser.getId());
    }

    public List<Feed> getUserNews(HttpServletRequest request, long userId, OffsetLimit offsetLimit) {
        Action pattern = new Action();
        pattern.setUserId(userId);
        Collection<Action> actions = DBUtilities.queryByPattern(persistenceManager, pattern, offsetLimit,
                ADDING_DATE_ORDERING);
        return actionsToFeeds(request, actions, false);
    }

    public long getUserNewsCount(long userId) {
        Action pattern = new Action();
        pattern.setUserId(userId);
        return DBUtilities.queryCountByPattern(persistenceManager, pattern);
    }

    public void initDatabase() {
        advancedRequestsManager.initDatabase();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        persistenceManager.close();
    }
}
