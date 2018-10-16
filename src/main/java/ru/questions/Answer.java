package ru.questions;

import java.awt.*;

/**
 * @author Белов Сергей
 * Вариант ответа на вопрос
 */
public class Answer {
    private String answer;      // ответ
    private boolean correct;    // признак корректности
    private boolean selected;   // выбран в контроле

    private final Color green   = new Color(100, 200, 100);
    private final Color yellow  = new Color(247, 250, 144);
    private final Color red     = new Color(250, 100, 100);
    private final Color blue    = new Color(100, 100, 200);
    private final Color black   = new Color(0, 0, 0);
    private final Color defaultBackground = null;

    public Answer(String answer, boolean correct, boolean selected) {
        this.answer = answer;
        this.correct = correct;
        this.selected = selected;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isCorrect() {
        return correct;
    }

    public boolean isSelected() {
        return selected;
    }
    public String isSelectedHTML() {
        return selected ? "checked" : "";
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Color getColor(){
        if (selected && correct) { return green;}       // отмечен правильный вариант
        else if (selected && !correct) {return red;}    // отмечен неправильный вариант
        else if (correct && !selected) {return yellow;} // не отмечен правильный вариант
        else {return null;}
    }

    public String getColorHTML(){
        Color color = new Color(255,255,255);
        if (selected && correct) { color = green;}      // отмечен правильный вариант
        else if (selected && !correct) {color = red;}   // отмечен неправильный вариант
        else if (correct && !selected) {color = yellow;}// не отмечен правильный вариант

        return "#" + Integer.toHexString(color.getRGB()).substring(2).toLowerCase();
    }

}
