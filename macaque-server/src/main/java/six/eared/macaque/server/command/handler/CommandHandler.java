package six.eared.macaque.server.command.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import six.eared.macaque.server.config.LoggerName;
import six.eared.macaque.server.enumeration.CommandType;

public abstract class CommandHandler {
    protected static final Logger log = LoggerFactory.getLogger(LoggerName.CONSOLE);

    private final CommandType commandType;

    protected CommandType getCommandType() {
        return commandType;
    }

    CommandHandler(CommandType commandType) {
        this.commandType = commandType;
    }

    /**
     * Handle the given command.
     *
     * @param command The command to handle.
     */
    abstract void handle(String command);





}
