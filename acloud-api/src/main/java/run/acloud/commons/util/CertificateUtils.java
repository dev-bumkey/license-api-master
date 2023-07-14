package run.acloud.commons.util;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.io.pem.PemObject;
import run.acloud.commons.vo.CertificateVO;

import javax.security.auth.x500.X500Principal;
import javax.security.auth.x500.X500PrivateCredential;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
public class CertificateUtils {

    /**
     * =============================
     * Root Certificate Information
     */
    private static final byte[] rootPublicKey = Base64.decode(
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi6PdDpgZPCLqrsi7A1Au\n" +
            "orwPokJ7a+1GmAetvgMaQivHmLu6VfnvaaDiHNisfwoM49Pzs8VY1Jshc/vKS280\n" +
            "UsHpzvVOecpagmHKo1k/582h33+oRNCJJqXtT0P7va48HwbxIVxD2GbH2MQi9YVE\n" +
            "cfWkiwOVbxnFL1MGr08wQ6iV9vF6tuzgZUUieDZISFblMVU71dD3wjzcMPRtfpCj\n" +
            "T3JuZ0bsasrYa8571iKgQ6oYPad7e6ky6SK90mbipxKzzXlyWe1bDygz0ElVoH9U\n" +
            "0IFFmbwjP10oci2aZH7hph4DTeKLmfc4JPXrmklk9v4XOC27dW0wSBLNso6RVCKC\n" +
            "vQIDAQAB");

    private static final byte[] rootPrivateKey = Base64.decode(
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCLo90OmBk8Iuqu\n" +
            "yLsDUC6ivA+iQntr7UaYB62+AxpCK8eYu7pV+e9poOIc2Kx/Cgzj0/OzxVjUmyFz\n" +
            "+8pLbzRSwenO9U55ylqCYcqjWT/nzaHff6hE0Ikmpe1PQ/u9rjwfBvEhXEPYZsfY\n" +
            "xCL1hURx9aSLA5VvGcUvUwavTzBDqJX28Xq27OBlRSJ4NkhIVuUxVTvV0PfCPNww\n" +
            "9G1+kKNPcm5nRuxqythrznvWIqBDqhg9p3t7qTLpIr3SZuKnErPNeXJZ7VsPKDPQ\n" +
            "SVWgf1TQgUWZvCM/XShyLZpkfuGmHgNN4ouZ9zgk9euaSWT2/hc4Lbt1bTBIEs2y\n" +
            "jpFUIoK9AgMBAAECggEAKNPYyq8S1b/ZwQ/EihP0BrWYlpb8khI2bTX1iRRMJaO6\n" +
            "QIBG22NvqgEVi1yYlq5AFY6/dsTRE5xl3Az22TZi5H142H9lmftdmjhwcHEkW6iG\n" +
            "VxvyfWhBaXDjISVd4dgjEOlNMDVTSt7GOK0kEYgM+orZOKHsG6c7vXn3fuNW17jw\n" +
            "ynu92ihuIxEmm81WVio5IY9lcpkBpYRPyq4a36kQb0cErZFHgCIs13hELyhyJTVW\n" +
            "m7CqZb8N3punjg28+3kb4KcsAE8d0z2M7zDBRmLvFia9ydqWyeLySRyx9+DZUgt9\n" +
            "t+8D3vHtx0LOgOObaXP9baeNqrQC3FRkcB0WGMXo+wKBgQC/FGK2dPXUCkyUziVI\n" +
            "k+J8OynD9Q/sNXTHCGkPk6Ww0C+qbRFx0j2kOOCLcNBIESyQ87Z21h5hAmlVVLYt\n" +
            "5VMerTDys4VrohiOvqyOEI6J+tqYg1whlU/Btgs8seu2BSOt4BuDhSM+tFvc69y0\n" +
            "aFZ0iP1fcBAerjawlgNIKKCmjwKBgQC7FWYxss666zP08gSgIhtteTm+U3HWG++T\n" +
            "5jgt6UMOVcWFi1xLutFk8QCibVe5fze+i8GnxKwIGeLzBPG4/u+/bWyD9Xuldaj2\n" +
            "lswFelWg0pEzVdgLmQxnV42FEaC/ktT+zYNTOB8Yaaqv6lYpFbT6JrBTOgADv6pj\n" +
            "9Bbc5sGH8wKBgAX33gly58bCt5eiRLnanKVit7A+NEwdc1NQKO/qna9DIWoCVBwp\n" +
            "A/HgMOtKD8dgefLhGd7mWzaOe7nFlpciZZE8uZ2rC8zL2FaFECCz1a/rnO5MlBEc\n" +
            "d8xGIl239PPPf0jMCi/2dZ5cxc3FtDQSWjjzmm5jsq0ypdSoKtwTIMlzAoGAGLID\n" +
            "cm4DAgOIMT49RBIcgr/s0mXbcgLwBhrusYjIFD1YPH+f6FM5ztRNXCOI3/CeICX8\n" +
            "ozO/7bXVEnVFn0DvztoX5/dgof+9FQfk2JhEc104U4lyYl2KmJ2jDby89mzgdt4U\n" +
            "BskyuZtBL8WEKBo4cIjo36OZWuVTDayr0+8V/ucCgYEAjnbHJjynyhcDQlh8I9hW\n" +
            "Q3MkNGdtLEMTPqMw3ysaH3ejxC6ejfVLLi3BxEcmxXH/agDxsrfMvC3Egv8aHVjs\n" +
            "Jyx9bFI97Q5PEUPfgxxlWSB4hKLCvMRyTyqOnt9T7NSomKoiOWtzXwNLe7iAIwPt\n" +
            "U6LWbol17o+PRvKZSktTXBc=");

