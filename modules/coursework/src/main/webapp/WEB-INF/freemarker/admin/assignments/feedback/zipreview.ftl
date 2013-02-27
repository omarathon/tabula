<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign commandName="addFeedbackCommand" />

<@spring.bind path=commandName>
<#assign hasErrors=status.errors.allErrors?size gt 0 />
<#assign hasGlobalErrors=status.errors.globalErrors?size gt 0 />
</@spring.bind>

<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/feedback/batch')}" commandName=commandName cssClass="submission-form double-submit-protection">
<input type="hidden" name="batch" value="true">

<h1>Submit feedback for ${assignment.name}</h1>

<@spring.bind path="fromArchive"><#assign fromArchive=status.actualValue /></@spring.bind>
<#if fromArchive>
<#assign verbed_your_noun="unpacked your Zip file"/>
<#else>
<#assign verbed_your_noun="received your files"/>
</#if>

<@spring.bind path="items">
<#assign itemsList=status.actualValue /> 
<p>
	<#if itemsList?size gt 0>
		I've ${verbed_your_noun} and I found feedback for ${itemsList?size} students.
		
		<#if hasErrors>
		However, there were some problems with its contents, which are shown below.
		You'll need to correct these problems with the zip and try again.
		</#if>
	<#else>
		I've ${verbed_your_noun} but I couldn't find any files that looked like feedback items.
	</#if>

</p>
</@spring.bind>

<#if hasGlobalErrors>
	<div class="alert alert-error"><@f.errors path="" cssClass="error"/></div>
<#else>


<@spring.bind path="items">
<#assign itemList=status.actualValue />
<#if itemList?size gt 0>
<table class="table table-bordered table-striped">
	<tr>
		<th>University ID</th>
		<th>Files</th>
	</tr>
<#list itemList as item>
	<tr>
	<@spring.nestedPath path="items[${item_index}]">
		<@f.hidden path="uniNumber" />
		<td>
			<@spring.bind path="uniNumber">
				${status.value}
			</@spring.bind>
			<@f.errors path="uniNumber" cssClass="error" />
			<#if item.submissionExists>
				<span class="warning">Feedback already exists for this user. New files will be added to the existing ones</span>
			</#if>
		</td>
		<#noescape>
		<@spring.bind path="file.attached" htmlEscape="false">
		<td>
			<#-- FIXME should be able to spring:bind to a list, not have to manually specify it like this -->
			<ul class="file-list">
			<#list addFeedbackCommand.items[item_index].listAttachments as attached>
				<li>
					<@f.hidden path="file.attached[${attached_index}]" />
					${attached.name}
					<@f.errors path="file" cssClass="error" />
					<#if attached.duplicate>
						<span class="warning">
							A feedback file with this name already exists for this student. It will be overwritten.
						</span>
					</#if>
				</li>
			</#list>
			</ul>
		</td>
		</@spring.bind>
		</#noescape>
	</@spring.nestedPath>
	</tr>
</#list>
</#if>
</@spring.bind>
</table>
</#if>

<div class="submit-buttons">
<#if hasErrors>
<input class="btn btn-primary" type="submit" value="Confirm" disabled="true">
<#else>
<input type="hidden" name="confirm" value="true">
<input class="btn btn-primary" type="submit" value="Confirm">
</#if>
or <a class="btn" href="<@routes.depthome module=assignment.module />">Cancel</a>
</div>
</@f.form>

</#escape>