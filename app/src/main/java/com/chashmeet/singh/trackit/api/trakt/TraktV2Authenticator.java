package com.chashmeet.singh.trackit.api.trakt;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import com.chashmeet.singh.trackit.api.trakt.entities.AccessToken;

public class TraktV2Authenticator implements Authenticator {

    public final TraktV2 trakt;

    public TraktV2Authenticator(TraktV2 trakt) {
        this.trakt = trakt;
    }

    public static Request handleAuthenticate(Response response, TraktV2 trakt) throws IOException {
        if (!TraktV2.API_HOST.equals(response.request().url().host())) {
            return null; // not a trakt API endpoint (possibly trakt OAuth or other API), give up.
        }
        if (responseCount(response) >= 2) {
            return null; // failed 2 times, give up.
        }
        if (trakt.refreshToken() == null || trakt.refreshToken().length() == 0) {
            return null; // have no refresh token, give up.
        }

        retrofit2.Response<AccessToken> refreshResponse = trakt.refreshAccessToken();
        if (!refreshResponse.isSuccessful()) {
            return null; // failed to retrieve a token, give up.
        }

        String accessToken = refreshResponse.body().access_token;
        trakt.accessToken(accessToken);
        trakt.refreshToken(refreshResponse.body().refresh_token);

        return response.request().newBuilder()
                .header(TraktV2.HEADER_AUTHORIZATION, "Bearer" + " " + accessToken)
                .build();
    }

    private static int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        return handleAuthenticate(response, trakt);
    }

}
