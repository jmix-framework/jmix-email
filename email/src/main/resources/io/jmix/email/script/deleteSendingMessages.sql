delete from EMAIL_SENDING_ATTACHMENT
where MESSAGE_ID in (
    select ID
    from EMAIL_SENDING_MESSAGE
    where IMPORTANT = {important} and CREATE_TS < (now() - interval '{days} days')
);

delete from EMAIL_SENDING_MESSAGE
where IMPORTANT = {important} and CREATE_TS < (now() - interval '{days} days');