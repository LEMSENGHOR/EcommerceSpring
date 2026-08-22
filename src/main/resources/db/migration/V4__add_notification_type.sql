-- =========================================================
-- V4__add_notification_type.sql
-- Phase 14: Notification — categorizes notifications (ORDER, PAYMENT,
-- PROMOTION, ACCOUNT, GENERAL) so the frontend can filter/icon them and
-- admins can report on send volume by category.
-- =========================================================

USE ecommercedb;

ALTER TABLE notifications
    ADD COLUMN type VARCHAR(30) NOT NULL DEFAULT 'GENERAL' AFTER message;

CREATE INDEX idx_notifications_user_id_is_read ON notifications(user_id, is_read);
