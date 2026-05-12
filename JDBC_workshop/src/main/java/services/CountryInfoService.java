package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CountryInfoService {

    private static final String BASE_URL = "https://restcountries.com/v3.1/capital/";
    private static final String NAME_URL  = "https://restcountries.com/v3.1/name/";

    public static class CountryInfo {
        public String commonName;
        public String officialName;
        public String capital;
        public String region;
        public String subregion;
        public String flagEmoji;
        public String flagPngUrl;
        public String currency;
        public String currencySymbol;
        public String languages;
        public long population;
        public String timezone;
        public String callingCode;
    }

    /**
     * Tries to find the country by treating the city as a capital first,
     * then falls back to a name search.
     */
    public static CountryInfo fetchByCity(String city) throws Exception {

        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String json = get(NAME_URL + encoded);
        JSONArray arr = new JSONArray(json);
        if (arr.length() == 0) throw new Exception("Country not found for: " + city);
        return parse(arr.getJSONObject(0));
    }

    private static CountryInfo parse(JSONObject obj) {
        CountryInfo info = new CountryInfo();

        // Names
        JSONObject name = obj.optJSONObject("name");
        if (name != null) {
            info.commonName  = name.optString("common", "N/A");
            info.officialName = name.optString("official", "N/A");
        }

        // Capital
        JSONArray caps = obj.optJSONArray("capital");
        info.capital = (caps != null && caps.length() > 0) ? caps.getString(0) : "N/A";

        // Region
        info.region    = obj.optString("region", "N/A");
        info.subregion = obj.optString("subregion", "N/A");

        // Flag
        JSONObject flags = obj.optJSONObject("flags");
        if (flags != null) {
            info.flagPngUrl  = flags.optString("png", "");
        }
        info.flagEmoji = obj.optString("flag", "🏳");

        // Currency
        JSONObject currencies = obj.optJSONObject("currencies");
        if (currencies != null && !currencies.isEmpty()) {
            String code = currencies.keys().next();
            JSONObject cur = currencies.optJSONObject(code);
            if (cur != null) {
                info.currency       = cur.optString("name", code);
                info.currencySymbol = cur.optString("symbol", code);
            }
        } else {
            info.currency = "N/A";
            info.currencySymbol = "";
        }

        // Languages
        JSONObject langs = obj.optJSONObject("languages");
        if (langs != null) {
            StringBuilder sb = new StringBuilder();
            for (String key : langs.keySet()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(langs.optString(key));
            }
            info.languages = sb.toString();
        } else {
            info.languages = "N/A";
        }

        // Population
        info.population = obj.optLong("population", 0);

        // Timezones
        JSONArray tz = obj.optJSONArray("timezones");
        info.timezone = (tz != null && tz.length() > 0) ? tz.getString(0) : "N/A";

        // Calling code (idd)
        JSONObject idd = obj.optJSONObject("idd");
        if (idd != null) {
            String root    = idd.optString("root", "");
            JSONArray sfxs = idd.optJSONArray("suffixes");
            String sfx     = (sfxs != null && sfxs.length() == 1) ? sfxs.getString(0) : "";
            info.callingCode = root + sfx;
        } else {
            info.callingCode = "N/A";
        }

        return info;
    }

    private static String get(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setRequestProperty("Accept", "application/json");

        int status = con.getResponseCode();
        if (status != 200) throw new Exception("HTTP " + status + " for " + urlStr);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}