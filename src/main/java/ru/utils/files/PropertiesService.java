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

import java.io.*;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Map.Entry.comparingByKey;

/**
 * Created by Сергей
 * Сервис для работы с файлом properties
 */
public class PropertiesService {
    private static final Logger LOG = LogManager.getLogger();
    private String fileName;                    // properties - файл
    private boolean addKey;                     // добавлять или нет новый параметр из файла
    private Map<String, String> propertyMap;    // список параметров со значениями

    /**
     * Инициализация без параметров
     * все параметры берутся из файла
     * для получения параметров нужно выполнить readProperties
     */
    public PropertiesService() {
        this.addKey = true; // список параметров из файла
        this.propertyMap = new LinkedHashMap<String, String>();
    }

    /**
     * Инициализация с параметрами
     * из файла будут браться только переданные параметры
     * для получения параметров нужно выполнить readProperties
     * @param propertyMap
     */
    public PropertiesService(Map<String, String> propertyMap) {
        this.addKey = false; // список параметров задан
        this.propertyMap = propertyMap;
    }

    /**
     * Инициализация с именем файла
     * все параметры берутся из файла
     * получение параметров происходит при инициализации
     * @param fileName
     */
    public PropertiesService(String fileName) {
        this.addKey = true; // список параметров из файла
        this.propertyMap = new LinkedHashMap<String, String>();
        readProperties(fileName);
    }

    /**
     * Инициализация с параметрами и именем файла
     * из файла будут браться только переданные параметры
     * получение параметров происходит при инициализации
     * @param fileName
     * @param propertyMap
     */
    public PropertiesService(String fileName, Map<String, String> propertyMap) {
//        this.addKey = true;
        this.addKey = false; // список параметров задан
        this.propertyMap = propertyMap;
        readProperties(fileName);
    }


    /**
     * Устанавливается уровень логирования
     * @param level
     */
    public void setLevel(Level level){
        Configurator.setLevel(LOG.getName(), level);
    }

    /**
     * Получение параметров из файла
     * дополнительно устанавливается уровень логирования
     * @param fileName
     * @param level
     */
    public void readProperties(String fileName, Level level) {
        setLevel(level);
        readProperties(fileName);
    }

