WITH updated AS (
    SELECT scope_id,
           CASE usertype
               WHEN 'A' THEN 'EmeritusMember'
               WHEN 'N' THEN 'StaffMember'
               WHEN 'O' THEN 'OtherMember'
               WHEN 'P' THEN 'ApplicantMember'
               WHEN 'S' THEN 'StudentMember'
               END AS scope_type
    FROM grantedpermission
             JOIN member ON grantedpermission.scope_id = member.universityid
    WHERE scope_type = 'Member'
)
UPDATE grantedpermission
SET scope_type = updated.scope_type
FROM updated
WHERE grantedpermission.scope_type = 'Member' AND grantedpermission.scope_id = updated.scope_id;

WITH updated AS (
    SELECT scope_id,
           CASE usertype
               WHEN 'A' THEN 'EmeritusMember'
               WHEN 'N' THEN 'StaffMember'
               WHEN 'O' THEN 'OtherMember'
               WHEN 'P' THEN 'ApplicantMember'
               WHEN 'S' THEN 'StudentMember'
               END AS scope_type
    FROM grantedrole
             JOIN member ON grantedrole.scope_id = member.universityid
    WHERE scope_type = 'Member'
)
UPDATE grantedrole
SET scope_type = updated.scope_type
FROM updated
WHERE grantedrole.scope_type = 'Member' AND grantedrole.scope_id = updated.scope_id;



