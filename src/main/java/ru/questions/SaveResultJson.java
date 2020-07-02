package ru.questions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Белов Сергей
 * Сохранение результата тестирования в XML-файл
 */
public class SaveResultJson implements SaveResult {

    private static final Logger LOG = LoggerFactory.getLogger(SaveResultJson.class);

    /**
     * Сохранение результата тестирования в XML-файл
     *
     * @param path         - путь для сохранения
     * @param fileName     - имя файла
     * @param user         - пользователь
     * @param startingTime - время начала теста
     * @param stoppingTime  - время окончания теста
     * @param theme        - тема
     * @param resultTXT    - результат тестирования
     * @param wrongAnswersList - список вопросов на которые дан неверный ответ
     */
    public void save(String path,
                     String fileName,
                     String user,
                     long startingTime,
                     long stoppingTime,
                     String theme,
                     String resultTXT,
                     List<String> wrongAnswersList) {

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        ResultTest resultTest = new ResultTest(
                user,
                dateFormat.format(startingTime),
                dateFormat.format(stoppingTime),
                theme,
                resultTXT,
                wrongAnswersList);

        List<ResultTest> resultTestList = new ArrayList<>();

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        File file = new File(path);
        file.mkdirs();
        file = new File(path, fileName);
        LOG.debug("Сохранение результата тестирования в файл {}", file);

        if (file.exists()) {
            try(
//                JsonReader reader = new JsonReader(new FileReader(file.toString()));
                JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
            ) {
                resultTestList = gson.fromJson(reader, new TypeToken<List<ResultTest>>() {}.getType());
            } catch (FileNotFoundException e ) {
                LOG.error("", e);
            } catch (UnsupportedEncodingException e) {
                LOG.error("", e);
            } catch (IOException e) {
                LOG.error("", e);
            }
        }
        resultTestList.add(resultTest);
        String json = gson.toJson(resultTestList);
        try(
            BufferedWriter fw = new BufferedWriter(
                                    new OutputStreamWriter(
                                        new FileOutputStream(file.toString(), false),
                                        "UTF-8"));
        ) {
            fw.write(json );
            fw.flush();
        } catch (IOException e) {
            LOG.error("", e);
        };

    }

    // класс с результатами тестирования
    class ResultTest {
        private String user;
        private String startingTime;
        private String stoppingTime;
        private String theme;
        private String result;
        private List<String> wrongAnswersList;

        public ResultTest(String user,
                          String startingTime,
                          String stoppingTime,
                          String theme,
                          String result,
                          List<String> wrongAnswersList) {

            this.user = user;
            this.startingTime = startingTime;
            this.stoppingTime = stoppingTime;
            this.theme = theme;
            this.result = result;
            this.wrongAnswersList = wrongAnswersList;
        }

        public String getUser() {
            return user;
        }

        public String getStartingTime() {
            return startingTime;
        }

        public String getStoppingTime() {
            return stoppingTime;
        }

        public String getTheme() {
            return theme;
        }

        public String getResult() {
            return result;
        }

        public List<String> getWrongAnswersList() { return wrongAnswersList; }
    }
}

