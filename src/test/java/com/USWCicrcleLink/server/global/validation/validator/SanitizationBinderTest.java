package com.USWCicrcleLink.server.global.validation.validator;

import com.USWCicrcleLink.server.global.validation.annotation.Sanitize;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.bind.WebDataBinder;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SanitizationBinderTest {

    private final SanitizationBinder sanitizationBinder = new SanitizationBinder();

    @Test
    void Sanitize_어노테이션이_붙은_필드만_정제한다() {
        TestRequest target = new TestRequest();
        WebDataBinder binder = new WebDataBinder(target);
        MutablePropertyValues propertyValues = new MutablePropertyValues();

        sanitizationBinder.initBinder(binder);
        propertyValues.add("sanitizedContent", "<script>alert('xss')</script><b>safe</b>");
        propertyValues.add("token", "token-<value>");

        binder.bind(propertyValues);

        assertThat(target.getSanitizedContent()).isEqualTo("<b>safe</b>");
        assertThat(target.getToken()).isEqualTo("token-<value>");
    }

    @Test
    void 바인딩_타깃이_없으면_예외_없이_종료한다() {
        WebDataBinder binder = new WebDataBinder(null);

        sanitizationBinder.initBinder(binder);

        assertThat(binder.getTarget()).isNull();
    }

    @Test
    void RequestBody도_Sanitize_어노테이션이_붙은_필드만_정제한다() throws NoSuchMethodException, IOException {
        TestRequest body = new TestRequest();
        body.setSanitizedContent("<script>alert('xss')</script><b>safe</b>");
        body.setToken("token-<value>");

        Object sanitizedBody = sanitizationBinder.afterBodyRead(
                body,
                new MockHttpInputMessage(new byte[0]),
                createMethodParameter(),
                TestRequest.class,
                MappingJackson2HttpMessageConverter.class
        );

        assertThat(sanitizedBody).isSameAs(body);
        assertThat(body.getSanitizedContent()).isEqualTo("<b>safe</b>");
        assertThat(body.getToken()).isEqualTo("token-<value>");
    }

    @Test
    void 비밀번호와_UUID_필드는_Sanitize_대상이_아니어서_원본을_유지한다() throws NoSuchMethodException, IOException {
        TestRequest body = new TestRequest();
        UUID uuid = UUID.randomUUID();
        body.setSanitizedContent("<script>alert('xss')</script><b>safe</b>");
        body.setPassword("P@ss<word>123");
        body.setUuid(uuid);

        sanitizationBinder.afterBodyRead(
                body,
                new MockHttpInputMessage(new byte[0]),
                createMethodParameter(),
                TestRequest.class,
                MappingJackson2HttpMessageConverter.class
        );

        assertThat(body.getSanitizedContent()).isEqualTo("<b>safe</b>");
        assertThat(body.getPassword()).isEqualTo("P@ss<word>123");
        assertThat(body.getUuid()).isEqualTo(uuid);
    }

    @Test
    void 컬렉션_필드에_붙은_Sanitize_어노테이션도_문자열_원소를_정제한다() throws NoSuchMethodException, IOException {
        TestRequest body = new TestRequest();
        body.setTags(List.of("<script>x</script><b>tag</b>", "<i>safe</i>"));

        sanitizationBinder.afterBodyRead(
                body,
                new MockHttpInputMessage(new byte[0]),
                createMethodParameter(),
                TestRequest.class,
                MappingJackson2HttpMessageConverter.class
        );

        assertThat(body.getTags()).containsExactly("<b>tag</b>", "<i>safe</i>");
    }

    private MethodParameter createMethodParameter() throws NoSuchMethodException {
        Method method = SanitizationBinderTest.class.getDeclaredMethod("testRequestBody", TestRequest.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void testRequestBody(TestRequest request) {
    }

    private static final class TestRequest {
        @Sanitize
        private String sanitizedContent;
        private String token;
        private String password;
        private UUID uuid;
        @Sanitize
        private List<String> tags;

        public String getSanitizedContent() {
            return sanitizedContent;
        }

        public void setSanitizedContent(String sanitizedContent) {
            this.sanitizedContent = sanitizedContent;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public UUID getUuid() {
            return uuid;
        }

        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }
}
