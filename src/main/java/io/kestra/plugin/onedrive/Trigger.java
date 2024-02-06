package io.kestra.plugin.onedrive;

import com.microsoft.graph.requests.GraphServiceClient;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.conditions.ConditionContext;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.executions.ExecutionTrigger;
import io.kestra.core.models.flows.State;
import io.kestra.core.models.triggers.AbstractTrigger;
import io.kestra.core.models.triggers.PollingTriggerInterface;
import io.kestra.core.models.triggers.TriggerContext;
import io.kestra.core.models.triggers.TriggerOutput;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import okhttp3.Request;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static io.kestra.core.utils.Rethrow.throwFunction;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "",
    description = ""
)
@Plugin(
    examples = {
        @Example(
            title = "",
            full = true,
            code = {
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
            }
        )
    }
)
public class Trigger extends AbstractTrigger implements PollingTriggerInterface, TriggerOutput<Downloads.Output> {

    @Schema(
        title = ""
    )
    @PluginProperty(dynamic = true)
    String clientId;

    @Schema(
        title = ""
    )
    @PluginProperty(dynamic = true)
    String clientSecret;

    @Schema(
        title = ""
    )
    @PluginProperty(dynamic = true)
    String tenantId;

    @Schema(
        title = ""
    )
    @PluginProperty(dynamic = true)
    String pemCertificate;

    @Schema(
        title = ""
    )
    @PluginProperty(dynamic = true)
    String authorizationCode;

    @Schema(
        title = ""
    )
    @PluginProperty(dynamic = true)
    String redirectUrl;

    @Schema(
        title = ""
    )
    @Builder.Default
    java.util.List<String> scopes = java.util.List.of("User.Read");

    @Schema(
        title = "The directory to list"
    )
    @PluginProperty(dynamic = true)
    @NotNull
    private String from;

    @Schema(
        title = "Interval amount",
        example = "3"
    )
    @PluginProperty(dynamic = true)
    @NotNull
    private long durationAmount;

    @Schema(
        title = "Interval unit type",
        example = "HOURS"
    )
    @PluginProperty(dynamic = true)
    @NotNull
    private ChronoUnit durationUnit;

    @Override
    public Optional<Execution> evaluate(ConditionContext conditionContext, TriggerContext context) throws Exception {
        RunContext runContext = conditionContext.getRunContext();

        List task = List.builder()
            .id(this.id)
            .type(List.class.getName())
            .clientId(clientId)
            .clientSecret(clientSecret)
            .scopes(scopes)
            .authorizationCode(authorizationCode)
            .pemCertificate(pemCertificate)
            .tenantId(tenantId)
            .redirectUrl(redirectUrl)
            .from(from)
            .build();

        List.Output run = task.run(runContext);

        if (run.getItems().isEmpty()) {
            return Optional.empty();
        }

        GraphServiceClient<Request> client = task.client(runContext);

        java.util.List<URI> list = run.getItems()
            .stream()
            .map(throwFunction(item -> {
                URI uri = runContext.storage().putFile(
                    Download.download(runContext, client, item)
                                                      );

                return URI.create(item.webUrl);
            }))
            .toList();

        ExecutionTrigger executionTrigger = ExecutionTrigger.of(
            this,
            Downloads.Output.builder().uris(list).build()
                                                               );

        Execution execution = Execution.builder()
            .id(runContext.getTriggerExecutionId())
            .namespace(context.getNamespace())
            .flowId(context.getFlowId())
            .flowRevision(context.getFlowRevision())
            .state(new State())
            .trigger(executionTrigger)
            .build();

        return Optional.of(execution);
    }

    @Override
    public Duration getInterval() {
        return Duration.of(durationAmount, durationUnit);
    }

}
