package cc.aabss.eventutils;

import org.apache.logging.log4j.message.*;

import org.jetbrains.annotations.NotNull;


public class PrefixMessageFactory extends AbstractMessageFactory {
    @NotNull private static final String PREFIX = "[EventUtils] ";

    @Override
    public Message newMessage(final CharSequence message) {
        return new SimpleMessage(PREFIX + message);
    }

    @Override
    public Message newMessage(final Object message) {
        return new ObjectMessage(PREFIX + message);
    }

    @Override
    public Message newMessage(final String message, final Object... params) {
        return new ParameterizedMessage(PREFIX + message, params);
    }
}