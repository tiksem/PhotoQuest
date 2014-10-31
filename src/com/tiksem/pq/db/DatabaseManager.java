package com.tiksem.pq.db;

import com.tiksem.pq.data.User;
import com.tiksem.pq.db.exceptions.UserExistsRegisterException;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

/**
 * Created by CM on 10/30/2014.
 */
public class DatabaseManager {
    private static DatabaseManager instance;

    private PersistenceManagerFactory factory;

    public static DatabaseManager getInstance() {
        if(instance == null){
            instance = new DatabaseManager();
        }

        return instance;
    }

    private DatabaseManager() {
        factory  = ObjectDBUtilities.createLocalConnectionFactory("PhotoQuest");
    }

    public User getUserByLogin(String login) {
        User user = new User();
        user.setLogin(login);
        return ObjectDBUtilities.getObjectByPattern(factory.getPersistenceManager(), user);
    }

    public User addUser(String login, String password) {
        PersistenceManager persistenceManager = factory.getPersistenceManager();
        Transaction transaction = persistenceManager.currentTransaction();
        transaction.begin();

        User user = getUserByLogin(login);
        if(user != null){
            throw new UserExistsRegisterException(login);
        }

        user = new User(login, password);
        user = persistenceManager.makePersistent(user);

        transaction.commit();
        return user;
    }
}
