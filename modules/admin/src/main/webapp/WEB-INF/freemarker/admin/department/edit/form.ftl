<#escape x as x?html>
<#compress>

<h1>Edit ${department.name}</h1>
<#assign commandName="editDepartmentCommand" />
<#assign command=editDepartmentCommand />
<#assign submitUrl><@routes.editdepartment department /></#assign>
<@f.form method="post" action="${submitUrl}" commandName=commandName cssClass="form-horizontal double-submit-protection">
<@f.errors cssClass="error form-errors" />
	<#if department.hasParent>
		<@form.labelled_row "code" "Department code">
			<@f.input path="code" cssClass="text" />
		</@form.labelled_row>
	</#if>

	<@form.labelled_row "fullName" "Department full name">
		<@f.input path="fullName" cssClass="text" />
	</@form.labelled_row>

	<@form.labelled_row "shortName" "Department short name">
		<@f.input path="shortName" cssClass="text" />
	</@form.labelled_row>

	<#if department.hasParent>
		<@form.labelled_row "filterRule" "Filter rule">
			<@f.select path="filterRule" id="filterRule">
				<@f.options items=allFilterRules itemLabel="name" itemValue="name" />
			</@f.select>
		</@form.labelled_row>
	</#if>

	<div class="submit-buttons form-actions">
		<input type="submit" value="Edit" class="btn btn-primary">
		<a class="btn" href="<@routes.home />">Cancel</a>
	</div>
</@f.form>

</#compress>
</#escape>