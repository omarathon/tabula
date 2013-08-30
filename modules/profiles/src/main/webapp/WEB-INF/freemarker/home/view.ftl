<#escape x as x?html>

<#assign showMyStudents = smallGroups?has_content />
<#list relationshipTypesMap?values as has_relationship>
	<#assign showMyStudents = showMyStudents || has_relationship />
</#list>

<#if !user.loggedIn>
	<p>
		You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
		to see a personalised view.
	</p>
<#else>
	<#if isPGR><h2><a href="<@routes.profile_by_id universityId />">My Student Profile</a></h2></#if>
	<div class="row-fluid">
		<div class="span6">
			<#include "../profile/search/form.ftl" />
	
			<#if showMyStudents>
				<h2>My students</h2>
			
				<ul>
					<#list relationshipTypesMap?keys as relationshipType>
						<#if relationshipTypesMapById[relationshipType.id]>
							<li><a href="<@routes.relationship_students relationshipType />">${relationshipType.studentRole?cap_first}s</a></li>
						</#if>
					</#list>

					<#list smallGroups as smallGroup>
					<#assign _groupSet=smallGroup.groupSet />
					<#assign _module=smallGroup.groupSet.module />
					<li><a href="<@routes.smallgroup smallGroup />">
						${_module.code?upper_case} (${_module.name}) ${_groupSet.name}, ${smallGroup.name}
					</a></li>
					</#list>
				</ul>
			</#if>
		</div>
		
		<div id="profile-dept-admin" class="span6">
			<#if adminDepartments?has_content>
				<h4>Departmental administration</h4>
		
				<#list adminDepartments as dept>
				<div class="clearfix">
					<div class="btn-group pull-right">
					  <a class="btn btn-small dropdown-toggle" data-toggle="dropdown"><i class="icon-wrench"></i> Manage <span class="caret"></span></a>
					  <ul class="dropdown-menu pull-right">	
							<li><a href="<@routes.deptperms dept/>">
								<i class="icon-user"></i> Edit departmental permissions
							</a></li>
							
							<#list dept.displayedStudentRelationshipTypes as relationshipType>
								<li><a href="<@routes.relationship_agents dept relationshipType />">
									<i class="icon-eye-open"></i> ${relationshipType.description}s
								</a></li>
								<li><a href="<@routes.relationship_missing dept relationshipType />">
									<i class="icon-eye-close"></i> Students with no ${relationshipType.description}
								</a></li>
															
								<#if features.personalTutorAssignment && !relationshipType.readOnly(dept)>
									<li><a href="<@routes.relationship_allocate dept relationshipType />">
										<i class="icon-random icon-fixed-width"></i> Assign ${relationshipType.description}s</a>
									</li>
								</#if>
							</#list>
					  </ul>
					</div>
					
					<h5>${dept.name}</h5>
				</div>
				
				<#if dept_has_next><hr></#if>
				</#list>
			</#if>
		</div>
	</div>
</#if>
</#escape>
