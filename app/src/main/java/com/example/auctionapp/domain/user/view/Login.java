package com.example.auctionapp.domain.user.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.auctionapp.MainActivity;
import com.example.auctionapp.domain.user.constant.Constants;
import com.example.auctionapp.domain.user.dto.LoginRequest;
import com.example.auctionapp.domain.user.dto.LoginResponse;
import com.example.auctionapp.domain.user.dto.OAuth2KakaoLoginRequest;
import com.example.auctionapp.domain.user.dto.OAuth2NaverLoginRequest;
import com.example.auctionapp.global.retrofit.MainRetrofitCallback;
import com.example.auctionapp.global.retrofit.MainRetrofitTool;
import com.example.auctionapp.domain.user.dto.OAuth2GoogleLoginRequest;
import com.example.auctionapp.R;
import com.example.auctionapp.global.retrofit.RetrofitTool;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.kakao.auth.AuthType;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.sdk.auth.AuthApiClient;
import com.kakao.sdk.common.KakaoSdk;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.kakao.usermgmt.response.model.Profile;
import com.kakao.usermgmt.response.model.UserAccount;
import com.kakao.util.OptionalBoolean;
import com.kakao.util.exception.KakaoException;

import static android.content.ContentValues.TAG;

import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;

import retrofit2.Response;

public class Login extends AppCompatActivity {

    ImageView loginBtn;
    ImageView btn_kakao_login;
    ImageView btn_google_login;
    ImageView btn_naver_login;
    EditText edit_id;
    EditText edit_pw;
    TextView signUp_tv;

    private SessionCallback sessionCallback = new SessionCallback();
    Session session;
    GoogleSignInClient mGoogleSignInClient;
    private int RC_SIGN_IN = 0116;  //google login request code
    OAuthLogin mOAuthLoginModule;
    Context mContext;

