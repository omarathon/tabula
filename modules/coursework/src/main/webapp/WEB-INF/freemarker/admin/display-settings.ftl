<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
<h1>Display settings for ${department.name}</h1>
<@f.form method="post" class="form-horizontal" action="${url('/admin/department/${department.code}/settings/display')}" commandName="displaySettingsCommand">
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
	
<div class="submit-buttons">
	<input type="submit" value="Save" class="btn btn-primary">
	or <a class="btn" href="<@routes.departmenthome department=department />">Cancel</a>
</div>
</@f.form>
</#escape>