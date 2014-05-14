<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

<h1>Create a scheme</h1>

<p class="progress-arrows">
	<span class="arrow-right">Properties</span>
	<span class="arrow-right arrow-left active">Students</span>
	<span class="arrow-right arrow-left">Points</span>
</p>

<@f.form id="newSchemeAddStudents" method="POST" commandName="command">

	<#if command.scheme.members.members?size == 0>
		<p>Select which students this scheme should apply to</p>

		<p>
			<span class="student-count">
				<@fmt.p command.scheme.members.members?size "student" /> on this scheme
			</span>
			<#assign popoverContent><#noescape>
				You can filter to select types of students (e.g. 1st year part-time UG)
				and then either use these to populate a stati list (which will not then change),
				or link this group to SITS so that the list of students will be updated automatically from there.
				You can also manually add students by ITS usercode or university number.

				You can tweak the list even when it is linked to SITS, by manually adding and excluding students.
			</#noescape></#assign>
			<@fmt.help_popover id="student-count" content="${popoverContent}" />
		</p>

		<p>
			<input
				type="submit"
				class="btn use-tooltip"
				name="${chooseStudentsString}"
				value="Add by type"
				data-container="body"
				title="Filter to select students by route, year of study etc"
			/>

			<a
				class="btn use-tooltip mass-add-users-button"
				data-container="body"
				title="Add a list of students by university ID or username"
			>
				Add manually
			</a>
		</p>

		<div class="membership">
			<#include "studentstable.ftl" />
		</div>

	<#else>

		<details>
			<summary class="large-chevron collapsible">
				<span class="legend">Students
					<small>Select which students this scheme should apply to</small>
				</span>

				<p>
					<span class="student-count">
						<@fmt.p command.scheme.members.members?size "student" /> on this scheme
					</span>
				</p>

				<p>
					<input
							type="submit"
							class="btn use-tooltip"
							name="${chooseStudentsString}"
							value="Add by type"
							data-container="body"
							title="Filter to select students by route, year of study etc"
							/>

					<a
							class="btn use-tooltip mass-add-users-button"
							data-container="body"
							title="Add a list of students by university ID or username"
							>
						Add manually
					</a>
				</p>
			</summary>

			<div class="membership">
				<#include "studentstable.ftl" />
			</div>
		</details>

	</#if>

	<p>&nbsp;</p>

	<p>
		<input
				type="submit"
				class="btn btn-success use-tooltip"
				name="${createAndAddPointsString}"
				value="Add points"
				title="Select which monitoring points this scheme should use"
				data-container="body"
				/>
		<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="create"
				value="Save"
				title="Save your blank scheme and add points to it later"
				data-container="body"
				/>
		<a class="btn" href="<@routes.manageHomeForYear command.scheme.department command.scheme.academicYear.startYear?c />">Cancel</a>
	</p>


	<div class="mass-add-users-modal modal fade hide" data-href="<@routes.manageUpdateMembership command.scheme />">
		<@modal.header>
			<h6>Add students manually</h6>
		</@modal.header>

		<@modal.body>
			<p>Type or paste in a list of usercodes or University numbers here, separated by white space, then click <code>Add</code>.</p>
			<textarea rows="6" class="input-block-level" name="massAddUsers"></textarea>
		</@modal.body>

		<@modal.footer>
			<a class="btn btn-success disabled spinnable spinner-auto add-students">Add</a>
		</@modal.footer>
	</div>
</@f.form>

</#escape>