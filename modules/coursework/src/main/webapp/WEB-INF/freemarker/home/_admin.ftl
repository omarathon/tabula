<#if nonempty(ownedDepartments) || nonempty(ownedModuleDepartments)>
	<h2>Administration</h2>

	<div class="row-fluid">
		<div class="span6">
			<h6>Late &amp; suspicious activity</h6>
			
			<#if activities?has_content>
				<#macro admin_href submission>
					<@url page='/admin/module/${submission.assignment.module.code}/assignments/${submission.assignment.id}/submissionsandfeedback/list' />
				</#macro>
				
				<table class="table table-condensed table-hover stream">
					<tbody>
						<#list activities as activity>
							<tr>
								<td>
									<div class="pull-right"><@fmt.date date=activity.date at=true /></div>
									
									<div>
										<#if activity.entityType == "Submission">
											<a href="<@admin_href activity.entity />"><b>${activity.title}</b> by ${activity.agent.warwickId}</a>

											<#if activity.entity.late>
												<span class="label-red">Late</span>
											<#elseif activity.entity.authorisedLate>
												<span class="label-blue">Authorised Late</span>
											</#if>
											<#if activity.entity.suspectPlagiarised>
												<span class="label-orange">Suspect Plagiarised</span>
											</#if>
										<#else>
											<#-- default -->
											<b>${activity.title}</b> by ${activity.agent.warwickId}
										</#if>
									</div>
									
									<div class="activity-message">
										<#if activity.entityType == "Submission">
											<@fmt.assignment_name activity.entity.assignment />
										<#elseif activity.entityType == "Assignment">
											<@fmt.assignment_name activity.entity />
										<#else>
											${activity.message}
										</#if>
									</div>
								</td>
							<tr>
						</#list>
					</tbody>
				</table>
				</ol>
			<#else>
				<p class="alert">There is no notable activity to show you right now.</p>
			</#if>
		</div>
		
		<div class="span6">
			<#if nonempty(ownedModuleDepartments)>
				<h6>My managed <@fmt.p number=ownedModuleDepartments?size singular="module" shownumber=false /></h6>
				
				<ul class="links">
					<#list ownedModuleDepartments as department>
						<li>
							<@link_to_department department />
						</li>
					</#list>
				</ul>
			</#if>

			<#if nonempty(ownedDepartments)>
				<h6>My department-wide <@fmt.p number=ownedDepartments?size singular="responsibility" plural="responsibilities" shownumber=false /></h6>
			
				<ul class="links">
					<#list ownedDepartments as department>
						<li>
							<@link_to_department department />
						</li>
					</#list>
				</ul>
			</#if>
		</div>
	</div>
</#if>