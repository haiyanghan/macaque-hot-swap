package six.eared.macaque.server.command.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import six.eared.macaque.common.util.ClassUtil;
import six.eared.macaque.server.config.LoggerName;
import six.eared.macaque.server.enumeration.CommandType;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandHandlerContext {
    private static final Logger log = LoggerFactory.getLogger(LoggerName.CONSOLE);


    private static Map<String, CommandHandler> handlerMap = null;


    /**
     * 处理命令
     *
     * @param command
     */
    public static void handle(String command) throws ClassNotFoundException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (handlerMap == null) {
            init();
        }
        CommandHandler handler = handlerMap.get(command);
        if (handler == null) {
            return;
        }
        handler.handle(command);
    }

    /**
     * 初始化
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private static void init() throws ClassNotFoundException, IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        handlerMap = new HashMap<>();
        List<Class> classes = ClassUtil.scanClass("six.eared.macaque.server.command.handler");
        for (Class clazz : classes) {
            if (clazz.getSuperclass()== CommandHandler.class) {
                CommandHandler handler = (CommandHandler) clazz.getConstructor().newInstance();
                Set<String> commandTypes = handler.getCommandType().names;
                for (String commandType : commandTypes) {
                    handlerMap.put(commandType, handler);
                }
            }
        }
    }


}
