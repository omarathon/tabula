--------------------------------------------------------
--  File created - Wednesday-January-16-2019   
--------------------------------------------------------
--------------------------------------------------------
--  DDL for Table ACCREDITEDPRIORLEARNING
--------------------------------------------------------

create table accreditedpriorlearning
(
  id              varchar(255) not null,
  scjcode         varchar(20)  not null,
  awardcode       varchar(20)  not null,
  sequencenumber  varchar(10)  not null,
  academicyear    smallint     not null,
  cats            numeric(5, 2),
  levelcode       varchar(20),
  reason          varchar(200),
  lastupdateddate timestamp(6),
  hib_version     numeric,
  constraint pk_accreditedpriorlearning primary key (id),
  constraint idx_apl_notional_key unique (scjcode, awardcode, sequencenumber)
);

--------------------------------------------------------
--  DDL for Table ADDRESS
--------------------------------------------------------

create table address
(
  id        varchar(255) not null,
  line1     varchar(255),
  line2     varchar(255),
  line3     varchar(255),
  line4     varchar(255),
  line5     varchar(255),
  postcode  varchar(100),
  telephone varchar(255),
  constraint pk_address primary key (id)
);

--------------------------------------------------------
--  DDL for Table ASSESSMENTGROUP
--------------------------------------------------------

create table assessmentgroup
(
  id            varchar(255) not null,
  assignment_id varchar(255),
  upstream_id   varchar(255) not null,
  occurrence    varchar(255),
  group_set_id  varchar(255),
  exam_id       varchar(255),
  constraint pk_assessmentgroup primary key (id)
);

create index idx_assgroup_assignment on assessmentgroup (assignment_id);
create index idx_assgroup_sgs on assessmentgroup (group_set_id);

--------------------------------------------------------
--  DDL for Table ASSIGNMENT
--------------------------------------------------------

create table assignment
(
  id                       varchar(255)           not null,
  academicyear             smallint               not null,
  attachmentlimit          int                    not null,
  closedate                timestamp(6),
  fileextensions           varchar(255),
  name                     varchar(255),
  opendate                 timestamp(6),
  module_id                varchar(255),
  collectmarks             boolean                not null,
  deleted                  boolean  default false not null,
  collectsubmissions       boolean  default false not null,
  restrictsubmissions      boolean  default false not null,
  allowlatesubmissions     boolean  default true  not null,
  allowresubmission        boolean  default false not null,
  displayplagiarismnotice  boolean  default false not null,
  membersgroup_id          varchar(255),
  archived                 boolean  default false not null,
  createddate              timestamp(6),
  allowextensions          boolean  default false,
  markscheme_id            varchar(255),
  feedback_template_id     varchar(255),
  openended                boolean  default false not null,
  summative                boolean  default true  not null,
  genericfeedback          text,
  dissertation             boolean  default false not null,
  settings                 text,
  turnitin_id              varchar(255),
  hidden_from_students     boolean  default false,
  submittoturnitin         boolean  default false not null,
  lastsubmittedtoturnitin  timestamp(6),
  submittoturnitinretries  smallint default 0,
  openendedreminderdate    timestamp(6),
  workflow_category        varchar(50),
  cm2_workflow_id          varchar(255),
  cm2assignment            boolean  default false not null,
  anonymous_marking_method varchar(255),
  constraint pk_assignment primary key (id),
  constraint chk_assignment_dates check (opendate <= closedate)
);

create index idx_assignment_arcdel on assignment (archived, deleted);
create index idx_assignment_created on assignment (createddate);
create index idx_assignment_feedbacktemplate on assignment (feedback_template_id);
create index idx_assignment_hfs on assignment (hidden_from_students);
create index idx_assignment_module on assignment (module_id);
create index idx_assignment_cm2workflow on assignment (cm2_workflow_id);

--------------------------------------------------------
--  DDL for Table ATTENDANCEMONITORINGCHECKPOINT
--------------------------------------------------------

create table attendancemonitoringcheckpoint
(
  id           varchar(255) not null,
  point_id     varchar(255) not null,
  student_id   varchar(100) not null,
  state        varchar(20)  not null,
  updated_date timestamp(6) not null,
  updated_by   varchar(255) not null,
  autocreated  boolean      not null,
  constraint pk_attendancemonitoringcheckpoint primary key (id),
  constraint idx_amc_pointstudent unique (point_id, student_id)
);

create index idx_amc_point_id on attendancemonitoringcheckpoint (point_id);
create index idx_amc_student_id on attendancemonitoringcheckpoint (student_id);

--------------------------------------------------------
--  DDL for Table ATTENDANCEMONITORINGPOINT
--------------------------------------------------------

create table attendancemonitoringpoint
(
  id           varchar(255)  not null,
  scheme_id    varchar(255)  not null,
  name         varchar(4000) not null,
  start_week   smallint,
  end_week     smallint,
  start_date   timestamp(6)  not null,
  end_date     timestamp(6)  not null,
  point_type   varchar(100)  not null,
  created_date timestamp(6)  not null,
  updated_date timestamp(6)  not null,
  settings     text,
  constraint pk_attendancemonitoringpoint primary key (id)
);

create index idx_amp_scheme_id on attendancemonitoringpoint (scheme_id);

--------------------------------------------------------
--  DDL for Table ATTENDANCEMONITORINGSCHEME
--------------------------------------------------------

create table attendancemonitoringscheme
(
  id              varchar(255) not null,
  name            varchar(4000),
  academicyear    smallint     not null,
  membersgroup_id varchar(255) not null,
  member_query    text,
  point_style     varchar(100) not null,
  department_id   varchar(255) not null,
  created_date    timestamp(6) not null,
  updated_date    timestamp(6) not null,
  constraint pk_attendancemonitoringscheme primary key (id)
);

--------------------------------------------------------
--  DDL for Table ATTENDANCEMONITORINGTOTAL
--------------------------------------------------------

create table attendancemonitoringtotal
(
  id                    varchar(255) not null,
  student_id            varchar(100) not null,
  unrecorded            smallint     not null,
  authorized            smallint     not null,
  unauthorized          smallint     not null,
  attended              smallint     not null,
  updated_date          timestamp(6) not null,
  department_id         varchar(255) not null,
  academicyear          smallint     not null,
  low_level_notified    timestamp(6),
  medium_level_notified timestamp(6),
  high_level_notified   timestamp(6),
  constraint pk_attendancemonitoringtotal primary key (id),
  constraint idx_amct_student_dept_year_unique unique (student_id, department_id, academicyear)
);

--------------------------------------------------------
--  DDL for Table ATTENDANCENOTE
--------------------------------------------------------

create table attendancenote
(
  id            varchar(255)                not null,
  student_id    varchar(100)                not null,
  updateddate   timestamp(6),
  updatedby     varchar(255)                not null,
  note          text,
  attachment_id varchar(255),
  discriminator varchar(100)                not null,
  parent_id     varchar(255),
  absence_type  varchar(20) default 'other' not null,
  constraint pk_attendancenote primary key (id),
  constraint idx_an_student_disc_parent unique (student_id, discriminator, parent_id)
);

--------------------------------------------------------
--  DDL for Table ATTENDANCETEMPLATE
--------------------------------------------------------

create table attendancetemplate
(
  id           varchar(255) not null,
  createddate  timestamp(6),
  updateddate  timestamp(6),
  point_style  varchar(100) not null,
  templatename varchar(255) not null,
  position     smallint,
  constraint pk_attendancetemplate primary key (id),
  constraint idx_at_template_name unique (templatename)
);

--------------------------------------------------------
--  DDL for Table ATTENDANCETEMPLATEPOINT
--------------------------------------------------------

