package com.tiksem.pq.db;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.pq.data.City;
import com.tiksem.pq.data.Country;
import com.utils.framework.CollectionUtils;
import com.utils.framework.collections.iterator.AbstractIterator;
import com.vkapi.location.Language;
import com.vkapi.location.VkCity;
import com.vkapi.location.VkCountry;
import com.vkapi.location.VkLocationApi;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * Created by CM on 1/22/2015.
 */
public class LocationManager {
    private MysqlObjectMapper mapper;
    private OutputStream progressStream;

    public LocationManager(MysqlObjectMapper mapper) {
        this.mapper = mapper;
    }

    public OutputStream getProgressStream() {
        return progressStream;
    }

    public void setProgressStream(OutputStream progressStream) {
        this.progressStream = progressStream;
    }

    public List<Country> getCountries() {
        return mapper.executeSQLQuery("SELECT * FROM country", Country.class);
    }

    private void writeProgress(String progress) {
        try {
            IOUtils.write(progress + "\n", progressStream);
        } catch (IOException e) {

        }
    }

    public void updateCountries() throws IOException {
        writeProgress("deleting countries...");
        mapper.executeNonSelectSQL("DELETE FROM country");
        writeProgress("inserting countries...");
        List<Country> countries = getCountriesFromVk();
        mapper.insertAll(countries);

    }

    public void updateCities() {
        writeProgress("deleting cities...");
        mapper.executeNonSelectSQL("DELETE FROM city");
        writeProgress("selecting countries...");
        List<Country> countries = getCountries();
        int citiesTotal = 0;
        int countryIndex = 0;
        for(final Country country : countries){
            final Iterator<VkCity> enCities = VkLocationApi.getCities(country.getId(), Language.en).iterator();
            final Iterator<VkCity> ruCities = VkLocationApi.getCities(country.getId(), Language.ru).iterator();

            final Iterator<City> cityIterator = new AbstractIterator<City>(){
                @Override
                public City next() {
                    VkCity en = enCities.next();
                    VkCity ru = ruCities.next();
                    if(en.id != ru.id){
                        throw new RuntimeException("WTF?");
                    }

                    City city = new City();
                    city.setId(en.id);
                    city.setCountryId(country.getId());
                    city.setRusName(ru.title);
                    city.setEngName(en.title);

                    return city;
                }

                @Override
                public boolean hasNext() {
                    return enCities.hasNext();
                }
            };

            writeProgress("inserting cities of country " + country.getEngName() + "...");
            while (cityIterator.hasNext()) {
                List<City> cities = CollectionUtils.toList(new Iterable<City>() {
                    @Override
                    public Iterator<City> iterator() {
                        return cityIterator;
                    }
                }, 500);
                mapper.insertAll(cities);
                citiesTotal += cities.size();
                writeProgress(citiesTotal + " cities inserted");
            }

            writeProgress((countryIndex + 1) + " / " + countries.size() + " inserted");
            countryIndex++;
        }
    }

    public void updateCitiesAndCountries() throws IOException {
        updateCountries();
        updateCities();
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
