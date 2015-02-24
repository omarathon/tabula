<#if features.exams>
	<#escape x as x?html>
	<h1>Create exam for <@fmt.module_name module /></h1>

	<#assign createExamUrl><@routes.createExam module academicYear /></#assign>

	<@f.form method="post" action="${createExamUrl}" commandName="command" cssClass="form-horizontal">

		<@form.labelled_row "name" "Exam name">
			<@f.input path="name" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "" "Academic year">
		<span class="uneditable-value">${academicYear.toString} <span class="hint">(can't be changed)</span></span>
		</@form.labelled_row>

		<div class="submit-buttons form-actions">
			<input type="submit" value="Create" class="btn btn-primary">
			<a class="btn" href="<@routes.depthome module=module />">Cancel</a>
		</div>

	</@f.form>

	</#escape>
</#if>