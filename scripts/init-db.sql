-- =============================================================================
-- AnchorPQ Database Initialization Script
-- =============================================================================
-- This script is executed when the PostgreSQL container starts for the first time.
-- It creates the necessary tables and seeds example data.

-- Enable UUID extension (if needed for future use)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================================================
-- Table: integrity_records
-- =============================================================================
-- Note: Hibernate will create this table automatically based on the entity.
-- This script provides explicit DDL for reference and can be used for manual setup.

-- The table will be created by Hibernate ORM based on the IntegrityRecord entity.
-- This comment block shows the expected schema:

/*
CREATE TABLE IF NOT EXISTS integrity_records (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    variant VARCHAR(30) NOT NULL,
    merkle_root VARCHAR(64) NOT NULL,
    signer_fingerprint VARCHAR(64) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uk_version_variant UNIQUE (version, variant)
);

CREATE INDEX IF NOT EXISTS idx_version_variant ON integrity_records(version, variant);
CREATE INDEX IF NOT EXISTS idx_created_at ON integrity_records(created_at);
*/

-- =============================================================================
-- Seed Data: Example Integrity Records
-- =============================================================================
-- These are example records for testing. In production, records should be
-- inserted via the API when new app versions are built.

-- Note: The actual INSERT will be performed by the data seeding endpoint
-- or through migrations. This is for documentation purposes.

/*
INSERT INTO integrity_records (version, variant, merkle_root, signer_fingerprint, description, active, created_at, updated_at)
VALUES
    ('1.0.0', 'release',
     'a1b2c3d4e5f6789012345678901234567890123456789012345678901234abcd',
     'fedcba0987654321fedcba0987654321fedcba0987654321fedcba09876543fe',
     'Initial release version',
     TRUE,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP),

    ('1.0.0', 'debug',
     'b2c3d4e5f67890123456789012345678901234567890123456789012345bcdee',
     'fedcba0987654321fedcba0987654321fedcba0987654321fedcba09876543fe',
     'Initial debug build',
     TRUE,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP),

    ('1.0.1', 'release',
     'c3d4e5f678901234567890123456789012345678901234567890123456cdefab',
     'fedcba0987654321fedcba0987654321fedcba0987654321fedcba09876543fe',
     'Bug fix release',
     TRUE,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP)
ON CONFLICT (version, variant) DO NOTHING;
*/

-- =============================================================================
-- Verification
-- =============================================================================
-- Log successful initialization
DO $$
BEGIN
    RAISE NOTICE 'AnchorPQ database initialized successfully';
END $$;

