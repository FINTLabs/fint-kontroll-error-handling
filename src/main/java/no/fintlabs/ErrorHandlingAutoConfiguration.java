package no.fintlabs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import no.fintlabs.controller.GlobalExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(LibraryDefaultsConfiguration.class)
public class ErrorHandlingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ExceptionMappingRegistry exceptionMappingRegistry() {
        return new ExceptionMappingRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    ProblemDetailFactory problemDetailFactory() {
        return new ProblemDetailFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    RequestBodyLogging requestBodyLogging(ObjectMapper objectMapper) {
        return new RequestBodyLogging(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    ResponseFilter responseFilter(ObjectProvider<Tracer> tracer, RequestBodyLogging requestBodyLogging) {
        return new ResponseFilter(tracer.getIfAvailable(), requestBodyLogging);
    }

    @Bean
    @ConditionalOnMissingBean
    GlobalExceptionHandler globalExceptionHandler(ProblemDetailFactory factory, RequestBodyLogging requestBodyLogging) {
        return new GlobalExceptionHandler(factory, requestBodyLogging);
    }
}
