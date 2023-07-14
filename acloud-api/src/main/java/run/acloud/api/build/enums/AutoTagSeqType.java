package run.acloud.api.build.enums;

import run.acloud.commons.enums.EnumCode;

public enum AutoTagSeqType implements EnumCode {
    DATETIME("%tY%<tm%<td-%<tH%<tM%<tS"), SEQUENCE("%05d");

    private String seqFormat;

    AutoTagSeqType(String seqFormat){
        this.seqFormat = seqFormat;
    }

    public<T> String getSequence(T param){
        String seq = String.format(this.seqFormat, param);
        return seq;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
