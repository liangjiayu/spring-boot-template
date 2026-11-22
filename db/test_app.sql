CREATE TABLE public.test_app (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code varchar(32) NOT NULL UNIQUE CHECK (code <> '' AND code !~ '[[:space:]]'),
    name varchar(100) NOT NULL CHECK (name ~ '[^[:space:]]'),
    description varchar(500),
    status varchar(16) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'active', 'inactive')),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
