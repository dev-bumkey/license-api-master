package run.acloud.api.build.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Application Task Http의 custom header 모델")
public class HttpHeader {

    @Schema(description = "Header Name", required = false)
    private String name;

    @Schema(description = "Header Value", required = false)
    private String value;

    public HttpHeader(){}

    public HttpHeader(String name, String value){
        this.name = name;
        this.value = value;
    }
}
