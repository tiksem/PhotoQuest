package com.tiksem.pq.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiksem.pq.data.annotations.AddingDate;
import com.tiksem.pq.data.annotations.Login;
import com.tiksem.pq.data.annotations.NameField;
import com.tiksem.pq.data.annotations.Password;

import javax.jdo.annotations.*;
import java.sql.Date;

/**
 * Created by CM on 10/30/2014.
 */

@PersistenceCapable
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

    @NotPersistent
    private String avatar;

    @NotPersistent
    private Boolean isFriend;

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

    public Boolean getIsFriend() {
        return isFriend;
    }

    public void setIsFriend(Boolean isFriend) {
        this.isFriend = isFriend;
    }

    public Long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(Long addingDate) {
        this.addingDate = addingDate;
    }
}
