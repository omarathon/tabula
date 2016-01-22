<#macro allNotes notes>
	<div class="allNotes">
		<table class="table">
			<tbody>
				<#list notes?keys as point>
					<#assign noteWithCheckpoint = mapGet(notes, point) />
					<#assign note = noteWithCheckpoint._1() />
					<tr class="point">
						<td class="point"><div class="attendance-note-record"><@attendancenote  point=point note=note checkpoint=noteWithCheckpoint._2()/></div>
						</td>
					</tr>
				</#list>

			</tbody>

		</table>
	</div>
</#macro>


<#macro checkpointNotes  checkpointNoteList type>
  <div class = "note-state ${type}">
	<table class="table">
		<tbody>
		<#list checkpointNoteList as checkpointNote>
			<#assign note = checkpointNote._1() />
			<#assign checkpoint = checkpointNote._2() />
				<tr class="point">
					<td class="point">
						<div class="attendance-note-record"><@attendancenote  point=checkpoint.point note=note checkpoint=checkpoint/></div>
					</td>
				</tr>
			</#list>

		</tbody>

	</table>
  </div>
</#macro>


<#macro unrecordedNotes  monitoringPointNoteList>
	<div class="note-state not-recorded">
		<table class="table">
			<tbody>
				<#list monitoringPointNoteList as monitoringPointNote>
					<#assign point = monitoringPointNote._1() />
					<#assign note = monitoringPointNote._2() />
				<tr class="point">

					<td class="point">
						<div class="attendance-note-record"><@attendancenote  point=monitoringPointNote._1() note=monitoringPointNote._2()/></div>

					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</div>
</#macro>


<#macro attendancenote  point note checkpoint="">
	${point.name}<div>(<@fmt.interval point.startDate point.endDate />)</div>
	<#if note.note?has_content><div>"${note.note}"</div></#if>
	<#if checkpoint?has_content>
		<div class="attendance-note-record"> <p>Recorded as ${checkpoint.state.description} by
			<@userlookup  id=checkpoint.updatedBy>
				<#if returned_user.foundUser>
				${returned_user.fullName}
				<#else>
				${checkpoint.updatedBy}
				</#if>
			</@userlookup>
		</div>
		<@fmt.date checkpoint.updatedDate />
	</#if>
</#macro>