create table attendancetemplatepoint
(
  id                 varchar(255)  not null,
  scheme_template_id varchar(255)  not null,
  name               varchar(4000) not null,
  start_week         smallint default 0,
  end_week           smallint default 0,
  start_date         timestamp(6),
  end_date           timestamp(6),
  created_date       timestamp(6)  not null,
  updated_date       timestamp(6)  not null,
  constraint pk_attendancetemplatepoint primary key (id)
);

create index idx_amp_template_scheme_id on attendancetemplatepoint (scheme_template_id);

--------------------------------------------------------
--  DDL for Table AUDITEVENT
--------------------------------------------------------

create table auditevent
(
  id                 numeric(38)  not null,
  eventdate          timestamp(6),
  eventtype          varchar(255) not null,
  eventstage         varchar(64)  not null,
  real_user_id       varchar(255),
  masquerade_user_id varchar(255),
  data               text,
  eventid            varchar(36),
  ip_address         varchar(255),
  user_agent         varchar(4000),
  read_only          boolean,
  constraint pk_auditevent primary key (id)
);

create sequence auditevent_seq increment by 1;
create index idx_auditevent_eventid on auditevent (eventid);
create index idx_auditevent_eventdate on auditevent (eventdate);

--------------------------------------------------------
--  DDL for Table AWARD
--------------------------------------------------------

create table award
(
  code            varchar(20) not null,
  shortname       varchar(20),
  name            varchar(300),
  lastupdateddate timestamp(6),
  constraint pk_award primary key (code)
);

--------------------------------------------------------
--  DDL for Table COREREQUIREDMODULE
--------------------------------------------------------

create table corerequiredmodule
(
  id           varchar(255) not null,
  routecode    varchar(20)  not null,
  academicyear smallint     not null,
  yearofstudy  smallint     not null,
  modulecode   varchar(20)  not null,
  constraint pk_corerequiredmodule primary key (id),
  constraint idx_corerequiredmodule_unique unique (id, routecode, academicyear, yearofstudy, modulecode)
);

--------------------------------------------------------
--  DDL for Table COURSE
--------------------------------------------------------

create table course
(
  code            varchar(20) not null,
  shortname       varchar(20),
  name            varchar(100),
  title           varchar(200),
  lastupdateddate timestamp(6),
  department_id   varchar(255),
  inuse           boolean default true,
  constraint pk_course primary key (code)
);

--------------------------------------------------------
--  DDL for Table COURSEYEARWEIGHTING
--------------------------------------------------------

create table courseyearweighting
(
  id           varchar(255)  not null,
  coursecode   varchar(255)  not null,
  academicyear smallint      not null,
  yearofstudy  smallint      not null,
  weighting    numeric(4, 3) not null,
  constraint pk_courseyearweighting primary key (id),
  constraint idx_courseyearweighting_unique unique (coursecode, academicyear, yearofstudy)
);

--------------------------------------------------------
--  DDL for Table CUSTOMROLEDEFINITION
--------------------------------------------------------

create table customroledefinition
(
  id                        varchar(255) not null,
  department_id             varchar(255) not null,
  name                      varchar(255) not null,
  custom_base_role_id       varchar(255),
  builtinbaseroledefinition varchar(255),
  hib_version               numeric default 0,
  candelegate               boolean default false,
  replaces_parent           boolean default false,
  constraint pk_customroledefinition primary key (id)
);

create index idx_customroledefinition_customid on customroledefinition (custom_base_role_id);
create index idx_customroledefinition_departmentrp on customroledefinition (department_id, replaces_parent);

--------------------------------------------------------
--  DDL for Table DEPARTMENT
--------------------------------------------------------

create table department
(
  id             varchar(255) not null,
  code           varchar(255),
  name           varchar(255),
  settings       text,
  parent_id      varchar(255),
  filterrulename varchar(128),
  shortname      varchar(255),
  constraint pk_department primary key (id),
  constraint idx_department_code unique (code)
);

create index idx_department_parent on department (parent_id);

--------------------------------------------------------
--  DDL for Table DEPARTMENTSMALLGROUP
--------------------------------------------------------

create table departmentsmallgroup
(
  id               varchar(255) not null,
  set_id           varchar(255),
  name             varchar(255),
  studentsgroup_id varchar(255),
  constraint pk_departmentsmallgroup primary key (id)
);

create index idx_departmentsmallgroup_set on departmentsmallgroup (set_id);

--------------------------------------------------------
--  DDL for Table DEPARTMENTSMALLGROUPSET
--------------------------------------------------------

create table departmentsmallgroupset
(
  id              varchar(255) not null,
  department_id   varchar(255) not null,
  academicyear    smallint     not null,
  name            varchar(255),
  archived        boolean default false,
  deleted         boolean default false,
  membersgroup_id varchar(255),
  member_query    text,
  constraint pk_departmentsmallgroupset primary key (id)
);

create index idx_departmentsmallgroupset_deleted on departmentsmallgroupset (deleted);
create index idx_departmentsmallgroupset_department on departmentsmallgroupset (department_id);

--------------------------------------------------------
--  DDL for Table DISABILITY
--------------------------------------------------------

create table disability
(
  code             varchar(20) not null,
  shortname        varchar(20),
  sitsdefinition   varchar(300),
  tabuladefinition varchar(30),
  lastupdateddate  timestamp(6),
  constraint pk_disability primary key (code)
);

--------------------------------------------------------
--  DDL for Table ENTITYREFERENCE
--------------------------------------------------------

create table entityreference
(
  id              varchar(255) not null,
  entity_type     varchar(255) not null,
  entity_id       varchar(255) not null,
  notification_id varchar(255),
  constraint pk_entityreference primary key (id)
);

create index idx_entityreference_notification on entityreference (notification_id);

--------------------------------------------------------
--  DDL for Table EXAM
--------------------------------------------------------

create table exam
(
  id              varchar(255)          not null,
  name            varchar(255),
  academicyear    smallint              not null,
  module_id       varchar(255),
  membersgroup_id varchar(255),
  deleted         boolean default false not null,
  workflow_id     varchar(255),
  released        boolean default false,
  constraint pk_exam primary key (id)
);

create index idx_exam_module on exam (module_id);

--------------------------------------------------------
--  DDL for Table EXTENSION
--------------------------------------------------------

create table extension
(
  id                   varchar(255)             not null,
  expirydate           timestamp(6),
  universityid         varchar(255),
  userid               varchar(255)             not null,
  assignment_id        varchar(255),
  approvedon           timestamp(6),
  approvalcomments     varchar(4000),
  requestedexpirydate  timestamp(6),
  requestedon          timestamp(6),
  disabilityadjustment boolean    default false not null,
  state                varchar(2) default 'U'   not null,
  reason               text,
  constraint pk_extension primary key (id),
  constraint idx_extension_ck unique (universityid, assignment_id)
);

create index idx_extension_assignment on extension (assignment_id);
create index idx_extension_state on extension (state);
create index idx_extension_user on extension (userid);
create index idx_extension_disabilityadjustment on extension (disabilityadjustment);

--------------------------------------------------------
--  DDL for Table FEEDBACK
--------------------------------------------------------

create table feedback
(
  id                     varchar(255)                      not null,
  uploaderid             varchar(255),
  uploaded_date          timestamp(6)                      not null,
  universityid           varchar(255),
  assignment_id          varchar(255),
  released               boolean      default false        not null,
  rating                 smallint,
  ratingprompt           boolean,
  ratinghelpful          boolean,
  actualmark             int,
  actualgrade            varchar(255),
  agreedmark             int,
  agreedgrade            varchar(255),
  first_marker_feedback  varchar(255),
  second_marker_feedback varchar(255),
  released_date          timestamp(6),
  third_marker_feedback  varchar(255),
  updated_date           timestamp(6)                      not null,
  discriminator          varchar(100) default 'assignment' not null,
  exam_id                varchar(255),
  userid                 varchar(255),
  anonymousid            int,
  constraint pk_feedback primary key (id),
  constraint idx_feedback_ck unique (universityid, assignment_id, exam_id)
);

