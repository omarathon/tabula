
<h1>Web system administrating system screen page</h1>

<#if user.masquerading>
<p>Oh, hello ${user.fullName}. <em>Or should I say, ${user.realUser.fullName}?!</em></p>
</#if>

<div class="row-fluid">

<div class="span8">
<h2>Normal regular stuff</h2>
<p><a class="btn" href="<@url page="/sysadmin/permissions-helper" />"><i class="icon-lock"></i> Permissions helper</a></p>
<p><a class="btn" href="<@url page="/sysadmin/departments/" />">List all departments in the system</a></p>
<p><a class="btn" href="<@url page="/sysadmin/relationships" />">Student relationship types</a></p>
<p><a class="btn" href="<@url page="/sysadmin/pointsettemplates" />">Monitoring point set templates</a></p>
<p><a class="btn" href="<@url page="/masquerade" context="/admin" />"><i class="icon-eye-open"></i> Masquerade</a></p>
<p><a class="btn" href="<@url page="/sysadmin/audit/list" />">List audit events</a></p>
<p><a class="btn" href="<@url page="/sysadmin/audit/search" />">List audit events (Index version)</a></p>
<p><a class="btn" href="<@url page="/sysadmin/jobs/list" context="/scheduling" />">Background jobs</a></p>
<p><a class="btn" href="<@url page="/sysadmin/features" />">Set feature flags</a></p>
<p><a class="btn" href="<@url page="/sysadmin/statistics" />">Internal statistics</a></p>

<h2>File syncing</h2>
<p><a class="btn" href="<@url page="/sysadmin/sync" context="/scheduling" />">Run file syncing</a></p>
<p><a class="btn" href="<@url page="/sysadmin/filesystem-cleanup" context="/scheduling" />">Delete unreferenced files from filesystem</a></p>
<p><a class="btn" href="<@url page="/sysadmin/filesystem-sanity" context="/scheduling" />">Sanity check filesystem</a></p>
</div>

<div class="span4">
<h2>God mode</h2>

<@f.form method="post" action="${url('/sysadmin/god')}">
	<#if user.god>
		<input type="hidden" name="action" value="remove" />
		<button id="disable-godmode-button" class="btn btn-info"><i class="icon-eye-close"></i> Disable God mode</button>
	<#else>
		<button id="enable-godmode-button" class="btn btn-warning"><i class="icon-eye-open"></i> Enable God mode</button>
	</#if>
</@f.form>

<h2>Scary special stuff</h2>

<p>
<@f.form method="post" action="${url('/sysadmin/import', '/scheduling')}">
  <input class="btn btn-danger" type="submit" value="Run department/module import" onclick="return confirm('Really? Could take a minute.')">
</@f.form>
</p>

<p>
<@f.form method="post" action="${url('/sysadmin/import-sits', '/scheduling')}">
  <input class="btn btn-danger" type="submit" value="Run assignment data import" onclick="return confirm('Really? Could take a minute.')">
</@f.form>
</p>

<p>
<@f.form method="post" action="${url('/sysadmin/import-profiles', '/scheduling')}">
  <input class="btn btn-danger" type="submit" value="Run profiles data import" onclick="return confirm('Really? Could take a minute.')">
</@f.form>
</p>

<p>
<a href="<@url page="/sysadmin/repl" />">Evaluator</a>
</p>

<hr>

<p>
<@f.form method="post" action="${url('/sysadmin/index/run-audit', '/scheduling')}" commandName="reindexForm">
Rebuild audit event index from
<div class="input-append"> 
<@f.input id="audit-from" path="from" cssClass="date-time-picker" placeholder="Click to pick a date" /><input class="btn btn-danger" type="submit" value="Index" onclick="return confirm('Really? Could take a while.')"/>
</div>
</@f.form>
</p>

<p>
<@f.form method="post" action="${url('/sysadmin/index/run-profiles', '/scheduling')}" commandName="reindexForm">
Rebuild profiles index for
<br />
<@f.input id="profiles-dept" path="deptCode" cssClass="span6" placeholder="deptCode (optional)" /> from
<div class="input-append"> 
<@f.input id="profiles-from" path="from" cssClass="date-time-picker" placeholder="Click to pick a date" /><input class="btn btn-danger" type="submit" value="Index" onclick="return confirm('Really? Could take a while.')"/>
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

<p>
	<@f.form method="post" action="${url('/sysadmin/jobs/create-test','/scheduling')}">
		<input class="btn" type="submit" value="Create test job">
	</@f.form>
</p>

</div>
</div>