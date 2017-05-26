<#import "*/modal_macros.ftl" as modal />
<#import "*/cm2_macros.ftl" as cm2 />

<#escape x as x?html>
	<#function route_function dept>
		<#local result><@routes.cm2.feedbackreport dept academicYear /></#local>
		<#return result />
	</#function>
	<@cm2.departmentHeader "Feedback report" department route_function academicYear />

	<#assign actionUrl><@routes.cm2.feedbackreport department /></#assign>
	<@f.form method="post" action=actionUrl commandName="feedbackReportCommand" cssClass="dirty-check">
		<p>Generate a feedback report for assignments with closing dates:</p>

		<@bs3form.labelled_form_group labelText="From" path="startDate">
			<@f.input path="startDate" cssClass="form-control date-time-picker" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group labelText="To" path="endDate">
			<@f.input path="endDate" cssClass="form-control date-time-picker" />
		</@bs3form.labelled_form_group>

		<div class="submit-buttons">
			<input type="submit" value="Generate report" class="btn btn-primary">
			<a class="btn btn-default" href="<@routes.cm2.departmenthome department />">Cancel</a>
		</div>
	</@f.form>
</#escape>