create index idx_feedback_markerfeedback1 on feedback (first_marker_feedback);
create index idx_feedback_markerfeedback2 on feedback (second_marker_feedback);
create index idx_feedback_markerfeedback3 on feedback (third_marker_feedback);
create index idx_feedback_assignment on feedback (assignment_id);

--------------------------------------------------------
--  ddl for table feedbackforsits
--------------------------------------------------------

create table feedbackforsits
(
  id                      varchar(255) not null,
  feedback_id             varchar(255) not null,
  status                  varchar(100),
  firstcreatedon          timestamp(6),
  lastinitialisedon       timestamp(6),
  dateofupload            timestamp(6),
  actualmarklastuploaded  int,
  actualgradelastuploaded varchar(2),
  initialiser             varchar(250),
  hib_version             numeric,
  constraint pk_feedbackforsits primary Key (id)
);

create index idx_feedbackforsits_feedback on feedbackforsits (feedback_id);

--------------------------------------------------------
--  ddl for table feedbacktemplate
--------------------------------------------------------

create table feedbacktemplate
(
  id            varchar(255) not null,
  name          varchar(255),
  description   varchar(4000),
  department_id varchar(255),
  attachment_id varchar(255),
  constraint pk_feedbacktemplate primary key (id)
);

create index idx_feedbacktemplate_attachment on feedbacktemplate (attachment_id);
create index idx_feedbacktemplate_department on feedbacktemplate (department_id);

--------------------------------------------------------
--  ddl for table fileattachment
--------------------------------------------------------

create table fileattachment
(
  id               varchar(255) not null,
  name             varchar(255),
  temporary        boolean      not null,
  feedback_id      varchar(255),
  submission_id    varchar(255),
  dateuploaded     timestamp(6) not null,
  extension_id     varchar(255),
  file_hash        varchar(255),
  meetingrecord_id varchar(255),
  member_note_id   varchar(255),
  uploadedby       varchar(255),
  constraint pk_fileattachment primary key (id)
);

create index idx_fileattachment_extension on fileattachment (extension_id);
create index idx_fileattachment_feedback on fileattachment (feedback_id);
create index idx_fileattachment_meetingrecord on fileattachment (meetingrecord_id);
create index idx_fileattachment_membernote on fileattachment (member_note_id);
create index idx_fileattachment_submission on fileattachment (submission_id);
create index idx_fileattachment_temporary on fileattachment (temporary, dateuploaded);

--------------------------------------------------------
--  ddl for table fileattachmenttoken
--------------------------------------------------------

create table fileattachmenttoken
(
  id                varchar(255) not null,
  expires           timestamp(6),
  date_used         timestamp(6),
  fileattachment_id varchar(255),
  constraint pk_fileattachmenttoken primary key (id)
);

--------------------------------------------------------
--  ddl for table formfield
--------------------------------------------------------

create table formfield
(
  id            varchar(255)                     not null,
  assignment_id varchar(255),
  name          varchar(255)                     not null,
  position      smallint                         not null,
  label         varchar(255),
  instructions  varchar(4000),
  fieldtype     varchar(255)                     not null,
  required      boolean                          not null,
  properties    varchar(4000)                    not null,
  context       varchar(50) default 'submission' not null,
  exam_id       varchar(255),
  constraint pk_formfield primary key (id)
);

create index idx_formfield_assignment on formfield (assignment_id);

--------------------------------------------------------
--  DDL for Table GRADEBOUNDARY
--------------------------------------------------------

create table gradeboundary
(
  id           varchar(255) not null,
  markscode    varchar(100) not null,
  grade        varchar(100) not null,
  minimummark  smallint     not null,
  maximummark  smallint     not null,
  signalstatus varchar(100) not null,
  constraint pk_gradeboundary primary key (id)
);

create index idx_gradeboundary_markscode on gradeboundary (markscode);

--------------------------------------------------------
--  DDL for Table GRANTEDPERMISSION
--------------------------------------------------------

create table grantedpermission
(
  id           varchar(255) not null,
  usergroup_id varchar(255),
  permission   varchar(255) not null,
  overridetype boolean default false,
  scope_type   varchar(255) not null,
  scope_id     varchar(255),
  hib_version  numeric default 0,
  constraint pk_grantedpermission primary key (id)
);

create index idx_grantedpermission_scope on grantedpermission (scope_type, scope_id);
create index idx_grantedpermission_type on grantedpermission (scope_type, scope_id, permission, overridetype);
create index idx_grantedpermission_group on grantedpermission (usergroup_id);

--------------------------------------------------------
--  DDL for Table GRANTEDROLE
--------------------------------------------------------

create table grantedrole
(
  id                    varchar(255) not null,
  usergroup_id          varchar(255),
  custom_role_id        varchar(255),
  builtinroledefinition varchar(255),
  scope_type            varchar(255) not null,
  scope_id              varchar(255),
  hib_version           numeric default 0,
  constraint pk_grantedrole primary key (id),
  constraint idx_grantedrole_type unique (scope_type, scope_id, custom_role_id, builtinroledefinition)
);

create index idx_grantedrole_scope on grantedrole (scope_type, scope_id);
create index idx_grantedrole_group on grantedrole (usergroup_id);

--------------------------------------------------------
--  DDL for Table JOB
--------------------------------------------------------

create table job
(
  id           varchar(100)          not null,
  jobtype      varchar(100)          not null,
  status       varchar(4000),
  data         text                  not null,
  started      boolean default false not null,
  finished     boolean default false not null,
  succeeded    boolean default false not null,
  progress     smallint,
  createddate  timestamp(6)          not null,
  updateddate  timestamp(6)          not null,
  realuser     varchar(255),
  apparentuser varchar(255),
  instance     varchar(255),
  constraint pk_job primary key (id)
);

create index idx_jobstarted on job (started, finished);

--------------------------------------------------------
--  DDL for Table MARK
--------------------------------------------------------

create table mark
(
  id           varchar(100) not null,
  feedback_id  varchar(100) not null,
  uploaderid   varchar(255) not null,
  uploadeddate timestamp(6) not null,
  marktype     varchar(100) not null,
  mark         int          not null,
  grade        varchar(255),
  reason       varchar(600),
  comments     text,
  constraint pk_mark primary key (id)
);

create index idx_mark_feedback on mark (feedback_id);

--------------------------------------------------------
--  DDL for Table MARKERFEEDBACK
--------------------------------------------------------

create table markerfeedback
(
  id                varchar(255) not null,
  mark              int,
  state             varchar(255),
  uploaded_date     timestamp(6) not null,
  feedback_id       varchar(255),
  grade             varchar(255),
  rejectioncomments text,
  deleted           boolean default false,
  marker            varchar(255),
  stage             varchar(255),
  updated_on        timestamp(6),
  constraint pk_markerfeedback primary key (id),
  constraint idx_mf_stage unique (feedback_id, marker, stage)
);

create index idx_markerfeedback_stage on markerfeedback (stage);

--------------------------------------------------------
--  DDL for Table MARKERFEEDBACKATTACHMENT
--------------------------------------------------------

create table markerfeedbackattachment
(
  marker_feedback_id varchar(255) not null,
  file_attachment_id varchar(255) not null,
  constraint pk_markerfeedbackattachment primary key (marker_feedback_id, file_attachment_id)
);

create index idx_markerfeedback_attachment on markerfeedbackattachment (file_attachment_id);
create index idx_markerfeedback_feedback on markerfeedbackattachment (marker_feedback_id);

--------------------------------------------------------
--  DDL for Table MARKER_USERGROUP
--------------------------------------------------------

