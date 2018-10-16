package ru.questions;

import java.util.List;

/**
 * @author Белов Сергей
 * Сохранение результата тестирования в файл
 */

public interface SaveResult {

    /**
     * Сохранение результата тестирования в файл
     * @param path          - путь для сохранения
     * @param fileName      - имя файла
     * @param user          - пользователь
     * @param startingTime  - время начала теста
     * @param stoppingTime  - время окончания теста
     * @param theme         - тема
     * @param resultTXT     - результат тестирования
     * @param wrongAnswersList - список вопросов на которые дан неверный ответ
     */
    void save(  String path,
                String fileName,
                String user,
                long startingTime,
                long stoppingTime,
                String theme,
                String resultTXT,
                List<String> wrongAnswersList);
}
