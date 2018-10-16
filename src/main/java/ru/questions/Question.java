package ru.questions;

import java.util.*;

/**
 * @author Белов Сергей
 *         Вопрос, включает в себя:
 *         String author            автор;
 *         String theme             тема;
 *         String question          вопрос;
 *         Integer type             тип (1-RadioButton; 2-CheckBox);
 *         List<Answer> answersList варианты ответов
 */
public class Question implements Comparable<Question> {

    public enum TypeOfAnswer {radio, checkbox}

    ;

    private TypeOfAnswer type;          // тип ответа RadioButton; CheckBox
    private String author;              // автор
    private String theme;               // тема
    private String question;            // вопрос
    private List<Answer> answersList;   // список вариантов ответа


    public Question(String authorParam,
                    String themeParam,
                    String questionParam,
                    List<Answer> answersListParam) {

        this.author = authorParam;
        this.theme = themeParam;
        this.question = questionParam;
        this.answersList = new ArrayList<>(answersListParam);

        // если правильных ответов более одного - CheckBox иначе RadioButton
        this.type = (int) answersList
                .stream()
                .filter((x) -> x.isCorrect()).count() > 1
                ? TypeOfAnswer.checkbox : TypeOfAnswer.radio;

        // перемешаем варианты ответов
        this.answersListShuffle();
    }

    public TypeOfAnswer getType() {
        return type;
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

    public int getCountAnswers() {
        return answersList.size();
    }

    public Answer getAnswer(int i) {
        return answersList.get(i);
    }

    public List<Answer> getAnswersList() {
        return answersList;
    }


    /**
     * Ответ правильный
     *
     * @return
     */
    public boolean isAnswerCorrect() {
        return answersList
                .stream()
                .filter((x) -> x.isCorrect() != x.isSelected())
                .count() == 0;
    }

    /**
     * Сбрасываем текущий выбор
     */
    public void clearAnswersSelected() {
        answersList
                .stream()
                .filter((x) -> x.isSelected())
                .forEach((x) -> x.setSelected(false));
/*
        for (int i = 0; i < getCountAnswers(); i++) {
            getAnswer(i).setSelected(false);
        }
*/
    }

    /**
     * Перемешаем варианты ответов
     */
    public void answersListShuffle() {
        Collections.shuffle(answersList);
    }


    @Override
    public int compareTo(Question o) {
        if (this == o) return 0;

        int compare = this.getTheme().compareTo(o.getTheme());
        if (compare != 0) return compare;

        compare = this.getQuestion().compareTo(o.getQuestion());
        if (compare != 0) return compare;

        return 0;
    }
}
