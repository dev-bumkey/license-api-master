package run.acloud.api.resource.task;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import run.acloud.api.k8sextended.util.Yaml;

import java.util.Map;

class K8sPatchSpecFactoryTest {
    @Test
    void mergeYamlToString() {
        K8sPatchSpecFactory factory = new K8sPatchSpecFactory();
        try {
            String current = "\n" +
                "##한글 주석은 어떻게 되나요?\n" +
                "en: \"english\"\n" +
                "doo: {}\n" +
                "foo:\n" +
                "  foo: spam\n" +
                "  kr: \"한글값\"\n" +
                "  bar: spam\n";
            String overwrite = "\n" +
                "## remarks in english\n" +
                "en: \"english아님\"\n" +
                "doo:\n" +
                "  abc: def\n" +
                "foo:\n" +
                "  bar: spam2\n" +
                "  kr: value of korean language\n" +
                "  spam: bar\n";
            System.out.println("----------------------------------------------");
            System.out.println(factory.mergeYamlToString(current, overwrite));
            System.out.println("----------------------------------------------");
        }
        catch (Exception ex) {
            ex.printStackTrace();
            Assertions.fail("mergeYaml Test Failed");
        }
    }
}