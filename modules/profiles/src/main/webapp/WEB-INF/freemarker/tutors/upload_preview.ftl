<#escape x as x?html>

	<#assign formDestination><@routes.tutors_allocate department /></#assign>
	<#assign mappingById=allocateStudentsToTutorsCommand.mappingById />

	<@spring.bind path="allocateStudentsToTutorsCommand">
		<#assign hasErrors=status.errors.allErrors?size gt 0 />
	</@spring.bind>

	<@f.form method="post" action=formDestination commandName="allocateStudentsToTutorsCommand">
		<h1>Preview personal tutor changes for ${department.name}</h1>
		
		<#include "_allocate_notifications_modal.ftl" />

		<div class="submit-buttons">
			<input type="hidden" name="confirm" value="true">
			<button type="button" class="btn btn-primary" data-toggle="modal" data-target="#notify-modal">Confirm</button>
			<a class="btn" href="<@routes.tutors_allocate department />">Cancel</a>
		</div>

		<table class="table table-bordered table-condensed table-striped">
			<thead>
				<tr>
					<th>Student ID</th>
					<th>Student Name</th>
					<th>Tutor ID</th>
					<th>Tutor Name</th>
				</tr>
			</thead>
			<tbody>
				<#macro noTutor path student>
					<tr class="error">
						<td>
							<@f.hidden path=path value=(student.universityId)! />
							${(student.universityId)!}
						</td>
						<td>
							${(student.fullName)!}
						</td>
						<td>
							
						</td>
						<td>
							
						</td>
					</tr>
				</#macro>
				
				<#macro hasTutor path student tutor>
					<tr>
						<td>
							<@f.hidden path=path value=(student.universityId)! />
							${(student.universityId)!}
						</td>
						<td>
							${(student.fullName)!}
						</td>
						<td>
							${(tutor.universityId)!}
						</td>
						<td>
							${(tutor.fullName)!}
						</td>
					</tr>
				</#macro>
				
				<#list allocateStudentsToTutorsCommand.unallocated as student>
					<@noTutor "unallocated[${student_index}]" student />
				</#list>
				
				<#list allocateStudentsToTutorsCommand.mapping?keys as tutor>
					<#assign existingStudents = mappingById[tutor.universityId]![] />
					
					<#list existingStudents as student>
						<@hasTutor "mapping[${tutor.universityId}][${student_index}]" student tutor />
					</#list>
				</#list>
			</tbody>
		</table>
	</@f.form>
</#escape>