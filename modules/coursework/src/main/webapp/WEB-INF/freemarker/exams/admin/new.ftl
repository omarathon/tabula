<#escape x as x?html>

<#if features.exams>

	<#import "*/sits_groups.ftl" as sits_groups />

	<h1>Create exam for <@fmt.module_name module /></h1>

	<#assign createExamUrl><@routes.createExam module academicYear /></#assign>

	<@f.form id="newExamForm" method="post" action="${createExamUrl}" commandName="command" cssClass="form-horizontal">

		<#include "_common_fields.ftl" />

		<div class="submit-buttons form-actions">
			<input type="submit" value="Create" class="btn btn-primary">
			<a class="btn" href="<@routes.departmentHomeWithYear module=module academicYear=academicYear />">Cancel</a>
		</div>
	</@f.form>

</#if>
</#escape>

<script>
	jQuery(function ($) {

		$('#newExamForm').on('submit',function (){
			$('.upstreamGroupsHidden').remove();
			$('.upstreamGroups:checked').each(function(i,input) {
				$('<input>', { 'class': 'upstreamGroupsHidden', type: 'hidden', name: 'upstreamGroups['+i+']', value:input.value }).appendTo('#sits-table');
			});
		});

	});
</script>