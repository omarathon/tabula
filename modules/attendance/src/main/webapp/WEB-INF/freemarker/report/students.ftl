<#escape x as x?html>

<h1>Record missed monitoring points in SITS:eVision</h1>

<#assign confirmPath><@routes.reportConfirm command.department /></#assign>
<@f.form commandName="command" action="${confirmPath}" method="GET" cssClass="form-horizontal">

	<#list ["academicYear", "period", "courseTypes", "routes", "modesOfAttendance", "yearsOfStudy", "sprStatuses", "modules"] as field>
		<@f.hidden path="${field}" />
	</#list>

	<#if students?size == 0>
		<div class="alert alert-info">
			There are no students with missed monitoring points who have not been recorded for the chosen period.
		</div>
	<#else>
		<#if (unrecordedStudentsCount > 0)>
			<div class="alert alert-warn">
				There <@fmt.p number=unrecordedStudentsCount singular="is" plural="are" shownumber=false />  <@fmt.p number=unrecordedStudentsCount singular="student" shownumber=true />  with unrecorded points. Once these have been sent to SITS, it will no longer be possible to record these points.
			</div>
		</#if>
		<p>Record missed points in the ${command.period} monitoring period for the following students:</p>

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
				<#list students as student_triple>
					<#assign student = student_triple._1() />
					<#assign missed = student_triple._2() />
					<#assign unrecorded = student_triple._3() />
					<tr>
						<td>${student.firstName}</td>
						<td>${student.lastName}</td>
						<td>${student.universityId}</td>
						<td>${missed}<#if (unrecorded > 0)> <i class="icon-warning-sign icon-fixed-width" title="There <@fmt.p number=unrecorded singular="is" plural="are" shownumber=false /> ${unrecorded} unrecorded <@fmt.p number=unrecorded singular="checkpoint" shownumber=false /> for this student"></i></#if> </td>
					</tr>
				</#list>
			</tbody>
		</table>

		<div class="submit-buttons">
			<div class="pull-right">
				<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Loading&hellip;">
					Record
				</button>
				<a class="btn" href="<@routes.viewDepartmentStudentsWithAcademicYear command.department command.academicYear command.serializeFilter />">Cancel</a>
			</div>
		</div>
	</#if>

</@f.form>

</#escape>