create table marker_usergroup
(
  id            varchar(255)                 not null,
  assignment_id varchar(255),
  markermap_id  varchar(255),
  marker_uni_id varchar(255),
  discriminator varchar(100) default 'first' not null,
  exam_id       varchar(255),
  constraint pk_marker_usergroup primary key (id)
);

--------------------------------------------------------
--  DDL for Table MARKINGDESCRIPTOR
--------------------------------------------------------

create table markingdescriptor
(
  id            varchar(255) not null,
  discriminator char(1)      not null,
  department_id varchar(255),
  min_mark      int          not null,
  max_mark      int          not null,
  text          text         not null,
  constraint pk_markingdescriptor primary key (id)
);

create index idx_markingdescriptor_department on markingdescriptor (department_id);

--------------------------------------------------------
--  DDL for Table MARKINGWORKFLOW
--------------------------------------------------------

create table markingworkflow
(
  id            varchar(255)          not null,
  workflowtype  varchar(255)          not null,
  name          varchar(255)          not null,
  department_id varchar(255)          not null,
  is_reusable   boolean default false not null,
  academicyear  smallint              not null,
  settings      text,
  constraint pk_markingworkflow primary key (id)
);

create index idx_markingworkflow_department on markingworkflow (department_id);

--------------------------------------------------------
--  DDL for Table MARKSCHEME
--------------------------------------------------------

create table markscheme
(
  id                   varchar(255)          not null,
  name                 varchar(255)          not null,
  department_id        varchar(255),
  firstmarkers_id      varchar(255),
  secondmarkers_id     varchar(255),
  studentschoosemarker boolean default false not null,
  markingmethod        varchar(255),
  constraint pk_markscheme primary key (id)
);

create index idx_markscheme_department on markscheme (department_id);

--------------------------------------------------------
--  DDL for Table MEETINGRECORD
--------------------------------------------------------

create table meetingrecord
(
  id                varchar(255)                   not null,
  relationship_id   varchar(255),
  creation_date     timestamp(6)                   not null,
  last_updated_date timestamp(6)                   not null,
  creator_id        varchar(255)                   not null,
  meeting_date      timestamp(6),
  title             varchar(500),
  description       text,
  meeting_format    varchar(50),
  deleted           boolean     default false,
  discriminator     varchar(10) default 'standard' not null,
  missed            boolean     default false,
  missed_reason     text,
  real_time         boolean     default false      not null,
  meeting_end_date  timestamp(6),
  meeting_location  varchar(255),
  constraint pk_meetingrecord primary key (id)
);

create index idx_meetingrecord_disriminator on meetingrecord (discriminator);
create index idx_meetingrecord_missed on meetingrecord (missed);
create index idx_meetingrecord_relationship on meetingrecord (relationship_id);
create index idx_meetingrecord_list on meetingrecord (deleted, creator_id, discriminator);

--------------------------------------------------------
--  DDL for Table MEETINGRECORDAPPROVAL
--------------------------------------------------------

create table meetingrecordapproval
(
  id               varchar(255) not null,
  meetingrecord_id varchar(255) not null,
  approver_id      varchar(255) not null,
  lastupdateddate  timestamp(6),
  comments         text,
  approval_state   varchar(50),
  creation_date    timestamp(6),
  approved_by      varchar(255),
  constraint pk_meetingrecordapproval primary key (id)
);

create index idx_meetingrecordapproval_member on meetingrecordapproval (approver_id);
create index idx_meetingrecordapproval_record on meetingrecordapproval (meetingrecord_id);

--------------------------------------------------------
--  DDL for Table MEETINGRECORDRELATIONSHIP
--------------------------------------------------------

create table meetingrecordrelationship
(
  meeting_record_id varchar(255) not null,
  relationship_id   varchar(255) not null,
  constraint pk_meetingrecordrelationship primary key (meeting_record_id, relationship_id)
);

create index idx_meetingrecordrelationship_meeting on meetingrecordrelationship (meeting_record_id);

--------------------------------------------------------
--  DDL for Table MEMBER
--------------------------------------------------------

create table member
(
  universityid           varchar(100) not null,
  userid                 varchar(100),
  firstname              varchar(255),
  lastname               varchar(255),
  email                  varchar(4000),
  title                  varchar(100),
  fullfirstname          varchar(255),
  gender                 varchar(1),
  nationality            varchar(100),
  homeemail              varchar(255),
  mobilenumber           varchar(100),
  photo_id               varchar(255),
  inuseflag              varchar(100),
  inactivationdate       timestamp(6),
  groupname              varchar(255),
  home_department_id     varchar(255),
  dateofbirth            date,
  teachingstaff          boolean,
  home_address_id        varchar(255),
  termtime_address_id    varchar(255),
  lastupdateddate        timestamp(6),
  usertype               varchar(100),
  hib_version            numeric default 0,
  phonenumber            varchar(100),
  jobtitle               varchar(255),
  mostsignificantcourse  varchar(20),
  missingfromimportsince timestamp(6),
  assistantsgroup_id     varchar(255),
  tier4_visa_requirement boolean,
  disability             varchar(20),
  timetable_hash         varchar(100),
  settings               text,
  deceased               boolean default false,
  lastimportdate         timestamp(6),
  secondnationality      varchar(100),
  disability_funding     varchar(2),
  constraint pk_member primary key (universityid)
);

create index idx_member_homedepartment on member (home_department_id);
create index idx_member_userid on member (userid);
create index idx_member_usertype on member (usertype);
create index idx_staffmember_assistants on member (assistantsgroup_id);

--------------------------------------------------------
--  DDL for Table MEMBERNOTE
--------------------------------------------------------

create table membernote
(
  id              varchar(255)               not null,
  memberid        varchar(255)               not null,
  note            text,
  title           varchar(500),
  creatorid       varchar(255)               not null,
  creationdate    timestamp(6)               not null,
  lastupdateddate timestamp(6)               not null,
  deleted         boolean     default false,
  discriminator   varchar(13) default 'note' not null,
  start_date      timestamp(6),
  end_date        timestamp(6),
  constraint pk_membernote primary key (id)
);

create index idx_membernote_member on membernote (memberid);
create index idx_membernote_discriminator on membernote (discriminator);

--------------------------------------------------------
--  DDL for Table MODEOFATTENDANCE
--------------------------------------------------------

create table modeofattendance
(
  code            varchar(6) not null,
  shortname       varchar(15),
  fullname        varchar(50),
  lastupdateddate timestamp(6),
  constraint pk_modeofattendance primary key (code)
);

--------------------------------------------------------
--  DDL for Table MODULE
--------------------------------------------------------

create table module
(
  id                     varchar(255) not null,
  active                 boolean      not null,
  code                   varchar(255),
  name                   varchar(255),
  department_id          varchar(255),
  missingfromimportsince timestamp(6),
  degreetype             varchar(100),
  shortname              varchar(15),
  constraint pk_module primary key (id),
  constraint idx_module_code unique (code)
);

create index idx_module_department on module (department_id);

--------------------------------------------------------
--  DDL for Table MODULEREGISTRATION
--------------------------------------------------------

create table moduleregistration
(
  id                  varchar(250) not null,
  scjcode             varchar(20)  not null,
  modulecode          varchar(20)  not null,
  academicyear        smallint     not null,
  cats                numeric(5, 2),
  assessmentgroup     varchar(2),
  selectionstatuscode varchar(6),
  lastupdateddate     timestamp(6),
  hib_version         numeric,
  occurrence          varchar(10),
  agreedmark          int,
  agreedgrade         varchar(2),
  actualmark          int,
  actualgrade         varchar(2),
  deleted             boolean default false,
  passfail            boolean default false,
  constraint pk_moduleregistration primary key (id),
  constraint idx_moduleregistration_notional_key unique (scjcode, modulecode, academicyear, cats, occurrence)
);

