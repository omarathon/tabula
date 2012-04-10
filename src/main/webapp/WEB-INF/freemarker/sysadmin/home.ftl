
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
<@f.form method="post" action="/sysadmin/import">
  <input type="submit" value="Run department/module import" onclick="return confirm('Really? Could take a minute.')">
</@f.form>
</p>

<hr>

<p>
<@f.form method="post" action="/sysadmin/index/run" commandName="reindexForm">
Rebuild index from <@f.input path="from" cssClass="date-time-picker" />
<input type="submit" value="Index" onclick="return confirm('Really? Could take a while.')"/>
</@f.form>