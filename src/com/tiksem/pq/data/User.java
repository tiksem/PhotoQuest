package com.tiksem.pq.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiksem.mysqljava.FieldsCheckingUtilities;
import com.tiksem.mysqljava.annotations.*;
import com.tiksem.mysqljava.annotations.NotNull;
import com.tiksem.mysqljava.exceptions.FieldCheckingException;

/**
 * Created by CM on 10/30/2014.
 */

@MultipleIndexes(indexes = {
        @MultipleIndex(fields = {"cityId", "nameData"}),
        @MultipleIndex(fields = {"cityId", "gender", "nameData"}),
        @MultipleIndex(fields = {"gender", "nameData"}),
        @MultipleIndex(fields = {"cityId", "gender", "rating", "id"}),
        @MultipleIndex(fields = {"gender", "rating", "id"}),
        @MultipleIndex(fields = {"cityId", "rating", "id"}),
        @MultipleIndex(fields = {"cityId", "gender", "id"}),
        @MultipleIndex(fields = {"gender", "id"}),
        @MultipleIndex(fields = {"cityId", "id"}),
        @MultipleIndex(fields = {"rating", "id"}),
        @MultipleIndex(fields = {"countryId", "nameData"}),
        @MultipleIndex(fields = {"countryId", "gender", "nameData"}),
        @MultipleIndex(fields = {"countryId", "gender", "rating", "id"}),
        @MultipleIndex(fields = {"countryId", "rating", "id"}),
        @MultipleIndex(fields = {"countryId", "gender", "id"}),
        @MultipleIndex(fields = {"countryId", "id"})
})
@Table
public class User implements WithAvatar {
    public static final int MAX_NAME_LENGTH = 20;
    public static final int MAX_LAST_NAME_LENGTH = 40;

    @PrimaryKey
    private Long id;

    @Unique(type = "VARCHAR(25)")
    @NotNull
    @Login
    @JsonIgnore
    private String login;
    @Stored(type = "VARCHAR(20)")
    @NotNull
    @JsonIgnore
    @Password
    private String password;

    @Stored
    private Long avatarId;

    @NotNull
    @Index(type = "VARCHAR(60)")
    @JsonIgnore
    private String nameData;

    private String name;
    private String lastName;

    @Stored
    @AddingDate
    @JsonIgnore
    private Long addingDate;

    @Stored
    @NotNull
    private Long unreadMessagesCount;
    
    @Stored
    @NotNull
    private Long unreadRepliesCount;
    
    @Stored
    @NotNull
    private Long sentRequestsCount;

    @Stored
    @NotNull
    private Long receivedRequestsCount;

    @Stored
    @NotNull
    private Integer cityId;

    @Stored
    @NotNull
    private Integer countryId;

    @Stored
    @NotNull
    @JsonIgnore
    private Long rating;

    @NotNull
    @Stored
    private Boolean gender;

    private String avatar;

    private RelationStatus relation;

    private String country;
    private String city;

