package example.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.fintlabs.ErrorHandlingAutoConfiguration;
import no.fintlabs.ExceptionMappingRegistry;
import no.fintlabs.ProblemDetailFactory;
import no.fintlabs.RequestBodyLogging;
import no.fintlabs.ResponseFilter;
import no.fintlabs.controller.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlingAutoConfigurationTest {

    @Test
    void registersErrorHandlingOutsideTheLibraryPackageWithoutATracer() {
        new WebApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withConfiguration(AutoConfigurations.of(ErrorHandlingAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ExceptionMappingRegistry.class);
                    assertThat(context).hasSingleBean(ProblemDetailFactory.class);
                    assertThat(context).hasSingleBean(RequestBodyLogging.class);
                    assertThat(context).hasSingleBean(ResponseFilter.class);
                    assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
                });
    }
}
