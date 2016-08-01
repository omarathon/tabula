<#escape x as x?html>

<h1>Upload missed monitoring points to SITS:eVision</h1>

<#assign confirmPath><@routes.attendance.viewReportConfirm department academicYear.startYear?c /></#assign>
<@f.form commandName="command" action="${confirmPath}" method="POST" cssClass="form-horizontal">

	<input type="hidden" name="period" value="${command.period}" />
	<input type="hidden" name="filterString" value="${command.serializeFilter}" />

	<#if studentReportCounts?size == 0>
		<div class="alert alert-info">
			All of the selected students have already been uploaded to SITS:eVision for this period.
		</div>
	<#elseif studentMissedReportCounts?size == 0>
		<div class="alert alert-info">
			None of the selected students have missed monitoring points for this period.
		</div>
	<#else>
		<#if (unrecordedStudentsCount > 0)>
			<div class="alert alert-warn">
				There <@fmt.p number=unrecordedStudentsCount singular="is" plural="are" shownumber=false />
				<@fmt.p number=unrecordedStudentsCount singular="student" shownumber=true />
				with unrecorded points during this period.
				Once these have been uploaded to SITS, it will no longer be possible to record attendance at these points.
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
				<#list studentMissedReportCounts as studentReportCount>
					<#assign student = studentReportCount.student />
					<#assign missed = studentReportCount.missed />
					<#assign unrecorded = studentReportCount.unrecorded />
					<tr>
						<input type="hidden" name="students" value="${student.universityId}" />
						<td>${student.firstName}</td>
						<td>${student.lastName}</td>
						<td>${student.universityId}</td>
						<td>${missed}<#if (unrecorded > 0)>
							<i
								class="icon-warning-sign icon-fixed-width"
								title="There <@fmt.p number=unrecorded singular="is" plural="are" shownumber=false /> ${unrecorded} unrecorded <@fmt.p number=unrecorded singular="checkpoint" shownumber=false /> for this student"
							></i>
							</#if>
						</td>
					</tr>
				</#list>
			</tbody>
		</table>

		<div class="submit-buttons">
			<div class="pull-right">
				<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Loading&hellip;">
					Upload
				</button>
				<a class="btn" href="<@routes.attendance.viewStudents department academicYear.startYear?c command.serializeFilter />">Cancel</a>
			</div>
		</div>
	</#if>

</@f.form>

</#escape>