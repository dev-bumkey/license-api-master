package run.acloud.commons.util;

import com.google.common.collect.Maps;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailCryptoProperties;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;


@Slf4j
@Component
public class CryptoUtils {
    private static CocktailCryptoProperties cocktailCryptoProperties;

    @Autowired
    private CocktailCryptoProperties injectedCocktailCryptoProperties;

    @PostConstruct
    public void init() {
        CryptoUtils.cocktailCryptoProperties = injectedCocktailCryptoProperties;
    }

    private CryptoUtils() {
    }

    public static String encryptAES(String value, boolean isThrow) throws CocktailException {
        try {
            if(StringUtils.isNotBlank(value)){
                String key = StringUtils.left(String.format("%s%s", ResourceUtil.getResourcePrefix(), new String(Base64.decodeBase64(cocktailCryptoProperties.getAesKey()))), 32);

                SecureRandom random = new SecureRandom();
                byte[] bytes = new byte[20];
                random.nextBytes(bytes);
                byte[] saltBytes = bytes;

                // Password-Based Key Derivation function 2
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                // 70000번 해시하여 256 bit 길이의 키를 만든다.
                PBEKeySpec spec = new PBEKeySpec(key.toCharArray(), saltBytes, 1000, 256);

                SecretKey secretKey = factory.generateSecret(spec);
                SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

                // 알고리즘/모드/패딩
                // CBC : Cipher Block Chaining Mode
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, secret);
                AlgorithmParameters params = cipher.getParameters();

                if (params != null) {
                    // Initial Vector(1단계 암호화 블록용)
                    IvParameterSpec ivParameterSpec = params.getParameterSpec(IvParameterSpec.class);
                    if (ivParameterSpec != null) {
                        byte[] ivBytes = ivParameterSpec.getIV();

                        byte[] encryptedTextBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
                        byte[] buffer = new byte[saltBytes.length + ivBytes.length + encryptedTextBytes.length];
                        System.arraycopy(saltBytes, 0, buffer, 0, saltBytes.length);
                        System.arraycopy(ivBytes, 0, buffer, saltBytes.length, ivBytes.length);
                        System.arraycopy(encryptedTextBytes, 0, buffer, saltBytes.length + ivBytes.length, encryptedTextBytes.length);

                        return Base64.encodeBase64String(buffer);
                    }
                }
            }

        } catch (GeneralSecurityException mex) {
            CocktailException ce = new CocktailException("fail CryptoUtils.encryptAES!!", mex, ExceptionType.CryptoFail_EncryptAES);
            if (isThrow){
                throw ce;
            } else {
                log.warn(ce.getMessage(), ce);
            }
        } catch (Exception ex) {
            CocktailException ce = new CocktailException("fail CryptoUtils.encryptAES!!", ex, ExceptionType.CryptoFail_EncryptAES);
            if (isThrow){
                throw ce;
            } else {
                log.warn(ce.getMessage(), ce);
            }
        }

