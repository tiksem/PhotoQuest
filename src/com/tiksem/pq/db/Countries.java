package com.tiksem.pq.db;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.pq.data.Country;
import com.utils.framework.CollectionUtils;
import com.utils.framework.Predicate;
import com.utils.framework.algorithms.Search;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by CM on 1/22/2015.
 */
public class Countries {
    private static Countries instance;
    private List<Country> countries;
    private MysqlObjectMapper mapper;
    private LocationManager locationManager;

    public synchronized static Countries getInstance() {
        if(instance == null){
            instance = new Countries();
        }

        return instance;
    }

    private Countries() {
        mapper = new MysqlObjectMapper();
        locationManager = new LocationManager(mapper);
        countries = locationManager.getCountries();
        mapper.close();
    }

    public Country getById(final short id) {
        return CollectionUtils.find(countries, new Predicate<Country>() {
            @Override
            public boolean check(Country item) {
                return item.getId().equals(id);
            }
        });
    }

    public List<Country> filterCountries(final String query, int limit) {
        return CollectionUtils.findAll(countries, new Predicate<Country>() {
            @Override
            public boolean check(Country item) {
                return item.getEngName().contains(query) || item.getRusName().contains(query);
            }
        }, limit);
    }

    public Country getCountryByName(final String name) {
        return CollectionUtils.find(countries, new Predicate<Country>() {
            @Override
            public boolean check(Country item) {
                return item.getEngName().equalsIgnoreCase(name) || item.getRusName().equalsIgnoreCase(name);
            }
        });
    }
}
