package com.ag.android.xboxprofile;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XboxConnector {
    private static final String TAG = "XboxConnector";

    private static final String LIVE_AUTH_ENDPOINT = "https://login.live.com/oauth20_authorize.srf";
    private static final String XBOXLIVE_AUTH_ENDPOINT = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_AUTH_ENDPOINT = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String PROFILE_ENDPOINT = "https://profile.xboxlive.com/users/xuid(%s)/settings";
    private static final String ACHIEVEMENTS_ENDPOINT = "https://achievements.xboxlive.com/users/xuid(%s)/achievements";

    private String mToken;
    private String mXuid;
    private String mUserHash;
    private final Listener mListener;

    public interface Listener {
        void onAuthenticationOK();

        void onAuthenticationKO();

        void onProfileReceived(XboxProfile profile);

        void onAchievementsReceived(List<XboxAchievement> achievements);
    }

    public XboxConnector(Listener listener) {
        mListener = listener;
    }

    public void authenticate(String username, String password) {
        Log.i(TAG, "Authenticating");
        new Authenticator().execute(username, password);
    }

    public void getProfile() {
        Log.i(TAG, "Getting user profile");
        new ProfileGetter().execute();
    }

    public void getAchievements() {
        Log.i(TAG, "Getting user achievements");
        new AchievementsGetter().execute();
    }


    //-------- private classes -----------

    private class Authenticator extends AsyncTask<String, Void, Boolean> {
        private static final String TAG = "Authenticator";
        private String mCookie;
        private String mFftag;
        private String mUrlPost;
        private Map<String, String> mAuthData;

        @Override
        protected Boolean doInBackground(String... params) {
            Log.i(TAG, "doInBackground");

            String username = params[0];
            String password = params[1];

            try {
                //getOAuth2Token(username, password);
                preAuth();
                auth(username, password);
                postAuth();
                return true;

            } catch (IOException ioe) {
                Log.e(TAG, "Error during authentication", ioe);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean authOk) {

            if (authOk)
                mListener.onAuthenticationOK();
            else
                mListener.onAuthenticationKO();
        }


        private void preAuth() throws IOException {
            Uri targetUrl = Uri
                    .parse(LIVE_AUTH_ENDPOINT)
                    .buildUpon()
                    .appendQueryParameter("client_id", "000000004C12AE6F")
                    .appendQueryParameter("redirect_uri", "https://login.live.com/oauth20_desktop.srf")
                    .appendQueryParameter("response_type", "token")
                    .appendQueryParameter("scope", "service::user.auth.xboxlive.com::MBI_SSL")
                    .build();

            NetworkUtils.Response res = NetworkUtils.getUrl(targetUrl.toString());

            mCookie = processCookie(res.getHeaders());
            Log.d(TAG, "Cookie found: " + mCookie);

            String resBody = new String(res.getData());

            Pattern fftagPattern = Pattern.compile("sFTTag:'.*value=\"(.*)\"/>'");
            Matcher fftagMatcher = fftagPattern.matcher(resBody);
            if (fftagMatcher.find())
                mFftag = fftagMatcher.group(1);

            Pattern urlPostPattern = Pattern.compile("urlPost:'(.+?(?='))");
            Matcher urlPostMatcher = urlPostPattern.matcher(resBody);
            if (urlPostMatcher.find())
                mUrlPost = urlPostMatcher.group(1);

            Log.d(TAG, "FFTag: [" + mFftag + "]");
            Log.d(TAG, "urlPost: [" + mUrlPost + "]");
        }


/*
        private void getOAuth2Token(String username, String password) throws IOException {
            Map<String, String> postParams = new HashMap<>();
            postParams.put("client_id", username);
            postParams.put("client_secret", password);
            postParams.put("grant_type", password);
            postParams.put("code", password);
            postParams.put("scope", password);
            postParams.put("redirect_uri", password);

            String resBody = NetworkUtils.getString(AUTH_ENDPOINT, postParams);
            Log.d(TAG, resBody);
        }
*/

        private void auth(String username, String password) throws IOException {
            Map<String, String> postParams = new HashMap<>();
            postParams.put("login", username);
            postParams.put("loginfmt", username);
            postParams.put("passwd", password);
            postParams.put("PPFT", mFftag);

            Map<String, String> reqHeaders = new HashMap<>();
            reqHeaders.put("Accept-Encoding", "identity");
            reqHeaders.put("Content-Type", "application/x-www-form-urlencoded");
            reqHeaders.put("Cookie", mCookie);

            NetworkUtils.Response res = NetworkUtils.getUrl(mUrlPost, postParams, reqHeaders);

            if (res.getStatusCode() != 302) {
                Log.e(TAG, new String(res.getData()));
                throw new IOException("Wrong status code: " + res.getStatusCode());
            }

            List<String> locations = res.getHeaders().get("Location");
            if (locations == null || locations.size() == 0) {
                throw new IOException("Missing location header");
            }

            //extract token from redirect URI
            mAuthData = new HashMap<>();
            String location = locations.get(0);
            String token = location.split("#")[1];

            for (String token2: token.split("&")) {
                String[] tokens3 = token2.split("=");
                mAuthData.put(tokens3[0], tokens3[1]);
            }
        }

        private void postAuth() throws IOException {
            String rpsTicket = "t=" + mAuthData.get("access_token");

            Map<String, String> reqHeaders = buildDefaultHeaders();
            reqHeaders.put("Content-Type", "application/json");

            String jsonString = "{ " +
                "\"RelyingParty\": \"http://auth.xboxlive.com\", " +
                "\"TokenType\": \"JWT\", " +
                "\"Properties\": { " +
                "\"AuthMethod\": \"RPS\", " +
                "\"SiteName\": \"user.auth.xboxlive.com\", " +
                "\"RpsTicket\": \"" + rpsTicket + "\" " +
                "} " +
                "} ";

            NetworkUtils.Response res = NetworkUtils.getUrl(XBOXLIVE_AUTH_ENDPOINT, jsonString, reqHeaders);
            XboxLiveAuthData xlAuthData = new Gson().fromJson(new String(res.getData()), XboxLiveAuthData.class);
            String userToken = xlAuthData.Token;


            jsonString = "{ " +
                    "\"RelyingParty\": \"http://xboxlive.com\", " +
                    "\"TokenType\": \"JWT\", " +
                    "\"Properties\": { " +
                    "\"UserTokens\": [\"" + userToken + "\"], " +
                    "\"SandboxId\": \"RETAIL\" " +
                    "} " +
                    "}";

            res = NetworkUtils.getUrl(XSTS_AUTH_ENDPOINT, jsonString, reqHeaders);
            XboxLiveAuthData xstsAuthData = new Gson().fromJson(new String(res.getData()), XboxLiveAuthData.class);

            //finally, update parent class fields
            mToken = xstsAuthData.Token;
            mXuid = xstsAuthData.DisplayClaims.xui[0].xid;
            mUserHash = xstsAuthData.DisplayClaims.xui[0].uhs;
        }
    }

    private class ProfileGetter extends AsyncTask<Void, Void, XboxProfile> {
        private static final String TAG = "ProfileGetter";

        @Override
        protected XboxProfile doInBackground(Void... params) {
            String targetSettings = "Gamertag,Gamerscore,GameDisplayPicRaw";
            String targetUrl = String.format(PROFILE_ENDPOINT, mXuid) + "?settings=" + targetSettings;

            Map<String, String> reqHeaders = buildDefaultHeaders();
            reqHeaders.put("Authorization", "XBL3.0 x=" + mUserHash + ";" + mToken);

            try {
                NetworkUtils.Response res = NetworkUtils.getUrl(targetUrl, (String)null, reqHeaders);

                ProfileData profileData = new Gson().fromJson(new String(res.getData()), ProfileData.class);
                return extractProfile(profileData.profileUsers[0].settings);

            } catch (IOException ioe) {
                Log.e(TAG, "Error getting user profile", ioe);
                return null;
            }
        }

        @Override
        protected void onPostExecute(XboxProfile profile) {
            mListener.onProfileReceived(profile);
        }

    }

    private class AchievementsGetter extends AsyncTask<Void, Void, List<XboxAchievement>> {
        private static final String TAG = "AchievementsGetter";

        @Override
        protected List<XboxAchievement> doInBackground(Void... params) {
            String targetUrl = String.format(ACHIEVEMENTS_ENDPOINT, mXuid);
            targetUrl += "?";
            targetUrl += "orderBy=UnlockTime";

            Map<String, String> reqHeaders = buildDefaultHeaders();
            reqHeaders.put("Authorization", "XBL3.0 x=" + mUserHash + ";" + mToken);

            try {
                NetworkUtils.Response res = NetworkUtils.getUrl(targetUrl, (String) null, reqHeaders);
                //Log.d(TAG, new String(res.getData()));

                //TODO: manage pagination
                /* {
                    "achievements": [...],
                    "pagingInfo":{
                        "continuationToken":"32",
                        "totalRecords":1140
                    }
                } */

                //DEBUG
//                String resBody = new String(res.getData());
//                int maxLogSize = 500;
//                for(int i = 0; i <= resBody.length() / maxLogSize; i++) {
//                    int start = i * maxLogSize;
//                    int end = (i+1) * maxLogSize;
//                    end = end > resBody.length() ? resBody.length() : end;
//                    Log.v(TAG, resBody.substring(start, end));
//                }
                //

                List<XboxAchievement> achievements = new ArrayList<>();
                AchievementsData jsonData = new Gson().fromJson(new String(res.getData()), AchievementsData.class);
                for (AchievementData itemData: jsonData.achievements) {
                    achievements.add(extractAchievement(itemData));
                }

                achievements.sort((a1, a2) -> {
                    if (a1.getDate() == null)
                        return 1;
                    else if (a2.getDate() == null)
                        return -1;
                    else
                        return -a1.getDate().compareTo(a2.getDate());
                });

                return achievements;

            } catch (IOException ioe) {
                Log.e(TAG, "Error getting achievements", ioe);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<XboxAchievement> achievements) {
            mListener.onAchievementsReceived(achievements);
        }

    }

    //-------- static utility methods -----------

    private static String processCookie(Map<String, List<String>> headerMap) {
        String res = "";

        List<String> cookieList = headerMap.get("Set-Cookie");
        if (cookieList == null)
            return res;

        for (String rawCookie: cookieList) {
            String[] tokens = rawCookie.split(";");
            if (!"".equals(res))
                res += "; ";

            res += tokens[0];
        }

        return res;
    }

    private static Map<String, String> buildDefaultHeaders() {
        Map<String, String> reqHeaders = new HashMap<>();
        reqHeaders.put("Accept", "application/json");
        reqHeaders.put("X-Xbl-Contract-Version", "2");
        reqHeaders.put("User-Agent", "XboxReplay; XboxLiveAuth/4.0");
        reqHeaders.put("Pragma", "no-cache");
        reqHeaders.put("Cache-Control", "no-store, must-revalidate, no-cache");
        reqHeaders.put("Accept-Encoding", "gzip, deflate, compress");

        return reqHeaders;
    }

    private static XboxProfile extractProfile(SettingData[] settings) {
        Map<String,String> settingsMap = new HashMap<>();

        for (SettingData setting: settings) {
            settingsMap.put(setting.id, setting.value);
        }

        XboxProfile profile = new XboxProfile(settingsMap.get("Gamertag"));
        profile.setPicUrl(settingsMap.get("GameDisplayPicRaw"));

        String scoreStr = settingsMap.get("Gamerscore");
        if (scoreStr != null)
            profile.setScore(Integer.parseInt(scoreStr));

        return profile;
    }

    private static XboxAchievement extractAchievement(AchievementData gsonObj) {
        XboxAchievement res = new XboxAchievement();

        res.setName(gsonObj.name);
        if (gsonObj.titleAssociations != null && gsonObj.titleAssociations.length > 0)
            res.setGameTitle(gsonObj.titleAssociations[0].name);

        res.setProgressState(gsonObj.progressState);

        if (gsonObj.progression != null && gsonObj.progression.timeUnlocked != null) {
            String dateStr = gsonObj.progression.timeUnlocked;

            //"2020-12-07T19:22:32.2500000Z"
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            Calendar calendar = Calendar.getInstance();

            try {
                calendar.setTime(sdf.parse(dateStr));
                res.setDate(calendar.getTime());

            } catch (ParseException e) {
                Log.e(TAG, "Could not parse timeUnlocked: " + dateStr);
            }
        }

        if (gsonObj.mediaAssets != null && gsonObj.mediaAssets.length > 0)
            res.setIconUrl(gsonObj.mediaAssets[0].url);

        res.setDescription(gsonObj.description);

        if (gsonObj.rewards != null && gsonObj.rewards.length > 0)
            res.setScoreReward(Integer.parseInt(gsonObj.rewards[0].value));

        return res;
    }

    //-------- Gson classes -----------

    private class XboxLiveAuthData {
        private String IssueInstant;
        private String NotAfter;
        private String Token;
        private DisplayClaimsData DisplayClaims;
    }

    private class DisplayClaimsData {
        private XuiData[] xui;
    }

    private class XuiData {
        private String gtg;
        private String xid;
        private String uhs;
    }


    private class ProfileData {
        private UserProfileData[] profileUsers;
    }

    private class UserProfileData {
        private SettingData[] settings;
    }

    private class SettingData {
        private String id;
        private String value;
    }


    private class AchievementsData {
        private AchievementData[] achievements;
    }

    private class AchievementData {
        private String name;
        private TitleData[] titleAssociations;
        private String progressState;
        private ProgressionData progression;
        private MediaAssetData[] mediaAssets;
        private String description;
        private RewardData[] rewards;
    }

    private class TitleData {
        private String name;
        private String id;
    }

    private class ProgressionData {
        private String timeUnlocked;
    }

    private class MediaAssetData {
        private String name;
        private String type;
        private String url;
    }

    private class RewardData {
        private String name;
        private String type;
        private String value;
    }
}
