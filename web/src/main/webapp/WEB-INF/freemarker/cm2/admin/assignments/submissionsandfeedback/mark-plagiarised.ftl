<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>

<@cm2.assignmentHeader "Plagiarism investigation" assignment "for" />

<#assign formAction><@routes.cm2.plagiarismInvestigation assignment /></#assign>
<@f.form method="post" action=formAction commandName="command">
	<@bs3form.errors path="" />
	<@f.hidden path="markPlagiarised" />
	<input type="hidden" name="confirmScreen" value="true" />

	<@spring.bind path="students">
		<@form.errors path="students" />
		<#assign students=status.actualValue />
		<#list students as usercode>
			<input type="hidden" name="students" value="${usercode}" />
		</#list>
	</@spring.bind>

	<@bs3form.form_group path="confirm">
		<@bs3form.checkbox path="confirm">
			<@f.checkbox path="confirm" id="confirm" />
			<#if command.markPlagiarised>
				I confirm that I wish to flag <@fmt.p students?size "submission" /> as being suspected of plagiarism. When publishing feedback to students, flagged submissions are held back.
			<#else>
				I confirm that I wish to remove the suspected plagiarism flag from <@fmt.p students?size "submission" />. When publishing feedback to students, submissions without the flag are included.
			</#if>
		</@bs3form.checkbox>
	</@bs3form.form_group>

	<div class="submit-buttons">
		<input class="btn btn-warning" type="submit" value="Confirm">
	</div>
</@f.form>
</#escape>