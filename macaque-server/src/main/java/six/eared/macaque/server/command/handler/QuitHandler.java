package six.eared.macaque.server.command.handler;

import six.eared.macaque.server.enumeration.CommandType;

public class QuitHandler extends CommandHandler{


    QuitHandler() {
        super(CommandType.QUIT);
    }

    @Override
    public void handle(String command) {
        System.exit(-1);
    }
}
