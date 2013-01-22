<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

	<#assign commandName="adminAddMarksCommand" />
	<#assign verbed_your_noun="received your files"/>
		
	<@spring.bind path=commandName>
	<#assign hasErrors=status.errors.allErrors?size gt 0 />
	</@spring.bind>
	
	<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/marks')}" commandName=commandName>
	
	<#assign isfile=RequestParameters.isfile/>
	
	<#if isfile = "true">
		<#assign text_acknowledge="I've ${verbed_your_noun} and I found marks for"/>
		<#assign text_problems="However, there were some problems with its contents, which are shown below.
				You'll need to correct these problems with the spreadsheet and try again.
				If you choose to confirm without fixing the spreadsheet any rows with errors
				will be ignored."/>
		<#assign column_headings_warning="Remember that the first row in all spreadsheets is assumed to be column headings and ignored."/>				
	<#else>
		<#assign text_acknowledge="You are submitting marks for "/>
		<#assign text_problems="However, there were some problems, which are shown below.
				You'll need to return to the previous page, correct these problems and try again.
				If you choose to confirm without fixing the data any rows with errors
				will be ignored."/>			
		<#assign column_headings_warning=""/>

	</#if>
	
		
	<h1>Submit marks for ${assignment.name}</h1>
	<#assign verbed_your_noun="received your files"/>
	
	<@spring.bind path="marks">
	<#assign itemsList=status.actualValue /> 
	<p>
		<#if itemsList?size gt 0>
			${text_acknowledge} ${itemsList?size} students.  
			<#if hasErrors>
				${text_problems}
			</#if>
		<#else>
			I've ${verbed_your_noun} but I couldn't find any rows that looked like marks. ${column_headings_warning}
		</#if>
	</p>
	</@spring.bind>
		
	<@spring.bind path="marks">
		<#assign itemList=status.actualValue />
		<#if itemList?size gt 0>
			<table class="marksUploadTable">
				<tr>
					<th>University ID</th>
					<th>Marks</th>
					<th>Grade</th>
				</tr>
				<#list itemList as item>
					<@spring.nestedPath path="marks[${item_index}]">
						<#if !item.isValid>
							<#assign errorClass="alert-error" />
						<#elseif item.warningMessage??>
							<#assign errorClass="alert" />
						<#else>
							<#assign errorClass="alert-success" />
						</#if>
						
						<tr class="${errorClass}">
							<@f.hidden path="universityId" />
							<@f.hidden path="actualMark" />
							<@f.hidden path="actualGrade" />
							<@f.hidden path="isValid" />
							<td>
								<@spring.bind path="universityId">
									${status.value}
								</@spring.bind>
								<@f.errors path="universityId" cssClass="error" />
								
								<#if item.warningMessage??>
								     <div class="warning">
								     	${item.warningMessage}
								     </div>
								</#if>
							</td>
							<td>
								<@spring.bind path="actualMark">
									${status.value}
								</@spring.bind>
								<@f.errors path="actualMark" cssClass="error" />
							</td>
							<td>
								<@spring.bind path="actualGrade">
									${status.value}
								</@spring.bind>
								<@f.errors path="actualGrade" cssClass="error" />
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
		or <a class="btn" href="<@routes.depthome module=assignment.module />">Cancel</a>
	</div>
	</@f.form>

</#escape>