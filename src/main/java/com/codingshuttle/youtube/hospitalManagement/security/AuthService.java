//package com.codingshuttle.youtube.hospitalManagement.security;
//
//import com.codingshuttle.youtube.hospitalManagement.dto.LoginRequestDto;
//import com.codingshuttle.youtube.hospitalManagement.dto.LoginResponseDto;
//import com.codingshuttle.youtube.hospitalManagement.dto.SignupResponseDto;
//import com.codingshuttle.youtube.hospitalManagement.entity.User;
//import com.codingshuttle.youtube.hospitalManagement.entity.type.AuthProviderType;
//import com.codingshuttle.youtube.hospitalManagement.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class AuthService {
//
//    private final AuthenticationManager authenticationManager;
//    private final AuthUtil authUtil;
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
//
//        Authentication authentication = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(
//                        loginRequestDto.getUsername(),
//                        loginRequestDto.getPassword()
//                )
//        );
//
//        User user = (User) authentication.getPrincipal();
//        String token = authUtil.generateAccessToken(user);
//
//        return new LoginResponseDto(token, user.getId());
//    }
//
//    public SignupResponseDto signup(LoginRequestDto signupRequestDto) {
//
//        User user=signUpInteral(signupRequestDto,AuthProviderType.EMAIL,null);
//
//        return new SignupResponseDto(user.getId(), user.getUsername());
//    }
//
//    public User signUpInteral(LoginRequestDto signupRequestDto,AuthProviderType authProviderType,String providerId) {
//        User user = userRepository.findByUsername(signupRequestDto.getUsername()).orElse(null);
//
//        if (user != null) {
//            throw new IllegalArgumentException("User already exists");
//        }
//
//         user= User.builder()
//                .username(signupRequestDto.getUsername())
////                .password(passwordEncoder.encode(signupRequestDto.getPassword()))
////                        .role("ROLE_PATIENT")  // optional default role
//                 .providerId(providerId)
//                 .providerType(authProviderType)
//                 .build();
//         if(authProviderType==AuthProviderType.EMAIL){
//             user.setPassword(passwordEncoder.encode(signupRequestDto.getPassword()));
//         }
//
//         return  userRepository.save(user);
//    }
//
//    public ResponseEntity<LoginResponseDto> handleOAuth2LoginRequest(OAuth2User oAuth2User, String registrationId) {
//
//        AuthProviderType providerType=authUtil.getProviderTypeFromRegistrationId(registrationId);
//        String providerId= authUtil.determineProviderIdFromOAuth2User(oAuth2User,registrationId);
//
//        User user=userRepository.findByProviderIdAndProviderType(providerId,providerType).orElseThrow(() -> new RuntimeException("User not found for providerId: " + providerId + " and providerType: " + providerType));
//
//        String email=oAuth2User.getAttribute("email");
//
//        User emailUser=userRepository.findByUsername(email).orElse(null);
//
//        if(user==null && emailUser == null){
//            //signup flow:
//            String username= authUtil.determineUsernameFromOAuth2User(oAuth2User,registrationId,providerId);
//             user=signUpInteral(new LoginRequestDto(username,null),providerType,providerId);
//        }else if(user !=  null){
//            if(email !=null && !email.isBlank() && !email.equals(user.getUsername())){
//                user.setUsername(email);
//                userRepository.save(user);
//            }
//        }else {
//            throw new BadCredentialsException("This email is already registered with provider "+email);
//        }
//
//        LoginResponseDto loginResponseDto=new LoginResponseDto(authUtil.generateAccessToken(user) ,user.getId());
//
//        return ResponseEntity.ok(loginResponseDto);
//        //fetch proverId and providerType
//        //save providerID providerType with user
//        // if user has an account : directly login
//
//
//        //otherwise first signup and login
//
//    }
//}


package com.codingshuttle.youtube.hospitalManagement.security;

