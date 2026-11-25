// src/aws-exports.js
const awsconfig = {
  Auth: {
    Cognito: {
      userPoolId: 'us-east-1_EA0XQzQRL',
      userPoolClientId: '4glot1q5anvvk3lbc1od6i6d7',
      signUpVerificationMethod: 'code',
      loginWith: {
        email: true,
        username: true
      }
    }
  }
};

export default awsconfig;