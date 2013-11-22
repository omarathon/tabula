<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#compress>

<h1>Create sub-department in ${department.name}</h1>
<#assign commandName="addSubDepartmentCommand" />
<#assign command=addSubDepartmentCommand />
<#assign submitUrl><@routes.createsubdepartment department /></#assign>
<@f.form method="post" action="${submitUrl}" commandName=commandName cssClass="form-horizontal">
<@f.errors cssClass="error form-errors" />
	<@form.labelled_row "code" "Department code">
		<@f.input path="code" cssClass="text" />
	</@form.labelled_row>

	<@form.labelled_row "name" "Department name">
		<@f.input path="name" cssClass="text" />
	</@form.labelled_row>
	
	<@form.labelled_row "filterRule" "Filter rule">
		<@f.select path="filterRule" id="filterRule">
			<@f.options items=allFilterRules itemLabel="name" itemValue="name" />
		</@f.select>
	</@form.labelled_row>

	<div class="submit-buttons form-actions">
	<input type="submit" value="Create" class="btn btn-primary">
	<a class="btn" href="<@routes.home />">Cancel</a>
	</div>
</@f.form>

</#compress>
</#escape>