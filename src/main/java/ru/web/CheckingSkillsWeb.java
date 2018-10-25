package ru.web;

import com.google.common.collect.ImmutableMap;

import java.util.*;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import okhttp3.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ru.utils.authorization.UserAuthorizationService;
import ru.questions.Questions;
import ru.utils.db.DBService;
import ru.utils.files.PropertiesService;

@Controller
@Scope("session")
//@CacheEvict(value = {"login", "params"}, allEntries = true)
//@SessionAttributes(value = "login")
//@SessionAttributes(types = CheckingSkillsWeb.class)
public class CheckingSkillsWeb {

    private final String VERSION = "1.0";
    private final String FILE_PROPERTIES = "../webapps/CheckingSkillsWeb/WEB-INF/classes/CheckingSkillsWeb.properties";

    private final Map<String, String> propertyMap = new LinkedHashMap<String, String>(){{
        put("QUESTION_MAX",    "10");                               // максимальное количество задаваемых вопросов
        put("QUESTION_FILE",   "C:/TEMP/questions/Questions.json"); // файл с вопросами
        put("RESULT_PATH",     "C:/TEMP/questions/result/");        // путь для сохранения результатов тестирования
        put("RESULT_FORMAT",   "JSON");                             // формат файла с результатами тестирования XML или JSON
        put("SHOW_ANSWERS",    "TRUE");                             // отображать правильные варианты ответов
        put("LOGGER_LEVEL",    "WARN");                             // уровень логирования
        put("HSQL_PATH",       "C:/TEMP/questions/HSQL/");          // HSQL путь к базе
        put("HSQL_DB",         "DB_CheckingSkills");                // HSQL имя базы
        put("HSQL_LOGIN",      "admin");                            // HSQL логин
        put("HSQL_PASSWORD",   "admin");                            // HSQL пароль
        put("USER_REGISTRATION","true");                            // Самостоятельная регистрация пользователей
    }};


/*
    // параметры из CheckingSkillsWeb.properties
    @Value("${QUESTION_MAX:10}")
    private int QUESTION_MAX;

    @Value("${QUESTION_FILE:C:/TEMP/questions/XMLDataTest.json}")
    private String QUESTION_FILE;

    @Value("${RESULT_PATH:C:/TEMP/questions/result/}")
    private String RESULT_PATH;

    @Value("${RESULT_FORMAT:JSON")
    private String RESULT_FORMAT;

    @Value("${LOGGER_LEVEL:DEBUG}")
    private Level LOGGER_LEVEL;

    @Value("${HSQL_PATH:C:/TEMP/questions/HSQL/}")
    private String HSQL_PATH;

    @Value("${HSQL_DB:DB_CheckingSkills}")
    private String HSQL_DB;

    @Value("${HSQL_LOGIN:admin}")
    private String HSQL_LOGIN;

    @Value("${HSQL_PASSWORD:admin}")
    private String HSQL_PASSWORD;

    @Value("${USER_REGISTRATION:true}")
    private boolean USER_REGISTRATION;
*/
    //    private static final Logger LOG = LogManager.getLogger(CheckingSkillsWeb.class);
    private static final Logger LOG = LogManager.getLogger();
    private static final MediaType JSON = MediaType.parse("application/json; charset=UTF-8");

    private WebUser webUser = new WebUser();
    private WebParamsObject webParams = new WebParamsObject();
    private PropertiesService propertiesService = new PropertiesService(propertyMap);
    private Questions questions = new Questions();
    private UserAuthorizationService userAuthorizationService = new UserAuthorizationService();

