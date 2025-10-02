# gateway-service

## Dependencies
- Java 17
- Spring Boot 3.5.5
    - Spring Boot Starter Data JPA
    - Hibernate 6.6.26
- MySQL 8.0.14
- QueryDSL 5.1.0
- Spring Cloud
    - Eureka Client 4.3.0
- Lombok 1.18.38

## API Specification

https://docs.google.com/spreadsheets/d/1erfJV-eh3TtUMcaTJLLNou-vhwnxBvB4MV5cJ72-hw8/edit?gid=416827453#gid=416827453

## Details

#### ExceptionHandling Filter
- 게이트웨이에서 발생하는 예외에 대해 응답 객체를 생성하여 반환

#### Authentication Filter
- 요청 시 헤더로 전달받는 JWT AccessToken에 대해 유저 정보를 얻어 헤더 값으로 주입
- 이후 각 서비스에서 `annotation`을 통해 각 마이크로 서비스에서 헤더 접근없이 사용할 수 있도록 중앙 집중 방식으로 구현하여 편의성 제공
- PassportHolderArgumentResolver
    ```java
    @Component
    @RequiredArgsConstructor
    public class PassportHolderArgumentResolver implements HandlerMethodArgumentResolver {
    
        private static final String USER_ID_HEADER = "X-User-Id";
        private static final String USER_ROLE_HEADER = "X-User-Role";
    
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(PassportHolder.class)
                    && parameter.getParameterType().equals(Passport.class);
        }
    
        @Override
        public Passport resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory
        ) {
            HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
            String id = request.getHeader(USER_ID_HEADER);
            String role = request.getHeader(USER_ROLE_HEADER);
    
            if (!StringUtils.hasText(id)) {
                throw new CustomRuntimeException(CommonExceptionType.INVALID_USER_ID);
            }
    
            if (!StringUtils.hasText(role)) {
                throw new CustomRuntimeException(CommonExceptionType.INVALID_USER_ROLE);
            }
    
            return new Passport(
                    Long.valueOf(id),
                    UserRole.of(role)
            );
        }
    }
    ```
- Passport
    ```java
    public record Passport(
        Long userId,

        UserRole role
    ) {
    }
    ```
- @PassportHolder
    ```java
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface PassportHolder {
    }
    ```

#### AdminAuthorization Filter
- MASTER 요청에 대해 수직 권한 상승에 대비해 필터링
- 각 마이크로 서비스에서 AuthorizeInterceptor와 @Authorize로 수직적 권한 상승을 방지하도록 편의성 제공

- AuthorizationInterceptor
    ```java
    @Slf4j
    @Component
    public class AuthorizationInterceptor implements HandlerInterceptor {

    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Authorize authorize = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Authorize.class); // method level

        if (authorize == null) {
            authorize = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Authorize.class); // class level
        }

        // 없으면 모든 권한 가능
        if (authorize == null) {
            return true;
        }

        // 명시 목적으로 bypass == true면 모든 권한 가능
        if (authorize.bypass()) {
            return true;
        }

        // 권한 목록이 비었으면 모든 권한 가능
        if (authorize.roles().length == 0) {
            return true;
        }

        String role = request.getHeader(USER_ROLE_HEADER);

        if (!StringUtils.hasText(role)) {
            throw new CustomRuntimeException(CommonExceptionType.INVALID_USER_ROLE);
        }

        UserRole userRole = UserRole.of(role);

        boolean isExists = Arrays.asList(authorize.roles())
                .contains(userRole);

        if (!isExists) {
            throw new CustomRuntimeException(CommonExceptionType.REQUEST_ACCESS_DENIED);
        }

            return true;
        }
    }
    ```
- @Authorize
    ```java
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface Authorize {
    
        UserRole[] roles() default {};
    
        boolean bypass() default false;
    }
    ```