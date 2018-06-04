<#escape x as x?html>

<h1>Manage marking descriptors</h1>

<table class="table table-bordered">
	<thead>
	<tr>
		<th>Class</th>
		<th>Scale</th>
		<th>Mark point</th>
		<th>University descriptor</th>
		<th>Departmental descriptor</th>
	</tr>
	</thead>
	<tbody>
	<#list markPoints as markPoint>
		<#assign universityDescriptor = mapGet(universityDescriptors, markPoint) />
		<tr>
			<td>${markPoint.markClass.name}</td>
			<td>${markPoint.name}</td>
			<td>${markPoint.mark}</td>
			<#if universityDescriptor.minMark == markPoint.mark>
				<td rowspan="${universityDescriptor.markPoints?size}">
					<p>
						${universityDescriptor.text}
					</p>
				</td>
			</#if>
			<#if mapGet(departmentDescriptors, markPoint)??>
				<#assign departmentDescriptor = mapGet(departmentDescriptors, markPoint) />
				<#if departmentDescriptor.minMark == markPoint.mark>
					<td rowspan="${departmentDescriptor.markPoints?size}">
						<p>
							${mapGet(departmentDescriptors, markPoint).text}
						</p>

						<p>
							<a href="<@routes.admin.editmarkingdescriptor department departmentDescriptor />" class="btn btn-default">Edit</a>
							<a href="<@routes.admin.deletemarkingdescriptor department departmentDescriptor />" class="btn btn-default">Delete</a>
						</p>
					</td>
				</#if>
			<#elseif !departmentDescriptor?? || departmentDescriptor.maxMark < markPoint.mark>
				<td>
					<p>
						<a href="<@routes.admin.addmarkingdescriptor department markPoint />" class="btn btn-default">Add</a>
					</p>
				</td>
			</#if>
		</tr>
	</#list>
	</tbody>
</table>

</#escape>
