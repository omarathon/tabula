<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign form=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<h1>Sys admin</h1>

<p><a href="/sysadmin/departments">List all departments in the system</a></p>

<p>
<@form.form method="post" action="/sysadmin/import">
  <input type="submit" value="Run department/module import">
</@form.form>
</p>