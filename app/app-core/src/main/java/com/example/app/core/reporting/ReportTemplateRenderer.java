package com.example.app.core.reporting;

import com.example.app.core.logging.Logging;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;

/**
 * Thin wrapper around the FreeMarker configuration used to render report templates.
 */
class ReportTemplateRenderer {

    private static final Logger LOGGER = Logging.getLogger(ReportTemplateRenderer.class);

    private final Configuration configuration;

    ReportTemplateRenderer() {
        configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates");
        configuration.setDefaultEncoding("UTF-8");
        configuration.setLocale(Locale.getDefault());
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
    }

    String render(ReportViewModel model) throws ReportGenerationException {
        Objects.requireNonNull(model, "model");
        try {
            Template template = configuration.getTemplate("report.ftl");
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            LOGGER.error("Unable to render report template for source {}", model.getSourcePath(), e);
            throw new ReportGenerationException("Unable to render report template", e);
        }
    }
}
