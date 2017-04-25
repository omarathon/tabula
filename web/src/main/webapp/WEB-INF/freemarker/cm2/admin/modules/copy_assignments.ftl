<#escape x as x?html>
	<h1>Create assignments from previous for ${title}</h1>

	<form action="" method="post" class="copy-assignments">

		<div class="submit-buttons">
			<input class="btn btn-primary" type="submit" value="Confirm">
			<#assign cancelDestination><@routes.cm2.departmenthome department /></#assign>
			<a class='btn btn-default' href='${cancelDestination}'>Cancel</a>
		</div>

		<#assign modules = copyAssignmentsCommand.modules />
		<#assign path = "copyAssignmentsCommand.assignments" />
		<#include "_assignment_list.ftl" />

		<@bs3form.labelled_form_group path="copyAssignmentsCommand.academicYear" labelText="Set academic year">
				<@f.select path="copyAssignmentsCommand.academicYear" id="academicYearSelect" cssClass="form-control">
					<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
				</@f.select>
			<div class="help-block">
				The new assignments' open and close dates will be offset by the appropriate number of years. You should check the open and close dates
				of all new assignments.
			</div>
		</@bs3form.labelled_form_group>


		<div class="submit-buttons">
			<input class="btn btn-primary" type="submit" value="Confirm">
			<a class='btn btn-default' href='${cancelDestination}'>Cancel</a>
		</div>

	</form>
</#escape>