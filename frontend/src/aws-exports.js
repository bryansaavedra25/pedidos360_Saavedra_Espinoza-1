// src/aws-exports.js
const awsconfig = {
  Auth: {
    Cognito: {
      userPoolId: 'us-east-1_J90tcuGXk',
      userPoolClientId: '380gg36v8lfp7a2e19tsj61giu',
      signUpVerificationMethod: 'code',
      loginWith: {
        email: true,
        username: true
      }
    }
  }
};

export default awsconfig;