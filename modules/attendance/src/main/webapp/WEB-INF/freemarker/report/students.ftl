<#escape x as x?html>

<h1>Upload missed monitoring points to SITS:eVision</h1>

<#assign confirmPath><@routes.reportConfirm command.department /></#assign>
<@f.form commandName="command" action="${confirmPath}" method="GET" cssClass="form-horizontal">

	<#list ["academicYear", "period", "courseTypes", "routes", "modesOfAttendance", "yearsOfStudy", "sprStatuses", "modules"] as field>
		<@f.hidden path="${field}" />
	</#list>

	<#if studentReportStatuses?size == 0>
		<div class="alert alert-info">
			All students with missed monitoring points during this period have already been uploaded to SITS:eVision.
		</div>
	<#else>
		<#if (unrecordedStudentsCount > 0)>
			<div class="alert alert-warn">
				There <@fmt.p number=unrecordedStudentsCount singular="is" plural="are" shownumber=false />  <@fmt.p number=unrecordedStudentsCount singular="student" shownumber=true />  with unrecorded points during this period. Once these have been uploaded to SITS, it will no longer be possible to record attendance at these points.
			</div>
		</#if>
		<p>Upload missed points in the ${command.period} monitoring period for the following students:</p>

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
				<#list studentReportStatuses as studentReportStatus>
					<#assign student = studentReportStatus.student />
					<#assign missed = studentReportStatus.unreported />
					<#assign unrecorded = studentReportStatus.unrecorded />
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
					Upload
				</button>
				<a class="btn" href="<@routes.viewDepartmentStudentsWithAcademicYear command.department command.academicYear command.serializeFilter />">Cancel</a>
			</div>
		</div>
	</#if>

</@f.form>

</#escape>