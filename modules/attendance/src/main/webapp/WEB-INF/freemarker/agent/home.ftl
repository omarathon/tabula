<#escape x as x?html>

<#import "../attendance_macros.ftl" as attendance_macros />

<#macro row student missedPoints>
	<tr class="student">
		<td>
			<@fmt.member_photo student "tinythumbnail" />
		</td>
		<td><h6>${student.firstName}</h6></td>
		<td><h6>${student.lastName}</h6></td>
		<td><a class="profile-link" href="<@routes.profile student />">${student.universityId}</a></td>
		<td>${student.groupName}</td>
		<td>${(student.mostSignificantCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!""}</td>
		<td>${(student.mostSignificantCourseDetails.route.name)!""}</td>
		<td><span class="badge badge-<#if (missedPoints > 2)>important<#elseif (missedPoints > 0)>warning<#else>success</#if>">${missedPoints}</span></td>
		<td><a class="btn btn-small btn-primary" href="<@routes.agentStudentView student command.relationshipType />">View &amp; record</a></td>
	</tr>
</#macro>

<h1>My ${command.relationshipType.studentRole}s</h1>

<#if students?size == 0>
	<p><em>No ${command.relationshipType.studentRole}s were found.</em></p>
<#else>

	<#assign thisPath><@routes.agentView command.relationshipType /></#assign>
	<@attendance_macros.academicYearSwitcher thisPath command.academicYear command.thisAcademicYear />

	<table class="students table table-bordered table-striped table-condensed">
		<thead>
		<tr>
			<th class="photo-col">Photo</th>
			<th class="student-col">First name</th>
			<th class="student-col">Last name</th>
			<th class="id-col">ID</th>
			<th class="type-col">Type</th>
			<th class="year-col">Year</th>
			<th class="course-but-photo-col">Course</th>
			<th class="missed-points-col" title="Missed monitoring points">Missed</th>
			<th></th>
		</tr>
		</thead>

		<tbody>
			<#list students as item>
				<@row item._1() item._2() />
			</#list>
		</tbody>
	</table>

	<script>
		jQuery(function($){
			$(".students").tablesorter({
				sortList: [[2,0], [4,0], [5,0]],
				headers: { 0:{sorter:false}, 8:{sorter:false} }
			});
		});
	</script>
</#if>
</#escape>