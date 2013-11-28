<#escape x as x?html>

<h1>Report missed monitoring points</h1>

<@f.form commandName="command" action="" method="POST" cssClass="form-horizontal">

	<#if command.thisPeriod == command.period && command.thisAcademicYear.toString == command.academicYear.toString>
		<div class="alert alert-warn">
			<p>You have chosen to send a report for the current monitoring period, which has not yet finished.</p>
	<#else>
		<div class="alert alert-info">
	</#if>

		<p>Once you send this report you will be unable to send another report for the chosen monitoring period for the selected students.</p>

		<p>Are you sure you wish to send the report?</p>

		<p>
			<@form.label checkbox=true>
				<@f.checkbox path="confirm" /> I confirm that I want to send this report.
			</@form.label>
			<@form.errors path="confirm"/>
		</p>

	</div>

	<div class="submit-buttons">
		<div class="pull-right">
			<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Loading&hellip;">
				Send report
			</button>
			<a class="btn" href="<@routes.viewDepartmentStudentsWithAcademicYear command.department command.academicYear command.serializeFilter />">Cancel</a>
		</div>
	</div>

</@f.form>

</#escape>