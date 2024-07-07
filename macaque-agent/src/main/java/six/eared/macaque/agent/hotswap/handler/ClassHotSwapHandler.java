package six.eared.macaque.agent.hotswap.handler;

import six.eared.macaque.agent.annotation.HotSwapFileType;
import six.eared.macaque.agent.asm2.AsmUtil;
import six.eared.macaque.agent.asm2.classes.ClazzDefinition;
import six.eared.macaque.agent.asm2.classes.ClazzDefinitionVisitorFactory;
import six.eared.macaque.agent.asm2.classes.CorrelationClazzDefinition;
import six.eared.macaque.agent.enhance.CompatibilityModeByteCodeEnhancer;
import six.eared.macaque.agent.hotswap.ClassHotSwapper;
import six.eared.macaque.agent.vcs.VersionChainTool;
import six.eared.macaque.agent.vcs.VersionView;
import six.eared.macaque.common.ExtPropertyName;
import six.eared.macaque.common.type.FileType;
import six.eared.macaque.common.util.CollectionUtil;
import six.eared.macaque.mbean.rmi.HotSwapRmiData;
import six.eared.macaque.mbean.rmi.RmiResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@HotSwapFileType(fileType = FileType.Class)
public class ClassHotSwapHandler extends FileHookHandler {

    @Override
    public RmiResult doHandler(HotSwapRmiData rmiData) throws Exception {
        return handler(rmiData.getFileData(), rmiData.getExtProperties());
    }

    @SuppressWarnings("unchecked")
    public RmiResult handler(byte[] bytes, Map<String, String> extProperties) throws Exception {
        List<ClazzDefinition> definitions = AsmUtil.readMultiClass(bytes, ClazzDefinitionVisitorFactory.DEFAULT);
        boolean compatibilityMode = Boolean.TRUE.toString()
                .equalsIgnoreCase(extProperties.get(ExtPropertyName.COMPATIBILITY_MODE));

        if (compatibilityMode) {
            Map<String, byte[]> classs = CompatibilityModeByteCodeEnhancer.enhance(definitions);
            return RmiResult.success().data(ClassHotSwapper.redefines(classs));
        } else {
            return RmiResult.success().data(ClassHotSwapper
                    .redefines(flatClassDefinition(definitions)));
        }
    }

    private Map<String, byte[]> flatClassDefinition(List<ClazzDefinition> definitions) {
        VersionView versionView = VersionChainTool.getActiveVersionView();

        Map<String, byte[]> flatMap = new HashMap<>();
        for (ClazzDefinition definition : definitions) {
            flatMap.put(definition.getClassName(), definition.getByteArray());
            versionView.addDefinition(definition);
        }
        return flatMap;
    }
}