create index idx_moduleregistration_module on moduleregistration (modulecode);

--------------------------------------------------------
--  DDL for Table MODULETEACHINGINFORMATION
--------------------------------------------------------

create table moduleteachinginformation
(
  id            varchar(255) not null,
  department_id varchar(255) not null,
  module_id     varchar(255) not null,
  percentage    numeric(6, 2),
  constraint pk_moduleteachinginformation primary key (id),
  constraint idx_moduleteachinginformation_ck unique (department_id, module_id)
);

create index idx_moduleteachinginformation_department on moduleteachinginformation (department_id);
create index idx_moduleteachinginformation_module on moduleteachinginformation (module_id);

--------------------------------------------------------
--  DDL for Table MONITORINGCHECKPOINT
--------------------------------------------------------

create table monitoringcheckpoint
(
  id                       varchar(250)                           not null,
  point_id                 varchar(250)                           not null,
  student_course_detail_id varchar(250),
  checked                  boolean      default false,
  createddate              timestamp(6),
  createdby                varchar(255),
  state                    varchar(20)  default 'attended'        not null,
  updateddate              timestamp(6) default current_timestamp not null,
  updatedby                varchar(255) default ''                not null,
  autocreated              boolean      default false,
  student_id               varchar(100)                           not null,
  constraint pk_monitoringcheckpoint primary key (id),
  constraint idx_monitoringcheckpoint_ck unique (point_id, student_id)
);

create index idx_monitoringcheckpoint_point on monitoringcheckpoint (point_id);
create index idx_monitoringcheckpoint_scd on monitoringcheckpoint (student_course_detail_id);
create index idx_monitoringcheckpoint_student on monitoringcheckpoint (student_id);

--------------------------------------------------------
--  DDL for Table MONITORINGPOINT
--------------------------------------------------------

create table monitoringpoint
(
  id                   varchar(250)       not null,
  point_set_id         varchar(250)       not null,
  name                 varchar(4000)      not null,
  defaultvalue         boolean  default false,
  createddate          timestamp(6),
  updateddate          timestamp(6),
  week                 smallint,
  senttoacademicoffice boolean  default false,
  validfromweek        smallint default 0 not null,
  requiredfromweek     smallint default 0 not null,
  settings             text,
  point_type           varchar(50),
  constraint pk_monitoringpoint primary key (id)
);

create index idx_monitoringpoint_set on monitoringpoint (point_set_id);

--------------------------------------------------------
--  DDL for Table MONITORINGPOINTREPORT
--------------------------------------------------------

create table monitoringpointreport
(
  id                             varchar(255) not null,
  student                        varchar(100) not null,
  student_course_details_id      varchar(20)  not null,
  student_course_year_details_id varchar(250) not null,
  createddate                    timestamp(6) not null,
  pusheddate                     timestamp(6),
  monitoringperiod               varchar(100) not null,
  academicyear                   smallint     not null,
  missed                         smallint     not null,
  reporter                       varchar(100) not null,
  constraint pk_monitoringpointreport primary key (id),
  constraint idx_monitoringpointreport_ck unique (student, monitoringperiod, academicyear)
);

--------------------------------------------------------
--  DDL for Table MONITORINGPOINTSET
--------------------------------------------------------

create table monitoringpointset
(
  id           varchar(250)       not null,
  route_id     varchar(250)       not null,
  year         smallint,
  createddate  timestamp(6),
  updateddate  timestamp(6),
  academicyear smallint default 0 not null,
  migratedto   varchar(255),
  constraint pk_monitoringpointset primary key (id),
  constraint idx_monitoringpointset_ck unique (route_id, year, academicyear)
);

--------------------------------------------------------
--  DDL for Table MONITORINGPOINTSETTEMPLATE
--------------------------------------------------------

create table monitoringpointsettemplate
(
  id           varchar(250) not null,
  createddate  timestamp(6),
  updateddate  timestamp(6),
  templatename varchar(255) not null,
  position     smallint,
  constraint pk_monitoringpointsettemplate primary key (id),
  constraint idx_monitoringpointsettemplate_name unique (templatename)
);

--------------------------------------------------------
--  DDL for Table MONITORINGPOINTTEMPLATE
--------------------------------------------------------

create table monitoringpointtemplate
(
  id               varchar(250)       not null,
  point_set_id     varchar(250)       not null,
  name             varchar(4000)      not null,
  defaultvalue     boolean  default false,
  createddate      timestamp(6),
  updateddate      timestamp(6),
  week             smallint,
  validfromweek    smallint default 0 not null,
  requiredfromweek smallint default 0 not null,
  settings         text,
  point_type       varchar(50),
  constraint pk_monitoringpointtemplate primary key (id)
);

create index idx_monitoringpointtemplate_pointsetid on monitoringpointtemplate (point_set_id);

--------------------------------------------------------
--  DDL for Table NEXTOFKIN
--------------------------------------------------------

create table nextofkin
(
  id           varchar(255) not null,
  member_id    varchar(255),
  address_id   varchar(255),
  firstname    varchar(255),
  lastname     varchar(255),
  relationship varchar(255),
  eveningphone varchar(255),
  email        varchar(255),
  constraint pk_nextofkin primary key (id)
);

create index idx_nextofkin_member on nextofkin (member_id);

--------------------------------------------------------
--  DDL for Table NORMALCATSLOAD
--------------------------------------------------------

create table normalcatsload
(
  id           varchar(255)  not null,
  academicyear smallint      not null,
  routecode    varchar(20)   not null,
  yearofstudy  smallint      not null,
  normalload   numeric(5, 2) not null,
  constraint pk_normalcatsload primary key (id),
  constraint idx_normalcatsload_ck unique (academicyear, routecode, yearofstudy)
);

--------------------------------------------------------
--  DDL for Table NOTIFICATION
--------------------------------------------------------

create table notification
(
  id                    varchar(255) not null,
  notification_type     varchar(255) not null,
  agent                 varchar(255),
  created               timestamp(6) not null,
  target_id             varchar(255),
  settings              text         not null,
  recipientuserid       varchar(255),
  recipientuniversityid varchar(255),
  priority              varchar(255),
  listeners_processed   boolean default true,
  constraint pk_notification primary key (id)
);

create index idx_notification_type on notification (notification_type);

--------------------------------------------------------
--  DDL for Table OBJECTCACHE
--------------------------------------------------------

create table objectcache
(
  key         varchar(100) not null,
  objectdata  bytea,
  createddate timestamp(6),
  constraint pk_objectcache primary key (key)
);

--------------------------------------------------------
--  DDL for Table ORIGINALITYREPORT
--------------------------------------------------------

create table originalityreport
(
  id                      varchar(100)           not null,
  submission_id           varchar(100),
  createddate             timestamp(6)           not null,
  similarity              smallint,
  overlap                 smallint,
  student_overlap         smallint,
  web_overlap             smallint,
  publication_overlap     smallint,
  attachment_id           varchar(100),
  turnitin_id             varchar(255),
  report_received         boolean  default false not null,
  lastsubmittedtoturnitin timestamp(6),
  submittoturnitinretries smallint default 0,
  filerequested           timestamp(6),
  lastreportrequest       timestamp(6),
  reportrequestretries    smallint default 0,
  lastturnitinerror       varchar(1000),
  nextsubmitattempt       timestamp(6),
  submitattempts          smallint default 0,
  submitteddate           timestamp(6),
  nextresponseattempt     timestamp(6),
  responseattempts        smallint default 0,
  responsereceived        timestamp(6),
  reporturl               varchar(500),
  significance            float(10),
  matchcount              int,
  sourcecount             int,
  urkundresponse          text,
  urkundresponsecode      varchar(100),
  constraint pk_originalityreport primary key (id)
);

