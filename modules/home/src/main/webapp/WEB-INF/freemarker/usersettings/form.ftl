<#escape x as x?html>
<h1>User settings for ${user.firstName}</h1>
<@f.form method="post" class="form-horizontal" action="${url('/settings')}" commandName="userSettingsCommand">
	

<#if (moduleRoles?size gt 0)>	

	<h2 name="submission-alerts">Submission email alerts</h2>
	
	<@form.row>
		<@form.field>
			<label><@f.radiobutton path="alertsSubmission" value="allSubmissions" />All submissions</label>
			<label><@f.radiobutton path="alertsSubmission" value="lateSubmissions" />Late submissions (includes authorised late)</label>
			<label><@f.radiobutton path="alertsSubmission" value="none" />No alerts</label>
		</@form.field>
	</@form.row>

<div class="submit-buttons">
	<input type="submit" value="Save" class="btn btn-primary">
	<a class="btn" href="${url('/')}">Cancel</a>
</div>

<#else>
	<p>There are no settings available for your level of access. </p>
</#if>
	
</@f.form>
</#escape>