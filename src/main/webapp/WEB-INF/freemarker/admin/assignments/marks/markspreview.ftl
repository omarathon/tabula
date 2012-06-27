<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

	<#assign commandName="addMarksCommand" />
	
	<@spring.bind path=commandName>
	<#assign hasErrors=status.errors.allErrors?size gt 0 />
	</@spring.bind>
	
	<@f.form method="post" action="/admin/module/${module.code}/assignments/${assignment.id}/marks" commandName=commandName>
	
	<h1>Submit marks for ${assignment.name}</h1>
	<#assign verbed_your_noun="received your files"/>
	
	<@spring.bind path="marks">
	<#assign itemsList=status.actualValue /> 
	<p>
		<#if itemsList?size gt 0>
			I've ${verbed_your_noun} and I found marks for ${itemsList?size} students.
			<#if hasErrors>
				However, there were some problems with its contents, which are shown below.
				You'll need to correct these problems with the spreadsheet and try again.
				If you choose to confirm without fixing the spreadsheet any rows with errors
				will be ignored.
			</#if>
		<#else>
			I've ${verbed_your_noun} but I couldn't find any rows that looked like marks. Remember that the first row in all spreadsheets is assumed to be column headings and ignored.
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
							<#assign errorClass="errorRow" />
						<#else>
							<#assign errorClass="" />
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