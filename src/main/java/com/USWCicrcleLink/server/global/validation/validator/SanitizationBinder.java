package com.USWCicrcleLink.server.global.validation.validator;

import com.USWCicrcleLink.server.global.validation.annotation.Sanitize;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.beans.PropertyEditorSupport;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ControllerAdvice
public class SanitizationBinder extends RequestBodyAdviceAdapter {
    private static final String HTML_LINK_TAG = "a";
    private static final String HTML_LINK_HREF_ATTRIBUTE = "href";
    private static final String[] ALLOWED_HTML_TAGS = {"a", "b", "strong", "i", "em", "u", "ul", "ol", "li", "p", "br"};
    private static final Safelist SANITIZE_SAFELIST = Safelist.none()
            .addTags(ALLOWED_HTML_TAGS)
            .addAttributes(HTML_LINK_TAG, HTML_LINK_HREF_ATTRIBUTE);

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        Object target = binder.getTarget();
        if (target == null) {
            return;
        }

        for (String fieldPath : findSanitizedStringFieldPaths(target.getClass())) {
            binder.registerCustomEditor(String.class, fieldPath, new SanitizingStringEditor());
        }
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        sanitizeRecursively(body, Collections.newSetFromMap(new IdentityHashMap<>()));
        return body;
    }

    @Override
    public Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                  Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    private List<String> findSanitizedStringFieldPaths(Class<?> targetType) {
        List<String> fieldPaths = new ArrayList<>();
        ReflectionUtils.doWithFields(targetType, field -> {
            if (isSanitizableStringField(field)) {
                fieldPaths.add(field.getName());
            }
        });
        return fieldPaths;
    }

    private void sanitizeRecursively(@Nullable Object target, Set<Object> visited) {
        if (target == null || isTerminalValue(target.getClass()) || !visited.add(target)) {
            return;
        }

        if (target instanceof Collection<?> collection) {
            collection.forEach(item -> sanitizeRecursively(item, visited));
            return;
        }

        if (target instanceof Map<?, ?> map) {
            map.values().forEach(value -> sanitizeRecursively(value, visited));
            return;
        }

        if (target.getClass().isArray()) {
            int length = Array.getLength(target);
            for (int index = 0; index < length; index++) {
                sanitizeRecursively(Array.get(target, index), visited);
            }
            return;
        }

        ReflectionUtils.doWithFields(target.getClass(), field -> sanitizeField(target, field, visited));
    }

    private void sanitizeField(Object target, Field field, Set<Object> visited) {
        ReflectionUtils.makeAccessible(field);
        Object fieldValue = ReflectionUtils.getField(field, target);

        if (isSanitizableStringField(field) && fieldValue instanceof String stringValue) {
            ReflectionUtils.setField(field, target, sanitize(stringValue));
            return;
        }

        if (field.isAnnotationPresent(Sanitize.class) && fieldValue instanceof Collection<?> collection) {
            ReflectionUtils.setField(field, target, sanitizeStringCollection(collection));
            return;
        }

        if (fieldValue != null && !isTerminalValue(field.getType())) {
            sanitizeRecursively(fieldValue, visited);
        }
    }

    private List<Object> sanitizeStringCollection(Collection<?> values) {
        List<Object> sanitizedValues = new ArrayList<>(values.size());
        for (Object value : values) {
            sanitizedValues.add(value instanceof String stringValue ? sanitize(stringValue) : value);
        }
        return sanitizedValues;
    }

    private boolean isSanitizableStringField(Field field) {
        return field.isAnnotationPresent(Sanitize.class) && String.class.equals(field.getType());
    }

    private boolean isTerminalValue(Class<?> type) {
        return BeanUtils.isSimpleValueType(type) || type.isEnum();
    }

    private String sanitize(@Nullable String content) {
        if (content == null) {
            return null;
        }
        return Jsoup.clean(content, SANITIZE_SAFELIST);
    }

    private final class SanitizingStringEditor extends PropertyEditorSupport {

        @Override
        public void setAsText(String text) {
            setValue(sanitize(text));
        }
    }
}
