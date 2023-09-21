//package six.eared.macaque.server.command.handler;
//
//import six.eared.macaque.common.type.FileType;
//import six.eared.macaque.common.util.FileUtil;
//import six.eared.macaque.core.client.MacaqueClient;
//import six.eared.macaque.mbean.rmi.HotSwapRmiData;
//import six.eared.macaque.mbean.rmi.RmiResult;
//import six.eared.macaque.server.enumeration.CommandType;
//
//public class HotSwapHandler extends CommandHandler {
//
//    private MacaqueClient client;
//    HotSwapHandler() {
//        super(CommandType.HOT_SWAP);
//    }
//
//    public void setClient(MacaqueClient client) {
//        this.client = client;
//    }
//
//    @Override
//    void handle(String command) {
//        String classPath = command;
//        try {
//            RmiResult result = client.hotswap(this.pid, new HotSwapRmiData(FileType.Class.getType(), FileUtil.readBytes(classPath)));
//            log.info("exec result: [{}]", result.getData());
//        } catch (Exception e) {
//            log.error("hotswap error", e);
//        }
//
//    }
//}
