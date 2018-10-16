<%@taglib prefix="tags" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="Java"
        contentType="text/html;charset=UTF-8"
        pageEncoding="UTF-8"
        errorPage="login.jsp"
%>
<!DOCTYPE html>
<html>
    <head>
        <tags:url value="/resources/css/style.css" var="style"/>
        <tags:url value="/resources/img/test.png" var="logo"/>
        <tags:url value="/resources/img/exit.png" var="exit"/>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" type="text/css" href="${style}">
        <title>Тестирование знаний (выбор темы)</title>
    </head>
    <body class="ru" id="ru">
        <div id="page">
            <header id=header" role="banner">
                <form:form method='post' action="login.jsp" name='logout' autocomplete='on' modelAttribute="login">
                    <table width="100%" style="background: #d0ffd0; color: #006600; border-radius: 10px;">
                        <tbody>
                            <tr>
                                <td width="40px" style="text-align:left;">
                                    <img src="${logo}" alt="" class="popup_logo" height="40px"/>
                                </td>
                                <td id="user-options" style="text-align:center; font-size:25px">
                                </td>
                                <td id="user-options" style="text-align:right;">
                                    ${webUser.getUserName()} (${webUser.getFullUserName()})
                                </td>
                                <td width="30px" style="text-align:right;">
                                    <button class="button_exit">
                                        <input type="image" src="${exit}" alt="" class="popup_logo" height="30px"/>
                                    </button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </form:form>
            </header>
        </div>
    <c:if test="${not empty errorMessage}">
        <div class="error" id="error">
            <span id="message" class="error_message">${errorMessage}</span>
            <a href="javascript:;" class="error_close" onClick="removeError()"></a>
        </div>
    </c:if>
        <div class="popup" id="popup_enter">
            <div class="popup_top">
                <table width="100%">
                    <tbody>
                        <tr>
                            <td>
                                <img src="${logo}" alt="" class="popup_logo" height="40px"/>
                            </td>
                            <td style="text-align:left;">
                                <span id="title_ru" class="popup_title">Тестирование знаний (версия ${VERSION})</span>
                            </td>
                         </tr>
                    </tbody>
                </table>
            </div>
            <div class="popup_form_params">
                <form:form method='post' action="desktop.jsp" name='desktop' autocomplete='on' modelAttribute="login">
                    <p>
                        <label for="theme" id="label-theme" >
                            <span id="theme_ru" class="label-ru">ТЕМА ТЕСТИРОВАНИЯ</span>
                        </label>
                        <div class="select">
                            <select id="theme" name="theme" required="reguired">
                                <c:forEach items="${themesList}" var="item">
                                    <option value="${item}">${item}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </p>
                    <button type="submit" value="Submit" class="button">
                        <div id="label-report">
                            <span id="report_ru">НАЧАТЬ ТЕСТИРОВАНИЕ</span>
                        </div>
                    </button>

                </form:form>
            </div>
        </div>
        <script type="text/javascript">
            function removeError() {
                var errorParent = document.getElementById('ru')
                var error = document.getElementById('error')
                errorParent.removeChild(error);
            }
        </script>
    </body>
</html>
