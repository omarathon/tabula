<#if features.exams>
	<#escape x as x?html>

	<#import "*/sits_groups.ftl" as sits_groups />

	<#if errors??>${errors.errorCount}${errors}</#if>


	<h1>Edit exam for <@fmt.module_name exam.module /></h1>

	<#assign updateExamUrl><@routes.editExam exam /></#assign>

	<@f.form id="editExamForm" method="post" action="${updateExamUrl}" commandName="command" cssClass="form-horizontal">

		<@form.labelled_row "name" "Exam name">
			<@f.input path="name" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "" "Academic year">
		<span class="uneditable-value">${exam.academicYear.toString} <span class="hint">(can't be changed)</span></span>
		</@form.labelled_row>

		<@sits_groups.exams_sits_groups command />

		<div class="submit-buttons form-actions">
			<input type="submit" value="Update" class="btn btn-primary">
			<a class="btn" href="<@routes.departmentHomeWithYear module=exam.module academicYear=exam.academicYear />">Cancel</a>
		</div>

	</@f.form>

	</#escape>
</#if>

<script>
	jQuery(function ($) {
		$('#editExamForm').on('submit',function (){
			$('.upstreamGroupsHidden').remove();
			$('.upstreamGroups:checked').each(function(i,input) {
				$('<input>', { 'class': 'upstreamGroupsHidden', type: 'hidden', name: 'upstreamGroups['+i+']', value:input.value }).appendTo('#sits-table');
			});
		});

	});
</script>