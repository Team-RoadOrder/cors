package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.EmailTokenEntity;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.enums.users.Gender;
import dev.gmpark.cors.exceptions.TransactionalException;
import dev.gmpark.cors.mappers.EmailTokenMapper;
import dev.gmpark.cors.mappers.RegisterMapper;
import dev.gmpark.cors.results.register.RegisterResult;
import dev.gmpark.cors.results.register.SendEmailResult;
import dev.gmpark.cors.results.register.VerifyEmailResult;
import dev.gmpark.cors.validators.EmailTokenValidator;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Service
public class RegisterService {
    private final RegisterMapper registerMapper;
    private final JavaMailSender mailSender;    // 메일 발송을 위한 의존성
    private final SpringTemplateEngine templateEngine; //HTML 파일을 템플릿 엔진으로 해석해서 문자열로 반환받기 위한 의존성
    private final EmailTokenMapper emailTokenMapper;
    @Value("${custom.property.kakao-client-id}")
    private String kakaoClientId;
    @Value("${custom.property.naver-client-id}")
    private String naverClientId;
    @Value("${custom.property.naver-client-secret}")
    private String naverClientSecret;
    @Autowired
    public RegisterService(RegisterMapper registerMapper, JavaMailSender mailSender, SpringTemplateEngine templateEngine, EmailTokenMapper emailTokenMapper) {
        this.registerMapper = registerMapper;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailTokenMapper = emailTokenMapper;
    }

