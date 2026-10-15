// src/aws-exports.js
const awsconfig = {
  Auth: {
    Cognito: {
      userPoolId: 'us-east-1_dzuH5paum',
      userPoolClientId: '59ukm8nie3gsecfi5e8dshhmm6',
      signUpVerificationMethod: 'code',
      loginWith: {
        email: true,
        username: true
      }
    }
  }
};

export default awsconfig;