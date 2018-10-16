package ru.questions;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Белов Сергей
 * Список всех вопросов
 */
public class Questions {

    private static final Logger LOG = LogManager.getLogger();

    // CheckingSkills.properties
    private int     QUESTION_MAX = 10;                          // максимальное количество задаваемых вопросов
    private String  QUESTION_FILE = "questions\\Questions.json";// файл с вопросами
    private String  RESULT_PATH = "result\\";                   // путь для сохранения результатов тестирования
    private String  RESULT_FORMAT = "JSON";                     // формат файла с результатами тестирования XML или JSON
    private boolean SHOW_ANSWERS = true;                        // отображать правильные варианты ответов
    private Level   LOGGER_LEVEL = Level.WARN;                  // уровень логирования

    private String theme;                                       // текущая тема
    private int questionMax;                                    // максимальное количество задаваемых вопросов с учетом имеющихся по теме
    private int questionNum;                                    // номер текущего вопроса
    private List<String> themesList = new ArrayList<>();        // список тем
    private List<QuestionJson> questionsJsonList;               // полный список вопросов (все темы)
    private List<Question> questionsList = new ArrayList<>();   // список текущих вопросов

    private String user = System.getProperty("user.name");      // текущий пользователь
    private long startingTime = 0;                              // время начала теста

    private ReadQuestions readQuestions; // чтение вопросов из файла (XML/JSON задается в properties (QUESTION_FILE расширение))
    private SaveResult saveResult;       // запись результатов тестирования в файл (XML/JSON задается в properties (RESULT_FORMAT))


    /**
     * Текущий пользователь
     */
    public void setUser(String user) { this.user = user; }

    /**
     * Текущий пользователь
     *
     * @return
     */
    public String getUser() { return user; }

    public boolean isShowAnswers(){
        return this.SHOW_ANSWERS;
    }

    /**
     * Устанавливаем параметры, читаем вопросы из файла QUESTION_FILE
     *
     * @param QUESTION_MAX  // максимальное количество задаваемых вопросов
     * @param QUESTION_FILE // файл с вопросами
     * @param RESULT_PATH   // путь для сохранения результатов тестирования
     * @param RESULT_FORMAT // формат файла с результатами тестирования XML или JSON
     * @param SHOW_ANSWERS  // отображать правильные варианты ответов
     * @param LOGGER_LEVEL  // уровень логирования
     */
    public void readQuestions(
            int     QUESTION_MAX,
            String  QUESTION_FILE,
            String  RESULT_PATH,
            String  RESULT_FORMAT,
            boolean SHOW_ANSWERS,
            Level   LOGGER_LEVEL) {

        this.QUESTION_MAX   = QUESTION_MAX;
        this.QUESTION_FILE  = QUESTION_FILE;
        this.RESULT_PATH    = RESULT_PATH;
        this.RESULT_FORMAT  = RESULT_FORMAT;
        this.SHOW_ANSWERS   = SHOW_ANSWERS;
        this.LOGGER_LEVEL   = LOGGER_LEVEL;

        Configurator.setLevel(LOG.getName(), LOGGER_LEVEL);

        if (questionsJsonList != null) questionsJsonList.clear();
        if (themesList != null) themesList.clear();

        // тип файла с вопросами
        if (QUESTION_FILE.toUpperCase().endsWith(".XML")) {
            readQuestions = new ReadQuestionsXml();
        } else if (QUESTION_FILE.toUpperCase().endsWith(".JSON")) {
            readQuestions = new ReadQuestionsJson();
        } else{
            readQuestions = null;
        }

        if (readQuestions != null) {
            // список вопросов (все темы)
            questionsJsonList = new ArrayList<>(readQuestions.read(QUESTION_FILE, LOGGER_LEVEL));

            // список тем
            if (questionsJsonList != null && !questionsJsonList.isEmpty()) {
                themesList = new ArrayList<>(
                        questionsJsonList
                                .stream()
                                .map(QuestionJson::getTheme)
                                .sorted()
                                .distinct()
                                .collect(Collectors.toList()));

                StringBuilder themes = new StringBuilder();
                themesList
                        .stream()
                        .forEach(x -> themes.append("\r\n\t").append(x));
                LOG.debug("Имеются вопросы по следующим темам:{}", themes.toString());

//                saveQuestionsGroupByThemes(RESULT_PATH, "Cp1251"); // сохраним вопросы с правильными вариантами ответов в файлы (по темам)
            } else {
                LOG.error("Ошибка при чтении вопросов из файла {}", QUESTION_FILE);
            }
        } else{
            LOG.error("Файл с вопросами имеет недопустимый формат (возможны JSON или XML)");
        }
    }

