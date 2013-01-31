<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<div id="tutor-view">
	<h1>View personal tutors for ${department.name}</h1>
	
	<#if tutorRelationships?has_content>
		<#assign oldTutor = "dummyForGrouping" />
		
		<table id="tutors" class="table table-striped table-bordered table-condensed">
			<#list tutorRelationships as rel>
				<#assign tutor = rel.agentParsed />
				<#assign student = rel.target />
				
				<#if rel.agent != oldTutor>
					<#-- start a new tbody for each tutor -->
					<tbody id="${rel.agent}">
						<tr>
							<#if tutor?is_string>
								<td>
									<#-- TODO: need a default photo here -->
								</td>
								<td>
									<h5>${tutor}</h5>
									<#if !tutor?string?starts_with("Not ")>
										<div class="muted">External to Warwick</div>
									</#if>
								</td>
							<#else>
								<td class="tutor-photo">
									<div class="photo">
										<img src="<@routes.photo tutor />" />
									</div>
								</td>
								<td>
									<h5>${tutor.fullName}</h5>
								</td>
							</#if>
						</tr>
						<tr>
							<td></td>
							<th>Tutee</th>
							<th>Year</th>
							<th>Course</th>
						</tr>
				</#if>
				
				<tr class="tutee">
					<td></td>
					<td>
						<div class="photo">
							<img src="<@routes.photo student />" />
						</div>
						<a href="<@routes.profile student />">${student.fullName}</a>
					</td>
					<td>${student.yearOfStudy}</td>
					<td>${student.route.name}</td>
				</tr>
								
				<#if rel.agent != oldTutor>
					</tbody>
					<#assign oldTutor = rel.agent />
				</#if>
			</#list>
		</table>
	<#else>
		<p class="alert alert-warning"><i class="icon-warning-sign"></i> No personal tutors are currently visible for ${department.name} in Tabula.</p>
	</#if>
</div>
</#escape>