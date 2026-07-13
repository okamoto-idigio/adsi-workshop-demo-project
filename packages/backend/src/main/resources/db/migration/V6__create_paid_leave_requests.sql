CREATE TABLE paid_leave_requests (
    id UUID PRIMARY KEY,
    requester_id UUID NOT NULL REFERENCES employees(id),
    approver_id UUID REFERENCES employees(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    leave_type VARCHAR(20) NOT NULL CHECK (leave_type IN ('FULL_DAY', 'AM_HALF', 'PM_HALF')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    reject_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_date_range CHECK (start_date <= end_date),
    CONSTRAINT chk_half_day_single CHECK (
        leave_type = 'FULL_DAY' OR start_date = end_date
    )
);

CREATE INDEX idx_paid_leave_requests_requester ON paid_leave_requests(requester_id);
CREATE INDEX idx_paid_leave_requests_status ON paid_leave_requests(status);
CREATE INDEX idx_paid_leave_requests_dates ON paid_leave_requests(start_date, end_date);
