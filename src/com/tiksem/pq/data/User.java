package com.tiksem.pq.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiksem.pq.data.annotations.*;
import sun.util.resources.LocaleNames_mk;

import javax.jdo.annotations.*;
import java.sql.Date;

/**
 * Created by CM on 10/30/2014.
 */

@PersistenceCapable
@PersistenceAware
public class User implements WithAvatar {
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long id;

    @Index
    @Login
    private String login;
    @Index
    @Password
    private String password;

    @JsonIgnore
    @Index
    private Long avatarId;

    @Index
    @NameField
    private String name;
    @Index
    @NameField
    private String lastName;

    @Index
    @AddingDate
    private Long addingDate;

    @Index
    private Long unreadMessagesCount;
    
    @Index
    private Long unreadRepliesCount;
    
    @Index
    private Long sentRequestsCount;

    @Index
    private Long receivedRequestsCount;

    @Index
    private String location;

    @Index
    private String countryCode;

    @NotPersistent
    private String avatar;

    @NotPersistent
    private RelationStatus relation;

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

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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
    }

    public Long getUnreadMessagesCount() {
        return unreadMessagesCount;
    }

    public void setUnreadMessagesCount(Long unreadMessagesCount) {
        if(unreadMessagesCount < 0){
            unreadMessagesCount = 0l;
        }

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

    public Long getUnreadRepliesCount() {
        return unreadRepliesCount;
    }

    public void setUnreadRepliesCount(Long unreadRepliesCount) {
        this.unreadRepliesCount = unreadRepliesCount;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
