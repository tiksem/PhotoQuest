package com.tiksem.pq.db;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.pq.data.Country;
import com.vkapi.location.Language;
import com.vkapi.location.VkCountry;
import com.vkapi.location.VkLocationApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by CM on 1/22/2015.
 */
public class LocationManager {
    private MysqlObjectMapper mapper;

    public LocationManager(MysqlObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<Country> getCountries() {
        return mapper.executeSQLQuery("SELECT * FROM country", Country.class);
    }

    public void updateCountries() throws IOException {
        List<Country> countries = getCountriesFromVk();

        mapper.insertAll(countries);
    }

    private List<Country> getCountriesFromVk() throws IOException {
        List<VkCountry> enCountries = VkLocationApi.getAllCountries(Language.en);
        List<VkCountry> ruCountries = VkLocationApi.getAllCountries(Language.ru);

        if(enCountries.size() != ruCountries.size()){
            throw new IllegalStateException("enCountries.size() != ruCountries.size()");
        }

        Comparator<VkCountry> comparator = new Comparator<VkCountry>() {
            @Override
            public int compare(VkCountry a, VkCountry b) {
                return (int) (a.id - b.id);
            }
        };
        Collections.sort(ruCountries, comparator);
        Collections.sort(enCountries, comparator);

        List<Country> countries = new ArrayList<Country>();

        int size = enCountries.size();
        for (int i = 0; i < size; i++) {
            Country country = new Country();
            VkCountry enCountry = enCountries.get(i);
            VkCountry ruCountry = ruCountries.get(i);
            if(enCountry.id != ruCountry.id){
                throw new RuntimeException("WTF?");
            }

            country.setId(enCountry.id);
            country.setEngName(enCountry.title);
            country.setRusName(ruCountry.title);
            countries.add(country);
        }

        return countries;
    }
}
