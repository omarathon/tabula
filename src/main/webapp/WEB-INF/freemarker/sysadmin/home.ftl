
<h1>Web system administrating system screen page</h1>

<#if user.masquerading>
<p>Oh, hello ${user.fullName}. <em>Or should I say, ${user.realUser.fullName}?!</em></p>
</#if>

<div class="row-fluid">

<div class="span8">
<h2>Normal regular stuff</h2>
<p><a class="btn" href="/sysadmin/departments/">List all departments in the system</a></p>
<p><a class="btn" href="/admin/masquerade/"><i class="icon-eye-open"></i> Masquerade</a></p>
<p><a class="btn" href="/sysadmin/audit/list">List audit events</a></p>
<p><a class="btn" href="/sysadmin/audit/search">List audit events (Index version)</a></p>
</div>

<div class="span4">
<h2>Scary special stuff</h2>

<p>
<@f.form method="post" action="/sysadmin/import">
  <input class="btn btn-danger" type="submit" value="Run department/module import" onclick="return confirm('Really? Could take a minute.')">
</@f.form>
</p>

<p>
<@f.form method="post" action="/sysadmin/import-sits">
  <input class="btn btn-danger" type="submit" value="Run assignment data import" onclick="return confirm('Really? Could take a minute.')">
</@f.form>
</p>

<hr>

<p>
<@f.form method="post" action="/sysadmin/index/run" commandName="reindexForm">
Rebuild index from
<div class="input-append"> 
<@f.input path="from" cssClass="date-time-picker" placeholder="Click to pick a date" /><input class="btn btn-danger" type="submit" value="Index" onclick="return confirm('Really? Could take a while.')"/>
</div>
</@f.form>
</p>

<h4>Maintenance mode</h4>

<#if maintenanceModeService.enabled>
<p>Currently <strong>enabled</strong>.</p>
<#else>
<p>Disabled.</p>
</#if>

<p><a href="<@url page="/sysadmin/maintenance"/>">Update settings</a></p>

</div>
</div>