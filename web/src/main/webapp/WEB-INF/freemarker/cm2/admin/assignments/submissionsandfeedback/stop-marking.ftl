<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>
<#assign formAction><@routes.cm2.stopMarking assignment /></#assign>

<@f.form method="post" action="${formAction}" modelAttribute="command">

	<@cm2.assignmentHeader "Stop marking" assignment "for" />

	<@bs3form.errors path="" />
	<input type="hidden" name="confirmScreen" value="true" />

	<#if command.studentsAlreadyFinished?has_content>
		<div class="alert alert-info">
			Marking could not be stopped for <a class="clickable" id="invalid-students"> <@fmt.p (command.studentsAlreadyFinished?size ) "student" /></a>
		</div>

		<div id="invalid-students-content" class="hide">
				<ul><#list command.studentsAlreadyFinished as student><li>${student}</li></#list></ul>
		</div>
		<script type="text/javascript">
			jQuery(function($){
				$("#invalid-students").popover({
					html: true,
					content: function(){return $('#invalid-students-content').html();},
					title: 'Marking has already been completed for the following students'
				});
			});
		</script>
	</#if>

	<#if command.students?size - command.studentsAlreadyFinished?size != 0>

		<@spring.bind path="students">
			<#assign students = status.actualValue />
			<p> Stop marking for <strong><@fmt.p (students?size - command.studentsAlreadyFinished?size) "student" /></strong>. </p>
			<#list students as usercode><input type="hidden" name="students" value="${usercode}" /></#list>
		</@spring.bind>

		<@bs3form.form_group>
			<@bs3form.checkbox path="confirm">
				<@f.checkbox path="confirm" />
					I confirm that I want to stop marking for <@fmt.p number=(students?size - command.studentsAlreadyFinished?size) singular="this student" plural="these students" shownumber=false />.
			</@bs3form.checkbox>
		</@bs3form.form_group>

		<div class="buttons">
			<input class="btn btn-primary" type="submit" value="Confirm">
			<a class="btn btn-default" href="<@routes.cm2.assignmentsubmissionsandfeedback assignment />">Cancel</a>
		</div>

	<#else>
		<div class="alert alert-info">
			Marking cannot be stopped for any of the students that you selected. <a href="<@routes.cm2.assignmentsubmissionsandfeedback assignment />">Go back to the assignment summary.</a>
		</div>
	</#if>

</@f.form>
</#escape>