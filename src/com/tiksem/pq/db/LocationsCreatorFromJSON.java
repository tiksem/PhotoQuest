package com.tiksem.pq.db;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.mysqljava.MysqlTablesCreator;
import com.tiksem.pq.data.City;
import com.tiksem.pq.data.Country;
import com.tiksem.pq.data.ProgressOperation;
import com.utils.framework.CollectionUtils;
import com.utils.framework.Predicate;
import com.utils.framework.google.translate.GoogleTranslate;
import com.utils.framework.io.IOUtilities;
import com.utils.framework.parsers.json.JsonCollections;
import com.vkapi.location.*;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Created by CM on 2/8/2015.
 */
public class LocationsCreatorFromJSON {
    private MysqlObjectMapper mapper;
    private String fileName;
    private List<Country> countries;
    private DatabaseAsyncTaskManager.Handler asyncTaskHandler;

    public LocationsCreatorFromJSON(MysqlObjectMapper mapper, String fileName) {
        this.mapper = mapper;
        this.fileName = fileName;
        asyncTaskHandler = DatabaseAsyncTaskManager.getInstance().createHandler();
    }

    private void insertCountries() {

    }

    private static class VkCountriesInfo {
        Map<String, String> translation;
        Map<String, Short> ides;
    }

    private VkCountriesInfo getCountriesVkInfo() {
        List<VkCountry> enCountries = VkLocationApi.getAllCountries(Language.en);
        List<VkCountry> ruCountries = VkLocationApi.getAllCountries(Language.ru);

        Map<String, String> translation = new HashMap<String, String>();
        Map<String, Short> ides = new HashMap<String, Short>();
        for (final VkCountry enCountry : enCountries) {
            VkCountry ruCountry = CollectionUtils.find(ruCountries, new Predicate<VkCountry>() {
                @Override
                public boolean check(VkCountry item) {
                    return item.id == enCountry.id;
                }
            });

            translation.put(enCountry.title, ruCountry.title);
            ides.put(enCountry.title, enCountry.id);
        }

        VkCountriesInfo result = new VkCountriesInfo();
        result.translation = translation;
        result.ides = ides;
        return result;
    }

    public void initLocations() throws IOException, JSONException {
        MysqlTablesCreator tablesCreator = new MysqlTablesCreator(mapper);
        tablesCreator.updateAndCreateTables(
                Arrays.asList(City.class, Country.class), null, null);
        String source = IOUtilities.toString(new FileInputStream(fileName));
        JSONObject json = new JSONObject(source);
        final Map<String, List<String>> map = JsonCollections.asStringListMap(json);
        List<String> enNames = new ArrayList<String>(map.keySet());

        final ProgressOperationManager progressOperationManager = new ProgressOperationManager(mapper);
        progressOperationManager.newOperation("Resolving Countries from vk", -1);

        final VkCountriesInfo vkCountriesInfo = getCountriesVkInfo();
        countries = CollectionUtils.transform(enNames, new CollectionUtils.Transformer<String, Country>() {
            @Override
            public Country get(String enName) {
                Country country = new Country();
                country.setEnName(enName);
                String ruName = vkCountriesInfo.translation.get(enName);
                country.setRuName(ruName);

                return country;
            }
        });

        progressOperationManager.newOperation("inserting countries...", -1);

        mapper.insertAll(countries);

        boolean a = false;
        if(a){
            return;
        }

        countries = mapper.executeSQLQuery("SELECT * FROM country", Country.class);

        progressOperationManager.newOperation("Resolving cities of countries...", countries.size());

        List<City> cities = new ArrayList<City>();
        for (final Country country : countries) {
            final int countryId = country.getId();
            List<String> cityNames = map.get(country.getEnName());
            CollectionUtils.transformAndAdd(cityNames, cities, new CollectionUtils.Transformer<String, City>() {
                @Override
                public City get(String name) {
                    City city = new City();
                    city.setEnName(name);
                    city.setCountryId(countryId);

                    try {
                        Short vkCountryId = vkCountriesInfo.ides.get(country.getEnName());
                        if (vkCountryId != null) {
                            List<VkCity> vkCities = VkLocationApi.searchCities(name, vkCountryId,
                                    Language.ru, 1);
                            if(!vkCities.isEmpty()){
                                String ruName = vkCities.get(0).title;
                                city.setRuName(ruName);
                            }
                        }
                    } catch (VkException e) {

                    }

                    progressOperationManager.setProgress(countries.indexOf(country));

                    return city;
                }
            });
        }

        try {
            progressOperationManager.newOperation("Inserting cities...", cities.size());
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    MysqlObjectMapper mapper = new MysqlObjectMapper();

                    while (true) {
                        try {
                            Thread.sleep(2000);
                            long count = mapper.getAllObjectsCount(City.class);
                            progressOperationManager.setProgress((int) count, mapper);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
            });
            mapper.insertAll(cities, true);
            thread.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
