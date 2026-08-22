-- =========================================================
-- V3__add_payment_failure_reason.sql
-- Phase 11: Payment — records why a charge attempt was declined,
-- so a FAILED payment row is a useful audit trail, not just a status flag.
-- =========================================================

USE ecommercedb;

ALTER TABLE payments
    ADD COLUMN failure_reason VARCHAR(255) NULL AFTER status;
