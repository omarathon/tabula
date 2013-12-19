
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

<h2>Imports</h2>

<p>
<@f.form method="post" action="${url('/sysadmin/import', '/scheduling')}">
  <input class="btn btn-danger" type="submit" value="Departments & modules" onclick="return confirm('Really? Could take a minute.')">
</@f.form>
</p>

<p>
<@f.form method="post" action="${url('/sysadmin/import-sits', '/scheduling')}">
  <input class="btn btn-danger" type="submit" value="SITS assignments" onclick="return confirm('Really? Could take a minute.')">
</@f.form>
</p>

<p>
<@f.form method="post" action="${url('/sysadmin/import-profiles', '/scheduling')}" commandName="blankForm">
	<div class="input-append">
		<@f.input id="profiles-dept" path="deptCode" cssClass="span8" placeholder="deptCode (optional)" /><input class="btn btn-danger" type="submit" value="Profiles" onclick="return confirm('Really? Could take a minute.')">
	</div>
</@f.form>
</p>

<h2>Indexing</h2>

<p>
<@f.form method="post" action="${url('/sysadmin/index/run-audit', '/scheduling')}" commandName="blankForm">
Rebuild audit event index from
<div class="input-append">
<@f.input id="xaudit-from" path="from" cssClass="date-time-picker" placeholder="Click to pick a date" /><input class="btn btn-danger" type="submit" value="Index" onclick="return confirm('Really? Could take a while.')"/>
</div>
</@f.form>
</p>

<p>
<@f.form method="post" action="${url('/sysadmin/index/run-profiles', '/scheduling')}" commandName="blankForm">
Rebuild profiles index for
<br />
<@f.input id="xprofiles-dept" path="deptCode" cssClass="span6" placeholder="deptCode (optional)" /> from
<div class="input-append">
<@f.input id="xprofiles-from" path="from" cssClass="date-time-picker" placeholder="Click to pick a date" /><input class="btn btn-danger" type="submit" value="Index" onclick="return confirm('Really? Could take a while.')"/>
</div>
</@f.form>
</p>

<h2>Scary special stuff</h2>

<p>
	<a href="<@url page="/sysadmin/repl" />">Evaluator</a>
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