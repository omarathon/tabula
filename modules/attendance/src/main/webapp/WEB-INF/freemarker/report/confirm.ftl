<#escape x as x?html>

<h1>Upload missed monitoring points</h1>

<@f.form commandName="command" action="" method="POST" cssClass="form-horizontal">


	<#if command.thisPeriod == command.period && command.thisAcademicYear.toString == command.academicYear.toString>
		<div class="alert alert-warn">
			<p>You have chosen to upload points for the current monitoring period, which has not yet finished.</p>
		</div>
	</#if>

	<div class="alert alert-info">
	<p>Once these points are uploaded to SITS you won't be able to change this information via Tabula.</p>
	<p>If information does need to be amended later, please contact the Administrative Officer for <a href="http://www2.warwick.ac.uk/studentrecords"> Student Records</a>.</p>

	<p>Are you sure you wish to upload these missed points?</p>

		<p>
			<@form.label checkbox=true>
				<@f.checkbox path="confirm" /> I confirm that I want to upload these missed points.
			</@form.label>
			<@form.errors path="confirm"/>
		</p>

	</div>

	<div class="submit-buttons">
		<div class="pull-right">
			<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Loading&hellip;">
				Upload
			</button>
			<a class="btn" href="<@routes.viewDepartmentStudentsWithAcademicYear command.department command.academicYear command.serializeFilter />">Cancel</a>
		</div>
	</div>

</@f.form>

</#escape>