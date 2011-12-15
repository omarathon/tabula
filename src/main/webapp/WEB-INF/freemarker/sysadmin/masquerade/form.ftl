<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign form=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<h1>Masquerade as a different user</h1>

<#if user.masquerading>

<p>Masquerading as ${user.apparentId} (${user.apparentUser.fullName}).</p>

<@form.form method="post" action="/sysadmin/masquerade">
  <input type="hidden" name="action" value="remove" />
  <input type="submit" value="Unmask">
</@form.form>

<#else>

<#if actionMessage = "removed">
<p>You are no longer masquerading.</p>
</#if>

<div>
<@form.form method="post" action="/sysadmin/masquerade">
  User ID: <input type="text" name="usercode" />
  <input type="submit" value="Enmasken">
</@form.form>
</div>

</#if>