    /**
     * Получение параметров из файла
     * @param fileName
     */
    public void readProperties(String fileName) {
        this.fileName = fileName;
        StringBuilder report = new StringBuilder();
        report
                .append("Параметры из файла ")
                .append(fileName)
                .append(":");

        boolean fileExists = false;
        InputStream inputStream = null;
        File file = new File(fileName);
        try {
            inputStream = new FileInputStream(file);
            fileExists = true;
        } catch (Exception e) {
            Exception exception = e;
            try {
                inputStream = ClassLoader.getSystemResourceAsStream(fileName);
                fileExists = true;
            } catch (Exception e2) {
                LOG.warn("Ошибка при чтении параметров из файла: {}\n" +
                        "Прямая ссылка:\n{}\n" +
                        "Из ресурсов:\n", fileName, exception, e2);
            }
        }

        if (fileExists && inputStream != null){
            StringBuilder reportTrace = new StringBuilder();
            reportTrace
                    .append("Параметры в файле ")
                    .append(fileName)
                    .append(":");

            Properties properties = new Properties();
            try {
                properties.load(inputStream);

                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    reportTrace
                            .append("\r\n\t")
                            .append(entry.getKey().toString())
                            .append(": ")
                            .append(entry.getValue().toString());

                    if (addKey || propertyMap.get(entry.getKey()) != null) {
                        propertyMap.put(
                                entry.getKey().toString(),
                                entry.getValue().toString());
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
            report.append("\r\n\tФайл не найден, используем параметры по умолчанию:");
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


    /**
     * Сохраняние параметра в файл
     * !!! Внимание форматирование и комментарии в файле пропадут
     * @param key
     * @param value
     * @return
     */
    public boolean setProperty(String key, String value) {
        boolean r = false;
        Properties properties = new Properties();

        try (InputStream inputStream = new FileInputStream(fileName)) {
            properties.load(inputStream);
            r = true;
        } catch (IOException e) {
            LOG.error(e);
        }

        if (r) {
            try (OutputStream outputStream = new FileOutputStream(fileName)) {
                properties.setProperty(key, value);
                properties.store(outputStream, null);
            } catch (IOException e) {
                r = false;
                LOG.error(e);
            }
        }
        return r;
    }

    /**
     * Наменование текущего файла с параметрами
     * @return
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Проверка наличия параметра
     * @param key
     * @return
     */
    public boolean containsKey(String key){
        return propertyMap.containsKey(key);
    }

    /**
     * Значение паметра в формате String
     * @param key
     * @return
     */
    public String get(String key) {
        if (containsKey(key)) {
            return propertyMap.get(key);
        } else {
            LOG.warn("Параметр <{}> не найден", key);
            return null;
        }
    }

    /**
     * Значение параметра в формате String
     * @param key
     * @return
     */
    public String getString(String key) {
        try {
            return get(key);
        } catch (Exception e) {
            LOG.error("Ошибка при получении параметра {}", e);
            return null;
        }
    }

    /**
     * Значение зашифрованного параметра
     * @param key
     * @return
     */
    public String getStringDecode(String key) {
        try {
            return getStringDecrypt(get(key));
        } catch (Exception e) {
            LOG.error("Ошибка при получении параметра {}", e);
            return null;
        }
    }

    /**
     * Значение параметра в формате int
     * @param key
     * @return
     */
    public int getInt(String key) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException e) {
            LOG.error("Не верный формат данных {}",
                    get(key),
                    e);
            return 0;
        }
    }

    /**
     * Значение параметра в формате long
     * @param key
     * @return
     */
    public long getLong(String key) {
        try {
            return Long.parseLong(get(key));
        } catch (NumberFormatException e) {
            LOG.error("Не верный формат данных {}",
                    get(key),
                    e);
            return 0L;
        }
    }

    /**
     * Значение параметра в формате double
     * @param key
     * @return
     */
    public double getDouble(String key) {
        try {
            return Double.parseDouble(get(key));
        } catch (NumberFormatException e) {
            LOG.error("Не верный формат данных {}",
                    get(key),
                    e);
            return 0;
        }
    }

    /**
     * Значение параметра в формате float
     * @param key
     * @return
     */
    public float getFloat(String key) {
        try {
            return Float.parseFloat(get(key));
        } catch (NumberFormatException e) {
            LOG.error("Не верный формат данных {}",
                    get(key),
                    e);
            return 0;
        }
    }

    /**
     * Значение параметра в формате boolean
     * @param key
     * @return
     */
    public boolean getBoolean(String key) {
        try {
            return Boolean.parseBoolean(get(key));
        } catch (Exception e) {
            LOG.error("Ошибка при получении параметра {}", e);
            return false;
        }
    }

    /**
     * Значение параметра в формате Date
     * формат dd/MM/yyy
     * @param key
     * @return
     */
    public Date getDate(String key) {
        return getDate(key, "dd/MM/yyyy");
    }

    /**
     * Значение параметра в формате Date
     * формат задается параметром dateFormat
     * @param key
     * @param dateFormat
     * @return
     */
    public Date getDate(String key, String dateFormat) {
        Date date = null;
        try {
            DateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
            date = simpleDateFormat.parse(get(key));
        } catch (ParseException e) {
            LOG.error("Не верный формат данных {}",
                    get(key),
                    e);
        }
        return date;
    }

    /**
     * Значение параметра в формате Level
     * @param key
     * @return
     */
    public Level getLevel(String key) {
        try {
            return Level.getLevel(get(key));
        } catch (Exception e) {
            LOG.error("Ошибка при получении параметра {}", e);
            return null;
        }
    }

    /**
     * Значение параметра в формате String[]
     * @param key
     * @return
     */
    public String[] getStringList(String key) {
        return get(key).split(",");
    }

    /**
     * Значение параметра в формате int[]
     * @param key
     * @return
     */
    public int[] getIntList(String key) {
        try {
            return Arrays
                    .stream(get(key).split(","))
                    .mapToInt(Integer::parseInt)
                    .toArray();
        } catch (Exception e) {
            LOG.error("Ошибка при получении параметра {}", e);
            return null;
        }
    }

    /**
     * Значение параметра в формате byte[]
     * radix = 16
     * @param key
     * @return
     */
    public byte[] getByteArray(String key) {
        return getByteArray(key, 16);
    }

    /**
     * Значение параметра в формате byte[]
     * radix задается параметром
     * @param key
     * @param radix
     * @return
     */
    public byte[] getByteArray(String key, int radix) {
        try {
            return new BigInteger(get(key), radix).toByteArray();
        } catch (Exception e) {
            LOG.error("Ошибка при получении параметра {}", e);
            return null;
        }
    }

    /**
     * Значение параметра в формате JSONObject
     * @param key
     * @return
     */
    public JSONObject getJSONObject(String key) {
        JSONObject jsonObject = null;
        String value = get(key);
        if (value != null && value.startsWith("{")) {
            try {
                jsonObject = new JSONObject(value);
            } catch (JSONException e) {
                LOG.error(e);
            }
        }
        return jsonObject;
    }

    /**
     * Значение параметра в формате JSONArray
     * @param key
     * @return
     */
    public JSONArray getJSONArray(String key) {
        JSONArray jsonArray = null;
        String value = get(key);
        if (value != null && value.startsWith("[")) {
            try {
                jsonArray = new JSONArray(value);
            } catch (JSONException e) {
                LOG.error(e);
            }
        }
        return jsonArray;
    }

    /**
     * Значение параметра в формате List<T>
     * @param key
     * @param typeToken
     * @param <T>
     * @return
     */
    public <T> List<T> getJsonList(String key, TypeToken typeToken) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(get(key), typeToken.getType());
    }
/*
    public List<?> getJsonList(String key) {
        Gson gson = new GsonBuilder().create();
        String jsonString = get(key);
        return gson.fromJson(jsonString, new TypeToken<List<?>>(){}.getType());
    }
*/

    /**
     * Шифрование строки
     * @param data
     * @return
     */
    private String getStringEncrypt(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes());
    }

    /**
     * Дешифрование строки
     * @param data
     * @return
     */
    private String getStringDecrypt(String data) {
        try {
            return new String((Base64.getDecoder().decode(data)));
        } catch (Exception e) {
            return "";
        }
    }

}
