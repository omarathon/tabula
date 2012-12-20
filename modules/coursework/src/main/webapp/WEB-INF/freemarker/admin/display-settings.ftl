<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
<h1>Display settings for ${department.name}</h1>
<@f.form method="post" class="form-horizontal" action="${url('/admin/department/${department.code}/settings/display')}" commandName="displaySettingsCommand">
	<@form.row>
		<@form.label></@form.label>
		<@form.field>
			<label class="checkbox">
				<@f.checkbox path="showStudentName" id="showStudentName" />
				Show student name with submission
			</label>
			<@f.errors path="showStudentName" cssClass="error" />
			<div class="help-block">
				
			</div>
		</@form.field>
	</@form.row>
	
<div class="submit-buttons">
	<input type="submit" value="Save" class="btn btn-primary">
	or <a class="btn" href="<@routes.departmenthome department=department />">Cancel</a>
</div>
</@f.form>
</#escape>