    public User(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public User() {
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getAvatar() {
        return avatar;
    }

    @Override
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public Long getAvatarId() {
        return avatarId;
    }

    @Override
    public void setAvatarId(Long avatarId) {
        this.avatarId = avatarId;
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public RelationStatus getRelation() {
        return relation;
    }

    public void setRelation(RelationStatus relation) {
        this.relation = relation;
    }

    public Long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(Long addingDate) {
        this.addingDate = addingDate;
    }

    @OnPrepareForStorage
    public void prepareForStorage() {
        if(unreadMessagesCount == null){
            unreadMessagesCount = 0l;
        }

        if(receivedRequestsCount == null){
            receivedRequestsCount = 0l;
        }

        if(sentRequestsCount == null){
            sentRequestsCount = 0l;
        }
        
        if(unreadRepliesCount == null){
            unreadRepliesCount = 0l;
        }

        if(rating == null){
            rating = 0l;
        }
    }

    public Long getUnreadMessagesCount() {
        return unreadMessagesCount;
    }

    public void setUnreadMessagesCount(Long unreadMessagesCount) {
        this.unreadMessagesCount = unreadMessagesCount;
    }

    public Long getSentRequestsCount() {
        return sentRequestsCount;
    }

    public void setSentRequestsCount(Long sentRequestsCount) {
        this.sentRequestsCount = sentRequestsCount;
    }

    public Long getReceivedRequestsCount() {
        return receivedRequestsCount;
    }

    public void setReceivedRequestsCount(Long receivedRequestsCount) {
        this.receivedRequestsCount = receivedRequestsCount;
    }

    public void incrementUnreadMessagesCount() {
        if(unreadMessagesCount == null){
            unreadMessagesCount = 0l;
        }

        unreadMessagesCount++;
    }

    public void decrementUnreadMessagesCount() {
        if(unreadMessagesCount == null){
            unreadMessagesCount = 0l;
        }

        unreadMessagesCount--;
        if(unreadMessagesCount < 0){
            throw new RuntimeException("unreadMessagesCount < 0");
        }
    }

    public void incrementSentRequestsCount() {
        if(sentRequestsCount == null){
            sentRequestsCount = 0l;
        }

        sentRequestsCount++;
    }

    public void decrementSentRequestsCount() {
        if(sentRequestsCount == null){
            sentRequestsCount = 0l;
        }

        sentRequestsCount--;
        if(sentRequestsCount < 0){
            throw new RuntimeException("sentRequestsCount < 0");
        }
    }

    public void incrementReceivedRequestsCount() {
        if(receivedRequestsCount == null){
            receivedRequestsCount = 0l;
        }

        receivedRequestsCount++;
    }

    public void decrementReceivedRequestsCount() {
        if(receivedRequestsCount == null){
            receivedRequestsCount = 0l;
        }

        receivedRequestsCount--;
        if(receivedRequestsCount < 0){
            throw new RuntimeException("receivedRequestsCount < 0");
        }
    }

    public void incrementUnreadRepliesCount() {
        if(unreadRepliesCount == null){
            unreadRepliesCount = 0l;
        }

        unreadRepliesCount++;
    }

    public void decrementUnreadRepliesCount() {
        if(unreadRepliesCount == null){
            unreadRepliesCount = 0l;
        }

        unreadRepliesCount--;
        if(unreadRepliesCount < 0){
            throw new RuntimeException("unreadRepliesCount < 0");
        }
    }

    public void incrementRating() {
        if(rating == null){
            rating = 0l;
        }

        rating++;
    }

    public void decrementRating() {
        if(rating == null){
            rating = 0l;
        }

        rating--;
        if(rating < 0){
            throw new RuntimeException("rating < 0");
        }
    }

    public Long getUnreadRepliesCount() {
        return unreadRepliesCount;
    }

    public void setUnreadRepliesCount(Long unreadRepliesCount) {
        this.unreadRepliesCount = unreadRepliesCount;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Long getRating() {
        return rating;
    }

    public void setRating(Long rating) {
        this.rating = rating;
    }

    public Boolean getGender() {
        return gender;
    }

    public void setGender(Boolean gender) {
        this.gender = gender;
    }

    public String getNameData() {
        return nameData;
    }

    public void setNameAndLastName(String name, String lastName) {
        this.name = FieldsCheckingUtilities.getFixedNameField(name, getClass(), "name");
        this.lastName = FieldsCheckingUtilities.getFixedNameField(lastName, getClass(), "lastName");;

        if(this.name.length() > MAX_NAME_LENGTH){
            throw new FieldCheckingException("name", "name is too big");
        }

        if (this.lastName.length() > MAX_LAST_NAME_LENGTH) {
            throw new FieldCheckingException("lastName", "lastName is too big");
        }

        this.nameData = this.name + " " + this.lastName;
    }

    public void setNameData(String nameData) {
        String[] parts = nameData.split(" ");
        if(parts.length != 2){
            throw new IllegalArgumentException("Illegal namedData format, should be something like " +
                    "Ivan Eblan");
        }

        setNameAndLastName(parts[0], parts[1]);
    }

    public Integer getCountryId() {
        return countryId;
    }

    public void setCountryId(Integer countryId) {
        this.countryId = countryId;
    }
}
