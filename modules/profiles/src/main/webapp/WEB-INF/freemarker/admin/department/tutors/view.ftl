<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<div id="tutor-view">
	<h1>View personal tutors for ${department.name}</h1>
	
	<#if tutorRelationships?has_content>
		<table id="tutors" class="table table-striped table-bordered table-condensed">
			<#list tutorRelationships?keys?sort as key>
				<#assign tutor = tutorRelationships[key]?first.agentParsed />
				<#assign tutees = tutorRelationships[key] />
				
				<tbody id="${key}">
					<tr>
						<#if tutor?is_string>
							<td>
								<h4>${tutor}</h4>
								<#if !tutor?string?starts_with("Not ")>
									<div class="muted">External to Warwick</div>
								</#if>
							</td>
						<#else>
							<td>
								<h4>${tutor.fullName}</h4>
							</td>
						</#if>
					</tr>
					<tr>
						<th>Tutee</th>
						<th>Year</th>
						<th>Course</th>
					</tr>
					
					<#list tutees as tuteeRelationship>
						<#assign student = tuteeRelationship.studentMember />
						<tr class="tutee">
							<td><a href="<@routes.profile student />">${student.fullName}</a></td>
							<td>${student.yearOfStudy}</td>
							<td>${student.route.name}</td>
						</tr>
					</#list>
				</tbody>
			</#list>
		</table>
	<#else>
		<p class="alert alert-warning"><i class="icon-warning-sign"></i> No personal tutors are currently visible for ${department.name} in Tabula.</p>
	</#if>
</div>
</#escape>