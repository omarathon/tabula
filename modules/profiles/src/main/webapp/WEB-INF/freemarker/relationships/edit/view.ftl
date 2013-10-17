<#escape x as x?html>

	<#if agentToDisplay??>
		<#assign pageAction="edit" >
	<#else>
		<#assign pageAction="add" >
	</#if>


	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		<h3>${pageAction?capitalize} ${relationshipType.agentRole?cap_first}</h3>
	</div>


	<#assign student = studentCourseDetails.student/>
	<div id="edit-agent-modal" class="modal-body">
	<@f.form method="post" commandName="editStudentRelationshipCommand" action="" cssClass="form-horizontal">

		<h5 id="studentName">${relationshipType.studentRole?capitalize}: ${student.fullName}</h5>
		<input id="student" name="student" type="hidden" value="${student.universityId}" />

		<div class="input-append">
			<div class="control-group">
				<label class="control-label" for="query"><b>${relationshipType.agentRole?cap_first}</b></label>
				<div class="controls">
					<span class="profile-search">
						<#if agentToDisplay??>
							<input type="text" name="query" value="${agentToDisplay.fullName}" id="query"/>
							<input type="hidden" name="agent" value="${agentToDisplay.universityId}" />
							<input type="hidden" name="currentAgent" value="${agentToDisplay.universityId}" />
						<#else>
							<input type="text" name="query" value="" id="query" />
							<input type="hidden" name="agent" />
						</#if>
					</span>
					<button class="inline-search-button btn" type="button"><i class="icon-search"></i></button>
				</div>
			</div>
		</div>

		<input type="hidden" name="remove" value="false" />
		<#if pageAction!="add">
			<button id="remove-agent" class="btn btn-danger" type="button">Remove</button>
		</#if>

		<div id="agentSearchResults"></div>

		<#if pageAction!="add">
			<div id="removeAgentMessage" style="display: none" class="alert clearfix">
				<p>Are you sure you want to remove <strong>${agentToDisplay.fullName}</strong> as ${student.firstName}'s ${relationshipType.agentRole}?</p>
				<button id="cancel-remove-agent" class="btn pull-right" type="button">Cancel</button>
				<button id="confirm-remove-agent" class="btn btn-primary pull-right" type="button">Confirm</button>
			</div>
		</#if>

		<div id="notify-agent-change">
			<#if pageAction!="add">
				<div id="notify-remove-agent" class="alert hide"><strong>${agentToDisplay.fullName}</strong> will no longer be ${student.firstName}'s ${relationshipType.agentRole}.</div>
			</#if>
			<p>Notify these people via email of this change</p>
			<div class="control-group">
				<div class="controls">
					<label class="checkbox">
						<input type="checkbox" name="notifyStudent" class="notifyStudent" checked />
						${relationshipType.studentRole?cap_first}
					</label>

					<label class="checkbox <#if pageAction=="add">muted</#if>">
						<input type="checkbox" name="notifyOldAgent" class="notifyOldAgent" <#if pageAction!="add">checked <#else> disabled </#if> />
						Old ${relationshipType.agentRole}
					</label>

					<label class="checkbox">
						<input type="checkbox" name="notifyNewAgent" class="notifyNewAgent" checked />
						New ${relationshipType.agentRole}
					</label>
				</div>
			</div>
		</div>

	</@f.form>
	</div>
	<div class="modal-footer">
			<div type="button" class="btn disabled" id="save-agent">Save</div>
	</div>


	<script type="text/javascript">
		jQuery(document).ready(function($) {

			$('#save-agent').click(function() {
				if ($(this).hasClass("disabled")) return;
				$.post($("#editStudentRelationshipCommand").prop('action'), $("#editStudentRelationshipCommand").serialize(), function(){
					$('#modal-change-agent').modal('hide');
					var agentId = $('#editStudentRelationshipCommand input[name=agent]').val();
					var remove = $('#editStudentRelationshipCommand input[name=remove]').val();
					var error = $('input[name=error]').val();


					if(agentId == undefined) {
						var action = "error";
					}
					else if(remove == "true") {
						var action = "removed";
					} else {
						var action = "changed";
					}

					var currentUrl = [location.protocol, '//', location.host, location.pathname].join('');    // url without query string
					window.location = currentUrl + "?action=agent" + action + "&agentId=" + agentId + "&relationshipType=${relationshipType.id}";
				});
			});

			function setStudent(memberString) {
				var member = memberString.split("|");

				if($('#editStudentRelationshipCommand input[name=currentAgent]').val() != member[1]) {
					$('#remove-agent').addClass("disabled");
					$('#notify-agent-change').show();
				} else {
					$('#remove-agent').removeClass("disabled");
				}

				$('#editStudentRelationshipCommand input[name=agent]').val(member[1]);
				$("#agentSearchResults").html("");

				$("#save-agent").removeClass("disabled").addClass("btn-primary");
				return member[0];
			}

			function agentHighlight(item) {
				var member = item.split("|");
				return '<h3 class="name">' + member[0] + '</h3><span class="description">' + member[3] + '</span>';
			}

			$('.inline-search-button').click(function() {
			    if ($(this).hasClass("disabled")) return;
				var container = $(this).parents("form");
				var target = "<@routes.relationship_search_json />";
				var query = container.find('input[name="query"]').val();
				if(query.length < 4) return;

				$('#notify-agent-change').hide();
				$("#agentSearchResults").html('<h5>Search results</h5><ul></ul>');
				$.get(target, { query : query }, function(data) {
					var members = flattenMemberData(data);
					$.each(data, function(i, member) {
						$("#agentSearchResults ul").append('<li data-value="'+members[i]+'"><a id="'+member.userId+'"><h6 class="name">' + member.name + '</h6>' + member.description + '</a></li>');
					});
				});
			});

			$('#remove-agent').click(function() {
				if ($(this).hasClass("disabled")) return;
				if ($('#notify-agent-change').is(':visible')) $('#notify-agent-change').hide();
				$('#removeAgentMessage').show();
			});

			$('#confirm-remove-agent').click(function() {
				$('.notifyNewAgent').attr("disabled", "disabled");
				$('.notifyNewAgent').prop("checked", false);
				$('.notifyNewAgent').closest("label").addClass("muted");
				$('#removeAgentMessage').hide();
				$('#notify-agent-change').show();
				$("#save-agent").removeClass("disabled").addClass("btn-primary");
				$('#editStudentRelationshipCommand input[name="query"]').prop('disabled', true);;
				$('#remove-agent').addClass('disabled');
				$('.inline-search-button').addClass('disabled');
				$('#notify-remove-agent').show();
				$("#editStudentRelationshipCommand input[name='remove']").val('true');
			});

			$('#cancel-remove-agent').click(function() {
				$('#removeAgentMessage').hide();
			});

			$('#agentSearchResults').on('click', 'li', function() {
				var queryInput = $(this).parents('#edit-agent-modal').find('.profile-search input[name="query"]');
				var memberName = setStudent($(this).attr('data-value'));
				queryInput.val(memberName);
				$("#save-agent").removeClass("disabled").addClass("btn-primary");
			});

			function flattenMemberData(data) {
				var members = [];
				$.each(data, function(i, member) {
					var item = member.name + "|" + member.id + "|" + member.userId + "|" + member.description;
					members.push(item);
				});

				return members;
			}

			var profileSearch = function(searchContainer, target, highlighterFunction, updaterFunction) {
				var container = $(searchContainer);
				var xhr = null;
				container.find('input[name="query"]').prop('autocomplete','off').each(function() {
					var $spinner = $('<div class="spinner-container" />');
					$(searchContainer).before($spinner);

					$(this).typeahead({
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

								var members = flattenMemberData(data);
								process(members);
							}).error(function(jqXHR, textStatus, errorThrown) { if (textStatus != "abort") $spinner.spin(false); });
						},

						matcher: function(item) { return true; },
						sorter: function(items) { return items; }, // use 'as-returned' sort
						highlighter: function(item) {
							return agentHighlight(item);
						},

						updater: function(item) {
							return updaterFunction(item);
						},
						minLength:3
					});
				});
			}


			profileSearch($('#edit-agent-modal .profile-search'), "<@routes.relationship_search_json />", agentHighlight, setStudent);


		}(jQuery));
	</script>

</#escape>
