package run.acloud.framework.configuration;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 5. 23.
 */
//@Configuration
//@EnableSwagger2
//@ComponentScan(value = "run.acloud.framework")
public class SwaggerConfig {
//    @Autowired
//    private CocktailServiceProperties cocktailServiceProperties;
//    @Autowired
//    private TypeResolver typeResolver;
//
//
//    private List<ResponseMessage> defaultResponseMessages = Arrays.asList(responseMessageBuilder().code(HttpStatus.OK.value()).message("ok").build());
//    private Class[] clazz = {HttpServletResponse.class, HttpServletRequest.class, HttpSession.class};
//    private List<Parameter> defaultParameters = Lists.newArrayList();
//    {
//        defaultParameters.add(new ParameterBuilder()
//                .name("user-id")
//                .parameterType("header")
//                .description("user-id")
//                .modelRef(new ModelRef("string"))
//                .required(false)
//                .build());
//        defaultParameters.add(new ParameterBuilder()
//                .name("user-role")
//                .parameterType("header")
//                .description("user-role")
//                .modelRef(new ModelRef("string"))
//                .required(false)
//                .build());
//        defaultParameters.add(new ParameterBuilder()
//                .name("user-workspace")
//                .parameterType("header")
//                .description("user-workspace")
//                .modelRef(new ModelRef("String"))
//                .required(false)
//                .build());
//        defaultParameters.add(new ParameterBuilder()
//                .name("user-grant")
//                .parameterType("header")
//                .description("user-grant")
//                .modelRef(new ModelRef("String"))
//                .required(false)
//                .build());
//    }
//
//    private ResponseMessageBuilder responseMessageBuilder() {
//        return new ResponseMessageBuilder();
//    }
//
//    @Bean
//    public Docket api() {
//
//        return new Docket(DocumentationType.SWAGGER_2)
//                .groupName("default")
//                .select()
//                .apis(RequestHandlerSelectors.any())
//                .paths(ant("/**"))
//                .build()
//                .globalOperationParameters(defaultParameters)
//                .globalResponseMessage(RequestMethod.GET, defaultResponseMessages)
//                .globalResponseMessage(RequestMethod.POST, defaultResponseMessages)
//                .globalResponseMessage(RequestMethod.PUT, defaultResponseMessages)
//                .globalResponseMessage(RequestMethod.DELETE, defaultResponseMessages)
//                .apiInfo(this.apiInfo())
//                .ignoredParameterTypes(clazz);
//    }
//
//    @Bean
//    public Docket externalApi() {
//        return new Docket(DocumentationType.SWAGGER_2)
//                .groupName("external")
//                .select()
//                .apis(withoutMethodAnnotation(Deprecated.class))
//                .apis(withoutMethodAnnotation(InHouse.class))
//                .apis(withoutClassAnnotation(Deprecated.class))
//                .apis(withoutClassAnnotation(InHouse.class))
//                .paths(Predicates.and( ant("/**")
//                        , Predicates.not(ant("/api/template/**"))
//                        , Predicates.not(ant("/api/test/**"))
//                        , Predicates.not(ant("/api/mig/**"))
//                        , Predicates.not(ant("/api/init/**"))
//                        , Predicates.not(ant("/api/check/**"))
//                        , Predicates.not(ant("/api/redoc"))
//                        , Predicates.not(ant("/api/label/**"))
//                        , Predicates.not(ant("/api/monitoring/**"))
//                        , Predicates.not(ant("/api/package/**"))
//                        , Predicates.not(ant("/api/grade"))
//                        , Predicates.not(ant("/api/grade/**"))
//                        , Predicates.not(ant("/api/file/**"))
//                        , Predicates.not(ant("/api/internal/cube/**"))
//                        , Predicates.not(ant("/api/cluster/*/secret"))
//                        , Predicates.not(ant("/api/cluster/*/secret/**"))
//                        , Predicates.not(ant("/api/cluster/*/secrets"))
//                        , Predicates.not(ant("/api/cluster/accessauth/**"))
//                        , Predicates.not(ant("/api/cluster/signature/**"))
//                        , Predicates.not(ant("/api/billing"))
//                        , Predicates.not(ant("/api/billing/**"))
//                        , Predicates.not(ant("/api/batch/**"))
//                        , Predicates.not(ant("/builder/**"))
//                        , Predicates.not(ant("/error"))
//                ))
//                .build()
//                .globalOperationParameters(defaultParameters)
//                .globalResponseMessage(RequestMethod.GET, defaultResponseMessages)
//                .globalResponseMessage(RequestMethod.POST, defaultResponseMessages)
//                .globalResponseMessage(RequestMethod.PUT, defaultResponseMessages)
//                .globalResponseMessage(RequestMethod.DELETE, defaultResponseMessages)
//                .apiInfo(this.apiInfo())
//                .ignoredParameterTypes(clazz);
//    }
//
//    public static Predicate<RequestHandler> withoutMethodAnnotation(final Class<? extends Annotation> annotation) {
//        return new Predicate<RequestHandler>() {
//            @Override
//            public boolean apply(RequestHandler input) {
//                return !input.isAnnotatedWith(annotation);
//            }
//        };
//    }
//
//    public static Predicate<RequestHandler> withoutClassAnnotation(final Class<? extends Annotation> annotation) {
//        return new Predicate<RequestHandler>() {
//            @Override
//            public boolean apply(RequestHandler input) {
//                return !declaringClass(input).transform(annotationPresent(annotation)).or(false);
//            }
//        };
//    }
//
//    private static Function<Class<?>, Boolean> annotationPresent(final Class<? extends Annotation> annotation) {
//        return new Function<Class<?>, Boolean>() {
//            @Override
//            public Boolean apply(Class<?> input) {
//                return input.isAnnotationPresent(annotation);
//            }
//        };
//    }
//
//    private static Optional<? extends Class<?>> declaringClass(RequestHandler input) {
//        return Optional.fromNullable(input.declaringClass());
//    }
//
//    private ApiInfo apiInfo() {
//
//        return new ApiInfo(
//                "Cocktail API",
//                "API Documentation for Cocktail",
//                cocktailServiceProperties.getReleaseVersion(),
//                "Not defined yet",
//                new Contact("acornsoft", "https://www.cocktailcloud.io/", "support@acornsoft.io"),
//                "Not defined yet",
//                "Not defined yet",
//                new Collection<VendorExtension>() {
//                    @Override
//                    public int size() {
//                        return 0;
//                    }
//
//                    @Override
//                    public boolean isEmpty() {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean contains(Object o) {
//                        return false;
//                    }
//
//                    @Override
//                    public Iterator<VendorExtension> iterator() {
//                        return null;
//                    }
//
//                    @Override
//                    public Object[] toArray() {
//                        return new Object[0];
//                    }
//
//                    @Override
//                    public <T> T[] toArray(T[] a) {
//                        return null;
//                    }
//
//                    @Override
//                    public boolean add(VendorExtension vendorExtension) {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean remove(Object o) {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean containsAll(Collection<?> c) {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean addAll(Collection<? extends VendorExtension> c) {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean removeAll(Collection<?> c) {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean retainAll(Collection<?> c) {
//                        return false;
//                    }
//
//                    @Override
//                    public void clear() {
//
//                    }
//                }
//        );
//    }
}
