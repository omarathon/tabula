<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#compress>

<h1>Create module</h1>
<#assign commandName="addModuleCommand" />
<#assign command=addModuleCommand />
<@f.form method="post" action="/admin/module/new" commandName=commandName cssClass="form-horizontal">
<@f.errors cssClass="error form-errors" />

	<@form.labelled_row "department" "Department code">
		<@f.input path="department" cssClass="text" />
	</@form.labelled_row>

	<@form.labelled_row "code" "Module code">
		<@f.input path="code" cssClass="text" />
	</@form.labelled_row>

	<@form.labelled_row "name" "Module name">
		<@f.input path="name" cssClass="text" />
	</@form.labelled_row>

<div class="submit-buttons">
<input type="submit" value="Create" class="btn btn-primary">
or <a class="btn" href="<@routes.home />">Cancel</a>
</div>

</@f.form>

</#compress>
</#escape>