import com.codingshuttle.youtube.hospitalManagement.dto.LoginRequestDto;
import com.codingshuttle.youtube.hospitalManagement.dto.LoginResponseDto;
import com.codingshuttle.youtube.hospitalManagement.dto.SignupRequestDto;
import com.codingshuttle.youtube.hospitalManagement.dto.SignupResponseDto;
import com.codingshuttle.youtube.hospitalManagement.entity.Patient;
import com.codingshuttle.youtube.hospitalManagement.entity.User;
import com.codingshuttle.youtube.hospitalManagement.entity.type.AuthProviderType;
import com.codingshuttle.youtube.hospitalManagement.entity.type.RoleType;
import com.codingshuttle.youtube.hospitalManagement.repository.PatientRepository;
import com.codingshuttle.youtube.hospitalManagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatientRepository patientRepository;

    // ============ EMAIL LOGIN ============
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDto.getUsername(),
                        loginRequestDto.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();
        String token = authUtil.generateAccessToken(user);
        return new LoginResponseDto(token, user.getId());
    }

    // ============ EMAIL SIGNUP ============
    public SignupResponseDto signup(SignupRequestDto signupRequestDto) {
        User user = signUpInternal(signupRequestDto, AuthProviderType.EMAIL, null);
        return new SignupResponseDto(user.getId(), user.getUsername());
    }

    // ============ COMMON SIGNUP LOGIC ============
    public User signUpInternal(SignupRequestDto signupRequestDto, AuthProviderType authProviderType, String providerId) {
        User existingUser = userRepository.findByUsername(signupRequestDto.getUsername()).orElse(null);

        if (existingUser != null) {
            throw new IllegalArgumentException("User already exists");
        }

        User user = User.builder()
                .username(signupRequestDto.getUsername())
                .providerId(providerId)
                .providerType(authProviderType)
                .roles(signupRequestDto.getRoles())
                .build();

        if (authProviderType == AuthProviderType.EMAIL) {
            user.setPassword(passwordEncoder.encode(signupRequestDto.getPassword()));
        }

         user=userRepository.save(user);
        Patient patient= Patient.builder()
                .name(signupRequestDto.getName())
                .email(signupRequestDto.getUsername())
                .user(user)
                .build();
        patientRepository.save(patient);

        return user;
    }

    // ============ OAUTH2 LOGIN HANDLER ============
    public ResponseEntity<LoginResponseDto> handleOAuth2LoginRequest(OAuth2User oAuth2User, String registrationId) {
        AuthProviderType providerType = authUtil.getProviderTypeFromRegistrationId(registrationId);
        String providerId = authUtil.determineProviderIdFromOAuth2User(oAuth2User, registrationId);
        String email = oAuth2User.getAttribute("email");
        String name= oAuth2User.getAttribute("name");

        // Try finding by provider first
        User user = userRepository.findByProviderIdAndProviderType(providerId, providerType).orElse(null);

        // Check if email-based user exists
        User emailUser = (email != null) ? userRepository.findByUsername(email).orElse(null) : null;

        if (user == null && emailUser == null) {
            // ✅ New OAuth2 user -> Signup
            String username = authUtil.determineUsernameFromOAuth2User(oAuth2User, registrationId, providerId);
            user = signUpInternal(new SignupRequestDto(username, null,name ,Set.of(RoleType.PATIENT)), providerType, providerId);
        } else if (user != null) {
            // ✅ Existing OAuth2 user -> Update username if email changed
            if (email != null && !email.isBlank() && !email.equals(user.getUsername())) {
                user.setUsername(email);
                userRepository.save(user);
            }
        } else {
            // ❌ Same email registered with different provider
            throw new BadCredentialsException("This email is already registered with another provider: " + email);
        }

        // Generate JWT
        String token = authUtil.generateAccessToken(user);
        LoginResponseDto loginResponseDto = new LoginResponseDto(token, user.getId());

        return ResponseEntity.ok(loginResponseDto);
    }
}

