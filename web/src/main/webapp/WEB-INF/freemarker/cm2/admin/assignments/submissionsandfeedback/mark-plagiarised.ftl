<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>

<@cm2.assignmentHeader "Plagiarism investigation" assignment "for" />

<#assign formAction><@routes.cm2.plagiarismInvestigation assignment /></#assign>
<@f.form method="post" action=formAction commandName="command">
	<#if command.markPlagiarised>
		<#assign verb = "mark">
		<#assign message = "Items marked in this way will be held back when feedback is published.">
	<#else>
		<#assign verb = "unmark">
		<#assign message = "Items will be included when feedback is published.">
	</#if>

	<@bs3form.errors path="" />
	<@f.hidden path="markPlagiarised" />
	<input type="hidden" name="confirmScreen" value="true" />

	<@spring.bind path="students">
		<@form.errors path="students" />
		<#assign students=status.actualValue />
		<p>
			${verb?cap_first}ing <strong><@fmt.p students?size "student" /></strong> as suspected of being plagiarised.
			${message}
		</p>
		<#list students as usercode>
			<input type="hidden" name="students" value="${usercode}" />
		</#list>
	</@spring.bind>

	<@bs3form.form_group path="confirm">
		<@bs3form.checkbox path="confirm">
			<@f.checkbox path="confirm" id="confirm" />
			<#if (students?size > 1)>
				I confirm that I want to ${verb} these students' submissions as suspected of being plagiarised.
			<#else>
				I confirm that I want to ${verb} this student's submission as suspected of being plagiarised.
			</#if>
		</@bs3form.checkbox>
	</@bs3form.form_group>

	<div class="submit-buttons">
		<input class="btn btn-warning" type="submit" value="Confirm">
	</div>
</@f.form>
</#escape>