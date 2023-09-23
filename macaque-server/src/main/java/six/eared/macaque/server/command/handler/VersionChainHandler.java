package six.eared.macaque.server.command.handler;

import six.eared.macaque.server.command.model.constants.VersionChainConstants;
import six.eared.macaque.server.enumeration.CommandType;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static six.eared.macaque.server.enumeration.CommandType.VERSION_CHAIN;

public class VersionChainHandler extends CommandHandler{



    VersionChainHandler() {
        super(VERSION_CHAIN);
    }

    @Override
    public void handle(String command) {
        String tmpDirStr = System.getProperty("java.io.tmpdir");
        if (tmpDirStr == null || tmpDirStr.trim().length() == 0) {
            log.error("Can't find temp directory");
            return;
        }
        //todo 细化文件夹n;
        File tmpDir = new File(tmpDirStr);
        if (!tmpDir.exists()) {
            log.error("Can't find temp directory");
            return;
        }

        String[] fileNames = tmpDir.list();
        if (fileNames == null) {
            log.info("No class file version.\n");
            return;
        }
        //VERSION_CHAIN-version+uid-name.class
        Map<String, String> version2ClassNameMap = Arrays.stream(fileNames)
                .filter(fileName -> fileName.endsWith(".class") && fileName.startsWith(VERSION_CHAIN.name()) && fileName.split(VersionChainConstants.FILE_NAME_SEPARATOR).length == 3
                )
                .collect(Collectors.toMap(fileName -> fileName.split(VersionChainConstants.FILE_NAME_SEPARATOR)[1]
                        , fileName -> fileName.split(VersionChainConstants.FILE_NAME_SEPARATOR)[1]));
        StringBuilder result = new StringBuilder();
        List<String> versions = version2ClassNameMap.keySet().stream().sorted(String::compareTo).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        for (String version : versions) {
            result.append(version).append("\t").append(version2ClassNameMap.get(version)).append("\n");
        }
        log.info(result.toString());
    }
}
