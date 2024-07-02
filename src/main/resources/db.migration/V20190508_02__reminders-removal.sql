delete from qrtz_simple_triggers where trigger_name in (select trigger_name from qrtz_triggers where job_group like '%earingHoldingReminder');

delete from qrtz_triggers where job_group like '%earingHoldingReminder';

delete from qrtz_job_details where job_group like '%earingHoldingReminder';

delete from qrtz_simple_triggers where trigger_name in (select trigger_name from qrtz_triggers where job_group like '%dwpResponseLateReminder');

delete from qrtz_triggers where job_group like '%dwpResponseLateReminder';

delete from qrtz_job_details where job_group like '%dwpResponseLateReminder';

commit;