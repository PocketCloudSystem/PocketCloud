package de.pocketcloud.cloud.server.config.overlay;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Representer;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlNodeOverlay {

    private static final ThreadLocal<Yaml> YAML = ThreadLocal.withInitial(() -> {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setProcessComments(true);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setProcessComments(true);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndent(2);
        dumperOptions.setIndicatorIndent(2);
        dumperOptions.setIndentWithIndicator(true);

        return new Yaml(new SafeConstructor(loaderOptions), new Representer(dumperOptions), dumperOptions, loaderOptions);
    });

    private YamlNodeOverlay() {}

    public static String overlay(String remoteRaw, String oldLocalRaw) {
        Yaml yaml = YAML.get();

        Node remoteNode = yaml.compose(new StringReader(remoteRaw));
        Node oldNode = yaml.compose(new StringReader(oldLocalRaw));
        if (remoteNode == null) return remoteRaw;
        if (oldNode == null) return remoteRaw;

        mergeInto(remoteNode, oldNode);

        StringWriter writer = new StringWriter();
        yaml.serialize(remoteNode, writer);

        return writer.toString().lines()
                .map(String::stripTrailing)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    private static void mergeInto(Node remote, Node old) {
        if (!(remote instanceof MappingNode remoteMap) || !(old instanceof MappingNode oldMap)) return;

        Map<String, Node> oldChildren = new LinkedHashMap<>();
        for (NodeTuple t : oldMap.getValue()) {
            if (t.getKeyNode() instanceof ScalarNode k) {
                oldChildren.put(k.getValue(), t.getValueNode());
            }
        }

        List<NodeTuple> newTuples = new ArrayList<>();
        for (NodeTuple t : remoteMap.getValue()) {
            Node keyNode = t.getKeyNode();
            Node valueNode = t.getValueNode();

            if (keyNode instanceof ScalarNode k) {
                Node oldValue = oldChildren.get(k.getValue());
                if (oldValue != null) {
                    if (valueNode instanceof MappingNode && oldValue instanceof MappingNode) {
                        mergeInto(valueNode, oldValue);
                    } else if (valueNode instanceof ScalarNode remoteScalar && oldValue instanceof ScalarNode oldScalar) {
                        if (!remoteScalar.getValue().equals(oldScalar.getValue())) {
                            ScalarNode replacement = new ScalarNode(
                                    oldScalar.getTag(), oldScalar.getValue(),
                                    remoteScalar.getStartMark(), remoteScalar.getEndMark(),
                                    oldScalar.getScalarStyle()
                            );
                            replacement.setBlockComments(remoteScalar.getBlockComments());
                            replacement.setInLineComments(remoteScalar.getInLineComments());
                            replacement.setEndComments(remoteScalar.getEndComments());
                            valueNode = replacement;
                        }
                    } else if (valueNode instanceof SequenceNode remoteSeq && oldValue instanceof SequenceNode oldSeq) {
                        if (!sameScalarList(remoteSeq, oldSeq)) {
                            SequenceNode replacement = new SequenceNode(
                                    remoteSeq.getTag(), oldSeq.getValue(), remoteSeq.getFlowStyle()
                            );
                            replacement.setBlockComments(remoteSeq.getBlockComments());
                            replacement.setInLineComments(remoteSeq.getInLineComments());
                            replacement.setEndComments(remoteSeq.getEndComments());
                            valueNode = replacement;
                        }
                    }
                }
            }
            newTuples.add(new NodeTuple(keyNode, valueNode));
        }
        remoteMap.setValue(newTuples);
    }

    private static boolean sameScalarList(SequenceNode a, SequenceNode b) {
        if (a.getValue().size() != b.getValue().size()) return false;
        for (int i = 0; i < a.getValue().size(); i++) {
            if (!(a.getValue().get(i) instanceof ScalarNode as) || !(b.getValue().get(i) instanceof ScalarNode bs)) {
                return false;
            }
            if (!as.getValue().equals(bs.getValue())) return false;
        }
        return true;
    }
}