package com.neobanco.backend.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private final int MAX_ATTEMPT = 5;
    private final long LOCK_TIME_DURATION = TimeUnit.MINUTES.toMillis(15);
    
    // Almacena: email -> [intentos_fallidos, timestamp_del_ultimo_intento]
    private ConcurrentHashMap<String, long[]> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
    }

    public void loginFailed(String key) {
        long[] attempts = attemptsCache.getOrDefault(key, new long[]{0, System.currentTimeMillis()});
        attempts[0]++;
        attempts[1] = System.currentTimeMillis();
        attemptsCache.put(key, attempts);
    }

    public boolean isBlocked(String key) {
        if (!attemptsCache.containsKey(key)) {
            return false;
        }
        long[] attempts = attemptsCache.get(key);
        if (attempts[0] >= MAX_ATTEMPT) {
            long timePassed = System.currentTimeMillis() - attempts[1];
            if (timePassed > LOCK_TIME_DURATION) {
                // El castigo ya pasó
                attemptsCache.remove(key);
                return false;
            }
            return true;
        }
        return false;
    }
}
