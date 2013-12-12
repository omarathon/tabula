<#escape x as x?html>

<h1>Report missed monitoring points</h1>

<#assign confirmPath><@routes.reportConfirm command.department /></#assign>
<@f.form commandName="command" action="${confirmPath}" method="GET" cssClass="form-horizontal">

	<#list ["academicYear", "period", "courseTypes", "routes", "modesOfAttendance", "yearsOfStudy", "sprStatuses", "modules"] as field>
		<@f.hidden path="${field}" />
	</#list>

	<#if students?size == 0>
		<div class="alert alert-info">
			There are no students with missed monitoring points that have not been reported for the chosen period.
		</div>
	<#else>
		<p>The following missed monitoring points will be recorded in SITS for the ${command.period} monitoring period:</p>

		<table class="table table-bordered table-striped table-condensed">
			<thead>
				<tr>
					<th>First name</th>
					<th>Last name</th>
					<th>University ID</th>
					<th>Missed points</th>
				</tr>
			</thead>
			<tbody>
				<#list students as student_pair>
					<#assign student = student_pair._1() />
					<#assign missed = student_pair._2() />
					<tr>
						<td>${student.firstName}</td>
						<td>${student.lastName}</td>
						<td>${student.universityId}</td>
						<td>${missed}</td>
					</tr>
				</#list>
			</tbody>
		</table>

		<div class="submit-buttons">
			<div class="pull-right">
				<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Loading&hellip;">
					Send report
				</button>
				<a class="btn" href="<@routes.viewDepartmentStudentsWithAcademicYear command.department command.academicYear command.serializeFilter />">Cancel</a>
			</div>
		</div>
	</#if>

</@f.form>

</#escape>