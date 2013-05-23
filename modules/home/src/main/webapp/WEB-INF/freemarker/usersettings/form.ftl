<#escape x as x?html>
<h1>User settings for ${user.firstName}</h1>
<@f.form method="post" class="form-horizontal" action="${url('/settings')}" commandName="userSettingsCommand">
	

<#if isCourseworkModuleManager>	

	<h2 name="submission-alerts">Submission email alerts</h2>
	
	<@form.row>
		<@form.field>
			<label><@f.radiobutton path="alertsSubmission" value="allSubmissions" />All submissions</label>
			<label><@f.radiobutton path="alertsSubmission" value="lateSubmissions" />Late submissions (includes authorised late)</label>
			<label><@f.radiobutton path="alertsSubmission" value="none" />No alerts</label>
		</@form.field>
	</@form.row>
	
	<@form.row>
		<@form.label>Week numbering system</@form.label>
		<@form.field>
			<@form.label checkbox=true>
				<@f.radiobutton path="weekNumberingSystem" value="" />
				Use the department's choice of week numbering system
			</@form.label>
			<@form.label checkbox=true>
				<@f.radiobutton path="weekNumberingSystem" value="term" />
				Count weeks from 1-10 for each term (the first week of the Spring term is Term 2, week 1)
			</@form.label>
			<@form.label checkbox=true>
				<@f.radiobutton path="weekNumberingSystem" value="cumulative" />
				Count term weeks cumulatively (the first week of the Spring term is Term 2, week 11)
			</@form.label>
			<@form.label checkbox=true>
				<@f.radiobutton path="weekNumberingSystem" value="academic" />
				Use academic week numbers, including vacations (the first week of the Spring term is week 15 or week 16)
			</@form.label>
			<@form.label checkbox=true>
				<@f.radiobutton path="weekNumberingSystem" value="none" />
				Use no week numbers, displaying dates instead
			</@form.label>
			<@f.errors path="weekNumberingSystem" cssClass="error" />
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