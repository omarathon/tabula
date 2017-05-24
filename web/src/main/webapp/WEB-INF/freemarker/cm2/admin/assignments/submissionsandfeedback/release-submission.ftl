<#escape x as x?html>
<#assign formAction><@routes.cm2.releaseForMarking assignment /></#assign>

<@f.form method="post" action="${formAction}" commandName="command">

	<h1>Release submissions to markers for ${assignment.name}</h1>

	<@bs3form.errors path="" />
	<input type="hidden" name="confirmScreen" value="true" />

	<#if command.unreleasableSubmissions?has_content>
		<div class="alert alert-info">
			<a class="clickable" id="invalid-submissions"> <@fmt.p (command.unreleasableSubmissions?size ) "submission" /></a> could not be released for marking.
		</div>

		<div id="invalid-submissions-content" class="hide">
			<#if command.studentsAlreadyReleased?has_content>
				<p>Already released for marking</p>
				<ul><#list command.studentsAlreadyReleased as submission><li>${submission}</li></#list></ul>
			</#if>
			<#if command.studentsWithoutKnownMarkers?has_content>
				<p>No marker allocated</p>
				<ul><#list command.studentsWithoutKnownMarkers as submission><li>${submission}</li></#list></ul>
			</#if>
		</div>
		<script type="text/javascript">
			jQuery(function($){
				$("#invalid-submissions").popover({
					html: true,
					content: function(){return $('#invalid-submissions-content').html();},
					title: 'Could not release the following students for marking'
				});
			});
		</script>
	</#if>

	<#if command.students?size - command.unreleasableSubmissions?size != 0>

		<@spring.bind path="students">
			<#assign students = status.actualValue />
			<p>
				Releasing <strong><@fmt.p (students?size - command.unreleasableSubmissions?size ) "student" /></strong> submissions to markers.
			</p>
			<#list students as usercode><input type="hidden" name="students" value="${usercode}" /></#list>
		</@spring.bind>

		<@bs3form.form_group>
			<@bs3form.checkbox path="confirm">
				<@f.checkbox path="confirm" />
				<#if (students?size > 1)>
					I confirm that I want to release these students' submissions to markers.
				<#else>
					I confirm that I want to release this student's submission to the marker.
				</#if>
			</@bs3form.checkbox>
		</@bs3form.form_group>

		<div class="buttons">
			<input class="btn btn-primary" type="submit" value="Confirm">
			<a class="btn btn-default" href="<@routes.cm2.assignmentsubmissionsandfeedback assignment />">Cancel</a>
		</div>

	<#else>
		<div class="alert alert-info">
			None of the students that you selected can be released for marking. <a href="<@routes.cm2.assignmentsubmissionsandfeedback assignment />">Go back to the assignment summary.</a>
		</div>
	</#if>

</@f.form>
</#escape>