        return null;
    }

    public static String encryptAES(String value) {
        try {
            return CryptoUtils.encryptAES(value, false);
        }catch (CocktailException e){
            return null;
        }
    }

    public static String decryptAES(String encryptedStr, boolean isThrow) throws CocktailException {
        try {
            if(StringUtils.isNotBlank(encryptedStr)){
                String key = StringUtils.left(String.format("%s%s", ResourceUtil.getResourcePrefix(), new String(Base64.decodeBase64(cocktailCryptoProperties.getAesKey()))), 32);

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                ByteBuffer buffer = ByteBuffer.wrap(Base64.decodeBase64(encryptedStr));

                byte[] saltBytes = new byte[20];
                buffer.get(saltBytes, 0, saltBytes.length);
                byte[] ivBytes = new byte[cipher.getBlockSize()];
                buffer.get(ivBytes, 0, ivBytes.length);
                byte[] encryptedTextBytes = new byte[buffer.capacity() - saltBytes.length - ivBytes.length];
                buffer.get(encryptedTextBytes);

                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                PBEKeySpec spec = new PBEKeySpec(key.toCharArray(), saltBytes, 1000, 256);

                SecretKey secretKey = factory.generateSecret(spec);
                SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

                cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes));

                byte[] decryptedTextBytes = cipher.doFinal(encryptedTextBytes);

                return new String(decryptedTextBytes);
            }

        } catch (GeneralSecurityException gse) {
            CocktailException ce = new CocktailException("fail CryptoUtils.decryptAES!!", gse, ExceptionType.CryptoFail_DecryptAES);
            if (isThrow){
                throw ce;
            } else {
                log.warn(ce.getMessage(), ce);
            }
        } catch (Exception ex) {
            CocktailException ce = new CocktailException("fail CryptoUtils.decryptAES!!", ex, ExceptionType.CryptoFail_DecryptAES);
            if (isThrow){
                throw ce;
            } else {
                log.warn(ce.getMessage(), ce);
            }
        }

        return null;
    }

    public static String decryptAES(String encryptedStr){
        try {
            return CryptoUtils.decryptAES(encryptedStr, false);
        }catch (CocktailException e){
            return null;
        }

    }

    public static String encryptDefaultAES(String value, boolean isThrow) throws CocktailException {
        try {
            if(StringUtils.isNotBlank(value)){
                String key = StringUtils.left(new String(Base64.decodeBase64(cocktailCryptoProperties.getDefaultAesKey())), 32);

                SecureRandom random = new SecureRandom();
                byte[] bytes = new byte[20];
                random.nextBytes(bytes);
                byte[] saltBytes = bytes;

                // Password-Based Key Derivation function 2
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                // 70000번 해시하여 256 bit 길이의 키를 만든다.
                PBEKeySpec spec = new PBEKeySpec(key.toCharArray(), saltBytes, 1000, 256);

                SecretKey secretKey = factory.generateSecret(spec);
                SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

                // 알고리즘/모드/패딩
                // CBC : Cipher Block Chaining Mode
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, secret);
                AlgorithmParameters params = cipher.getParameters();

                if (params != null) {
                    // Initial Vector(1단계 암호화 블록용)
                    byte[] ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();

                    byte[] encryptedTextBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
                    byte[] buffer = new byte[saltBytes.length + ivBytes.length + encryptedTextBytes.length];
                    System.arraycopy(saltBytes, 0, buffer, 0, saltBytes.length);
                    System.arraycopy(ivBytes, 0, buffer, saltBytes.length, ivBytes.length);
                    System.arraycopy(encryptedTextBytes, 0, buffer, saltBytes.length + ivBytes.length, encryptedTextBytes.length);

                    return Base64.encodeBase64String(buffer);
                }
            }

        } catch (GeneralSecurityException me) {
            CocktailException ce = new CocktailException("fail CryptoUtils.encryptDefaultAES!!", me, ExceptionType.CryptoFail_DecryptAES);
            if (isThrow){
                throw ce;
            } else {
                log.warn(ce.getMessage(), ce);
            }
        } catch (Exception ex) {
            CocktailException ce = new CocktailException("fail CryptoUtils.encryptDefaultAES!!", ex, ExceptionType.CryptoFail_DecryptAES);
            if (isThrow){
                throw ce;
            } else {
                log.warn(ce.getMessage(), ce);
            }
        }

        return null;
    }

    public static String encryptDefaultAES(String value) {
        try {
            return CryptoUtils.encryptDefaultAES(value, false);
        }catch (CocktailException e){
            return null;
        }
    }

    public static String decryptDefaultAES(String encryptedStr, boolean isThrow) throws CocktailException {
        try {
            if(StringUtils.isNotBlank(encryptedStr)){
                String key = StringUtils.left(new String(Base64.decodeBase64(cocktailCryptoProperties.getDefaultAesKey())), 32);

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                ByteBuffer buffer = ByteBuffer.wrap(Base64.decodeBase64(encryptedStr));

                byte[] saltBytes = new byte[20];
                buffer.get(saltBytes, 0, saltBytes.length);
                byte[] ivBytes = new byte[cipher.getBlockSize()];
                buffer.get(ivBytes, 0, ivBytes.length);
                byte[] encryptedTextBytes = new byte[buffer.capacity() - saltBytes.length - ivBytes.length];
                buffer.get(encryptedTextBytes);

                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                PBEKeySpec spec = new PBEKeySpec(key.toCharArray(), saltBytes, 1000, 256);

                SecretKey secretKey = factory.generateSecret(spec);
                SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

                cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes));

                byte[] decryptedTextBytes = cipher.doFinal(encryptedTextBytes);

                return new String(decryptedTextBytes);
            }

        } catch (GeneralSecurityException ge) {
            CocktailException ce = new CocktailException("fail CryptoUtils.decryptDefaultAES!!", ge, ExceptionType.CryptoFail_DecryptAES);
            if (isThrow){
                throw ce;
            } else {
                log.warn(ce.getMessage(), ce);
            }
        } catch (Exception ex) {
            CocktailException ce = new CocktailException("fail CryptoUtils.decryptDefaultAES!!", ex, ExceptionType.CryptoFail_DecryptAES);
            if (isThrow){
                throw ce;
            } else {
                log.warn(ce.getMessage(), ce);
            }
        }

        return null;
    }

    public static String decryptDefaultAES(String encryptedStr){
        try {
            return CryptoUtils.decryptDefaultAES(encryptedStr, false);
        }catch (CocktailException e){
            return null;
        }

    }


    public static String encryptRSA(String value, boolean isThrow) throws CocktailException {
        try {
            if(StringUtils.isNotBlank(value)){
                String strPublicKey = CryptoUtils.decryptDefaultAES(cocktailCryptoProperties.getRsaPublicKey(), isThrow);

                if (StringUtils.isNotBlank(strPublicKey)) {
                    //평문으로 전달받은 공개키를 공개키객체로 만드는 과정
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    byte[] bytePublicKey = java.util.Base64.getDecoder().decode(strPublicKey.getBytes());
                    X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(bytePublicKey);
                    PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

                    //만들어진 공개키객체를 기반으로 암호화모드로 설정하는 과정
                    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
                    cipher.init(Cipher.ENCRYPT_MODE, publicKey);

                    //평문을 암호화하는 과정
                    byte[] byteEncryptedData = cipher.doFinal(value.getBytes());
                    return java.util.Base64.getEncoder().encodeToString(byteEncryptedData);
                }

            }
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | InvalidKeySpecException |BadPaddingException | InvalidKeyException exs) {
            CocktailException ce = new CocktailException("fail CryptoUtils.encryptRSA!!", exs, ExceptionType.CryptoFail_EncryptRSA);
            if (isThrow){
                throw ce;
            } else {
                log.warn(ce.getMessage(), ce);
            }
        }

        return null;
    }

    public static String encryptRSA(String value) {
        try {
            return CryptoUtils.encryptRSA(value, false);
        }catch (CocktailException e){
            return null;
        }
    }

    public static String decryptRSA(String encryptedStr, boolean isThrow) throws CocktailException {
        try {
            if(StringUtils.isNotBlank(encryptedStr)){
                String strPrivateKey = CryptoUtils.decryptDefaultAES(cocktailCryptoProperties.getRsaPrivateKey(), isThrow);

                if (StringUtils.isNotBlank(strPrivateKey)) {
                    //평문으로 전달받은 개인키를 개인키객체로 만드는 과정
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    byte[] bytePrivateKey = java.util.Base64.getDecoder().decode(strPrivateKey.getBytes());
                    PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bytePrivateKey);
                    PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

                    //만들어진 개인키객체를 기반으로 암호화모드로 설정하는 과정
                    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
                    cipher.init(Cipher.DECRYPT_MODE, privateKey);

                    //암호문을 평문화하는 과정
                    byte[] byteEncryptedData = java.util.Base64.getDecoder().decode(encryptedStr.getBytes());
                    byte[] byteDecryptedData = cipher.doFinal(byteEncryptedData);
                    return new String(byteDecryptedData);
                }

            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException exs) {
            CocktailException ce = new CocktailException("fail CryptoUtils.decryptRSA!!", exs, ExceptionType.CryptoFail_DecryptRSA);
            log.debug(ce.getMessage(), ce);
            if (isThrow){
                throw ce;
            } else {
                log.warn(ce.getMessage(), ce);
            }
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            CocktailException ce = new CocktailException("fail CryptoUtils.decryptRSA!!", ex, ExceptionType.CryptoFail_DecryptRSA);
            log.debug(ce.getMessage(), ce);
            if (isThrow){
                throw ce;
            } else {
                log.warn(ce.getMessage(), ce);
            }
        }

        return null;
    }

    public static String decryptRSA(String encryptedStr){
        try {
            return CryptoUtils.decryptRSA(encryptedStr, false);
        }catch (CocktailException e){
            return null;
        }

    }

    public static Map<String, Object> getRSAPublicKeySpec(boolean isThrow) throws CocktailException {

        Map<String, Object> rsa = Maps.newHashMap();

        try {
            String strPublicKey = CryptoUtils.decryptDefaultAES(cocktailCryptoProperties.getRsaPublicKey(), isThrow);

            if (StringUtils.isNotBlank(strPublicKey)) {
                //평문으로 전달받은 공개키를 공개키객체로 만드는 과정
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                byte[] bytePublicKey = java.util.Base64.getDecoder().decode(strPublicKey.getBytes());
                X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(bytePublicKey);
                PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

                RSAPublicKeySpec publicSpec = keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);
                String modulus = publicSpec.getModulus().toString(16);
                String exponent = publicSpec.getPublicExponent().toString(16);
                rsa.put("modulus", modulus);
                rsa.put("exponent", exponent);

                return rsa;
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exs) {
            CocktailException ce = new CocktailException("fail CryptoUtils.getRSAPublicKeySpec!!", exs, ExceptionType.CryptoFail_GetRSAPublicKeySpec);
            if (isThrow){
                throw ce;
            } else {
                log.warn(ce.getMessage(), ce);
            }
        }

        return null;
    }

    public static Map<String, Object> getRSAPublicKeySpec(){
        try {
            return CryptoUtils.getRSAPublicKeySpec(false);
        }catch (CocktailException e){
            return null;
        }
    }

    public static String getStringRSAPublicKey(boolean isThrow) throws CocktailException {

        try {
            String strPublicKey = CryptoUtils.decryptDefaultAES(cocktailCryptoProperties.getRsaPublicKey(), isThrow);

            if (StringUtils.isNotBlank(strPublicKey)) {
                //평문으로 전달받은 공개키를 공개키객체로 만드는 과정
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                byte[] bytePublicKey = java.util.Base64.getDecoder().decode(strPublicKey.getBytes());
                X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(bytePublicKey);
                PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

                return java.util.Base64.getEncoder().encodeToString(publicKey.getEncoded());
            }
        } catch (NoSuchAlgorithmException ex) {
            CocktailException ce = new CocktailException("fail CryptoUtils.getStringRSAPublicKey!!", ex, ExceptionType.CryptoFail_GetRSAPublicKeySpec);
            log.debug(ce.getMessage(), ce);
            if (isThrow){
                throw ce;
            } else {
                log.warn(ce.getMessage(), ce);
            }
        } catch (InvalidKeySpecException ex) {
            CocktailException ce = new CocktailException("fail CryptoUtils.getStringRSAPublicKey!!", ex, ExceptionType.CryptoFail_GetRSAPublicKeySpec);
            if (isThrow){
                throw ce;
            } else {
                log.warn(ce.getMessage(), ce);
            }
        }

        return null;
    }

    public static String getStringRSAPublicKey(){
        try {
            return CryptoUtils.getStringRSAPublicKey(false);
        }catch (CocktailException e){
            return null;
        }

    }

    public static String generateSalt() throws Exception {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return new String(java.util.Base64.getEncoder().encode(bytes));
    }


}
