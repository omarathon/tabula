<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign time_remaining=durationFormatter(assignment.closeDate) />

<div>
    <h1>Authorise late submissions for ${assignment.name}</h1>
    <p>
		This assignment <@fmt.tense assignment.closeDate "closes" "closed" /> 
		on <strong><@fmt.date date=assignment.closeDate /> (${time_remaining})</strong>.
		To authorise an extension for a student click the "Grant" button next to the student's University ID. If you
		wish to grant an extension to a user that is not listed below, please ensure that they appear in the students
		list on the <a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/edit" />">assignment edit page</a>.
    </p>
    <div class="alert alert-info">
    	<i class="icon-envelope"></i> Students will automatically be notified by email when you grant, modify or revoke an extension.
    </div>
    <div id="extension-list">
        <table class="extensionListTable table table-striped table-bordered">
            <tr class="extension-header">
				<th>Name</th>
				<th>Status</th>
				<th>New deadline</th>
				<th><!-- Actions column--></th>
			</tr>

			<#if RequestParameters.highlight??>
				<#assign highlightId = RequestParameters.highlight>
			<#else>
				<#assign highlightId = "">
			</#if>

			<!-- list extension requests -->
			<#if extensionRequests??>
				<#list extensionRequests as extension>
					<#if (highlightId == extension.universityId)>
						<#assign highlightClass = "highlight">
					<#else>
						<#assign highlightClass = "">
					</#if>
					<tr id="row${extension.universityId}" class="extension-row ${highlightClass}">
						<td>${studentNameLookup[extension.universityId]}</td>
						<td class="status">
							<#if extension.approved>
								<span class="label label-success">Approved</span>
							<#elseif extension.rejected>
								<span class="label label-important">Rejected</span>
							<#else>
								<span class="label label-info">Awaiting approval</span><br/>
								Requested <@fmt.date date=extension.requestedOn at=true/>
							</#if>
						</td>
						<td class="expiryDate"><#if extension.expiryDate??> <@fmt.date date=extension.expiryDate at=true/></#if></td>
						<td><#if isExtensionManager>
							<a class="review-extension btn btn-mini btn-primary" href="<@routes.extensionreviewrequest assignment=assignment uniId=extension.universityId />" data-toggle="modal" data-target="#extension-model">
								<i class="icon-edit icon-white"></i> Review request
							</a>
						</#if></td>
					</tr>
				</#list>
			</#if>

            <!-- list existing extensions -->
			<#if existingExtensions??>
				<#list existingExtensions as extension>
					<tr id="row${extension.universityId}" class="extension-row">
						<td>${studentNameLookup[extension.universityId]}</td>
						<td class="status">
							<#if extension.approved>
								<span class="label label-success">Approved</span>
							<#elseif extension.rejected>
								<span class="label label-important">Rejected</span>
							</#if>
						</td>
						<td class="expiryDate"><#if extension.expiryDate??> <@fmt.date date=extension.expiryDate at=true/></#if></td>
						<td>
							<a class="hide new-extension btn btn-mini btn-success" href="<@routes.extensionadd assignment=assignment uniId=extension.universityId />" data-toggle="modal" data-target="#extension-model">
								<i class="icon-ok icon-white"></i> Grant
							</a>
							<a class="modify-extension btn btn-mini btn-primary" href="<@routes.extensionedit assignment=assignment uniId=extension.universityId />" data-toggle="modal" data-target="#extension-model">
								<i class="icon-edit icon-white"></i> Modify
							</a>
							&nbsp;
							<a class="revoke-extension btn btn-mini btn-danger" href="<@routes.extensiondelete assignment=assignment uniId=extension.universityId />" data-toggle="modal" data-target="#extension-model">
								<i class="icon-remove icon-white"></i> Revoke
							</a>
						</td>
					</tr>
				</#list>
			</#if>
			<#if potentialExtensions??>
				<#list potentialExtensions as universityId>
					<tr id="row${universityId}" class="extension-row">
						<td>${studentNameLookup[universityId]}</td>
						<td  class="status"></td>
						<td class="expiryDate"></td>
						<td>
							<a class="new-extension btn btn-mini btn-success" href="<@routes.extensionadd assignment=assignment uniId=universityId />" data-toggle="modal" data-target="#extension-model">
								<i class="icon-ok icon-white"></i> Grant
							</a>
							<a class="hide modify-extension btn btn-mini btn-primary" href="<@routes.extensionedit assignment=assignment uniId=universityId />" data-toggle="modal" data-target="#extension-model">
								<i class="icon-edit icon-white"></i> Modify
							</a>
							&nbsp;
							<a class="hide revoke-extension btn btn-mini btn-danger" href="<@routes.extensiondelete assignment=assignment uniId=universityId />" data-toggle="modal" data-target="#extension-model">
								<i class="icon-remove icon-white"></i> Revoke
							</a>
						</td>
					</tr>
				</#list>
			</#if>
		</table>
		<div id="extension-model" class="modal fade"></div>
    </div>
</div>

</#escape>