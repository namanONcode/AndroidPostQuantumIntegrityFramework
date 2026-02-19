-- =============================================================================
-- AnchorPQ Database Initialization Script
-- Creates tables and seeds initial data for integrity verification
-- =============================================================================

-- Create integrity_records table if not exists
CREATE TABLE IF NOT EXISTS integrity_records (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    variant VARCHAR(50) NOT NULL,
    merkle_root VARCHAR(64) NOT NULL,
    signer_fingerprint VARCHAR(64),
    description TEXT,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(version, variant)
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_integrity_records_version_variant
ON integrity_records(version, variant);

-- Create verification_logs table for audit
CREATE TABLE IF NOT EXISTS verification_logs (
    id BIGSERIAL PRIMARY KEY,
    client_ip VARCHAR(45),
    version VARCHAR(50),
    variant VARCHAR(50),
    submitted_root VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    message TEXT,
    verified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create index for verification logs
CREATE INDEX IF NOT EXISTS idx_verification_logs_verified_at
ON verification_logs(verified_at DESC);

-- =============================================================================
-- Seed Data for Demo Application
-- These records are used to validate the demo app's integrity
-- =============================================================================

-- Insert demo app canonical Merkle root
-- This will be updated by the CI/CD pipeline with the actual root
INSERT INTO integrity_records (version, variant, merkle_root, description)
VALUES
    ('1.0.0', 'debug', 'DEMO_MERKLE_ROOT_PLACEHOLDER', 'Demo app debug build - placeholder'),
    ('1.0.0', 'release', 'DEMO_MERKLE_ROOT_PLACEHOLDER', 'Demo app release build - placeholder')
ON CONFLICT (version, variant) DO UPDATE SET
    merkle_root = EXCLUDED.merkle_root,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

-- Grant permissions (for production, adjust as needed)
GRANT SELECT, INSERT, UPDATE ON integrity_records TO anchorpq;
GRANT SELECT, INSERT ON verification_logs TO anchorpq;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO anchorpq;

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'AnchorPQ database initialized successfully';
END $$;