    private boolean isOk;

//    @RequestMapping(value = {"/", "/login", "/logout", "/registration", "/params", "/desktop", "/result"}, method = RequestMethod.GET)
    @RequestMapping(value = {"/", "/login", "/logout", "/registration", "/params", "/desktop", "/result"})
    private ModelAndView showLogin(HttpServletRequest request,
                                   HttpServletResponse response) throws MessagingException {

/*
        response.setHeader("Cache-Control", "private, no-cache, no-store, must-revalidate");
//        response.setHeader("expires", "-1");
        response.setHeader("pragma", "no-cache");
*/

        if (webUser.isDefinedUser()){
            LOG.info("Выход пользователя {} ({})", webUser.getUserName(), webUser.getFullUserName());
            userAuthorizationService.endSession();
            webUser.clear();
        }
//        return createModel("login", new WebParamsObject());
        return createModel("login", webParams);
    }

    @RequestMapping(value = "/registration", method = RequestMethod.POST)
    private ModelAndView showRegistration(@ModelAttribute("login") WebParamsObject webParams,
                                          HttpServletRequest request,
                                          HttpServletResponse response) throws MessagingException {

/*
        response.setHeader("Cache-Control", "private, no-cache, no-store, must-revalidate");
//        response.setHeader("expires", "-1");
        response.setHeader("pragma", "no-cache");
*/

        return createModel("login", webParams);
    }

    @RequestMapping(value = "/params", method = RequestMethod.POST)
    private ModelAndView createParams(@ModelAttribute("login") WebParamsObject webParams,
                                      HttpServletRequest request,
                                      HttpServletResponse response) throws MessagingException {

/*
        response.setHeader("Cache-Control", "private, no-cache, no-store, must-revalidate");
//        response.setHeader("expires", "-1");
        response.setHeader("pragma", "no-cache");
*/

        ModelAndView mav;
        isOk = true;
        String errorMessage = "";
//        String curPath = new File(".").getAbsolutePath();
//        LOG.debug("Текущий каталог: {}", curPath);

        if (webParams.getUserName() != null && !webParams.getUserName().isEmpty()) {
            webUser.setUserName(webParams.getUserName());
            webUser.setPassword(webParams.getPassword());
            if (webParams.getFullUserName() != null && !webParams.getFullUserName().isEmpty()) {
                webUser.setFullUserName((webParams.getFullUserName()));
            }
        } else{
            webUser.clear();
        }

        propertiesService.readProperties(FILE_PROPERTIES, propertiesService.getLevel("LOGGER_LEVEL"));

        userAuthorizationService.setLoggerLevel(propertiesService.getLevel("LOGGER_LEVEL"));

        userAuthorizationService.connect(
                DBService.TypeDB.hsqldb,
                propertiesService.getString("HSQL_PATH"),
                0,
                propertiesService.getString("HSQL_DB"),
                propertiesService.getString("HSQL_LOGIN"),
                propertiesService.getString("HSQL_PASSWORD"));

        if ( !(isOk = userAuthorizationService.isUserCorrect(
                webUser.getUserName(),
                webParams.getFullUserName(),
                webUser.getPassword(),
                webParams.getPassword2()))) {
            errorMessage = userAuthorizationService.getErrorMessage();
        }

        userAuthorizationService.disconnect();

        if (isOk) {
            webUser.setFullUserName(userAuthorizationService.getFullUserName());
//            webUser.setPassword(webParams.getPassword());

            questions.setUser(webUser.getUserName());
            questions.readQuestions(
                    propertiesService.getInt("QUESTION_MAX"),
                    propertiesService.getString("QUESTION_FILE"),
                    propertiesService.getString("RESULT_PATH"),
                    propertiesService.getString("RESULT_FORMAT"),
                    propertiesService.getBoolean("SHOW_ANSWERS"),
                    propertiesService.getLevel("LOGGER_LEVEL")); // читаем вопросы из файла

            if (questions.getThemesList().size() > 0) {
                mav = createModel("params", ImmutableMap.<String, Object>builder()
                        .put("themesList", questions.getThemesList())
                        .build());
            } else {
                mav = createModel("login", webParams, Optional.of("Ошибка при чтении вопросов из файла"));
            }

        } else {
            if (propertiesService.getBoolean("USER_REGISTRATION") &&
                userAuthorizationService.getError().equals(UserAuthorizationService.ErrorList.Login)) {

                webUser.setFullUserName("");
                mav = createModel("registration", webParams);
            } else {
                if (webParams.getPassword2() != null) {
                    mav = createModel("registration", webParams, Optional.of(errorMessage));
                } else {
                    mav = createModel("login", webParams, Optional.of(errorMessage));
                }
            }
        }
        return mav;
    }