     @Transactional
     public RegisterResult register(RegisterEntity register, EmailTokenEntity emailToken) {
         boolean isSocialRegister = register.getSocialTypeCode() != null && register.getSocialId() != null;
         if (register == null || register.getName() == null || register.getName().isEmpty()
                 || register.getEmail() == null || register.getEmail().isEmpty()
                 // || register.getPassword() == null ... (비밀번호 검사는 아래에서 별도로 함)
                 || register.getUsertype() == null || register.getUsertype().isEmpty()
                 || register.getPhone() == null || register.getPhone().isEmpty()
                 || register.getAddress() == null || register.getAddress().isEmpty()
                 || register.getAddressDetail() == null || register.getAddressDetail().isEmpty()) {
             return RegisterResult.FAILURE;
         }
         if (isSocialRegister) {
             // 소셜 가입이면: 비밀번호를 입력받지 않았으므로 랜덤한 값으로 설정 (DB NOT NULL 제약 조건 해결)
             // 사용자는 이 비밀번호를 모르므로 이메일 로그인 시도는 실패하게 됨 (보안상 안전)
             register.setPassword(UUID.randomUUID().toString());
         } else {
             // 일반 가입이면: 비밀번호 필수 체크
             if (register.getPassword() == null || register.getPassword().isEmpty()) {
                 return RegisterResult.FAILURE;
             }
         }
         if( emailToken == null ||
                 !EmailTokenValidator.validateEmail(emailToken.getEmail()) ||
                 !EmailTokenValidator.validateCode(emailToken.getCode()) ||
                 !EmailTokenValidator.validateSalt(emailToken.getSalt()))  {
             return RegisterResult.FAILURE;
         }
         EmailTokenEntity dbEmailToken = this.emailTokenMapper.select(
                 emailToken.getEmail(),
                 emailToken.getCode(),
                 emailToken.getSalt()
         );
         // 비밀번호 암호화시키기
         BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
         String rawPassword = register.getPassword();
         String hashedPassword = encoder.encode(rawPassword);
         register.setPassword(hashedPassword);

         if( dbEmailToken == null || !dbEmailToken.isVerified() || dbEmailToken.isUsed()) {
             return RegisterResult.FAILURE;
         }
         dbEmailToken.setUsed(true);

         if(this.emailTokenMapper.update(dbEmailToken) < 1) {
             throw new TransactionalException(RegisterResult.FAILURE);
         }
         RegisterEntity dbEmail = this.registerMapper.selectByEmail(register.getEmail());
            if(dbEmail != null){
                return RegisterResult.FAILURE_EMAIL_DUPLICATE;
            }
         if (register.getUsertype().equals("customer")) {
             register.setLevel(0);
             if (Arrays.stream(Gender.values()).map(x -> x.code).noneMatch(x -> x.equals(register.getGender()))) {
                 return RegisterResult.FAILURE;   // 무슨 실패인지 로직 확장해서 작성하기
             }
         } else if (register.getUsertype().equals("owner")) {
             register.setLevel(3);
             if (register.getStoreName() == null || register.getStoreName().isEmpty()
                     || register.getBusinessNum() == null || register.getBusinessNum().isEmpty()) {
                 return RegisterResult.FAILURE;    // 마찬가지로 확장하기
             }
         } else {
             return RegisterResult.FAILURE;
         }
         return  this.registerMapper.insertRegister(register) < 1 ? RegisterResult.FAILURE : RegisterResult.SUCCESS;
     }
    public Pair<SendEmailResult,EmailTokenEntity> sendEmail(String email, int type) throws MessagingException {
        if( !EmailTokenValidator.validateEmail(email) ) {
            return Pair.of(SendEmailResult.FAILURE, null);
        }
        String code = RandomStringUtils.randomNumeric(6); // "000000" ~ "999999"
        String salt = new BCryptPasswordEncoder().encode(String.format("%s%s%f%f", email, code, Math.random(), Math.random())); // 아무거나 섞으면됨 $2a$~~~
        EmailTokenEntity emailToken = new EmailTokenEntity();
        emailToken.setEmail(email);
        emailToken.setCode(code);
        emailToken.setSalt(salt);
        emailToken.setVerified(false);
        emailToken.setUsed(false);
        emailToken.setCreatedAt(LocalDateTime.now());
        emailToken.setExpiresAt(LocalDateTime.now().plusMinutes(3L)); // 3분으로 변경
        int insertResult = this.emailTokenMapper.insert(emailToken);
        if(insertResult<1){
            return Pair.of(SendEmailResult.FAILURE, null);
        }
        Context context = new Context();
        context.setVariable("code", code);
        String body = this.templateEngine.process("register/sendEmail_0", context);
        MimeMessage message = this.mailSender.createMimeMessage();

        MimeMessageHelper messageHelper = new MimeMessageHelper(message);
        messageHelper.setFrom("krumin8384@gmail.com");
        messageHelper.setTo(email);
        messageHelper.setSubject("[CORS] 회원가입 인증번호 안내");
        messageHelper.setText(body,true);
        this.mailSender.send(message);
        return Pair.of(SendEmailResult.SUCCESS, emailToken);
    }
    public VerifyEmailResult verifyEmail(EmailTokenEntity emailToken) {
        if( emailToken == null ||
                !EmailTokenValidator.validateEmail(emailToken.getEmail()) ||
                !EmailTokenValidator.validateCode(emailToken.getCode()) ||
                !EmailTokenValidator.validateSalt(emailToken.getSalt()))  {
            return VerifyEmailResult.FAILURE;
        }
        EmailTokenEntity dbEmailToken = this.emailTokenMapper.select(
                emailToken.getEmail(),
                emailToken.getCode(),
                emailToken.getSalt()
        );
        if( dbEmailToken == null ||dbEmailToken.isVerified() || dbEmailToken.isUsed()) {
            return VerifyEmailResult.FAILURE;
        }
        if(LocalDateTime.now().isAfter(dbEmailToken.getExpiresAt())){
            return VerifyEmailResult.FAILURE_EXPIRED;
        }
        dbEmailToken.setVerified(true);
        return this.emailTokenMapper.update(dbEmailToken) > 0
                ?  VerifyEmailResult.SUCCESS : VerifyEmailResult.FAILURE;
    }

}
