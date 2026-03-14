package com.USWCicrcleLink.server.global.validation.validator;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;

@ControllerAdvice
public class SanitizationBinder {
    private static final Safelist ALLOWED_SAFELIST = Safelist.none()
            .addTags("a", "b", "strong", "i", "em", "u", "ul", "ol", "li", "p", "br")
            .addAttributes("a", "href");

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                super.setValue(sanitizeContent(text));
            }
        });
    }

    private String sanitizeContent(String content) {
        if (content == null) {
            return null;
        }
        return Jsoup.clean(content, ALLOWED_SAFELIST);
    }
}