create index idx_originalityreport_attachment on originalityreport (attachment_id);
create index idx_originalityreport_submission on originalityreport (submission_id);

--------------------------------------------------------
--  DDL for Table OUTSTANDINGSTAGES
--------------------------------------------------------

create table outstandingstages
(
  feedback_id varchar(255) not null,
  stage       varchar(255) not null,
  constraint pk_outstandingstages primary key (feedback_id, stage)
);

create index idx_outstandingstages_feedback on outstandingstages (feedback_id);

--------------------------------------------------------
--  DDL for Table RECIPIENTNOTIFICATIONINFO
--------------------------------------------------------

create table recipientnotificationinfo
(
  id              varchar(255)          not null,
  notification_id varchar(255)          not null,
  recipient       varchar(255)          not null,
  dismissed       boolean default false not null,
  email_sent      boolean default false not null,
  attemptedat     timestamp(6),
  constraint pk_recipientnotificationinfo primary key (id),
  constraint idx_recipientnotificationinfo_ck unique (notification_id, recipient)
);

create index idx_recipientnotificationinfo_dea on recipientnotificationinfo (dismissed, email_sent, attemptedat);

--------------------------------------------------------
--  DDL for Table ROLEOVERRIDE
--------------------------------------------------------

create table roleoverride
(
  id                        varchar(255) not null,
  custom_role_definition_id varchar(255),
  permission                varchar(255) not null,
  overridetype              boolean default false,
  hib_version               numeric default 0,
  constraint pk_roleoverride primary key (id)
);

create index idx_roleoverride_customrole on roleoverride (custom_role_definition_id);

--------------------------------------------------------
--  DDL for Table ROUTE
--------------------------------------------------------

create table route
(
  id                        varchar(255) not null,
  active                    boolean      not null,
  code                      varchar(255),
  name                      varchar(255),
  degreetype                varchar(100),
  department_id             varchar(255),
  missingfromimportsince    timestamp(6),
  teachingdepartmentsactive boolean default false,
  constraint pk_route primary key (id),
  constraint idx_route_code unique (code)
);

create index idx_route_department on route (department_id);

--------------------------------------------------------
--  DDL for Table ROUTETEACHINGINFORMATION
--------------------------------------------------------

create table routeteachinginformation
(
  id            varchar(255) not null,
  department_id varchar(255) not null,
  route_id      varchar(255) not null,
  percentage    numeric(6, 2),
  constraint pk_routeteachinginformation primary key (id),
  constraint idx_routeteachinginformation_ck unique (department_id, route_id)
);

create index idx_routeteachinginformation_department on routeteachinginformation (department_id);
create index idx_routeteachinginformation_route on routeteachinginformation (route_id);

--------------------------------------------------------
--  DDL for Table SCHEDULEDTRIGGER
--------------------------------------------------------

create table scheduledtrigger
(
  id             varchar(255) not null,
  trigger_type   varchar(255) not null,
  scheduled_date timestamp(6) not null,
  target_id      varchar(255),
  completed_date timestamp(6),
  constraint pk_scheduledtrigger primary key (id)
);

create index idx_scheduledtrigger_type on scheduledtrigger (trigger_type);

--------------------------------------------------------
--  DDL for Table SCHEDULED_NOTIFICATION
--------------------------------------------------------

create table scheduled_notification
(
  id                varchar(255) not null,
  notification_type varchar(255) not null,
  scheduled_date    timestamp(6) not null,
  target_id         varchar(255),
  completed         boolean,
  constraint pk_scheduled_notification primary key (id)
);

create index idx_scheduled_notification_type on scheduled_notification (notification_type);

--------------------------------------------------------
--  DDL for Table SITSSTATUS
--------------------------------------------------------

create table sitsstatus
(
  code            varchar(6) not null,
  shortname       varchar(15),
  fullname        varchar(50),
  lastupdateddate timestamp(6),
  constraint pk_sitsstatus primary key (code)
);

--------------------------------------------------------
--  DDL for Table SMALLGROUP
--------------------------------------------------------

create table smallgroup
(
  id                   varchar(255) not null,
  set_id               varchar(255),
  name                 varchar(255),
  studentsgroup_id     varchar(255),
  settings             text,
  linked_dept_group_id varchar(255),
  constraint pk_smallgroup primary key (id)
);

create index idx_smallgroup_linked_dsg on smallgroup (linked_dept_group_id);
create index idx_smallgroup_set on smallgroup (set_id);

--------------------------------------------------------
--  DDL for Table SMALLGROUPEVENT
--------------------------------------------------------

create table smallgroupevent
(
  id              varchar(255) not null,
  group_id        varchar(255),
  weekranges      varchar(255),
  day             smallint,
  starttime       varchar(255),
  endtime         varchar(255),
  title           varchar(255),
  location        varchar(255),
  tutorsgroup_id  varchar(255),
  relatedurl      varchar(255),
  relatedurltitle varchar(255),
  constraint pk_smallgroupevent primary key (id),
  constraint chk_smallgroupevent_dates check ((starttime is null and endtime is null) or (starttime is not null and endtime is not null))
);

create index idx_smallgroupevent_group on smallgroupevent (group_id);

--------------------------------------------------------
--  DDL for Table SMALLGROUPEVENTATTENDANCE
--------------------------------------------------------

create table smallgroupeventattendance
(
  id                     varchar(250)                           not null,
  occurrence_id          varchar(250)                           not null,
  universityid           varchar(250)                           not null,
  state                  varchar(20)  default 'attended'        not null,
  updateddate            timestamp(6) default current_timestamp not null,
  updatedby              varchar(255)                           not null,
  added_manually         boolean      default false,
  replaces_attendance_id varchar(255),
  constraint pk_smallgroupeventattendance primary key (id),
  constraint idx_smallgroupeventattendance_ck unique (occurrence_id, universityid)
);

create index idx_smallgroupeventattendance_occurrence on smallgroupeventattendance (occurrence_id);
create index idx_smallgroupeventattendance_universityid on smallgroupeventattendance (universityid);
create index idx_smallgroupeventattendance_replaces on smallgroupeventattendance (replaces_attendance_id);

--------------------------------------------------------
--  DDL for Table SMALLGROUPEVENTOCCURRENCE
--------------------------------------------------------

create table smallgroupeventoccurrence
(
  id       varchar(255) not null,
  week     int          not null,
  event_id varchar(255) not null,
  constraint pk_smallgroupeventoccurrence primary key (id),
  constraint idx_smallgroupeventoccurrence_ck unique (event_id, week)
);

--------------------------------------------------------
--  DDL for Table SMALLGROUPSET
--------------------------------------------------------

create table smallgroupset
(
  id                       varchar(255) not null,
  module_id                varchar(255) not null,
  academicyear             smallint     not null,
  name                     varchar(255),
  archived                 boolean      default false,
  deleted                  boolean      default false,
  group_format             varchar(255) not null,
  membersgroup_id          varchar(255),
  allocation_method        varchar(50)  default 'Manual',
  released_to_students     boolean      default false,
  released_to_tutors       boolean      default false,
  self_group_switching     boolean      default true,
  settings                 text,
  open_for_signups         boolean      default false,
  collect_attendance       boolean      default true,
  linked_dept_group_set_id varchar(255),
  default_weekranges       varchar(255),
  default_tutorsgroup_id   varchar(255),
  default_location         varchar(255),
  email_students           boolean      default true,
  email_tutors             boolean      default true,
  default_day              smallint,
  default_starttime        varchar(255) default '12:00',
  default_endtime          varchar(255) default '13:00',
  constraint pk_smallgroupset primary key (id)
);

create index idx_smallgroupset_deleted on smallgroupset (deleted);
create index idx_smallgroupset_module on smallgroupset (module_id);
create index idx_smallgroupset_dsgs on smallgroupset (linked_dept_group_set_id);

