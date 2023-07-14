package run.acloud.commons.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jaxb.runtime.DatatypeConverterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailSignatureProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Component
public class SignatureUtils {
    @Autowired
    private CocktailSignatureProperties cocktailSignatureProperties;

    /**
     * Signed Hash 발급 (Default Secret 사용)
     * @param payload
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public String getSignedHmacKey(String payload) throws NoSuchAlgorithmException, InvalidKeyException {
        return getSignedHmacKey(cocktailSignatureProperties.getSigSecret(), payload);
    }

    /**
     * Signed Hash 발급
     * @param secret
     * @param payload
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public String getSignedHmacKey(String secret, String payload) throws NoSuchAlgorithmException, InvalidKeyException {
        String sigAlg = cocktailSignatureProperties.getSigHmacAlg();

        return this.getSignedHmacKey(secret, payload, sigAlg);
    }


    /**
     * Signed Hash 발급
     * @param secret
     * @param payload
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public String getSignedHmacKey(String secret, String payload, String sigAlg) throws NoSuchAlgorithmException, InvalidKeyException {
        if(StringUtils.isEmpty(sigAlg)) {
            throw new CocktailException("Signature Method Algorithm is null", ExceptionType.InvalidParameter);
        }
        final Mac hasher = Mac.getInstance(sigAlg);
        hasher.init(new SecretKeySpec(secret.getBytes(), sigAlg));
        final byte[] hash = hasher.doFinal(payload.getBytes());

        return DatatypeConverterImpl.theInstance.printHexBinary(hash).toLowerCase();
    }
}