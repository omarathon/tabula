<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<div id="tutor-view">
	<h1>View personal tutors for ${department.name}</h1>
	
	<#if tutorRelationships?has_content>
		<table id="tutors" class="table table-bordered">
			<#list tutorRelationships?keys?sort as key>
				<#assign tutor = tutorRelationships[key]?first.agentParsed />
				<#assign tutees = tutorRelationships[key] />
				
				<tbody id="${key}">
					<tr>
						<td>
							<#if tutor?is_string>
								<h4>${tutor}</h4>
								<#if !tutor?string?starts_with("Not ")>
									<div class="muted">External to Warwick</div>
								</#if>
							<#else>
								<h4>${tutor.fullName}</h4>
							</#if>
							
							<table class="tutees table-bordered table-striped table-condensed">
								<thead>
									<tr>
										<th class="tutee-col">Tutee</th>
										<th class="type-col">Type</th>
										<th class="year-col">Year</th>
										<th class="course-col">Course</th>
									</tr>
								</thead>
								
								<tbody>
									<#list tutees as tuteeRelationship>
										<#assign student = tuteeRelationship.studentMember />
										<tr class="tutee">
											<td>
												<h6><a href="<@routes.profile student />">${student.fullName}</a></h6>
												<span class="muted">${student.universityId}</span>
											</td>
											<td>${student.groupName}</td>
											<td>${student.yearOfStudy}</td>
											<td>${student.route.name}</td>
										</tr>
									</#list>
								</tbody>
							</table>
						</td>
					</tr>
				</tbody>
			</#list>
		</table>
	<#else>
		<p class="alert alert-warning"><i class="icon-warning-sign"></i> No personal tutors are currently visible for ${department.name} in Tabula.</p>
	</#if>
</div>
</#escape>