--------------------------------------------------------
--  DDL for Table STAGEMARKERS
--------------------------------------------------------

create table stagemarkers
(
  id          varchar(255) not null,
  stage       varchar(255) not null,
  markers     varchar(255) not null,
  workflow_id varchar(255) not null,
  constraint pk_stagemarkers primary key (id)
);

create index idx_stagemarkers_workflow on stagemarkers (workflow_id);

--------------------------------------------------------
--  DDL for Table STUDENTCOURSEDETAILS
--------------------------------------------------------

create table studentcoursedetails
(
  scjcode                varchar(20) not null,
  universityid           varchar(20) not null,
  sprcode                varchar(20) not null,
  routecode              varchar(20),
  coursecode             varchar(20),
  awardcode              varchar(20),
  sprstatuscode          varchar(12),
  levelcode              varchar(10),
  begindate              date,
  enddate                date,
  expectedenddate        date,
  courseyearlength       smallint,
  mostsignificant        boolean,
  lastupdateddate        timestamp(6),
  hib_version            numeric,
  department_id          varchar(255),
  latestyeardetails      varchar(250),
  missingfromimportsince timestamp(6),
  scjstatuscode          varchar(12),
  reasonfortransfercode  varchar(10),
  sprstartacademicyear   smallint,
  constraint pk_studentcoursedetails primary key (scjcode)
);

create index idx_studentcoursedetails_department on studentcoursedetails (department_id);
create index idx_studentcoursedetails_route on studentcoursedetails (routecode);
create index idx_studentcoursedetails_sprcode on studentcoursedetails (sprcode);
create index idx_studentcoursedetails_sprstatus on studentcoursedetails (sprstatuscode);
create index idx_studentcoursedetails_universityid on studentcoursedetails (universityid);

--------------------------------------------------------
--  DDL for Table STUDENTCOURSEDETAILSNOTE
--------------------------------------------------------

create table studentcoursedetailsnote
(
  code    varchar(255) not null,
  scjcode varchar(255),
  note    text,
  constraint pk_studentcoursedetailsnote primary key (code)
);

create index idx_studentcoursedetailsnote_scd on studentcoursedetailsnote (scjcode);

--------------------------------------------------------
--  DDL for Table STUDENTCOURSEYEARDETAILS
--------------------------------------------------------

create table studentcourseyeardetails
(
  id                       varchar(250) not null,
  scjcode                  varchar(20)  not null,
  scesequencenumber        smallint     not null,
  academicyear             smallint     not null,
  enrolmentstatuscode      varchar(10),
  modeofattendancecode     varchar(10),
  yearofstudy              smallint,
  lastupdateddate          timestamp(6),
  hib_version              numeric,
  moduleregistrationstatus varchar(10),
  missingfromimportsince   timestamp(6),
  enrolment_department_id  varchar(255),
  cas_used                 boolean,
  tier4visa                boolean,
  enrolledorcompleted      boolean default false,
  overcatting              text,
  agreedmark               numeric(4, 1),
  agreedmarkuploadeddate   timestamp(6),
  agreedmarkuploadedby     varchar(255),
  routecode                varchar(20),
  studylevel               varchar(10),
  blockoccurrence          varchar(10),
  constraint pk_studentcourseyeardetails primary key (id),
  constraint idx_studentcourseyeardetails_ck unique (scjcode, scesequencenumber)
);

create index idx_studentcourseyeardetails_department on studentcourseyeardetails (enrolment_department_id);
create index idx_studentcourseyeardetails_moa on studentcourseyeardetails (modeofattendancecode);
create index idx_studentcourseyeardetails_year on studentcourseyeardetails (yearofstudy);

--------------------------------------------------------
--  DDL for Table STUDENTRELATIONSHIP
--------------------------------------------------------

create table studentrelationship
(
  id                  varchar(255)                 not null,
  relationship_type   varchar(50)                  not null,
  agent               varchar(255),
  uploaded_date       timestamp(6),
  start_date          timestamp(6),
  end_date            timestamp(6),
  percentage          numeric(6, 2),
  scjcode             varchar(20)                  not null,
  agent_type          varchar(20) default 'member' not null,
  external_agent_name varchar(255),
  terminated          boolean     default false,
  replacedby          varchar(255),
  constraint pk_studentrelationship primary key (id)
);

create index idx_studentrelationship_type on studentrelationship (relationship_type);
create index idx_studentrelationship_agent on studentrelationship (agent);
create index idx_studentrelationship_agenttype on studentrelationship (agent_type);
create index idx_studentrelationship_scjcode on studentrelationship (scjcode);

--------------------------------------------------------
--  DDL for Table STUDENTRELATIONSHIPTYPE
--------------------------------------------------------

create table studentrelationshiptype
(
  id                    varchar(255)           not null,
  urlpart               varchar(255)           not null,
  description           varchar(50)            not null,
  agentrole             varchar(50)            not null,
  studentrole           varchar(50)            not null,
  defaultsource         varchar(20)            not null,
  defaultdisplay        boolean  default true,
  expected_ug           boolean  default false,
  expected_pgt          boolean  default false,
  expected_pgr          boolean  default false,
  sort_order            smallint default 2,
  defaultrdxtype        varchar(12),
  expected_foundation   boolean  default false not null,
  expected_presessional boolean  default false not null,
  constraint pk_studentrelationshiptype primary key (id),
  constraint idx_studentrelationshiptype_urlpart unique (urlpart)
);

--------------------------------------------------------
--  DDL for Table STUDYDETAILS
--------------------------------------------------------

create table studydetails
(
  universityid         varchar(100) not null,
  hib_version          numeric default 0,
  sprcode              varchar(100),
  sitscoursecode       varchar(100),
  route_id             varchar(255),
  study_department_id  varchar(255),
  yearofstudy          smallint,
  intendedaward        varchar(12),
  begindate            date,
  enddate              date,
  expectedenddate      date,
  fundingsource        varchar(6),
  courseyearlength     smallint,
  sprstatuscode        varchar(6),
  enrolmentstatuscode  varchar(6),
  modeofattendancecode varchar(10),
  scjcode              varchar(20),
  constraint pk_studydetails primary key (universityid)
);

create index idx_studydetails_sprcode on studydetails (sprcode);

--------------------------------------------------------
--  DDL for Table STUDYLEVEL
--------------------------------------------------------

create table studylevel
(
  code            varchar(20) not null,
  shortname       varchar(30),
  name            varchar(100),
  lastupdateddate timestamp(6),
  hib_version     numeric,
  constraint pk_studylevel primary key (code)
);

--------------------------------------------------------
--  DDL for Table SUBMISSION
--------------------------------------------------------

create table submission
(
  id                      varchar(255) not null,
  submitted               boolean      not null,
  submitted_date          timestamp(6),
  universityid            varchar(255),
  userid                  varchar(255) not null,
  assignment_id           varchar(255),
  state                   varchar(255),
  plagiarisminvestigation varchar(50) default 'NotInvestigated',
  constraint pk_submission primary key (id),
  constraint idx_submission_ck unique (assignment_id, universityid)
);

create index idx_submission_assignment on submission (assignment_id);
create index idx_submission_user on submission (userid);

--------------------------------------------------------
--  DDL for Table SUBMISSIONVALUE
--------------------------------------------------------

create table submissionvalue
(
  id                 varchar(100) not null,
  name               varchar(255) not null,
  submission_id      varchar(255),
  value_old          varchar(4000),
  feedback_id        varchar(255),
  marker_feedback_id varchar(255),
  value              text,
  constraint pk_submissionvalue primary key (id)
);

create index idx_submissionvalue_feedback on submissionvalue (feedback_id);
create index idx_submissionvalue_markerfeedback on submissionvalue (marker_feedback_id);
create index idx_submissionvalue_submission on submissionvalue (submission_id);

