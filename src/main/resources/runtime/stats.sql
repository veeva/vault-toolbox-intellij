DROP TABLE IF EXISTS stats;

CREATE TABLE stats AS
SELECT
    substr(timestamp, 1, 10) as log_date,
    vault_id,
    user_id,
    class_name,
    action_trigger_name,
    log_level,
    message AS sample_message,
    COUNT(DISTINCT execution_id) AS unique_executions,
    COUNT(*) AS total_log_lines

FROM runtime

GROUP BY
    log_date,
    vault_id,
    user_id,
    class_name,
    action_trigger_name,
    log_level

ORDER BY
    log_date DESC,
    total_log_lines DESC,
    class_name ASC;