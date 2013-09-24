<#import "../membernote/member_notes_macros.ftl" as member_notes_macros />

<#assign canDeletePurgeMemberNote = can.do("MemberNotes.Delete", profile) />
<#assign canEditMemberNote = can.do("MemberNotes.Update", profile) />
<#assign canCreateMemberNote = can.do("MemberNotes.Create", profile) />

<section id="membernote-details" class="clearfix membernote">
	<h4>Administrative notes</h4>

	<#if memberNotes??><@member_notes_macros.member_note_list memberNotes /></#if>



</section>


