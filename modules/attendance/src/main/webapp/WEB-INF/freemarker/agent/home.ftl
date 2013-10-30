<#escape x as x?html>

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

<form class="form-inline" action="<@routes.agentView command.relationshipType />">
	<label>Academic year
		<select name="academicYear">
			<#assign academicYears = [command.thisAcademicYear.previous.toString, command.thisAcademicYear.toString, command.thisAcademicYear.next.toString] />
			<#list academicYears as year>
				<option <#if command.academicYear.toString == year>selected</#if> value="${year}">${year}</option>
			</#list>
		</select>
	</label>
	<button type="submit" class="btn btn-primary">Change</button>
</form>

<#if students?size == 0>
	<p><em>No students were found for this academic year.</em></p>
<#else>
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