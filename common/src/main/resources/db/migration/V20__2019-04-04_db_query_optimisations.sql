-- This might not make much sense but we regularly query on just where listeners_processed = false
-- NotificationDao.unprocessedNotifications
create index idx_notification_processed on notification (listeners_processed);

-- NotificationDao.findActionRequiredNotificationsByEntityAndType
create index idx_entityreference_entity on entityreference (entity_type, entity_id);