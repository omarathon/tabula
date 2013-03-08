<#escape x as x?html>

	<#assign commandName="uploadPersonalTutorsCommand" />
	<#assign formDestination><@routes.tutor_upload department /></#assign> 
		
	<@spring.bind path=commandName>
		<#assign hasErrors=status.errors.allErrors?size gt 0 />
	</@spring.bind>
	
	<@f.form method="post" action=formDestination commandName=commandName>	
		<h1>Preview personal tutor changes for ${department.name}</h1>
		
		<@spring.bind path="rawStudentRelationships">
			<#assign itemList = status.actualValue />
			<#if itemList?size gt 0>
				<#if hasErrors>
					<div class="alert alert-warning alert-block">
						<h4>Your spreadsheet has problems, highlighted below.</h4>
						<p>You should fix these problems in your spreadsheet and try again.
						If you choose to confirm <i>without</i> fixing the spreadsheet any rows with errors
						will be ignored.</p>
					</div>
				<#else>
					<p>You are setting tutors for the <@fmt.p itemList?size "a tutor" "tutors" 1 0 false /> for the <b><@fmt.p itemList?size "student" /></b>
					listed below.
				</#if>
				
				<div class="submit-buttons">
					<input type="hidden" name="confirm" value="true">
					<input class="btn btn-primary" type="submit" value="Confirm">
					<a class="btn" href="<@routes.tutor_upload department />">Cancel</a>
				</div>

				<table class="table table-bordered table-condensed">
					<thead>
						<tr>
							<th>Student ID</th>
							<th>Student Name</th>
							<th>Tutor ID</th>
							<th>Tutor Name</th>
						</tr>
					</thead>
					<tbody>
					<#list itemList as item>
						<@spring.nestedPath path="rawStudentRelationships[${item_index}]">
							<@f.hidden path="targetUniversityId" />
							<@f.hidden path="agentUniversityId" />
							<@f.hidden path="agentName" />
							<@f.hidden path="isValid" />
							
							<#if !item.isValid>
								<tr class="error">
							<#else>
								<tr class="success">
							</#if>
								<td>
									${item.targetUniversityId}
								</td>
								<td>
									<#if item.targetMember?has_content>
										${item.targetMember.fullName}
									</#if>
								</td>
								<td>
									<#if item.agentUniversityId?has_content>
										${item.agentUniversityId}
									</#if>
								</td>
								<td>
									<#if item.agentName?has_content>
										${item.agentName} (no ID - assumed external)
									<#else>
										<#if item.agentMember?has_content>
											${item.agentMember.fullName}
										</#if>
									</#if>
								</td>
							</tr>
							<#if !item.isValid>
								<tr class="error"><td colspan="4"><i class="icon-warning-sign"></i> <@f.errors path="*" cssClass="" /></td></tr>
							</#if>
						</@spring.nestedPath>
					</#list>
					</tbody>
				</table>
			<#else>
				<div class="alert alert-error alert-block">
					<h4>I couldn't find any valid data.</h4>
					<ul>
						<li>Check that the first row has the required column headings.</li>
						<li>Check that there are data rows with valid student IDs, and either tutor IDs or names for each.</li>
					</ul>
				</div>
				
				<div class="submit-buttons">
					<a class="btn" href="<@routes.home />">Cancel</a>
				</div>
			</#if>
		</@spring.bind>
	</@f.form>
</#escape>