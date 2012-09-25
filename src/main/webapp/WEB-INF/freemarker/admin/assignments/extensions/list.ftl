<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign time_remaining=durationFormatter(assignment.closeDate) />

<div>
    <h1>Authorise late submissions for ${assignment.name}</h1>
    <p>
		This assignment closes on <strong><@fmt.date date=assignment.closeDate timezone=true /> (${time_remaining})</strong>.
		To authorise an extension for a student click the "Grant" button next to the students university ID. If you
		wish to grant an extension to a user that is not listed below, please ensure that they appear in the students
		list on the <a href="/admin/module/${module.code}/assignments/${assignment.id}/edit">assignment edit page</a>.
    </p><br/>
    <div id="extension-list">
        <table class="extensionListTable table table-striped table-bordered">
            <tr class="extension-header">
				<th>University ID</th>
				<th>Status</th>
				<th>New deadline</th>
				<th><!-- Actions column--></th>
			</tr>

			<!-- list extension requests -->
			<#if extensionRequests??>
				<#list extensionRequests as extension>
					<tr id="row${extension.universityId}" class="extension-row">
						<td>${extension.universityId}</td>
						<td class="status">
							<#if extension.approved>
								<span class="label-green">Approved</span>
							<#elseif extension.rejected>
								<span class="label-red">Rejected</span>
							<#else>
								<span class="label-blue">Awaiting approval</span><br/>
								Requested <@fmt.date date=extension.requestedOn at=true/>
							</#if>
						</td>
						<td class="expiryDate"></td>
						<td>
							<a class="new-extension btn btn-mini btn-primary" href="review-request/${extension.universityId}" data-toggle="modal" data-target="#extension-model">
								<i class="icon-edit icon-white"></i> Review request
							</a>
						</td>
					</tr>
				</#list>
			</#if>

            <!-- list existing extensions -->
			<#if existingExtensions??>
				<#list existingExtensions as extension>
					<tr id="row${extension.universityId}" class="extension-row">
						<td>${extension.universityId}</td>
						<td class="status">
							<#if extension.approved>
								<span class="label-green">Approved</span>
							<#elseif extension.rejected>
								<span class="label-red">Rejected</span>
							</#if>
						</td>
						<td class="expiryDate"><#if extension.expiryDate??> <@fmt.date date=extension.expiryDate at=true/></#if></td>
						<td>
							<a class="hide new-extension btn btn-mini btn-success" href="add?universityId=${extension.universityId}" data-toggle="modal" data-target="#extension-model">
								<i class="icon-ok icon-white"></i> Grant
							</a>
							<a class="modify-extension btn btn-mini btn-primary" href="edit/${extension.universityId}" data-toggle="modal" data-target="#extension-model">
								<i class="icon-edit icon-white"></i> Modify
							</a>
							<a class="revoke-extension btn btn-mini btn-danger" href="delete/${extension.universityId}" data-toggle="modal" data-target="#extension-model">
								<i class="icon-remove icon-white"></i> Revoke
							</a>
						</td>
					</tr>
				</#list>
			</#if>
			<#if potentialExtensions??>
				<#list potentialExtensions as universityId>
					<tr id="row${universityId}" class="extension-row">
						<td>${universityId}</td>
						<td  class="status"></td>
						<td class="expiryDate"></td>
						<td>
							<a class="new-extension btn btn-mini btn-success" href="add?universityId=${universityId}" data-toggle="modal" data-target="#extension-model">
								<i class="icon-ok icon-white"></i> Grant
							</a>
							<a class="hide modify-extension btn btn-mini btn-primary" href="edit/${universityId}" data-toggle="modal" data-target="#extension-model">
								<i class="icon-edit icon-white"></i> Modify
							</a>
							<a class="hide revoke-extension btn btn-mini btn-danger" href="delete/${universityId}" data-toggle="modal" data-target="#extension-model">
								<i class="icon-remove icon-white"></i> Revoke
							</a>
						</td>
					</tr>
				</#list>
			</#if>
		</table>
		<div id="extension-model" class="modal fade"></div>
		<script type="text/javascript">
			jQuery(function($){
				// models use ajax to retrieve their contents
				$('#extension-list').on('click', 'a[data-toggle=modal]', function(e){
					e.preventDefault();
					$this = $(this);
					target = $this.attr('data-target');
					url = $this.attr('href');
					$(target).load(url);
				});

				// any date fields returned by ajax will have datetime pickers bound to them as required
				$('#extension-list').on('focus', 'input.date-time-picker', function(e){
					e.preventDefault();
					$(this).AnyTime_noPicker().AnyTime_picker({
						format: "%e-%b-%Y %H:%i:%s",
						firstDOW: 1
					});
				});

				$('#extension-list').on('click', 'input[type=submit]', function(e){
					e.preventDefault();
					var $form = $(this).closest('form');
					$.post($form.attr('action'), $form.serialize(), function(data){
						if(data.status == "error"){
							// delete any old errors
							$(".error").remove();
							for(error in data.result){
								addError(error, data.result[error]);
							}
						}
						else {
							var action = data.action;
							$.each(data.result, function(){
								modifyRow(this, action);
							});
							// hide the model
							jQuery("#extension-model").modal('hide');
						}
					});
				});

				// set reject and approved flags
				jQuery("#extension-model").on('click', '#approveButton', function(){
					jQuery(".approveField").val("1");
					jQuery(".rejectField").val("0");
				});

				jQuery("#extension-model").on('click', '#rejectButton', function(){
					jQuery(".approveField").val("0");
					jQuery(".rejectField").val("1");
				});

				var addError = function(field, message){
					var $field = $("input[name='"+field+"']");
					$field.closest(".control-group").addClass("error");
					// insert error message
					$field.after('<span class="error help-inline">'+message+'</span>');
				};

				var modifyRow = function(results, action){
					$row =  $("#extension-list").find("#row"+results.id);
					if(action === "add"){
						updateRowUI(results);
						$(".new-extension", $row).hide();
						$(".modify-extension", $row).show();
						$(".revoke-extension", $row).show();
					} else if (action === "edit"){
						updateRowUI(results);
					} else if (action === "delete"){
						$("td.expiryDate", $row).html("");
						$("td.status", $row).html("");
						$(".new-extension", $row).show();
						$(".modify-extension", $row).hide();
						$(".revoke-extension", $row).hide();
					}
				};

				var updateRowUI = function(json){
					for(prop in json){
						$row.find('.'+prop).html(json[prop]);
					}
					$('.status').each(function(){
						if ($(this).html() === "Approved")
							$(this).html('<span class="label-green">Approved</span>');
						else if ($(this).html() === "Rejected")
							$(this).html('<span class="label-red">Rejected</span>');
					});
				};
			});
		</script>
    </div>
</div>

</#escape>