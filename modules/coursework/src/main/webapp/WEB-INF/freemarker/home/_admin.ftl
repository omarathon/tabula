<#if nonempty(ownedDepartments) || nonempty(ownedModuleDepartments)>
	<h2>Administration</h2>
	
	<#if activities?has_content>
		<p>Here are some activities:</p>
		<ol>
			<#list activities as activity>
				<li>
					${activity.toString!"[untitled]"}
				</li>
			</#list>
		</ol>
	</#if>
</#if>

<#if nonempty(ownedDepartments)>
	<p>You're a departmental administrator.</p>
	<ul class="links">
		<#list ownedDepartments as department>
			<li>
			<@link_to_department department />
			</li>
		</#list>
	</ul>
</#if>

<#if nonempty(ownedModuleDepartments)>
	<p>You're a manager for one or more modules.</p>
	<ul class="links">
		<#list ownedModuleDepartments as department>
			<li>
			<@link_to_department department />
			</li>
		</#list>
	</ul>
</#if>