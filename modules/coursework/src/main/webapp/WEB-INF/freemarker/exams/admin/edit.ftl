<#if features.exams>
	<#escape x as x?html>
	<h1>Edit exam for <@fmt.module_name exam.module /></h1>

	<@f.form method="post" action="${url('/exams/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/edit')}" commandName="command" cssClass="form-horizontal">

		<@form.labelled_row "name" "Exam name">
			<@f.input path="name" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "" "Academic year">
		<span class="uneditable-value">${exam.academicYear.toString} <span class="hint">(can't be changed)</span></span>
		</@form.labelled_row>

		<div class="submit-buttons form-actions">
			<input type="submit" value="Update" class="btn btn-primary">
			<a class="btn" href="<@routes.depthome module=exam.module />">Cancel</a>
		</div>

	</@f.form>

	</#escape>
</#if>