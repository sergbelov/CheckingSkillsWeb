package ru.questions;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Белов Сергей
 * Вопрос с ответами (Json)
 */
public class QuestionJson {

    private String author;              // автор
    private String theme;               // тема
    private String question;            // вопрос
    private List<String> answersTrue;   // список верных вариантов ответа
    private List<String> answersFalse;  // список ложных вариантов ответа

    public QuestionJson(String author,
                        String theme,
                        String question,
                        List<String> answersTrue,
                        List<String> answersFalse) {

        this.author = author;
        this.theme = theme;
        this.question = question;
        this.answersTrue = new ArrayList<>(answersTrue);
        this.answersFalse = new ArrayList<>(answersFalse);
    }

    public String getAuthor() {
        return author;
    }

    public String getTheme() {
        return theme;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getAnswersTrue() {
        return answersTrue;
    }

    public List<String> getAnswersFalse() {
        return answersFalse;
    }
}
