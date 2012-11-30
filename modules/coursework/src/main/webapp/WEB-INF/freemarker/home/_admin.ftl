<#if nonempty(ownedDepartments) || nonempty(ownedModuleDepartments)>
	<h2>Administration</h2>
	
	<div class="row-fluid">
		<div class="span6">
			<h6 class="muted">Activities</h6>
			
			<#if activities?has_content>
				<table class="table table-condensed table-hover">
					<caption>Recent activity</caption>
					
					<tbody>
						<#list activities as activity>
							<tr>
								<td>
									<b>${activity.title}</b>
									<br>
									<#-- For <@fmt.assignment_link activity.submission.assignment /> -->
									${activity.message}
									by ${activity.agent.warwickId}
								</td>
								<td style="vertical-align:bottom;">
									<@fmt.date date=activity.date at=true />
								</td>
							<tr>
							<!--
								${activity.toString!"[untitled]"}
							-->
						</#list>
					</tbody>
				</table>
				</ol>
			<#else>
				<p class="alert">There is no relevant activity to show you right now.</p>
			</#if>
		</div>
		
		<div class="span6">
			<#if nonempty(ownedModuleDepartments)>
				<h6 class="muted">My managed <@fmt.p number=ownedModuleDepartments?size singular="module" shownumber=false /></h6>
				
				<ul class="links">
					<#list ownedModuleDepartments as department>
						<li>
							<@link_to_department department />
						</li>
					</#list>
				</ul>
			</#if>

			<#if nonempty(ownedDepartments)>
				<h6 class="muted">My department-wide <@fmt.p number=ownedDepartments?size singular="responsibility" plural="responsibilities" shownumber=false /></h6>
			
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