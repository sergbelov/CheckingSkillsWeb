package ru.questions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Белов Сергей
 * Чтение вопросов из Json-файла
 */
public class ReadQuestionsJson implements ReadQuestions {

    private static final Logger LOG = LoggerFactory.getLogger(ReadQuestionsJson.class);

    private List<QuestionJson> questionsJsonList = new ArrayList<>(); // полный список вопросов (все темы)

    /**
     * Чтение вопросов из файла
     *
     * @param fileJson
     * @param level
     * @return
     */
    @Override
    public List<QuestionJson> read(String fileJson, Level level) {

        Configurator.setLevel(LOG.getName(), level);

        questionsJsonList.clear();

        File file = new File(fileJson);
        if (!file.exists()) { // файл с вопросами не найден
            LOG.error("Не найден файл с вопросами {}", fileJson);
/*
            JOptionPane.showMessageDialog(
                    null,
                    "<html>Не найден Json-файл с вопросами<br/>" +
                            "Укажите данный файл...</html>");

            JFileChooser dialog = new JFileChooser();
            dialog.showOpenDialog(null);
            file = dialog.getSelectedFile();
            if (file != null) {
                file = new File(file.getAbsolutePath());
            }
*/
        } else {
            LOG.debug("Чтение вопросов из файла {}", fileJson);

            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            try (
//                    JsonReader reader = new JsonReader(new FileReader(file.toString()));
                    JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            ) {
                questionsJsonList = gson.fromJson(reader, new TypeToken<List<QuestionJson>>() {
                }.getType());

            } catch (FileNotFoundException e) {
                LOG.error("", e);
            } catch (UnsupportedEncodingException e) {
                LOG.error("", e);
            } catch (IOException e) {
                LOG.error("", e);
            }
        }
        return questionsJsonList;
    }
}
