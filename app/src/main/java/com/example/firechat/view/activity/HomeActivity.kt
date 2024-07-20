package com.example.firechat.view.activity

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.example.firechat.R
import com.example.firechat.databinding.HomeActivityBinding
import com.example.firechat.databinding.HomeNavigationBinding
import com.example.firechat.model.data.CurrentUserData
import com.example.firechat.service.TaskRemoveService
import com.example.firechat.util.*
import com.example.firechat.view.adapter.ChattingListRecyclerAdapter
import com.example.firechat.view.dialog.LoadingDialog
import com.example.firechat.viewModel.AuthViewModel
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: HomeActivityBinding
    private lateinit var drawer: HomeNavigationBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerButton: ImageButton
    private lateinit var drawerProfile: ImageButton
    private lateinit var newChatButton: ImageButton
    private lateinit var logoutButton: ImageButton
    lateinit var chattingRoomRecycler: RecyclerView
    private lateinit var loadingDialog: LoadingDialog

    private lateinit var uid: String
    private lateinit var profileURI: Uri
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var profileReference: StorageReference
    private var lastBackPressedTime = 0L
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initProperty()
        initView()
        initListener()
        setRecycler()

        // 뒤로가기 버튼 클릭 콜백 연결
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        // 앱 강제종료시 Firebase 로그아웃을 위한 서비스 시작
        startService(Intent(this, TaskRemoveService::class.java))
    }

    // 메모리 누수 방지를 위해 Destory시 콜백을 비활성화
    override fun onDestroy() {
        super.onDestroy()
        backPressedCallback.isEnabled = false
    }

    // 현재 사용자 정보를 가지고 있는 Singleton 객체에서 uid를 가져옴
    // 사용자의 uid/profile.jpg로 FireStorage 파일 경로 지정
    private fun initProperty() {
        uid = CurrentUserData.uid!!
        firebaseStorage = FirebaseStorage.getInstance()
        firebaseStorage.maxDownloadRetryTimeMillis = 10000
        firebaseStorage.maxUploadRetryTimeMillis = 10000
        profileReference = firebaseStorage.getReference("$uid/profile.jpg")
    }

    private fun initView() {
        chattingRoomRecycler = binding.chattingRoomRecycler
        newChatButton = binding.newChat
        drawerButton = binding.homeDrawerButton
        drawerLayout = binding.homeDrawerLayout
        loadingDialog = LoadingDialog(this)

        // 채팅방 드로어에 사용할 View 초기화
        // 드로어 내의 요소에 접근할 때 ViewBinding을 이용
        // 나가기 버튼은 드로어 하단에 위치함
        drawer = binding.homeDrawer
        logoutButton = drawer.homeDrawerBottomMenuLogout
        drawerProfile = drawer.homeDrawerProfileImage
        drawer.homeDrawerId.text = CurrentUserData.userName
        drawer.homeDrawerEmail.text = CurrentUserData.email
        getProfileImage()
    }

    private fun initListener() {
        // 드로어 안의 로그아웃 버튼 선택시 로그아웃 함수 실행
        logoutButton.setOnClickListener {
            logout()
        }

        // 새로운 채팅 생성 버튼
        // 클릭시 채팅방 생성 Activity 시작
        newChatButton.setOnClickListener {
            startActivity(Intent(this, NewChatActivity::class.java))
        }

        // 드로어 버튼을 누르면 우측에서(GravityCompat.END) 드로어 메뉴가 등장
        drawerButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        // 드로어의 프로필 이미지를 누르면 새로운 프로필 이미지를 업로드하기 위한 과정 수행
        drawerProfile.setOnClickListener {
            val intent =
                Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            registerForActivityResult.launch(intent)
        }
    }

    // 프로필 이미지를 새로운 이미지로 바꾸기 위한 작업 수행
    private val registerForActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    loadingDialog.show()
                    profileURI = result.data?.data!!
                    uploadProfileImage(this)
                }
            }
        }

    // 리사이클러 뷰를 초기화하는 메소드
    // decoration을 사용해 Item에 구분선을 추가함
    private fun setRecycler() {
        chattingRoomRecycler.layoutManager = LinearLayoutManager(this)
        val decoration = DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL)
        chattingRoomRecycler.addItemDecoration(decoration)

        val adapter = ChattingListRecyclerAdapter(this)
        adapter.setHasStableIds(true)
        chattingRoomRecycler.setHasFixedSize(true)
        chattingRoomRecycler.recycledViewPool.setMaxRecycledViews(0, 0)
        chattingRoomRecycler.adapter = adapter
    }

    // 로그아웃 메소드
    private fun logout() {
        // 로그아웃 경고창
        AlertDialog.Builder(this)
            .setTitle("로그아웃")
            .setMessage("로그아웃 하시겠습니까?")
            .setPositiveButton("확인") { dialog, _ ->
                // View Model의 로그아웃 메소드 호출 후 로그인 화면으로 돌아감
                viewModel.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                dialog.dismiss()
                finish()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    // 드로어에 현재 유저의 프로필 이미지를 불러오는 메소드
    // 이미지를 원형으로 잘라서(RequestOptions().circleCrop()) 삽입
    private fun getProfileImage() {
        Glide.with(this)
            .load(profileReference)
            .timeout(10000)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(R.drawable.baseline_person_24)
            .apply(RequestOptions().circleCrop())
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (e?.message != null) {
                        if (!e.message!!.contains("Object does not exist at location.")) {
                            handleError(
                                this@HomeActivity,
                                e,
                                "Profile Image Error",
                                "프로필 이미지 에러",
                                "프로필 이미지 관련 에러"
                            )
                        }
                    }

                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            })
            .into(drawerProfile)
    }

    // 새로운 프로필 이미지를 업로드하는 메소드
    private fun uploadProfileImage(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            withTimeout(10000) {
                try {
                    // URI를 사용해 서버에 프로필 이미지를 업로드
                    profileReference.putFile(profileURI).addOnSuccessListener {
                        showText(context, "프로필 이미지 업로드를 성공했습니다.")
                        getProfileImage()
                    }.addOnFailureListener {
                        runOnUiThread {
                            handleError(
                                context,
                                it,
                                "Uploading Profile Image Error",
                                "프로필 이미지 업로드 에러",
                                "프로필 이미지 업로드를 실패했습니다."
                            )
                        }
                    }.await()
                } catch (_: Exception) {
                } finally {
                    runOnUiThread {
                        loadingDialog.dismiss()
                    }
                }
            }
        }
    }

    // 뒤로가기 버튼을 두번 클릭시 앱을 종료하는 기능을 수행하는 콜백
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // 만약 드로어가 열려있다면 열려있는 드로어를 닫음
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawers()
            } else {
                if (System.currentTimeMillis() > lastBackPressedTime + 2000) {
                    lastBackPressedTime = System.currentTimeMillis()
                    showText(this@HomeActivity, "뒤로가기 버튼을 한번 더 누르면 앱이 종료됩니다.")
                } else {
                    finish()
                }
            }
        }
    }
}