<#escape x as x?html>

<h1>Manage marking descriptors</h1>

<#assign universityDescriptorPopoverContent>
	<p>
		These are the generic descriptors for the 20-point undergraduate marking scale provided by <a target="_blank" href="https://warwick.ac.uk/quality/categories/examinations/marking/ug2017/staff">Teaching Quality</a> for reference. Departments should create their own descriptors to explain the discipline-specific skills and knowledge that a student is required to demonstrate to achieve the respective points on the scale.
	</p>
</#assign>

<#assign departmentalDescriptorPopoverContent>
	<p>
		Enter your department's descriptors for the 20-point undergraduate marking scale. The descriptors should explain the discipline-specific skills and knowledge that a student is required to demonstrate to achieve the respective points on the scale. For guidance on when and how to use the marking scale, visit the <a target="_blank" href="https://warwick.ac.uk/quality/categories/examinations/marking/ug2017/staff">Teaching Quality</a> website.
	</p>
</#assign>

<table class="table table-bordered">
	<thead>
	<tr>
		<th style="width: 10%">Class</th>
		<th style="width: 10%">Scale</th>
		<th style="width: 10%">Mark point</th>
		<th style="width: 35%">
			University descriptor 
			<@fmt.help_popover id="help-universityDescriptor" content=universityDescriptorPopoverContent html=true />
		</th>
		<th style="width: 35%">
			Departmental descriptor 
			<@fmt.help_popover id="help-departmentalDescriptor" content=departmentalDescriptorPopoverContent html=true />
		</th>
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