--------------------------------------------------------
--  DDL for Table SYLLABUSPLUSLOCATION
--------------------------------------------------------

create table syllabuspluslocation
(
  id              varchar(255) not null,
  upstream_name   varchar(255) not null,
  name            varchar(255) not null,
  map_location_id varchar(255) not null,
  constraint pk_syllabuspluslocation primary key (id)
);

create index idx_syllabuspluslocation_upstream on syllabuspluslocation (upstream_name);

--------------------------------------------------------
--  DDL for Table UPSTREAMASSESSMENTGROUP
--------------------------------------------------------

create table upstreamassessmentgroup
(
  id              varchar(100) not null,
  modulecode      varchar(100) not null,
  assessmentgroup varchar(100) not null,
  academicyear    smallint     not null,
  occurrence      varchar(100) not null,
  sequence        varchar(100),
  constraint pk_upstreamassessmentgroup primary key (id),
  constraint idx_upstreamassessmentgroup_ck unique (occurrence, modulecode, assessmentgroup, academicyear, sequence)
);

create index idx_upstreamassessmentgroup_academicyear on upstreamassessmentgroup (academicyear);

--------------------------------------------------------
--  DDL for Table UPSTREAMASSESSMENTGROUPMEMBER
--------------------------------------------------------

create table upstreamassessmentgroupmember
(
  id               varchar(255) not null,
  group_id         varchar(255) not null,
  universityid     varchar(250) not null,
  position         int,
  actualmark       int,
  actualgrade      varchar(255),
  agreedmark       int,
  agreedgrade      varchar(255),
  resitactualmark  int,
  resitactualgrade varchar(255),
  resitagreedmark  int,
  resitagreedgrade varchar(255),
  constraint pk_upstreamassessmentgroupmember primary key (id)
);

create index idx_upstreamassessmentgroupmember_group on upstreamassessmentgroupmember (group_id);

--------------------------------------------------------
--  DDL for Table UPSTREAMASSIGNMENT
--------------------------------------------------------

create table upstreamassignment
(
  id              varchar(100)  not null,
  modulecode      varchar(100)  not null,
  assessmentgroup varchar(100)  not null,
  sequence        varchar(100)  not null,
  name            varchar(4000) not null,
  assessmenttype  varchar(32),
  module_id       varchar(255),
  in_use          boolean default true,
  markscode       varchar(100),
  weighting       smallint,
  constraint pk_upstreamassignment primary key (id),
  constraint idx_upstreamassignment_ck unique (modulecode, sequence)
);

create index idx_upstreamassignment_module on upstreamassignment (module_id);
create index idx_upstreamassignment_inuse on upstreamassignment (in_use);

--------------------------------------------------------
--  DDL for Table UPSTREAMMEMBER
--------------------------------------------------------

create table upstreammember
(
  universityid varchar(100) not null,
  userid       varchar(100) not null,
  firstname    varchar(255),
  lastname     varchar(255),
  email        varchar(4000),
  constraint pk_upstreammember primary key (universityid)
);

--------------------------------------------------------
--  DDL for Table UPSTREAMMODULELIST
--------------------------------------------------------

create table upstreammodulelist
(
  code         varchar(255) not null,
  academicyear smallint     not null,
  routecode    varchar(20)  not null,
  yearofstudy  smallint     not null,
  constraint pk_upstreammodulelist primary key (code)
);

--------------------------------------------------------
--  DDL for Table UPSTREAMMODULELISTENTRY
--------------------------------------------------------

create table upstreammodulelistentry
(
  id          varchar(255) not null,
  listcode    varchar(255) not null,
  matchstring varchar(255) not null,
  constraint pk_upstreammodulelistentry primary key (id)
);

create index idx_upstreammodulelistentry_listcode on upstreammodulelistentry (listcode);

--------------------------------------------------------
--  DDL for Table UPSTREAMROUTERULE
--------------------------------------------------------

create table upstreamrouterule
(
  id           varchar(255) not null,
  academicyear smallint,
  routecode    varchar(20)  not null,
  levelcode    varchar(10),
  constraint pk_upstreamrouterule primary key (id)
);

--------------------------------------------------------
--  DDL for Table UPSTREAMROUTERULEENTRY
--------------------------------------------------------

create table upstreamrouteruleentry
(
  id          varchar(255) not null,
  rule_id     varchar(255) not null,
  listcode    varchar(255) not null,
  min_cats    numeric(5, 2),
  max_cats    numeric(5, 2),
  min_modules smallint,
  max_modules smallint,
  constraint pk_upstreamrouteruleentry primary key (id)
);

create index idx_upstreamrouteruleentry_rule on upstreamrouteruleentry (rule_id);

--------------------------------------------------------
--  DDL for Table USERGROUP
--------------------------------------------------------

create table usergroup
(
  id            varchar(255) not null,
  basewebgroup  varchar(255),
  universityids boolean default false,
  constraint pk_usergroup primary key (id)
);

create index idx_usergroup_basewebgroup on usergroup (basewebgroup);

--------------------------------------------------------
--  DDL for Table USERGROUPEXCLUDE
--------------------------------------------------------

create table usergroupexclude
(
  group_id varchar(255) not null,
  usercode varchar(255)
);

create index idx_usergroupexclude_group on usergroupexclude (group_id);
create index idx_usergroupexclude_uid on usergroupexclude (usercode, group_id);

--------------------------------------------------------
--  DDL for Table USERGROUPINCLUDE
--------------------------------------------------------

create table usergroupinclude
(
  group_id varchar(255) not null,
  usercode varchar(255)
);

create index idx_usergroupinclude_group on usergroupinclude (group_id);
create index idx_usergroupinclude_uid on usergroupinclude (usercode, group_id);

--------------------------------------------------------
--  DDL for Table USERGROUPSTATIC
--------------------------------------------------------

create table usergroupstatic
(
  group_id varchar(255) not null,
  usercode varchar(255)
);

create index idx_usergroupstatic_group on usergroupinclude (group_id);
create index idx_usergroupstatic_uid on usergroupinclude (usercode);

--------------------------------------------------------
--  DDL for Table USERSETTINGS
--------------------------------------------------------

create table usersettings
(
  id       varchar(100) not null,
  userid   varchar(100) not null,
  settings text,
  constraint pk_usersettings primary key (id),
  constraint idx_usersettings_fk unique (userid)
);

--------------------------------------------------------
--  Ref Constraints
--------------------------------------------------------

alter table assignment
  add constraint fk_assignment_cm2workflow foreign key (cm2_workflow_id) references markingworkflow (id);
alter table markingworkflow
  add constraint fk_markingworkflow_department foreign key (department_id) references department (id);
alter table moduleteachinginformation
  add constraint fk_moduleteachinginformation_department foreign key (department_id) references department (id);
alter table moduleteachinginformation
  add constraint fk_moduleteachinginformation_module foreign key (module_id) references module (id);
alter table outstandingstages
  add constraint fk_outstandingstages_feedback foreign key (feedback_id) references feedback (id);
alter table recipientnotificationinfo
  add constraint fk_recipientnotificationinfo_notification foreign key (notification_id) references notification (id);
alter table routeteachinginformation
  add constraint fk_routeteachinginformation_department foreign key (department_id) references department (id);
alter table routeteachinginformation
  add constraint fk_routeteachinginformation_route foreign key (route_id) references route (id);
alter table smallgroupeventoccurrence
  add constraint fk_smallgroupeventoccurrence_event foreign key (event_id) references smallgroupevent (id);
alter table stagemarkers
  add constraint fk_stagemarkers_workflow foreign key (workflow_id) references markingworkflow (id);
alter table studentcoursedetailsnote
  add constraint fk_studentcoursedetailsnote_scd foreign key (scjcode) references studentcoursedetails (scjcode);
