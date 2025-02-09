package wh.plus.crm.service.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import wh.plus.crm.helper.EmailContentBuilder;

import java.util.Map;

@Component
public class ThymeleafEmailContentBuilder implements EmailContentBuilder {

    @Autowired
    private TemplateEngine templateEngine;

    @Override
    public String build(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        // Nazwa szablonu bez rozszerzenia – Thymeleaf domyślnie szuka plików .html w katalogu templates
        return templateEngine.process(templateName, context);
    }


}
