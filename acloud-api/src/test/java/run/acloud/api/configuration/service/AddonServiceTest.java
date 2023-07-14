package run.acloud.api.configuration.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AddonServiceTest {

    @Test
    void isValidYaml() {
        AddonService addonService = new AddonService();
        String current = "\n" +
            "##한글 주석은 어떻게 되나요?\n" +
            "en: \"english\"\n" +
            "foo:\n" +
            "  foo: spam\n" +
            "  kr: \"한글값\"\n" +
            "  bar: spam\n";
        String overwrite = "\n" +
            "## remarks in english\n" +
            "en: \"english아님\"\n" +
            "foo:\n" +
            "  bar: spam2\n" +
            "  kr: value of korean language\n" +
            "  spam: bar\n";

        Assertions.assertTrue(addonService.isValidYaml(current));
        Assertions.assertTrue(addonService.isValidYaml(overwrite));
    }
}