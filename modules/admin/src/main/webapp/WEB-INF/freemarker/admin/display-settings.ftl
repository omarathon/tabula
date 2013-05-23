<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
<h1>Display settings for ${department.name}</h1>
<@f.form method="post" class="form-horizontal" action="" commandName="displaySettingsCommand">
	<@form.row>
		<@form.label></@form.label>
		<@form.field>
			<@form.label checkbox=true>
				<@f.checkbox path="showStudentName" id="showStudentName" />
				Show student name with submission
			</@form.label>
			<@f.errors path="showStudentName" cssClass="error" />
			<div class="help-block">
				If this option is enabled, all assignments in this department will display the student's name in place of their university ID.
			</div>
		</@form.field>
	</@form.row>
	
	<@form.row>
		<@form.label></@form.label>
		<@form.field>
			<@form.label checkbox=true>
				<@f.checkbox path="plagiarismDetection" id="plagiarismDetection" />
				Enable Turnitin plagiarism detection of assignment submissions
			</@form.label>
			<@f.errors path="plagiarismDetection" cssClass="error" />
			<div class="help-block">
				If you turn this option off, it won't be possible to submit any assignment submissions in this department to Turnitin.
			</div>
		</@form.field>
	</@form.row>
	
	<@form.row>
		<@form.label>Assignment detail view</@form.label>
		<@form.field>
			<@form.label checkbox=true>
				<@f.radiobutton path="assignmentInfoView" value="default" />
				Let Tabula choose the best view to display for submissions and feedback
			</@form.label>
			<@form.label checkbox=true>
				<@f.radiobutton path="assignmentInfoView" value="table" />
				Show the expanded table view of submissions and feedback first
			</@form.label>
			<@form.label checkbox=true>
				<@f.radiobutton path="assignmentInfoView" value="summary" />
				Show the summary view of submissions and feedback first
			</@form.label>
			<@f.errors path="assignmentInfoView" cssClass="error" />
		</@form.field>
	</@form.row>
	
	<@form.row>
		<@form.label>Week numbering system</@form.label>
		<@form.field>
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
	<#if (returnTo!"")?length gt 0>
		<#assign cancelDestination=returnTo />
	<#else>
		<#assign cancelDestination><@routes.departmenthome department=department /></#assign>
	</#if>
	or <a class="btn" href="${cancelDestination}">Cancel</a>
</div>
</@f.form>
</#escape>