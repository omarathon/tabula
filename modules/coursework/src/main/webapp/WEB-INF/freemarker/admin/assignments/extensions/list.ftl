<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign time_remaining=durationFormatter(assignment.closeDate) />

<div>
    <h1>Grant extensions for ${assignment.name}</h1>
    <p>
		This assignment <@fmt.tense assignment.closeDate "closes" "closed" />
		on <strong><@fmt.date date=assignment.closeDate at=true timezone=true /> (${time_remaining})</strong>.
    </p>
    <div class="alert alert-info">
    	<i class="icon-envelope"></i> Students will automatically be notified by email when you grant, modify or revoke an extension.
    </div>
    <div id="extension-list">
        <table class="extensionListTable table table-striped table-bordered tabula-greenLight tablesorter">
            <thead>
                <tr class="extension-header">
                    <th>First name</th>
                    <th>Last name</th>
                    <th>New deadline</th>
                    <th>Extension status</th>
                    <th>
	                    Actions
	                    <a class="use-popover" id="popover-actions-help" data-html="true" data-container="body"
						   data-placement="bottom"
	                       data-original-title="Granting extensions"
	                       data-content="<p>To grant an extension for a student, click on the 'Grant' button next to the student's name.</p>
	                                     <p>If you wish to grant an extension to a student that is not listed below, please ensure that they appear in the students lists in
	                                     <a href='../edit'>assignment properties</a></p>
	                                     <p>Once an extension is granted you can change it using the 'Modify' button or withdraw it using the 'Revoke' button.</p>
	                                     <p>If a student has requested an extension, you can click on the 'Review' button to grant or reject.</p>
	                                    ">
		                   <i class="icon-question-sign"></i></a>
                    </th>
                </tr>
            </thead>

			<tbody>
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
							<#assign student = studentNameLookup[extension.universityId]>
							<td><h6>${student.firstName}</h6></td>
							<td><h6>${student.lastName}</h6></td>
							<td class="expiryDate"><#if extension.expiryDate??> <@fmt.date date=extension.expiryDate at=true/></#if></td>
							<td class="status">
								<#if extension.approved>
									Granted
								<#elseif extension.rejected>
									Rejected
								<#else>
									<span class="requestedExtension">Requested <@fmt.date date=extension.requestedOn at=true /></span>
								</#if>
							</td>
							<td><#if isExtensionManager>
								<a class="review-extension btn btn-mini btn-primary" href="<@routes.extensionreviewrequest assignment=assignment uniId=extension.universityId />" data-toggle="modal" data-target="#extension-model">
									<i class="icon-edit icon-white"></i> Review
								</a>
							</#if></td>
						</tr>
					</#list>
				</#if>

				<!-- list existing extensions -->
				<#if existingExtensions??>
					<#list existingExtensions as extension>
						<tr id="row${extension.universityId}" class="extension-row">
							<#assign student = studentNameLookup[extension.universityId]>
							<td><h6>${student.firstName}</h6></td>
							<td><h6>${student.lastName}</h6></td>
							<td class="expiryDate"><#if extension.expiryDate??> <@fmt.date date=extension.expiryDate capitalise=true shortMonth=true /></#if></td>
							<td class="status">
								<#if extension.approved>
									Granted
								<#elseif extension.rejected>
									Rejected
								</#if>
							</td>


							<td>
								<a class="new-extension btn btn-mini btn-success" href="<@routes.extensionadd assignment=assignment uniId=extension.universityId />" data-toggle="modal" data-target="#extension-model" style="display:none;">
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
							<#assign student = studentNameLookup[universityId]>
							<td><h6>${student.firstName}</h6></td>
							<td><h6>${student.lastName}</h6></td>
							<td class="expiryDate"></td>
							<td  class="status"></td>
							<td>
								<a class="new-extension btn btn-mini btn-success" href="<@routes.extensionadd assignment=assignment uniId=universityId />" data-toggle="modal" data-target="#extension-model">
									<i class="icon-ok icon-white"></i> Grant
								</a>
								<a class="modify-extension btn btn-mini btn-primary" href="<@routes.extensionedit assignment=assignment uniId=universityId />" data-toggle="modal" data-target="#extension-model" style="display:none;">
									<i class="icon-edit icon-white"></i> Modify
								</a>
								&nbsp;
								<a class="revoke-extension btn btn-mini btn-danger" href="<@routes.extensiondelete assignment=assignment uniId=universityId />" data-toggle="modal" data-target="#extension-model" style="display:none;">
									<i class="icon-remove icon-white"></i> Revoke
								</a>
							</td>
						</tr>
					</#list>
				</#if>
	        </tbody>
		</table>
		<div id="extension-model" class="modal fade"></div>
    </div>
</div>

<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
<script type="text/javascript">

	(function($) {

	$('.extensionListTable').tablesorter({
		sortList: [[1,0]],
		headers: { 4: { sorter: false } },
		textExtraction: function(node) {
			var $el = $(node);

			return $el.text().trim();
		}
		});
	})(jQuery);

</script>

</#escape>