package alfio.controller.api;


import alfio.TestConfiguration;
import alfio.config.DataSourceConfiguration;
import alfio.config.Initializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.lang.model.type.ArrayType;
import java.lang.reflect.ParameterizedType;
import java.util.*;

@SpringBootTest
@ContextConfiguration(classes = {DataSourceConfiguration.class, TestConfiguration.class, ControllerConfiguration.class})
@ActiveProfiles({Initializer.PROFILE_DEV, Initializer.PROFILE_DISABLE_JOBS, Initializer.PROFILE_INTEGRATION_TEST})
public class CheckJsonBodyRequest {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Check that all our classes are annotated/formed correctly so we can
     * Deserialize from json to java object.
     */
    @Test
    void checkSerialization() {
        var om = objectMapper;
        var handlerMethods = this.requestMappingHandlerMapping.getHandlerMethods();
        var classesToCheck = new HashSet<Class<?>>();
        for (var item : handlerMethods.entrySet()) {
            var method = item.getValue();
            Arrays
                .stream(method.getMethodParameters())
                .filter(p -> p.hasParameterAnnotation(RequestBody.class))
                .forEach(p -> {
                    var k = p.getGenericParameterType();
                    if (k instanceof ParameterizedType) {
                        Arrays
                            .stream(((ParameterizedType) k).getActualTypeArguments())
                            .filter(g -> g.getTypeName().startsWith("alfio.")) // keep only our types
                            .filter(g -> !((Class) g).isEnum()) // filter out enums
                            .forEach(genericParamType -> classesToCheck.add((Class) genericParamType));
                    } else if (p.getParameterType().getTypeName().startsWith("alfio.") && !p.getParameterType().isEnum()){
                        classesToCheck.add(p.getParameterType());
                    }
                });
        }
        classesToCheck.forEach(k -> {
            Assertions.assertDoesNotThrow(() -> {
                om.readValue("{}", k);
            }, "Was not able to deserialize value of type " + k);
        });
    }

}