    //private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        KakaoSdk.init(this, getString(R.string.kakao_app_key));
        LinearLayout goSignUp = (LinearLayout) findViewById(R.id.goSignUp);
        goSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Login.this, SignUp.class);
                startActivity(intent);
            }
        });
        edit_id = findViewById(R.id.editID);
        edit_pw = findViewById(R.id.editPW);


        // App ?????????
        loginBtn = (ImageView)findViewById(R.id.loginButton);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginRequest loginRequest = LoginRequest.of(edit_id.getText().toString(), edit_pw.getText().toString());
                RetrofitTool.getAPIWithNullConverter().login(loginRequest)
                        .enqueue(MainRetrofitTool.getCallback(new LoginCallback()));
                Intent intent = new Intent(Login.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });


        //????????? ?????????
        btn_kakao_login = (ImageView)findViewById(R.id.btn_kakao_login);
        btn_kakao_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //kakao login
                session = Session.getCurrentSession();
                session.addCallback(sessionCallback);
                session.open(AuthType.KAKAO_LOGIN_ALL, Login.this);
            }
        });
        //?????? ?????????
        btn_google_login = (ImageView) findViewById(R.id.btn_google_login);
        btn_google_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ?????? ????????? ????????? ???????????? ??????????????? ????????? ????????? ????????????.
                // DEFAULT_SIGN_IN parameter??? ????????? ID??? ???????????? ????????? ????????? ??????????????? ????????????.
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken("221537301769-e1qd8130nulhheiqo68nv8upistikcp4.apps.googleusercontent.com")
                        .requestEmail() // email addresses??? ?????????
                        .build();

                // ????????? ?????? GoogleSignInOptions??? ????????? GoogleSignInClient ????????? ??????
                mGoogleSignInClient = GoogleSignIn.getClient(Login.this, gso);

                // ????????? ????????? ?????? ????????? ????????????.
                GoogleSignInAccount gsa = GoogleSignIn.getLastSignedInAccount(Login.this);
                // ????????? ???????????? ??????
                if (gsa != null) {
                    String idToken = gsa.getIdToken();
                    OAuth2GoogleLoginRequest oAuth2GoogleLoginRequest = new OAuth2GoogleLoginRequest(idToken);
                    RetrofitTool.getAPIWithNullConverter().googleIdTokenValidation(oAuth2GoogleLoginRequest)
                            .enqueue(MainRetrofitTool.getCallback(new LoginCallback()));
                    System.out.println("userId"+Constants.userId);
                    Intent intent = new Intent(Login.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                } else
                    googleSignIn();
            }
        });
        //????????? ?????????
        btn_naver_login = (ImageView)findViewById(R.id.btn_naver_login);
        mContext = getApplicationContext();
        btn_naver_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                naverSignIn();
            }
        });

        // ??? ????????????
        signUp_tv = (TextView) findViewById(R.id.signUp_tv);
        signUp_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Login.this, SignUp.class);
                startActivity(intent);
            }
        });

    }
    private class LoginCallback implements MainRetrofitCallback<LoginResponse> {
        @Override
        public void onSuccessResponse(Response<LoginResponse> response) {
            Constants.userId = response.body().getUserId();
            Constants.token = response.body().getToken();
            Log.d(TAG, "retrofit success, idToken: " + response.body().toString());

        }
        @Override
        public void onFailResponse(Response<LoginResponse> response) {
            Log.d(TAG, "onFailResponse");
        }
        @Override
        public void onConnectionFail(Throwable t) {
            Log.e("????????????", t.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ?????? ?????? ??????
        Session.getCurrentSession().removeCallback(sessionCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // ????????????|????????? ??????????????? ?????? ????????? ????????? SDK??? ??????
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        // google
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
        if (requestCode == 12501) { return; }   // google account ?????? ??? ?????? ???

        super.onActivityResult(requestCode, resultCode, data);
    }

    // google login method
    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // google login method
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount acct = completedTask.getResult(ApiException.class);

            if (acct != null) {
                String personName = acct.getDisplayName();
                String personGivenName = acct.getGivenName();
                String personFamilyName = acct.getFamilyName();
                String personEmail = acct.getEmail();
                String personId = acct.getId();
                Uri personPhoto = acct.getPhotoUrl();
                String idToken = acct.getIdToken();

                Log.d(TAG, "handleSignInResult:personName "+personName);
                Log.d(TAG, "handleSignInResult:personGivenName "+personGivenName);
                Log.d(TAG, "handleSignInResult:personEmail "+personEmail);
                Log.d(TAG, "handleSignInResult:personId "+personId);
                Log.d(TAG, "handleSignInResult:personFamilyName "+personFamilyName);
                Log.d(TAG, "handleSignInResult:personPhoto "+personPhoto);
                Log.d(TAG, "handleSignInResult:idToken "+idToken);

                /* ---------retrofit ??????---------
                Retrofit googleRetrofit = new Retrofit.Builder()
                        .baseUrl("http://10.0.2.2:8000")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                RestAPI service1 = googleRetrofit.create(RestAPI.class);
                Call<RetrofitClass> call = service1.getIDtoken();
                call.enqueue(new Callback<RetrofitClass>() {
                    @Override
                    public void onResponse(Call<RetrofitClass> call, Response<RetrofitClass> response) {
                        if(response.isSuccessful()) {
                            //??????????????? ????????? ????????? ??????
                            RetrofitClass idToken = response.body();
                            Log.d(TAG, "onResponse: ??????, idToken: " + idToken.toString());
                        }else {
                            Log.d(TAG, "onResponse: ??????");
                        }
                    }

                    @Override
                    public void onFailure(Call<RetrofitClass> call, Throwable t) {
                        Log.d(TAG, "onFailure: " + t.getMessage());
                    }
                });
                 ------------------------------- */
                OAuth2GoogleLoginRequest oAuth2GoogleLoginRequest = new OAuth2GoogleLoginRequest(idToken);
                RetrofitTool.getAPIWithNullConverter().googleIdTokenValidation(oAuth2GoogleLoginRequest)
                        .enqueue(MainRetrofitTool.getCallback(new LoginCallback()));

                Intent intent = new Intent(Login.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.e(TAG, "signInResult:failed code=" + e.getStatusCode());

        }
    }
    //kakao session callback
    public class SessionCallback implements ISessionCallback {

        // ???????????? ????????? ??????
        @Override
        public void onSessionOpened() {
            requestMe();
        }

        // ???????????? ????????? ??????
        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            Log.e("SessionCallback :: ", "onSessionOpenFailed : " + exception.getMessage());
        }

        // ????????? ?????? ??????
        public void requestMe() {
            UserManagement.getInstance()
                    .me(new MeV2ResponseCallback() {
                        @Override
                        public void onSessionClosed(ErrorResult errorResult) {
                            Log.e("KAKAO_API", "????????? ?????? ??????: " + errorResult);
                        }

                        @Override
                        public void onFailure(ErrorResult errorResult) {
                            Log.e("KAKAO_API", "????????? ?????? ?????? ??????: " + errorResult);
                        }

                        @Override
                        public void onSuccess(MeV2Response result) {
//                            onBackPressed();
                            // kakao id token?
                            Log.i("KAKAO_API", "????????? ?????????: " + result.getId());
//                            Log.i("KAKAO_API", "????????? ??????: " + );


                            UserAccount kakaoAccount = result.getKakaoAccount();
                            if (kakaoAccount != null) {

                                // ?????????
                                String email = kakaoAccount.getEmail();

                                if (email != null) {
                                    Log.i("KAKAO_API", "email: " + email);

                                } else if (kakaoAccount.emailNeedsAgreement() == OptionalBoolean.TRUE) {
                                    // ?????? ?????? ??? ????????? ?????? ??????
                                    // ???, ?????? ????????? ???????????? ????????? ????????? ?????? ???????????? ????????? ????????? ????????? ???????????? ???????????? ?????????.

                                } else {
                                    // ????????? ?????? ??????
                                }

                                // ?????????
                                Profile profile = kakaoAccount.getProfile();


                                if (profile != null) {

                                    Log.d("KAKAO_API", "nickname: " + profile.getNickname());
                                    Log.d("KAKAO_API", "profile image: " + profile.getProfileImageUrl());
                                    Log.d("KAKAO_API", "thumbnail image: " + profile.getThumbnailImageUrl());
                                    String accessToken = AuthApiClient.getInstance().getTokenManagerProvider()
                                            .getManager().getToken().getAccessToken();
                                    Log.d("KAKAO_API", "accessToken: "+ accessToken);
                                    OAuth2KakaoLoginRequest oAuth2KakaoLoginRequest = new OAuth2KakaoLoginRequest(accessToken);
                                    RetrofitTool.getAPIWithNullConverter().kakaoAccessTokenValidation(oAuth2KakaoLoginRequest)
                                            .enqueue(MainRetrofitTool.getCallback(new LoginCallback()));

                                } else if (kakaoAccount.profileNeedsAgreement() == OptionalBoolean.TRUE) {
                                    // ?????? ?????? ??? ????????? ?????? ?????? ??????

                                } else {
                                    // ????????? ?????? ??????
                                }

                            }
                            Intent intent = new Intent(Login.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                    });
        }
    }
    //naver login method
    public void naverSignIn() {
        mOAuthLoginModule = OAuthLogin.getInstance();
        mOAuthLoginModule.init(
                mContext
                ,getString(R.string.naver_client_id)
                ,getString(R.string.naver_client_secret)
                ,getString(R.string.naver_client_name)
                //,OAUTH_CALLBACK_INTENT
                // SDK 4.1.4 ??????????????? OAUTH_CALLBACK_INTENT????????? ???????????? ????????????.
        );

        @SuppressLint("HandlerLeak")
        OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler() {
            @Override
            public void run(boolean success) {
                if (success) {
                    String accessToken = mOAuthLoginModule.getAccessToken(mContext);
                    String refreshToken = mOAuthLoginModule.getRefreshToken(mContext);
                    long expiresAt = mOAuthLoginModule.getExpiresAt(mContext);
                    String tokenType = mOAuthLoginModule.getTokenType(mContext);

                    Log.i("LoginData","accessToken : "+ accessToken);
                    Log.i("LoginData","refreshToken : "+ refreshToken);
                    Log.i("LoginData","expiresAt : "+ expiresAt);
                    Log.i("LoginData","tokenType : "+ tokenType);

                    OAuth2NaverLoginRequest oAuth2NaverLoginRequest = new OAuth2NaverLoginRequest(accessToken);
                    RetrofitTool.getAPIWithNullConverter().naverAccessTokenValidation(oAuth2NaverLoginRequest)
                            .enqueue(MainRetrofitTool.getCallback(new LoginCallback()));

                    System.out.println("testnaver");

                    onBackPressed();

                } else {
                    String errorCode = mOAuthLoginModule
                            .getLastErrorCode(mContext).getCode();
                    String errorDesc = mOAuthLoginModule.getLastErrorDesc(mContext);
                    Toast.makeText(mContext, "errorCode:" + errorCode
                            + ", errorDesc:" + errorDesc, Toast.LENGTH_SHORT).show();
                }
            };
        };
        mOAuthLoginModule.startOauthLoginActivity(Login.this, mOAuthLoginHandler);
    }


}