package com.aicodingmentor.execution.service;

import com.aicodingmentor.execution.model.SupportedLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DriverHarnessInjector {

    private static final Logger log = LoggerFactory.getLogger(DriverHarnessInjector.class);

    private final DriverHarnessRegistry driverHarnessRegistry;

    public DriverHarnessInjector(DriverHarnessRegistry driverHarnessRegistry) {
        this.driverHarnessRegistry = driverHarnessRegistry;
    }

    /**
     * Injects the learner's function code into the problem's driver harness.
     * If the user already wrote a complete standalone program with entrypoint, it is left intact.
     *
     * @param userCode the learner's code
     * @param language the target programming language
     * @param problemSlug the slug of the problem
     * @return the complete, executable source code
     */
    public String wrapUserCode(String userCode, SupportedLanguage language, String problemSlug) {
        if (userCode == null || userCode.trim().isEmpty()) {
            return userCode;
        }

        SupportedLanguage lang = language != null ? language : SupportedLanguage.JAVA;

        // Check if user already provided complete standalone runnable program
        if (isAlreadyStandalone(userCode, lang)) {
            log.debug("User code detected as standalone program for language [{}]. Skipping driver harness injection.", lang);
            return userCode;
        }

        DriverHarnessRegistry.ProblemTemplate template = driverHarnessRegistry.getTemplate(problemSlug, lang);
        if (template == null || template.driverHarness() == null || template.driverHarness().isBlank()) {
            return userCode;
        }

        String sanitizedUserCode = userCode;
        if (lang == SupportedLanguage.JAVA) {
            sanitizedUserCode = userCode.replaceAll("public\\s+class\\s+", "class ");
        }

        log.debug("Injecting user code into driver harness for problem [{}] and language [{}]", problemSlug, lang);
        return template.driverHarness().replace("{{USER_CODE}}", sanitizedUserCode);
    }

    private boolean isAlreadyStandalone(String code, SupportedLanguage language) {
        String trimmed = code.trim();
        return switch (language) {
            case JAVA -> trimmed.contains("static void main") || trimmed.contains("public static void main");
            case PYTHON -> trimmed.contains("__name__ == '__main__'") || trimmed.contains("__name__ == \"__main__\"") || trimmed.contains("sys.stdin");
            case CPP -> trimmed.contains("int main(") || trimmed.contains("int main (") || trimmed.contains("void main(");
            case JAVASCRIPT -> trimmed.contains("readFileSync") || trimmed.contains("readline");
        };
    }
}
