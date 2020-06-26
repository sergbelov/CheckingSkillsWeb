package ru.web;

import com.google.common.collect.ImmutableMap;

import java.io.*;
import java.util.*;
import java.util.jar.Manifest;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javafx.application.Application;
import okhttp3.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ru.utils.authorization.UserAuthorizationService;
import ru.questions.Questions;
import ru.utils.files.PropertiesService;

@Controller
@Scope("session")
//@CacheEvict(value = {"login", "params"}, allEntries = true)
//@SessionAttributes(value = "login")
//@SessionAttributes(types = CheckingSkillsWeb.class)
public class CheckingSkillsWeb {

//    private final String VERSION = "2019.10.05";
    private String VERSION = "none";
    //    private final String FILE_PROPERTIES = "../webapps/CheckingSkillsWeb/WEB-INF/classes/CheckingSkillsWeb.properties";
    private final String FILE_PROPERTIES = "CheckingSkillsWeb.properties";

    private final Map<String, String> propertyMap = new LinkedHashMap<String, String>() {{
        put("QUESTION_MAX", "10");                                  // максимальное количество задаваемых вопросов
        put("QUESTION_FILE", "C:/TEMP/questions/Questions.json");    // файл с вопросами
        put("RESULT_PATH", "C:/TEMP/questions/result/");           // путь для сохранения результатов тестирования
        put("RESULT_FORMAT", "JSON");                                // формат файла с результатами тестирования XML или JSON
        put("SHOW_ANSWERS", "TRUE");                                // отображать правильные варианты ответов
        put("LOGGER_LEVEL", "INFO");                                // уровень логирования
        put("DB_URL", "jdbc:hsqldb:file:C:/TEMP/questions/HSQL/TEMP;hsqldb.lock_file=false");                              // тип SQL-подключения: HSQLDB, ORACLE, SQLSERVER
        put("DB_USERNAME", "admin");                               // DB пользователь
        put("DB_PASSWORD", "admin");                               // DB пароль
        put("USER_REGISTRATION", "true");                                // Разрешена самостоятельная регистрация пользователей
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

//    private WebUser webUser = new WebUser();
//    private WebParamsObject webParams = new WebParamsObject();
//    private Questions questions = new Questions();

    private ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
    private WebUser webUser = context.getBean("webUserBean", WebUser.class);
    private WebParams webParams = context.getBean("webParamsBean", WebParams.class);
    private Questions questions = context.getBean("questionsBean", Questions.class);

    private PropertiesService propertiesService = new PropertiesService(propertyMap);
    private UserAuthorizationService userAuthorizationService = null;

    private boolean isOk;
    private String session;

    public CheckingSkillsWeb(){
        String pathToPom = "../webapps/CheckingSkillsWeb/META-INF/maven/ru.utils/CheckingSkillsWeb/pom.xml";
        if ((new File(pathToPom)).exists()) {
            Model model = null;
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(pathToPom));) {
                MavenXpp3Reader reader = new MavenXpp3Reader();
                model = reader.read(bufferedReader);
            } catch (Exception e) {
                LOG.error("Ошибка при чтении данных из pom.xml", e);
            }
            if (model != null) {
                VERSION = model.getVersion();
            }
        }
    }

    @RequestMapping(value = {"/", "/login", "/logout", "/registration", "/params", "/desktop", "/result"}, method = RequestMethod.GET)
    private ModelAndView showLoginGet() {
//        return createModel("forward:/login", new WebParamsObject());
        return createModel("login", new WebParams());
    }

    //    @RequestMapping(value = {"/", "/login", "/logout", "/registration", "/params", "/desktop", "/result"}, method = RequestMethod.GET)
//    @RequestMapping(value = {"/", "/login", "/logout", "/registration", "/params", "/desktop", "/result"})
    @RequestMapping(value = {"/", "/login", "/logout"})
    private ModelAndView showLogin(HttpServletRequest request,
                                   HttpServletResponse response) throws MessagingException {

/*
        response.setHeader("Cache-Control", "private, no-cache, no-store, must-revalidate");
//        response.setHeader("expires", "-1");
        response.setHeader("pragma", "no-cache");
*/

        if (webUser.isDefinedUser()) {
            LOG.info("Выход пользователя {} ({})", webUser.getUserName(), webUser.getFullUserName());
            userAuthorizationService.endSession();
            webUser.clear();
        }

//        return createModel("login", new WebParamsObject());
        return createModel("login", webParams);
    }

    @RequestMapping(value = "/registration", method = RequestMethod.POST)
    private ModelAndView showRegistration(@ModelAttribute("login") WebParams webParams,
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
    private ModelAndView createParams(@ModelAttribute("login") WebParams webParams,
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
        } else {
            webUser.clear();
        }

        propertiesService.readProperties(FILE_PROPERTIES, propertiesService.getLevel("LOGGER_LEVEL"));

        if (userAuthorizationService == null) {
            userAuthorizationService = new UserAuthorizationService.Builder()
                    .dbUrl(propertiesService.getString("DB_URL"))
                    .dbUserName(propertiesService.getString("DB_USERNAME"))
                    .dbPassword(propertiesService.getString("DB_PASSWORD"))
                    .loggerLevel(propertiesService.getLevel("LOGGER_LEVEL"))
                    .build();
        }

        userAuthorizationService.setSessionDuration(60000);

        if (!userAuthorizationService.connect()) {
            errorMessage = userAuthorizationService.getErrorMessage();
        }

        if (!(isOk = userAuthorizationService.isUserCorrect(
                webUser.getUserName(),
                webParams.getFullUserName(),
                webUser.getPassword(),
                webParams.getPassword2()))) {
            errorMessage = userAuthorizationService.getErrorMessage();
        }

//        userAuthorizationService.disconnect();

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
                    userAuthorizationService.getError().equals(UserAuthorizationService.Error.LOGIN)) {

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
    private ModelAndView createDesktop(@ModelAttribute("params") WebParams webParams,
                                       HttpServletRequest request,
                                       HttpServletResponse response) throws MessagingException {

/*
        response.setHeader("Cache-Control", "private, no-cache, no-store, must-revalidate");
//        response.setHeader("expires", "-1");
        response.setHeader("pragma", "no-cache");
*/

        ModelAndView mav = null;
        String errorMessage = "";

/*
        if (!userAuthorizationService.isSessionCorrect()){
            errorMessage = "Сессия просрочена";
            mav = createModel("login", webParams, Optional.of(errorMessage));
        }
*/

        if (errorMessage.isEmpty() && (webParams.getTheme() == null || webParams.getTheme().isEmpty())) {
            errorMessage = "Выберите тему для тестирования";
            mav = createModel("params", webParams, Optional.of(errorMessage));
        }

        if (errorMessage.isEmpty()) {
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
    private ModelAndView createResult(@ModelAttribute("desktop") WebParams webParams,
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

/*
        if (!userAuthorizationService.isSessionCorrect()){
            errorMessage = "Сессия просрочена";
        }
*/

        for (int q = 0; q < questions.getQuestionMax(); q++) {
            if (request.getParameterValues("answer" + q) != null) {
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

    private ModelAndView createModel(String viewName, WebParams webParams) {
        return createModel(viewName, webParams, Optional.empty());
    }

    private ModelAndView createModel(String viewName, WebParams webParams, Optional<String> errorMessage) {
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
