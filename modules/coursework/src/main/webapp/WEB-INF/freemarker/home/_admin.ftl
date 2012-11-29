<#if nonempty(ownedDepartments) || nonempty(ownedModuleDepartments)>
	<h2>Administration</h2>
	
	<div class="row">
		<div class="span6">
			<h3>Activities</h3>
			
			<#if activities?has_content>
				<table class="table table-condensed table-hover">
					<caption>Recent activity</caption>
					
					<tbody>
						<#list activities as activity>
							<#-- Do some conversion -->
							<@userlookup id=activity.id>
							<#if returned_user.foundUser>
								<assign uni_id=${returned_user.warwickId} />
							<#else>
								<assign uni_id="[unknown]" />
							</#if>
						
							<tr>
								<td>
									<b>New submission</b>
									<br>
									For <@fmt.assignment_link activity.submission.assignment />
									by ${uni_id}
								</td>
								<td style="vertical-align:bottom;">
									<@fmt.date activity.date at=true />
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
				<h3>My managed <@fmt.p ownedModuleDepartments.length "module" shownumber=false /></h3>
				
				<ul class="links">
					<#list ownedModuleDepartments as department>
						<li>
							<@link_to_department department />
						</li>
					</#list>
				</ul>
			</#if>

			<#if nonempty(ownedDepartments)>
				<h3>My department-wide <@fmt.p ownedDepartments.length "responsibility" "responsibilities" shownumber=false /></h3>
			
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