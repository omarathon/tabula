<#assign module = assignment.module />
<#assign department = module.department />
<#assign feedbackGraphs = studentFeedbackGraphs />
<#macro row graph>
	<#assign u = graph.student />
	<tr class="itemContainer" data-contentid="${u.warwickId}">
		<#if department.showStudentName>
			<td class="student-col toggle-cell"><h6 class="toggle-icon" >${u.firstName}</h6></td>
			<td class="student-col toggle-cell"><h6>${u.lastName}</h6></td>
		<#else>
			<td class="student-col toggle-cell"><h6 class="toggle-icon">${u.warwickId}</h6></td>
		</#if>
		<td class="status-col toggle-cell content-cell">
			<dl style="margin: 0; border-bottom: 0;">
				<dt>
					<#if graph.hasSubmission>
						<div class="label">Submitted</div>
					<#else>
						<div class="label label-warning">No submission</div>
					</#if>
					<#if graph.hasPublishedFeedback>
						<div class="label label-success">Published</div>
					<#elseif graph.hasCompletedFeedback>
						<div class="label label-success">Marking completed</div>
					<#elseif graph.hasFeedback>
						<div class="label label-warning marked">Marked</div>
					</#if>
				</dt>
				<dd style="display: none;" class="table-content-container" data-contentid="${u.warwickId}">
					<div id="content-${u.warwickId}" class="feedback-container content-container" data-contentid="${u.warwickId}">
						<p>No data is currently available.</p>
					</div>
				</dd>
			</dl>
		</td>
	</tr>
</#macro>

<#escape x as x?html>
	<h1>Online marking for ${assignment.name} (${assignment.module.code?upper_case})</h1>

	<#import "../turnitin/_report_macro.ftl" as tin />
	<#import "../submissionsandfeedback/_submission_details.ftl" as sd />

	<#-- TODO 20 day turnaround deadline status alert thing rendering -->

	<table id="online-marking-table" class="students table table-bordered table-striped tabula-greenLight sticky-table-headers">
		<thead<#if feedbackGraphs?size == 0> style="display: none;"</#if>>
			<tr>
				<#if department.showStudentName>
					<th class="student-col">First name</th>
					<th class="student-col">Last name</th>
				<#else>
					<th class="student-col">University ID</th>
				</#if>

				<th class="status-col">Status</th>
			</tr>
		</thead>

		<#if feedbackGraphs?size gt 0>
			<tbody>
				<#list feedbackGraphs as graph>
					<@row graph />
				</#list>
			</tbody>
		</#if>
	</table>

	<#if feedbackGraphs?size gt 0>
		<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
		<script type="text/javascript">
		(function($) {
			var tsOptions = {
				sortList: [<#if department.showStudentName>[2, 0], </#if>[1, 0], [0,0]]
			};

			$('#online-marking-table').expandingTable({
				contentUrl: '${info.requestedUri!""}',
				useIframe: true,
				tableSorterOptions: tsOptions
			});
		})(jQuery);
		</script>
		<script type="text/javascript">
			jQuery(function($) {

				$('#main-content').on('tabula.expandingTable.contentChanged', '.feedback-container', function(e) {
					var $container = $(this);
					var $form = $container.find('.onlineFeedback form');
					var contentId = $container.attr('data-contentid');
					var $row = $('tr.itemContainer[data-contentid='+contentId+']');

					// record the initial values of the fields
					$('input, textarea', $form).each(function() {
						$(this).data('initialvalue', $(this).val());
					});

					$('.cancel-feedback', $form).on('click', function(e) {
						e.preventDefault();
						e.stopPropagation();
						if(hasChanges($container)) {
							var message = 'Discarding unsaved changes is irreversible. Are you sure?';
							var modalHtml = "<div class='modal hide fade' id='confirmModal'>" +
												"<div class='modal-body'>" +
													"<h5>"+message+"</h5>" +
												"</div>" +
												"<div class='modal-footer'>" +
													"<a class='confirm btn'>Yes, discard</a>" +
													"<a data-dismiss='modal' class='btn btn-primary'>No, go back to editing</a>" +
												"</div>" +
											"</div>"
							var $modal = $(modalHtml);
							$modal.modal();
							$('a.confirm', $modal).on('click', function(){
								resetFormValues($form, $row);
								$modal.modal('hide');
							});
						} else {
							resetFormValues($form, $row);
						}
					});

					$form.ajaxForm({
						iframe: true,
						success: function(resp){
							var $resp = $(resp);
							if($resp.find('pre#dev').length) {
								$container.html($resp.find('#column-1-content'));
								$container.trigger('tabula.expandingTable.contentChanged');
							} else {
								// there is an ajax-response class somewhere in the response text
								var $response = $resp.find('.ajax-response').andSelf().filter('.ajax-response');

								var success = $response.length && $response.data('status') == 'success';
								if (success) {
									var $statusContainer = $row.find('.status-col dt');
									if(!$statusContainer.find('.marked').length){
										$statusContainer.append($('<div class="label label-warning marked">Marked</div>'));
									}

									$statusContainer.find('.unsaved').remove();

									$container.removeData('loaded');

									$row.trigger('tabula.expandingTable.toggle');
									$row.next('tr').trigger('tabula.expandingTable.toggle');

									$container.html("<p>No data is currently available.</p>");
								} else {
									$container.html($resp);
									$container.trigger('tabula.expandingTable.contentChanged');
								}
							}
						},
						error: function(){alert("There has been an error. Please reload and try again.");}
					});
				});

				$('#main-content').on('tabula.expandingTable.parentRowCollapsed', '.feedback-container', function(e) {
					var $this = $(this);
					if(hasChanges($this)) {
						var contentId = $this.attr('data-contentid');
						var $row = $('tr.itemContainer[data-contentid='+contentId+']');
						var $statusContainer = $row.find('.status-col dt');
						if(!$statusContainer.find('.unsaved').length){
							$statusContainer.append($('<div class="label label-important unsaved">Unsaved changes</div>'));
						}
					}
				});

				$('#main-content').on("click", ".remove-attachment", function(e) {
					var $this = $(this);
					var $form = $this.closest('form');
					var $li = $this.closest("li");
					$li.find('input, a').remove();
					$li.find('span').before('<i class="icon-remove"></i>&nbsp;').wrap('<del />');
					var $ul = $li.closest('ul');

					if (!$ul.next().is('.alert')) {
						var alertMarkup = '<p class="alert pending-removal"><i class="icon-lightbulb"></i> Files marked for removal won\'t be deleted until you <samp>Save</samp>.</p>';
						$ul.after(alertMarkup);
					}

					if($('input[name=attachedFiles]').length == 0){
						var $blankInput = $('<input name="attachedFiles" type="hidden" />');
						$form.append($blankInput);
					}
					return false;
				});

				function resetFormValues($form, $row) {
					// reset all the data for this row
					$('input, textarea', $form).each(function() {
						$(this).val($(this).data('initialvalue'));
					});

					// remove unsaved badges
					$row.find('.unsaved').remove();

					// collapse the row
					$row.trigger('tabula.expandingTable.toggle');
				}

				function hasChanges($container) {
					return $container.data('loaded') &&
						($container.find('.pending-removal').length > 0 || inputsHaveChanges($container));
				}

				function inputsHaveChanges($container) {
					var modifiedField = false;
					var $inputs = $container.find(':input');
					$inputs.each(function() {
						modifiedField = $(this).val() != $(this).data("initialvalue");
						return !modifiedField; // false breaks from loop
					});
					return modifiedField;
				}

			});
		</script>
	<#else>
		<p>There are no submissions to mark for this assignment.</p>
	</#if>
</#escape>