    /**
     * Задаем текущую тему
     *      Максимально количество задаваемых вопросов
     *      (равно количеству вопросов по теме, но не более QUESTION_MAX)
     *
     * @param theme
     */
    private void setTheme(String theme) {
        this.theme = theme;
        questionMax = Math.min(getCountQuestionsInTheme(theme), QUESTION_MAX);
    }

    /**
     * Текущая тема
     *
     * @return
     */
    public String getTheme() { return theme; }

    /**
     * Максимальное количество задаваемых вопросов (с учетом имеющихся по теме)
     *
     * @return
     */
    public int getQuestionMax() { return questionMax; }

    /**
     * Количество вопросов по теме
     *
     * @param theme
     * @return
     */
    public int getCountQuestionsInTheme(String theme) {
        return (int) questionsJsonList
                .stream()
                .filter((x) -> x.getTheme().equals(theme))
                .count();
    }

    /**
     * Сисок тем
     *
     * @return
     */
    public List<String> getThemesList() { return themesList; }

    /**
     * Список текущих вопросов
     *
     * @return
     */
    public List<Question> getQuestionsList() { return questionsList; }


    /**
     * Вопрос текущий
     *
     * @return
     */
    public Question get() { return questionsList.get(questionNum); }

    /**
     * Вопрос по номеру
     *
     * @param q
     * @return
     */
    public Question get(int q) { return questionsList.get(q); }

    /**
     * Текущий номер вопроса
     *
     * @return
     */
    public int getQuestionNum() {
        return questionNum;
    }

    /**
     * Переход к предыдущему вопросу
     */
    public void prevQuestion() {
        if (questionNum > 0) questionNum--;
    }

    /**
     * Переход к следующему вопросу
     */
    public void nextQuestion() {
        if (questionNum < questionMax - 1) questionNum++;
    }

    /**
     * Первый вопрос в списке?
     *
     * @return
     */
    public boolean isFirstQuestion() {
        return questionNum == 0;
    }

    /**
     * Последний вопрос в списке ?
     *
     * @return
     */
    public boolean isLastQuestion() {
        return questionNum == (questionMax - 1);
    }

    /**
     * Корректность ответов (количество ответов с ошибками)
     *
     * @return
     */
    public int getCountNotCorrectAnswers() {
        return (int) questionsList
                    .stream()
                    .filter(q -> !q.isAnswerCorrect())
                    .count();
    }

    /**
     * Сохраним вопросы с правильными ответами (сгруппировав по темам)
     */
    public void saveQuestionsGroupByThemes(final String path, final String charset) {
//        StandardCharsets.US_ASCII.name();
        StringBuilder sbQuestions = new StringBuilder();

        themesList // список тем
                .stream()
                .sorted()
                .forEach(t -> {

                    try (
                        BufferedWriter bw = new BufferedWriter(
                                                new OutputStreamWriter(
                                                    new FileOutputStream(path +  File.separator + t.replace("\\", "_") + ".txt", false),
                                                    charset != null & charset.length() > 0 ? charset : "Cp1251"));
                    ) {
                        sbQuestions.setLength(0);
                        sbQuestions
                                .append("##################################################\r\n")
                                .append(t)
                                .append("\r\n##################################################\r\n");

                        questionsJsonList // список вопросов по теме
                                .stream()
                                .filter(q -> q.getTheme().equals(t))
                                .forEach(q -> {
                                    sbQuestions
                                            .append("==================================================\r\n")
                                            .append(q.getQuestion())
                                            .append("\r\n");
                                    for (String a : q.getAnswersTrue()) {
                                        sbQuestions
                                                .append("..................................................\r\n")
                                                .append(a)
                                                .append("\r\n");
                                    }
                                });
                        bw.write(sbQuestions.toString());
                        bw.flush();
                    } catch (IOException e) {
                        LOG.error(e);
                    }

                });
    }