    @RequestMapping(value = "/desktop", method = RequestMethod.POST)
    private ModelAndView createDesktop(@ModelAttribute("params") WebParamsObject webParams,
                                       HttpServletRequest request,
                                       HttpServletResponse response) throws MessagingException {

/*
        response.setHeader("Cache-Control", "private, no-cache, no-store, must-revalidate");
//        response.setHeader("expires", "-1");
        response.setHeader("pragma", "no-cache");
*/

        ModelAndView mav;
        String errorMessage = "";

        if (webParams.getTheme() == null || webParams.getTheme().isEmpty()) {
            errorMessage = "Выберите тему для тестирования";
        }

        if (!errorMessage.isEmpty()) {
            mav = createModel("params", webParams, Optional.of(errorMessage));
        } else {
            questions.stop(false);
            questions.start(webParams.getTheme());
// выводим отчет
            mav = createModel("desktop", ImmutableMap.<String, Object>builder()
                    .put("result", "")
                    .put("questions", questions)
                    .build());
        }

        return mav;
    }

    @RequestMapping(value = "/result", method = RequestMethod.POST)
    private ModelAndView createResult(@ModelAttribute("desktop") WebParamsObject webParams,
                                      HttpServletRequest request,
                                      HttpServletResponse response) throws MessagingException {

/*
        response.setHeader("Cache-Control", "private, no-cache, no-store, must-revalidate");
//        response.setHeader("expires", "-1");
        response.setHeader("pragma", "no-cache");
*/

        ModelAndView mav;
        String errorMessage = "";
        StringBuilder res = new StringBuilder("\r\n");

        for (int q = 0; q < questions.getQuestionMax(); q++){
            if (request.getParameterValues("answer"+q) != null) {
//            LOG.info( q + " - "+ request.getParameterValues("answer"+q).length);
//            res.append("Вопрос:\r\n")
//               .append(questions.getString(q).getQuestion()).append("\r\n");

                for (String a : request.getParameterValues("answer" + q)) {
//                res.append("Ответы:\r\n")
//                   .append(questions.getString(q).getAnswer(Integer.parseInt(a)).getAnswer()).append("\r\n");
                    questions.get(q).getAnswer(Integer.parseInt(a)).setSelected(true);
                }
            }
        }

        if (!errorMessage.isEmpty()) {
            mav = createModel("desktop", webParams, Optional.of(errorMessage));
        } else {

            String result = questions.stop();
// выводим отчет
            mav = createModel("desktop", ImmutableMap.<String, Object>builder()
                    .put("result", result)
                    .put("questions", questions)
                    .build());
        }

        return mav;
    }

    private ModelAndView createModel(String viewName, WebParamsObject webParams) {
        return createModel(viewName, webParams, Optional.empty());
    }

    private ModelAndView createModel(String viewName, WebParamsObject webParams, Optional<String> errorMessage) {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        if (errorMessage.isPresent()) {
            builder = builder.put("errorMessage", errorMessage.get());
//            LOG.error("Есть ошибка {}", errorMessage.getString());
        }
//        return createModel(viewName, builder.put(WebParamsObject.NAME, webParams).build());
//        LOG.info("createModel(viewName = {} )", viewName);
        return createModel(viewName, builder.put("login", webParams).build());
    }

    private ModelAndView createModel(String viewName, Map<String, Object> attribute) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName(viewName);
        attribute.forEach((t, u) -> mav.addObject(t, u));
        mav.addObject("VERSION", VERSION);
        mav.addObject("webUser", webUser);
        return mav;
    }

}
