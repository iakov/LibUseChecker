library SMTP;

types {
    Properties(java.util.Properties);
    Object(Object);
    Transport(javax.mail.Transport);
    Message(javax.mail.Message);
}

fun Properties.put(key: Object, value: Object): Object {
    pre("SMTP_PORT_INT", ("value should be instanceof int rather than %s when key is mail.smtp.port", value.getClass().getName()), !(key instanceof String) || (key instanceof String && (!((String)key).equals("mail.smtp.port") || ((String)key).equals("mail.smtp.port") && value instanceof Integer)));
}

fun Transport.send(msg: Message) {
    pre("MESSAGE_FROM_NOT_SPECIFIED", "Use setFrom() method for message before sending", msg.getFrom() != null && msg.getFrom().length > 0);
}
