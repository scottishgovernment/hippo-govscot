package scot.gov.publishing.hippo.hst.request;

import freemarker.core.Environment;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.hippoecm.hst.servlet.HstFreemarkerServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Writer;
import java.util.stream.Collectors;

import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.stream;

/**
 * Logs template errors in a structured log format.
 */
public class LoggingHstFreemarkerServlet extends HstFreemarkerServlet {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingHstFreemarkerServlet.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        getConfiguration().setTemplateExceptionHandler(
                LoggingHstFreemarkerServlet::logFreemarkerException);
    }

    static void logFreemarkerException(TemplateException ex, Environment env, Writer out) {
        String messageWithoutStackTop = ex.getMessageWithoutStackTop();
        String[] messageParts = messageWithoutStackTop.split("\n----");
        String message = messageParts[0].trim();
        String tips = stream(copyOfRange(messageParts, 1, messageParts.length))
                .map(String::trim)
                .collect(Collectors.joining("\n"));
        String position = String.format("%s[%d:%d]-[%d:%d]",
                ex.getTemplateSourceName(),
                ex.getLineNumber(),
                ex.getColumnNumber(),
                ex.getEndLineNumber(),
                ex.getEndColumnNumber());

        LOG.atWarn()
                .addKeyValue("message", message)
                .addKeyValue("tips", tips)
                .addKeyValue("position", position)
                .addKeyValue("expression", ex.getBlamedExpressionString())
                .addKeyValue("ftlStack", ex.getFTLInstructionStack())
                .addArgument(message)
                .log("Error in template {}", ex);
    }

}
