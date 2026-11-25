package com.example.model;

import java.util.List;

public class AuthModels {

    public static class LoginRequest {
        public String identifier;
        public String password;
        public String fcmToken;

        public LoginRequest(String identifier, String password, String fcmToken) {
            this.identifier = identifier;
            this.password = password;
            this.fcmToken = fcmToken;
        }
    }

    public static class RegisterCustomerRequest {
        public String fullName;
        public String mobile;
        public String email;
        public String password;
        public String address;
        public String profilePhotoUrl;
        public Double latitude;
        public Double longitude;
    }

    public static class RegisterWorkerRequest {
        public String fullName;
        public String mobile;
        public String email;
        public String password;
        public String address;
        public String profilePhotoUrl;
        public int experienceYears;
        public List<Long> serviceIds;
        public String aadhaarNumber;
        public String aadhaarDocUrl;
        public String otherDocUrl;
        public String bankAccountNumber;
        public String ifscCode;
        public String upiId;
    }

    public static class AuthData {
        public String token;
        public long userId;
        public String fullName;
        public String email;
        public String mobile;
        public String role;
        public String profilePhotoUrl;
        public String verificationStatus;
    }
}
