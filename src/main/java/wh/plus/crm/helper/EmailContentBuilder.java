package wh.plus.crm.helper;

import java.util.Map;

public interface EmailContentBuilder {

    String build(String templateName, Map<String, Object> variables);

}
