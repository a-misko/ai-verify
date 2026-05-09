package com.github.aiverifier.launcher;

import com.github.aiverifier.core.service.VerificationPipeline;
import com.github.aiverifier.impl.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "ai-verifier",
        description = "AI-assisted runtime backend verification",
        mixinStandardHelpOptions = true,
        version = "ai-verifier 0.1.0"
)
public class VerifyCommand implements Callable<Integer> {

    @Option(names = {"--config", "-c"}, description = "Path to ai-verifier.yml config", required = true)
    private Path configPath;

    @Option(names = {"--scenario", "-s"}, description = "Path to scenario.yml", required = true)
    private Path scenarioPath;

    @Override
    public Integer call() {
        VerificationPipeline pipeline = createPipeline();
        return pipeline.execute(configPath, scenarioPath);
    }

    private VerificationPipeline createPipeline() {
        return new DefaultVerificationPipeline(
                new YamlConfigLoader(),
                new YamlScenarioLoader(),
                new HttpEnvironmentChecker(),
                new ShellGitDiffCollector(),
                new DefaultProjectInventoryCollector(),
                new DefaultAuthFlowResolver(),
                new DefaultPromptBuilder(),
                new DefaultAiProviderFactory(),
                new DefaultFeatureExtractor(),
                new SecurityFeatureValidator(),
                new DefaultKarateRunner(),
                new DefaultReportWriter()
        );
    }
}
