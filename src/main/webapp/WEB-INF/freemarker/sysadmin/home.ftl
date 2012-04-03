<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign form=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<h1>Web system administrating system screen page</h1>

<#if user.masquerading>
<p>Oh, hello ${user.fullName}. <em>Or should I say, ${user.realUser.fullName}?!</em></p>
</#if>

<p><a href="/sysadmin/departments/">List all departments in the system</a></p>

<p><a href="/admin/masquerade/">Masquerade</a></p>

<p><a href="/sysadmin/audit/list">List audit events</a></p>

<p><a href="/sysadmin/audit/search">List audit events (Index version)</a></p>

<hr>

<p>
<@form.form method="post" action="/sysadmin/import">
  <input type="submit" value="Run department/module import">
</@form.form>
</p>