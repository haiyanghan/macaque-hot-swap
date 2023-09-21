package six.eared.macaque.server.enumeration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum CommandType {

    QUIT("quit,stop"),
    VERSION_CHAIN("VersionChain,version_chain"),
    HOT_SWAP("hot_swap,HotSwap"),
    ;

    public final Set<String> names;

    CommandType(String names) {
        this.names = new HashSet<>(Arrays.asList(names.split(",")));
    }
}
