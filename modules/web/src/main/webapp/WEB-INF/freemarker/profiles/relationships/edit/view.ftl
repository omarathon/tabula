<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>

	<#if agentToDisplay??>
		<#assign pageAction="edit" >
	<#else>
		<#assign pageAction="add" >
	</#if>

	<@modal.wrapper>
		<@modal.header>
			<h3 class="modal-title">${pageAction?capitalize} ${relationshipType.agentRole?cap_first}</h3>
		</@modal.header>


		<#assign student = studentCourseDetails.student/>

		<@modal.body>
			<@f.form method="post" commandName="editStudentRelationshipCommand" action="" cssClass="dirty-check-ignore">

				<h5 id="studentName">${relationshipType.studentRole?capitalize}: ${student.fullName}</h5>
				<input id="student" name="student" type="hidden" value="${student.universityId}" />

				<@bs3form.labelled_form_group path="" labelText="${relationshipType.agentRole?cap_first}">
					<div class="input-group profile-search-results">
						<#if agentToDisplay??>
							<input type="text" name="query" value="${agentToDisplay.fullName}" id="query" class="form-control" />
							<input type="hidden" name="agent" value="${agentToDisplay.universityId}" />
							<input type="hidden" name="currentAgent" value="${agentToDisplay.universityId}" />
						<#else>
							<input type="text" name="query" value="" id="query" class="form-control" />
							<input type="hidden" name="agent" />
						</#if>
						<span class="input-group-btn">
							<button class="inline-search-button btn btn-default" type="button"><i class="fa fa-search"></i></button>
						</span>
					</div>
				</@bs3form.labelled_form_group>

				<input type="hidden" name="remove" value="false" />
				<#if pageAction!="add">
					<button id="remove-agent" class="btn btn-danger" type="button">Remove</button>
				</#if>

				<#if pageAction!="add">
					<div id="removeAgentMessage" style="display: none" class="alert alert-info clearfix">
						<p>Are you sure you want to remove <strong>${agentToDisplay.fullName}</strong> as ${student.firstName}'s ${relationshipType.agentRole}?</p>
						<div class="pull-right">
							<button id="confirm-remove-agent" class="btn btn-primary" type="button">Confirm</button>
							<button id="cancel-remove-agent" class="btn" data-dismiss="modal" type="button">Cancel</button>
						</div>
					</div>
				</#if>

				<div id="notify-agent-change">
					<#if pageAction!="add">
						<div id="notify-remove-agent" class="alert alert-info hide"><strong>${agentToDisplay.fullName}</strong> will no longer be ${student.firstName}'s ${relationshipType.agentRole}.</div>
					</#if>
					<p>Notify these people via email of this change</p>
					<@bs3form.checkbox>
						<input type="checkbox" name="notifyStudent" class="notifyStudent" checked />
						${relationshipType.studentRole?cap_first}
					</@bs3form.checkbox>
					<@bs3form.checkbox>
						<input type="checkbox" name="notifyOldAgents" class="notifyOldAgents" <#if pageAction!="add">checked <#else> disabled </#if> />
						Old ${relationshipType.agentRole}
					</@bs3form.checkbox>
					<@bs3form.checkbox>
						<input type="checkbox" name="notifyNewAgent" class="notifyNewAgent" checked />
						New ${relationshipType.agentRole}
					</@bs3form.checkbox>
				</div>
			</@f.form>
		</@modal.body>

		<@modal.footer>
			<div class="pull-right">
				<button id="save-agent" class="btn btn-default disabled" type="button">Save</button>
				<button id="cancel-save-agent" class="btn btn-default" data-dismiss="modal" type="button">Cancel</button>
			</div>
		</@modal.footer>
	</@modal.wrapper>


	<script type="text/javascript">
		jQuery(function($) {
			var $form = $('#editStudentRelationshipCommand')
				, $modal = $('#modal-change-agent')
				, $modalBody = $modal.find('.modal-body')
				, $saveButton = $('#save-agent');

			function submitForm() {
				$.post($form.prop('action'), $form.serialize(), function() {
					$modal.modal('hide');
					var agentId = $form.find('input[name=agent]').val()
						, remove = $form.find('input[name=remove]').val()
						, action = 'changed';

					if(agentId == undefined || !agentId) {
						action = "error";
					} else if(remove == "true") {
						action = "removed";
					}

					var currentUrl = [location.protocol, '//', location.host, location.pathname].join('');    // url without query string
					window.location = currentUrl + '?action=agent' + action + '&agentId=' + agentId + '&relationshipType=${relationshipType.id}';
				});
			}

			$modalBody.on('submit', 'form', function(e){
				e.preventDefault();
			});

			$saveButton.click(function() {
				if ($(this).hasClass("disabled")) return;
				submitForm();
			});

			$('#remove-agent').click(function() {
				if ($(this).hasClass("disabled")) return;
				if ($('#notify-agent-change').is(':visible')) $('#notify-agent-change').hide();
				$('#removeAgentMessage').show();
			});

			$('#confirm-remove-agent').click(function() {
				$('.notifyNewAgent').attr("disabled", "disabled")
					.prop("checked", false)
					.closest("label").addClass("muted");
				$('#removeAgentMessage').hide();
				$('#notify-agent-change').show();
				$("#save-agent").removeClass("disabled").addClass("btn-primary");
				$form.find('input[name="query"]').prop('disabled', true);
				$('#remove-agent').addClass('disabled');
				$('.inline-search-button').addClass('disabled');
				$('#notify-remove-agent').show();
				$form.find("input[name='remove']").val('true');
			});

			var profileSearch = function(searchContainer, target, highlighterFunction, updaterFunction) {
				var container = $(searchContainer);
				var xhr = null;
				container.find('input[name="query"]').prop('autocomplete','off').each(function() {
					var $spinner = $('<div class="spinner-container" />');
					$(this).before($spinner).bootstrap3Typeahead({
						source: function(query, process) {
							if (xhr != null) {
								xhr.abort();
								xhr = null;
							}

							query = $.trim(query);
							if (query.length < 3) { process([]); return; }

							// At least one of the search terms must have more than 1 character
							var terms = query.split(/\s+/g);
							if ($.grep(terms, function(term) { return term.length > 1; }).length == 0) {
								process([]); return;
							}

							$spinner.spin('small');
							xhr = $.get(target, { query : query }, function(data) {
								$spinner.spin(false);

								var members = [];
								$.each(data, function(i, member) {
									var item = member.name + '|' + member.id + '|' + member.userId + '|' + member.description;
									members.push(item);
								});

								process(members);
							}).error(function(jqXHR, textStatus, errorThrown) { if (textStatus != "abort") $spinner.spin(false); });
						},

						matcher: function(item) { return true; },
						sorter: function(items) { return items; }, // use 'as-returned' sort
						highlighter: highlighterFunction,

						updater: updaterFunction,
						minLength:3
					});
				});
			};

			profileSearch($modalBody.find('.input-group'), "<@routes.profiles.relationship_search_json />", function(item) {
				var member = item.split("|");
				return '<h3 class="name">' + member[0] + '</h3><span class="description">' + member[3] + '</span>';
			}, function(memberString) {
				var member = memberString.split("|");

				if($form.find('input[name=currentAgent]').val() != member[1]) {
					$('#remove-agent').addClass("disabled");
					$('#notify-agent-change').show();
				} else {
					$('#remove-agent').removeClass("disabled");
				}

				$form.find('input[name=agent]').val(member[1]);
				$saveButton.removeClass("disabled").addClass("btn-primary");
				return member[0];
			});

			$('#notify-agent-change').hide();
		});
	</script>

</#escape>
