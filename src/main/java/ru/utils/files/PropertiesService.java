package ru.utils.files;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Map.Entry.comparingByKey;

/**
 * Created by Сергей on 19.05.2018.
 */
public class PropertiesService {
    private static final Logger LOG = LogManager.getLogger();
    private boolean addKey;
    private Map<String, String> propertyMap;

    public PropertiesService() {
        addKey = true; // список параметров из файла
        propertyMap = new LinkedHashMap<String, String>();
    }

    public PropertiesService(Map<String, String> propertyMap) {
        addKey = false; // список параметров задан
        this.propertyMap = propertyMap;
    }

    public PropertiesService(String fileName) {
        addKey = true; // список параметров из файла
        propertyMap = new LinkedHashMap<String, String>();
        readProperties(fileName);
    }

    public PropertiesService(String fileName, Map<String, String> propertyMap) {
        addKey = false; // список параметров задан
        this.propertyMap = propertyMap;
        readProperties(fileName);
    }


    public void readProperties(String fileName, Level level) {
        Configurator.setLevel(LOG.getName(), level);
        readProperties(fileName);
    }

    public void readProperties(String fileName) {
        StringBuilder report = new StringBuilder();
        report
                .append("Параметры из файла ")
                .append(fileName)
                .append(":");

        boolean fileExists = false;
        File file = new File(fileName);
        if (file.exists()) { // найден файл с параметрами
            StringBuilder reportTrace = new StringBuilder();
            reportTrace
                    .append("Параметры в файле ")
                    .append(fileName)
                    .append(":");

            try (InputStream is = new FileInputStream(file)) {
                Properties pr = new Properties();
                pr.load(is);

                for (Map.Entry<Object, Object> entry : pr.entrySet()) {
                    reportTrace
                            .append("\r\n\t")
                            .append(entry.getKey().toString())
                            .append(": ")
                            .append(entry.getValue().toString());

                    if (addKey || propertyMap.get(entry.getKey()) != null) {
                        propertyMap.put(entry.getKey().toString(), entry.getValue().toString());
                    }
                }
                LOG.trace(reportTrace);
//                for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
//                    propertyMap.put(entry.getKey(), pr.getProperty(entry.getKey(), entry.getValue()));
//                }
                fileExists = true;
            } catch (IOException e) {
                LOG.error(e);
            }
        } else {
            report.append("\r\n\tФайл не найден, параметры по умолчанию:");
        }

        // параметры со значениями
        propertyMap
                .entrySet()
                .stream()
//                .sorted(comparingByKey())
                .forEach(x -> {
                    report
                            .append("\r\n\t")
                            .append(x.getKey())
                            .append(": ")
                            .append(x.getValue());
                });

        if (fileExists) {
            LOG.info(report);
        } else {
            LOG.warn(report);
        }
    }

    public String getString(String propertyName) {
        return propertyMap.get(propertyName);
    }

    public int getInt(String propertyName) {
        return Integer.parseInt(propertyMap.get(propertyName));
    }

    public long getLong(String propertyName) {
        return Long.parseLong(propertyMap.get(propertyName));
    }

    public double getDouble(String propertyName) {
        return Double.parseDouble(propertyMap.get(propertyName));
    }

    public float getFloat(String propertyName) {
        return Float.parseFloat(propertyMap.get(propertyName));
    }

    public boolean getBoolean(String propertyName) {
        return Boolean.parseBoolean(propertyMap.get(propertyName));
    }

    public Date getDate(String propertyName) {
        return getDate(propertyName, "dd/MM/yyyy");
    }

    public Date getDate(String propertyName, String dateFormat) {
        Date date = null;
        try {
            DateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
            date = simpleDateFormat.parse(propertyMap.get(propertyName));
        } catch (ParseException e) {
            LOG.error(e);
        }
        return date;
    }

    public Level getLevel(String propertyName) {
        return Level.getLevel(propertyMap.get(propertyName));
    }

    public String[] getStringList(String propertyName) {
        return propertyMap.get(propertyName).split(",");
    }

    public int[] getIntList(String propertyName) {
        return Arrays
                .stream(propertyMap.get(propertyName).split(","))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    public JSONObject getJSONObject(String propertyName) {
        JSONObject jsonObject = null;
        String value = propertyMap.get(propertyName);
        if (value != null && value.startsWith("{")) {
            try {
                jsonObject = new JSONObject(value);
            } catch (JSONException e) {
                LOG.error(e);
            }
        }
        return jsonObject;
    }

    public JSONArray getJSONArray(String propertyName) {
        JSONArray jsonArray = null;
        String value = propertyMap.get(propertyName);
        if (value != null && value.startsWith("[")) {
            try {
                jsonArray = new JSONArray(value);
            } catch (JSONException e) {
                LOG.error(e);
            }
        }
        return jsonArray;
    }

    public <T> List<T> getJsonList(String propertyName, TypeToken typeToken) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(propertyMap.get(propertyName), typeToken.getType());
    }
/*
    public List<?> getJsonList(String propertyName) {
        Gson gson = new GsonBuilder().create();
        String jsonString = propertyMap.get(propertyName);
        return gson.fromJson(jsonString, new TypeToken<List<?>>(){}.getType());
    }
*/

}
