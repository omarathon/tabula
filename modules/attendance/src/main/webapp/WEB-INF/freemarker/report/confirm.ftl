<#escape x as x?html>

<h1>Record missed monitoring points</h1>

<@f.form commandName="command" action="" method="POST" cssClass="form-horizontal">
	<div class="alert alert-warn">
	<#if command.thisPeriod == command.period && command.thisAcademicYear.toString == command.academicYear.toString>
			<p>You have chosen to send a report for the current monitoring period, which has not yet finished.</p>
	</#if>

	<p>Once these points are recorded in SITS:eVision you won't be able to change this information via Tabula.</p>
	<p>If information does need to be amended later, please contact the Administrative Officer for <a href="http://www2.warwick.ac.uk/studentrecords"> Student Records</a>.</p>

	<p>Are you sure you wish to record these missed points?</p>

		<p>
			<@form.label checkbox=true>
				<@f.checkbox path="confirm" /> I confirm that I want to record these missed points.
			</@form.label>
			<@form.errors path="confirm"/>
		</p>

	</div>

	<div class="submit-buttons">
		<div class="pull-right">
			<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Loading&hellip;">
				Record points
			</button>
			<a class="btn" href="<@routes.viewDepartmentStudentsWithAcademicYear command.department command.academicYear command.serializeFilter />">Cancel</a>
		</div>
	</div>

</@f.form>

</#escape>