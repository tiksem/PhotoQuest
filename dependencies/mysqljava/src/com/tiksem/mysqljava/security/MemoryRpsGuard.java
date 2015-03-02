package com.tiksem.mysqljava.security;

import com.tiksem.mysqljava.MysqlObjectMapper;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by CM on 3/2/2015.
 */
public class MemoryRpsGuard extends AbstractRpsGuard {
    private Map<ApiRequest, ApiRequest> requests = new ConcurrentHashMap<ApiRequest, ApiRequest>();

    public MemoryRpsGuard(Map<String, String> settings) {
        super(settings);
    }

    @Override
    protected void replace(MysqlObjectMapper mapper, ApiRequest request) {
        requests.put(request, request);
    }

    @Override
    protected void insert(MysqlObjectMapper mapper, ApiRequest request) {
        requests.put(request, request);
    }

    @Override
    protected ApiRequest getObjectByPattern(MysqlObjectMapper mapper, ApiRequest pattern) {
        return requests.get(pattern);
    }

    @Override
    public void clearUnbannedIPes(MysqlObjectMapper mapper) {
        for(Iterator<Map.Entry<ApiRequest, ApiRequest>> it = requests.entrySet().iterator(); it.hasNext(); ) {
            ApiRequest apiRequest = it.next().getValue();
            if(apiRequest.getBannedUntilTime() == null &&
                    apiRequest.getAttemptsCountBeforeBan() == null){
                it.remove();
            }
        }
    }
}
