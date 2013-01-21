<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

	<#assign commandName="uploadPersonalTutorsCommand" />
		
	<@spring.bind path=commandName>
	<#assign hasErrors=status.errors.allErrors?size gt 0 />
	</@spring.bind>
	
	<@f.form method="post" action="${url('/admin/department/${department.code}/tutors')}" commandName=commandName>
	
	<#assign isfile=RequestParameters.isfile/>
	
	<#if isfile = "true">
		<#assign text_acknowledge="Your data contains tutors for "/>
		<#assign text_problems="However, there were some problems with its contents, which are shown below.
				You'll need to correct these problems with the spreadsheet and try again.
				If you choose to confirm without fixing the spreadsheet any rows with errors
				will be ignored."/>
		<#assign column_headings_warning="Remember that the first row in all spreadsheets is assumed to be column headings and ignored."/>				
	<#else>
		<#assign text_acknowledge="You are uploading tutors for "/>
		<#assign text_problems="However, there were some problems, which are shown below.
				You'll need to return to the previous page, correct these problems and try again.
				If you choose to confirm without fixing the data any rows with errors
				will be ignored."/>			
		<#assign column_headings_warning=""/>

	</#if>
	
		
	<h1>Review personal tutor changes for ${department.name}</h1>
	
	<@spring.bind path="rawStudentRelationships">
	<#assign itemsList=status.actualValue /> 
	<p>
		<#if itemsList?size gt 0>
			${text_acknowledge} <@fmt.p itemsList?size "student" /> (below). 
			<#if hasErrors>
				${text_problems}
			<#else>
				Click "Confirm" to store them.
			</#if>
		<#else>
			No rows found with recognisable tutor data. ${column_headings_warning}
		</#if>
	</p>
	</@spring.bind>
		
	<div class="submit-buttons">
		<input type="hidden" name="confirm" value="true">
		<input class="btn btn-primary" type="submit" value="Confirm">
		or <a class="btn" href="<@routes.home />">Cancel</a>
	</div>
	
	<@spring.bind path="rawStudentRelationships">
		<#assign itemList=status.actualValue />
		<#if itemList?size gt 0>
			<table class="tutorTable">
				<tr>
					<th>Student ID</th>
					<th>Student Name</th>
					<th>Tutor ID</th>
					<th>Tutor Name <span class="muted">derived from tutor ID</span></th>
					<th>Tutor Name <span class="muted">for non-University members</span></th>
				</tr>
				<#list itemList as item>
					<@spring.nestedPath path="rawStudentRelationships[${item_index}]">
						<#if !item.isValid>
							<#assign errorClass="alert-error" />
						<#elseif item.warningMessage??>
							<#assign errorClass="alert" />
						<#else>
							<#assign errorClass="alert-success" />
						</#if>
						
						<tr class="${errorClass}">
							<@f.hidden path="targetUniversityId" />
							<@f.hidden path="agentUniversityId" />
							<@f.hidden path="agentName" />
							<@f.hidden path="isValid" />
							<td>
								<@spring.bind path="targetUniversityId">
									${status.value}
								</@spring.bind>
								<@f.errors path="targetUniversityId" cssClass="error" />
							</td>
							<td>
								<@spring.bind path="targetMember.fullName">
									${status.value}
								</@spring.bind>
								<@f.errors path="targetMember.fullName" cssClass="error" />							
							</td>
							<td>
								<@spring.bind path="agentUniversityId">
									${status.value}
								</@spring.bind>
								<@f.errors path="agentUniversityId" cssClass="error" />
							</td>
							<td>
								<@spring.bind path="agentMember.fullName">
									${status.value}
								</@spring.bind>
								<@f.errors path="agentMember.fullName" cssClass="error" />							
							</td>
							<td>
								<@spring.bind path="agentNameIfNonMember">
									${status.value}
								</@spring.bind>
								<@f.errors path="agentNameIfNonMember" cssClass="error" />
							</td>
						</tr>
					</@spring.nestedPath>
				</#list>
			</table>
		</#if>
	</@spring.bind>
	
	<div class="submit-buttons">
		<input type="hidden" name="confirm" value="true">
		<input class="btn btn-primary" type="submit" value="Confirm">
		or <a class="btn" href="<@routes.home />">Cancel</a>
	</div>
	</@f.form>

</#escape>