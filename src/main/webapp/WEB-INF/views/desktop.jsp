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
<c:choose>
    <c:when test="${questions.isStarted()}">
        <title>Тестирование знаний (тест)</title>
    </c:when>
    <c:otherwise>
        <title>Тестирование знаний (результат тестирования)</title>
    </c:otherwise>
</c:choose>
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
                                    ${questions.getTheme()}
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
        <div>
            ${result}
        </div>
        <hr>
<c:choose>
    <c:when test="${webUser.isDefinedUser()}">
<c:choose>
    <c:when test="${questions.isStarted()}">
        <form:form method='post' action="result.jsp" name='result' autocomplete='on' modelAttribute="login">
        <% int qq = 0; %>
        <c:forEach items="${questions.getQuestionsList()}" var="itemQuestion">
                <div>
                        <fieldset style="border-radius: 10px;">
                            <legend><h5>[<%=qq+1%> из ${questions.getQuestionMax()}] ${itemQuestion.getAuthor()}</h5></legend>
                            <h3>${itemQuestion.getQuestion()}</h3>
            <% int aa = 0; %>
            <c:forEach items="${itemQuestion.getAnswersList()}" var="itemAnswer">
                                <hr>
                                <p>
                                    <label><input type="${itemQuestion.getType()}" id="answer<%=qq%>" name="answer<%=qq%>" value="<%=aa%>" ${itemAnswer.isSelectedHTML()} />
                                    ${itemAnswer.getAnswer()}</label>
                                </p>
                <% aa++; %>
            </c:forEach>
                        </fieldset>
                </div>
                </br>
                <% qq++; %>
        </c:forEach>
            <hr>
            <button style="width: 250px; height: 32px; background: #19aa39; color: #FFF;" class="button" type="submit" value="Submit">
                <div id="label-desktop">
                    <span id="report_ru">ЗАВЕРШИТЬ ТЕСТИРОВАНИЕ</span>
                </duv>
            </button>
        </form:form>
    </c:when>
    <c:otherwise>
        <form:form method='post' action="params.jsp" name='params' autocomplete='off' modelAttribute="login">
            <c:if test="${questions.isShowAnswers()}">
            <% int qq = 0; %>
            <c:forEach items="${questions.getQuestionsList()}" var="itemQuestion">
                <div>
                        <fieldset style="border-radius: 10px;">
                            <legend><h5>[<%=qq+1%> из ${questions.getQuestionMax()}] ${itemQuestion.getAuthor()}</h5></legend>
                            <h3>${itemQuestion.getQuestion()}</h3>
            <% int aa = 0; %>
            <c:forEach items="${itemQuestion.getAnswersList()}" var="itemAnswer">
                                <hr>
                                <p style="background-color:${itemAnswer.getColorHTML()}">
                                    <label><input type="${itemQuestion.getType()}" id="answer<%=qq%>" name="answer<%=qq%>" value="<%=aa%>" ${itemAnswer.isSelectedHTML()} disabled />
                                    ${itemAnswer.getAnswer()}</label>
                                </p>
                <% aa++; %>
            </c:forEach>
                        </fieldset>
                </div>
                </br>
            <% qq++; %>
            </c:forEach>
            </c:if>
            <hr>
             <button style="width: 250px; height: 32px; background: #19aa39; color: #FFF;" class="button" type="submit" value="Submit">
                <span id="report_ru">ВЫБОР ТЕМЫ ТЕСТИРОВАНИЯ</span>
            </button>
        </form:form>
    </c:otherwise>
</c:choose>
</c:when>
<c:otherwise>
    Необходима авторизация !
</c:otherwise>
</c:choose>
    </body>
</html>