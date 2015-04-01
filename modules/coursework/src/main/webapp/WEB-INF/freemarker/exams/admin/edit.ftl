<#if features.exams>
	<#escape x as x?html>

	<#import "*/sits_groups.ftl" as sits_groups />

	<h1>Edit exam for <@fmt.module_name exam.module /></h1>

	<#assign updateExamUrl><@routes.editExam exam /></#assign>

	<@f.form id="editExamForm" method="post" action="${updateExamUrl}" commandName="command" cssClass="form-horizontal">

		<#include "_common_fields.ftl" />

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