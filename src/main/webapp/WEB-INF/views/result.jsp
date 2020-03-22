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
<%@ include file="header.jsp" %>
        <p>Тестирование завершено</p>
        ${result}
    </body>
</html>