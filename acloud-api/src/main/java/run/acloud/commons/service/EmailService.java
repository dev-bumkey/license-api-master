package run.acloud.commons.service;

import com.google.common.collect.Lists;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailEmailProperties;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 6.
 */
@Slf4j
@Service
public class EmailService {

    @Autowired
    private CocktailEmailProperties cocktailEmailProperties;

    public class MailAuth extends Authenticator {

        PasswordAuthentication pa;

        public MailAuth() {
            String mail_id = cocktailEmailProperties.getMailSmtpId();
            String mail_pw = cocktailEmailProperties.getMailSmtpPw();

            pa = new PasswordAuthentication(mail_id, mail_pw);
        }

        public PasswordAuthentication getPasswordAuthentication() {
            return pa;
        }
    }

    public void sendMail(String recipient, String title, String body, boolean istext) throws CocktailException {
        this.sendMail(Collections.singletonList(recipient), title, body, istext);
    }

    public void sendMail(List<String> recipients, String title, String body, boolean istext) throws CocktailException {

        if (this.availableEmail()) {
            Properties prop = new Properties();

            // 로그인시 TLS를 사용할 것인지 설정
            prop.put("mail.smtp.starttls.enable", cocktailEmailProperties.getMailSmtpStarttlsEnable());

            // 이메일 발송을 처리해줄 SMTP서버
            prop.put("mail.smtp.host", cocktailEmailProperties.getMailSmtpHost());

            // SMTP 서버의 인증을 사용한다는 의미
            prop.put("mail.smtp.auth", cocktailEmailProperties.getMailSmtpAuth());

            // TLS의 포트번호는 587이며 SSL의 포트번호는 465이다.
            prop.put("mail.smtp.port", cocktailEmailProperties.getMailSmtpPort());

            // connect timeout (millisecond)
            prop.put("mail.smtp.connectiontimeout", cocktailEmailProperties.getMailSmtpConnectTimeout()); //10초

            prop.put("mail.smtps.localhost", cocktailEmailProperties.getMailSmtpLocalhost());
            prop.put("mail.transport.protocol", cocktailEmailProperties.getMailTransportProtocol());
            prop.put("mail.debug", cocktailEmailProperties.getMailDebug());


            Authenticator auth = new MailAuth();

            Session session = Session.getDefaultInstance(prop, auth);
            session.setDebug(BooleanUtils.toBoolean(cocktailEmailProperties.getMailDebug()));

            MimeMessage msg = new MimeMessage(session);

            try {
                // 보내는 날짜 지정
                msg.setSentDate(new Date());

                // 발송자를 지정한다. 발송자의 메일, 발송자명
                msg.setFrom(new InternetAddress(cocktailEmailProperties.getMailSmtpId(), cocktailEmailProperties.getMailSmtpFromName()));

                // Message 클래스의 setRecipient() 메소드를 사용하여 수신자를 설정한다. setRecipient() 메소드로 수신자, 참조,
                // 숨은 참조 설정이 가능하다.
                // Message.RecipientType.TO : 받는 사람
                // Message.RecipientType.CC : 참조
                // Message.RecipientType.BCC : 숨은 참조
                if (CollectionUtils.isNotEmpty(recipients)) {
                    msg.addRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients.stream().collect(Collectors.joining(","))));
                }

                // 메일의 제목 지정
                msg.setSubject(title, "UTF-8");

                // Transport는 메일을 최종적으로 보내는 클래스로 메일을 보내는 부분이다.
                MimeBodyPart mbp = new MimeBodyPart();
                if (istext) {
                    mbp.setText(body, "UTF-8");
                } else {
                    mbp.setText(body,"UTF-8","html"); // HTML 형식
                }
                // create the Multipart and its parts to it
                Multipart mp = new MimeMultipart();
                mp.addBodyPart(mbp);
                // add the Multipart to the message
                msg.setContent(mp);

                Transport.send(msg);

            } catch (UnsupportedEncodingException | MessagingException e) {
                this.exceptionHandle(e);
            } catch (IOException e) {
                this.exceptionHandle(e);
            }
        } else {
            throw new CocktailException("Invalid mail prop info.", ExceptionType.SendMailFail);
        }
    }

    public String getMailFormFile(String mailFormPath) throws Exception {
        String templateStr = null;
        if (StringUtils.isNotBlank(mailFormPath)) {
            // get file
            Path sourceFilePath = Paths.get(mailFormPath);
            if (sourceFilePath != null && Files.exists(sourceFilePath) && Files.isReadable(sourceFilePath)) {
                // read file
                templateStr = new String(Files.readAllBytes(sourceFilePath));
            } else {
                throw new CocktailException("Invalid mail form filepath.", ExceptionType.SendMailFail);
            }
        } else {
            throw new CocktailException("mail form filepath empty.", ExceptionType.SendMailFail);
        }

        return templateStr;
    }

    public String genMailFormWithFile(String mailFormPath, Object dataVo, List<String> ignoreFields) throws Exception {
        try {
            // get file
            String templateStr = this.getMailFormFile(mailFormPath);

            // gen mail form
            templateStr = this.replaceMailFormWithObj(templateStr, dataVo, ignoreFields);

            return templateStr;
        } catch (IOException e) {
            throw new CocktailException("genMailForm fail.", e, ExceptionType.SendMailFail);
        } catch (SecurityException e) {
            throw new CocktailException("genMailForm fail.", e, ExceptionType.SendMailFail);
        } catch (IllegalAccessException e) {
            throw new CocktailException("genMailForm fail.", e, ExceptionType.SendMailFail);
        } catch (IllegalArgumentException e) {
            throw new CocktailException("genMailForm fail.", e, ExceptionType.SendMailFail);
        } catch (InvocationTargetException e) {
            throw new CocktailException("genMailForm fail.", e, ExceptionType.SendMailFail);
        } catch (CocktailException e) {
            throw e;
        } catch (Exception e) {
            throw new CocktailException("genMailForm fail.", e, ExceptionType.SendMailFail);
        }
    }

    public String replaceMailFormWithObj(String templateStr, Object dataVo, List<String> ignoreFields) throws Exception {
        try {
            if (StringUtils.isNotBlank(templateStr)) {
                // 제외할 필드
                if (CollectionUtils.isEmpty(ignoreFields)) {
                    ignoreFields = Lists.newArrayList();
                }

                Class clazz = dataVo.getClass();
                if (clazz != null) {
                    Field[] fields = clazz.getDeclaredFields();
                    if (ArrayUtils.isNotEmpty(fields)) {
                        for (Field fieldRow : fields) {
                            if (!ignoreFields.contains(fieldRow.getName())) {
                                // find getter method
                                Method getter = ReflectionUtils.findMethod(clazz, String.format("get%s", StringUtils.capitalize(fieldRow.getName())));
                                if (getter != null) {
                                    // get getter method return type;
                                    Class getterReturnClazz = getter.getReturnType();

                                    if (getterReturnClazz != null) {
                                        // 우선, DataType이 String, Integer만 지원
                                        if (StringUtils.equalsIgnoreCase(getterReturnClazz.getSimpleName(), "List")) {

                                            List<Object> returnObjects = (List<Object>) getter.invoke(dataVo, (Object[]) null);

                                        } else if (StringUtils.equalsIgnoreCase(getterReturnClazz.getSimpleName(), "String")) {

                                            Object returnObject = getter.invoke(dataVo, (Object[]) null);
                                            String returnValue = (String) Optional.ofNullable(returnObject).orElseGet(() ->"");
                                            templateStr = StringUtils.replaceAll(templateStr, String.format("#\\{%s\\}", fieldRow.getName()), returnValue);

                                        } else if (StringUtils.equalsIgnoreCase(getterReturnClazz.getSimpleName(), "Integer")) {

                                            Object returnObject = getter.invoke(dataVo, (Object[]) null);
                                            Integer returnValue = (Integer) Optional.ofNullable(returnObject).orElseGet(() ->0);
                                            templateStr = StringUtils.replaceAll(templateStr, String.format("#\\{%s\\}", fieldRow.getName()), returnValue.toString());

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                throw new CocktailException("mail form filepath empty.", ExceptionType.SendMailFail);
            }

            return templateStr;
        } catch (SecurityException e) {
            throw new CocktailException("genMailForm fail.", e, ExceptionType.SendMailFail);
        } catch (IllegalAccessException e) {
            throw new CocktailException("genMailForm fail.", e, ExceptionType.SendMailFail);
        } catch (IllegalArgumentException e) {
            throw new CocktailException("genMailForm fail.", e, ExceptionType.SendMailFail);
        } catch (InvocationTargetException e) {
            throw new CocktailException("genMailForm fail.", e, ExceptionType.SendMailFail);
        } catch (CocktailException e) {
            throw e;
        } catch (Exception e) {
            throw new CocktailException("genMailForm fail.", e, ExceptionType.SendMailFail);
        }
    }

    public String replaceMailForm(String templateStr, String srcValiableName, String destValiableValue) {
        if (StringUtils.isNotBlank(templateStr)) {
            templateStr = StringUtils.replaceAll(templateStr, String.format("#\\{%s\\}", srcValiableName), destValiableValue);
        }

        return templateStr;
    }

    public boolean availableEmail() {
        return StringUtils.isNotBlank(cocktailEmailProperties.getMailSmtpId()) && StringUtils.isNotBlank(cocktailEmailProperties.getMailSmtpPw());
    }

    public void exceptionHandle(Exception e) throws CocktailException{
        this.exceptionHandle(e, true);
    }

    public void exceptionHandle(Exception e, boolean isThrow) throws CocktailException{

        String errMsg = "";
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause != null) {
            errMsg = rootCause.getMessage();
        }
        if (StringUtils.isBlank(errMsg)) {
            for(Throwable throwableRow : ExceptionUtils.getThrowableList(e)){
                errMsg = throwableRow.getMessage();
                break;
            }
        }

        CocktailException ce = new CocktailException(e.getMessage(), e, ExceptionType.SendMailFail, errMsg);

        if(isThrow){
            throw ce;
        } else {
            log.debug(ce.getMessage(), ce);
        }
    }
}
