<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#assign formAction><@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/release-submissions'/></#assign>

<@f.form method="post" action="${formAction}" commandName="releaseForMarkingCommand">

	<h1>Release submissions to markers for ${assignment.name}</h1>

	<@form.errors path="" />
	<input type="hidden" name="confirmScreen" value="true" />

	<@spring.bind path="students">
		<@form.errors path="students" />
		<#assign students=status.actualValue />

		<#if releaseForMarkingCommand.invalidSubmissions?has_content>
			<p>
				<a href="" class="invalid-submissions"><@fmt.p (releaseForMarkingCommand.invalidSubmissions?size ) "submission" /></a>
				could not be released for marking. Marked submissions cannot be re-released.
			</p>
			<div class="hidden invalid-submissions-list">
				<ul><#list releaseForMarkingCommand.invalidSubmissions as submission>
					<li>${submission.universityId}</li>
				</#list></ul>
			</div>
			<script>
				var listHtml = jQuery(".invalid-submissions-list").html();
				jQuery(".invalid-submissions").on('click',function(e){e.preventDefault()})
				jQuery(".invalid-submissions").tooltip({
					html: true,
					placement: 'right',
					title: listHtml
				});
			</script>
		</#if>
		<p>
			Releasing <strong><@fmt.p (students?size - releaseForMarkingCommand.invalidSubmissions?size ) "student" /></strong> submissions to markers.
		</p>
		<#list students as uniId>
			<input type="hidden" name="students" value="${uniId}" />
		</#list>
	</@spring.bind>

	<p>
		<@form.errors path="confirm" />
		<@form.label checkbox=true><@f.checkbox path="confirm" />
			<#if (students?size > 1)>
				I confirm that I want to release these students' submissions to markers.
			<#else>
				I confirm that I want to release this student's submission to the marker.
			</#if>
		</@form.label>
	</p>

	<div class="submit-buttons">
		<input class="btn btn-warn" type="submit" value="Confirm">
	</div>
</@f.form>
</#escape>