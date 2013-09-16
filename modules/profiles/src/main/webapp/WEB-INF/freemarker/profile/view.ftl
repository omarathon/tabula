<#import "profile_macros.ftl" as profile_macros />
<#escape x as x?html>

<#if user.staff>
	<#include "search/form.ftl" />

	<hr class="full-width" />
</#if>

<article class="profile">
	<section id="personal-details" class="clearfix">

		<@fmt.member_photo profile />


		<header>
			<h1><@fmt.profile_name profile /></h1>
			<h5><@fmt.profile_description profile /></h5>
		</header>

		<div class="data clearfix">
			<div class="col1">
				<table class="profile-info">
					<tbody>
						<tr>
							<th>Official name</th>
							<td>${profile.officialName}</td>
						</tr>

						<tr>
							<th>Preferred name</th>
							<td>${profile.fullName}</td>
						</tr>

						<#if profile.gender??>
							<tr>
								<th>Gender</th>
								<td>${profile.gender.description}</td>
							</tr>
						</#if>

						<#if profile.nationality??>
							<tr>
								<th>Nationality</th>
								<td><@fmt.nationality profile.nationality?default('Unknown') /></td>
							</tr>
						</#if>

						<#if profile.dateOfBirth??>
							<tr>
								<th>Date of birth</th>
								<td><@warwick.formatDate value=profile.dateOfBirth.toDateTimeAtStartOfDay() pattern="dd/MM/yyyy" /></td>
							</tr>
						</#if>

						<#if profile.student && profile.termtimeAddress??>
							<tr class="address">
								<th>Term-time address</th>
								<td><@profile_macros.address profile.termtimeAddress /></td>
							</tr>
						</#if>

						<#if profile.student && profile.nextOfKins?? && profile.nextOfKins?size gt 0>
							<tr>
								<th>Emergency contacts</th>
								<td>
									<#list profile.nextOfKins as kin>
										<div>
											<#if kin.firstName?? && kin.lastName??>${kin.fullName}</#if>
											<#if kin.relationship??>(${kin.relationship})</#if>
										</div>
									</#list>
								</td>
							</tr>
						</#if>
					</tbody>
				</table>

				<br class="clearfix">
			</div>

			<div class="col2">
				<table class="profile-info">
					<tbody>
						<#if profile.email??>
							<tr>
								<th>Warwick email</th>
								<td><i class="icon-envelope"></i> <a href="mailto:${profile.email}">${profile.email}</a></td>
							</tr>
						</#if>

						<#if profile.homeEmail??>
							<tr>
								<th>Alternative email</th>
								<td><i class="icon-envelope"></i> <a href="mailto:${profile.homeEmail}">${profile.homeEmail}</a></td>
							</tr>
						</#if>

						<#if profile.phoneNumber??>
							<tr>
								<th>Phone number</th>
								<td>${phoneNumberFormatter(profile.phoneNumber)}</td>
							</tr>
						</#if>

						<#if profile.mobileNumber??>
							<tr>
								<th>Mobile phone</th>
								<td>${phoneNumberFormatter(profile.mobileNumber)}</td>
							</tr>
						</#if>

						<#if profile.universityId??>
							<tr>
								<th>University number</th>
								<td>${profile.universityId}</td>
							</tr>
						</#if>

						<#if profile.userId??>
							<tr>
								<th>IT code</th>
								<td>${profile.userId}</td>
							</tr>
						</#if>

						<#if profile.student && profile.homeAddress??>
							<tr class="address">
								<th>Home address</th>
								<td><@profile_macros.address profile.homeAddress /></td>
							</tr>
						</#if>
					</tbody>
				</table>
			</div>
		</div>

		<#if isSelf>
			<div style="margin-top: 12px;"><span class="use-tooltip" data-placement="bottom" title="Your profile is only visible to you, and to staff who have permission to see student records.">Who can see this information?</span></div>
		</#if>
	</section>

	<#if (profile.studentCourseDetails)?? && (profile.mostSignificantCourseDetails)??>

		<!-- most significant course first -->
		<#assign studentCourseDetails=profile.mostSignificantCourseDetails>

		<#if profile.studentCourseDetails?size gt 1>
			<hr>
			<h3>Course: <@fmt.course_description_for_heading studentCourseDetails /></h3>
		</#if>

		<div class="tabbable">
			<#if features.personalTimetables>
				<script type="text/javascript">
					var weeks = ${weekRangesDumper()}
				</script>
			</#if>
			<ol class="panes">

				<#if features.personalTimetables>
					<li id="timetable-pane">
						<section id="timetable-details" class="clearfix" >
						<h4>Timetable</h4>
						<div class='fullCalendar' data-viewname='agendaWeek' data-studentid='${studentCourseDetails.student.universityId}'>
						</div>
            </section>
					</li>
				</#if>

				<li id="course-pane">
					<#include "_course_details.ftl" />
				</li>


				<#if features.profilesMemberNotes>
					<li id="membernote-pane"">
						<#include "_member_notes.ftl" />
					</li>
				</#if>
				
				<#list (studentCourseDetails.department.displayedStudentRelationshipTypes)![] as relationshipType>
					<#if studentCourseDetails.hasRelationship(relationshipType) || relationshipType.displayIfEmpty(studentCourseDetails)>
						<li id="${relationshipType.id}-pane">
							<@profile_macros.relationship_section studentCourseDetails relationshipType meetingsById[relationshipType.id] />
						</li>
					</#if>
				</#list>

				<#if numSmallGroups gt 0>
					<li id="sg-pane" style="display:none;">
						<#include "_small_groups.ftl" />
					</li>
				</#if>
			</ol>

			<div id="note-modal" class="modal hide fade" style="display:none;"></div>
			<div id="modal" class="modal hide fade" style="display:none;"></div>

				<div id="modal-change-agent" class="modal hide fade"></div>
		
				<script type="text/javascript">
				jQuery(function($){
					// load edit personal agent
					$(".relationship-section").on("click", ".edit-agent-link, .add-agent-link", function(e) {
						e.preventDefault();
						var url = $(this).attr('href');
						$("#modal-change-agent").load(url,function(){
							$("#modal-change-agent").modal('show');
						});
					});
				});
				</script>

			<script type="text/javascript">
				jQuery(function($){
				// load edit member note
					$(".membernote").on("click", ".membernote_create", function(e) {
						e.preventDefault();
						var url = $(this).attr('href');
						$("#note-modal").load(url,function(){
							$("#note-modal").modal('show');
						});
					});
				});
			</script>
		</div>


		<!-- and then the others -->
		<#list profile.studentCourseDetails as studentCourseDetails>
			<#if studentCourseDetails.scjCode != profile.mostSignificantCourseDetails.scjCode>
				<#if profile.studentCourseDetails?size gt 1>
					<hr>
					<h3>Course: <@fmt.course_description_for_heading studentCourseDetails /></h3>
				</#if>

				<#include "_course_details.ftl" />

			</#if>
		</#list>
	</#if>
</article>

<#if user.sysadmin>
	<div class="alert alert-info sysadmin-only-content" style="margin-top: 2em;">
		<button type="button" class="close" data-dismiss="alert">&times;</button>

		<h4>Sysadmin-only actions</h4>

		<p>This is only shown to Tabula system administrators. Click the &times; button to see the page as a non-administrator sees it.</p>

		<@f.form method="post" action="${url('/sysadmin/import-profiles/' + profile.universityId, '/scheduling')}">
			<button class="btn btn-large" type="submit">Re-import details from Membership, SITS and about a billion other systems</button>
		</@f.form>
	</div>
</#if>
</#escape>
