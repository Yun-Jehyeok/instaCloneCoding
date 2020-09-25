package com.instaclonecoding

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import android.util.Base64


class LoginActivity : AppCompatActivity() {
    //firebase authentication(인증) library 불러옴
    var auth : FirebaseAuth? = null
    //googleSignInClient 객체 생성
    //구글 공식 문서를 보면 사용자의 ID와 기본 Profile 정보를 요청하기 위해 해당 객체를 생성하라고 지시한다.
    var googleSignInClient : GoogleSignInClient? = null
    //구글 로그인에 사용할 request 코드
    var GOOGLE_LOGIN_CODE = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        email_login_button.setOnClickListener {
            signinAndSignup()
        }

        google_sign_in_button.setOnClickListener {
            googleLogin()
        }

        //GoogleSignInClient 객체 생성(gso)
        //구글 로그인 옵션
        //사용자 ID, 이메일 주소 및 기본 프로필을 요청하도록 로그인 구성
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            //내장되어 있는 구글 api키를 받아오고
            .requestIdToken(getString(R.string.default_web_client_id))
            //이메일 받아오고
            .requestEmail()
            //build
            .build()

        //받아온 정보를 googleSignInClient에 세팅
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    fun printHashKey() {
        try {
            val info: PackageInfo = getPackageManager()
                .getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = String(Base64.encode(md.digest(), 0))
                Log.i("TAG", "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e("TAG", "printHashKey()", e)
        } catch (e: Exception) {
            Log.e("TAG", "printHashKey()", e)
        }
    }

    //회원가입
    fun signinAndSignup() {
        //이메일과 비밀번호를 auth에 넣어주며, addOnCompleteListener를 이용해 성공유무의 값 확인
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())?.addOnCompleteListener {
            //화살표 함수 비슷하게 이해하면 될듯합니다.
                task ->
            if(task.isSuccessful) {
                //회원가입 성공
                moveMainPage(task.result?.user)
            } else if(!task.exception?.message.isNullOrEmpty()) {
                //회원가입 실패
                //Error Toast 띄우기
                //Toast란 앱을 사용하다보면 잠깐 메세지가 떴다 자동으로 사라지는 경우가 있는데 이를 "Toast 메시지"라고 한다.
                //Toast.LENGTH_LONG은 토스트를 시간적으로 길게 보여주고 싶을 때 사용
                //반대의 경우는 SHORT
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            } else {
                //이미 있는 아이디
                signinEmail()
            }
        }
    }

    //로그인
    fun signinEmail() {
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())?.addOnCompleteListener {
                task ->
            if(task.isSuccessful) {
                //로그인
                moveMainPage(task.result?.user)
            } else {
                //로그인 실패
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    //로그인 성공 후, 메인페이지로 이동
    //firebase의 유저 상태를 파라미터로
    fun moveMainPage(user:FirebaseUser?) {
        //유저가 있을 경우
        if(user != null) {
            //MainActivity를 호출하고 MainActivity로 이동
            //Intent는 Activity 전환 시 객체를 전달할 때 사용한다.
            //이 경우 MainActivity로 Activity가 전환되며, Intent를 통해 this를 전달한다.
            //this는 user가 되겠죠
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    fun googleLogin() {
        //googleSignInClient 안에는 사용자 정보를 불러올 getClient가 들어있고
        //그걸 Intent로 설정
        var signInIntent = googleSignInClient?.signInIntent
        //startActivity와 유사한 기능이며
        //이번엔 구글에 지정돼 있는 구글 계정 선택 페이지로 넘어간다.
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == GOOGLE_LOGIN_CODE) {
            //구글에서 넘어오는 로그인 결과값
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)

            if(result!!.isSuccess) {
                var account = result?.signInAccount

                //firebase에 account 넘겨줌
                firebaseAuthWithGoogle(account)
            }
        }
    }

    fun firebaseAuthWithGoogle(account : GoogleSignInAccount?) {
        //account에서 id 토큰을 가져온다.
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener {
                    task ->
                if(task.isSuccessful) {
                    //로그인
                    moveMainPage(task.result?.user)
                } else {
                    //로그인 실패
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }
}