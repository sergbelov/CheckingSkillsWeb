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
        <title>Тестирование знаний (результат тестирования)</title>
    </head>
    <body>
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
                                    <button style="border: none; padding: 0; background: none;">
                                        <input type="image" src="${exit}" alt="" class="popup_logo" height="30px"/>
                                    </button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </form:form>
            </header>
        </div>
        </br>
        <p>Тестирование завершено</p>
        ${result}
    </body>
</html>