    /**
     * ===============================================
     * Cocktail Root Certificate Principal Information
     */
    public static final String ROOT_CN = "cocktailcloud.io";
    public static final String ROOT_C = "KR";
    public static final String ROOT_ST = "Seoul";
    public static final String ROOT_O = "Acornsoft";
    public static final String ROOT_OU = "Cocktail Lab";

    /**
     * ====================================
     * Common Environment
     */
    // 1 week
    private static final long VALIDITY_PERIOD = 7 * 24 * 60 * 60 * 1000L;

    public static final String INTERMEDIATE_CN = "pki.cocktailcloud.io";
    public static final String INTERMEDIATE_C = "KR";
    public static final String INTERMEDIATE_ST = "Seoul";
    public static final String INTERMEDIATE_O = "Acornsoft";
    public static final String INTERMEDIATE_OU = "Cocktail Lab";

    public static final String ROOT_ALIAS = "root";
    public static final String INTERMEDIATE_ALIAS = "intermediate";
    public static final String END_ENTITY_ALIAS = "end";

    private static long baseTime = 0x17217671c60L; // Root certificate 값을 항상 균일하게 생성하기 위함...

    /**
     * Generate RSA KeyPair
     *
     * @return
     * @throws Exception
     */
    public static KeyPair generateRSAKeyPair() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen.initialize(2048, new SecureRandom());

