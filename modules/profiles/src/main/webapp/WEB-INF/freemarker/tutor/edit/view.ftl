<#escape x as x?html>

	<#if tutorToDisplay??>
		<#assign pageAction="edit" >
	<#else>
		<#assign pageAction="add" >
	</#if>
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		<h3>${pageAction?capitalize} Personal Tutor</h3>
	</div>


	<#if user.staff>
		<#assign student = studentCourseDetails.student/>
		<div id="edit-personal-tutor-modal" class="modal-body">
		<@f.form method="post" commandName="editStudentRelationshipCommand" action="" cssClass="form-horizontal">


				<h5 id="tuteeName">Personal Tutee: ${student.fullName}</h5>
				<input id="student" name="student" type="hidden" value="${student.universityId}" />

				<div class="input-append">
					<div class="control-group">
						<label class="control-label" for="query"><b>Personal Tutor</b></label>
						<div class="controls">
							<span class="profile-search">
								<#if tutorToDisplay??>
									<input type="text" name="query" value="${tutorToDisplay.fullName}" id="query"/>
									<input type="hidden" name="tutor" value="${tutorToDisplay.universityId}" />
									<input type="hidden" name="currentTutor" value="${tutorToDisplay.universityId}" />
								<#else>
									<input type="text" name="query" value="" id="query" />
									<input type="hidden" name="tutor" />
								</#if>
							</span>
							<button class="inline-search-button btn" type="button"><i class="icon-search"></i></button>
						</div>
					</div>
				</div>

				<input type="hidden" name="remove" value="false" />
				<#if pageAction!="add">
					<button id="remove-tutor" class="btn btn-danger" type="button">Remove</button>
				</#if>

				<div id="tutorSearchResults"></div>

				<#if pageAction!="add">
					<div id="removeTutorMessage" style="display: none" class="alert clearfix">
						<p>Are you sure you want to remove <strong>${tutorToDisplay.fullName}</strong> as ${student.firstName}'s personal tutor?</p>
						<button id="cancel-remove-tutor" class="btn pull-right" type="button">Cancel</button>
						<button id="confirm-remove-tutor" class="btn btn-primary pull-right" type="button">Confirm</button>
					</div>
				</#if>

				<div id="notify-tutor-change">
				    <#if pageAction!="add">
						<div id="notify-remove-tutor" class="alert hide"><strong>${tutorToDisplay.fullName}</strong> will no longer be ${student.firstName}'s personal tutor.</div>
				  	</#if>
					<p>Notify these people via email of this change</p>
					<div class="control-group">
						<div class="controls">
							<label class="checkbox">
								<input type="checkbox" name="notifyTutee" class="notifyTutee" checked />
								Tutee
							</label>

							<label class="checkbox <#if pageAction=="add">muted</#if>">
								<input type="checkbox" name="notifyOldTutor" class="notifyOldTutor" <#if pageAction!="add">checked <#else> disabled </#if> />
								Old tutor
							</label>

							<label class="checkbox">
								<input type="checkbox" name="notifyNewTutor" class="notifyNewTutor" checked />
								New tutor
							</label>
						</div>
					</div>
				</div>




		</@f.form>
		</div>
		<div class="modal-footer">
				<div type="button" class="btn disabled" id="save-tutor">Save</div>
			</div>
	</#if>

	<script>
		jQuery(document).ready(function($) {

			$('#save-tutor').click(function() {
				if ($(this).hasClass("disabled")) return;
				$.post($("#editStudentRelationshipCommand").prop('action'), $("#editStudentRelationshipCommand").serialize(), function(){
					$('#modal-change-tutor').modal('hide');
					var tutorId = $('#editStudentRelationshipCommand input[name=tutor]').val();
					var remove = $('#editStudentRelationshipCommand input[name=remove]').val();
					if(remove == "true") {
						var action = "removed";
					} else {
						var action = "changed";
					}

					var currentUrl = [location.protocol, '//', location.host, location.pathname].join('');    // url without query string
					window.location = currentUrl + "?action=tutor" + action + "&tutorId=" + tutorId;
				});
			});

			function setStudent(memberString) {
				var member = memberString.split("|");

				if($('#editStudentRelationshipCommand input[name=currentTutor]').val() != member[1]) {
					$('#remove-tutor').addClass("disabled");
					$('#notify-tutor-change').show();
				} else {
					$('#remove-tutor').removeClass("disabled");
				}

				$('#editStudentRelationshipCommand input[name=tutor]').val(member[1]);
				$("#tutorSearchResults").html("");

				$("#save-tutor").removeClass("disabled").addClass("btn-primary");
				return member[0];
			}

			function tutorHighlight(item) {
				var member = item.split("|");
				return '<h3 class="name">' + member[0] + '</h3><span class="description">' + member[3] + '</span>';
			}

			$('.inline-search-button').click(function() {
			    if ($(this).hasClass("disabled")) return;
				var container = $(this).parents("form");
				var target = "../tutor/search.json";
				var query = container.find('input[name="query"]').val();
				if(query.length < 4) return;

				$('#notify-tutor-change').hide();
				$("#tutorSearchResults").html('<h5>Search results</h5><ul></ul>');
				$.get(target, { query : query }, function(data) {
					var members = flattenMemberData(data);
					$.each(data, function(i, member) {
						$("#tutorSearchResults ul").append('<li data-value="'+members[i]+'"><a id="'+member.userId+'"><h6 class="name">' + member.name + '</h6>' + member.description + '</a></li>');
					});
				});
			});

			$('#remove-tutor').click(function() {
				if ($(this).hasClass("disabled")) return;
				if ($('#notify-tutor-change').is(':visible')) $('#notify-tutor-change').hide();
				$('#removeTutorMessage').show();
			});

			$('#confirm-remove-tutor').click(function() {
				$('.notifyNewTutor').attr("disabled", "disabled");
				$('.notifyNewTutor').prop("checked", false);
				$('.notifyNewTutor').closest("label").addClass("muted");
				$('#removeTutorMessage').hide();
				$('#notify-tutor-change').show();
				$("#save-tutor").removeClass("disabled").addClass("btn-primary");
				$('#editStudentRelationshipCommand input[name="query"]').prop('disabled', true);;
				$('#remove-tutor').addClass('disabled');
				$('.inline-search-button').addClass('disabled');
				$('#notify-remove-tutor').show();
				$("#editStudentRelationshipCommand input[name='remove']").val('true');
			});

			$('#cancel-remove-tutor').click(function() {
				$('#removeTutorMessage').hide();
			});

			$('#tutorSearchResults').on('click', 'li', function() {
				var queryInput = $(this).parents('#edit-personal-tutor-modal').find('.profile-search input[name="query"]');
				var memberName = setStudent($(this).attr('data-value'));
				queryInput.val(memberName);
				$("#save-tutor").removeClass("disabled").addClass("btn-primary");
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
							return tutorHighlight(item);
						},

						updater: function(item) {
							return updaterFunction(item);
						},
						minLength:3
					});
				});
			}


			profileSearch($('#edit-personal-tutor-modal .profile-search'), "../tutor/search.json", tutorHighlight, setStudent);


		}(jQuery));
	</script>

</#escape>
