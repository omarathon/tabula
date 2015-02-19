<#if features.exams>

	<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
	<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
	<#escape x as x?html>
	<h1>Create exam for <@fmt.module_name module /></h1>

	<@f.form method="post" action="${url('/exams/admin/module/${module.code}/exam/new')}" commandName="command" cssClass="form-horizontal">

	<@form.labelled_row "name" "Exam name">
		<@f.input path="name" cssClass="text" />
	</@form.labelled_row>

	<@form.labelled_row "academicYear" "Academic year">
		<@f.select path="academicYear" id="academicYear">
			<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
		</@f.select>
	</@form.labelled_row>

	<div class="submit-buttons form-actions">
		<input type="submit" value="Create" class="btn btn-primary">
		<a class="btn" href="<@routes.depthome module=module />">Cancel</a>
	</div>

	</@f.form>

	</#escape>
</#if>