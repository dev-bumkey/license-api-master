package run.acloud.api.terminal.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class TerminalMessage {

    public TerminalMessage(String Op, String Data){
        this.Op = Op;
        this.Data = Data;
    }
    private String Op;
    private String Data;
    private int Rows;
    private int Cols;
}
