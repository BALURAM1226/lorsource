<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date,java.util.List, ru.org.linux.boxlet.BoxletVectorRunner"   buffer="60kb"%>
<%@ page import="ru.org.linux.site.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
  ~ Copyright 1998-2009 Linux.org.ru
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

<title>LINUX.ORG.RU - Русская информация об ОС Linux</title>
<META NAME="Keywords" CONTENT="linux линукс операционная система документация gnu бесплатное свободное програмное обеспечение софт unix юникс software free documentation operating system новости news">
<META NAME="Description" CONTENT="Все о Linux на русском языке">
<LINK REL="alternate" TITLE="L.O.R RSS" HREF="section-rss.jsp?section=1" TYPE="application/rss+xml">

<%
  response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
  response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 2 * 1000).getTime());

%>
<jsp:include page="WEB-INF/jsp/header-main.jsp"/>
<%
  boolean columns3 = tmpl.getProf().getBoolean("main.3columns");

  Connection db = null;
  try {
%>

<div style="clear: both"></div>
<div class="<%= columns3?"newsblog2":"newsblog"%>">
  <div class="<%= columns3?"newsblog-in2":"newsblog-in"%>">

<h1><a href="view-news.jsp?section=1">Новости</a></h1>
<%
  if (tmpl.isModeratorSession()) {
    out.print("<div class=\"nav\"  style=\"border-bottom: none\">");

    if (db==null) {
      db = LorDataSource.getConnection();
    }

    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery("select count(*) from topics,groups,sections where section=sections.id AND sections.moderate and topics.groupid=groups.id and not deleted and not topics.moderate AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");

    if (rs.next()) {
      int count = rs.getInt("count");

      out.print("[<a style=\"text-decoration: none\" href=\"view-all.jsp\">Неподтвержденных: " + count + ", ");
    }

    rs.close();

    rs = st.executeQuery("select count(*) from topics,groups where section=1 AND topics.groupid=groups.id and not deleted and not topics.moderate AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");

    if (rs.next()) {
      int count = rs.getInt("count");

      out.print(" в том числе новостей: " + count + "</a>]");
    }

    rs.close();

    st.close();

    out.print("</div>");

    db.close(); db=null;
  }

  NewsViewer nv = NewsViewer.getMainpage(tmpl.getConfig(), tmpl.getProf());

  out.print(ViewerCacher.getViewer(nv, tmpl, false));
%>
<div class="nav">
  [<a href="view-news.jsp?section=1&amp;offset=20">← предыдущие</a>]
  [<a href="add-section.jsp?section=1">добавить новость</a>]
  [<a href="section-rss.jsp?section=1">RSS</a>]
</div>
</div>
</div>

<%
  BoxletVectorRunner boxes;

  if (tmpl.getProf().getBoolean("main.3columns")) {
    boxes = new BoxletVectorRunner((List) tmpl.getProf().getObject("main3-1"));
  }
  else {
    boxes = new BoxletVectorRunner((List) tmpl.getProf().getObject("main2"));
  }
%>

<div class=column>
  <% if (Template.isSessionAuthorized(session)) { %>
<div class=boxlet>
<h2>Вход на сайт</h2>
<div class="boxlet_content">
Вы вошли как <b><%= session.getAttribute("nick") %></b>
<%
  if (db==null) {
    db = LorDataSource.getConnection();
  }
  
  User user = User.getUser(db, (String) session.getAttribute("nick"));

  out.print("<br>(статус: " + user.getStatus() + ')');
%><p>
* <a href="rules.jsp">Правила</a><br>
* <a href="edit-profile.jsp">Настройки</a><br>&nbsp;<br>
* <a href="show-topics.jsp?nick=<%= user.getNick() %>">Мои темы</a><br>
* <a href="show-comments.jsp?nick=<%= user.getNick() %>">Мои комментарии</a><br>
* <a href="show-replies.jsp?nick=<%= user.getNick() %>">Ответы на мои комментарии</a><br>
  </p>
</div>
</div>
  <% } %>

<!-- boxes -->
<%

  out.print(boxes.getContent(tmpl.getObjectConfig(), tmpl.getProf()));

%>
</div>
<% if (columns3) { %>
<div class=column2>
<%
  boxes = new BoxletVectorRunner((List) tmpl.getProf().getObject("main3-2"));

  out.print(boxes.getContent(tmpl.getObjectConfig(), tmpl.getProf()));
%>
</div>
<% } %>

<div style="clear: both"></div>

<% } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<jsp:include page="WEB-INF/jsp/footer-main.jsp"/>
