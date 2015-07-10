<h1>Web system administrating system screen page</h1>

<#if user.masquerading>
<p>Oh, hello ${user.fullName}. <em>Or should I say, ${user.realUser.fullName}?!</em></p>
</#if>

<div class="row-fluid">

<div class="span8">
<h2>Normal regular stuff</h2>
<p><a class="btn" href="<@url page="/sysadmin/permissions-helper" />"><i class="icon-fixed-width fa fa-fw icon-lock fa fa-lock"></i> Permissions helper</a></p>
<p><a class="btn" href="<@url page="/sysadmin/departments/" />"><i class="icon-fixed-width fa fa-fw icon-sitemap fa fa-sitemap"></i> List all departments in the system</a></p>
<p><a class="btn" href="<@url page="/sysadmin/relationships" />"><i class="icon-fixed-width fa fa-fw icon-heart-empty fa fa-heart-o"></i> Student relationship types</a></p>
<p><a class="btn" href="<@url page="/sysadmin/attendancetemplates" />"><i class="icon-fixed-width fa fa-fw icon-copy fa fa-files-o"></i> Attendance monitoring templates</a></p>
<p><a class="btn" href="<@url page="/masquerade" context="/admin" />"><i class="icon-fixed-width fa fa-fw icon-eye-open fa fa-eye"></i> Masquerade</a></p>
<p><a class="btn" href="<@url page="/sysadmin/audit/search" />"><i class="icon-fixed-width fa fa-fw icon-list-alt fa fa-list-alt"></i> List audit events (Index version)</a></p>
<p><a class="btn" href="<@url page="/sysadmin/jobs/list" context="/scheduling" />"><i class="icon-fixed-width fa fa-fw icon-refresh fa fa-refresh"></i> Background jobs</a></p>
<p><a class="btn" href="<@url page="/sysadmin/emails/list" context="/scheduling" />"><i class="icon-fixed-width fa fa-fw icon-envelope-alt fa fa-envelope-o"></i> Email queue</a></p>
<p><a class="btn" href="<@url page="/sysadmin/features" />"><i class="icon-fixed-width fa fa-fw icon-flag-alt fa fa-flag-o"></i> Set feature flags</a></p>
<p><a class="btn" href="<@url page="/sysadmin/statistics" />"><i class="icon-fixed-width fa fa-fw icon-bar-chart fa fa-bar-chart-o"></i> Internal statistics</a></p>
<p><a class="btn" href="<@url page="/sysadmin/complete-scheduled-notification" context="/scheduling" />"><i class="icon-fixed-width fa fa-fw icon-time fa fa-clock-o"></i> Complete scheduled notifications</a></p>
<p><a class="btn" href="<@url page="/sysadmin/event-calendar" />"><i class="icon-fixed-width fa fa-fw icon-calendar fa fa-calendar"></i> Upcoming event calendar</a></p>

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
		<button id="disable-godmode-button" class="btn btn-info"><i class="icon-eye-close fa fa-eye-slash"></i> Disable God mode</button>
	<#else>
		<button id="enable-godmode-button" class="btn btn-warning"><i class="icon-eye-open fa fa-eye"></i> Enable God mode</button>
	</#if>
</@f.form>

<h2>Imports</h2>

<p>
<@f.form method="post" action="${url('/sysadmin/import', '/scheduling')}">
  <input class="btn btn-danger" type="submit" value="Departments, modules, routes etc." onclick="return confirm('Really? Could take a minute.')">
</@f.form>
</p>


<p>
<@f.form method="post" action="${url('/sysadmin/import-department', '/scheduling')}" commandName="blankForm">
	<div class="input-append">
		<@f.input id="import-modules-dept" path="deptCode" cssClass="span8" placeholder="deptCodes" /><input class="btn btn-danger" type="submit" value="Modules" onclick="return confirm('Really? Could take a minute.')">
	</div>
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
		<@f.input id="import-profiles-dept" path="deptCode" cssClass="span8" placeholder="deptCode (optional)" /><input class="btn btn-danger" type="submit" value="Profiles" onclick="return confirm('Really? Could take a minute.')">
	</div>
</@f.form>
</p>

<h2>Indexing</h2>

<#macro reindex_form name text>
<p>
<@f.form method="post" action="${url('/sysadmin/index/run-'+name, '/scheduling')}" commandName="blankForm">
	Rebuild ${text} from
	<div class="input-append">
		<@f.input id="index-${name}-from" path="from" cssClass="date-time-picker" placeholder="Click to pick a date" /><input class="btn btn-danger" type="submit" value="Index" onclick="return confirm('Really? Could take a while.')"/>
	</div>
	<#if name = 'profiles'>
		For department
		<@f.input id="index-${name}-dept" path="deptCode" cssClass="span6" placeholder="Dept code (optional)" />
	</#if>
</@f.form>
</p>
</#macro>

<@reindex_form 'audit' 'audit event index' />
<@reindex_form 'profiles' 'profiles index' />
<@reindex_form 'notifications' 'notification stream index' />

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

<h4>Emergency message</h4>
<#if emergencyMessageService.enabled>
	<p>Currently <strong>enabled</strong>.</p>
<#else>
	<p>Disabled.</p>
</#if>
<p><a href="<@url page="/sysadmin/emergencymessage"/>">Update settings</a></p>

<p>
	<@f.form method="post" action="${url('/sysadmin/jobs/create-test','/scheduling')}">
		<input class="btn" type="submit" value="Create test job">
	</@f.form>
</p>

</div>
</div>