        return kpGen.generateKeyPair();
    }

    /**
     * Convert X509CertificateHolder -> X509Certificate
     *
     * @param certHolder
     * @return
     * @throws CertificateException
     */
    private static X509Certificate convertCert(X509CertificateHolder certHolder) throws CertificateException {
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
    }

    /**
     * Generate RSA Root KeyPair
     *
     * @return
     * @throws Exception
     */
    public static KeyPair generateRootKeyPair() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyFactory kFact = KeyFactory.getInstance("RSA", "BC");

        return new KeyPair(
            kFact.generatePublic(new X509EncodedKeySpec(rootPublicKey)),
            kFact.generatePrivate(new PKCS8EncodedKeySpec(rootPrivateKey)));
    }

    /**
     * Generate RSA Root KeyPair from Exists KeyPair
     *
     * @return
     * @throws Exception
     */
    public static KeyPair generateRootKeyPair(String publicKey, String privateKey) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyFactory kFact = KeyFactory.getInstance("RSA", "BC");

        return new KeyPair(
            kFact.generatePublic(new X509EncodedKeySpec(Base64.decode(publicKey))),
            kFact.generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(privateKey))));
    }

    /**
     * Generate a sample V1 certificate to use as a CA root certificate
     */
    public static X509CertificateHolder generateRootCert(KeyPair pair, boolean isNew) throws Exception {
        StringBuilder principalSb = new StringBuilder();
        principalSb.append(String.format("CN=%s", ROOT_CN));
        principalSb.append(String.format(", C=%s", ROOT_C));
        principalSb.append(String.format(", ST=%s", ROOT_ST));
        principalSb.append(String.format(", O=%s", ROOT_O));
        principalSb.append(String.format(", OU=%s", ROOT_OU));
        String principalStr = principalSb.toString();

        long settedBaseTime;
        if(isNew) {
            settedBaseTime = System.currentTimeMillis();
        }
        else {
            settedBaseTime = baseTime;
        }

        JcaX509v1CertificateBuilder certBldr = new JcaX509v1CertificateBuilder(
            new X500Principal(principalStr),
            BigInteger.valueOf(1),
            new Date(settedBaseTime),
            new Date(settedBaseTime + 1024 * VALIDITY_PERIOD), // allow 1024 weeks for the root
            new X500Principal(principalStr),
            pair.getPublic()
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(pair.getPrivate());

        return certBldr.build(signer);
    }

    /**
     * Generate a sample V3 certificate to use as an intermediate CA certificate
     */
    public static X509CertificateHolder generateIntermediateCert(PublicKey intKey, PrivateKey caKey, X509Certificate caCert) throws Exception {
        StringBuilder principalSb = new StringBuilder();
        principalSb.append(String.format("CN=%s", INTERMEDIATE_CN));
        principalSb.append(String.format(", C=%s", INTERMEDIATE_C));
        principalSb.append(String.format(", ST=%s", INTERMEDIATE_ST));
        principalSb.append(String.format(", O=%s", INTERMEDIATE_O));
        principalSb.append(String.format(", OU=%s", INTERMEDIATE_OU));
        String principalStr = principalSb.toString();

        long timeMillis = System.currentTimeMillis();

        JcaX509v3CertificateBuilder certBldr = new JcaX509v3CertificateBuilder(
            caCert.getSubjectX500Principal(),
            BigInteger.valueOf(1),
            new Date(timeMillis),
            new Date(timeMillis + 1024 * VALIDITY_PERIOD),
            new X500Principal(principalStr),
            intKey
        );

        JcaX509ExtensionUtils utils = new JcaX509ExtensionUtils();
        certBldr.addExtension(Extension.authorityKeyIdentifier, false, utils.createAuthorityKeyIdentifier(caCert));
        certBldr.addExtension(Extension.subjectKeyIdentifier, false, utils.createSubjectKeyIdentifier(intKey));
        certBldr.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
        certBldr.addExtension(Extension.keyUsage, true,
            new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.cRLSign)
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(caKey);
        return certBldr.build(signer);
    }

    /**
     * Generate a sample V3 certificate to use as an end entity certificate
     */
    public static X509CertificateHolder generateEndEntityCert(PublicKey entityKey, PrivateKey caKey, X509Certificate caCert, List<String> domains, List<String> ipAddress) throws Exception {
        StringBuilder principalSb = new StringBuilder();
        principalSb.append(String.format("CN=%s", "cert.cocktailcloud.io"));
        String principalStr = principalSb.toString();

        long timeMillis = System.currentTimeMillis();

        JcaX509v3CertificateBuilder certBldr = new JcaX509v3CertificateBuilder(
            caCert.getSubjectX500Principal(),
            BigInteger.valueOf(1),
            new Date(timeMillis),
            new Date(timeMillis + 1024 * VALIDITY_PERIOD),
            new X500Principal(principalStr),
            entityKey
        );
        JcaX509ExtensionUtils utils = new JcaX509ExtensionUtils();
        certBldr.addExtension(Extension.authorityKeyIdentifier, false, utils.createAuthorityKeyIdentifier(caCert));
        certBldr.addExtension(Extension.subjectKeyIdentifier, false, utils.createSubjectKeyIdentifier(entityKey));
        certBldr.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certBldr.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));

        /** IP Address, Domain Extension 설정 **/
        int arraySize = 0;
        if(CollectionUtils.isNotEmpty(domains)) {
            arraySize = arraySize + domains.size();
        }
        if(CollectionUtils.isNotEmpty(ipAddress)) {
            arraySize = arraySize + ipAddress.size();
        }

        log.debug("array size : " + arraySize);
        if(arraySize > 0) {
            GeneralName[] generalNameArr = new GeneralName[arraySize];
            int inputCount = 0;
            for(String domain : Optional.ofNullable(domains).orElseGet(() ->Lists.newArrayList())) {
                log.debug("domain/ip count : " + inputCount + " : " + domain);
                generalNameArr[inputCount] = new GeneralName(GeneralName.dNSName, domain);
                inputCount++;
            }
            for(String ip : Optional.ofNullable(ipAddress).orElseGet(() ->Lists.newArrayList())) {
                log.debug("domain/ip count : " + inputCount + " : " + ip);
                generalNameArr[inputCount] = new GeneralName(GeneralName.iPAddress, ip);
                inputCount++;
            }

            GeneralNames generalNames = new GeneralNames(generalNameArr);

            certBldr.addExtension(Extension.subjectAlternativeName, false, generalNames);
        }

        ContentSigner signer = new JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(caKey);

        return certBldr.build(signer);
    }

    /**
     * Generate a X500PrivateCredential for the root entity.
     */
    public static X500PrivateCredential createRootCredential() throws Exception {
        return createRootCredential(false);
    }

    /**
     * Generate a X500PrivateCredential for the root entity.
     */
    public static X500PrivateCredential createRootCredential(boolean isNew) throws Exception {
        KeyPair rootPair = null;
        if (isNew) {
            rootPair = generateRSAKeyPair();
        }
        else {
            rootPair = generateRootKeyPair();
        }

        X509Certificate rootCert = convertCert(generateRootCert(rootPair, isNew));

        return new X500PrivateCredential(rootCert, rootPair.getPrivate(), ROOT_ALIAS);
    }

    /**
     * Generate a X500PrivateCredential for the root entity.
     */
    public static X500PrivateCredential createRootCredential(String publicKey, String privateKey) throws Exception {
        KeyPair rootPair = generateRootKeyPair(publicKey, privateKey);

        X509Certificate rootCert = convertCert(generateRootCert(rootPair, false));

        return new X500PrivateCredential(rootCert, rootPair.getPrivate(), ROOT_ALIAS);
    }

    /**
     * Generate a X500PrivateCredential for the intermediate entity.
     */
    public static X500PrivateCredential createIntermediateCredential(PrivateKey caKey,
                                                                     X509Certificate caCert)
        throws Exception {
        KeyPair interPair = generateRSAKeyPair();
        X509Certificate interCert = convertCert(generateIntermediateCert(interPair.getPublic(), caKey, caCert));

        return new X500PrivateCredential(interCert, interPair.getPrivate(), INTERMEDIATE_ALIAS);
    }

    /**
     * Generate a X500PrivateCredential for the end entity.
     */
    public static X500PrivateCredential createEndEntityCredential(PrivateKey caKey, X509Certificate caCert, String domain, String ipAddress)
        throws Exception {
        List<String> domains = new ArrayList<>();
        if(StringUtils.isNotBlank(domain)) {
            domains.add(domain);
        }
        List<String> ips = new ArrayList<>();
        if(StringUtils.isNotBlank(ipAddress)) {
            ips.add(ipAddress);
        }

        return createEndEntityCredential(caKey, caCert, domains, ips);
    }

    /**
     * Generate a X500PrivateCredential for the end entity.
     */
    public static X500PrivateCredential createEndEntityCredential(PrivateKey caKey, X509Certificate caCert, List<String> domains, List<String> ipAddress)
        throws Exception {
        KeyPair endPair = generateRSAKeyPair();
        X509Certificate endCert = convertCert(generateEndEntityCert(endPair.getPublic(), caKey, caCert, domains, ipAddress));

        return new X500PrivateCredential(endCert, endPair.getPrivate(), END_ENTITY_ALIAS);
    }

    /**
     * get Certificate
     *
     * @param privateKey
     * @param certificate
     * @return
     * @throws Exception
     */
    public static CertificateVO generatePemCertificate(PrivateKey privateKey, X509Certificate certificate) throws Exception {
        RSAPrivateKey priv = (RSAPrivateKey) privateKey;
        RSAPublicKey pub = (RSAPublicKey) certificate.getPublicKey();

        return generatePemCertificate(priv.getEncoded(), pub.getEncoded(), certificate.getEncoded());
    }

    /**
     * get Certificate
     *
     * @param privateKey
     * @param certificate
     * @return
     * @throws Exception
     */
    public static CertificateVO generatePemCertificate(PrivateKey privateKey, PublicKey publicKey, byte[] certificate) throws Exception {
        RSAPrivateKey priv = (RSAPrivateKey) privateKey;
        RSAPublicKey pub = (RSAPublicKey) publicKey;

        return generatePemCertificate(priv.getEncoded(), pub.getEncoded(), certificate);
    }

    /**
     * get Certificate
     *
     * @param privateKey
     * @param publicKey
     * @param certificate
     * @return
     * @throws Exception
     */
    public static CertificateVO generatePemCertificate(byte[] privateKey, byte[] publicKey, byte[] certificate) throws Exception {
        CertificateVO certificateVO = new CertificateVO();

        certificateVO.setPrivateKey(generatePemCertificateContent(privateKey, "RSA PRIVATE KEY"));
        certificateVO.setPublicKey(generatePemCertificateContent(publicKey, "RSA PUBLIC KEY"));
        certificateVO.setCertificate(generatePemCertificateContent(certificate, "CERTIFICATE"));

        return certificateVO;
    }

    /**
     * get Certificate Content
     *
     * @param encoded
     * @param type
     * @return
     * @throws Exception
     */
    private static String generatePemCertificateContent(byte[] encoded, String type) throws IOException {
        String content = "";
        PemObject pemObject = new PemObject(type, encoded);

        StringWriter writer = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        try {
            pemWriter.writeObject(pemObject);
            pemWriter.flush();
            content = writer.toString();

        } catch (IOException ioe) {
            throw ioe;
        } finally {
            pemWriter.close();
            writer.close();
        }

        return content;
    }

}