    /**
     * Начинаем тестирование
     */
    public void start(String theme) {
        if (theme != null && !theme.isEmpty() && startingTime == 0) {
            setTheme(theme);
            questionNum = 0;
            questionsList.clear();
            List<Answer> answersList = new ArrayList<>();

            // выберем questionMax случайных вопросов по текущей теме
            Random random = new Random();
            IntStream
                    .generate(() -> random.nextInt(questionsJsonList.size()))
                    .distinct()
                    .filter(n -> questionsJsonList.get(n).getTheme().equals(theme))
                    .limit(questionMax)
                    .forEach(q -> {
                        for (String a : questionsJsonList.get(q).getAnswersTrue()) {
                            answersList.add(new Answer(a, true, false));
                        }
                        for (String a : questionsJsonList.get(q).getAnswersFalse()) {
                            answersList.add(new Answer(a, false, false));
                        }
                        questionsList.add(
                                new Question(
                                        questionsJsonList.get(q).getAuthor(),
                                        questionsJsonList.get(q).getTheme(),
                                        questionsJsonList.get(q).getQuestion(),
                                        answersList));

                        answersList.clear();
                    });

            startingTime = System.currentTimeMillis();  // время старта
            LOG.info("Пользователь {} начал тестирование по теме {}",
                    user,
                    theme);
        }
    }

    /**
     * Заканчиваем тестирование
     * Сохраним результат тестирования
     *
     * @return
     */
    public String stop(boolean saveResult){
        if (saveResult) {
            return  stop();
        } else {
            startingTime = 0;
            return "";
        }
    }

    public String stop() {
        StringBuilder message = new StringBuilder();
        if (startingTime > 0) {
            StringBuilder resultTXT = new StringBuilder();
            int countError = getCountNotCorrectAnswers();
            int correctAnswer = getQuestionMax() - countError;
            int correctAnswerProc = correctAnswer * 100 / getQuestionMax();

            if (countError == 0) { // ошибок нет
                message.append("<html>Отлично!");
                resultTXT.append("Отлично! ");

            } else { // ошибки есть
                message.append("<html>Имеются ошибки!<br>");
                if (this.SHOW_ANSWERS) {
                    message .append("Анализ: ")
                            .append("<font color=\"#10aa10\">правильный выбор; </font>")
                            .append("<font color=\"#ffb000\">нужно было выбрать; </font>")
                            .append("<font color=\"#ff10010\">неправильный выбор</font>.");
                }
                resultTXT.append("Имеются ошибки! ");
            }
            message.append("<br/>Дан верный ответ на ")
                    .append(correctAnswer)
                    .append(" из ")
                    .append(getQuestionMax())
                    .append(" (")
                    .append(correctAnswerProc)
                    .append("%)<br/></html>");

            resultTXT.append("Дан верный ответ на ")
                    .append(correctAnswer)
                    .append(" из ")
                    .append(getQuestionMax())
                    .append(" (")
                    .append(correctAnswerProc)
                    .append("%)");

            // результат тестирования в лог-файл
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            StringBuilder wrongAnswers = new StringBuilder();
            List<String> wrongAnswersList = new ArrayList<>();
            questionsList
                    .stream()
                    .filter(q -> !q.isAnswerCorrect())
                    .map(q -> q.getQuestion())
                    .forEach(q -> {
                        wrongAnswersList.add(q);
                        wrongAnswers.append("\r\n\t").append(q);
                    });

            LOG.info("Пользователь {} завершил тестирование по теме {}; Результат: {}{}",
                    user,
                    theme,
                    resultTXT,
                    wrongAnswers);

            // сохраняем результат тестирования
            String fileResultName = "";
            switch (RESULT_FORMAT.toUpperCase()) {
                case "XML":
                    fileResultName = "result.xml";
                    saveResult = new SaveResultXml();
                    break;

                case "JSON":
                    fileResultName = "result.json";
                    saveResult = new SaveResultJson();
                    break;

                default:
                    saveResult = null;
            }

            if (saveResult != null) {
                saveResult.save(
                        RESULT_PATH,
                        fileResultName,
                        user,
                        startingTime,
                        System.currentTimeMillis(),
                        getTheme(),
                        resultTXT.toString(),
                        wrongAnswersList);
            } else {
                LOG.warn("Указан недопустимый формат для сохранения результатов тестирования (возможны JSON или XML); Результат тестирования не сохранен!");
            }

            startingTime = 0;
        }
        return message.toString();
    }

    public boolean isStarted() { return startingTime > 0 ? true : false; }
}