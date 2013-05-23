<#escape x as x?html>
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		<h3>Edit Personal Tutor</h3>
	</div>

	<#if user.staff>
		<@f.form method="post" action="${url('/tutor/${student.universityId}/edit')}" commandName="editTutorCommand" cssClass="form-horizontal">
			<div class="modal-body" id="edit-personal-tutor-modal">

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
				<!-- fresh -->
				<button id="remove-tutor" class="btn btn-danger" type="button">Remove</button>

				<div id="tutorSearchResults"></div>

				<div class="control-group" id="notify-tutor-change">
					<div class="controls">
						<label class="checkbox">
							<@f.checkbox path="notifyCommand.notifyTutee" value="false" />
							Notify tutee of change
						</label>

						<label class="checkbox">
							<@f.checkbox path="notifyCommand.notifyOldTutor" value="false" />
							Notify old tutor of change
						</label>

						<label class="checkbox">
							<@f.checkbox path="notifyCommand.notifyNewTutor" value="false" />
							Notify new tutor of change
						</label>
					</div>
				</div>

			</div>

			<div class="modal-footer">
				<input type="submit" class="btn btn-primary" id="save-tutor" value="Save" />
			</div>
		</@f.form>
	</#if>

	<script>
		jQuery(function($) {

			$('#save-tutor').click(function() {
				$.post($("#editTutorCommand").prop('action'), $("#editTutorCommand").serialize());
				$('#modal').modal('hide');
				var tutor = $('#editTutorCommand input[name=tutor]').val();
				if(tutor =="") {
					var action = "removed";
				} else {
		         	var action = "changed";
				}

				var currentUrl = [location.protocol, '//', location.host, location.pathname].join('');    // url without query string
				window.location = currentUrl + "?action=tutor" + action + "&tutorId=" + tutor;
			});

			function setStudent(memberString) {
				var member = memberString.split("|");
				$('#editTutorCommand input[name=tutor]').val(member[1]);
				$("#tutorSearchResults").html("");
				$('#notify-tutor-change').show();
				return member[0];
			}

			function tutorHighlight(item) {
				var member = item.split("|");
				return '<h3 class="name">' + member[0] + '</h3><span class="description">' + member[3] + '</span>';
			}

			$('.inline-search-button').click(function() {
				var container = $(this).parents("form");
				var target = "../tutor/search.json";
				var query = container.find('input[name="query"]').val();

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
				$('input[name=tutor]').val("");
				$(this).parents('#modal').find('.profile-search input[name="query"]').val("");
				// open up a "are you sure?" flash message
				// if that is clicked
			});

			$('#tutorSearchResults').on('click', 'li', function() {
				var queryInput = $(this).parents('#modal').find('.profile-search input[name="query"]');
				var memberName = setStudent($(this).attr('data-value'));
				queryInput.val(memberName);
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

		});
	</script>
</#escape>
