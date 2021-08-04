delete from EMAIL_SENDING_ATTACHMENT
where MESSAGE_ID in ({placeHolders});

delete from EMAIL_SENDING_MESSAGE
where ID in ({placeHolders});