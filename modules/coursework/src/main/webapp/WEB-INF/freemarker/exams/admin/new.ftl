<#escape x as x?html>

<#if features.exams>

	<#include "*/sits_groups.ftl" />

	<h1>Create exam for <@fmt.module_name module /></h1>

	<#assign createExamUrl><@routes.createExam module academicYear /></#assign>

	<@f.form id="newExamForm" method="post" action="${createExamUrl}" commandName="command" cssClass="form-horizontal">

		<@form.labelled_row "name" "Exam name">
			<@f.input path="name" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "" "Academic year">
		<span class="uneditable-value">${academicYear.toString} <span class="hint">(can't be changed)</span></span>
		</@form.labelled_row>

		<@exams_sits_groups command />

		<div class="submit-buttons form-actions">
			<input type="submit" value="Create" class="btn btn-primary">
			<a class="btn" href="<@routes.depthome module=module />">Cancel</a>
		</div>
	</@f.form>

</#if>
</#escape>

<script>
	jQuery(function ($) {

		$('#newExamForm').submit(function (){
			$('.upstreamGroupsHidden').remove();
			$('.upstreamGroups:checked').each(function(i,input) {
				$('<input>', { 'class': 'upstreamGroupsHidden', type: 'hidden', name: 'upstreamGroups['+i+']', value:input.value }).appendTo('#sits-table');
			});
		});

	});
</script>