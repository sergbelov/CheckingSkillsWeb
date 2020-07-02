package ru.questions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Белов Сергей
 * Чтение вопросов из XML-файла
 */
public class ReadQuestionsXml implements ReadQuestions{

    private static final Logger LOG = LoggerFactory.getLogger(ReadQuestionsXml.class);

    private String author;                                              // автор
    private String theme;                                               // тема
    private String question;                                            // вопрос
    private List<QuestionJson> questionsJsonList = new ArrayList<>();   // полный список вопросов (все темы)

    // для преобразования XML в Json
    private List<String> answersTrue = new ArrayList<>();
    private List<String> answersFalse = new ArrayList<>();

    /**
     * Чтение вопросов из файла
     *
     * @param fileXML
     * @param level
     * @return
     */
    @Override
    public List<QuestionJson> read(String fileXML, Level level) {

        Configurator.setLevel(LOG.getName(), level);

        questionsJsonList.clear();

        File file = new File(fileXML);
        if (!file.exists()) { // файл с вопросами не найден
            LOG.error("Не найден файл с вопросами {}", fileXML);
/*
            JOptionPane.showMessageDialog(
                    null,
                    "<html>Не найден XML-файл с вопросами<br/>" +
                            "Укажите данный файл...</html>");

            JFileChooser dialog = new JFileChooser();
            dialog.showOpenDialog(null);
            file = dialog.getSelectedFile();
            if (file != null) {
                file = new File(file.getAbsolutePath());
            }
*/
        } else {
            LOG.debug("Чтение вопросов из файла {}", fileXML);

            try {
                // Строим объектную модель исходного XML файла
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(file);

                // Выполнять нормализацию не обязательно, но рекомендуется
                doc.getDocumentElement().normalize();

                // Получаем все узлы с именем "QuestionBlock"
                NodeList nodesList = doc.getElementsByTagName("QuestionBlock");
                NodeList nodesList2;

                for (int i = 0; i < nodesList.getLength(); i++) {
                    Node node = nodesList.item(i);
                    if (Node.ELEMENT_NODE == node.getNodeType()) {
                        Element element = (Element) node;
                        theme = formatText(element.getElementsByTagName("theme").item(0).getTextContent());
                        question = formatText(element.getElementsByTagName("question").item(0).getTextContent());
                        if (element.getElementsByTagName("author").item(0) != null) {
                            author = formatText(element.getElementsByTagName("author").item(0).getTextContent());
                        } else {
                            author = "";
                        }
                        nodesList2 = element.getElementsByTagName("at");
                        for (int x = 0; x < nodesList2.getLength(); x++) {
                            answersTrue.add(formatText(nodesList2.item(x).getTextContent()));
                        }

                        nodesList2 = element.getElementsByTagName("af");
                        for (int x = 0; x < nodesList2.getLength(); x++) {
                            answersFalse.add(formatText(nodesList2.item(x).getTextContent()));
                        }

                        questionsJsonList.add(
                                new QuestionJson(
                                        author,
                                        theme,
                                        question,
                                        answersTrue,
                                        answersFalse));

                        answersTrue.clear();
                        answersFalse.clear();
                    }
                }

            } catch (ParserConfigurationException | SAXException | IOException e) {
                LOG.error("", e);
            }

            // место нахождение файла с вопросами
            File parentFolder = new File(file.getAbsolutePath()
                    .substring(0, file.getAbsolutePath().lastIndexOf(
                            File.separator)));

            // запишем массив с вопросами в Json-файл
//            Gson gson = new Gson();
            Gson gson = new GsonBuilder() // с форматированием
                    .setPrettyPrinting()
                    .create();

            String json = gson.toJson(questionsJsonList);
            try (
//                FileWriter fw = new FileWriter("Questions.json", false);
                    BufferedWriter fw = new BufferedWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(parentFolder.getAbsolutePath() + "/Questions.json", false),
                                    "UTF-8"));
            ) {
//            fw.write("{\"questions\":"+ json +"}");
                fw.write(json);
                fw.flush();
            } catch (IOException e) {
                LOG.error("", e);
            }
        }

        return questionsJsonList;
    }


    /**
     * Уберем из строки: переводы строк, табуляции, двойные пробелы
     *
     * @return
     */
    private String formatText(String text) {
        if (text != null) {
            return text
                    .replaceAll("\t", " ")
                    .replaceAll("\r", "")
                    .replaceAll("\n", " ")
                    .replaceAll("[\\s]{2,}", " ")
                    .trim();
        } else {
            return text;
        }
    }

}
